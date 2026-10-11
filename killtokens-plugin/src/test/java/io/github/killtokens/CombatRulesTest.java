package io.github.killtokens;
import org.junit.jupiter.api.Test;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CombatRulesTest {
    private KillTokens plugin() {KillTokens plugin=mock(KillTokens.class,CALLS_REAL_METHODS);doReturn(new YamlConfiguration()).when(plugin).getConfig();
        try {for(String name:new String[]{"combat","opponents"}) {var field=KillTokens.class.getDeclaredField(name);field.setAccessible(true);field.set(plugin,new java.util.HashMap<>());}}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}return plugin;}
    @Test void hitTagsBothPlayersAndQuitKills() {
        KillTokens plugin=plugin();Player attacker=player(),victim=player();EntityDamageByEntityEvent hit=mock(EntityDamageByEntityEvent.class);
        when(hit.getEntity()).thenReturn(victim);when(hit.getDamager()).thenReturn(attacker);when(hit.getFinalDamage()).thenReturn(5.0);
        plugin.hit(hit);assertTrue(plugin.getCombatSeconds(attacker.getUniqueId())>0);assertTrue(plugin.getCombatSeconds(victim.getUniqueId())>0);
        PlayerQuitEvent quit=mock(PlayerQuitEvent.class);when(quit.getPlayer()).thenReturn(victim);plugin.quit(quit);verify(victim).setHealth(0);assertEquals(0,plugin.getCombatSeconds(victim.getUniqueId()));
    }
    @Test void pearlTagsThrowerAndKickPunishesButBypassDoesNotTag() {
        KillTokens plugin=plugin();Player player=player();EnderPearl pearl=mock(EnderPearl.class);when(pearl.getShooter()).thenReturn(player);
        ProjectileLaunchEvent launch=mock(ProjectileLaunchEvent.class);when(launch.getEntity()).thenReturn(pearl);plugin.pearl(launch);
        assertTrue(plugin.getCombatSeconds(player.getUniqueId())>0);PlayerKickEvent kick=mock(PlayerKickEvent.class);when(kick.getPlayer()).thenReturn(player);plugin.kicked(kick);verify(player).setHealth(0);
        Player bypass=player();when(bypass.hasPermission("killtokens.combat-bypass")).thenReturn(true);when(pearl.getShooter()).thenReturn(bypass);plugin.pearl(launch);assertEquals(0,plugin.getCombatSeconds(bypass.getUniqueId()));
    }
    @Test void zeroDamageDoesNotTag() {
        KillTokens plugin=plugin();EntityDamageByEntityEvent hit=mock(EntityDamageByEntityEvent.class);Player victim=player();when(hit.getEntity()).thenReturn(victim);plugin.hit(hit);assertEquals(0,plugin.getCombatSeconds(victim.getUniqueId()));
    }
    private Player player(){Player p=mock(Player.class);when(p.getUniqueId()).thenReturn(java.util.UUID.randomUUID());return p;}
}
