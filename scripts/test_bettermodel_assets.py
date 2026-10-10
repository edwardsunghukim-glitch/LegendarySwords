#!/usr/bin/env python3
"""Regression checks for actual authored motions, loops and effect assets."""
from __future__ import annotations

import base64
import importlib.util
import json
import math
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/bettermodel"
SWORDS = {"master_sword", "flameblade", "fierce_deity_sword", "frostblade",
          "thunderblade", "royal_claymore", "eightfold_blade", "biggoron_sword"}
SWORDS |= {w["id"] for w in json.loads(Path(__file__).with_name("expanded_weapons.json").read_text())}
REVISED_ACTION_SWORDS = {"master_sword", "fierce_deity_sword", "thunderblade",
                        "eightfold_blade", "biggoron_sword"}
ITEM_MODELS = Path(__file__).resolve().parents[1] / "resource-pack/assets/legendaryswords/models/item"


def load(kind, sword):
    suffix = "_slash" if kind == "models" else ""
    return json.loads((ROOT / kind / f"legendaryswords_{sword}{suffix}.bbmodel").read_text())


def frames(animator, channel="rotation"):
    return [key for key in animator["keyframes"] if key["channel"] == channel]


def values(key):
    return tuple(float(key["data_points"][0][axis]) for axis in "xyz")


def clips(model):
    return {animation["name"]: animation for animation in model["animations"]}


