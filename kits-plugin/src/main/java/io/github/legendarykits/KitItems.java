package io.github.legendarykits;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.*;

final class KitItems {
    static ItemStack[] spawnLoadout(ItemStack[] current, ItemStack[] kit) {
        ItemStack[] planned=copy(kit);
        List<ItemStack> displaced=new ArrayList<>();
        for(int i=0;i<current.length;i++) {
            ItemStack item=current[i];if(empty(item))continue;
            boolean managed=false;
            for(ItemStack saved:kit) if(!empty(saved)&&item.isSimilar(saved)){managed=true;break;}
            // Replace kit-owned item types with the exact saved stacks, not extra copies.
            if(managed)continue;
            if(empty(planned[i]))planned[i]=item.clone();else displaced.add(item);
        }
        List<Integer> free=new ArrayList<>();
        for(int i=0;i<36;i++)if(empty(planned[i]))free.add(i);
        ItemStack[] moved=insert(new ItemStack[free.size()],displaced);
        if(moved==null)return null;
        for(int i=0;i<free.size();i++)planned[free.get(i)]=moved[i];
        return planned;
    }
    static boolean empty(ItemStack item) { return item == null || (item.getType() == Material.AIR || item.getType() == Material.CAVE_AIR || item.getType() == Material.VOID_AIR) || item.getAmount() <= 0; }
    static ItemStack[] copy(ItemStack[] items) {
        ItemStack[] out = new ItemStack[items.length];
        for (int i=0;i<items.length;i++) if (!empty(items[i])) out[i]=items[i].clone();
        return out;
    }
    // Operates on a copy: failure never changes the actual player inventory.
    static ItemStack[] insert(ItemStack[] storage, Collection<ItemStack> additions) {
        ItemStack[] out=copy(storage);
        for (ItemStack original : additions) {
            if (empty(original)) continue;
            ItemStack item=original.clone();
            for (ItemStack existing : out) {
                if (empty(existing) || !existing.isSimilar(item)) continue;
                int moved=Math.min(item.getAmount(),Math.max(0,existing.getMaxStackSize()-existing.getAmount()));
                existing.setAmount(existing.getAmount()+moved); item.setAmount(item.getAmount()-moved);
            }
            for (int i=0;i<out.length && item.getAmount()>0;i++) if (empty(out[i])) {
                int moved=Math.min(item.getAmount(),item.getMaxStackSize());
                out[i]=item.clone(); out[i].setAmount(moved); item.setAmount(item.getAmount()-moved);
            }
            if (item.getAmount()>0) return null;
        }
        return out;
    }
}
