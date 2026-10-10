#!/usr/bin/env python3
"""Package the built plugin, resource pack, and source for installation on Windows."""
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
import hashlib
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "target"
STAMP = (2026, 1, 1, 0, 0, 0)


def write_zip(path, entries):
    with ZipFile(path, "w", compression=ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries):
            info = ZipInfo(name, STAMP)
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info, data, compress_type=ZIP_DEFLATED, compresslevel=9)


def main():
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    version = ET.parse(ROOT / "pom.xml").findtext("m:version", namespaces=ns)
    jar = TARGET / f"LegendarySwords-{version}.jar"
    pack = TARGET / "LegendarySwords-resource-pack.zip"
    if not jar.is_file() or not pack.is_file():
        raise SystemExit("Run mvn verify before creating the download bundle.")
    with ZipFile(pack) as archive:
        if "pack.mcmeta" not in archive.namelist():
            raise SystemExit("Resource pack metadata must be at the ZIP root.")
        if archive.testzip() is not None:
            raise SystemExit("Resource pack ZIP checksum validation failed.")
        sword_ids = sorted(Path(name).stem for name in archive.namelist()
                           if name.startswith("assets/legendaryswords/items/") and name.endswith(".json")
                           and not Path(name).stem.endswith("_animated"))
        for sword in sword_ids:
            if f"assets/legendaryswords/items/{sword}_animated.json" not in archive.namelist():
                raise SystemExit("Resource pack is missing the animated inventory-safe item variant for " + sword)
    if not sword_ids:
        raise SystemExit("Resource pack must contain LegendarySwords item definitions.")
    with ZipFile(jar) as archive:
        required = ["plugin.yml", "config.yml", "io/github/legendaryswords/LegendarySwordsPlugin.class", "resource-pack.zip"]
        for name in required:
            if name not in archive.namelist():
                raise SystemExit("Plugin JAR is missing " + name)
        if archive.read("resource-pack.zip") != pack.read_bytes():
            raise SystemExit("Bundled and downloadable resource packs differ; rebuild with mvn verify.")
        models = {name for name in archive.namelist() if name.startswith("bettermodel/") and name.endswith(".bbmodel")}
        expected_models = {f"bettermodel/players/legendaryswords_{sword}.bbmodel" for sword in sword_ids}
        expected_models.update(f"bettermodel/models/legendaryswords_{sword}_slash.bbmodel" for sword in sword_ids)
        if models != expected_models:
            missing = sorted(expected_models - models)
            unexpected = sorted(models - expected_models)
            raise SystemExit(f"BetterModel assets must match the {len(sword_ids)} sword IDs. Missing: {missing}; unexpected: {unexpected}")
        model_entries = [(name.removeprefix("bettermodel/"), archive.read(name))
                         for name in archive.namelist()
                         if name.startswith("bettermodel/") and not name.endswith("/")]
        if archive.testzip() is not None:
            raise SystemExit("Plugin JAR checksum validation failed.")
    models_zip = TARGET / "LegendarySwords-BetterModel-models.zip"
    write_zip(models_zip, model_entries)
    sha1 = hashlib.sha1(pack.read_bytes()).hexdigest()
    digest = TARGET / "LegendarySwords-resource-pack.sha1"
    digest.write_text(sha1 + "\n", encoding="ascii")
    source_entries = []
    for name in (".gitignore", "README.md", "pom.xml", "docs", "src", "resource-pack", "scripts"):
        path = ROOT / name
        paths = [path] if path.is_file() else sorted(path.rglob("*"))
        for file in paths:
            if file.is_file() and "__pycache__" not in file.parts and file.suffix != ".pyc":
                source_entries.append((file.relative_to(ROOT).as_posix(), file.read_bytes()))
    source = TARGET / "LegendarySwords-source.zip"
    write_zip(source, source_entries)
    combined = TARGET / f"LegendarySwords-combined-resource-pack-{version}.zip"
    combined_entries = []
    combined_note = "Download the matching combined pack from the link below."
    if combined.is_file():
        with ZipFile(combined) as merged, ZipFile(pack) as swords:
            if merged.testzip() is not None:
                raise SystemExit("Combined resource pack ZIP checksum validation failed.")
            for name in swords.namelist():
                if name.startswith("assets/") and not name.endswith("/") and merged.read(name) != swords.read(name):
                    raise SystemExit("Combined pack contains outdated sword assets; merge again: " + name)
        combined_digest = hashlib.sha1(combined.read_bytes()).hexdigest()
        combined_entries = [(combined.name, combined.read_bytes()),
                            (combined.with_suffix(".sha1").name, (combined_digest + "\n").encode("ascii"))]
        combined_note = "The matching combined pack is included in this bundle."
    instructions = f"""LegendarySwords {version} — Paper 1.21.11

Stop the server, remove the previous LegendarySwords JAR from plugins, upload
{jar.name} directly into plugins and restart. Keep only one plugin JAR.
Keep your existing configuration and player data. BetterModel 3.1 requires Java
25 on the server; uploading this compiled JAR needs no JDK on your computer.

Character animations play ONLY during abilities and ultimates. Standing,
walking, running, ordinary swings and manual poses use normal player movement.
bettermodel.abilities-only and bettermodel.cast-body-animations default to true,
including old configs. The old player-body-animations false does not disable
new temporary casts. Cast bodies restore after their clip/scene ends.

Ultimates use a shared charge/reveal scene, then release the attack and damage
once. The sword appears ahead of the caster; nearby visible players within
40 blocks receive the same title and see world effects. The final caster pose
holds until scene completion. Windup defaults to 1.2 seconds; configure
cinematics.windup-ticks (24 ticks). Damage respects normal Paper armor rules.
There are no forced cameras, teleports or changes
to game mode. Use another player to inspect body animations; the caster keeps normal rendering and inventory.
A timed fallback releases a stalled visual scene once. Death, teleport, world
change or shutdown disarms pending attacks.

Run /ls cinematics on and /ls cinematics status. Server cinematics.enabled must
be true. Use /ls give all, /ls charge 100 and /ls resetcooldowns to test.
Right-click = ability. Sneak + right-click = charged ultimate.
Player-target damage still needs pvp-enabled true and protection permission.

Temporary cast bodies still use BetterModel's equipment path. The item-copy and
server-thread snapshot fixes remain, but the prior disconnect has not been
validated on a live client. If it recurs, set bettermodel.cast-body-animations
false and restart; shared scenes and attack effects remain without disguises.

Reuse LegendarySwords-combined-resource-pack-0.2.2.zip if already installed.
No model/texture/clip/client asset changes are included in {version}.
{combined_note}
A combined ZIP named {version} in this bundle is byte-identical to the 0.2.2 ZIP.
For first installation or an older pack, download the combined pack from:
https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.2.2.zip
Keep it zipped in Minecraft Options > Resource Packs > Open Pack Folder.
The standalone sword pack does not contain BetterModel's generated rig/shaders.

The JAR installs {len(expected_models)} BetterModel models automatically, updating known
originals with backups and preserving edited files. For manual installation,
extract LegendarySwords-BetterModel-models.zip; copy players files into
plugins/BetterModel/players and models files into plugins/BetterModel/models.
Back up edits first, then /bm reload. Generate a new BetterModel build.zip and
merge it only if other server models/generated pack settings changed.

To put the project into your GitHub repository, extract LegendarySwords-source.zip
and copy its CONTENTS into GitHub Desktop's Show in Explorer folder. Keep .git.
Commit and push. Building source requires JDK 25+ and Maven 3.9+; run mvn verify.
See README.md and docs/TESTING.md for the complete server checklist.

Compilation and automated checks were run. No live-client test was performed.
Resource-pack SHA-1: {sha1}
"""

    bundle = TARGET / "LegendarySwords-download.zip"
    write_zip(bundle, [(jar.name, jar.read_bytes()), (pack.name, pack.read_bytes()),
                       (digest.name, digest.read_bytes()), (source.name, source.read_bytes()),
                       (models_zip.name, models_zip.read_bytes()),
                       ("INSTALL-FIRST.txt", instructions.encode("utf-8"))] + combined_entries)
    print("Source:", source)
    print("Download:", bundle)
    print("BetterModel models:", models_zip)
    print("Resource-pack SHA-1:", sha1)
    print("Bundle SHA-256:", hashlib.sha256(bundle.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
