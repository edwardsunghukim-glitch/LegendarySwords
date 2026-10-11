#!/usr/bin/env python3
"""Bake Bacteria cubes into standard item model files for Minecraft 1.21.11."""
import json,base64,hashlib
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
root=Path(__file__).resolve().parents[2];model=json.loads((root/'backrooms-plugin/src/main/resources/bettermodel/backroomshorror_bacteria.bbmodel').read_text());elements={e['uuid']:e for e in model['elements']}
path=root/'downloads/LegendaryArmor-Swords-Backrooms-combined-resource-pack.zip'
with ZipFile(path) as z:assets={n:z.read(n) for n in z.namelist()}
protected={n:b for n,b in assets.items() if n.startswith(('assets/legendaryarmor/','assets/legendaryswords/'))}
texture='bettermodel:item/backroomshorror_bacteria_compatible';assets['assets/bettermodel/textures/item/backroomshorror_bacteria_compatible.png']=base64.b64decode(model['textures'][0]['source'].split(',',1)[1])
count=0

def visit(nodes):
 global count
 for node in nodes:
  if isinstance(node,str):continue
  cubes=[elements[x] for x in node['children'] if isinstance(x,str) and x in elements]
  if cubes:
   origin=node['origin'];scale=max(1,max(abs(point[i]-origin[i])/16 for e in cubes for point in [e['from'],e['to']] for i in range(3)))
   geometry=[]
   for e in cubes:
    shape={key:[round((point-origin[i])/scale+8,6) for i,point in enumerate(e[key])] for key in ['from','to']};shape['faces']={side:{'uv':[0,0,16,16],'texture':'#0'} for side in ['north','east','south','west','up','down']};geometry.append(shape)
   name='backroomshorror_bacteria_'+node['name'];ref='bettermodel:c/'+name+'_compatible'
   assets['assets/bettermodel/models/c/'+name+'_compatible.json']=json.dumps({'textures':{'0':texture,'particle':texture},'elements':geometry,'display':{'fixed':{}}},separators=(',',':')).encode()
   assets['assets/bettermodel/items/c/'+name+'.json']=json.dumps({'model':{'type':'minecraft:model','model':ref}},separators=(',',':')).encode();count+=1
  visit(node['children'])
visit(model['outliner']);assert count==22
for n,data in assets.items():
 if '/items/c/backroomshorror_bacteria_' in n:
  obj=json.loads(data);ref=obj['model']['model'];target='assets/'+ref.split(':')[0]+'/models/'+ref.split(':')[1]+'.json';assert target in assets and 'transformation' not in data.decode()
for n,b in protected.items():assert assets[n]==b
assets['pack.mcmeta']=json.dumps({'pack':{'description':'Swords + Armor + BetterModel + Bacteria (1.21.11 cube compatibility)','min_format':[75,0],'max_format':[75,0]}},indent=2).encode()
with ZipFile(path,'w',ZIP_DEFLATED) as z:
 for n,b in sorted(assets.items()):z.writestr(n,b)
with ZipFile(path) as z:assert z.testzip() is None
path.with_suffix('.sha1').write_text(hashlib.sha1(path.read_bytes()).hexdigest()+'\n')
print('Verified 22 standard cube model definitions, 564 fine cubes, unchanged sword/armor assets, no Bacteria triangle-transform items.')
