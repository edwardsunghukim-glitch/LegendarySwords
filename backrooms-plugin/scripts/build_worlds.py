#!/usr/bin/env python3
"""Write original, playable Anvil world folders using standard Minecraft NBT."""
import struct,zlib,gzip,math,io,zipfile
from pathlib import Path

def s(value):
 b=value.encode();return struct.pack('>H',len(b))+b

def tag(t,name,payload):return bytes([t])+s(name)+payload

def compound(values):return b''.join(values)+b'\0'
def integer(name,n):return tag(3,name,struct.pack('>i',n))
def long(name,n):return tag(4,name,struct.pack('>q',n))
def string(name,value):return tag(8,name,s(value))
def byte(name,n):return tag(1,name,struct.pack('>b',n))
def lst(name,t,values):return tag(9,name,bytes([t])+struct.pack('>i',len(values))+b''.join(values))
def root(values):return tag(10,'',compound(values))
def wall(x,z):return x%8==0 and z%8 not in (3,4) or z%8==0 and x%8 not in (3,4)
def block(level,x,y,z):
 if y==62:return 'bedrock'
 if y==63:return 'emerald_block' if (x,z)==(52,52) else ['yellow_terracotta','gray_concrete','deepslate_tiles'][level]
 if y==68:return 'sea_lantern' if x%8==4 and z%8==4 else 'smooth_sandstone' if level==0 else 'smooth_stone'
 if 64<=y<68 and wall(x,z):return ['yellow_concrete','light_gray_concrete','polished_blackstone_bricks'][level]
 if level==2 and y==67 and x%8==1:return 'copper_block'
 if (x,y,z)==(52,64,52):return 'light_weighted_pressure_plate'
 return 'air'
def packed(values,bits):
 per=64//bits;words=[]
 for start in range(0,len(values),per):
  word=0
  for i,v in enumerate(values[start:start+per]):word|=v<<(i*bits)
  words.append(struct.pack('>Q',word))
 return struct.pack('>i',len(words))+b''.join(words)
def section(level,cx,cz,sy):
 palette={};values=[]
 for y in range(16):
  for z in range(16):
   for x in range(16):
    name='minecraft:'+block(level,cx*16+x,sy*16+y,cz*16+z);palette.setdefault(name,len(palette));values.append(palette[name])
 states=[lst('palette',10,[compound([string('Name',name)]) for name in palette])]
 if len(palette)>1:states.append(tag(12,'data',packed(values,max(4,(len(palette)-1).bit_length()))))
 return compound([byte('Y',sy),tag(10,'block_states',compound(states)),tag(10,'biomes',compound([lst('palette',8,[s('minecraft:plains')])]))])
def chunk(level,cx,cz):
 height=packed([133]*256,9)
 return root([integer('DataVersion',3953),integer('xPos',cx),integer('zPos',cz),integer('yPos',-4),string('Status','minecraft:full'),long('LastUpdate',0),long('InhabitedTime',0),byte('isLightOn',0),lst('sections',10,[section(level,cx,cz,y) for y in range(-4,20)]),lst('block_entities',10,[]),lst('block_ticks',10,[]),lst('fluid_ticks',10,[]),lst('PostProcessing',9,[b'\x02'+struct.pack('>i',0) for _ in range(24)]),tag(10,'Heightmaps',compound([tag(12,'MOTION_BLOCKING',height),tag(12,'WORLD_SURFACE',height)])),tag(10,'structures',compound([tag(10,'starts',b'\0'),tag(10,'References',b'\0')]))])
