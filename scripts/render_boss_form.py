"""Render an actual textured BetterModel boss pose with Blender (optional art review).
Usage: blender -b -t 4 --python scripts/render_boss_form.py -- input.bbmodel output.png [--player-scale]
The image uses authored geometry, embedded textures and the idle clip.
"""
import bpy,json,math,base64,sys
from pathlib import Path
from mathutils import Matrix,Vector,Euler
args=sys.argv[sys.argv.index('--')+1:];src=Path(args[0]);output=args[1]
d=json.loads(src.read_text());bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
groupdefs={g['uuid']:g for g in d.get('groups',[])};transforms={};phase=.45
clips={a['name']:a for a in d.get('animations',[])};clip=clips.get('idle',{})
def vals(k): return [float(k.get('data_points',[{}])[0].get(a,0)) for a in 'xyz']
def pose(ident,channel,default):
 keys=[k for k in clip.get('animators',{}).get(ident,{}).get('keyframes',[]) if k['channel']==channel]
 if not keys:return default
 keys.sort(key=lambda k:k['time']);t=phase*clip.get('length',1)
 left=keys[0];right=keys[-1]
 for k in keys:
  if k['time']<=t:left=k
  if k['time']>=t:right=k;break
 ratio=(t-left['time'])/(right['time']-left['time']) if right['time']!=left['time'] else 0
 return [a+(b-a)*ratio for a,b in zip(vals(left),vals(right))]
def walk(node,parent):
 if isinstance(node,str):transforms[node]=parent;return
 g=groupdefs.get(node['uuid'],node);origin=Vector(g.get('origin',[0,0,0]));angles=[a+b for a,b in zip(g.get('rotation',[0,0,0]),pose(node['uuid'],'rotation',[0,0,0]))]
 rot=Euler([math.radians(x) for x in angles],'XYZ').to_matrix().to_4x4();scale=Matrix.Diagonal(Vector(pose(node['uuid'],'scale',[1,1,1])+[1]))
 mat=parent@Matrix.Translation(origin+Vector(pose(node['uuid'],'position',[0,0,0])))@rot@scale@Matrix.Translation(-origin)
 for child in node.get('children',[]):walk(child,mat)
for node in d.get('outliner',[]):walk(node,Matrix.Identity(4))
materials=[]
for i,t in enumerate(d.get('textures',[])):
 png=Path('/tmp/bb-preview-'+str(i)+'.png');png.write_bytes(base64.b64decode(t['source'].split(',',1)[1]));im=bpy.data.images.load(str(png))
 mat=bpy.data.materials.new('atlas'+str(i));mat.use_nodes=True;bsdf=mat.node_tree.nodes.get('Principled BSDF');tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=im;tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color']);bsdf.inputs['Roughness'].default_value=.7;materials.append(mat)
faces={'north':[0,1,5,4],'south':[3,7,6,2],'west':[0,4,7,3],'east':[1,2,6,5],'down':[0,3,2,1],'up':[4,5,6,7]};allpoints=[]
for e in d['elements']:
 if e.get('type','cube')!='cube' or e.get('visibility',True)==False:continue
 a,b=e['from'],e['to'];pts=[(a[0],a[1],a[2]),(b[0],a[1],a[2]),(b[0],a[1],b[2]),(a[0],a[1],b[2]),(a[0],b[1],a[2]),(b[0],b[1],a[2]),(b[0],b[1],b[2]),(a[0],b[1],b[2])]
 origin=Vector(e.get('origin',[0,0,0]));rotation=Euler([math.radians(x) for x in e.get('rotation',[0,0,0])],'XYZ').to_matrix().to_4x4();mat=transforms.get(e['uuid'],Matrix.Identity(4))@Matrix.Translation(origin)@rotation@Matrix.Translation(-origin)
 pts=[mat@Vector(p) for p in pts];pts=[(p.x,p.z,p.y) for p in pts];allpoints+=pts
 selected=[(name,value) for name,value in e['faces'].items() if value.get('texture') is not None]
 mesh=bpy.data.meshes.new(e['uuid']);mesh.from_pydata(pts,[],[faces[name] for name,f in selected]);mesh.update();obj=bpy.data.objects.new(e.get('name','part'),mesh);bpy.context.collection.objects.link(obj)
 for material in materials:obj.data.materials.append(material)
 uv=mesh.uv_layers.new()
 for poly,(name,face) in zip(mesh.polygons,selected):
  idx=int(face.get('texture',0));poly.material_index=idx;texture=d['textures'][idx];w=texture.get('uv_width',d['resolution']['width']);h=texture.get('uv_height',d['resolution']['height']);u0,v0,u1,v1=face['uv'];coords=[(u0/w,1-v1/h),(u1/w,1-v1/h),(u1/w,1-v0/h),(u0/w,1-v0/h)]
  for loop,coord in zip(poly.loop_indices,coords):uv.data[loop].uv=coord
