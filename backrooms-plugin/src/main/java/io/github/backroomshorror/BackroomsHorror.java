package io.github.backroomshorror;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import java.util.*;
import java.io.File;

public final class BackroomsHorror extends JavaPlugin implements Listener {
 private final Map<UUID,Npc> npcs=new LinkedHashMap<>();private final Map<UUID,Location> returns=new HashMap<>();private NamespacedKey marker;private int ticks;private YamlConfiguration travel;private File travelFile;private SpawnProtection protection;
 private static final class Npc {final NpcType type;final Mob mob;final int born;int next;int move;Npc(NpcType type,Mob mob,int ticks){this.type=type;this.mob=mob;born=ticks;next=ticks+40;}}
 @Override public void onEnable(){saveDefaultConfig();protection=new SpawnProtection(this);marker=new NamespacedKey(this,"npc");travelFile=new File(getDataFolder(),"travel.yml");travel=YamlConfiguration.loadConfiguration(travelFile);for(String id:travel.getKeys(false))try{Location l=travel.getLocation(id);if(l!=null)returns.put(UUID.fromString(id),l);}catch(IllegalArgumentException ignored){}
  getServer().getPluginManager().registerEvents(this,this);for(World world:Bukkit.getWorlds())for(Entity e:world.getEntities())if(e.getPersistentDataContainer().has(marker))e.remove();getServer().getScheduler().runTaskTimer(this,this::tick,5,5);
 }
 @Override public void onDisable(){npcs.values().forEach(n->n.mob.remove());npcs.clear();saveTravel();}
 @Override public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String world,String id){int level=level(world);return level<0?null:new BackroomsGenerator(level);}
 private int level(String world){String prefix=getConfig().getString("world-prefix","backrooms_horror")+"_";if(!world.startsWith(prefix))return -1;try{int n=Integer.parseInt(world.substring(prefix.length()));return n>=0&&n<=2?n:-1;}catch(NumberFormatException e){return -1;}}
 private World map(int level){String name=getConfig().getString("world-prefix","backrooms_horror")+"_"+level;World world=Bukkit.getWorld(name);if(world==null){world=new WorldCreator(name).generator(new BackroomsGenerator(level)).generateStructures(false).createWorld();if(world==null)throw new IllegalStateException("Map world could not be loaded");world.setSpawnLocation(4,64,4);world.getWorldBorder().setCenter(0,0);world.getWorldBorder().setSize(Math.max(128,Math.min(1024,getConfig().getInt("world-size",256))));world.setDifficulty(Difficulty.NORMAL);world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setTime(18000);}return world;}
 private void saveTravel(){for(var e:returns.entrySet())travel.set(e.getKey().toString(),e.getValue());try{travel.save(travelFile);}catch(java.io.IOException e){getLogger().warning("Could not save return positions: "+e.getMessage());}}
 private boolean combat(Player p){var plugin=Bukkit.getPluginManager().getPlugin("KillTokens");if(plugin==null||!plugin.isEnabled())return false;try{return ((Number)plugin.getClass().getMethod("getCombatSeconds",UUID.class).invoke(plugin,p.getUniqueId())).longValue()>0;}catch(ReflectiveOperationException e){p.sendMessage("Cannot check combat status; travel is paused.");return true;}}
 private void enter(Player p,int destination){if(!p.hasPermission("backrooms.use")){p.sendMessage("No permission.");return;}if(combat(p)){p.sendMessage("You cannot travel while in combat.");return;}if(level(p.getWorld().getName())<0){returns.put(p.getUniqueId(),p.getLocation());saveTravel();}try{World world=map(destination);if(p.teleport(new Location(world,4.5,64,4.5)))p.sendMessage(Component.text("Level "+destination+" • Find the emerald exit plate.",TextColor.color(0xe0cc83)));}catch(RuntimeException e){p.sendMessage("Map creation failed; check console.");getLogger().warning(e.toString());}}
 private Location outside(Player p){Location saved=returns.get(p.getUniqueId());if(saved!=null&&saved.getWorld()!=null&&level(saved.getWorld().getName())<0)return saved;return Bukkit.getWorlds().stream().filter(w->level(w.getName())<0).findFirst().map(World::getSpawnLocation).orElse(null);}
 private void leave(Player p){if(level(p.getWorld().getName())<0){p.sendMessage("You are not in a Backrooms map.");return;}if(combat(p)){p.sendMessage("You cannot travel while in combat.");return;}Location to=outside(p);if(to==null){p.sendMessage("No return world is available.");return;}if(p.teleport(to)){returns.remove(p.getUniqueId());travel.set(p.getUniqueId().toString(),null);saveTravel();p.sendMessage("You escaped the Backrooms.");}}
 private double setting(Npc n,String name,double fallback){double value=getConfig().getDouble("npcs."+n.type.id()+"."+name,fallback);return Double.isFinite(value)?Math.clamp(value,0,200):fallback;}
 private boolean eligible(Player p){return !p.isDead()&&(p.getGameMode()==GameMode.SURVIVAL||p.getGameMode()==GameMode.ADVENTURE)&&!safe(p.getLocation());}
 private boolean safe(Location at){if(protection.protectedAt(at))return true;int level=level(at.getWorld().getName());if(level<0)return false;return Math.pow(at.getX()-4.5,2)+Math.pow(at.getZ()-4.5,2)<Math.pow(getConfig().getDouble("safe-entry-radius",10),2);}
 private void summon(NpcType type,Location at,CommandSender sender){long count=npcs.values().stream().filter(n->n.mob.getWorld().equals(at.getWorld())).count();if(count>=Math.max(1,getConfig().getInt("npc-limit-per-world",8))){if(sender!=null)sender.sendMessage("NPC limit reached in this world.");return;}if(!at.getBlock().isPassable()||!at.clone().add(0,1,0).getBlock().isPassable()){if(sender!=null)sender.sendMessage("Choose an open space for the entity.");return;}
  Entity e=at.getWorld().spawnEntity(at,type.entity);if(!(e instanceof Mob mob)||!e.isValid()){e.remove();if(sender!=null)sender.sendMessage("Mob spawning is blocked here.");return;}
  Npc n=new Npc(type,mob,ticks);mob.getPersistentDataContainer().set(marker,PersistentDataType.STRING,type.id());mob.customName(Component.text(type.title,TextColor.color(type.color)));mob.setCustomNameVisible(true);mob.setPersistent(false);mob.setRemoveWhenFarAway(false);mob.setCanPickupItems(false);if(mob.getEquipment()!=null)mob.getEquipment().clear();mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Math.max(2,setting(n,"hearts",25)*2));mob.setHealth(mob.getAttribute(Attribute.MAX_HEALTH).getValue());mob.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(Math.min(.5,setting(n,"speed",.27)));if(mob.getAttribute(Attribute.ATTACK_DAMAGE)!=null)mob.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(setting(n,"damage",7));
  if(mob instanceof Zombie zombie){zombie.setAdult();zombie.setShouldBurnInDay(false);}if(mob instanceof Wolf wolf)wolf.setAngry(true);npcs.put(e.getUniqueId(),n);if(sender!=null)sender.sendMessage("Summoned "+type.title+".");
 }
 private void tick(){ticks+=5;for(Npc n:new ArrayList<>(npcs.values())){
  if(!n.mob.isValid()||n.mob.isDead()||ticks-n.born>Math.max(30,getConfig().getInt("npc-lifetime-seconds",300))*20){n.mob.remove();npcs.remove(n.mob.getUniqueId());continue;}
  Player target=n.mob.getWorld().getPlayers().stream().filter(this::eligible).filter(p->p.getLocation().distanceSquared(n.mob.getLocation())<32*32).filter(n.mob::hasLineOfSight).min(Comparator.comparingDouble(p->p.getLocation().distanceSquared(n.mob.getLocation()))).orElse(null);
  boolean watched=target!=null&&NpcRules.watching(target.getEyeLocation().getDirection(),n.mob.getEyeLocation().toVector().subtract(target.getEyeLocation().toVector()));
  if(n.type==NpcType.WATCHER){n.mob.setAI(!watched);if(watched)n.mob.setVelocity(new Vector());}
  n.mob.setTarget(target);if(target!=null&&!(n.type==NpcType.WATCHER&&watched)&&ticks>=n.next){move(n,target,n.move++%3);n.next=ticks+(int)(Math.max(2,setting(n,"cooldown-seconds",7))*20);}
 }
 if(ticks%40==0)for(Player p:Bukkit.getOnlinePlayers())if(level(p.getWorld().getName())>=0){p.playSound(p.getLocation(),Sound.BLOCK_BEACON_AMBIENT,.18f,.5f);p.sendActionBar(Component.text("Level "+level(p.getWorld().getName())+" • Emerald plates lead out",TextColor.color(0xbca976)));}
 if(getConfig().getBoolean("auto-spawn",false)&&ticks%Math.max(20,getConfig().getInt("spawn-interval-seconds",30)*20)==0)for(Player p:Bukkit.getOnlinePlayers())if(level(p.getWorld().getName())>=0&&eligible(p)){int r=Math.max(12,Math.min(40,getConfig().getInt("spawn-distance",22)));Location at=p.getLocation().clone().add(java.util.concurrent.ThreadLocalRandom.current().nextInt(-r,r+1),0,java.util.concurrent.ThreadLocalRandom.current().nextInt(-r,r+1));at.setY(64);if(at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4)&&!safe(at))summon(NpcType.values()[java.util.concurrent.ThreadLocalRandom.current().nextInt(4)],at,null);}
 }
 private void hit(Npc n,Player p,double factor,PotionEffectType effect){if(!eligible(p)||p.getWorld()!=n.mob.getWorld())return;double old=p.getHealth();p.damage(setting(n,"damage",7)*factor,n.mob);if(p.getHealth()<old&&effect!=null)p.addPotionEffect(new PotionEffect(effect,60,0));}
 private void burst(Npc n,double radius,PotionEffectType effect){Location c=n.mob.getLocation();for(int i=0;i<40;i++){double a=i*Math.PI/20;c.getWorld().spawnParticle(Particle.SMOKE,c.clone().add(Math.cos(a)*radius,.5,Math.sin(a)*radius),1);}for(Player p:c.getWorld().getPlayers())if(eligible(p)&&p.getLocation().distanceSquared(c)<radius*radius&&n.mob.hasLineOfSight(p))hit(n,p,.6,effect);}
 private void move(Npc n,Player p,int move){Location at=n.mob.getLocation();Vector direction=p.getLocation().toVector().subtract(at.toVector());if(direction.lengthSquared()<.01)return;direction.normalize();
  switch(n.type){
   case STALKER -> {if(move==0){n.mob.setVelocity(direction.multiply(.9).setY(.2));at.getWorld().spawnParticle(Particle.SMOKE,at,20,.3,1,.3,.02);}else if(move==1)burst(n,5,PotionEffectType.DARKNESS);else{n.mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,50,0));p.playSound(at,Sound.ENTITY_ENDERMAN_STARE,.7f,.6f);}}
   case SMILER -> {if(move==0)burst(n,6,PotionEffectType.BLINDNESS);else if(move==1){n.mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,60,1));at.getWorld().spawnParticle(Particle.SOUL,at,18,.5,1,.5,.01);}else{p.playSound(at,Sound.ENTITY_GHAST_SCREAM,.5f,.5f);if(p.getLocation().distanceSquared(at)<9)hit(n,p,1.2,PotionEffectType.WEAKNESS);}}
   case HOUND -> {if(move==0){n.mob.setVelocity(direction.multiply(1).setY(.45));at.getWorld().spawnParticle(Particle.CLOUD,at,15,.4,.2,.4,.03);}else if(move==1)burst(n,5,PotionEffectType.SLOWNESS);else if(p.getLocation().distanceSquared(at)<9)hit(n,p,1.4,PotionEffectType.WEAKNESS);}
   case WATCHER -> {if(move==0&&!NpcRules.watching(p.getEyeLocation().getDirection(),n.mob.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector()))){Location step=at.clone().add(direction.multiply(Math.min(5,Math.sqrt(p.getLocation().distanceSquared(at))-2)));if(step.getBlock().isPassable()&&step.clone().add(0,1,0).getBlock().isPassable()&&!safe(step)){n.mob.teleport(step);step.getWorld().spawnParticle(Particle.PORTAL,step,25,.4,1,.4,.1);}}else if(move==1)burst(n,5,PotionEffectType.DARKNESS);else if(p.getLocation().distanceSquared(at)<16)hit(n,p,1.2,PotionEffectType.SLOWNESS);}
  }
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageByEntityEvent e){if(e.getEntity() instanceof Player p&&npcs.containsKey(e.getDamager().getUniqueId())&&!eligible(p))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true) public void target(EntityTargetLivingEntityEvent e){if(npcs.containsKey(e.getEntity().getUniqueId())&&(!(e.getTarget() instanceof Player p)||!eligible(p)))e.setCancelled(true);}
 @EventHandler public void death(EntityDeathEvent e){if(npcs.remove(e.getEntity().getUniqueId())!=null){e.getDrops().clear();e.setDroppedExp(0);}}
 @EventHandler(ignoreCancelled=true) public void transform(EntityTransformEvent e){if(npcs.containsKey(e.getEntity().getUniqueId()))e.setCancelled(true);}
 @EventHandler public void unload(ChunkUnloadEvent e){for(Npc n:new ArrayList<>(npcs.values()))if(n.mob.getLocation().getChunk().equals(e.getChunk())){n.mob.remove();npcs.remove(n.mob.getUniqueId());}}
 @EventHandler public void load(org.bukkit.event.world.EntitiesLoadEvent e){for(Entity entity:e.getEntities())if(entity.getPersistentDataContainer().has(marker)&&!npcs.containsKey(entity.getUniqueId()))entity.remove();}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void build(BlockPlaceEvent e){if(level(e.getBlock().getWorld().getName())>=0&&!getConfig().getBoolean("allow-building",false)&&!e.getPlayer().hasPermission("backrooms.admin"))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(level(e.getBlock().getWorld().getName())>=0&&!getConfig().getBoolean("allow-building",false)&&!e.getPlayer().hasPermission("backrooms.admin"))e.setCancelled(true);}
 @EventHandler public void respawn(PlayerRespawnEvent e){if(level(e.getPlayer().getWorld().getName())>=0){Location to=outside(e.getPlayer());if(to!=null)e.setRespawnLocation(to);}}
 @EventHandler(ignoreCancelled=true) public void interact(PlayerInteractEvent e){if(e.getClickedBlock()==null)return;var block=e.getClickedBlock();Player p=e.getPlayer();if(e.getAction()==Action.PHYSICAL&&level(block.getWorld().getName())>=0&&Rooms.exit(block.getX(),block.getZ())){e.setCancelled(true);int level=level(p.getWorld().getName());if(level<2)enter(p,level+1);else leave(p);}else if(e.getAction()==Action.RIGHT_CLICK_BLOCK){Location portal=getConfig().getLocation("portal");if(portal!=null&&portal.equals(block.getLocation())){e.setCancelled(true);enter(p,0);}}}
 @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){if(args.length==0){sender.sendMessage("/br summon <stalker|smiler|hound|watcher> • /br map <0|1|2> • /br enter <level> • /br leave");return true;}String sub=args[0].toLowerCase(Locale.ROOT);
  if(Set.of("summon","map","remove","setportal","reload").contains(sub)&&!sender.hasPermission("backrooms.admin")){sender.sendMessage("No permission.");return true;}
  if(sub.equals("reload")){reloadConfig();protection=new SpawnProtection(this);sender.sendMessage("Backrooms settings reloaded.");return true;}if(sub.equals("remove")){npcs.values().forEach(n->n.mob.remove());npcs.clear();sender.sendMessage("Removed tracked Backrooms entities.");return true;}if(sub.equals("status")){sender.sendMessage("Backrooms entities: "+npcs.size());return true;}if(!(sender instanceof Player p)){sender.sendMessage("Run this command in game.");return true;}
  switch(sub){case "summon" -> {var type=args.length>1?NpcType.find(args[1]):Optional.<NpcType>empty();if(type.isEmpty())p.sendMessage("Choose stalker, smiler, hound or watcher.");else summon(type.get(),p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3)),p);}
   case "enter","map" -> {int destination=0;try{if(args.length>1)destination=Integer.parseInt(args[1]);if(destination<0||destination>2)throw new NumberFormatException();enter(p,destination);}catch(NumberFormatException e){p.sendMessage("Choose level 0, 1 or 2.");}}
   case "leave" -> leave(p);
   case "setportal" -> {var block=p.getTargetBlockExact(5);if(block==null)p.sendMessage("Look at the block players should right-click to enter.");else{getConfig().set("portal",block.getLocation());saveConfig();p.sendMessage("Entry block saved.");}}
   default -> p.sendMessage("/br summon, map, enter, leave, status, setportal, remove, reload");
  }return true;
 }
 @Override public List<String> onTabComplete(CommandSender sender,Command cmd,String alias,String[] args){List<String> choices=List.of();if(args.length==1)choices=sender.hasPermission("backrooms.admin")?List.of("summon","map","enter","leave","status","setportal","remove","reload"):List.of("enter","leave","status");else if(args.length==2&&args[0].equalsIgnoreCase("summon")&&sender.hasPermission("backrooms.admin"))choices=Arrays.stream(NpcType.values()).map(NpcType::id).toList();else if(args.length==2&&(args[0].equalsIgnoreCase("enter")||args[0].equalsIgnoreCase("map")))choices=List.of("0","1","2");String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return choices.stream().filter(s->s.startsWith(prefix)).toList();}
}
