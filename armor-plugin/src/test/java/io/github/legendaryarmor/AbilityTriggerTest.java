package io.github.legendaryarmor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
class AbilityTriggerTest {
    @Test void fullSetAndSneakUsesAbilityInsteadOfSwapping() throws Exception {trigger(true);}
    @Test void missingSetLeavesOrdinarySwapAlone() throws Exception {trigger(false);}
    private void trigger(boolean fullSet) throws Exception {
        LegendaryArmor plugin=mock(LegendaryArmor.class,CALLS_REAL_METHODS);ArmorCatalog catalog=mock(ArmorCatalog.class);ArmorAbilities abilities=mock(ArmorAbilities.class);
        for(Object[] entry:new Object[][]{{"catalog",catalog},{"abilities",abilities}}){var field=LegendaryArmor.class.getDeclaredField((String)entry[0]);field.setAccessible(true);field.set(plugin,entry[1]);}
        Player player=mock(Player.class);PlayerInventory inventory=mock(PlayerInventory.class);when(player.getInventory()).thenReturn(inventory);
        when(player.isSneaking()).thenReturn(true);when(player.hasPermission("legendaryarmor.use")).thenReturn(true);
        if(fullSet)when(catalog.fullSet(null)).thenReturn(ArmorSet.ANGEL);
        PlayerSwapHandItemsEvent event=mock(PlayerSwapHandItemsEvent.class);when(event.getPlayer()).thenReturn(player);
        plugin.swap(event);
        if(fullSet){verify(event).setCancelled(true);verify(abilities).activate(player,ArmorSet.ANGEL);}
        else {verify(event,never()).setCancelled(anyBoolean());verifyNoInteractions(abilities);}
    }
}
