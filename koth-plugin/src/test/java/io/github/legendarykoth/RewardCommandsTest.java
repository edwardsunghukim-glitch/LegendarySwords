package io.github.legendarykoth;
import org.bukkit.command.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RewardCommandsTest {
    @Test void holderDeathOrDisconnectResetsProgressImmediately() throws Exception {
        LegendaryKoTH plugin=mock(LegendaryKoTH.class,CALLS_REAL_METHODS);
        CaptureState state=new CaptureState();java.util.UUID id=java.util.UUID.randomUUID();state.tick(java.util.Set.of(id),100,true);
        var field=LegendaryKoTH.class.getDeclaredField("capture");field.setAccessible(true);field.set(plugin,state);
        org.bukkit.entity.Player p=mock(org.bukkit.entity.Player.class);when(p.getUniqueId()).thenReturn(id);
        var death=mock(org.bukkit.event.entity.PlayerDeathEvent.class);when(death.getEntity()).thenReturn(p);plugin.died(death);assertNull(state.holder);
        state.tick(java.util.Set.of(id),100,true);
        var quit=mock(org.bukkit.event.player.PlayerQuitEvent.class);when(quit.getPlayer()).thenReturn(p);plugin.quit(quit);assertEquals(0,state.seconds);
    }
    @Test void winnerPlaceholdersAndOptionalSlashAreExpanded() {
        assertEquals("killtokengive Alex 10",LegendaryKoTH.rewardCommand(" /killtokengive %player% 10 ","Alex"));
        assertEquals("give Alex diamond 3",LegendaryKoTH.rewardCommand("give {player} diamond 3","Alex"));
    }
    @Test void managingEventAndSettingConsoleCommandsRequiresAdminPermission() {
        LegendaryKoTH plugin=mock(LegendaryKoTH.class,CALLS_REAL_METHODS);CommandSender sender=mock(CommandSender.class);
        for(String action:java.util.List.of("start","stop","setregion","setreward","reward","reload"))
            assertTrue(plugin.onCommand(sender,mock(Command.class),"koth",new String[]{action,"give","Alex","diamond"}));
        verify(sender,times(6)).sendMessage("No permission to manage KoTH.");verify(plugin,never()).getConfig();
    }
}
