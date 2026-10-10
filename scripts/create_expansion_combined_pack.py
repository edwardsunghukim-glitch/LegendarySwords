#!/usr/bin/env python3
"""Combine the supplied snapshot pack, all sword items and official new effect exports.

Run mvn verify first: BetterModelPackExportTest exports all expansion effects
using the official model compiler. This preserves the prior named-alias snapshot
pack; a different server exporter configuration needs a fresh build.zip instead.
"""
import hashlib
import json
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]


def main():
    original = ROOT / 'downloads/LegendarySwords-combined-resource-pack-0.2.2.zip'
    swords = ROOT / 'target/LegendarySwords-resource-pack.zip'
    exported = ROOT / 'target/bettermodel-expansion-pack'
    with ZipFile(original) as archive:
        assert archive.testzip() is None
        entries = {name: archive.read(name) for name in archive.namelist()
                   if not name.endswith('/') and not name.startswith('assets/legendaryswords/')}
    with ZipFile(swords) as archive:
        assert archive.testzip() is None
        entries.update({name: archive.read(name) for name in archive.namelist() if not name.endswith('/')})
    aliases = list(exported.glob('assets/bettermodel/items/c/*.json'))
    weapons = json.loads((ROOT / 'scripts/expanded_weapons.json').read_text())
    expected = len(weapons) * 3
    if len(aliases) != expected:
        raise ValueError(f'Expected {expected} named slash aliases; run mvn clean verify first.')
    for path in exported.rglob('*'):
        if path.is_file():
            name, data = path.relative_to(exported).as_posix(), path.read_bytes()
            if name in entries and entries[name] != data:
                raise ValueError('Conflicting generated resource: ' + name)
            entries[name] = data
    # Resolve every new item/model/texture reference before packaging it.
    def check(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key == 'model' and isinstance(child, str) and child.startswith('bettermodel:'):
                    assert 'assets/bettermodel/models/' + child.split(':', 1)[1] + '.json' in entries, child
                check(child)
        elif isinstance(value, list):
            for child in value: check(child)
    for path in aliases:
        check(json.loads(path.read_text()))
    for path in exported.glob('assets/bettermodel/models/legendaryswords_expansion/*.json'):
        model = json.loads(path.read_text())
        for value in model.get('textures', {}).values():
            if value.startswith('bettermodel:'):
                assert 'assets/bettermodel/textures/' + value.split(':', 1)[1] + '.png' in entries, value
    version = ET.parse(ROOT / 'pom.xml').findtext('{http://maven.apache.org/POM/4.0.0}version')
    output = ROOT / f'target/LegendarySwords-combined-resource-pack-{version}.zip'
    with ZipFile(output, 'w', ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries.items()):
            info = ZipInfo(name, (2026, 1, 1, 0, 0, 0)); info.compress_type = ZIP_DEFLATED; info.external_attr = 0o644 << 16
            archive.writestr(info, data)
    digest = hashlib.sha1(output.read_bytes()).hexdigest()
    output.with_suffix('.sha1').write_text(digest + '\n')
    print('Combined pack:', output)
    print(f'{len(weapons)+8} sword items; {expected} named slash aliases; SHA-1:', digest)


if __name__ == '__main__':
    main()
