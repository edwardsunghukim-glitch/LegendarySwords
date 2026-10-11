#!/usr/bin/env python3
"""Validate actual equipment/model references and the preserved combined pack."""
from pathlib import Path
import unittest,json,hashlib,zipfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'armor-resource-pack/assets/legendaryarmor'
SETS=['angel','dragon','leviathan','stone','frost']
class ArmorPackTest(unittest.TestCase):
 def test_equipment_layers_resolve_and_have_correct_aspect(self):
  for name in SETS:
   data=json.loads((ASSETS/'equipment'/f'{name}.json').read_text())
   for layer in ['humanoid','humanoid_leggings']:
    texture=data['layers'][layer][0]['texture'].split(':')[1]
    with Image.open(ASSETS/'textures/entity/equipment'/layer/(texture+'.png')) as image:
     self.assertEqual(image.size,(256,128));self.assertEqual(image.mode,'RGBA');self.assertIsNotNone(image.getbbox())
 def test_all_armor_atlases_are_distinct(self):
  digests={hashlib.sha256((ASSETS/'textures/entity/equipment/humanoid'/f'{name}.png').read_bytes()).digest() for name in SETS}
  self.assertEqual(len(digests),5)
 def test_item_definitions_resolve_models_and_textures(self):
  items=list((ASSETS/'items').glob('*.json'));self.assertEqual(len(items),35)
  for item in items:
   ref=json.loads(item.read_text())['model']['model'].split(':')[1];model=json.loads((ASSETS/'models'/(ref+'.json')).read_text())
   for texture in model['textures'].values():
    self.assertTrue((ASSETS/'textures'/(texture.split(':')[1]+'.png')).is_file(),texture)
   for element in model.get('elements',[]):
    self.assertTrue(all(-16<=v<=32 for v in element['from']+element['to']))
    self.assertTrue(all(a<b for a,b in zip(element['from'],element['to'])))
    if 'rotation' in element:self.assertIn(element['rotation']['angle'],[-45,-22.5,0,22.5,45])
 def test_accessories_and_attack_models_are_different(self):
  paths=list((ASSETS/'models/item').glob('*_back.json'));self.assertEqual(len(paths),5)
  self.assertEqual(len({json.dumps(json.loads(p.read_text())['elements'],sort_keys=True) for p in paths}),5)
  for name in ['sunburst','dragon_flame','tidal_wave','stone_spike','frost_crystal']:
   self.assertGreater(len(json.loads((ASSETS/'models/item'/f'{name}.json').read_text())['elements']),3)
 def test_combined_pack_preserves_all_existing_sword_and_bettermodel_assets(self):
  with zipfile.ZipFile(ROOT/'downloads/LegendarySwords-combined-resource-pack-0.5.3.zip') as old,zipfile.ZipFile(ROOT/'downloads/LegendaryArmor-Swords-combined-resource-pack.zip') as combined:
   for name in old.namelist():
    if name!='pack.mcmeta':self.assertEqual(old.read(name),combined.read(name),name)
   self.assertIn('assets/legendaryarmor/equipment/angel.json',combined.namelist())
   self.assertEqual(json.loads(combined.read('pack.mcmeta'))['pack']['min_format'],[75,0])
 def test_zip_root_and_checksum(self):
  for name in ['LegendaryArmor-resource-pack','LegendaryArmor-Swords-combined-resource-pack']:
   file=ROOT/'downloads'/(name+'.zip')
   self.assertEqual(hashlib.sha1(file.read_bytes()).hexdigest(),file.with_suffix('.sha1').read_text().strip())
   with zipfile.ZipFile(file) as pack:self.assertIn('pack.mcmeta',pack.namelist());self.assertIsNone(pack.testzip());self.assertEqual(len(pack.namelist()),len(set(pack.namelist())))
if __name__=='__main__':unittest.main()
