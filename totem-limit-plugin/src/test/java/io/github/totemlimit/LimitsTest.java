package io.github.totemlimit;
import org.junit.jupiter.api.Test;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LimitsTest {
    private ItemStack stack(Material type,int amount) {
        ItemStack item=mock(ItemStack.class);when(item.getType()).thenReturn(type);when(item.getAmount()).thenReturn(amount);
        when(item.clone()).thenAnswer(call->{ItemStack clone=mock(ItemStack.class);when(clone.getType()).thenReturn(type);return clone;});return item;
    }
    @Test void trimKeepsExactlyTwoTotemsAcrossSlotsAndReturnsExcess() {
        ItemStack a=stack(Material.TOTEM_OF_UNDYING,1),b=stack(Material.TOTEM_OF_UNDYING,1),c=stack(Material.TOTEM_OF_UNDYING,1);
        ItemStack[] slots={a,b,c};var excess=TotemLimit.trim(slots,Material.TOTEM_OF_UNDYING,2);
        assertSame(a,slots[0]);assertSame(b,slots[1]);assertNull(slots[2]);assertEquals(1,excess.size());verify(excess.getFirst()).setAmount(1);
    }
    @Test void pearlsAcrossStacksTrimToThirtyTwoWithoutRemovingOtherItems() {
        ItemStack other=stack(Material.DIAMOND,64);ItemStack[] slots={stack(Material.ENDER_PEARL,16),other,stack(Material.ENDER_PEARL,16),stack(Material.ENDER_PEARL,10)};
        var excess=TotemLimit.trim(slots,Material.ENDER_PEARL,32);assertSame(other,slots[1]);assertNull(slots[3]);verify(excess.getFirst()).setAmount(10);
    }
    @Test void cursorCountsAndZeroLimitReturnsEverything() {
        ItemStack item=stack(Material.TOTEM_OF_UNDYING,1);assertEquals(2,TotemLimit.count(new ItemStack[]{item},item,Material.TOTEM_OF_UNDYING));
        ItemStack[] slots={item};assertEquals(1,TotemLimit.trim(slots,Material.TOTEM_OF_UNDYING,0).size());assertNull(slots[0]);
    }
}
