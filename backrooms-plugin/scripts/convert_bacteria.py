#!/usr/bin/env python3
"""Convert the user-supplied skinned GLB geometry to a BetterModel mesh rig."""
import json,struct,uuid,base64,io,math,sys
from pathlib import Path
import numpy as np
from PIL import Image
NS=uuid.UUID('71976b5d-c420-44b2-b06c-9e653f93ccda')
def uid(name):return str(uuid.uuid5(NS,name))
def quat(q):
 x,y,z,w=q
 return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w),0],[2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w),0],[2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y),0],[0,0,0,1]])
def convert(path):
 raw=Path(path).read_bytes();length,kind=struct.unpack_from('<II',raw,12);d=json.loads(raw[20:20+length]);start=20+length;blen,bkind=struct.unpack_from('<II',raw,start);binary=raw[start+8:start+8+blen]
 def accessor(index):
  a=d['accessors'][index];v=d['bufferViews'][a['bufferView']];types={5121:'u1',5123:'<u2',5125:'<u4',5126:'<f4'};counts={'SCALAR':1,'VEC3':3,'VEC4':4,'MAT4':16};dtype=np.dtype(types[a['componentType']]);n=counts[a['type']];offset=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',dtype.itemsize*n)
  return np.ndarray((a['count'],n),dtype=dtype,buffer=binary,offset=offset,strides=(stride,dtype.itemsize)).copy()
 nodes=d['nodes'];parents={child:i for i,node in enumerate(nodes) for child in node.get('children',[])};world={}
 def matrix(i):
  if i in world:return world[i]
  node=nodes[i]
  if 'matrix'in node:m=np.array(node['matrix']).reshape(4,4).T
  else:m=quat(node.get('rotation',[0,0,0,1]));m[:3,:3]=m[:3,:3]@np.diag(node.get('scale',[1,1,1]));m[:3,3]=node.get('translation',[0,0,0])
  world[i]=matrix(parents[i])@m if i in parents else m;return world[i]
 mesh_node=next(i for i,n in enumerate(nodes) if 'mesh'in n);p=d['meshes'][nodes[mesh_node]['mesh']]['primitives'][0];pos=accessor(p['attributes']['POSITION']).astype(float);indices=accessor(p['indices']).ravel().reshape(-1,3);joints=accessor(p['attributes']['JOINTS_0']);weights=accessor(p['attributes']['WEIGHTS_0']);skin=d['skins'][nodes[mesh_node]['skin']];joint_nodes=skin['joints'];inv=accessor(skin['inverseBindMatrices']).reshape(-1,4,4).transpose(0,2,1)
 # Preserve the uploaded bind-pose mesh, including the current rest-pose skeleton.
 hom=np.c_[pos,np.ones(len(pos))];positions=np.zeros((len(pos),4))
 for influence in range(4):
  for joint in range(len(joint_nodes)):
   mask=joints[:,influence]==joint
   if mask.any():positions[mask]+=(hom[mask]@(matrix(joint_nodes[joint])@inv[joint]).T)*weights[mask,influence,None]
 positions=positions[:,:3];lo=positions.min(axis=0);hi=positions.max(axis=0);scale=3.2/(hi[1]-lo[1]);offset=np.array([(lo[0]+hi[0])/2,lo[1],(lo[2]+hi[2])/2]);vertices=(positions-offset)*scale*16
 names={i:nodes[i]['name'].lower().replace('.','_') for i in joint_nodes};origins={i:((matrix(i)[:3,3]-offset)*scale*16).tolist() for i in joint_nodes}
 face_groups={i:[] for i in joint_nodes}
 for triangle in indices:
  score=np.zeros(len(joint_nodes))
  for vertex in triangle:
   for j,w in zip(joints[vertex],weights[vertex]):score[int(j)]+=float(w)
  face_groups[joint_nodes[int(score.argmax())]].append(triangle)
 elements=[];groups=[];outlines={}
 for joint in joint_nodes:
  name=names[joint];group={'name':name,'uuid':uid('bone/'+name),'origin':origins[joint],'rotation':[0,0,0],'children':[]};groups.append({k:v for k,v in group.items() if k!='children'});outlines[joint]=group
  faces=face_groups[joint]
  if faces:
   used=sorted(set(int(v) for face in faces for v in face));mesh={'type':'mesh','name':name+'_mesh','uuid':uid('mesh/'+name),'origin':[0,0,0],'rotation':[0,0,0],'vertices':{str(i):[round(float(v),6) for v in vertices[i]] for i in used},'faces':{str(f):{'vertices':[str(int(v)) for v in face],'uv':{str(int(v)):[8,8] for v in face},'texture':0} for f,face in enumerate(faces)},'visibility':True};elements.append(mesh);group['children'].append(mesh['uuid'])
 roots=[]
 for joint in joint_nodes:
  parent=parents.get(joint)
  while parent is not None and parent not in outlines:parent=parents.get(parent)
  if parent in outlines:outlines[parent]['children'].append(outlines[joint])
  else:roots.append(outlines[joint])
 def clip(name,length,loop):
  animators={}
  for joint in joint_nodes:
   bone=names[joint];frames=[]
   for k in range(9):
    t=k/8;angle=math.sin(t*math.pi*2);v=[0,0,0]
    if name=='idle' and 'spine' in bone:v=[angle*1.5,0,angle*.7]
    if name in ('walk','run'):
     power=12 if name=='walk' else 22;side=1 if bone.endswith('_l') else -1
     if 'thigh' in bone or 'upper_arm' in bone:v=[angle*power*side,0,angle*3]
     elif 'shin' in bone or 'forearm' in bone:v=[max(0,angle*side)*power*.7,0,0]
     elif bone=='spine':v=[angle*2,0,angle*2]
    if name=='charge' and bone=='spine':v=[-math.sin(t*math.pi)*18,0,0]
    if name=='scream' and 'spine' in bone:v=[math.sin(t*math.pi)*-4,angle*2,0]
    if name=='claw' and ('upper_arm' in bone or 'forearm' in bone):v=[-math.sin(t*math.pi)*40,0,math.sin(t*math.pi)*(12 if bone.endswith('_l') else -12)]
    frames.append({'channel':'rotation','data_points':[dict(zip(['x','y','z'],v))],'uuid':uid(f'{name}/{bone}/{k}'),'time':round(t*length,5),'interpolation':'linear'})
   animators[uid('bone/'+bone)]={'name':bone,'type':'bone','keyframes':frames}
  return {'uuid':uid('animation/'+name),'name':name,'loop':loop,'length':length,'animators':animators}
 image=Image.new('RGBA',(16,16),(12,12,10,255));buf=io.BytesIO();image.save(buf,format='PNG')
 model={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':'backroomshorror_bacteria','resolution':{'width':16,'height':16},'elements':elements,'groups':groups,'outliner':roots,'textures':[{'name':'bacteria_material.png','id':'0','uuid':uid('texture'),'width':16,'height':16,'uv_width':16,'uv_height':16,'source':'data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()}],'animations':[clip('idle',2,'loop'),clip('walk',1.1,'loop'),clip('run',.65,'loop'),clip('charge',.5,'once'),clip('scream',1,'once'),clip('claw',.65,'once')]}
 out=Path(__file__).resolve().parents[1]/'src/main/resources/bettermodel/backroomshorror_bacteria.bbmodel';out.write_text(json.dumps(model,separators=(',',':')))
 assert sum(len(e['faces']) for e in elements)==len(indices);assert len(groups)==23;assert abs((vertices.max(axis=0)[1]-vertices.min(axis=0)[1])/16-3.2)<1e-5
 print('Converted',len(indices),'triangles across',len(groups),'rig bones; height 3.2 blocks; added six animations.')
 # Render the actual converted mesh for visual inspection.
 import matplotlib;matplotlib.use('Agg')
 import matplotlib.pyplot as plt
 from mpl_toolkits.mplot3d.art3d import Poly3DCollection
 from matplotlib.collections import PolyCollection
 fig,ax=plt.subplots(figsize=(6,8),facecolor='#d4d0c2');ax.set_facecolor('#d4d0c2');verts=vertices/16
 triangles=verts[indices];normals=np.cross(triangles[:,1]-triangles[:,0],triangles[:,2]-triangles[:,0]);lengths=np.linalg.norm(normals,axis=1);normals/=np.maximum(lengths[:,None],1e-12)
 light=np.array([-.3,.6,.75]);light/=np.linalg.norm(light);shade=np.maximum(.07,np.abs(normals@light)*.13);colors=np.c_[shade,shade,shade,np.ones(len(shade))]
 ax.add_collection(PolyCollection(triangles[:,:,[0,1]],facecolors=colors,edgecolors='none'));ax.set_xlim(-1.65,1.65);ax.set_ylim(-.1,3.5);ax.set_aspect('equal');ax.set_axis_off();ax.set_title('Uploaded Lifeform — converted Bacteria',color='#222222');fig.savefig(out.parents[4]/'bacteria-preview.png',dpi=140,bbox_inches='tight');plt.close(fig)

if __name__=='__main__':convert(sys.argv[1])