def leveldat(level):
 layers=[compound([integer('height',1),string('block','minecraft:bedrock')]),compound([integer('height',1),string('block','minecraft:yellow_terracotta')])]
 settings=compound([string('biome','minecraft:plains'),byte('lakes',0),byte('features',0),lst('layers',10,layers),lst('structure_overrides',8,[])])
 generator=compound([string('type','minecraft:flat'),tag(10,'settings',settings)])
 overworld=compound([string('type','minecraft:overworld'),tag(10,'generator',generator)])
 dims=[tag(10,'minecraft:overworld',overworld)]
 for key,preset,biome_type in [('the_nether','nether','multi_noise'),('the_end','end','the_end')]:
  bs=[string('type','minecraft:'+biome_type)]
  if key=='the_nether':bs.append(string('preset','minecraft:nether'))
  dims.append(tag(10,'minecraft:'+key,compound([string('type','minecraft:'+key),tag(10,'generator',compound([string('type','minecraft:noise'),string('settings','minecraft:'+preset),tag(10,'biome_source',compound(bs))]))])))
 worldgen=compound([long('seed',11235813+level),byte('generate_features',0),byte('bonus_chest',0),tag(10,'dimensions',compound(dims))])
 rules=compound([string('doMobSpawning','false'),string('doDaylightCycle','false'),string('doWeatherCycle','false')])
 data=[integer('DataVersion',3953),tag(10,'Version',compound([integer('Id',3953),string('Name','1.21'),byte('Snapshot',0)])),integer('version',19133),string('LevelName','Backrooms Level '+str(level)),integer('GameType',0),byte('hardcore',0),byte('allowCommands',1),byte('initialized',1),byte('Difficulty',2),byte('DifficultyLocked',0),integer('SpawnX',4),integer('SpawnY',64),integer('SpawnZ',4),tag(5,'SpawnAngle',struct.pack('>f',0)),long('Time',18000),long('DayTime',18000),long('LastPlayed',0),integer('rainTime',0),integer('thunderTime',0),byte('raining',0),byte('thundering',0),tag(10,'GameRules',rules),tag(10,'WorldGenSettings',worldgen),tag(10,'DataPacks',compound([lst('Enabled',8,[s('vanilla')]),lst('Disabled',8,[])])),tag(6,'BorderCenterX',struct.pack('>d',0)),tag(6,'BorderCenterZ',struct.pack('>d',0)),tag(6,'BorderSize',struct.pack('>d',256)),tag(6,'BorderSizeLerpTarget',struct.pack('>d',256)),long('BorderSizeLerpTime',0)]
 return gzip.compress(root([tag(10,'Data',compound(data))]),mtime=0)
def make(level):
 regions={}
 for cx in range(-8,8):
  for cz in range(-8,8):regions.setdefault((cx//32,cz//32),[]).append((cx,cz))
 files={'level.dat':leveldat(level)}
 for (rx,rz),coords in regions.items():
  header=bytearray(8192);body=bytearray();sector=2
  for cx,cz in coords:
   raw=chunk(level,cx,cz);compressed=zlib.compress(raw,6);record=struct.pack('>I',len(compressed)+1)+b'\x02'+compressed;count=math.ceil(len(record)/4096);assert count<256
   index=(cx%32)+(cz%32)*32;header[index*4:index*4+4]=((sector<<8)|count).to_bytes(4,'big');body.extend(record);body.extend(b'\0'*(count*4096-len(record)));sector+=count
  files[f'region/r.{rx}.{rz}.mca']=bytes(header)+bytes(body)
 return files
if __name__=='__main__':
 out=Path(__file__).resolve().parents[2]/'downloads'
 install='''These are actual Minecraft world folders (level.dat + region files), not schematics.\nEach original map is 256 x 256 blocks and fully pregenerated.\nLevel 0: yellow rooms. Level 1: concrete corridors. Level 2: dark pipe corridors.\n\nWith BackroomsHorror installed: stop the server; extract each world ZIP into\na server-root folder named backrooms_horror_0, backrooms_horror_1 or backrooms_horror_2.\nThese folders belong beside the normal world folder, not inside plugins.\nDo not overwrite existing world folders; back them up or choose a fresh install.\nRestart and run /br map 0 (or 1/2) to load the desired map.\nThe gold plate on the emerald block at x=52,z=52 advances levels and escapes.\nUse /br summon <type> for NPCs or enable auto-spawn in the plugin config.\n\nWithout the plugin: these maps also work as ordinary Minecraft Java worlds;\nescape plates are decorative and no custom entities are included. Extract\neach ZIP into its own folder inside .minecraft/saves.\n\nWorld format is Minecraft Java 1.21, which Paper/Minecraft 1.21.11 can upgrade.\nKeep BackroomsHorror installed for matching generation beyond the pregenerated\narea. No mod models, mod files or third-party maps are included.\n'''
 for level in range(3):
  files=make(level)
  with zipfile.ZipFile(out/f'Backrooms-level-{level}-world.zip','w',zipfile.ZIP_DEFLATED) as z:
   for name,data in files.items():z.writestr(name,data)
   z.writestr('INSTALL-WORLD.txt',install)
  print('Built actual world',level,'with 256 chunks and',len(files)-1,'region files.')
