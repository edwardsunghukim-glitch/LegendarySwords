package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;
final class ArmorAbilities implements AutoCloseable {
    private final JavaPlugin plugin;private final ArmorCatalog catalog;
    private final Map<UUID,Long> cooldowns=new HashMap<>();private final Set<Scene> scenes=new HashSet<>();
    ArmorAbilities(JavaPlugin plugin,ArmorCatalog catalog){this.plugin=plugin;this.catalog=catalog;}
    static boolean inCone(Vector origin,Vector direction,Vector target,double distance,double cosine) {
        Vector delta=target.clone().subtract(origin);return delta.lengthSquared()<=distance*distance&&(delta.lengthSquared()<.01||delta.normalize().dot(direction.clone().normalize())>=cosine);
    }
    boolean eligible(Player owner,LivingEntity target) {
        if(target==owner||target.isDead()||target instanceof ArmorStand)return false;
        if(target instanceof Player other) {
            if(!plugin.getConfig().getBoolean("pvp",true)||!owner.getWorld().getPVP()||other.getGameMode()==GameMode.CREATIVE||other.getGameMode()==GameMode.SPECTATOR||!owner.canSee(other))return false;
            var team=owner.getScoreboard().getEntryTeam(owner.getName());
            if(!plugin.getConfig().getBoolean("friendly-fire",false)&&team!=null&&team.hasEntry(other.getName()))return false;
        }return true;
    }
    void activate(Player player,ArmorSet set) {
        if(!plugin.getConfig().getBoolean("abilities-enabled",true)){player.sendMessage("Armor abilities are disabled.");return;}
        if(player.getGameMode()==GameMode.SPECTATOR||player.isDead())return;
        if(player.getWorld().isChunkLoaded(player.getLocation().getBlockX()>>4,player.getLocation().getBlockZ()>>4)==false)return;
        long now=System.currentTimeMillis(),until=cooldowns.getOrDefault(player.getUniqueId(),0L);
        if(until>now){player.sendActionBar(ArmorCatalog.text("Armor ability: "+((until-now+999)/1000)+"s cooldown",set.color));return;}
        cooldowns.put(player.getUniqueId(),now+Math.max(1,Math.min(3600,plugin.getConfig().getLong("sets."+set.id+".cooldown-seconds",25)))*1000);
        Scene scene=new Scene(player,set);scenes.add(scene);scene.runTaskTimer(plugin,0,2);
        player.sendActionBar(ArmorCatalog.text(set.ability,set.color));
    }
    private final class Scene extends BukkitRunnable {
        final Player owner;final ArmorSet set;final Location origin;final Vector direction;
        final List<ItemDisplay> visuals=new ArrayList<>();final Set<UUID> struck=new HashSet<>();int tick;
        Scene(Player owner,ArmorSet set){this.owner=owner;this.set=set;origin=owner.getLocation().clone();direction=owner.getEyeLocation().getDirection().normalize();}
        ItemDisplay mesh(String name,Location at,float size) {
            if(!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))return null;
            ItemDisplay entity=at.getWorld().spawn(at,ItemDisplay.class,d->{
                d.setItemStack(ArmorCosmetics.model(name));d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setPersistent(false);d.setGravity(false);d.setBrightness(new Display.Brightness(15,15));d.setTeleportDuration(2);
                var transform=d.getTransformation();transform.getScale().set(size);d.setTransformation(transform);
            });visuals.add(entity);return entity;
        }
        void finish(){for(ItemDisplay mesh:visuals)mesh.remove();visuals.clear();scenes.remove(this);cancel();}
        @Override public void run() {
            if(!owner.isOnline()||owner.isDead()||catalog.fullSet(owner.getInventory().getArmorContents())!=set||!owner.getWorld().equals(origin.getWorld())){finish();return;}
            if(tick==0){
                owner.getWorld().playSound(origin,set==ArmorSet.STONE?Sound.ENTITY_IRON_GOLEM_ATTACK:set==ArmorSet.DRAGON?Sound.ENTITY_ENDER_DRAGON_GROWL:Sound.BLOCK_AMETHYST_BLOCK_RESONATE,1,.7f);
                if(set==ArmorSet.ANGEL){heal(owner,value("healing",6));mesh("sunburst",origin.clone().add(0,.12,0),16);}
                if(set==ArmorSet.FROST)for(int i=0;i<12;i++){double angle=i*Math.PI/6;mesh("frost_crystal",origin.clone().add(Math.cos(angle)*6,.75,Math.sin(angle)*6),1.5f);}
                if(set==ArmorSet.LEVIATHAN)mesh("tidal_wave",origin.clone().add(0,1,0),5);
            }
            if(set==ArmorSet.DRAGON)breath();else if(set==ArmorSet.LEVIATHAN)wave();else domain();
            tick+=2;if(tick>=40&&!isCancelled())finish();
        }
        double value(String name,double fallback){double value=plugin.getConfig().getDouble("sets."+set.id+"."+name,fallback);return Double.isFinite(value)?Math.clamp(value,0,1000):fallback;}
        boolean damage(LivingEntity target,double amount){
            if(amount<=0||!eligible(owner,target))return false;
            var before=target.getLastDamageCause();target.damage(amount,owner);var after=target.getLastDamageCause();
            return after!=null&&after!=before&&!after.isCancelled();
        }
        void breath() {
            Location eye=owner.getEyeLocation();Vector aim=eye.getDirection().normalize();
            for(int step=2;step<=16;step+=2) {
                Location point=eye.clone().add(aim.clone().multiply(step));if(!point.getBlock().isPassable())break;
                point.getWorld().spawnParticle(Particle.FLAME,point,12,.3+step*.035,.3+step*.035,.3+step*.035,.03);
                if(tick%8==0&&step%4==0)mesh("dragon_flame",point,(float)(.5+step*.05));
            }
            if(tick%8!=0)return;
            for(Entity entity:owner.getNearbyEntities(18,18,18))if(entity instanceof LivingEntity target&&eligible(owner,target)&&owner.hasLineOfSight(target)
                    &&inCone(eye.toVector(),aim,target.getEyeLocation().toVector(),17,.85)){
                if(damage(target,value("damage",4)))target.setFireTicks((int)value("fire-seconds",4)*20);
            }
        }
        void wave() {
            Vector aim=direction.clone();aim.setY(0);if(aim.lengthSquared()<.01){double yaw=Math.toRadians(origin.getYaw());aim.setX(-Math.sin(yaw));aim.setZ(Math.cos(yaw));}aim.normalize();
            Location center=origin.clone().add(aim.multiply(tick*.5)).add(0,1,0);
            if(!center.getBlock().isPassable()){finish();return;}
            if(!visuals.isEmpty())visuals.getFirst().teleport(center);
            center.getWorld().spawnParticle(Particle.SPLASH,center,45,2,1,.5,.1);
            for(Entity entity:center.getWorld().getNearbyEntities(center,3,2,3))if(entity instanceof LivingEntity target&&eligible(owner,target)&&struck.add(target.getUniqueId())){
                if(damage(target,value("damage",14)))target.setVelocity(direction.clone().multiply(1.1).setY(.35));
            }
        }
        void domain() {
            double radius=set==ArmorSet.STONE?Math.min(10,1+tick*.3):6;
            for(int i=0;i<64;i++) {
                double angle=i*Math.PI/32;Location point=origin.clone().add(Math.cos(angle)*radius,.25,Math.sin(angle)*radius);
                origin.getWorld().spawnParticle(Particle.DUST,point,1,0,0,0,0,new Particle.DustOptions(Color.fromRGB(set.color),1.4f));
                if(set==ArmorSet.FROST&&tick%8==0)origin.getWorld().spawnParticle(Particle.SNOWFLAKE,point.clone().add(0,2,0),3,.1,1,.1,.01);
            }
            if(set==ArmorSet.STONE&&tick%8==0)for(int i=0;i<6;i++){double a=i*Math.PI/3;mesh("stone_spike",origin.clone().add(Math.cos(a)*radius,.75,Math.sin(a)*radius),1.5f);}
            if(tick%10!=0)return;
            for(Entity entity:origin.getWorld().getNearbyEntities(origin,radius,4,radius))if(entity instanceof LivingEntity target&&eligible(owner,target)&&target.getLocation().distanceSquared(origin)<=radius*radius&&owner.hasLineOfSight(target)) {
                if(set!=ArmorSet.FROST&&!struck.add(target.getUniqueId()))continue;
                if(!damage(target,value("damage",set==ArmorSet.STONE?20:set==ArmorSet.ANGEL?8:5)))continue;
                if(set==ArmorSet.FROST)target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,(int)value("slow-seconds",4)*20,2));
                if(set==ArmorSet.STONE)target.setVelocity(outward(target.getLocation().toVector(),origin.toVector()));
            }
        }
    }
    static Vector outward(Vector target,Vector origin){Vector delta=target.clone().subtract(origin);if(delta.lengthSquared()<.01)return new Vector(0,.55,0);return delta.normalize().multiply(.7).setY(.55);}
    static void heal(Player player,double amount){var max=player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);if(max!=null&&!player.isDead())player.setHealth(Math.min(max.getValue(),player.getHealth()+Math.max(0,amount)));}
    @Override public void close(){for(Scene scene:new ArrayList<>(scenes))scene.finish();cooldowns.clear();}
}
