package io.github.legendarybosses;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
final class SpawnProtection {
 private final JavaPlugin plugin;private WorldGuardRegion regions;private boolean failed;
 SpawnProtection(JavaPlugin plugin){this.plugin=plugin;var wg=plugin.getServer().getPluginManager().getPlugin("WorldGuard");if(wg!=null&&wg.isEnabled())try{regions=new WorldGuardRegion(wg);}catch(ReflectiveOperationException|LinkageError e){failed=true;plugin.getLogger().severe("Cannot read WorldGuard spawn regions; boss targeting is paused: "+e.getMessage());}}
 boolean protectedAt(Location at){
  if(!plugin.getConfig().getBoolean("spawn-protection.enabled",true))return false;
  if(failed)return true;
  if(regions!=null)try{for(String name:plugin.getConfig().getStringList("spawn-protection.regions")){Object region=regions.find(at.getWorld(),name);if(region!=null&&regions.contains(region,at))return true;}}catch(ReflectiveOperationException|RuntimeException e){failed=true;plugin.getLogger().severe("WorldGuard query failed; boss targeting paused: "+e.getMessage());return true;}
  Location spawn=at.getWorld().getSpawnLocation();double radius=plugin.getConfig().getDouble("spawn-protection.world-spawn-radius",16);
  return radius>0&&Math.pow(at.getX()-spawn.getX(),2)+Math.pow(at.getZ()-spawn.getZ(),2)<=radius*radius;
 }
}