def clearance_module():
    path = Path(__file__).with_name("player_pose_clearance.py")
    spec = importlib.util.spec_from_file_location("player_pose_clearance", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def animated_item_model(sword):
    return json.loads((ITEM_MODELS / (sword + "_animated.json")).read_text())


def motion_signature(animation):
    # Ignore UUIDs/names so cosmetic metadata cannot hide identical motions.
    return tuple(sorted((animator["name"], tuple((key["channel"], key["time"], values(key))
                        for key in animator["keyframes"]))
                        for animator in animation["animators"].values()))


def rotate_player_vector(vector, angles):
    """BetterModel's Blockbench 5 conversion followed by its JOML ZYX rotation."""
    x, y, z = vector
    a, b, c = map(math.radians, (-angles[0], angles[1], -angles[2]))
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    return (x * math.cos(c) - y * math.sin(c), x * math.sin(c) + y * math.cos(c), z)


class BetterModelAssetsTest(unittest.TestCase):
    def test_animated_fixed_geometry_matches_authored_hand_transform(self):
        module = clearance_module()
        for sword in sorted(SWORDS):
            base = json.loads((ITEM_MODELS / (sword + ".json")).read_text())
            animated = animated_item_model(sword)
            with self.subTest(sword=sword):
                self.assertEqual(animated["display"]["fixed"], base["display"]["thirdperson_righthand"])
                self.assertEqual(module.held_item_samples(animated, "fixed"),
                                 module.held_item_samples(base, "thirdperson_righthand"))

    def test_carry_and_pose_clear_full_arms_and_held_blades(self):
        module = clearance_module()
        for sword in sorted(SWORDS):
            checker = module.PlayerPoseClearance(load("players", sword),
                                                 animated_item_model(sword), "fixed")
            for name in ("idle", "walk", "run", "pose"):
                with self.subTest(sword=sword, motion=name):
                    self.assertFalse(checker.collisions(name), "arm or held blade intersects the body")

    def test_revised_action_clips_clear_full_arms_and_held_blades(self):
        module = clearance_module()
        for sword in sorted(REVISED_ACTION_SWORDS):
            checker = module.PlayerPoseClearance(load("players", sword),
                                                 animated_item_model(sword), "fixed")
            for name in ("swing", "ability", "ultimate"):
                with self.subTest(sword=sword, motion=name):
                    self.assertFalse(checker.collisions(name), "action puts arm or held blade inside the body")

    def test_expanded_cast_clips_keep_arms_and_items_outside_body(self):
        module = clearance_module()
        for sword in sorted(SWORDS):
            checker = module.PlayerPoseClearance(load("players", sword), animated_item_model(sword), "fixed")
            for name in ("ability", "ultimate", "release"):
                with self.subTest(sword=sword, motion=name):
                    self.assertFalse(checker.collisions(name), "cast clips intersect player body")

    def test_walking_hands_stay_forward_and_knees_bend_back(self):
        for sword in SWORDS:
            model = load("players", sword)
            origins = {group["name"]: (-group["origin"][0], group["origin"][1], -group["origin"][2])
                       for group in model["groups"]}
            for name in ("idle", "walk", "run"):
                by_name = {animator["name"]: frames(animator)
                           for animator in clips(model)[name]["animators"].values()}
                for index in range(len(by_name["pra_right_arm"])):
                    # Follow the actual shoulder/elbow/torso pivots to the sword grip.
                    # At entity yaw zero Minecraft forward is positive world Z.
                    hand = origins["pri_right_item"]
                    for bone in ("prfa_right_forearm", "pra_right_arm", "pc_chest",
                                 "pw_waist", "phip_hip"):
                        origin = origins[bone]
                        relative = rotate_player_vector(tuple(a - b for a, b in zip(hand, origin)),
                                                        values(by_name[bone][index]))
                        hand = tuple(a + b for a, b in zip(relative, origin))
                    self.assertGreater(hand[2], 0, f"{sword} {name}: sword grip behind body")
            for name in ("walk", "run"):
                by_name = {animator["name"]: frames(animator)
                           for animator in clips(model)[name]["animators"].values()}
                for key in by_name["phip_hip"]:
                    self.assertEqual(values(key), (0, 0, 0), "hips must remain attached to thighs")
                for shin in ("prfl_right_foreleg", "plfl_left_foreleg"):
                    for key in by_name[shin]:
                        self.assertLessEqual(values(key)[0], 0, "recovery knee bends forward")

    def test_all_swords_have_complete_player_and_effect_clips(self):
        self.assertEqual(len(list(ROOT.rglob("*.bbmodel"))), len(SWORDS) * 2 + 5)
        for sword in SWORDS:
            for kind, expected in (("players", {"idle", "walk", "run", "pose", "swing", "ability", "ultimate", "draw", "charge", "release", "recover"}),
                                   ("models", {"swing", "ability", "ultimate"})):
                model = load(kind, sword)
                self.assertEqual(model["meta"]["format_version"], "5.0")
                self.assertEqual(set(clips(model)), expected)
                groups = {group["uuid"] for group in model["groups"]}
                for animation in model["animations"]:
                    self.assertGreater(animation["length"], 0)
                    self.assertTrue(animation["animators"])
                    self.assertLessEqual(set(animation["animators"]), groups)
                    for animator in animation["animators"].values():
                        for channel in {key["channel"] for key in animator["keyframes"]}:
                            keys = frames(animator, channel)
                            times = [key["time"] for key in keys]
                            self.assertEqual(times, sorted(set(times)))
                            self.assertGreaterEqual(times[0], 0)
                            self.assertLessEqual(times[-1], animation["length"])

    def test_locomotion_loops_match_seams_and_animate_split_legs(self):
        for sword in SWORDS:
            model = load("players", sword)
            for animation in model["animations"]:
                for animator in animation["animators"].values():
                    # Animations follow the real player; no vertical root hops.
                    self.assertFalse(animator["name"] == "player_root" and frames(animator, "position"))
                self.assertEqual(animation["loop"], "loop" if animation["name"] in {"idle", "walk", "run"} else "once")
            for name in ("idle", "walk", "run"):
                animation = clips(model)[name]
                for animator in animation["animators"].values():
                    keys = frames(animator)
                    self.assertEqual(values(keys[0]), values(keys[-1]))
                if name != "idle":
                    by_name = {animator["name"]: animator for animator in animation["animators"].values()}
                    for leg in ("prl_right_leg", "pll_left_leg", "prfl_right_foreleg", "plfl_left_foreleg"):
                        self.assertGreater(len({values(key) for key in frames(by_name[leg])}), 3)
            self.assertLess(clips(model)["run"]["length"], clips(model)["walk"]["length"])

    def test_each_sword_has_distinct_guard_actions_and_running_motion(self):
        for name in ("idle", "walk", "run", "swing", "ability", "ultimate", "pose"):
            signatures = {motion_signature(clips(load("players", sword))[name]) for sword in SWORDS}
            self.assertEqual(len(signatures), len(SWORDS), f"repeated {name} choreography")
        for sword in SWORDS:
            for name in ("swing", "ability", "ultimate", "pose"):
                animation = clips(load("players", sword))[name]
                moving = sum(len({values(key) for key in frames(animator)}) > 1
                             for animator in animation["animators"].values())
                self.assertGreaterEqual(moving, 4, f"{sword} {name} lacks full-body motion")

    def test_effect_shapes_are_distinct_and_embedded_textures_are_valid(self):
        shapes = set()
        for sword in SWORDS:
            model = load("models", sword)
            shapes.add(tuple((tuple(cube["from"]), tuple(cube["to"])) for cube in model["elements"]))
            self.assertGreaterEqual(len(model["elements"]), 50)
            self.assertEqual(len(model["groups"]), 3)
            for animation in model["animations"]:
                for animator in animation["animators"].values():
                    scales = frames(animator, "scale")
                    self.assertLess(max(values(scales[0])), .02)
                    self.assertLess(max(values(scales[-1])), .02)
                    self.assertGreater(max(max(values(key)) for key in scales), 1)
            for texture in model["textures"]:
                data = base64.b64decode(texture["source"].split(",", 1)[1], validate=True)
                self.assertTrue(data.startswith(b"\x89PNG\r\n\x1a\n"))
                offset = 8
                while offset < len(data):
                    length = struct.unpack(">I", data[offset:offset + 4])[0]
                    block = data[offset + 4:offset + 8 + length]
                    crc = struct.unpack(">I", data[offset + 8 + length:offset + 12 + length])[0]
                    self.assertEqual(zlib.crc32(block) & 0xffffffff, crc)
                    offset += length + 12
                self.assertEqual(offset, len(data))
        self.assertEqual(len(shapes), len(SWORDS), "signature effect shapes repeat")

    def test_boss_forms_are_original_monsters_with_animated_limbs_and_no_equipment_bones(self):
        shapes=set()
        for sword in ['angel_sovereign','granite_colossus','sky_leviathan','infernal_dragon','frost_titan']:
            model=json.loads((ROOT/'models'/f'legendaryswords_{sword}_boss.bbmodel').read_text())
            self.assertGreater(len(model['elements']),20)
            self.assertEqual({g['name'] for g in model['groups']},{'body','head','left','right','tail'})
            self.assertTrue({'idle','transform','smash'}.issubset(clips(model)))
            if sword=='infernal_dragon': self.assertIn('breath',clips(model))
            for g in model['groups']:
                node=next(n for n in model['outliner'] if n['uuid']==g['uuid'])
                self.assertTrue(node['children'], sword+' has an invisible group')
            self.assertTrue(any(len({values(k) for k in frames(a)})>1 for a in clips(model)['smash']['animators'].values()))
            shapes.add(tuple((tuple(e['from']),tuple(e['to'])) for e in model['elements']))
        self.assertEqual(len(shapes),5)

    def test_generator_reproduces_committed_assets_without_external_files(self):
        path = Path(__file__).with_name("generate_bettermodel_assets.py")
        spec = importlib.util.spec_from_file_location("legendaryswords_model_authoring", path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory(prefix="legendaryswords-assets-") as tmp:
            generated = Path(tmp)
            module.ROOT = generated
            module.main()
            for expected in ROOT.rglob("*.bbmodel"):
                actual = generated / expected.relative_to(ROOT)
                self.assertEqual(actual.read_bytes(), expected.read_bytes(), expected.name)


if __name__ == "__main__":
    unittest.main(verbosity=2)
