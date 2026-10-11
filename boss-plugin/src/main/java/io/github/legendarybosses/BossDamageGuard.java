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
    private final Set<UUID> attacking=new HashSet<>();
    BossDamageGuard(Function<UUID,BossType> types){this(types,()->1);}
    BossDamageGuard(Function<UUID,BossType> types,java.util.function.DoubleSupplier multiplier){this.types=types;this.multiplier=multiplier;}
    void attack(Entity boss,Runnable damage){
        UUID id=boss.getUniqueId();boolean added=attacking.add(id);
        try{damage.run();}finally{if(added)attacking.remove(id);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void damage(EntityDamageEvent event){
        UUID id=event.getEntity().getUniqueId();BossType type=types.apply(id);
        if(type==null)return;
        var cause=event.getCause();
        boolean blocked=cause==EntityDamageEvent.DamageCause.FALL
            ||cause==EntityDamageEvent.DamageCause.SUFFOCATION
            ||cause==EntityDamageEvent.DamageCause.THORNS&&attacking.contains(id);
        if(event instanceof EntityDamageByEntityEvent hit){
            Entity source=hit.getDamager();
            if(source instanceof Projectile projectile&&projectile.getShooter() instanceof Entity shooter)source=shooter;
            blocked|=source.getUniqueId().equals(id);
        }
        if(type==BossType.DRAGON)blocked|=cause==EntityDamageEvent.DamageCause.FIRE
            ||cause==EntityDamageEvent.DamageCause.FIRE_TICK
            ||cause==EntityDamageEvent.DamageCause.LAVA
            ||cause==EntityDamageEvent.DamageCause.HOT_FLOOR;
        if(blocked)event.setCancelled(true);
        else event.setDamage(event.getDamage()*BossRules.bounded(multiplier.getAsDouble(),.35,.01,1));
    }
}
