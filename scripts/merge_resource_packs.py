#!/usr/bin/env python3
"""Combine BetterModel's generated ZIP and LegendarySwords' item pack safely."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
from zipfile import BadZipFile, ZIP_DEFLATED, ZipFile, ZipInfo

STAMP = (2026, 1, 1, 0, 0, 0)


def read_pack(path):
    entries = {}
    with ZipFile(path) as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            name = info.filename
            parts = PurePosixPath(name).parts
            if name.startswith("/") or "\\" in name or ".." in parts:
                raise ValueError(f"Unsafe ZIP path in {path}: {name}")
            if name in entries:
                raise ValueError(f"Duplicate ZIP entry in {path}: {name}")
            entries[name] = archive.read(info)
    if "pack.mcmeta" not in entries:
        raise ValueError(f"{path} must have pack.mcmeta at the ZIP root.")
    metadata = json.loads(entries["pack.mcmeta"])
    if not isinstance(metadata, dict) or not isinstance(metadata.get("pack"), dict):
        raise ValueError(f"Invalid pack metadata in {path}")
    return entries, metadata


def merge_packs(bettermodel, swords, output):
    if output.resolve() in {bettermodel.resolve(), swords.resolve()}:
        raise ValueError("The output must be a new file; do not overwrite either input pack.")
    entries, metadata = read_pack(bettermodel)
    sword_entries, sword_metadata = read_pack(swords)
    item_definitions = [name for name in sword_entries
                        if name.startswith("assets/legendaryswords/items/") and name.endswith(".json")]
    if not item_definitions:
        raise ValueError("The sword pack does not contain LegendarySwords item definitions.")
    for name, data in sword_entries.items():
        if name in {"pack.mcmeta", "pack.png"}:
            continue
        if name in entries and entries[name] != data:
            raise ValueError(f"Different assets share the path {name}; refusing to replace either one.")
        entries[name] = data
    # Preserve BetterModel's overlays, filters and other metadata sections, while
    # using the sword pack's Minecraft version declaration. Never leave an older
    # supported_formats/pack_format mixed with the new min_format/max_format.
    for key, value in sword_metadata.items():
        if key == "pack":
            continue
        if key in metadata and metadata[key] != value:
            raise ValueError(f"Both packs have different {key!r} metadata; merge it manually.")
        metadata[key] = value
    pack = metadata["pack"]
    for key in ("pack_format", "supported_formats", "min_format", "max_format", "description"):
        pack.pop(key, None)
    pack.update(sword_metadata["pack"])
    pack["description"] = "LegendarySwords + BetterModel — combined pack"
    entries["pack.mcmeta"] = (json.dumps(metadata, indent=2, ensure_ascii=False) + "\n").encode("utf-8")
    if "pack.png" in sword_entries:
        entries["pack.png"] = sword_entries["pack.png"]
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, "w", compression=ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries.items()):
            info = ZipInfo(name, STAMP)
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info, data, compress_type=ZIP_DEFLATED, compresslevel=9)
    digest = hashlib.sha1(output.read_bytes()).hexdigest()
    output.with_suffix(".sha1").write_text(digest + "\n", encoding="ascii")
    return digest, len(item_definitions)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bettermodel", type=Path, help="Fresh build.zip generated after installing the new models")
    parser.add_argument("swords", type=Path, help="LegendarySwords-resource-pack.zip from the same plugin release")
    parser.add_argument("output", type=Path, help="New combined resource-pack ZIP")
    args = parser.parse_args()
    try:
        digest, count = merge_packs(args.bettermodel, args.swords, args.output)
    except (OSError, ValueError, BadZipFile) as error:
        parser.exit(1, str(error) + "\n")
    print("Combined pack:", args.output)
    print("Sword item definitions:", count)
    print("SHA-1:", digest)
    print("This preserves the generated BetterModel assets; build.zip must come from the updated models.")


if __name__ == "__main__":
    main()
