#!/usr/bin/env python3
"""Validate and reproducibly package the committed Minecraft resource-pack assets.

Uses only Python's standard library. Does not change source artwork. Maven can
package these same static assets without Python; use this for deeper validation.
"""

import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import shutil
import struct
import sys
import zipfile
import zlib

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PACK = ROOT / "resource-pack"
ITEMS = ("master_sword", "flameblade", "fierce_deity_sword")
RESOURCE_ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9/._-]+$")


class PackValidationError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise PackValidationError(message)


def load_json(path):
    require(path.is_file(), "Missing JSON: " + str(path))
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (UnicodeError, json.JSONDecodeError) as exc:
        raise PackValidationError("Invalid JSON in " + str(path) + ": " + str(exc)) from exc


def resource_path(pack, name, directory, suffix):
    require(isinstance(name, str) and RESOURCE_ID.fullmatch(name), "Invalid resource ID: " + repr(name))
    namespace, value = name.split(":", 1)
    require(".." not in value.split("/") and not value.startswith("/"), "Unsafe resource path: " + name)
    return pack / "assets" / namespace / directory / (value + suffix)


def read_png(path):
    data = path.read_bytes()
    require(data[:8] == b"\x89PNG\r\n\x1a\n", "Not a PNG: " + str(path))
    cursor, compressed, header, ended = 8, bytearray(), None, False
    while cursor < len(data):
        require(cursor + 12 <= len(data), "Truncated PNG chunk: " + str(path))
        length = struct.unpack(">I", data[cursor:cursor + 4])[0]
        kind = data[cursor + 4:cursor + 8]
        require(cursor + 12 + length <= len(data), "Truncated PNG data: " + str(path))
        payload = data[cursor + 8:cursor + 8 + length]
        checksum = struct.unpack(">I", data[cursor + 8 + length:cursor + 12 + length])[0]
        require(zlib.crc32(kind + payload) & 0xFFFFFFFF == checksum,
                "PNG checksum mismatch: " + str(path))
        if kind == b"IHDR":
            require(header is None and length == 13, "Invalid PNG header: " + str(path))
            header = struct.unpack(">IIBBBBB", payload)
        elif kind == b"IDAT":
            compressed.extend(payload)
        elif kind == b"IEND":
            require(length == 0 and cursor + 12 == len(data), "Invalid PNG end: " + str(path))
            ended = True
            break
        cursor += length + 12
    require(header is not None and ended, "Incomplete PNG: " + str(path))
    width, height, depth, color, compression, filtering, interlace = header
    require(width > 0 and height > 0, "Empty PNG: " + str(path))
    require((depth, color, compression, filtering, interlace) == (8, 6, 0, 0, 0),
            "Expected an 8-bit RGBA PNG: " + str(path))
    try:
        raw = zlib.decompress(compressed)
    except zlib.error as exc:
        raise PackValidationError("Invalid compressed PNG: " + str(path)) from exc
    stride = width * 4 + 1
    require(len(raw) == height * stride, "Invalid PNG pixel length: " + str(path))
    require(all(raw[row * stride] <= 4 for row in range(height)), "Invalid PNG filter: " + str(path))
    return width, height


