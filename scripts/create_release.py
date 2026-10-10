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
                           if name.startswith("assets/legendaryswords/items/") and name.endswith(".json"))
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
    instructions = f"""LegendarySwords {version} — Paper 1.21.11

BetterModel 3.1 requires Java 25 on the SERVER. Core combat without BetterModel
can run on Java 21. Uploading this compiled release needs no JDK on your computer.
Building source yourself requires a Java 25+ JDK and Maven 3.9+.

If upgrading from 0.2.0 to the 0.2.1 animation fix, keep the combined texture
pack made from build (12).zip. This patch changes server animation keyframes
and carry poses without changing the client geometry or texture mappings.
Install the new JAR and restart; unedited bundled player files update automatically.
The pack installation/merging steps below apply to first installation or other
model changes, not solely to this animation patch under the same configuration.

1. Stop the server. Remove the old LegendarySwords JAR from plugins, then upload
   {jar.name} directly into plugins and start the server. Keep only one version.
2. Install {pack.name} in Minecraft's Resource Packs menu to see all {len(sword_ids)} swords.
3. BetterModel integration uses the 3.1 API. The plugin installs {len(expected_models)} models,
   upgrades its known original assets, and preserves files you edited in Blockbench.
   For manual installation, extract LegendarySwords-BetterModel-models.zip and
   upload its players files to plugins/BetterModel/players and models files to
   plugins/BetterModel/models. Back up your own edits before replacing any file.
4. Run /bm reload and download the NEW plugins/BetterModel/build.zip.
   A v0.1 combined pack is outdated. Combine that fresh build.zip with the new
   sword pack if your server supports one resource-pack URL. Full instructions
   and scripts/merge_resource_packs.py are in LegendarySwords-source.zip.
   The sword pack alone does not contain BetterModel's generated player/shader assets.
5. Join as an operator. Run /ls give all, /ls status, and /ls charge 100 to try an ultimate.
   /ls resetcooldowns clears your cooldowns when repeating an attack for testing.
6. Right click = ability. Sneak + right click = ultimate. /ls pose = sword pose.
   Walk or sprint while holding a sword to test its movement animations in F5.
   /ls cinematics toggles the in-place ultimate display; it does not teleport you.

To put this project in your GitHub repository:
Extract LegendarySwords-source.zip and copy its CONTENTS into the LegendarySwords
folder opened by GitHub Desktop's Show in Explorer. Keep your existing .git folder.
Then enter a summary, Commit to main, and Push origin (or Publish branch).
The source ZIP contains README.md and docs/ with full setup instructions.

Detailed models, animated energy textures, glint and particles work on a vanilla
client. Emissive overlays require supported client shaders/mods. BetterModel
animates third-person bodies; it cannot replace the vanilla first-person arm swing.

Compilation and automated checks were run. Client presentation, animations, and
multiplayer behavior still need testing on your actual Paper server.

Resource-pack SHA-1: {sha1}
"""
    bundle = TARGET / "LegendarySwords-download.zip"
    write_zip(bundle, [(jar.name, jar.read_bytes()), (pack.name, pack.read_bytes()),
                       (digest.name, digest.read_bytes()), (source.name, source.read_bytes()),
                       (models_zip.name, models_zip.read_bytes()),
                       ("INSTALL-FIRST.txt", instructions.encode("utf-8"))])
    print("Source:", source)
    print("Download:", bundle)
    print("BetterModel models:", models_zip)
    print("Resource-pack SHA-1:", sha1)
    print("Bundle SHA-256:", hashlib.sha256(bundle.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
