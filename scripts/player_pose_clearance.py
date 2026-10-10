"""Check authored player poses against actual limb and held-item geometry.

All coordinates use model units (16 per block). This samples volume interiors;
it is a regression check, not continuous collision detection or a client render.
The .25-unit core inset allows normal skin surfaces and shoulder seams to touch.
"""
from __future__ import annotations

import itertools
import math

CORE_INSET = .25
IDENTITY = ((1., 0., 0.), (0., 1., 0.), (0., 0., 1.))
ARM_BONES = {"pra_right_arm", "prfa_right_forearm", "pla_left_arm", "plfa_left_forearm"}
BODY_BONES = {"phip_hip", "pw_waist", "pc_chest"}
HEAD_BONE = "h_ph_head"
ITEM_BONE = "pri_right_item"


def matmul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3))
                       for j in range(3)) for i in range(3))


def matvec(a, p):
    x, y, z = p
    return (a[0][0] * x + a[0][1] * y + a[0][2] * z,
            a[1][0] * x + a[1][1] * y + a[1][2] * z,
            a[2][0] * x + a[2][1] * y + a[2][2] * z)


def rotation(axis, degrees):
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    if axis == "x":
        return ((1., 0., 0.), (0., c, -s), (0., s, c))
    if axis == "y":
        return ((c, 0., s), (0., 1., 0.), (-s, 0., c))
    if axis == "z":
        return ((c, -s, 0.), (s, c, 0.), (0., 0., 1.))
    raise ValueError("Unknown rotation axis: " + axis)


def rotation_zyx(angles):
    return matmul(matmul(rotation("z", angles[2]), rotation("y", angles[1])),
                  rotation("x", angles[0]))


def invert_xz(vector):
    return -vector[0], vector[1], -vector[2]


def box_samples(low, high, count):
    return itertools.product(*[[a + (b - a) * i / (count - 1) for i in range(count)]
                               for a, b in zip(low, high)])


def held_item_samples(model, display_context="thirdperson_righthand"):
    """Apply actual JSON element rotations and the selected client display."""
    display = model["display"][display_context]
    angles = display.get("rotation", (0., 0., 0.))
    # Minecraft ItemTransform appends X, Y, then Z; BetterModel uses ZYX above.
    display_rotation = matmul(matmul(rotation("x", angles[0]), rotation("y", angles[1])),
                              rotation("z", angles[2]))
    scale = display.get("scale", (1., 1., 1.))
    translation = display.get("translation", (0., 0., 0.))
    result = []
    for element in model["elements"]:
        element_rotation = element.get("rotation")
        for point in box_samples(element["from"], element["to"], 3):
            if element_rotation:
                origin = element_rotation.get("origin", (8., 8., 8.))
                relative = tuple(point[i] - origin[i] for i in range(3))
                axis, angle = element_rotation["axis"], element_rotation["angle"]
                if element_rotation.get("rescale", False):
                    axis_index = "xyz".index(axis)
                    factor = 1. / math.cos(math.radians(angle))
                    relative = tuple(v if i == axis_index else v * factor
                                     for i, v in enumerate(relative))
                relative = matvec(rotation(axis, angle), relative)
                point = tuple(relative[i] + origin[i] for i in range(3))
            centered = tuple((point[i] - 8.) * scale[i] for i in range(3))
            point = matvec(display_rotation, centered)
            result.append(tuple(point[i] + translation[i] for i in range(3)))
    return result


def channel_value(keys, time):
    """Interpolate serialized keyframes, rather than importing authoring poses."""
    if not keys:
        return 0., 0., 0.
    previous = keys[0]
    for following in keys[1:]:
        if time <= following[0]:
            weight = max(0., (time - previous[0]) / (following[0] - previous[0]))
            return tuple(a + (b - a) * weight for a, b in zip(previous[1], following[1]))
        previous = following
    return previous[1]


