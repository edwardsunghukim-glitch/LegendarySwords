package io.github.legendarybosses;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import java.util.UUID;
class BossDamageGuardTest {
 private final Zombie boss=mock(Zombie.class);
 private final UUID id=UUID.randomUUID();
 private BossDamageGuard guard(BossType type){when(boss.getUniqueId()).thenReturn(id);return new BossDamageGuard(key->key.equals(id)?type:null);}
 private EntityDamageEvent event(EntityDamageEvent.DamageCause cause){var event=mock(EntityDamageEvent.class);when(event.getEntity()).thenReturn(boss);when(event.getCause()).thenReturn(cause);return event;}
 @Test void thornsDuringAbilityIsBlockedButNormalThornsRemain(){var guard=guard(BossType.STONE);var reflected=event(EntityDamageEvent.DamageCause.THORNS);guard.attack(boss,()->guard.damage(reflected));verify(reflected).setCancelled(true);var ordinary=event(EntityDamageEvent.DamageCause.THORNS);guard.damage(ordinary);verify(ordinary,never()).setCancelled(true);}
 @Test void exceptionsDoNotLeaveBossImmuneToLaterPlayerDamage(){var guard=guard(BossType.FROST);assertThrows(IllegalStateException.class,()->guard.attack(boss,()->{throw new IllegalStateException();}));var reflected=event(EntityDamageEvent.DamageCause.THORNS);guard.damage(reflected);verify(reflected,never()).setCancelled(true);}
 @Test void directSelfHitsAndOwnProjectilesAreBlocked(){var guard=guard(BossType.ANGEL);var hit=mock(EntityDamageByEntityEvent.class);when(hit.getEntity()).thenReturn(boss);when(hit.getDamager()).thenReturn(boss);guard.damage(hit);verify(hit).setCancelled(true);var arrow=mock(Arrow.class);when(arrow.getShooter()).thenReturn(boss);var projectile=mock(EntityDamageByEntityEvent.class);when(projectile.getEntity()).thenReturn(boss);when(projectile.getDamager()).thenReturn(arrow);guard.damage(projectile);verify(projectile).setCancelled(true);}
 @Test void PlayerMeleeAndArrowsStillDamageBoss(){var guard=guard(BossType.DRAGON);Player player=mock(Player.class);when(player.getUniqueId()).thenReturn(UUID.randomUUID());var hit=mock(EntityDamageByEntityEvent.class);when(hit.getEntity()).thenReturn(boss);when(hit.getDamager()).thenReturn(player);guard.damage(hit);verify(hit,never()).setCancelled(true);var arrow=mock(Arrow.class);when(arrow.getShooter()).thenReturn(player);when(hit.getDamager()).thenReturn(arrow);guard.damage(hit);verify(hit,never()).setCancelled(true);}
 @Test void dragonFireCannotBurnItWhileOtherBossesRemainVulnerable(){var guard=guard(BossType.DRAGON);for(var cause:new EntityDamageEvent.DamageCause[]{EntityDamageEvent.DamageCause.FIRE,EntityDamageEvent.DamageCause.FIRE_TICK,EntityDamageEvent.DamageCause.LAVA,EntityDamageEvent.DamageCause.HOT_FLOOR}){var event=event(cause);guard.damage(event);verify(event).setCancelled(true);}var stone=guard(BossType.STONE);var fire=event(EntityDamageEvent.DamageCause.FIRE_TICK);stone.damage(fire);verify(fire,never()).setCancelled(true);}
 @Test void incomingDamageIsReducedWithoutChangingBossHealth(){when(boss.getUniqueId()).thenReturn(id);var guard=new BossDamageGuard(key->BossType.STONE,()->.35);var event=event(EntityDamageEvent.DamageCause.ENTITY_ATTACK);when(event.getDamage()).thenReturn(20.0);guard.damage(event);verify(event).setDamage(7.0);verify(event,never()).setCancelled(true);}
}
