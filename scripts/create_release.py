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
    with ZipFile(jar) as archive:
        required = ["plugin.yml", "config.yml", "io/github/legendaryswords/LegendarySwordsPlugin.class", "resource-pack.zip"]
        for name in required:
            if name not in archive.namelist():
                raise SystemExit("Plugin JAR is missing " + name)
        if archive.read("resource-pack.zip") != pack.read_bytes():
            raise SystemExit("Bundled and downloadable resource packs differ; rebuild with mvn verify.")
        models = [name for name in archive.namelist() if name.startswith("bettermodel/") and name.endswith(".bbmodel")]
        if len(models) != 6:
            raise SystemExit("Plugin JAR must contain all six BetterModel animation files.")
    with ZipFile(pack) as archive:
        if "pack.mcmeta" not in archive.namelist():
            raise SystemExit("Resource pack metadata must be at the ZIP root.")
        if archive.testzip() is not None:
            raise SystemExit("Resource pack ZIP checksum validation failed.")
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
    instructions = f"""LegendarySwords {version} — Paper 1.21.11 / Java 21

1. Put {jar.name} in your server's plugins folder, then restart.
2. Install {pack.name} in Minecraft's Resource Packs menu for a quick test.
3. BetterModel 1.15.2 will receive six animation files automatically. Run /bm reload,
   and also distribute its generated plugins/BetterModel/build.zip to players.
   The sword pack alone does not contain BetterModel's generated player/shader assets.
4. Join as an operator. Run /ls give all, and /ls charge 100 to try an ultimate.
5. Right click = ability. Sneak + right click = ultimate. /ls cinematics toggles the camera.

To put this project in your GitHub repository:
Extract LegendarySwords-source.zip and copy its CONTENTS into the LegendarySwords
folder opened by GitHub Desktop's Show in Explorer. Keep your existing .git folder.
Then enter a summary, Commit to main, and Push origin (or Publish branch).
The source ZIP contains README.md and docs/ with full setup instructions.

Detailed models, animated energy textures, glint and particles work on a vanilla
client. Emissive overlays require supported client shaders/mods. BetterModel
animates third-person bodies; it cannot replace the vanilla first-person arm swing.

Compilation and automated checks were run. Client presentation, cameras, and
multiplayer behavior still need testing on your actual Paper server.

Resource-pack SHA-1: {sha1}
"""
    bundle = TARGET / "LegendarySwords-download.zip"
    write_zip(bundle, [(jar.name, jar.read_bytes()), (pack.name, pack.read_bytes()),
                       (digest.name, digest.read_bytes()), (source.name, source.read_bytes()),
                       ("INSTALL-FIRST.txt", instructions.encode("utf-8"))])
    print("Source:", source)
    print("Download:", bundle)
    print("Resource-pack SHA-1:", sha1)
    print("Bundle SHA-256:", hashlib.sha256(bundle.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
