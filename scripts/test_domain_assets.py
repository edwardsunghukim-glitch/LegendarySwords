#!/usr/bin/env python3
"""Check second-collection meshes and texture topology, not just recolored PNG hashes."""
import json
from pathlib import Path
import struct
import unittest
import zlib

ROOT=Path(__file__).resolve().parents[1]
WEAPONS=json.loads((ROOT/'scripts/expanded_weapons.json').read_text())
ORIGINALS=[{'id':name} for name in ['master_sword','flameblade','fierce_deity_sword','frostblade','thunderblade','royal_claymore','eightfold_blade','biggoron_sword']]
ASSETS=ROOT/'resource-pack/assets/legendaryswords'


def frame_pixels(path):
    data=path.read_bytes();offset=8;compressed=b''
    while offset<len(data):
        size=struct.unpack('>I',data[offset:offset+4])[0];tag=data[offset+4:offset+8]
        if tag==b'IHDR': width,height,depth,color,*_=struct.unpack('>IIBBBBB',data[offset+8:offset+8+size]);assert (width,height,depth,color)==(128,512,8,6)
        if tag==b'IDAT': compressed+=data[offset+8:offset+8+size]
        offset+=12+size
    raw=zlib.decompress(compressed);rows=[]
    for y in range(32):
        start=y*(128*4+1);assert raw[start]==0
        rows.append([sum(raw[start+1+x*4:start+1+x*4+3]) for x in range(32)])
    return rows


class DomainAssetsTest(unittest.TestCase):
    def test_new_weapon_geometry_is_distinct_and_does_not_clone_previous_meshes(self):
        def shape(path):
            data=json.loads(path.read_text());return tuple((tuple(e['from']),tuple(e['to'])) for e in data['elements'])
        previous={shape(p) for p in (ASSETS/'models/item').glob('*.json') if not p.stem.endswith('_animated') and p.stem not in {w['id'] for w in WEAPONS[16:]}}
        new={shape(ASSETS/'models/item'/f'{w["id"]}.json') for w in WEAPONS[16:]}
        self.assertEqual(len(new),26);self.assertTrue(new.isdisjoint(previous))

    def test_all_expansion_blades_have_different_engraving_topology(self):
        signatures=set()
        for w in WEAPONS+ORIGINALS:
            rows=frame_pixels(ASSETS/'textures/item'/f'{w["id"]}.png')
            # A color-invariant contrast map detects carved/luminous boundaries.
            signature=tuple(abs(rows[y][x]-rows[y][x+1])>40 for y in range(32) for x in range(31))
            self.assertGreater(sum(signature),25,w['id']+' has no visible engraving')
            signatures.add(signature)
        self.assertEqual(len(signatures),len(WEAPONS)+len(ORIGINALS),'repeated pattern with only a tint change')

    def test_second_collection_has_no_disconnected_geometry(self):
        for weapon in WEAPONS[16:]:
            elements=json.loads((ASSETS/'models/item'/f'{weapon["id"]}.json').read_text())['elements']
            seen={0};pending=[0]
            while pending:
                a=elements[pending.pop()]
                for i,b in enumerate(elements):
                    if i not in seen and all(a['from'][axis]<=b['to'][axis]+.025 and b['from'][axis]<=a['to'][axis]+.025 for axis in range(3)):
                        seen.add(i);pending.append(i)
            self.assertEqual(len(seen),len(elements),weapon['id']+' has floating/disconnected pieces')

    def test_second_collection_effect_shapes_do_not_copy_existing_effect_models(self):
        new_ids={w['id'] for w in WEAPONS[16:]};old=set();new=set()
        for p in (ROOT/'src/main/resources/bettermodel/models').glob('*.bbmodel'):
            d=json.loads(p.read_text());shape=tuple((tuple(e['from']),tuple(e['to'])) for e in d['elements'])
            (new if p.stem.removeprefix('legendaryswords_').removesuffix('_slash') in new_ids else old).add(shape)
        self.assertEqual(len(new),26);self.assertTrue(new.isdisjoint(old))


if __name__=='__main__': unittest.main()