def validate_animation(path, size):
    metadata = load_json(path)
    animation = metadata.get("animation", {})
    require(isinstance(animation, dict), "Invalid animation object: " + str(path))
    width = animation.get("width", min(size))
    height = animation.get("height", min(size))
    require(isinstance(width, int) and isinstance(height, int) and width > 0 and height > 0,
            "Invalid animation frame dimensions: " + str(path))
    require(size[0] % width == 0 and size[1] % height == 0,
            "Animation frames do not fit PNG: " + str(path))
    count = (size[0] // width) * (size[1] // height)
    require(animation.get("frametime", 1) > 0, "Animation frametime must be positive: " + str(path))
    for frame in animation.get("frames", range(count)):
        index = frame.get("index") if isinstance(frame, dict) else frame
        require(isinstance(index, int) and 0 <= index < count, "Animation index out of range: " + str(path))
        if isinstance(frame, dict):
            require(frame.get("time", 1) > 0, "Invalid frame time: " + str(path))


def vector(value, label, bounds=None):
    require(isinstance(value, list) and len(value) == 3, "Expected three coordinates: " + label)
    require(all(isinstance(x, (int, float)) and math.isfinite(x) for x in value), "Invalid coordinate: " + label)
    if bounds:
        require(all(bounds[0] <= x <= bounds[1] for x in value), "Coordinate out of range: " + label)


def validate_model(pack, path):
    model = load_json(path)
    require(isinstance(model.get("elements"), list) and model["elements"], "Missing geometry: " + str(path))
    textures = model.get("textures", {})
    require(isinstance(textures, dict) and textures, "Missing textures: " + str(path))
    for key, name in textures.items():
        texture_path = resource_path(pack, name, "textures", ".png")
        require(texture_path.is_file(), "Missing texture " + name + " referenced by " + str(path))
    for element in model["elements"]:
        vector(element.get("from"), str(path) + " from", (-16, 32))
        vector(element.get("to"), str(path) + " to", (-16, 32))
        require(all(a < b for a, b in zip(element["from"], element["to"])), "Degenerate model cube: " + str(path))
        rotation = element.get("rotation")
        if rotation:
            vector(rotation.get("origin"), str(path) + " rotation origin")
            require(rotation.get("axis") in ("x", "y", "z"), "Invalid rotation axis: " + str(path))
            require(rotation.get("angle") in (-45, -22.5, 0, 22.5, 45), "Unsupported element rotation: " + str(path))
        faces = element.get("faces", {})
        require(faces and set(faces).issubset({"north", "south", "east", "west", "up", "down"}),
                "Invalid model faces: " + str(path))
        for face in faces.values():
            texture = face.get("texture", "")
            require(texture.startswith("#") and texture[1:] in textures, "Unresolved face texture: " + str(path))
            uv = face.get("uv")
            require(isinstance(uv, list) and len(uv) == 4 and all(isinstance(x, (int, float)) and 0 <= x <= 16 for x in uv),
                    "Invalid texture UV: " + str(path))
    for display in model.get("display", {}).values():
        for component in ("rotation", "translation", "scale"):
            if component in display:
                vector(display[component], str(path) + " " + component)


def validate(pack):
    pack = Path(pack)
    metadata = load_json(pack / "pack.mcmeta").get("pack", {})
    require(metadata.get("min_format") == [75, 0] and metadata.get("max_format") == [75, 0],
            "Expected Minecraft 1.21.11 resource pack format 75.0")
    require(bool(metadata.get("description")), "Pack description is missing")
    json_paths = sorted(pack.rglob("*.json"))
    for path in json_paths:
        load_json(path)
    png_paths = sorted(pack.rglob("*.png"))
    for path in png_paths:
        size = read_png(path)
        animation = path.with_suffix(".png.mcmeta")
        if animation.is_file():
            validate_animation(animation, size)
        if path.stem.endswith("_e"):
            base_texture = path.with_name(path.stem[:-2] + ".png")
            require(base_texture.is_file(), "Emissive overlay has no base texture: " + str(path))
            require(size == read_png(base_texture), "Emissive overlay dimensions differ from base: " + str(path))
            base_animation = base_texture.with_suffix(".png.mcmeta")
            if base_animation.is_file():
                require(animation.is_file() and load_json(animation) == load_json(base_animation),
                        "Emissive animation differs from base: " + str(path))
    for path in pack.rglob("*.png.mcmeta"):
        require(path.with_suffix("").is_file(), "Animation metadata has no texture: " + str(path))
    for name in ITEMS:
        item_path = pack / "assets" / "legendaryswords" / "items" / (name + ".json")
        item = load_json(item_path).get("model", {})
        require(item.get("type") == "minecraft:model", "Expected modern item model in " + str(item_path))
        model_path = resource_path(pack, item.get("model"), "models", ".json")
        require(model_path.is_file(), "Item references missing model: " + str(item_path))
        validate_model(pack, model_path)
    return {"items": len(ITEMS), "json": len(json_paths), "png": len(png_paths)}


def archive_files(pack):
    for path in sorted(pack.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(pack)
        if relative.parts[0] == "assets" or str(relative) in ("pack.mcmeta", "pack.png"):
            yield path, relative.as_posix()


def build(pack=DEFAULT_PACK, output=None, bundled_output=None):
    pack = Path(pack)
    counts = validate(pack)
    output = Path(output or ROOT / "target" / "LegendarySwords-resource-pack.zip")
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path, relative in archive_files(pack):
            info = zipfile.ZipInfo(relative, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info, path.read_bytes(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    checksum = hashlib.sha1(output.read_bytes()).hexdigest()
    output.with_suffix(".sha1").write_text(checksum + "\n", encoding="ascii")
    bundled_output = Path(bundled_output or ROOT / "target" / "generated-resources" / "resource-pack.zip")
    bundled_output.parent.mkdir(parents=True, exist_ok=True)
    if output.resolve() != bundled_output.resolve():
        shutil.copyfile(output, bundled_output)
    return output, checksum, counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--validate-only", action="store_true", help="Validate without creating output files")
    parser.add_argument("--pack", type=Path, default=DEFAULT_PACK)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--bundled-output", type=Path)
    args = parser.parse_args()
    try:
        if args.validate_only:
            counts = validate(args.pack)
            print("Validated {items} item definitions, {json} JSON assets and {png} PNG files.".format(**counts))
        else:
            output, checksum, counts = build(args.pack, args.output, args.bundled_output)
            print("Validated {items} item definitions, {json} JSON assets and {png} PNG files.".format(**counts))
            print("Resource pack: " + str(output))
            print("SHA-1: " + checksum)
    except (PackValidationError, OSError) as exc:
        print("Resource pack validation failed: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
