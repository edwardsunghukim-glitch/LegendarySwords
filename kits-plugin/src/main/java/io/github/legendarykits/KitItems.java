package io.github.legendarykits;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.*;

final class KitItems {
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
