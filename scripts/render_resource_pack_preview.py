#!/usr/bin/env python3
"""Render an honest orthographic preview of the actual committed item geometry.

Optional authoring aid, standard library only. Does not simulate Minecraft glint,
shader bloom, particles or BetterModel animations. Source PNGs use RGBA/filter0.
"""

import json
import math
from pathlib import Path
import struct
import zlib

from generate_resource_pack_assets import png

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "resource-pack"
WIDTH, HEIGHT = 1024, 512


def read_atlas(path):
    data = path.read_bytes()
    cursor, compressed = 8, bytearray()
    while cursor < len(data):
        count = struct.unpack(">I", data[cursor:cursor + 4])[0]
        kind = data[cursor + 4:cursor + 8]
        payload = data[cursor + 8:cursor + 8 + count]
        if kind == b"IHDR":
            width, height = struct.unpack(">II", payload[:8])
        if kind == b"IDAT":
            compressed.extend(payload)
        cursor += count + 12
    raw = zlib.decompress(compressed)
    stride = width * 4 + 1
    rows = [raw[y * stride + 1:(y + 1) * stride] for y in range(width)]
    return width, b"".join(rows)


def rotate(point, axis, angle):
    a = math.radians(angle)
    c, s = math.cos(a), math.sin(a)
    x, y, z = point
    if axis == "z":
        return x * c - y * s, x * s + y * c, z
    if axis == "y":
        return x * c + z * s, y, -x * s + z * c
    return x, y * c - z * s, y * s + z * c


def project(point, center):
    centered = (point[0] - 8, point[1] - 12.2, point[2] - 8)
    oriented = rotate(rotate(rotate(centered, "y", -23), "x", -7), "z", -14)
    return center + oriented[0] * 12, 252 - oriented[1] * 12, oriented[2]


def render():
    pixels = bytearray()
    for y in range(HEIGHT):
        for x in range(WIDTH):
            shade = int(12 + 9 * (1 - y / HEIGHT))
            pixels.extend((shade, shade + 1, shade + 10, 255))
    depths = [float("inf")] * (WIDTH * HEIGHT)
    # (Vertex indices, diffuse shade), indexed in a common cube vertex order.
    faces = {"north": ((0, 1, 2, 3), 1.0), "south": ((5, 4, 7, 6), 0.79),
             "west": ((4, 0, 3, 7), 0.73), "east": ((1, 5, 6, 2), 0.86),
             "up": ((3, 2, 6, 7), 1.05), "down": ((4, 5, 1, 0), 0.63)}
    for center, name in zip((171, 512, 853), ("master_sword", "flameblade", "fierce_deity_sword")):
        base = PACK / "assets/legendaryswords"
        model = json.loads((base / "models/item" / (name + ".json")).read_text())
        texture_size, texture = read_atlas(base / "textures/item" / (name + ".png"))
        for element in model["elements"]:
            x0, y0, z0 = element["from"]
            x1, y1, z1 = element["to"]
            vertices = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0),
                        (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
            rotation = element.get("rotation")
            if rotation:
                origin = rotation["origin"]
                vertices = [tuple(rotate(tuple(p[i] - origin[i] for i in range(3)),
                                         rotation["axis"], rotation["angle"])[i] + origin[i] for i in range(3))
                            for p in vertices]
            projected = [project(v, center) for v in vertices]
            for face, face_data in element["faces"].items():
                indices, shade = faces[face]
                u0, v0, u1, v1 = face_data["uv"]
                uv = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
                for tri in ((0, 1, 2), (0, 2, 3)):
                    points = [projected[indices[i]] for i in tri]
                    coords = [uv[i] for i in tri]
                    ax, ay, az = points[0]
                    bx, by, bz = points[1]
                    cx, cy, cz = points[2]
                    denominator = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
                    if abs(denominator) < 0.000001:
                        continue
                    xmin, xmax = max(0, math.floor(min(ax, bx, cx))), min(WIDTH - 1, math.ceil(max(ax, bx, cx)))
                    ymin, ymax = max(0, math.floor(min(ay, by, cy))), min(HEIGHT - 1, math.ceil(max(ay, by, cy)))
                    for y in range(ymin, ymax + 1):
                        for x in range(xmin, xmax + 1):
                            px, py = x + 0.5, y + 0.5
                            a = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / denominator
                            b = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / denominator
                            c = 1 - a - b
                            if min(a, b, c) < -0.0001:
                                continue
                            z = a * az + b * bz + c * cz
                            index = y * WIDTH + x
                            if z >= depths[index]:
                                continue
                            u = a * coords[0][0] + b * coords[1][0] + c * coords[2][0]
                            v = a * coords[0][1] + b * coords[1][1] + c * coords[2][1]
                            tx = min(texture_size - 1, max(0, int(u / 16 * texture_size)))
                            ty = min(texture_size - 1, max(0, int(v / 16 * texture_size)))
                            texel = (ty * texture_size + tx) * 4
                            color = texture[texel:texel + 4]
                            for channel in range(3):
                                pixels[index * 4 + channel] = max(0, min(255, int(color[channel] * shade)))
                            depths[index] = z
    output = PACK / "preview.png"
    png(output, WIDTH, HEIGHT, pixels)
    print(output)


if __name__ == "__main__":
    render()