class PlayerPoseClearance:
    """Prepare the actual bbmodel hierarchy, skin cubes and item model once."""
    def __init__(self, player_model, item_model, display_context="thirdperson_righthand"):
        if player_model["meta"]["format_version"] != "5.0":
            raise ValueError("Clearance conversion requires a Blockbench 5 model")
        self.model = player_model
        self.groups = {g["uuid"]: g for g in player_model["groups"]}
        self.names = {g["name"]: g["uuid"] for g in player_model["groups"]}
        self.parents = {}
        owners = {}
        self.order = []

        def visit(nodes, parent=None):
            for node in nodes:
                if isinstance(node, str):
                    owners[node] = parent
                else:
                    bone = node["uuid"]
                    self.parents[bone] = parent
                    self.order.append(bone)
                    visit(node["children"], bone)
        visit(player_model["outliner"])
        self.origins = {uid: invert_xz(group["origin"]) for uid, group in self.groups.items()}
        self.defaults = {uid: invert_xz(group.get("rotation", (0., 0., 0.)))
                         for uid, group in self.groups.items()}
        self.arm_points = {}
        self.body_boxes = []
        self.head_boxes = []
        for element in player_model["elements"]:
            bone = owners.get(element["uuid"])
            if bone is None or element["name"] != "skin":
                continue
            name = self.groups[bone]["name"]
            if name not in ARM_BONES | BODY_BONES | {HEAD_BONE}:
                continue
            # Actual template cubes have no independent rotation. Do not silently
            # approve a changed rig whose geometry requires another conversion.
            if any(element.get("rotation", (0., 0., 0.))):
                raise ValueError("Rotated player skin cube is not supported")
            first, last = invert_xz(element["from"]), invert_xz(element["to"])
            origin = self.origins[bone]
            low = tuple(min(a, b) + CORE_INSET - o for a, b, o in zip(first, last, origin))
            high = tuple(max(a, b) - CORE_INSET - o for a, b, o in zip(first, last, origin))
            if name in ARM_BONES:
                self.arm_points[bone] = list(box_samples(low, high, 7))
            else:
                target = self.head_boxes if name == HEAD_BONE else self.body_boxes
                target.append((bone, low, high))
        if len(self.arm_points) != 4 or len(self.body_boxes) != 3 or len(self.head_boxes) != 1:
            raise ValueError("Expected four split arms, three torso cubes and one head")
        self.item_points = held_item_samples(item_model, display_context)

    def frame(self, channels, time):
        """Compose default + loaded animation angles through the actual rig."""
        poses = {}
        for bone in self.order:
            animated = invert_xz(channel_value(channels.get(bone, ()), time))
            angles = tuple(a + b for a, b in zip(self.defaults[bone], animated))
            local_rotation = rotation_zyx(angles)
            parent = self.parents[bone]
            if parent is None:
                poses[bone] = self.origins[bone], local_rotation
            else:
                parent_position, parent_rotation = poses[parent]
                offset = tuple(a - b for a, b in zip(self.origins[bone], self.origins[parent]))
                offset = matvec(parent_rotation, offset)
                position = tuple(a + b for a, b in zip(parent_position, offset))
                poses[bone] = position, matmul(parent_rotation, local_rotation)
        return poses

    @staticmethod
    def inside(point, boxes, poses):
        for bone, low, high in boxes:
            position, matrix = poses[bone]
            x, y, z = (a - b for a, b in zip(point, position))
            # Inverse of a rotation matrix is its transpose.
            local = (matrix[0][0] * x + matrix[1][0] * y + matrix[2][0] * z,
                     matrix[0][1] * x + matrix[1][1] * y + matrix[2][1] * z,
                     matrix[0][2] * x + matrix[1][2] * y + matrix[2][2] * z)
            if all(a < value < b for a, value, b in zip(low, local, high)):
                return True
        return False

    def collisions(self, name):
        animation = next(a for a in self.model["animations"] if a["name"] == name)
        channels = {}
        times = {0., float(animation["length"])}
        for bone, animator in animation["animators"].items():
            keys = []
            for key in animator["keyframes"]:
                if key["channel"] != "rotation" or key["interpolation"] != "linear":
                    raise ValueError("Carry clearance expects authored linear rotation channels")
                time = float(key["time"])
                keys.append((time, tuple(float(key["data_points"][0][axis]) for axis in "xyz")))
                times.add(time)
            channels[bone] = sorted(keys)
        times = sorted(times)
        # Check authored poses, halfway through each transition, and loop endpoints.
        samples = sorted(set(times) | {(a + b) / 2 for a, b in zip(times, times[1:])})
        failures = {}
        for time in samples:
            poses = self.frame(channels, time)
            for bone, points in self.arm_points.items():
                part = self.groups[bone]["name"]
                if part in failures:
                    continue
                position, matrix = poses[bone]
                for point in points:
                    point = tuple(a + b for a, b in zip(position, matvec(matrix, point)))
                    if self.inside(point, self.body_boxes, poses):
                        failures[part] = round(time, 5)
                        break
            position, matrix = poses[self.names[ITEM_BONE]]
            for point in self.item_points:
                point = tuple(a + b for a, b in zip(position, matvec(matrix, point)))
                for part, boxes in (("sword inside torso", self.body_boxes),
                                    ("sword inside head", self.head_boxes)):
                    if part not in failures and self.inside(point, boxes, poses):
                        failures[part] = round(time, 5)
        return failures
