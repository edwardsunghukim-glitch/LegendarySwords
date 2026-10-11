package io.github.legendarykits;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KitItemsTest {
    private static ItemStack item(int amount) {
        ItemStack item=mock(ItemStack.class);AtomicInteger count=new AtomicInteger(amount);
        when(item.getType()).thenReturn(Material.STONE);when(item.getAmount()).thenAnswer(a->count.get());
        doAnswer(a->{count.set(a.getArgument(0));return null;}).when(item).setAmount(anyInt());
        when(item.getMaxStackSize()).thenReturn(64);when(item.isSimilar(any())).thenReturn(true);
        when(item.clone()).thenAnswer(a->item(count.get()));return item;
    }
    @Test void fullInventoryFailureDoesNotConsumeOrMutateAnything() {
        ItemStack original=item(64), reward=item(2);
        assertNull(KitItems.insert(new ItemStack[]{original},List.of(reward)));
        assertEquals(64,original.getAmount());assertEquals(2,reward.getAmount());
    }
    @Test void mergesStacksAndUsesFreeSlotWithoutTouchingSource() {
        ItemStack original=item(60), reward=item(10);
        ItemStack[] result=KitItems.insert(new ItemStack[]{original,null},List.of(reward));
        assertNotNull(result);assertEquals(64,result[0].getAmount());assertEquals(6,result[1].getAmount());
        assertEquals(60,original.getAmount());assertEquals(10,reward.getAmount());
    }
    @Test void failureAfterFirstRewardStillLeavesOriginalInventoryUntouched() {
        ItemStack original=item(62);
        assertNull(KitItems.insert(new ItemStack[]{original},List.of(item(2),item(1))));
        assertEquals(62,original.getAmount());
    }
    @Test void adminCommandsRejectPlayersWithoutPermissionBeforeAccessingKits() {
        LegendaryKits plugin=mock(LegendaryKits.class,CALLS_REAL_METHODS);Player p=mock(Player.class);
        for(String action:List.of("create","cooldown","permission"))
            assertTrue(plugin.onCommand(p,mock(Command.class),"kits",new String[]{action,"starter","0"}));
        verify(p,times(3)).sendMessage("No permission to edit kits.");verify(plugin,never()).getServer();
    }
}
