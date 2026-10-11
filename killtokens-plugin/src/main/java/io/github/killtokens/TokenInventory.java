package io.github.killtokens;
import org.bukkit.inventory.ItemStack;
import java.util.function.Predicate;

final class TokenInventory {
    record Grant(ItemStack[] items,int overflow) {}
    private static boolean empty(ItemStack item) {
        return item==null||item.getAmount()<=0||item.getType()==org.bukkit.Material.AIR;
    }
    static ItemStack[] copy(ItemStack[] items) {
        ItemStack[] out=new ItemStack[items.length];for(int i=0;i<items.length;i++)if(items[i]!=null)out[i]=items[i].clone();return out;
    }
    static long count(ItemStack[] items,Predicate<ItemStack> token) {
        long count=0;for(ItemStack item:items)if(token.test(item))count+=Math.max(0,item.getAmount());return count;
    }
    static ItemStack[] take(ItemStack[] items,long amount,Predicate<ItemStack> token) {
        if(amount<=0||count(items,token)<amount)return null;
        ItemStack[] out=copy(items);
        for(int i=0;i<out.length&&amount>0;i++)if(token.test(out[i])) {
            int taken=(int)Math.min(amount,out[i].getAmount());amount-=taken;
            if(taken==out[i].getAmount())out[i]=null;else out[i].setAmount(out[i].getAmount()-taken);
        }
        return out;
    }
    static ItemStack[] give(ItemStack[] items,int amount,ItemStack template,Predicate<ItemStack> token) {
        if(amount<=0)return null;
        Grant grant=giveWithOverflow(items,amount,template,token);
        return grant.overflow==0?grant.items:null;
    }
    static Grant giveWithOverflow(ItemStack[] items,int amount,ItemStack template,Predicate<ItemStack> token) {
        ItemStack[] out=copy(items);
        // Only merge identical tokens; old configured names retain their own stacks.
        for(ItemStack item:out)if(token.test(item)&&item.isSimilar(template)) {
            int moved=Math.min(amount,Math.max(0,item.getMaxStackSize()-item.getAmount()));
            item.setAmount(item.getAmount()+moved);amount-=moved;
        }
        for(int i=0;i<out.length&&amount>0;i++)if(empty(out[i])) {
            int moved=Math.min(amount,template.getMaxStackSize());out[i]=template.clone();out[i].setAmount(moved);amount-=moved;
        }
        return new Grant(out,amount);
    }
}
