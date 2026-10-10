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

from generate_resource_pack_assets import NAMES, PALETTES, png

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "resource-pack"
WIDTH, HEIGHT = 1440, 1180

# A tiny authoring-preview font keeps this script dependency-free. These labels
# are part of the preview only and are never included in the Minecraft pack.
FONT = dict(zip("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-.", (
    "01110/10001/10001/11111/10001/10001/10001", "11110/10001/10001/11110/10001/10001/11110",
    "01111/10000/10000/10000/10000/10000/01111", "11110/10001/10001/10001/10001/10001/11110",
    "11111/10000/10000/11110/10000/10000/11111", "11111/10000/10000/11110/10000/10000/10000",
    "01111/10000/10000/10111/10001/10001/01111", "10001/10001/10001/11111/10001/10001/10001",
    "11111/00100/00100/00100/00100/00100/11111", "00111/00010/00010/00010/00010/10010/01100",
    "10001/10010/10100/11000/10100/10010/10001", "10000/10000/10000/10000/10000/10000/11111",
    "10001/11011/10101/10101/10001/10001/10001", "10001/11001/10101/10011/10001/10001/10001",
    "01110/10001/10001/10001/10001/10001/01110", "11110/10001/10001/11110/10000/10000/10000",
    "01110/10001/10001/10001/10101/10010/01101", "11110/10001/10001/11110/10100/10010/10001",
    "01111/10000/10000/01110/00001/00001/11110", "11111/00100/00100/00100/00100/00100/00100",
    "10001/10001/10001/10001/10001/10001/01110", "10001/10001/10001/10001/10001/01010/00100",
    "10001/10001/10001/10101/10101/11011/10001", "10001/10001/01010/00100/01010/10001/10001",
    "10001/10001/01010/00100/00100/00100/00100", "11111/00001/00010/00100/01000/10000/11111",
    "01110/10001/10011/10101/11001/10001/01110", "00100/01100/00100/00100/00100/00100/01110",
    "01110/10001/00001/00010/00100/01000/11111", "11110/00001/00001/01110/00001/00001/11110",
    "00010/00110/01010/10010/11111/00010/00010", "11111/10000/10000/11110/00001/00001/11110",
    "01110/10000/10000/11110/10001/10001/01110", "11111/00001/00010/00100/01000/01000/01000",
    "01110/10001/10001/01110/10001/10001/01110", "01110/10001/10001/01111/00001/00001/01110",
    "00000/00000/00000/11111/00000/00000/00000", "00000/00000/00000/00000/00000/00100/00100")))


def label(pixels, message, center_x, y, size=3, color=(214, 224, 243)):
    left = round(center_x - (len(message) * 6 - 1) * size / 2)
    for position, character in enumerate(message.upper()):
        glyph = FONT.get(character)
        if not glyph:
            continue
        for gy, row in enumerate(glyph.split("/")):
            for gx, value in enumerate(row):
                if value != "1":
                    continue
                for dy in range(size):
                    for dx in range(size):
                        x, py = left + (position * 6 + gx) * size + dx, y + gy * size + dy
                        if 0 <= x < WIDTH and 0 <= py < HEIGHT:
                            index = (py * WIDTH + x) * 4
                            pixels[index:index + 3] = bytes(color)


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
    return center[0] + oriented[0] * 11, center[1] - oriented[1] * 11, oriented[2]


def render():
    pixels = bytearray()
    for y in range(HEIGHT):
        for x in range(WIDTH):
            shade = int(12 + 9 * (1 - y / HEIGHT))
            if 125 <= y <= 590 or 660 <= y <= 1130:
                column = x // 360
                if 18 <= x - column * 360 <= 342:
                    shade += 4
            pixels.extend((shade, shade + 1, shade + 10, 255))
    depths = [float("inf")] * (WIDTH * HEIGHT)
    # (Vertex indices, diffuse shade), indexed in a common cube vertex order.
    faces = {"north": ((0, 1, 2, 3), 1.0), "south": ((5, 4, 7, 6), 0.79),
             "west": ((4, 0, 3, 7), 0.73), "east": ((1, 5, 6, 2), 0.86),
             "up": ((3, 2, 6, 7), 1.05), "down": ((4, 5, 1, 0), 0.63)}
    for position, name in enumerate(NAMES):
        center = (180 + 360 * (position % 4), 350 + 535 * (position // 4))
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
        title = name.replace("_", " ").upper()
        label_y = 548 + 535 * (position // 4)
        label(pixels, title, center[0], label_y, size=2)
        color = PALETTES[name][2]
        for y in range(label_y - 15, label_y - 12):
            for x in range(center[0] - 56, center[0] + 56):
                index = (y * WIDTH + x) * 4
                pixels[index:index + 3] = bytes(color)
    label(pixels, "LEGENDARY SWORDS", WIDTH // 2, 32, size=5, color=(233, 239, 255))
    label(pixels, "EIGHT ORIGINAL MODELS - 128PX TEXTURES - ANIMATED ENERGY", WIDTH // 2, 90,
          size=2, color=(142, 159, 190))
    label(pixels, "SOURCE ART PREVIEW - SIMPLE LIGHTING - NO SHADER BLOOM", WIDTH // 2, 1150,
          size=2, color=(116, 130, 156))
    output = PACK / "preview.png"
    png(output, WIDTH, HEIGHT, pixels)
    print(output)


if __name__ == "__main__":
    render()
