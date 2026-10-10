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
NAMES = ("master_sword", "flameblade", "fierce_deity_sword")


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


# Sixteen 16-pixel material tiles, expressed in Minecraft's 0..16 UV space.
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
}


def texture(name):
    """Paint a small brushed-metal atlas with glowing cracks and runic inlays."""
    frames = 4 if name != "master_sword" else 1
    pixels = []
    emissive_pixels = []
    for frame in range(frames):
        for y in range(64):
            for x in range(64):
                tile = x // 16 + 4 * (y // 16)
                lx, ly = x % 16, y % 16
                base = PALETTES[name][tile]
                grain = ((lx * 19 + ly * 11 + tile * 3) % 13) - 6
                ridge = 17 if lx in (0, 1) else (-20 if lx in (14, 15) else 0)
                if tile in (2, 8, 14):
                    grain += round(22 * math.sin((ly + frame * 2) * 0.8))
                    if (lx + ly * 2 + frame) % 7 == 0:
                        grain += 25
                if tile == 4:
                    grain += 15 if (lx + ly) % 4 == 0 else -5
                if tile == 6:
                    ridge += round(35 * (1 - abs(lx - 7.5) / 7.5))
                if tile == 8:
                    grain += 27 if ((lx in (7, 8) and ly > 2) or (ly in (4, 11) and lx in (4, 11))) else -10
                color = [max(0, min(255, v + grain + ridge)) for v in base]
                pixels.extend(color + [255])
                # Only energy, gem, rune, inlay and core tiles get client-optional
                # emissive overlays. The rest stays transparent, including metal.
                emissive_pixels.extend(color + [255] if tile in (2, 6, 8, 13, 14) else [0, 0, 0, 0])
    path = ASSETS / "textures" / "item" / (name + ".png")
    png(path, 64, 64 * frames, pixels)
    emissive_path = path.with_name(name + "_e.png")
    png(emissive_path, 64, 64 * frames, emissive_pixels)
    if frames > 1:
        metadata = {"animation": {"frametime": 4 if name == "flameblade" else 7,
                                  "width": 64, "height": 64,
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
    for name, geometry in zip(NAMES, (master_sword, flameblade, fierce_deity_sword)):
        texture(name)
        write_json(ASSETS / "items" / (name + ".json"),
                   {"model": {"type": "minecraft:model", "model": "legendaryswords:item/" + name}})
        write_json(ASSETS / "models" / "item" / (name + ".json"),
                   {"credit": "Original LegendarySwords voxel art; authored procedurally, no ripped assets.",
                    "ambientocclusion": False, "gui_light": "front",
                    "textures": {"atlas": "legendaryswords:item/" + name,
                                 "particle": "legendaryswords:item/" + name},
                    "elements": geometry(), "display": DISPLAY})
    write_json(ROOT / "pack.mcmeta", {"pack": {"description": "LegendarySwords - three legendary blades with original voxel art",
                                               "min_format": [75, 0], "max_format": [75, 0]}})
    icon()
    emissive_config = ROOT / "assets" / "minecraft" / "optifine" / "emissive.properties"
    emissive_config.parent.mkdir(parents=True, exist_ok=True)
    emissive_config.write_text("# Optional OptiFine-compatible emissive textures; vanilla ignores this.\nsuffix.emissive=_e\n", encoding="ascii")
    print("Generated original sword geometry, three texture atlases, animations, and pack icon.")


if __name__ == "__main__":
    main()
