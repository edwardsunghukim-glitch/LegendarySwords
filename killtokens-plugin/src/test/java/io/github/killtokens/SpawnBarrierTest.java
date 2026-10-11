package io.github.killtokens;
import org.junit.jupiter.api.Test;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class SpawnBarrierTest {
    @Test void entryAndTeleportAcrossSpawnBlockedButExitAllowed() throws Exception {
        KillTokens plugin=mock(KillTokens.class);Server server=mock(Server.class);var manager=mock(org.bukkit.plugin.PluginManager.class);
        when(plugin.getServer()).thenReturn(server);when(server.getPluginManager()).thenReturn(manager);when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        SpawnBarrier barrier=new SpawnBarrier(plugin,p->true);WorldGuardRegion adapter=mock(WorldGuardRegion.class);
        var field=SpawnBarrier.class.getDeclaredField("regions");field.setAccessible(true);field.set(barrier,adapter);
        World world=mock(World.class);when(world.getUID()).thenReturn(java.util.UUID.randomUUID());Object region=new Object();
        when(adapter.find(world,"spawn")).thenReturn(region);when(adapter.contains(eq(region),any(Location.class))).thenAnswer(call->{double x=((Location)call.getArgument(1)).getX();return x>=0&&x<5;});
        Player player=mock(Player.class);Location outside=new Location(world,-2,64,0),inside=new Location(world,2,64,0),beyond=new Location(world,7,64,0);
        assertTrue(barrier.blocked(player,outside,inside));assertTrue(barrier.blocked(player,outside,beyond));assertFalse(barrier.blocked(player,inside,outside));assertFalse(barrier.blocked(player,outside,new Location(world,-4,64,0)));
    }
}
