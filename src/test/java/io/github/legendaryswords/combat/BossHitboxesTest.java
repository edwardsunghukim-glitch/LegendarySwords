package io.github.legendaryswords.combat;
import io.github.legendaryswords.sword.SwordType;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossHitboxesTest {
    @Test void bodyHitRedirectsOnceToOwnerAndCancelsReceiverDamage() throws Exception {
        var plugin=org.mockito.Mockito.mock(io.github.legendaryswords.LegendarySwordsPlugin.class);
        var combat=org.mockito.Mockito.mock(CombatService.class);var boxes=new BossHitboxes(plugin,combat);
        var owner=org.mockito.Mockito.mock(org.bukkit.entity.Player.class);var attacker=org.mockito.Mockito.mock(org.bukkit.entity.Player.class);var slime=org.mockito.Mockito.mock(org.bukkit.entity.Slime.class);
        java.util.UUID targetId=java.util.UUID.randomUUID(),ownerId=java.util.UUID.randomUUID(),attackerId=java.util.UUID.randomUUID();
        org.mockito.Mockito.when(slime.getUniqueId()).thenReturn(targetId);org.mockito.Mockito.when(owner.getUniqueId()).thenReturn(ownerId);org.mockito.Mockito.when(attacker.getUniqueId()).thenReturn(attackerId);
        org.mockito.Mockito.when(owner.isOnline()).thenReturn(true);org.mockito.Mockito.when(combat.eligible(attacker,owner)).thenReturn(true);
        Class<?> proxyClass=java.util.Arrays.stream(BossHitboxes.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("Proxy")).findFirst().orElseThrow();
        var constructor=proxyClass.getDeclaredConstructors()[0];constructor.setAccessible(true);
        var field=BossHitboxes.class.getDeclaredField("proxies");field.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.Map<java.util.UUID,Object> proxies=(java.util.Map<java.util.UUID,Object>)field.get(boxes);
        proxies.put(targetId,constructor.newInstance(owner,slime,new BossHitboxes.Part(0,3,0,4)));
        var e=org.mockito.Mockito.mock(org.bukkit.event.entity.EntityDamageByEntityEvent.class);
        org.mockito.Mockito.when(e.getEntity()).thenReturn(slime);org.mockito.Mockito.when(e.getDamager()).thenReturn(attacker);org.mockito.Mockito.when(e.getDamage()).thenReturn(8d);
        boxes.damage(e);boxes.damage(e);
        org.mockito.Mockito.verify(e,org.mockito.Mockito.times(2)).setCancelled(true);org.mockito.Mockito.verify(owner).damage(8d,attacker);
    }
    @Test void serpentProfilesCoverLongBodiesAndBothWings() {
        var dragon=BossHitboxes.parts(SwordType.INFERNAL_DRAGON);var leviathan=BossHitboxes.parts(SwordType.SKY_LEVIATHAN);
        assertTrue(dragon.stream().anyMatch(p->p.back()>=15));assertTrue(leviathan.stream().anyMatch(p->p.back()>=21));
        assertTrue(dragon.stream().anyMatch(p->p.right()< -4));assertTrue(dragon.stream().anyMatch(p->p.right()>4));
        assertTrue(leviathan.size()<=16);
    }
    @Test void coverageRotatesWithPlayerHeadingAndKeepsHeight() {
        var part=new BossHitboxes.Part(0,3,10,4);
        Location south=BossHitboxes.position(new Location(null,0,0,0,0,0),part);
        assertEquals(-10,south.getZ(),.001);assertEquals(1,south.getY(),.001);
        Location west=BossHitboxes.position(new Location(null,0,0,0,90,0),part);
        assertEquals(10,west.getX(),.001);assertEquals(0,west.getZ(),.001);
    }
}
