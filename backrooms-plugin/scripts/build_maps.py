#!/usr/bin/env python3
"""Build original Sponge v2 schematics; no third-party map assets."""
import gzip,struct,zipfile
from pathlib import Path
W=L=96;H=7

def text(s):
 b=s.encode();return struct.pack('>H',len(b))+b

def tag(kind,name,payload):return bytes([kind])+text(name)+payload

def integer(n):return struct.pack('>i',n)

def varint(n):
 b=bytearray()
 while n>127:b.append((n&127)|128);n>>=7
 b.append(n);return b

def wall(x,z):return x%8==0 and z%8 not in (3,4) or z%8==0 and x%8 not in (3,4)

def make(level):
 palette={'minecraft:air':0}
 floor=['yellow_terracotta','gray_concrete','deepslate_tiles'][level]
 walls=['yellow_concrete','light_gray_concrete','polished_blackstone_bricks'][level]
 values=[]
 for y in range(H):
  for z in range(L):
   for x in range(W):
    block='air'
    if y==0:block='bedrock'
    elif y==1:block='emerald_block' if (x,z)==(52,52) else floor
    elif y==6:block='sea_lantern' if x%8==4 and z%8==4 else 'smooth_sandstone' if level==0 else 'smooth_stone'
    elif wall(x,z) or x in (0,W-1) or z in (0,L-1):block=walls
    elif level==2 and x%8==1 and y==5:block='copper_block'
    if (x,z)==(52,52) and y==2:block='light_weighted_pressure_plate'
    key='minecraft:'+block
    palette.setdefault(key,len(palette));values.append(palette[key])
 data=b''.join(varint(n) for n in values)
 p=b''.join(tag(3,name,integer(index)) for name,index in palette.items())+b'\x00'
 root=b''.join([
  tag(3,'Version',integer(2)),tag(3,'DataVersion',integer(3953)),
  tag(2,'Width',struct.pack('>h',W)),tag(2,'Height',struct.pack('>h',H)),tag(2,'Length',struct.pack('>h',L)),
  tag(11,'Offset',integer(3)+integer(0)*3),tag(3,'PaletteMax',integer(len(palette))),tag(10,'Palette',p),
  tag(7,'BlockData',integer(len(data))+data),tag(9,'BlockEntities',b'\x0a'+integer(0)),b'\x00'])
 payload=tag(10,'Schematic',root)
 # Validate gzip round-trip, palette indices and volume before publishing.
 encoded=gzip.compress(payload,mtime=0);assert gzip.decompress(encoded)==payload
 assert len(values)==W*H*L and all(v<len(palette) for v in values)
 return encoded

if __name__=='__main__':
 out=Path(__file__).resolve().parents[2]/'downloads';out.mkdir(exist_ok=True)
 guide='''BackroomsHorror original maps — three 96 x 96 Sponge v2 schematics.\n\nThe plugin itself creates separate 256 x 256 worlds with /br map 0, 1 or 2.\nYou do NOT need WorldEdit for the plugin-generated worlds.\n\nThese optional .schem files require WorldEdit to paste into your own map:\n1. Copy the three .schem files to plugins/WorldEdit/schematics.\n2. In game: //schem load backrooms_level0\n3. Stand at the desired lower corner and use //paste -a.\n\nLevels: 0 yellow rooms, 1 concrete corridors, 2 dark pipe corridors.\nThe gold exit plate is 52 blocks from the paste origin on both horizontal axes.\nEntry room center is 4 blocks from the origin. Schematics contain blocks only;\nNPCs are added using /br summon. Level travel is active only in plugin-generated\nworlds. These are original maps, not exports from Backrooms: Found Footage.\n'''
 with zipfile.ZipFile(out/'BackroomsHorror-maps.zip','w',zipfile.ZIP_DEFLATED) as z:
  for level in range(3):z.writestr(f'backrooms_level{level}.schem',make(level))
  z.writestr('MAP-INSTALL.txt',guide)
 print('Validated three 96 x 7 x 96 map schematics.')