if '--player-scale' in args:
 # Neutral reference mannequin, 1.8 blocks = 28.8 Blockbench units tall.
 refx=max(p[0] for p in allpoints)+13;refy=min(p[1] for p in allpoints)
 reference=bpy.data.materials.new('player size reference');reference.diffuse_color=(.95,.73,.24,1)
 for center,dimensions in [((0,0,25.2),(7.2,7.2,7.2)),((0,0,16.2),(7.2,3.6,10.8)),
   ((-5.4,0,16.2),(3.6,3.6,10.8)),((5.4,0,16.2),(3.6,3.6,10.8)),
   ((-1.8,0,5.4),(3.6,3.6,10.8)),((1.8,0,5.4),(3.6,3.6,10.8))]:
  location=(refx+center[0],refy+center[1],center[2])
  bpy.ops.mesh.primitive_cube_add(size=1,location=location);obj=bpy.context.object;obj.name='1.8-block player scale';obj.dimensions=dimensions;obj.data.materials.append(reference)
  allpoints.extend([(location[0]+sx*dimensions[0]/2,location[1]+sy*dimensions[1]/2,location[2]+sz*dimensions[2]/2) for sx in (-1,1) for sy in (-1,1) for sz in (-1,1)])
lo=Vector([min(p[i] for p in allpoints) for i in range(3)]);hi=Vector([max(p[i] for p in allpoints) for i in range(3)]);center=(lo+hi)/2;size=(hi-lo).length
bpy.ops.object.camera_add(location=center+Vector((size*.75,-size*1.05,size*.55)));cam=bpy.context.object;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=max(hi.x-lo.x,hi.y-lo.y,hi.z-lo.z)*1.4;bpy.context.scene.camera=cam
bpy.context.view_layer.update()
# Fit projected bounds, including the scale mannequin, to the output aspect ratio.
view=cam.matrix_world.inverted();projected=[view@Vector(p) for p in allpoints]
px=[p.x for p in projected];py=[p.y for p in projected]
cam.location+=cam.rotation_euler.to_matrix()@Vector(((min(px)+max(px))/2,(min(py)+max(py))/2,0))
cam.data.ortho_scale=max(max(px)-min(px),(max(py)-min(py))*1000/700)*1.12
cam.data.clip_end=10000
for loc,power in [((.2,-.8,1),12000),((-.7,.2,.5),8000)]:
 bpy.ops.object.light_add(type='AREA',location=center+Vector(loc)*size);light=bpy.context.object;light.data.energy=power*(size/30)**2;light.data.shape='DISK';light.data.size=size*.8;light.rotation_euler=(center-light.location).to_track_quat('-Z','Y').to_euler()
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=False;scene.render.resolution_x=1000;scene.render.resolution_y=700;scene.render.resolution_percentage=100;scene.world.color=(.12,.12,.12);scene.render.image_settings.file_format='PNG';scene.render.filepath=output;scene.view_settings.view_transform='Standard';bpy.ops.render.render(write_still=True)
