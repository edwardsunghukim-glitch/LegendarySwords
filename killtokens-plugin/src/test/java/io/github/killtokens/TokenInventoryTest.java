package io.github.killtokens;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenInventoryTest {
    private static final Predicate<ItemStack> TOKEN=i->i!=null&&i.getType()==Material.SUNFLOWER;
    private static ItemStack item(Material type,int amount) {
        ItemStack item=mock(ItemStack.class);AtomicInteger count=new AtomicInteger(amount);
        when(item.getType()).thenReturn(type);when(item.getAmount()).thenAnswer(a->count.get());
        doAnswer(a->{count.set(a.getArgument(0));return null;}).when(item).setAmount(anyInt());
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.isSimilar(any())).thenAnswer(a->a.getArgument(0) instanceof ItemStack other&&other.getType()==type);
        when(item.clone()).thenAnswer(a->item(type,count.get()));return item;
    }
    @Test void depositScansAllSlotsAndDoesNotMutateSource() {
        ItemStack[] current=new ItemStack[41];current[12]=item(Material.SUNFLOWER,5);current[40]=item(Material.SUNFLOWER,3);current[0]=item(Material.STONE,10);
        assertEquals(8,TokenInventory.count(current,TOKEN));ItemStack[] planned=TokenInventory.take(current,7,TOKEN);
        assertNull(planned[12]);assertEquals(1,planned[40].getAmount());assertEquals(10,planned[0].getAmount());assertEquals(5,current[12].getAmount());
    }
    @Test void insufficientDepositMakesNoChanges() {
        ItemStack[] current={item(Material.SUNFLOWER,2)};assertNull(TokenInventory.take(current,3,TOKEN));assertEquals(2,current[0].getAmount());
    }
    @Test void withdrawalOverflowIsExactlyWhatDoesNotFit() {
        ItemStack[] current={item(Material.SUNFLOWER,60),item(Material.STONE,64)};
        var grant=TokenInventory.giveWithOverflow(current,10,item(Material.SUNFLOWER,1),TOKEN);
        assertEquals(64,grant.items()[0].getAmount());assertEquals(6,grant.overflow());assertEquals(60,current[0].getAmount());
    }
    @Test void fullInventoryReturnsEntireWithdrawalAsOverflow() {
        var grant=TokenInventory.giveWithOverflow(new ItemStack[]{item(Material.STONE,64)},100,item(Material.SUNFLOWER,1),TOKEN);
        assertEquals(100,grant.overflow());
    }
    @Test void fullKillRewardPlanDoesNotPartiallyGiveTokens() {
        ItemStack[] current={item(Material.SUNFLOWER,63)};assertNull(TokenInventory.give(current,2,item(Material.SUNFLOWER,1),TOKEN));assertEquals(63,current[0].getAmount());
    }
}
