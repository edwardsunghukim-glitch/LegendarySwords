package io.github.backroomshorror;
import java.util.UUID;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.java.JavaPlugin;
interface BacteriaVisuals {
 void add(Mob mob);
 void remove(UUID id);
 void close();
 default void action(UUID id,String name){}
 default String problem(){return null;}
 static BacteriaVisuals create(JavaPlugin plugin){
  if(plugin.getConfig().getString("bacteria-renderer","bettermodel").equalsIgnoreCase("blocks"))return new BacteriaModels(plugin);
  if(plugin.getServer().getPluginManager().isPluginEnabled("BetterModel"))try{return new BetterModelBacteria(plugin);}catch(RuntimeException|LinkageError e){plugin.getLogger().warning("BetterModel integration failed: "+e);}
  return new BacteriaVisuals(){public void add(Mob mob){throw new IllegalStateException(problem());}public void remove(UUID id){}public void close(){}public String problem(){return "Bacteria requires BetterModel 3.1. Install it and restart (or set bacteria-renderer: blocks for the old shape).";}};
 }
}
