#!/usr/bin/env python3
"""Independently read packaged NBT/Anvil worlds and check their playable blocks."""
import gzip,zlib,struct,io,zipfile
from pathlib import Path

def read(f,n):
 b=f.read(n);assert len(b)==n;return b
def number(f,fmt):return struct.unpack(fmt,read(f,struct.calcsize(fmt)))[0]
def string(f):return read(f,number(f,'>H')).decode()
def value(f,t):
 formats={1:'>b',2:'>h',3:'>i',4:'>q',5:'>f',6:'>d'}
 if t in formats:return number(f,formats[t])
 if t==7:return read(f,number(f,'>i'))
 if t==8:return string(f)
 if t==9:
  subtype=number(f,'>B');return [value(f,subtype) for _ in range(number(f,'>i'))]
 if t in (11,12):return [number(f,'>i' if t==11 else '>q') for _ in range(number(f,'>i'))]
 if t==10:
  out={}
  while True:
   kind=number(f,'>B')
   if kind==0:return out
   name=string(f);out[name]=value(f,kind)
 raise ValueError(t)
def nbt(raw):
 f=io.BytesIO(raw);assert number(f,'>B')==10;string(f);return value(f,10)
def block(chunks,x,y,z):
 chunk=chunks[x//16,z//16];section=next(s for s in chunk['sections'] if s['Y']==y//16);states=section['block_states'];palette=states['palette']
 if len(palette)==1:return palette[0]['Name']
 bits=max(4,(len(palette)-1).bit_length());per=64//bits;index=(y%16)*256+(z%16)*16+x%16;word=states['data'][index//per];p=(word>>((index%per)*bits))&((1<<bits)-1);assert p<len(palette);return palette[p]['Name']
for level in range(3):
 path=Path(__file__).resolve().parents[2]/'downloads'/f'Backrooms-level-{level}-world.zip'
 with zipfile.ZipFile(path) as z:
  assert z.testzip() is None;data=nbt(gzip.decompress(z.read('level.dat')))['Data'];assert data['SpawnY']==64 and data['BorderSize']==256
  chunks={}
  for name in z.namelist():
   if not name.endswith('.mca'):continue
   raw=z.read(name)
   for index in range(1024):
    header=int.from_bytes(raw[index*4:index*4+4],'big');sector=header>>8;count=header&255
    if not sector:continue
    assert count and (sector+count)*4096<=len(raw);offset=sector*4096;length=int.from_bytes(raw[offset:offset+4],'big');assert length+4<=count*4096 and raw[offset+4]==2
    chunk=nbt(zlib.decompress(raw[offset+5:offset+4+length]));assert chunk['Status']=='minecraft:full' and chunk['DataVersion']==3953;assert len(chunk['sections'])==24
    coords=chunk['xPos'],chunk['zPos'];assert coords not in chunks;chunks[coords]=chunk
  assert len(chunks)==256
  assert block(chunks,4,64,4)=='minecraft:air'
  assert block(chunks,4,63,4)=='minecraft:'+['yellow_terracotta','gray_concrete','deepslate_tiles'][level]
  assert block(chunks,52,63,52)=='minecraft:emerald_block'
  assert block(chunks,52,64,52)=='minecraft:light_weighted_pressure_plate'
  assert block(chunks,4,68,4)=='minecraft:sea_lantern'
  assert block(chunks,-4,64,-4)=='minecraft:air'
 print('Validated world',level,': 256 chunks, usable entry, lighting blocks, and escape plate.')
