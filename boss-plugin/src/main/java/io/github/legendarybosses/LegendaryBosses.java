package io.github.legendarybosses;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

public final class LegendaryBosses extends JavaPlugin implements Listener {
 private final Map<UUID,Boss> bosses=new LinkedHashMap<>();private int tick;private NamespacedKey marker;private BossDamageGuard damageGuard;
 private static final AnimationModifier LOOP=AnimationModifier.builder().type(AnimationIterator.Type.LOOP).priority(10).build();
 private static final AnimationModifier ACTION=AnimationModifier.builder().type(AnimationIterator.Type.PLAY_ONCE).priority(30).override(true).build();
 private static final class Boss {
  final BossType type;final Zombie entity;final EntityTracker model;final BossBar bar;final Location home;final int born;final boolean flying;int nextAttack,attackIndex;Scene scene;
  Boss(BossType type,Zombie entity,EntityTracker model,int tick,boolean flying){this.flying=flying;this.type=type;this.entity=entity;this.model=model;home=entity.getLocation();born=tick;nextAttack=tick+60;bar=Bukkit.createBossBar(type.title,type.color,BarStyle.SEGMENTED_10);}
 }
 private static final class Scene {final Location origin,target;final Vector direction;final int style;int age;Scene(Location origin,Location target,int style){this.origin=origin;this.target=target;this.style=style;direction=target.toVector().subtract(origin.toVector());if(direction.lengthSquared()<.01)direction.setZ(1);direction.normalize();}}
 @Override public void onEnable(){saveDefaultConfig();marker=new NamespacedKey(this,"boss");getServer().getPluginManager().registerEvents(this,this);damageGuard=new BossDamageGuard(id->{Boss b=bosses.get(id);return b==null?null:b.type;},()->option("incoming-damage-multiplier",.35,.01,1));getServer().getPluginManager().registerEvents(damageGuard,this);
  // A boss encounter never resumes as an untracked vanilla mob after a crash/restart.
  for(World world:Bukkit.getWorlds())for(Entity entity:world.getEntities())if(entity.getPersistentDataContainer().has(marker))entity.remove();
  getServer().getScheduler().runTaskTimer(this,this::update,2,2);
 }
 @Override public void onDisable(){for(Boss b:new ArrayList<>(bosses.values()))remove(b);}
 private double option(String path,double fallback,double min,double max){return BossRules.bounded(getConfig().getDouble(path,fallback),fallback,min,max);}
 private double setting(Boss b,String name,double fallback,double min,double max){return option("bosses."+b.type.id()+"."+name,fallback,min,max);}
 private boolean flying(Boss b){return b.flying;}
 private void remove(Boss b){bosses.remove(b.entity.getUniqueId());b.scene=null;b.bar.removeAll();b.model.close();b.entity.remove();}
 private void summon(Player at,BossType type,CommandSender sender){
  if(bosses.size()>=option("max-active-bosses",5,1,30)){sender.sendMessage("Active boss limit reached. /boss remove all");return;}
  String id=getConfig().getString("bosses."+type.id()+".model","legendaryswords_"+type.id()+"_boss");var renderer=BetterModel.modelOrNull(id);
  if(renderer==null){sender.sendMessage("Model not loaded: "+id+". Install the five LegendarySwords boss models and run /bm reload.");return;}
  Location location=at.getLocation().add(at.getLocation().getDirection().setY(0).normalize().multiply(5));location.setPitch(0);
  Zombie mob=location.getWorld().spawn(location,Zombie.class,z->{z.setAdult();z.setPersistent(false);z.setRemoveWhenFarAway(false);z.setShouldBurnInDay(false);z.setCanPickupItems(false);z.getEquipment().clear();z.getPersistentDataContainer().set(marker,PersistentDataType.STRING,type.id());z.getAttribute(Attribute.MAX_HEALTH).setBaseValue(BossRules.health(getConfig().getDouble("health-hearts",100)));z.setHealth(z.getAttribute(Attribute.MAX_HEALTH).getValue());z.getAttribute(Attribute.SCALE).setBaseValue(3);z.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1);z.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(.23);z.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(0);});
  try {if(!mob.isValid())throw new IllegalStateException("Mob spawn was cancelled by another plugin");
   EntityTracker model=renderer.create(BukkitAdapter.adapt(mob),new TrackerModifier(false,false,true),t->{t.hideOption(EntityHideOption.DEFAULT);t.scaler(ModelScaler.value((float)option("bosses."+type.id()+".scale",1.5,.25,5)));});
   Boss b=new Boss(type,mob,model,tick,getConfig().getBoolean("bosses."+type.id()+".flying",type==BossType.ANGEL||type==BossType.DRAGON||type==BossType.LEVIATHAN));bosses.put(mob.getUniqueId(),b);mob.setGravity(!flying(b));mob.setAI(!flying(b));model.animate("idle",LOOP);model.animate("transform",ACTION);sender.sendMessage("Summoned "+type.title+" ("+(int)(mob.getHealth()/2)+" hearts). /boss list");
  }catch(RuntimeException|LinkageError error){mob.remove();getLogger().warning("Boss model failed: "+error);sender.sendMessage("Boss could not be rendered. Check the console and BetterModel version.");}
 }
 private List<Player> nearby(Boss b,double range){return b.entity.getWorld().getPlayers().stream().filter(p->!p.isDead()&&(p.getGameMode()==GameMode.SURVIVAL||p.getGameMode()==GameMode.ADVENTURE)&&p.getLocation().distanceSquared(b.home)<Math.pow(option("leash-range",64,16,256),2)&&p.getLocation().distanceSquared(b.entity.getLocation())<range*range).toList();}
 private void update(){tick+=2;for(Boss b:new ArrayList<>(bosses.values())){
  if(!b.entity.isValid()||b.entity.isDead()||b.model.isClosed()||tick-b.born>option("lifetime-seconds",1800,30,86400)*20){remove(b);continue;}
  List<Player> players=nearby(b,option("target-range",40,8,128));for(Player p:new ArrayList<>(b.bar.getPlayers()))if(!players.contains(p))b.bar.removePlayer(p);for(Player p:players)if(!b.bar.getPlayers().contains(p))b.bar.addPlayer(p);
  b.bar.setProgress(Math.clamp(b.entity.getHealth()/b.entity.getAttribute(Attribute.MAX_HEALTH).getValue(),0,1));
  Player target=players.stream().filter(b.entity::hasLineOfSight).min(Comparator.comparingDouble(p->p.getLocation().distanceSquared(b.entity.getLocation()))).orElse(null);
  if(b.entity.getLocation().distanceSquared(b.home)>Math.pow(option("leash-range",64,16,256),2)){b.entity.teleport(b.home);b.scene=null;}
  if(flying(b)){Location goal=target==null?b.home.clone().add(0,setting(b,"flight-height",5,2,16),0):target.getLocation().add(0,setting(b,"flight-height",5,2,16),0);Vector move=goal.toVector().subtract(b.entity.getLocation().toVector());if(move.length()>1)move.normalize().multiply(.3);else move.multiply(.12);if(!b.entity.getLocation().add(move.clone().multiply(2)).getBlock().isPassable())move.setY(.35);b.entity.setVelocity(move);}
  else b.entity.setTarget(target);
  if(target!=null){Location face=b.entity.getLocation();face.setDirection(target.getEyeLocation().toVector().subtract(b.entity.getEyeLocation().toVector()));b.entity.setRotation(face.getYaw(),0);}
  if(b.scene!=null)scene(b);
  else if(target!=null&&tick>=b.nextAttack){int attack=b.attackIndex++%4;b.scene=new Scene(b.entity.getLocation().add(0,1,0),target.getLocation(),attack);b.bar.setTitle(b.type.title+" • "+b.type.attacks.get(attack));b.model.animate(attack==0&&b.type==BossType.DRAGON?"breath":"smash",ACTION);b.nextAttack=tick+(int)(setting(b,"attack-cooldown-seconds",4,1,60)*20);}
 }}
 private void particles(Boss b,Location location,int count){if(b.type.particle==Particle.BLOCK)location.getWorld().spawnParticle(Particle.BLOCK,location,count,.1,.1,.1,0,Material.DEEPSLATE.createBlockData());else location.getWorld().spawnParticle(b.type.particle,location,count,.12,.12,.12,.02);}
 private void ring(Boss b,Location center,double radius){for(int i=0;i<48;i++){double angle=i*Math.PI/24;particles(b,center.clone().add(Math.cos(angle)*radius,.15,Math.sin(angle)*radius),1);}}
 private void damage(Boss b,Location center,double radius,double multiplier,boolean slow){for(Player p:nearby(b,60))if(p.getLocation().distanceSquared(center)<=radius*radius){double old=p.getHealth();damageGuard.attack(b.entity,()->p.damage(setting(b,"damage",14,0,200)*multiplier,b.entity));if(p.getHealth()<old){if(b.type==BossType.DRAGON)p.setFireTicks(Math.max(60,p.getFireTicks()));Vector v=p.getLocation().toVector().subtract(center.toVector()).setY(.4);if(v.lengthSquared()>.01)p.setVelocity(v.normalize().multiply(.65));if(slow)p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,1));}}}
 private void scene(Boss b){Scene s=b.scene;s.age+=2;
  // Twelve-tick warning gives players a visible dodge window before impact.
  if(s.age<=12){ring(b,s.target,2+s.age/6.0);return;}
  int age=s.age-12;switch(b.type){
   case DRAGON -> {switch(s.style){
    case 0 -> {for(double d=1;d<=18;d+=.6){Location at=s.origin.clone().add(s.direction.clone().multiply(d));if(!at.getBlock().isPassable())break;particles(b,at,(int)(2+d/4));if(age%10==2)damage(b,at,1.5, .5,false);}}
    case 1 -> {arc(b,s,age,8);if(age==2)damage(b,s.target,5,1.2,false);}
    case 2 -> rain(b,s,age,false);
    case 3 -> {ring(b,s.target,age/4.0);if(age%10==2)damage(b,s.target,age/4.0, .5,false);}
   }}
   case LEVIATHAN -> {switch(s.style){
    case 0 -> {Location wave=s.origin.clone().add(s.direction.clone().multiply(age*.55));ring(b,wave,3);if(age%6==2)damage(b,wave,3,.6,false);}
    case 1 -> {ring(b,s.target,6);for(Player p:nearby(b,60))if(p.getLocation().distanceSquared(s.target)<36){Vector pull=s.target.toVector().subtract(p.getLocation().toVector());if(pull.lengthSquared()>.1)p.setVelocity(pull.normalize().multiply(.25));}if(age%12==2)damage(b,s.target,6,.4,true);}
    case 2 -> {arc(b,s,age,6);if(age==2)damage(b,s.target,4,1.4,false);}
    case 3 -> {for(int i=0;i<4;i++)ring(b,s.target.clone().add(0,i,0),8);if(age%16==2)damage(b,s.target,8,.5,true);}
   }}
   case STONE -> {switch(s.style){
    case 0 -> {ring(b,s.target,age/3.0);spikes(b,s.target,age);if(age==14)damage(b,s.target,7,1.2,false);}
    case 1 -> rain(b,s,age,false);
    case 2 -> {spikes(b,s.target,age);if(age==2)damage(b,s.target,4,1.5,false);}
    case 3 -> {ring(b,s.target,8);spikes(b,s.target,age);if(age%14==2)damage(b,s.target,8,.5,true);}
   }}
   case FROST -> {switch(s.style){
    case 0 -> {arc(b,s,age,8);if(age==2)damage(b,s.target,5,1,true);}
    case 1 -> {for(int y=0;y<5;y++)ring(b,s.target.clone().add(0,y*.7,0),4);if(age%14==2)damage(b,s.target,4,.45,true);}
    case 2 -> rain(b,s,age,true);
    case 3 -> {for(int i=1;i<4;i++)ring(b,s.target,2*i+Math.sin(age*.2));if(age%14==2)damage(b,s.target,7,.5,true);}
   }}
   case ANGEL -> {switch(s.style){
    case 0 -> {arc(b,s,age,7);if(age==2)damage(b,s.target,5,1,false);}
    case 1 -> {ring(b,s.target,6);for(int i=0;i<8;i++)particles(b,s.target.clone().add(Math.cos(i*Math.PI/4)*6,age*.15,Math.sin(i*Math.PI/4)*6),3);if(age==20)damage(b,s.target,6,1.3,false);}
    case 2 -> rain(b,s,age,false);
    case 3 -> {ring(b,s.origin,age*.25);if(age%12==2)damage(b,s.origin,age*.25,.5,false);if(age==2)b.entity.setHealth(Math.min(b.entity.getAttribute(Attribute.MAX_HEALTH).getValue(),b.entity.getHealth()+6));}
   }}
  }
  if(age>=40){b.scene=null;b.bar.setTitle(b.type.title);}
 }
 private void arc(Boss b,Scene s,int age,double radius){double yaw=Math.atan2(s.direction.getZ(),s.direction.getX());for(int i=0;i<25;i++){double angle=yaw-Math.PI/2+i*Math.PI/24;particles(b,s.target.clone().add(Math.cos(angle)*radius,.5+Math.sin(age*.2),Math.sin(angle)*radius),2);}}
 private void spikes(Boss b,Location at,int age){for(int i=0;i<8;i++){double angle=i*Math.PI/4;for(int y=0;y<4;y++)particles(b,at.clone().add(Math.cos(angle)*3,y*.6,Math.sin(angle)*3),2);}}
 private void rain(Boss b,Scene s,int age,boolean slow){for(int i=0;i<5;i++){double a=i*Math.PI*2/5;Location at=s.target.clone().add(Math.cos(a)*4,Math.max(0,9-age*.3),Math.sin(a)*4);particles(b,at,4);}if(age==30){ring(b,s.target,7);damage(b,s.target,7,1.2,slow);s.target.getWorld().playSound(s.target,Sound.ENTITY_GENERIC_EXPLODE,1,.7f);}}
 @EventHandler(ignoreCancelled=true) public void transform(EntityTransformEvent event){if(bosses.containsKey(event.getEntity().getUniqueId()))event.setCancelled(true);}
 @EventHandler public void death(EntityDeathEvent event){Boss b=bosses.remove(event.getEntity().getUniqueId());if(b==null)return;b.bar.removeAll();b.model.close();event.getDrops().clear();event.setDroppedExp(0);if(BossRules.reward(getConfig().getBoolean("require-player-kill",true),b.entity.getKiller()!=null)){var armor=getServer().getPluginManager().getPlugin("LegendaryArmor");boolean unbreakable=armor instanceof JavaPlugin p?p.getConfig().getBoolean("unbreakable",true):true;event.getDrops().add(ArmorLoot.create(b.type,java.util.concurrent.ThreadLocalRandom.current().nextInt(4),unbreakable));}}
 @EventHandler public void unload(ChunkUnloadEvent event){for(Boss b:new ArrayList<>(bosses.values()))if(b.entity.getLocation().getChunk().equals(event.getChunk()))remove(b);}
 @EventHandler public void load(org.bukkit.event.world.EntitiesLoadEvent event){for(Entity e:event.getEntities())if(e.getPersistentDataContainer().has(marker)&&!bosses.containsKey(e.getUniqueId()))e.remove();}
 @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){if(!sender.hasPermission("legendarybosses.admin")){sender.sendMessage("No permission.");return true;}if(args.length==0){sender.sendMessage("/boss summon <angel|dragon|leviathan|stone|frost> [player], list, remove <id|all>, reload");return true;}
  switch(args[0].toLowerCase(Locale.ROOT)){
   case "summon" -> {if(args.length<2){sender.sendMessage("Choose angel, dragon, leviathan, stone or frost.");break;}var type=BossType.find(args[1]);Player at=args.length>2?Bukkit.getPlayerExact(args[2]):sender instanceof Player p?p:null;if(type.isEmpty()||at==null){sender.sendMessage("Specify a valid boss and an online player (required from console).");break;}summon(at,type.get(),sender);}
   case "list" -> {sender.sendMessage("Active bosses: "+bosses.size());for(Boss b:bosses.values())sender.sendMessage(b.type.id()+" "+b.entity.getUniqueId()+" • "+(int)(b.entity.getHealth()/2)+" hearts");}
   case "remove" -> {if(args.length!=2){sender.sendMessage("/boss remove <id|all>");break;}if(args[1].equalsIgnoreCase("all")){for(Boss b:new ArrayList<>(bosses.values()))remove(b);sender.sendMessage("Removed all bosses without rewards.");}else try{Boss b=bosses.get(UUID.fromString(args[1]));if(b==null)sender.sendMessage("Boss not found.");else{remove(b);sender.sendMessage("Boss removed without rewards.");}}catch(IllegalArgumentException e){sender.sendMessage("Use an ID from /boss list.");}}
   case "reload" -> {reloadConfig();sender.sendMessage("Config reloaded. Health and size changes apply to newly summoned bosses.");}
   default -> sender.sendMessage("/boss summon, list, remove, reload");
  }return true;
 }
 @Override public List<String> onTabComplete(CommandSender sender,Command cmd,String alias,String[] args){if(!sender.hasPermission("legendarybosses.admin"))return List.of();List<String> choices=List.of();if(args.length==1)choices=List.of("summon","list","remove","reload");else if(args.length==2&&args[0].equalsIgnoreCase("summon"))choices=Arrays.stream(BossType.values()).map(BossType::id).toList();else if(args.length==2&&args[0].equalsIgnoreCase("remove")){choices=new ArrayList<>(List.of("all"));for(UUID id:bosses.keySet())choices.add(id.toString());}else if(args.length==3&&args[0].equalsIgnoreCase("summon"))choices=Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return choices.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();}
}
