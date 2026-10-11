#!/usr/bin/env python3
"""Original armor artwork, wearable equipment assets, accessories, and attack meshes.
Requires Pillow. No source/example creature artwork is reused.
"""
from pathlib import Path
import json,math,random,zipfile,hashlib
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
PACK=ROOT/'armor-resource-pack'
A=PACK/'assets/legendaryarmor'
THEMES={
 'angel':('Seraph Ascendant',(220,231,239),(201,159,65),(255,241,181)),
 'dragon':('Dreadfire Wyrm',(56,33,55),(167,93,58),(255,177,57)),
 'leviathan':('Abyssal Sovereign',(24,91,118),(49,153,169),(87,228,235)),
 'stone':('Runebound Colossus',(86,81,72),(124,116,97),(111,222,192)),
 'frost':('Winter Revenant',(92,136,173),(178,220,236),(160,244,255)),
}
def savejson(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2)+'\n')
def savepng(path,image):path.parent.mkdir(parents=True,exist_ok=True);image.save(path)
def mix(c,f):return tuple(max(0,min(255,int(v*f))) for v in c)
def face(image,rect,theme,kind):
 name,base,trim,glow=THEMES[theme];x0,y0,x1,y1=[int(v*4) for v in rect];draw=ImageDraw.Draw(image);w,h=x1-x0,y1-y0
 rng=random.Random(theme+str(rect)+kind)
 # Broad forged plates with a restrained brushed-metal highlight.
 for y in range(y0,y1):
  for x in range(x0,x1):
   u=(x-x0+.5)/w;v=(y-y0+.5)/h
   shade=.72+.48*math.exp(-((u-.28)/.25)**2)-.12*v+rng.uniform(-.035,.035)
   image.putpixel((x,y),mix(base,shade)+(255,))
 draw.rectangle((x0,y0,x1-1,y1-1),outline=mix(base,.45)+(255,),width=2)
 draw.line((x0+2,y1-3,x0+2,y0+2,x1-3,y0+2),fill=mix(base,1.65)+(255,),width=1)
 draw.line((x0+3,y1-3,x1-3,y1-3,x1-3,y0+3),fill=mix(base,.6)+(255,),width=1)
 if h>w:
  # Articulated plates use asymmetrical seams rather than wallpaper patterns.
  yy=y0+int(h*.68);draw.line((x0+3,yy,x1-4,yy-2),fill=mix(base,.4)+(255,),width=2)
  draw.line((x0+3,yy+2,x1-4,yy),fill=mix(base,1.35)+(255,),width=1)
 # Chipped corners, scoring and a few rusty rivets.
 for x,y in [(x0+3,y0+3),(x1-4,y0+3),(x0+3,y1-4),(x1-4,y1-4)]:
  draw.rectangle((x,y,x+1,y+1),fill=trim+(255,))
 for _ in range(max(1,w*h//180)):
  x=rng.randint(x0+3,x1-4);y=rng.randint(y0+3,y1-4);length=rng.randint(2,5)
  draw.line((x,y,min(x1-3,x+length),min(y1-3,y+2)),fill=mix(base,.38)+(255,),width=1)
  draw.point((x,y-1),fill=mix(base,1.6)+(255,))
 # Small set-specific emblems, not full-surface glowing patterns.
 if w>=20 and h>=24:
  cx=(x0+x1)//2;cy=y0+h//3
  if theme=='angel':
   draw.line((cx-4,cy-3,cx,cy,cx+4,cy-3),fill=trim+(255,),width=2);draw.line((cx,cy-2,cx,cy+5),fill=trim+(255,),width=1)
  elif theme=='dragon':draw.line((cx-3,cy-4,cx+1,cy-1,cx-1,cy+2,cx+3,cy+5),fill=trim+(255,),width=2)
  elif theme=='leviathan':draw.arc((cx-5,cy-4,cx+5,cy+4),10,160,fill=trim+(255,),width=2)
  elif theme=='stone':draw.rectangle((cx-3,cy-3,cx+3,cy+3),outline=trim+(255,),width=1)
  else:draw.line((cx-3,cy-3,cx+3,cy+3),fill=trim+(255,),width=1);draw.line((cx+3,cy-3,cx-3,cy+3),fill=trim+(255,),width=1)

def atlas(theme,leggings=False):
 image=Image.new('RGBA',(256,128))
 # All six cube faces for helmet/body/arm/leg, matching vanilla armor UVs.
 regions=[(8,0,16,8),(16,0,24,8),(0,8,8,16),(8,8,16,16),(16,8,24,16),(24,8,32,16),
 (20,16,28,20),(28,16,36,20),(16,20,20,32),(20,20,28,32),(28,20,32,32),(32,20,40,32),
 (44,16,48,20),(48,16,52,20),(40,20,44,32),(44,20,48,32),(48,20,52,32),(52,20,56,32),
 (4,16,8,20),(8,16,12,20),(0,20,4,32),(4,20,8,32),(8,20,12,32),(12,20,16,32)]
 for region in regions:face(image,region,theme,'leggings' if leggings else 'armor')
 draw=ImageDraw.Draw(image);_,base,trim,glow=THEMES[theme]
 # Heavy plate framing and articulated knee/gauntlet seams.
 for rect in [(81,81,110,126),(177,81,190,126),(17,81,30,126)]:
  x0,y0,x1,y1=rect
  draw.rectangle(rect,outline=trim+(255,),width=2)
  for yy in [y0+4,y1-5]:draw.line((x0+2,yy,x1-2,yy),fill=mix(base,1.4)+(255,),width=2)
 draw.rectangle((19,97,28,105),fill=mix(base,.8)+(255,),outline=trim+(255,),width=2)
 draw.rectangle((177,106,190,123),outline=mix(trim,1.3)+(255,),width=2)
 # Inset low-contrast green scale panels, surrounded by dark plate frames.
 greens={'angel':(153,177,201),'dragon':(108,48,67),'leviathan':(49,153,169),'stone':(49,47,46),'frost':(178,220,236)}
 green=greens[theme]
 for panel in [(85,84,106,114),(179,85,188,101),(20,84,28,95)]:
  x0,y0,x1,y1=panel;draw.rectangle(panel,fill=green+(255,))
  for yy in range(y0+2,y1-1,4):
   for xx in range(x0+2,x1-1,4):
    x=xx+(2 if ((yy-y0)//4)%2 else 0)
    if theme in ['dragon','leviathan']:draw.line((x-2,yy,x,yy+2,min(x1,x+2),yy),fill=mix(green,.65)+(255,),width=1)
    elif theme=='angel':draw.line((x-2,yy+2,x,yy,x+2,yy+2),fill=mix(green,.8)+(255,),width=1)
    elif theme=='stone':draw.line((x,yy,x-1,yy+2,x+1,yy+3),fill=mix(green,.6)+(255,),width=1)
    else:draw.line((x-2,yy,x,yy+3,x+2,yy),fill=mix(green,1.2)+(255,),width=1)
  draw.rectangle(panel,outline=mix(base,.5)+(255,),width=1)
 # Visor slits and a chest core; these differ in outline and placement by set.
 hx,hy=32,32
 draw.rectangle((hx+4,hy+13,hx+27,hy+19),fill=(11,17,24,255))
 if theme=='angel':draw.line((hx+5,hy+15,hx+26,hy+15),fill=glow+(255,),width=2)
 elif theme=='dragon':
  for x in [hx+7,hx+22]:draw.polygon([(x-3,hy+14),(x+3,hy+14),(x,hy+18)],fill=glow+(255,))
 elif theme=='leviathan':draw.line((hx+5,hy+17,hx+14,hy+14,hx+26,hy+17),fill=glow+(255,),width=2)
 elif theme=='stone':
  for x in [hx+8,hx+23]:draw.rectangle((x-2,hy+14,x+1,hy+17),fill=glow+(255,))
 else:
  draw.line((hx+4,hy+15,hx+27,hy+15),fill=glow+(255,),width=1)
 if leggings:
  draw.rectangle((80,81,111,88),fill=trim+(255,));draw.rectangle((93,81,98,88),outline=glow+(255,),width=1)
 else:
  cx,cy=96,93
  if theme=='angel':draw.polygon([(cx,cy-7),(cx+5,cy),(cx,cy+7),(cx-5,cy)],fill=trim+(255,),outline=mix(base,.5)+(255,))
  elif theme=='dragon':draw.polygon([(cx,cy-7),(cx+6,cy+4),(cx,cy+7),(cx-6,cy+4)],fill=glow+(255,))
  elif theme=='leviathan':draw.ellipse((cx-5,cy-5,cx+5,cy+5),fill=trim+(255,),outline=mix(base,.5)+(255,),width=2)
  elif theme=='stone':draw.rectangle((cx-5,cy-5,cx+5,cy+5),outline=glow+(255,),width=2)
  else:
   for angle in range(0,360,60):a=math.radians(angle);draw.line((cx,cy,cx+math.cos(a)*7,cy+math.sin(a)*7),fill=glow+(255,),width=2)
 return image

def cube(x,y,z,w,h,d,material='base',angle=0,axis='z'):
 element={'from':[x,y,z],'to':[x+w,y+h,z+d],'faces':{face:{'texture':'#'+material,'uv':[0,0,16,16]} for face in ['north','south','east','west','up','down']}}
 if angle:element['rotation']={'origin':[x+w/2,y+h/2,z+d/2],'axis':axis,'angle':angle,'rescale':False}
 return element
MODELS={}
KNIGHTPARTS={}
def model(name,theme,elements):
 MODELS[name]=elements
 savejson(A/'models/item'/f'{name}.json',{'ambientocclusion':False,'textures':{key:f'legendaryarmor:item/material/{theme}_{key}' for key in ['base','trim','glow','cloth','leather']},'elements':elements,'display':{'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}}})
 savejson(A/'items'/f'{name}.json',{'model':{'type':'minecraft:model','model':f'legendaryarmor:item/{name}'}})
def accessories(theme):
 back=[];crown=[]
 if theme=='angel':
  for side in [-1,1]:
   for feather in range(10):
    x=8+side*(3+feather*1.5);y=8+feather*.7
    back.append(cube(x-(1.4 if side<0 else 0),y-5,7,1.4,9-feather*.35,1,'base',-22.5*side))
    back.append(cube(x-(.25 if side<0 else 0),y-4,6.8,.25,6,.3,'trim',-22.5*side))
  for i in range(16):a=i*math.pi/8;crown.append(cube(8+math.cos(a)*5-.55,8,8+math.sin(a)*5-.55,1.1,.6,1.1,'glow'))
 elif theme=='dragon':
  for side in [-1,1]:
   for i in range(7):back.append(cube(8+side*(2+i*1.9)-(1.5 if side<0 else 0),6+i*.8,7,1.5,8-i*.7,.7,'trim',-22.5*side))
   for i in range(4):back.append(cube(8+side*(4+i*2.5)-1,6+i*1.3,6.7,2.5,2,.5,'base',-22.5*side))
   for i in range(4):crown.append(cube(8+side*(4+i*.6)-.5,8+i*1.2,8+i*.6,1.2,1.5,1.4,'trim' if i<3 else 'glow',22.5*side))
  for y in [5,8,11,14]:back.append(cube(7.5,y,6,1,1,2,'glow',22.5,'x'))
 elif theme=='leviathan':
  for side in [-1,1]:
   for i in range(8):back.append(cube(8+side*(2+i*1.35)-.6,5+i*.7,7,1.2,9-i*.7,.6,'base',-22.5*side))
   for i in range(4):crown.append(cube(8+side*(3+i*.7)-.4,7+i,8,.8,3,.6,'glow',-22.5*side))
  for i in range(8):back.append(cube(7.4,3+i*1.6,6.2,1.2,1,2.5,'trim',22.5,'x'))
 elif theme=='stone':
  back+=[cube(3,3,7,10,11,2,'base'),cube(5,5,6.4,6,7,.7,'trim'),cube(7,7,6,2,3,.5,'glow')]
  for side in [-1,1]:
   for i in range(3):back.append(cube(8+side*(7+i*1.5)-1.5,10-i,7,3,2.5,3,'base',22.5*side))
   crown.append(cube(8+side*5-1,7,7,2,4,2,'base'));crown.append(cube(8+side*5-.4,8,6.6,.8,1.8,.5,'glow'))
  crown.append(cube(3,7,7,10,1,2,'trim'))
 else:
  for side in [-1,1]:
   for i in range(6):back.append(cube(8+side*(3+i*1.7)-.7,6+i*.6,7,1.4,7-i*.6,1.2,'glow',-22.5*side))
   for i in range(3):crown.append(cube(8+side*(2+i*1.6)-.4,8,7.5,.8,3+i*.8,.8,'glow',22.5*side))
  back.append(cube(6,4,6.2,4,9,1,'base'));crown.append(cube(3,7.5,7,10,.8,2,'trim'))
 # True extra volume: layered shoulder shells, collar, breastplate and waist lames.
 bulky=[]
 for side in [-1,1]:
  x=-1 if side<0 else 12
  bulky += [cube(x,5.8,6.2,4.5,3.2,4.5,'base',22.5*side),cube(x-.2,5.2,6,4.9,.8,4.9,'trim',22.5*side),
            cube(x+.5,4.3,7,5,1.5,4.5,'base',22.5*side),cube(x+1,3.5,7.4,4,1,4,'trim',22.5*side)]
  # Raised shoulder insignia.
  bulky.append(cube(x+2,7,5.8,1.8,1.3,.5,'glow'))
 bulky += [cube(3,2,11,10,6,2.4,'base'),cube(3,7.3,10.8,10,.8,2.8,'trim'),
           cube(7.2,3.5,13.4,1.6,3.3,.5,'trim'),cube(7.5,5,14,1,1,.3,'glow'),
           cube(3,8,7,2,2.2,5,'base'),cube(11,8,7,2,2.2,5,'base')]
 for row in range(3):bulky.append(cube(3.3,-.5+row*.9,11.2,9.4,.8,2,'trim' if row==0 else 'base'))
 # Each order gets an individual chest silhouette and shoulder treatment.
 if theme=='angel':
  bulky += [cube(4,3.5,13.3,3,3,.5,'trim',-22.5),cube(9,3.5,13.3,3,3,.5,'trim',22.5)]
 elif theme=='dragon':
  for side in [-1,1]:bulky.append(cube(8+side*9-.6,9,7,.9,3,1.2,'trim',22.5*side))
 elif theme=='leviathan':
  for side in [-1,1]:bulky.append(cube(8+side*8-.5,8,7,1,3,3,'glow',22.5*side))
 elif theme=='stone':bulky += [cube(2,1.5,10.5,12,7,3,'base'),cube(6.5,4,13.6,3,3,.4,'glow')]
 else:
  for side in [-1,1]:bulky.append(cube(8+side*9-.5,8,7,1,3.5,1,'glow',22.5*side))
 # Cloth panels, strapped belt and overlapping armored tassets.
 bulky += [cube(5,-6,11,6,6.5,.6,'cloth'),cube(2.6,.3,11.5,10.8,1.2,2.3,'leather'),cube(7,.3,14,2,1.2,.4,'trim')]
 for side in [-1,1]:
  for row in range(3):bulky.append(cube(8+side*3-1.6,-3+row*1.2,12,3.2,1.1,1,'base',22.5*side))
 for row in range(4):bulky.append(cube(4.2+row*1.5,3+row*.8,14,1.9,.5,.35,'leather',22.5))
 # Green scale insets sit inside dark breastplate edges.
 bulky += [cube(4.2,3,14,2.7,4.2,.25,'cloth'),cube(9.1,3,14,2.7,4.2,.25,'cloth')]
 # Scarred knight silhouettes gain heavy shoulder spikes and smaller tip pieces.
 for side in [-1,1]:
  for index in range(3):
   x=8+side*(6.3+index*1.7)-.45
   bulky += [cube(x,8.8-index*.45,7,.9,2.8-index*.25,1,'base',22.5*side),
             cube(x+.2,11.1-index*.6,7.2,.5,1,.5,'trim',22.5*side)]
 # Inset rivets, raised filigree and distinct front plates.
 for side in [-1,1]:
  for row in range(3):bulky.append(cube(8+side*3.8-.15,3+row*1.4,13.6,.3,.3,.35,'trim'))
 if theme=='dragon':
  for side in [-1,1]:
   for row in range(3):bulky.append(cube(8+side*2.3-1,3+row*1.3,13.7,2,.6,.5,'trim',22.5*side))
 elif theme=='leviathan':
  for row in range(4):bulky.append(cube(4.4,3+row*1.1,13.7,7.2,.25,.4,'trim'))
 elif theme=='frost':
  for side in [-1,1]:bulky.append(cube(8+side*2.4-.3,3.2,13.7,.6,3.7,.4,'trim',22.5*side))
 elif theme=='stone':
  bulky += [cube(3,3,13.6,1,4.5,.5,'trim'),cube(12,3,13.6,1,4.5,.5,'trim')]
 KNIGHTPARTS[theme]=bulky
 back=bulky
 # Reinforced brow and temple guards remain above the first-person eye line.
 crown = [cube(2.7,4.8,3.5,10.6,1.4,1.7,'base'),cube(2.7,4.4,3.3,10.6,.5,1.9,'trim'),
           cube(2.5,4.8,5,1.8,2.1,6,'base'),cube(11.7,4.8,5,1.8,2.1,6,'base')]
 # Closed greathelm with separate cheek guards, visor slot and reinforced crest.
 helmet=[cube(2.5,3,3,1.5,10,10,'base'),cube(12,3,3,1.5,10,10,'base'),
         cube(4,3,3,8,10,1.2,'base'),cube(4,12,4,8,1.5,8,'base'),
         cube(4,3,11.8,8,4.6,1.4,'base'),cube(4,8.6,11.8,8,3.4,1.4,'base'),
         cube(7.5,7.2,12,.9,2,1.6,'trim'),cube(3.8,7.5,13,8.4,.3,.35,'trim'),
         cube(7.3,12.5,3,1.4,1.5,10,'trim')]
 for side in [-1,1]:
  for row in range(3):helmet.append(cube(8+side*2.2-.15,4+row*.8,13.3,.3,.3,.2,'leather'))
 if theme in ['dragon','leviathan']:
  for side in [-1,1]:
   for i in range(5):helmet.append(cube(8+side*(4.5+i*.65)-.6,12+i*1.5,6-i*.2,1.3-i*.13,2,1.4-i*.15,'base',22.5*side))
 elif theme=='stone':helmet.append(cube(6,13,5,4,3,4,'base'))
 elif theme=='frost':
  for side in [-1,1]:helmet.append(cube(8+side*4-.5,12,6,1,5,1,'trim',22.5*side))
 else:helmet.append(cube(7.4,13,5,1.2,4,5,'cloth'))
 model(theme+'_helmet_shell',theme,helmet)
 model(theme+'_back',theme,back);model(theme+'_crown',theme,crown)
def wings():
 for theme in ['angel','dragon','leviathan']:
  elements=[]
  for side in [-1,1]:
   if theme=='angel':
    for i in range(13):
     x=8+side*(3+i*1.4)-.6
     elements.append(cube(x,8+i*.5-4,7,1.2,8-i*.3,.7,'base',-22.5*side))
     elements.append(cube(x+.25,8+i*.5-3,6.8,.2,5-i*.2,.3,'trim',-22.5*side))
   else:
    for i in range(10):
     x=8+side*(3+i*1.8)-.8
     elements.append(cube(x,5+i*.65,7,1.6,7-i*.45,.45,'cloth',-22.5*side))
     if i%2==0:elements.append(cube(x+.3,5+i*.65,6.7,.5,8-i*.45,.8,'trim',-22.5*side))
  model(theme+'_wings',theme,elements)
def icons(theme):
 _,base,trim,glow=THEMES[theme]
 for name in ['boots','leggings','chestplate','helmet']:
  image=Image.new('RGBA',(32,32));d=ImageDraw.Draw(image)
  shapes={'helmet':[(5,5),(26,5),(28,20),(24,25),(19,25),(19,18),(12,18),(12,25),(7,25),(3,20)],
          'chestplate':[(9,4),(4,7),(2,16),(7,19),(9,15),(9,27),(23,27),(23,15),(25,19),(30,16),(28,7),(23,4),(21,8),(11,8)],
          'leggings':[(7,4),(25,4),(25,28),(18,28),(17,15),(15,15),(14,28),(7,28)],
          'boots':[(6,8),(13,8),(13,23),(15,23),(15,28),(3,28),(3,23),(6,22),(6,8)]}
  d.polygon(shapes[name],fill=base+(255,),outline=trim+(255,),width=2)
  if name=='boots':d.polygon([(21,8),(28,8),(28,22),(30,23),(30,28),(18,28),(18,23),(21,22)],fill=base+(255,),outline=trim+(255,),width=2)
  if name=='helmet':
   d.line((7,13,24,13),fill=(12,19,29,255),width=3)
   if theme=='dragon':d.polygon([(3,6),(5,1),(10,7)],fill=glow+(255,));d.polygon([(22,7),(27,1),(29,6)],fill=glow+(255,))
   elif theme=='angel':d.ellipse((8,0,24,5),outline=glow+(255,),width=1)
   elif theme=='leviathan':d.polygon([(4,12),(0,4),(6,8)],fill=trim+(255,));d.polygon([(26,8),(31,4),(28,12)],fill=trim+(255,))
   elif theme=='frost':
    for x in [8,15,22]:d.polygon([(x-2,6),(x,0),(x+2,6)],fill=glow+(255,))
  if theme=='angel':
   for x in [10,14,18,22]:d.line((x,18,x-2,23),fill=glow+(255,),width=1)
  elif theme=='dragon':
   for y in [19,23]:d.line((9,y,13,y+2,17,y,21,y+2),fill=trim+(255,),width=1)
  elif theme=='leviathan':d.arc((9,18,22,25),0,180,fill=glow+(255,),width=2)
  elif theme=='stone':d.rectangle((13,18,19,24),outline=glow+(255,),width=1)
  else:
   d.line((16,17,16,25),fill=glow+(255,));d.line((12,19,20,23),fill=glow+(255,));d.line((12,23,20,19),fill=glow+(255,))
  if name=='helmet':
   d.rectangle((0,0,31,6),fill=(0,0,0,0));d.rectangle((5,6,26,8),fill=base+(255,),outline=trim+(255,))
   if theme in ['dragon','leviathan']:
    d.polygon([(4,8),(2,1),(8,7)],fill=base+(255,),outline=trim+(255,))
    d.polygon([(24,7),(29,1),(27,8)],fill=base+(255,),outline=trim+(255,))
   elif theme=='angel':d.rectangle((14,1,17,6),fill=trim+(255,))
   elif theme=='frost':
    for x in [10,16,22]:d.polygon([(x-2,6),(x,1),(x+2,6)],fill=trim+(255,))
   else:d.rectangle((11,2,21,6),fill=base+(255,),outline=trim+(255,))
  # Paint the icon interior from the corresponding actual wearable surface.
  atlas_image=atlas(theme,name=='leggings')
  regions={'helmet':(8,8,16,16),'chestplate':(20,20,28,32),'leggings':(4,20,8,32),'boots':(4,26,8,32)}
  uv=regions[name];texture=atlas_image.crop(tuple(v*4 for v in uv)).resize((32,32),Image.Resampling.LANCZOS)
  pixels=image.load();paint=texture.load()
  for y in range(32):
   for x in range(32):
    if pixels[x,y][:3]==base and pixels[x,y][3]:pixels[x,y]=paint[x,y]
  image=image.resize((64,64),Image.Resampling.NEAREST)
  savepng(A/'textures/item'/f'{theme}_{name}.png',image)
  savejson(A/'models/item'/f'{theme}_{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'legendaryarmor:item/{theme}_{name}'}})
  savejson(A/'items'/f'{theme}_{name}.json',{'model':{'type':'minecraft:model','model':f'legendaryarmor:item/{theme}_{name}'}})
def effects():
 ring=[]
 for i in range(16):a=i*math.pi/8;ring.append(cube(8+math.cos(a)*6-.7,8,8+math.sin(a)*6-.7,1.4,.5,1.4,'glow'))
 ring +=[cube(7.5,8,1,1,.5,14,'trim'),cube(1,8,7.5,14,.5,1,'trim')]
 model('sunburst','angel',ring)
 flame=[cube(5+i*.7,4+i*.4,6,6-i*.9,7-i*.5,4-i*.4,'glow' if i%2 else 'trim',22.5 if i%2 else -22.5) for i in range(5)]
 model('dragon_flame','dragon',flame)
 wave=[]
 for i in range(10):x=0+i*1.6;wave.append(cube(x,2+math.sin(i*.7)*1.5,7,1.7,6+math.cos(i*.5)*2,1,'base'));wave.append(cube(x,9+math.cos(i*.5)*2,6.6,1.7,.7,1.5,'glow'))
 model('tidal_wave','leviathan',wave)
 model('stone_spike','stone',[cube(4,0,4,8,3,8,'base'),cube(5,3,5,6,4,6,'trim'),cube(6,7,6,4,4,4,'base'),cube(7,11,7,2,3,2,'glow')])
 model('frost_crystal','frost',[cube(5,0,5,6,3,6,'base'),cube(6,3,6,4,7,4,'glow'),cube(7,10,7,2,4,2,'glow'),cube(2,1,7,2,7,2,'trim',-22.5),cube(12,1,7,2,7,2,'trim',22.5)])
def preview(winged=False):
 canvas=Image.new('RGB',(1600,760),(17,22,32));d=ImageDraw.Draw(canvas)
 try:font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',19);small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',14)
 except OSError:font=small=ImageFont.load_default()
 d.text((35,25),'LegendaryArmor — armor inspired by the five boss forms',font=font,fill=(243,225,174))
 d.text((35,57),'Front-view authoring preview. In-game lighting, player skin and movement are not simulated.',font=small,fill=(169,182,201))
 # Draw front-facing boxes from real accessory geometry, then actual mapped texture faces.
 for index,theme in enumerate(THEMES):
  title,base,trim,glow=THEMES[theme];cx=160+index*320;scale=7;ground=480
  def box(x,y,z,w,h,depth,texture,uv):
   face=texture.crop(tuple(int(v*4) for v in uv)).resize((max(1,round(w*scale)),max(1,round(h*scale))),Image.Resampling.NEAREST)
   canvas.paste(face,(round(cx+x*scale),round(ground-y*scale-h*scale)),face)
  def accessory(name,anchor,bulky_only=False):
   elements=KNIGHTPARTS[theme] if bulky_only else MODELS[name]
   for element in elements:
    x,y,z=element['from'];xx,yy,zz=element['to'];material=element['faces']['north']['texture'][1:];color={'base':base,'trim':trim,'glow':glow,'cloth':{'angel':(153,177,201),'dragon':(108,48,67),'leviathan':(49,153,169),'stone':(49,47,46),'frost':(178,220,236)}[theme],'leather':(67,42,30)}[material]
    # Apply element rotation in front projection; side-axis rotations only modestly alter depth.
    corners=[(x,y),(xx,y),(xx,yy),(x,yy)];rotation=element.get('rotation')
    if rotation and rotation['axis']=='z':
     ox,oy,_=rotation['origin'];a=math.radians(rotation['angle']);corners=[(ox+(px-ox)*math.cos(a)-(py-oy)*math.sin(a),oy+(px-ox)*math.sin(a)+(py-oy)*math.cos(a)) for px,py in corners]
    points=[(cx+(px-8)*scale,ground-(py-8+anchor)*scale) for px,py in corners]
    d.polygon(points,fill=color,outline=mix(color,.6))
  if winged and theme in ['angel','dragon','leviathan']:accessory(theme+'_wings',22)
  accessory(theme+'_back',22)
  tex=atlas(theme);legs=atlas(theme,True)
  box(-4,24,0,8,8,8,tex,(8,8,16,16));box(-4,12,0,8,12,4,tex,(20,20,28,32))
  box(-8,12,0,4,12,4,tex,(44,20,48,32));box(4,12,0,4,12,4,tex,(44,20,48,32))
  box(-4,0,0,4,12,4,legs,(4,20,8,32));box(0,0,0,4,12,4,legs,(4,20,8,32))
  box(-4,0,0,4,4,4,tex,(4,28,8,32));box(0,0,0,4,4,4,tex,(4,28,8,32))
  accessory(theme+'_back',22,True)
  accessory(theme+'_crown',33)
  accessory(theme+'_helmet_shell',27)
  d.text((cx-140,530),title,font=font,fill=glow)
  descriptions={'angel':'Ivory feathers + gold guardian plates','dragon':'Ember scales + curved dragon horns','leviathan':'Blue-green scales + fin details','stone':'Stone slabs + glowing mint runes','frost':'Ice-blue facets + crystal details'}
  d.text((cx-140,562),descriptions[theme],font=small,fill=(180,195,214))
  for j,piece in enumerate(['helmet','chestplate','leggings','boots']):
   icon=Image.open(A/'textures/item'/f'{theme}_{piece}.png').resize((56,56),Image.Resampling.NEAREST);canvas.paste(icon,(cx-130+j*64,615),icon)
 d.text((35,714),'Wear all four pieces for the passive • Sneak + F for the active ability • Accessories use item displays, no disguise',font=small,fill=(203,213,229))
 savepng(ROOT/'downloads'/('LegendaryArmor-wing-preview.png' if winged else 'LegendaryArmor-preview.png'),canvas)
def main():
 savejson(PACK/'pack.mcmeta',{'pack':{'description':'LegendaryArmor — five original fantasy armor sets','min_format':[75,0],'max_format':[75,0]}})
 for theme in THEMES:
  savepng(A/'textures/entity/equipment/humanoid'/f'{theme}.png',atlas(theme))
  savepng(A/'textures/entity/equipment/humanoid_leggings'/f'{theme}.png',atlas(theme,True))
  savejson(A/'equipment'/f'{theme}.json',{'layers':{layer:[{'texture':f'legendaryarmor:{theme}'}] for layer in ['humanoid','humanoid_leggings']}})
  for key,color in zip(['base','trim','glow','cloth','leather'],[*THEMES[theme][1:],{'angel':(153,177,201),'dragon':(108,48,67),'leviathan':(49,153,169),'stone':(49,47,46),'frost':(178,220,236)}[theme],(67,42,30)]):
   image=Image.new('RGBA',(32,32),color+(255,));draw=ImageDraw.Draw(image)
   for x in range(32):
    shade=.72+.42*math.exp(-((x-9)/8)**2)
    draw.line((x,0,x,31),fill=mix(color,shade)+(255,))
   draw.rectangle((0,0,31,31),outline=mix(color,.45)+(255,),width=2)
   rng=random.Random(theme+key)
   for _ in range(12):
    x=rng.randint(3,25);y=rng.randint(3,27)
    draw.line((x,y,x+rng.randint(2,5),y+1),fill=mix(color,.45)+(255,));draw.point((x,y-1),fill=mix(color,1.5)+(255,))
   if key=='cloth':
    for row in range(2,31,4):
     for col in range(2,31,4):
      x=col+(2 if row//4%2 else 0)
      draw.line((x-2,row,x,row+2,x+2,row),fill=mix(color,.6)+(255,),width=1)
   savepng(A/'textures/item/material'/f'{theme}_{key}.png',image)
  icons(theme);accessories(theme)
 wings();effects();preview();preview(True)
 output=ROOT/'downloads/LegendaryArmor-resource-pack.zip'
 with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
  for file in sorted(PACK.rglob('*')):
   if file.is_file():z.write(file,file.relative_to(PACK))
 output.with_suffix('.sha1').write_text(hashlib.sha1(output.read_bytes()).hexdigest()+'\n')
 # Preserve all sword/BetterModel data and reject collisions when making a single client pack.
 combined=ROOT/'downloads/LegendaryArmor-Swords-combined-resource-pack.zip'
 with zipfile.ZipFile(ROOT/'downloads/LegendarySwords-combined-resource-pack-0.5.3.zip') as old,zipfile.ZipFile(output) as new,zipfile.ZipFile(combined,'w',zipfile.ZIP_DEFLATED) as merged:
  entries={name:old.read(name) for name in old.namelist()}
  for name in new.namelist():
   if name=='pack.mcmeta':continue
   data=new.read(name)
   if name in entries and entries[name]!=data:raise ValueError('Asset collision: '+name)
   entries[name]=data
  meta=json.loads(entries['pack.mcmeta']);meta['pack']['description']='LegendarySwords + BetterModel + LegendaryArmor';entries['pack.mcmeta']=json.dumps(meta,indent=2).encode()
  for name,data in sorted(entries.items()):merged.writestr(name,data)
 combined.with_suffix('.sha1').write_text(hashlib.sha1(combined.read_bytes()).hexdigest()+'\n')
 print('Generated five armor atlases, twenty item icons, fifteen accessories, five ability meshes and combined pack.')
if __name__=='__main__':main()
