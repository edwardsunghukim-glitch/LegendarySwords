package io.github.backroomshorror;
import java.util.*;
import java.nio.file.*;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.java.JavaPlugin;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.tracker.*;
/** The user's converted Lifeform mesh, using the server's BetterModel resource pack. */
final class BetterModelBacteria implements BacteriaVisuals {
 static final String MODEL="backroomshorror_bacteria";
 private final Map<UUID,EntityTracker> models=new HashMap<>();private final Map<UUID,Mob> mobs=new HashMap<>();private final Map<UUID,String> motion=new HashMap<>();
 private static final AnimationModifier LOOP=AnimationModifier.builder().type(AnimationIterator.Type.LOOP).priority(10).build();
 private static final AnimationModifier ACTION=AnimationModifier.builder().type(AnimationIterator.Type.PLAY_ONCE).priority(30).override(true).build();
 BetterModelBacteria(JavaPlugin plugin){
  var engine=plugin.getServer().getPluginManager().getPlugin("BetterModel");
  try{Path file=engine.getDataFolder().toPath().resolve("models/"+MODEL+".bbmodel");boolean exists=Files.exists(file);boolean upgrade=exists&&java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))).equals("2f1a0e85d78da6c9214220837f9df374d23875af756bf94c5c9657e0442cb97e");if(!exists||upgrade){if(upgrade){Path backup=plugin.getDataFolder().toPath().resolve("bacteria-backups/mesh-"+System.currentTimeMillis()+".bbmodel");Files.createDirectories(backup.getParent());Files.copy(file,backup);}Files.createDirectories(file.getParent());try(var resource=plugin.getResource("bettermodel/"+MODEL+".bbmodel")){if(resource==null)throw new IllegalStateException("Missing bundled Bacteria mesh");Files.copy(resource,file,StandardCopyOption.REPLACE_EXISTING);}plugin.getLogger().info("Installed Bacteria model. Run /bm reload and install the newly generated BetterModel build.zip resource pack.");}}
  catch(java.io.IOException|java.security.NoSuchAlgorithmException e){throw new IllegalStateException("Could not install Bacteria model",e);}
  plugin.getServer().getScheduler().runTaskTimer(plugin,this::tick,1,1);
 }
 @Override public String problem(){return BetterModel.modelOrNull(MODEL)==null?"Bacteria model is not loaded. Run /bm reload, then install BetterModel's updated build.zip resource pack.":null;}
 @Override public void add(Mob mob){var renderer=BetterModel.modelOrNull(MODEL);if(renderer==null)throw new IllegalStateException(problem());EntityTracker tracker=renderer.create(BukkitAdapter.adapt(mob),new TrackerModifier(false,false,true),t->{t.hideOption(EntityHideOption.DEFAULT);t.scaler(ModelScaler.value(1));});models.put(mob.getUniqueId(),tracker);mobs.put(mob.getUniqueId(),mob);tracker.animate("idle",LOOP);motion.put(mob.getUniqueId(),"idle");}
 private void tick(){for(UUID id:new ArrayList<>(models.keySet())){Mob mob=mobs.get(id);EntityTracker tracker=models.get(id);if(mob==null||!mob.isValid()||mob.isDead()||tracker.isClosed()){remove(id);continue;}var velocity=mob.getVelocity();double speed=velocity.getX()*velocity.getX()+velocity.getZ()*velocity.getZ();String clip=speed>.0005?(speed>.04?"run":"walk"):"idle";if(!clip.equals(motion.get(id))){tracker.stopAnimation(motion.get(id));tracker.animate(clip,LOOP);motion.put(id,clip);}}}
 @Override public void action(UUID id,String name){EntityTracker tracker=models.get(id);if(tracker!=null&&!tracker.isClosed())tracker.animate(name,ACTION);}
 @Override public void remove(UUID id){EntityTracker tracker=models.remove(id);mobs.remove(id);motion.remove(id);if(tracker!=null)tracker.close();}
 @Override public void close(){for(UUID id:new ArrayList<>(models.keySet()))remove(id);}
}
