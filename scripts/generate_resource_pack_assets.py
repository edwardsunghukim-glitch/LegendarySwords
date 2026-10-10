#!/usr/bin/env python3
"""Regenerate original voxel sword art. Uses only Python's standard library.

This authoring command intentionally writes resource-pack source files. Normal
builds use build_resource_pack.py and do not regenerate or change source art.
"""

import json
import math
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / "resource-pack"
ASSETS = ROOT / "assets" / "legendaryswords"
NAMES = ("master_sword", "flameblade", "fierce_deity_sword", "frostblade",
         "thunderblade", "royal_claymore", "eightfold_blade", "biggoron_sword")
TEXTURE_SIZE = 128


def png(path, width, height, pixels):
    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))
    raw = b"".join(b"\x00" + bytes(pixels[y * width * 4:(y + 1) * width * 4])
                   for y in range(height))
    encoded = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(encoded)


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


# Sixteen 32-pixel material tiles, expressed in Minecraft's 0..16 UV space.
TILES = {name: [(i % 4) * 4, (i // 4) * 4,
                (i % 4) * 4 + 4, (i // 4) * 4 + 4]
         for i, name in enumerate(("blade", "edge", "energy", "guard",
                                   "grip", "gold", "gem", "dark",
                                   "rune", "metal", "tip", "accent",
                                   "stripe", "inlay", "core", "shadow"))}


PALETTES = {
    "master_sword": [(160, 218, 239), (226, 247, 252), (84, 217, 247), (73, 51, 137),
                     (50, 41, 91), (229, 195, 101), (65, 225, 209), (42, 53, 73),
                     (144, 223, 249), (144, 151, 169), (202, 239, 253), (91, 91, 171),
                     (78, 102, 123), (174, 162, 229), (201, 253, 255), (40, 30, 69)],
    "flameblade": [(237, 103, 39), (255, 202, 84), (255, 139, 32), (80, 45, 40),
                   (60, 36, 32), (201, 150, 77), (255, 95, 32), (52, 41, 45),
                   (255, 199, 93), (108, 69, 60), (255, 226, 139), (197, 68, 40),
                   (117, 63, 37), (242, 162, 57), (255, 237, 155), (40, 28, 27)],
    "fierce_deity_sword": [(127, 207, 193), (203, 246, 237), (102, 244, 227), (85, 101, 124),
                          (49, 61, 81), (169, 188, 187), (178, 245, 255), (41, 63, 70),
                          (219, 253, 255), (94, 130, 133), (226, 255, 250), (94, 159, 175),
                          (92, 120, 123), (152, 226, 229), (229, 255, 255), (28, 43, 62)],
    "frostblade": [(135, 187, 225), (211, 242, 255), (83, 205, 255), (64, 103, 156),
                   (38, 60, 91), (164, 195, 226), (161, 229, 255), (32, 61, 92),
                   (221, 251, 255), (88, 133, 174), (237, 255, 255), (74, 147, 211),
                   (100, 148, 197), (130, 222, 254), (215, 251, 255), (20, 33, 65)],
    "thunderblade": [(183, 164, 63), (255, 236, 144), (255, 220, 54), (87, 62, 28),
                     (47, 37, 28), (234, 172, 52), (255, 242, 113), (50, 43, 38),
                     (254, 243, 161), (126, 102, 53), (255, 252, 204), (183, 114, 36),
                     (126, 87, 29), (242, 202, 72), (255, 255, 225), (31, 25, 22)],
    "royal_claymore": [(183, 189, 208), (239, 246, 251), (241, 213, 127), (164, 110, 34),
                       (93, 32, 44), (231, 180, 67), (223, 71, 98), (51, 42, 60),
                       (245, 201, 99), (118, 133, 151), (244, 248, 255), (164, 51, 66),
                       (107, 117, 139), (239, 192, 78), (253, 238, 191), (43, 25, 40)],
    "eightfold_blade": [(189, 202, 215), (239, 246, 250), (235, 122, 120), (57, 55, 63),
                       (62, 25, 30), (198, 149, 76), (237, 91, 96), (34, 35, 46),
                       (236, 167, 157), (125, 137, 151), (235, 246, 252), (110, 37, 45),
                       (101, 118, 139), (217, 99, 94), (255, 211, 198), (27, 20, 28)],
    "biggoron_sword": [(152, 173, 172), (218, 239, 226), (237, 132, 72), (103, 81, 53),
                       (75, 47, 30), (189, 143, 68), (248, 156, 64), (48, 54, 52),
                       (228, 172, 100), (109, 125, 126), (224, 246, 231), (139, 84, 40),
                       (102, 130, 128), (201, 128, 62), (255, 223, 157), (34, 32, 27)],
}


def texture(name):
    """Paint faceted metal, leather stitches, ornamental etching and energy.

    Metal stays stable between frames; only luminous materials animate. Each
    sword has its own pattern instead of uniformly pulsing the whole atlas.
    """
    frames = 4
    pixels = []
    emissive_pixels = []
    for frame in range(frames):
        for y in range(TEXTURE_SIZE):
            for x in range(TEXTURE_SIZE):
                tile = x // 32 + 4 * (y // 32)
                lx, ly = x % 32, y % 32
                base = PALETTES[name][tile]
                grain = ((lx * 19 + ly * 11 + tile * 3) % 9) - 4
                ridge = 24 if lx < 3 else (-29 if lx > 28 else 0)
                ridge += round(8 * math.cos(lx * math.pi / 32))
                if tile in (0, 1, 9, 10, 12):
                    # Fine directional forge lines and occasional scratches.
                    grain += 6 if (lx * 3 + ly) % 17 == 0 else 0
                    grain -= 8 if (lx + ly // 5 + tile) % 23 == 0 else 0
                if tile in (2, 6, 8, 13, 14):
                    phase = ly - frame * 6
                    grain += round(16 * math.sin(phase * 0.27))
                    if name == "flameblade":
                        grain += 24 if (lx * 3 + phase * 2) % 19 < 3 else -6
                    elif name == "frostblade":
                        grain += 23 if abs(lx - 16) == abs(phase % 32 - 16) else -2
                    elif name == "thunderblade":
                        arc = 15 + round(8 * math.sin(phase * 0.6))
                        grain += 42 if abs(lx - arc) < 2 else -14
                    elif name == "fierce_deity_sword":
                        grain += round(17 * math.cos(lx * 0.3 + frame))
                    else:
                        grain += 18 if (lx + phase * 2) % 23 < 2 else 0
                if tile == 4:
                    grain += 15 if (lx + ly // 2) % 8 < 2 else -7
                    grain += 15 if lx in (4, 27) and ly % 5 < 2 else 0
                if tile in (3, 5, 11):
                    grain += 12 if (lx + ly) % 14 == 0 else 0
                    grain -= 14 if (lx - ly) % 21 == 0 else 0
                if tile == 6:
                    ridge += round(34 * (1 - (abs(lx - 15.5) + abs(ly - 15.5)) / 31))
                if tile == 8:
                    grain += 28 if ((lx in (15, 16) and 4 < ly < 28)
                                     or (ly in (8, 23) and lx in range(7, 25))
                                     or (lx in (7, 24) and 8 < ly < 24)) else -15
                color = [max(0, min(255, v + grain + ridge)) for v in base]
                pixels.extend(color + [255])
                # Only energy, gem, rune, inlay and core tiles get client-optional
                # emissive overlays. The rest stays transparent, including metal.
                emissive_pixels.extend(color + [255] if tile in (2, 6, 8, 13, 14) else [0, 0, 0, 0])
    path = ASSETS / "textures" / "item" / (name + ".png")
    png(path, TEXTURE_SIZE, TEXTURE_SIZE * frames, pixels)
    emissive_path = path.with_name(name + "_e.png")
    png(emissive_path, TEXTURE_SIZE, TEXTURE_SIZE * frames, emissive_pixels)
    if frames > 1:
        metadata = {"animation": {"frametime": {"flameblade": 3, "thunderblade": 2,
                                                "frostblade": 8, "fierce_deity_sword": 6}.get(name, 10),
                                  "width": TEXTURE_SIZE, "height": TEXTURE_SIZE,
                                  "interpolate": True, "frames": [0, 1, 2, 3]}}
        write_json(path.with_suffix(".png.mcmeta"), metadata)
        write_json(emissive_path.with_suffix(".png.mcmeta"), metadata)


def box(elements, name, start, end, material, side=None, rotation=None):
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        tile = side if side and face not in ("north", "south") else material
        faces[face] = {"uv": TILES[tile], "texture": "#atlas"}
    element = {"name": name, "from": start, "to": end, "faces": faces}
    if rotation:
        element["rotation"] = {"origin": rotation[0], "axis": "z",
                               "angle": rotation[1], "rescale": False}
    elements.append(element)


def grip(elements, guard="guard", handle="grip", length=5):
    box(elements, "Faceted pommel", [6.9, -2.9, 6.9], [9.1, -1.5, 9.1], guard, "dark")
    box(elements, "Pommel jewel", [7.35, -3.35, 7.35], [8.65, -2.8, 8.65], "gem")
    box(elements, "Wrapped handle", [7.35, -1.5, 7.35], [8.65, length, 8.65], handle, "shadow")
    for i in range(7):
        y = -1.2 + i * 0.78
        box(elements, "Raised grip wrap %d" % i, [7.25, y, 7.25], [8.75, y + 0.22, 8.75], "accent", "dark")
    box(elements, "Gold grip collar", [7.05, length - 0.6, 7.05], [8.95, length, 8.95], "gold")


def master_sword():
    elements = []
    grip(elements)
    box(elements, "Winged crossguard", [4.8, 5, 6.9], [11.2, 6.2, 9.1], "guard", "shadow")
    for sign in (-1, 1):
        x0, x1 = (2.4, 5.9) if sign < 0 else (10.1, 13.6)
        box(elements, "Purple swept guard wing", [x0, 4.3, 7.15], [x1, 5.5, 8.85], "guard", "accent",
            ([5.8 if sign < 0 else 10.2, 5.2, 8], -22.5 if sign < 0 else 22.5))
        x0, x1 = (3, 4.5) if sign < 0 else (11.5, 13)
        box(elements, "Wing end ridge", [x0, 4, 7.3], [x1, 4.7, 8.7], "accent", "guard")
    box(elements, "Golden guard crest", [7.0, 5.3, 6.6], [9.0, 7.1, 9.4], "gold", "guard")
    box(elements, "Turquoise crest jewel", [7.55, 5.8, 6.35], [8.45, 6.7, 9.65], "gem", "energy")
    for side in (-1, 1):
        for i in range(2):
            x0 = 5.1 + i * 0.65 if side < 0 else 9.6 + i * 0.65
            box(elements, "Gold guard filigree", [x0, 5.35, 6.77], [x0 + 0.55, 5.9, 9.23], "gold", "guard")
        x0 = 6.85 if side < 0 else 8.75
        box(elements, "Pommel facet", [x0, -2.7, 6.8], [x0 + 0.4, -1.75, 9.2], "accent", "guard")
        box(elements, "Jeweled grip collar", [x0, 4.0, 6.95], [x0 + 0.4, 4.55, 9.05], "gem", "gold")
    box(elements, "Blade shoulder", [6.3, 7.1, 7.3], [9.7, 9.4, 8.7], "metal", "edge")
    box(elements, "Forged blade", [6.75, 9.4, 7.45], [9.25, 25.7, 8.55], "blade", "edge")
    box(elements, "Light edge left", [6.45, 10, 7.6], [6.75, 25.7, 8.4], "edge")
    box(elements, "Light edge right", [9.25, 10, 7.6], [9.55, 25.7, 8.4], "edge")
    box(elements, "Central sacred inlay", [7.87, 11, 7.32], [8.13, 24.9, 8.68], "energy", "blade")
    # Stepped geometry, rather than a copied logo texture, creates a small
    # three-triangle sacred crest on both sides of the forged blade shoulder.
    for center_x, base_y in ((8, 8.6), (7.48, 7.85), (8.52, 7.85)):
        for tier, width in enumerate((0.72, 0.34)):
            for z0, z1 in ((7.16, 7.29), (8.71, 8.84)):
                box(elements, "Sacred crest stepped triangle", [center_x - width / 2, base_y + tier * 0.35, z0],
                    [center_x + width / 2, base_y + tier * 0.35 + 0.32, z1], "gold")
    for x in (7.12, 8.65):
        box(elements, "Shoulder inlay border", [x, 7.2, 7.17], [x + 0.23, 7.65, 8.83], "inlay", "gold")
    for y in (10.2, 11.0, 11.8):
        box(elements, "Runic mark", [7.5, y, 7.25], [8.5, y + 0.26, 8.75], "rune")
    box(elements, "Tapered tip base", [7.05, 25.7, 7.5], [8.95, 27, 8.5], "tip", "edge")
    box(elements, "Tapered tip crown", [7.35, 27, 7.6], [8.65, 28, 8.4], "tip", "edge")
    box(elements, "Blade point", [7.75, 28, 7.7], [8.25, 28.8, 8.3], "core")
    return elements


def flameblade():
    elements = []
    grip(elements)
    box(elements, "Dark iron crossguard", [4.5, 5, 6.6], [11.5, 6.4, 9.4], "guard", "dark")
    for x in (4.1, 10.7):
        box(elements, "Guard ember studs", [x, 5.2, 6.3], [x + 1.2, 6.7, 9.7], "gold", "dark")
    for x in (5.8, 7.75, 9.6):
        for z0, z1 in ((6.42, 6.6), (9.4, 9.58)):
            box(elements, "Faceted guard rivet", [x, 5.2, z0], [x + 0.55, 5.9, z1], "gold", "metal")
    for y in (-1.3, 0.85, 3.7):
        box(elements, "Copper grip wire", [7.2, y, 7.2], [8.8, y + 0.16, 8.8], "gold", "dark")
    box(elements, "Caged ember gem", [7.1, 5.2, 6.25], [8.9, 7.2, 9.75], "gem", "gold")
    box(elements, "Heavy blade base", [5.9, 7.2, 7.0], [10.1, 11, 9.0], "metal", "dark")
    box(elements, "Molten great blade", [6.0, 11, 7.15], [10.0, 24.5, 8.85], "blade", "accent")
    box(elements, "Bright cutting edge", [5.6, 11.2, 7.4], [6.0, 24.5, 8.6], "edge")
    box(elements, "Bright back edge", [10.0, 11.2, 7.4], [10.35, 24.0, 8.6], "edge")
    for x in (6.2, 9.4):
        box(elements, "Sculpted forge channel", [x, 12.1, 7.02], [x + 0.28, 22.7, 8.98], "dark", "metal")
    for i in range(8):
        y = 9.0 + i * 1.95
        x = 7.3 + (0.5 if i % 2 else -0.5)
        box(elements, "Animated magma fissure %d" % i, [x, y, 6.97], [x + 0.55, y + 2.3, 9.03], "energy", "core",
            ([x + 0.3, y + 1, 8], 22.5 if i % 2 else -22.5))
        if i % 2 == 0:
            box(elements, "Branching molten crack", [x - 0.65, y + 0.95, 6.93], [x + 0.3, y + 1.2, 9.07], "energy", "core")
    box(elements, "Flame tip shoulder", [6.4, 24.5, 7.2], [9.65, 25.8, 8.8], "blade", "edge")
    box(elements, "Flame tip slope", [6.9, 25.8, 7.3], [9.2, 26.9, 8.7], "tip", "edge")
    box(elements, "Flame tip point", [7.4, 26.9, 7.55], [8.6, 28.2, 8.45], "core", "edge")
    for x, y in ((5.5, 17), (10.0, 20), (5.7, 23)):
        box(elements, "Jagged flame silhouette", [x, y, 7.4], [x + 0.7, y + 2.6, 8.6], "energy", "tip",
            ([x + 0.35, y, 8], -22.5 if x < 8 else 22.5))
        box(elements, "Flame spur stepped middle", [x + 0.15, y + 1.9, 7.5], [x + 0.6, y + 3.0, 8.5], "tip", "energy")
        box(elements, "Flame spur point", [x + 0.3, y + 3.0, 7.6], [x + 0.5, y + 3.45, 8.4], "core")
    return elements


def fierce_deity_sword():
    elements = []
    grip(elements, length=5)
    box(elements, "Silver crossguard", [4.4, 5, 7.0], [11.6, 6.3, 9.0], "guard", "edge")
    for x, angle in ((4.1, 22.5), (10.4, -22.5)):
        box(elements, "Crescent guard end", [x, 4.7, 7.2], [x + 1.5, 6.9, 8.8], "accent", "edge",
            ([x + 0.75, 5.5, 8], angle))
    box(elements, "Lunar guard jewel", [7.25, 5.4, 6.55], [8.75, 7.5, 9.45], "gem", "guard")
    for x in (5.0, 6.15, 9.5, 10.65):
        box(elements, "Silver lunar guard engraving", [x, 5.25, 6.87], [x + 0.35, 6.05, 9.13], "inlay", "metal")
    for x in (7.05, 8.65):
        for y in (5.4, 6.9):
            box(elements, "Jewel setting claw", [x, y, 6.4], [x + 0.3, y + 0.45, 9.6], "metal", "edge")
    for x in (7.25, 8.3):
        box(elements, "Lunar pommel engraving", [x, -2.7, 6.72], [x + 0.4, -1.7, 9.28], "rune", "core")
    # Alternating diagonal pairs form the recognizable open double-helix blade.
    for i in range(5):
        y = 7.6 + i * 3.65
        for side in (-1, 1):
            x = 6.0 if side < 0 else 8.7
            angle = 22.5 * side * (1 if i % 2 == 0 else -1)
            box(elements, "Helical blade %d %d" % (i, side), [x, y, 7.4], [x + 1.3, y + 4.15, 8.6],
                "blade", "edge", ([x + 0.65, y + 2.075, 8], angle))
            box(elements, "Lunar blade inlay %d %d" % (i, side), [x + 0.45, y + 0.2, 7.27],
                [x + 0.85, y + 3.95, 8.73], "energy", "core",
                ([x + 0.65, y + 2.075, 8], angle))
            box(elements, "Helix bright raised edge %d %d" % (i, side), [x + 0.02, y + 0.18, 7.31],
                [x + 0.2, y + 3.97, 8.69], "edge", "inlay",
                ([x + 0.65, y + 2.075, 8], angle))
    box(elements, "Joining spear point", [6.7, 25.7, 7.4], [9.3, 26.7, 8.6], "blade", "edge")
    box(elements, "Spear tip middle", [7.1, 26.7, 7.5], [8.9, 27.7, 8.5], "tip", "edge")
    box(elements, "Spear tip crown", [7.6, 27.7, 7.65], [8.4, 28.7, 8.35], "core")
    return elements


def frostblade():
    """An asymmetric crystalline blade with visible frozen facets and shards."""
    elements = []
    grip(elements)
    box(elements, "Icebound silver guard", [4.5, 5, 6.7], [11.5, 6.3, 9.3], "guard", "edge")
    for x, angle in ((3.2, -22.5), (11, 22.5)):
        box(elements, "Swept crystalline quillon", [x, 5.2, 7.1], [x + 1.8, 7.1, 8.9], "blade", "edge",
            ([x + 0.9, 5.5, 8], angle))
        box(elements, "Frozen guard shard", [x + 0.5, 6.7, 7.35], [x + 1.25, 8, 8.65], "tip", "energy")
    box(elements, "Blue sapphire setting", [7, 5.2, 6.5], [9, 7.5, 9.5], "metal", "gold")
    box(elements, "Living frost crystal", [7.4, 5.5, 6.3], [8.6, 7.25, 9.7], "gem", "core")
    box(elements, "Glacier blade shoulder", [5.9, 7.5, 7.05], [10.1, 11, 8.95], "blade", "metal")
    box(elements, "Frozen blade body", [6.3, 11, 7.2], [9.55, 25.4, 8.8], "blade", "edge")
    box(elements, "Crystalline cutting face", [5.9, 11.3, 7.5], [6.3, 25.4, 8.5], "edge", "tip")
    box(elements, "Recessed cold fuller", [7.35, 9, 7], [8.65, 24.8, 9], "dark", "metal")
    box(elements, "Animated icy vein", [7.84, 9, 6.84], [8.16, 27.1, 9.16], "energy", "core")
    for i in range(5):
        y = 10.5 + i * 2.9
        x = 8.75 if i % 2 else 6.4
        box(elements, "Etched frost branch %d" % i, [x, y, 6.93], [x + 0.7, y + 1.9, 9.07], "inlay", "core",
            ([x + 0.35, y + 1, 8], 22.5 if i % 2 else -22.5))
        for z0, z1 in ((6.9, 7.05), (8.95, 9.1)):
            box(elements, "Carved snow rune %d" % i, [7.4, y + 0.9, z0], [8.6, y + 1.15, z1], "rune")
    for i, (x, y, height) in enumerate(((9.25, 12.8, 3), (9.3, 17.4, 3.6), (9.25, 22, 4.1), (5.2, 21.6, 2.6))):
        angle = -22.5 if x < 8 else 22.5
        box(elements, "Ice saw crystal %d" % i, [x, y, 7.25], [x + 1.3, y + height, 8.75], "blade", "edge",
            ([x + 0.5, y + 0.3, 8], angle))
        box(elements, "Saw crystal tip %d" % i, [x + 0.37, y + height - 0.1, 7.5],
            [x + 0.85, y + height + 0.8, 8.5], "core", "tip")
    box(elements, "Jagged central spear", [6.85, 25.4, 7.3], [9.05, 27.3, 8.7], "tip", "edge")
    box(elements, "Glacier spear bevel", [7.25, 27.3, 7.45], [8.65, 28.65, 8.55], "edge", "energy")
    box(elements, "Clear ice apex", [7.75, 28.65, 7.6], [8.25, 29.8, 8.4], "core")
    for x in (7.05, 8.55):
        box(elements, "Sapphire pommel frame", [x, -2.8, 6.8], [x + 0.4, -1.6, 9.2], "metal", "edge")
    return elements


def thunderblade():
    """A forked conductor sword with a deliberately open electric tip."""
    elements = []
    grip(elements)
    box(elements, "Bronze conductor guard", [4.6, 5, 6.6], [11.4, 6.4, 9.4], "guard", "dark")
    for x, sign in ((3.9, -1), (10.9, 1)):
        box(elements, "Lightning guard hook", [x, 5.2, 7], [x + 1.2, 8, 9], "gold", "metal",
            ([x + 0.6, 5.5, 8], sign * 22.5))
        box(elements, "Charged quillon tip", [x + 0.25, 7.3, 7.3], [x + 0.95, 8.5, 8.7], "energy", "core")
    box(elements, "Amber capacitor housing", [6.7, 5.1, 6.3], [9.3, 7.6, 9.7], "metal", "gold")
    box(elements, "Amber capacitor", [7.15, 5.65, 6.1], [8.85, 7.1, 9.9], "gem", "core")
    for x in (6.75, 8.9):
        box(elements, "Capacitor prong", [x, 5.3, 6.05], [x + 0.35, 7.4, 9.95], "gold", "edge")
    box(elements, "Blade conductor base", [5.8, 7.7, 7.05], [10.2, 10.8, 8.95], "metal", "edge")
    box(elements, "Deep electric channel", [6.1, 10.8, 7.2], [9.9, 22, 8.8], "dark", "metal")
    for side in (-1, 1):
        x = 5.7 if side < 0 else 9.4
        box(elements, "Conductive blade rail", [x, 10.8, 7.2], [x + 0.9, 24, 8.8], "blade", "edge")
        box(elements, "Rail bright edge", [x - 0.14 if side < 0 else x + 0.85, 11, 7.5],
            [x + 0.05 if side < 0 else x + 1.05, 24, 8.5], "edge")
        fork_x = 6.15 if side < 0 else 9
        box(elements, "Separated lightning tine", [fork_x, 23.3, 7.35], [fork_x + 0.85, 27.6, 8.65], "blade", "edge",
            ([fork_x + 0.42, 23.8, 8], -22.5 * side))
        box(elements, "Charged fork crown", [fork_x + 0.18, 27.2, 7.5], [fork_x + 0.68, 29.1, 8.5], "energy", "core",
            ([fork_x + 0.42, 23.8, 8], -22.5 * side))
    for i in range(6):
        y = 10.3 + i * 1.95
        x = 7.25 if i % 2 == 0 else 8.05
        box(elements, "Animated zigzag conductor %d" % i, [x, y, 7], [x + 0.55, y + 2.6, 9], "energy", "core",
            ([x + 0.275, y + 1.3, 8], 22.5 if i % 2 == 0 else -22.5))
    for y in (10, 15, 20):
        box(elements, "Gold rail insulator band", [5.55, y, 7.02], [10.45, y + 0.3, 8.98], "gold", "dark")
        for x in (6.4, 9):
            box(elements, "Insulator bronze pin", [x, y - 0.25, 6.88], [x + 0.6, y + 0.6, 9.12], "metal", "gold")
    for y in (-0.9, 1, 2.9):
        box(elements, "Copper wire grip winding", [7.17, y, 7.17], [8.83, y + 0.18, 8.83], "gold", "dark")
    return elements


def long_grip(elements, theme="guard", length=6):
    box(elements, "Two handed wrapped grip", [7.15, -3, 7.15], [8.85, length, 8.85], "grip", "shadow")
    box(elements, "Oversized forged pommel", [6.7, -4.5, 6.7], [9.3, -3, 9.3], theme, "metal")
    box(elements, "Pommel inset jewel", [7.3, -4.75, 6.5], [8.7, -3.3, 9.5], "gem", "gold")
    for i in range(9):
        y = -2.7 + i * 0.95
        box(elements, "Long grip braid %d" % i, [7.04, y, 7.04], [8.96, y + 0.2, 8.96], "accent", "dark")
    for y in (-2.7, length - 0.5):
        box(elements, "Grip end collar", [6.95, y, 6.95], [9.05, y + 0.5, 9.05], "gold", "guard")


def royal_claymore():
    """A broad silver greatsword, elaborate gold hilt and carved royal fuller."""
    elements = []
    long_grip(elements)
    box(elements, "Gilded royal crossguard", [3.8, 6, 6.6], [12.2, 7.3, 9.4], "gold", "guard")
    for x, sign in ((2.5, -1), (11.5, 1)):
        box(elements, "Sweeping royal quillon", [x, 5.5, 6.9], [x + 2, 7.25, 9.1], "gold", "guard",
            ([x + 1, 6.4, 8], sign * 22.5))
        box(elements, "Quillon crimson jewel", [x + 0.6, 5.2, 6.7], [x + 1.4, 6.4, 9.3], "gem", "gold")
    box(elements, "Royal crest shield", [6.55, 6.25, 6.15], [9.45, 9.1, 9.85], "gold", "metal")
    box(elements, "Crest enamel inset", [6.95, 6.6, 5.98], [9.05, 8.7, 10.02], "accent", "gold")
    for tier, (x0, x1) in enumerate(((7.15, 8.85), (7.45, 8.55), (7.8, 8.2))):
        box(elements, "Embossed golden crest %d" % tier, [x0, 7.0 + tier * 0.43, 5.85],
            [x1, 7.37 + tier * 0.43, 10.15], "gold", "core")
    box(elements, "Broad silver ricasso", [5.4, 9.1, 7.05], [10.6, 12, 8.95], "metal", "edge")
    box(elements, "Broad royal blade", [5.65, 12, 7.25], [10.35, 27, 8.75], "blade", "edge")
    for x in (5.28, 10.35):
        box(elements, "Silver sharpened bevel", [x, 12, 7.5], [x + 0.37, 27, 8.5], "edge", "tip")
    box(elements, "Deep royal blade fuller", [7.6, 10, 7.05], [8.4, 26.7, 8.95], "dark", "metal")
    box(elements, "Golden blade spine", [7.88, 10, 6.92], [8.12, 27.3, 9.08], "inlay", "gold")
    for i in range(6):
        y = 12.3 + i * 2.1
        for x, angle in ((6.6, -22.5), (8.8, 22.5)):
            box(elements, "Royal engraved laurel %d" % i, [x, y, 7.08], [x + 0.45, y + 0.9, 8.92], "gold", "inlay",
                ([x + 0.225, y + 0.45, 8], angle))
    box(elements, "Royal taper lower", [6.2, 27, 7.35], [9.8, 28.4, 8.65], "tip", "edge")
    box(elements, "Royal taper upper", [6.9, 28.4, 7.5], [9.1, 29.6, 8.5], "tip", "edge")
    box(elements, "Royal crown blade apex", [7.6, 29.6, 7.65], [8.4, 30.5, 8.35], "core", "edge")
    for x in (6.95, 8.7):
        box(elements, "Crown pommel side", [x, -4.7, 6.65], [x + 0.35, -3, 9.35], "gold", "guard")
    return elements


def eightfold_blade():
    """A slim curved katana with lacquered hilt and a sculpted octagonal tsuba."""
    elements = []
    long_grip(elements, theme="dark", length=5)
    box(elements, "Octagonal tsuba middle", [5.25, 5, 6], [10.75, 5.75, 10], "guard", "gold")
    box(elements, "Octagonal tsuba front bevel", [6.15, 5, 5.55], [9.85, 5.75, 6], "guard", "gold")
    box(elements, "Octagonal tsuba rear bevel", [6.15, 5, 10], [9.85, 5.75, 10.45], "guard", "gold")
    for x in (5.0, 10.75):
        box(elements, "Tsuba beveled flank", [x, 5.08, 6.7], [x + 0.25, 5.67, 9.3], "gold", "dark")
    box(elements, "Brass habaki blade collar", [7.2, 5.75, 7.32], [9.2, 7.25, 8.68], "gold", "metal")
    # Successive lower-pivot sections overlap at their actual rotated endpoints;
    # otherwise an item mesh can look like a row of disconnected floating bars.
    segments = [(7.1, 7.25, 1.9, 6.95, 0), (7.125, 14.1, 1.85, 5.7, -22.5),
                (9.35, 19.3, 1.7, 5.0, -22.5), (11.3, 23.85, 1.5, 3.3, -22.5)]
    for i, (x, y, width, height, angle) in enumerate(segments):
        rotation = ([x + width / 2, y, 8], angle) if angle else None
        box(elements, "Curved forged katana section %d" % i, [x, y, 7.4], [x + width, y + height, 8.6], "blade", "edge", rotation)
        box(elements, "Katana tempered edge %d" % i, [x, y + 0.1, 7.28], [x + 0.25, y + height - 0.1, 8.72], "edge", "tip", rotation)
        box(elements, "Katana carved spine %d" % i, [x + width - 0.25, y + 0.1, 7.27],
            [x + width - 0.05, y + height - 0.1, 8.73], "stripe", "metal", rotation)
        box(elements, "Red animated hamon %d" % i, [x + 0.4, y + 0.2, 7.23],
            [x + 0.55, y + height - 0.2, 8.77], "energy", "inlay", rotation)
    box(elements, "Angled katana kissaki", [12.8, 26.8, 7.45], [13.7, 28.15, 8.55], "tip", "edge",
        ([13.25, 26.8, 8], -22.5))
    box(elements, "Curved katana apex", [13.56, 28.05, 7.6], [14, 28.85, 8.4], "core", "edge",
        ([13.78, 28.05, 8], -22.5))
    for i in range(7):
        y = -2.4 + i
        for x, angle in ((7.1, -45), (8.1, 45)):
            box(elements, "Crimson diamond grip wrap %d" % i, [x, y, 6.99], [x + 0.45, y + 0.75, 9.01], "accent", "gold",
                ([x + 0.22, y + 0.38, 8], angle))
    for x in (6.3, 9.3):
        box(elements, "Tsuba red inset", [x, 4.87, 6.6], [x + 0.4, 5.88, 9.4], "gem", "gold")
    box(elements, "Lacquered pommel plate", [6.65, -4.45, 6.65], [9.35, -3.75, 9.35], "dark", "gold")
    return elements


def biggoron_sword():
    """A massive rounded mountain-forged sword with stone-set bronze fittings."""
    elements = []
    long_grip(elements, theme="guard", length=6)
    box(elements, "Massive bronze crossguard", [3, 6, 6.2], [13, 7.4, 9.8], "guard", "metal")
    for x in (2.5, 11.7):
        box(elements, "Heavy guard end cap", [x, 5.7, 6.1], [x + 1.8, 7.8, 9.9], "gold", "metal")
    box(elements, "Mountain amber stone setting", [6.3, 6, 5.85], [9.7, 9, 10.15], "metal", "guard")
    box(elements, "Goron amber stone", [6.9, 6.6, 5.6], [9.1, 8.5, 10.4], "gem", "core")
    box(elements, "Wide reinforced blade heel", [3.8, 9, 6.8], [12.2, 12.6, 9.2], "metal", "dark")
    box(elements, "Mountain forged broad blade", [4.15, 12.6, 7], [11.85, 25.5, 9], "blade", "edge")
    for x in (3.7, 11.85):
        box(elements, "Broad pale cutting bevel", [x, 12.6, 7.35], [x + 0.45, 25.5, 8.65], "edge", "tip")
    box(elements, "Deep wide blade channel", [7.45, 10.2, 6.85], [8.55, 27.2, 9.15], "dark", "metal")
    box(elements, "Amber animated mountain spine", [7.84, 10.4, 6.71], [8.16, 28.8, 9.29], "energy", "inlay")
    # Broad stepped semicircle keeps this silhouette recognizably rounded.
    for i, (half_width, y0, y1) in enumerate(((3.6, 25.5, 27), (3.0, 27, 28.4),
                                             (2.2, 28.4, 29.6), (1.3, 29.6, 30.5), (0.5, 30.5, 31.1))):
        box(elements, "Rounded greatsword crown %d" % i, [8 - half_width, y0, 7.1 + i * 0.08],
            [8 + half_width, y1, 8.9 - i * 0.08], "tip", "edge")
    for i in range(5):
        y = 12.5 + i * 2.55
        for x in (5.15, 9.65):
            box(elements, "Mountain rune pillar %d" % i, [x, y, 6.82], [x + 0.4, y + 1.25, 9.18], "inlay", "metal")
            box(elements, "Mountain rune crossbar %d" % i, [x - 0.4, y + 0.6, 6.79], [x + 0.8, y + 0.86, 9.21], "rune", "gold")
    for x in (4.6, 5.8, 9.65, 10.85):
        box(elements, "Reinforcement rivet", [x, 10, 6.65], [x + 0.6, 10.7, 9.35], "gold", "metal")
    for x in (4.9, 10.55):
        box(elements, "Heel bronze blade border", [x, 11.1, 6.85], [x + 0.55, 12.4, 9.15], "gold", "dark")
    box(elements, "Hammered pommel bottom", [6.5, -4.5, 6.5], [9.5, -3.7, 9.5], "gold", "metal")
    return elements


DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0, 5.8, 0.5], "scale": [0.65] * 3},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 5.8, 0.5], "scale": [0.65] * 3},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.1, 4.0, -2], "scale": [0.6] * 3},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.1, 4.0, -2], "scale": [0.6] * 3},
    "gui": {"rotation": [0, 0, -35], "translation": [0, -2.0, 0], "scale": [0.55] * 3},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 4.5, 0], "scale": [0.5] * 3},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, -3.2, 0], "scale": [0.6] * 3},
    "head": {"rotation": [0, 0, 0], "translation": [0, 10, 0], "scale": [0.5] * 3},
}


