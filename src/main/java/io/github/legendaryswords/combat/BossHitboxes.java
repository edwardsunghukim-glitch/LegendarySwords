package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.sword.SwordType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import java.util.*;

/** Invisible damage receivers: visual-model coverage, not a replacement movement collider. */
final class BossHitboxes implements Listener,AutoCloseable {
    static final String MARKER="legendaryswords_boss_hitbox";
    private static final Map<UUID,BossHitboxes> RECEIVERS=new HashMap<>();
    record Part(double right,double up,double back,double width) {}
    private record Proxy(Player owner,Slime entity,Part part) {}
    private final LegendarySwordsPlugin plugin;
    private final CombatService combat;
    private final Map<UUID,Proxy> proxies=new HashMap<>();
    private final Map<UUID,List<Proxy>> owners=new HashMap<>();
    private final Map<String,Long> lastHits=new HashMap<>();
    private boolean registered;
    BossHitboxes(LegendarySwordsPlugin plugin,CombatService combat){this.plugin=plugin;this.combat=combat;}
    void register(){plugin.getServer().getPluginManager().registerEvents(this,plugin);registered=true;Bukkit.getOnlinePlayers().forEach(BossHealth::clear);}
    @EventHandler public void joined(PlayerJoinEvent e){BossHealth.clear(e.getPlayer());}
    void start(Player owner,SwordType sword) {
        if(!registered)return;
        stop(owner);
        BossHealth.boost(owner,plugin.getConfig().getDouble("bosses.health-bonus-by-form."+sword.id(),plugin.getConfig().getDouble("bosses.health-bonus",80)));
        if(!plugin.getConfig().getBoolean("bosses.hitboxes.enabled",true))return;
        List<Part> parts=parts(sword);
        var configured=plugin.getConfig().getMapList("bosses.hitboxes.parts."+sword.id());
        if(!configured.isEmpty()) {
            parts=new ArrayList<>();
            for(var row:configured)if(parts.size()<16)parts.add(new Part(number(row.get("right"),0),number(row.get("up"),2),number(row.get("back"),0),Math.max(.5,Math.min(8,number(row.get("width"),3)))));
        }
        List<Proxy> owned=new ArrayList<>();owners.put(owner.getUniqueId(),owned);
        for(Part part:parts) {
            Location at=position(owner.getLocation(),part);
            if(!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))continue;
            Slime entity=at.getWorld().spawn(at,Slime.class,s->{
                s.setMetadata(MARKER,new FixedMetadataValue(plugin,true));
                s.setSize((int)Math.ceil(part.width/.52));s.setAI(false);s.setGravity(false);s.setSilent(true);s.setInvisible(true);
                s.setCollidable(false);s.setPersistent(false);s.setRemoveWhenFarAway(false);s.setCanPickupItems(false);
            });
            Proxy proxy=new Proxy(owner,entity,part);owned.add(proxy);proxies.put(entity.getUniqueId(),proxy);
            RECEIVERS.put(entity.getUniqueId(),this);
            // The owner must be able to aim through their own body receivers.
            owner.hideEntity(plugin,entity);
        }
    }
    private static double number(Object value,double fallback){return value instanceof Number n&&Double.isFinite(n.doubleValue())?Math.max(-32,Math.min(32,n.doubleValue())):fallback;}
    static Location position(Location origin,Part part) {
        double yaw=Math.toRadians(origin.getYaw());
        return origin.clone().add(-Math.cos(yaw)*part.right+Math.sin(yaw)*part.back,part.up-part.width/2,-Math.sin(yaw)*part.right-Math.cos(yaw)*part.back);
    }
    static List<Part> parts(SwordType sword) {
        List<Part> out=new ArrayList<>();
        if(sword==SwordType.INFERNAL_DRAGON||sword==SwordType.SKY_LEVIATHAN) {
            int length=sword==SwordType.SKY_LEVIATHAN?21:15;
            for(int back=-3;back<=length;back+=3)out.add(new Part(Math.sin(back*.35)*1.5,3,back,3.5));
            out.add(new Part(-4.5,4,3,4));out.add(new Part(4.5,4,3,4));
        } else {
            out.add(new Part(0,2,0,4));out.add(new Part(0,5,0,4));out.add(new Part(0,8,0,3));
            double reach=sword==SwordType.ANGEL_SOVEREIGN?5.5:3;
            out.add(new Part(-reach,5,0,4));out.add(new Part(reach,5,0,4));
        }
        return List.copyOf(out);
    }
    void tick(Player owner) {
        for(Proxy proxy:owners.getOrDefault(owner.getUniqueId(),List.of())) {
            Location at=position(owner.getLocation(),proxy.part);
            if(proxy.entity.isValid()&&at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4)){
                proxy.entity.setFireTicks(0);proxy.entity.teleport(at);
            }
        }
    }
    void stop(Player owner) {
        if(!registered)return;
        for(Proxy proxy:owners.getOrDefault(owner.getUniqueId(),List.of())){RECEIVERS.remove(proxy.entity.getUniqueId());proxies.remove(proxy.entity.getUniqueId());proxy.entity.remove();}
        owners.remove(owner.getUniqueId());BossHealth.clear(owner);
        String prefix=owner.getUniqueId()+":";lastHits.keySet().removeIf(k->k.startsWith(prefix));
    }
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void damage(EntityDamageEvent e) {
        Proxy proxy=proxies.get(e.getEntity().getUniqueId());if(proxy==null)return;e.setCancelled(true);
        if(!(e instanceof EntityDamageByEntityEvent hit)||!proxy.owner.isOnline()||proxy.owner.isDead())return;
        Entity source=hit.getDamager();Player attacker=source instanceof Player p?p:source instanceof Projectile projectile&&projectile.getShooter() instanceof Player p?p:null;
        if(attacker!=null&&!combat.eligible(attacker,proxy.owner))return;
        if(source.getUniqueId().equals(proxy.owner.getUniqueId()))return;
        if(!claim(proxy.owner.getUniqueId(),attacker==null?source.getUniqueId():attacker.getUniqueId()))return;
        proxy.owner.damage(e.getDamage(),source);
    }
    private boolean claim(UUID owner,UUID attacker) {
        String key=owner+":"+attacker;long now=System.currentTimeMillis();
        if(now-lastHits.getOrDefault(key,0L)<400)return false;lastHits.put(key,now);return true;
    }
    static Player owner(Entity entity) {
        BossHitboxes service=RECEIVERS.get(entity.getUniqueId());
        Proxy proxy=service==null?null:service.proxies.get(entity.getUniqueId());return proxy==null?null:proxy.owner;
    }
    static boolean claimHit(Entity entity,Player attacker) {
        BossHitboxes service=RECEIVERS.get(entity.getUniqueId());Player owner=owner(entity);
        return service!=null&&owner!=null&&service.claim(owner.getUniqueId(),attacker.getUniqueId());
    }
    @EventHandler(ignoreCancelled=true) public void projectile(ProjectileHitEvent e) {
        if(e.getHitEntity()==null)return;Proxy proxy=proxies.get(e.getHitEntity().getUniqueId());
        if(proxy!=null&&e.getEntity().getShooter() instanceof Player p&&p.getUniqueId().equals(proxy.owner.getUniqueId()))e.setCancelled(true);
    }
    @EventHandler public void drops(EntityDeathEvent e){if(e.getEntity().hasMetadata(MARKER)){e.getDrops().clear();e.setDroppedExp(0);}}
    @Override public void close(){if(!registered)return;for(var owned:new ArrayList<>(owners.values()))if(!owned.isEmpty())stop(owned.get(0).owner);RECEIVERS.entrySet().removeIf(e->e.getValue()==this);proxies.clear();owners.clear();lastHits.clear();HandlerList.unregisterAll(this);registered=false;}
}
