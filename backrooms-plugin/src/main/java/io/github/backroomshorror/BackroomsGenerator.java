package io.github.backroomshorror;
import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import java.util.Random;
final class BackroomsGenerator extends ChunkGenerator {
 private final int level;
 BackroomsGenerator(int level){this.level=level;}
 @Override public ChunkData generateChunkData(World world,Random random,int cx,int cz,BiomeGrid biomes){
  ChunkData data=createChunkData(world);Material floor=level==0?Material.YELLOW_TERRACOTTA:level==1?Material.GRAY_CONCRETE:Material.DEEPSLATE_TILES;
  Material walls=level==0?Material.YELLOW_CONCRETE:level==1?Material.LIGHT_GRAY_CONCRETE:Material.POLISHED_BLACKSTONE_BRICKS;
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){int gx=cx*16+x,gz=cz*16+z;data.setBlock(x,62,z,Material.BEDROCK);data.setBlock(x,63,z,floor);data.setBlock(x,68,z,Rooms.lamp(gx,gz)?Material.SEA_LANTERN:level==0?Material.SMOOTH_SANDSTONE:Material.SMOOTH_STONE);
   if(Rooms.wall(gx,gz))for(int y=64;y<68;y++)data.setBlock(x,y,z,walls);
   if(level==2&&Math.floorMod(gx,8)==1) data.setBlock(x,67,z,Material.COPPER_BLOCK);
   if(Rooms.exit(gx,gz)){data.setBlock(x,63,z,Material.EMERALD_BLOCK);data.setBlock(x,64,z,Material.LIGHT_WEIGHTED_PRESSURE_PLATE);}
  }return data;
 }
 @Override public Location getFixedSpawnLocation(World world,Random random){return new Location(world,4.5,64,4.5);}
 @Override public boolean shouldGenerateNoise(){return false;}
 @Override public boolean shouldGenerateSurface(){return false;}
 @Override public boolean shouldGenerateCaves(){return false;}
 @Override public boolean shouldGenerateDecorations(){return false;}
 @Override public boolean shouldGenerateMobs(){return false;}
 @Override public boolean shouldGenerateStructures(){return false;}
}
