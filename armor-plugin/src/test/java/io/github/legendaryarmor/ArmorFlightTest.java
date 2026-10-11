package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.metadata.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ArmorFlightTest {
    @Test void fullSetGrantsFlightAndRemovalRestoresSurvival() {
        JavaPlugin plugin=mock(JavaPlugin.class);Player player=player();ArmorFlight flight=new ArmorFlight(plugin);
        flight.update(player,true);verify(player).setAllowFlight(true);flight.update(player,false);verify(player).setAllowFlight(false);verify(player).setFlying(false);
    }
    @Test void preexistingFlightAndCreativeModeArePreserved() {
        JavaPlugin plugin=mock(JavaPlugin.class);Player player=player();when(player.getAllowFlight()).thenReturn(true);when(player.isFlying()).thenReturn(true);
        ArmorFlight flight=new ArmorFlight(plugin);flight.update(player,true);flight.clear(player);verify(player).setAllowFlight(true);verify(player).setFlying(true);verify(player,never()).setAllowFlight(false);
        Player creative=player();when(creative.getGameMode()).thenReturn(GameMode.CREATIVE);flight.update(creative,true);flight.clear(creative);verify(creative,never()).setAllowFlight(false);
    }
    @Test void armorRemovedDuringBossDoesNotInterruptBossFlight() {
        JavaPlugin plugin=mock(JavaPlugin.class);Player player=player();ArmorFlight flight=new ArmorFlight(plugin);flight.update(player,true);
        when(player.hasMetadata("legendaryswords_boss_flight")).thenReturn(true);flight.clear(player);verify(player,never()).setAllowFlight(false);verify(player).removeMetadata("legendaryarmor_flight",plugin);
    }
    @Test void equippedDuringBossRemembersOriginalNonFlyingState() {
        JavaPlugin plugin=mock(JavaPlugin.class);Player player=player();when(player.getAllowFlight()).thenReturn(true);MetadataValue baseline=mock(MetadataValue.class);when(baseline.value()).thenReturn(Map.of("allowed",false,"flying",false));
        when(player.getMetadata("legendaryswords_boss_flight")).thenReturn(List.of(baseline));ArmorFlight flight=new ArmorFlight(plugin);flight.update(player,true);flight.clear(player);verify(player).setAllowFlight(false);verify(player).setFlying(false);
    }
    private Player player(){Player player=mock(Player.class);when(player.getUniqueId()).thenReturn(UUID.randomUUID());when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);return player;}
}
