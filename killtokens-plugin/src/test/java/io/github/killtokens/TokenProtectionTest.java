package io.github.killtokens;
import org.bukkit.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class TokenProtectionTest {
    private void placement(boolean genuine) throws Exception {
        KillTokens plugin=mock(KillTokens.class,CALLS_REAL_METHODS);
        NamespacedKey key=new NamespacedKey("killtokens","killtoken");
        var field=KillTokens.class.getDeclaredField("marker");field.setAccessible(true);field.set(plugin,key);
        ItemStack item=mock(ItemStack.class);ItemMeta meta=mock(ItemMeta.class);PersistentDataContainer data=mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(Material.SUNFLOWER);when(item.hasItemMeta()).thenReturn(true);when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);when(data.has(key,PersistentDataType.BYTE)).thenReturn(genuine);
        BlockPlaceEvent event=mock(BlockPlaceEvent.class);when(event.getItemInHand()).thenReturn(item);when(event.getPlayer()).thenReturn(mock(Player.class));
        plugin.place(event);
        if(genuine)verify(event).setCancelled(true);else verify(event,never()).setCancelled(anyBoolean());
    }
    @Test void genuineTokenCannotBePlaced() throws Exception {placement(true);}
    @Test void renamedOrdinarySunflowerIsNotTreatedAsAToken() throws Exception {placement(false);}
}
