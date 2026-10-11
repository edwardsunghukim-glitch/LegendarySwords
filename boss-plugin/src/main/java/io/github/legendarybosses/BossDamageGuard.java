package io.github.legendarybosses;

import java.util.*;
import java.util.function.Function;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;

/** Prevents self hits and damage reflected synchronously from a boss ability. */
final class BossDamageGuard implements Listener {
    private final Function<UUID,BossType> types;
    private final java.util.function.DoubleSupplier multiplier;
    private final java.util.function.DoubleSupplier hitLimit;
    private final Set<UUID> attacking=new HashSet<>();
    final Set<UUID> burning=new HashSet<>();
    java.util.function.Predicate<org.bukkit.Location> safe=location->false;
    BossDamageGuard(Function<UUID,BossType> types){this(types,()->1);}
    BossDamageGuard(Function<UUID,BossType> types,java.util.function.DoubleSupplier multiplier){this(types,multiplier,()->1000);}
    BossDamageGuard(Function<UUID,BossType> types,java.util.function.DoubleSupplier multiplier,java.util.function.DoubleSupplier hitLimit){this.types=types;this.multiplier=multiplier;this.hitLimit=hitLimit;}
    void attack(Entity boss,Runnable damage){
        UUID id=boss.getUniqueId();boolean added=attacking.add(id);
        try{damage.run();}finally{if(added)attacking.remove(id);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void damage(EntityDamageEvent event){
        UUID id=event.getEntity().getUniqueId();BossType type=types.apply(id);
        if(type==null){
            if(event.getEntity() instanceof Player player){
                boolean protectedPlayer=safe.test(player.getLocation());
                if(event instanceof EntityDamageByEntityEvent hit){Entity source=hit.getDamager();if(source instanceof Projectile projectile&&projectile.getShooter() instanceof Entity shooter)source=shooter;
                    if(protectedPlayer&&types.apply(source.getUniqueId())!=null)event.setCancelled(true);
                }
                if(burning.contains(id)){if(player.getFireTicks()<=0)burning.remove(id);else if(protectedPlayer&&event.getCause()==EntityDamageEvent.DamageCause.FIRE_TICK){event.setCancelled(true);player.setFireTicks(0);burning.remove(id);}}
            }return;
        }
        var cause=event.getCause();
        boolean blocked=cause==EntityDamageEvent.DamageCause.FALL
            ||cause==EntityDamageEvent.DamageCause.SUFFOCATION
            ||cause==EntityDamageEvent.DamageCause.THORNS
            ||attacking.contains(id);
        if(event instanceof EntityDamageByEntityEvent hit){
            Entity source=hit.getDamager();
            if(source instanceof Projectile projectile&&projectile.getShooter() instanceof Entity shooter)source=shooter;
            blocked|=!(source instanceof Player);
        }
        if(!(event instanceof EntityDamageByEntityEvent))blocked=true;
        if(type==BossType.DRAGON)blocked|=cause==EntityDamageEvent.DamageCause.FIRE
            ||cause==EntityDamageEvent.DamageCause.FIRE_TICK
            ||cause==EntityDamageEvent.DamageCause.LAVA
            ||cause==EntityDamageEvent.DamageCause.HOT_FLOOR;
        if(blocked)event.setCancelled(true);
        else event.setDamage(Math.min(BossRules.bounded(hitLimit.getAsDouble(),10,.1,1000),event.getDamage()*BossRules.bounded(multiplier.getAsDouble(),.35,.01,1)));
    }
}
