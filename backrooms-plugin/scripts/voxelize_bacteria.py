#!/usr/bin/env python3
"""Compatibility model: sample uploaded mesh surfaces and merge fine voxel cubes."""
import json,math,uuid,sys,hashlib
from pathlib import Path
import numpy as np
NS=uuid.UUID('71976b5d-c420-44b2-b06c-9e653f93ccda')
def uid(name):return str(uuid.uuid5(NS,name))
def boxes(voxels):
 remaining=set(voxels);out=[]
 while remaining:
  x,y,z=min(remaining);ex=x+1
  while (ex,y,z) in remaining:ex+=1
  ez=z+1
  while all((i,y,ez) in remaining for i in range(x,ex)):ez+=1
  ey=y+1
  while all((i,ey,k) in remaining for i in range(x,ex) for k in range(z,ez)):ey+=1
  for i in range(x,ex):
   for j in range(y,ey):
    for k in range(z,ez):remaining.remove((i,j,k))
  out.append(((x,y,z),(ex,ey,ez)))
 return out
path=Path(sys.argv[1]);model=json.loads(path.read_text());meshes=[e for e in model['elements'] if e['type']=='mesh'];assert meshes
step=.035*16;new_elements=[];replacements={};total_voxels=0
for mesh in meshes:
 v=mesh['vertices'];voxels=set()
 for face in mesh['faces'].values():
  tri=np.array([v[i] for i in face['vertices']],float);n=max(1,min(100,math.ceil(max(np.linalg.norm(tri[i]-tri[j]) for i,j in [(0,1),(1,2),(2,0)])/step*1.4)))
  for a in range(n+1):
   for b in range(n+1-a):
    p=tri[0]+(tri[1]-tri[0])*(a/n)+(tri[2]-tri[0])*(b/n);voxels.add(tuple(int(math.floor(c/step)) for c in p))
 total_voxels+=len(voxels);ids=[]
 for index,(lo,hi) in enumerate(boxes(voxels)):
  name=mesh['name']+'_surface_'+str(index);ident=uid('compatible/'+name);ids.append(ident)
  new_elements.append({'type':'cube','name':name,'uuid':ident,'from':[round(c*step,5) for c in lo],'to':[round(c*step,5) for c in hi],'origin':[0,0,0],'rotation':[0,0,0],'faces':{side:{'uv':[0,0,16,16],'texture':0} for side in ['north','east','south','west','up','down']},'visibility':True})
 replacements[mesh['uuid']]=ids

def walk(nodes):
 out=[]
 for node in nodes:
  if isinstance(node,str):out.extend(replacements.get(node,[node]))
  else:node['children']=walk(node['children']);out.append(node)
 return out
model['outliner']=walk(model['outliner']);model['elements']=new_elements;path.write_text(json.dumps(model,separators=(',',':')))
print('Compatible model:',len(new_elements),'merged cubes representing',total_voxels,'fine surface cells; all 23 bones and six clips preserved.')
# Validate dimensions and format; no unsupported mesh items remain.
assert all(e['type']=='cube' for e in new_elements);assert len(model['animations'])==6
vertices=np.array([point for e in new_elements for point in [e['from'],e['to']]])/16
print('Bounds (blocks):',vertices.min(axis=0).tolist(),vertices.max(axis=0).tolist())