def icon():
    # Original pixel emblem: luminous blade over a circular deep-purple badge.
    width = 64
    pixels = []
    for y in range(width):
        for x in range(width):
            distance = math.hypot(x - 31.5, y - 31.5)
            color = (23, 21, 45, 255)
            if distance < 28:
                color = (38, 34, 76, 255)
            if 25 < distance < 27:
                color = (103, 80, 157, 255)
            if 29 <= x <= 34 and 11 <= y <= 42:
                color = (148, 226, 246, 255)
            if 31 <= x <= 32 and 8 <= y <= 39:
                color = (229, 255, 255, 255)
            if 21 <= x <= 42 and 39 <= y <= 42:
                color = (158, 124, 245, 255)
            if 30 <= x <= 33 and 43 <= y <= 53:
                color = (110, 78, 154, 255)
            if 28 <= x <= 35 and 53 <= y <= 55:
                color = (236, 204, 120, 255)
            if (x, y) in ((15, 19), (46, 25), (21, 30), (43, 13)):
                color = (220, 249, 255, 255)
            pixels.extend(color)
    png(ROOT / "pack.png", width, width, pixels)


def main():
    geometries = (master_sword, flameblade, fierce_deity_sword, frostblade,
                  thunderblade, royal_claymore, eightfold_blade, biggoron_sword)
    for name, geometry in zip(NAMES, geometries):
        texture(name)
        display = json.loads(json.dumps(DISPLAY))
        if name in ("royal_claymore", "biggoron_sword"):
            display["gui"]["scale"] = [0.47] * 3
            display["firstperson_righthand"]["scale"] = [0.55] * 3
            display["firstperson_lefthand"]["scale"] = [0.55] * 3
            display["thirdperson_righthand"]["scale"] = [0.68] * 3
            display["thirdperson_lefthand"]["scale"] = [0.68] * 3
        if name == "eightfold_blade":
            display["gui"]["rotation"] = [0, 0, -28]
            display["gui"]["translation"] = [-1, -1, 0]
            display["gui"]["scale"] = [0.5] * 3
        elements = geometry()
        if len(elements) > 80:
            raise ValueError(name + " exceeds the bounded 80-cube art budget")
        write_json(ASSETS / "items" / (name + ".json"),
                   {"model": {"type": "minecraft:model", "model": "legendaryswords:item/" + name}})
        model = {"credit": "Original LegendarySwords voxel art; authored procedurally, no ripped assets.",
                    "ambientocclusion": False, "gui_light": "front",
                    "textures": {"atlas": "legendaryswords:item/" + name,
                                 "particle": "legendaryswords:item/" + name},
                    "elements": elements, "display": display}
        write_json(ASSETS / "models" / "item" / (name + ".json"), model)
        # Keep the actual inventory item visible while BetterModel renders its
        # animated hand. Only vanilla third-person hands use the empty branch.
        animated_name = name + "_animated"
        write_json(ASSETS / "items" / (animated_name + ".json"), {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["thirdperson_lefthand", "thirdperson_righthand"],
                       "model": {"type": "minecraft:empty"}}],
            "fallback": {"type": "minecraft:model", "model": "legendaryswords:item/" + animated_name},
        }})
        animated_model = json.loads(json.dumps(model))
        animated_model["display"]["fixed"] = json.loads(json.dumps(display["thirdperson_righthand"]))
        write_json(ASSETS / "models" / "item" / (animated_name + ".json"), animated_model)
        print("%s: %d sculpted elements, 128px animated material atlas" % (name, len(elements)))
    write_json(ROOT / "pack.mcmeta", {"pack": {"description": "LegendarySwords - eight detailed legendary blades",
                                               "min_format": [75, 0], "max_format": [75, 0]}})
    icon()
    emissive_config = ROOT / "assets" / "minecraft" / "optifine" / "emissive.properties"
    emissive_config.parent.mkdir(parents=True, exist_ok=True)
    emissive_config.write_text("# Optional OptiFine-compatible emissive textures; vanilla ignores this.\nsuffix.emissive=_e\n", encoding="ascii")
    print("Generated eight original sword models, animated emissive material atlases, and pack icon. Run expand_weapon_assets.py for the sixteen additions.")


if __name__ == "__main__":
    main()
