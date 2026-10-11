#!/usr/bin/env python3
"""Overlay a fresh BetterModel build on the user's existing combined pack."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import json,hashlib,sys

def merge(build,previous,output):
 with ZipFile(previous) as z:
  assert z.testzip() is None;old={n:z.read(n) for n in z.namelist() if not n.endswith('/')}
 with ZipFile(build) as z:
  assert z.testzip() is None;new={n:z.read(n) for n in z.namelist() if not n.endswith('/')}
 for n in [*old,*new]:
  assert not n.startswith('/') and '..' not in Path(n).parts
 merged=old.copy();merged.update(new)
 merged['pack.mcmeta']=json.dumps({'pack':{'description':'LegendaryArmor + LegendarySwords + BetterModel + Backrooms Bacteria','min_format':[75,0],'max_format':[75,0]}},indent=2).encode()
 # Keep user armor and sword artwork byte-for-byte.
 protected=[n for n in old if n.startswith(('assets/legendaryarmor/','assets/legendaryswords/'))]
 for n in protected:assert merged[n]==old[n]
 bacteria=[n for n in merged if 'backroomshorror_bacteria' in n];assert bacteria
 assert 'assets/bettermodel/models/mesh_triangle_duplex.json' in merged
 assert 'assets/bettermodel/textures/item/mesh_pixel.png' in merged
 missing=set()
 def visit(value):
  if isinstance(value,dict):
   for key,entry in value.items():
    if key=='model' and isinstance(entry,str) and entry.startswith('bettermodel:'):
     path='assets/bettermodel/models/'+entry.split(':',1)[1]+'.json'
     if path not in merged:missing.add(path)
    if key=='textures' and isinstance(entry,dict):
     for texture in entry.values():
      if isinstance(texture,str) and texture.startswith('bettermodel:'):
       path='assets/bettermodel/textures/'+texture.split(':',1)[1]+'.png'
       if path not in merged:missing.add(path)
    visit(entry)
  elif isinstance(value,list):
   for entry in value:visit(entry)
 for name,data in merged.items():
  if name.endswith('.json'):visit(json.loads(data))
 assert not missing,sorted(missing)
 output=Path(output)
 with ZipFile(output,'w',ZIP_DEFLATED) as z:
  for name,data in sorted(merged.items()):z.writestr(name,data)
 with ZipFile(output) as z:assert z.testzip() is None;assert z.read('pack.mcmeta')==merged['pack.mcmeta']
 sha=hashlib.sha1(output.read_bytes()).hexdigest();output.with_suffix('.sha1').write_text(sha+'\n')
 print('Verified',len(bacteria),'Bacteria assets;',len(protected),'unchanged armor/sword assets;',len(merged),'total files.')
 print('SHA-1:',sha)
if __name__=='__main__':merge(*sys.argv[1:])
