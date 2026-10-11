package io.github.legendarykits;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.*;

final class KitItems {
    static ItemStack[] spawnLoadout(ItemStack[] current, ItemStack[] kit) {
        ItemStack[] planned=copy(current);
        // Preserve existing gear; equip kit armor/offhand into empty slots.
        for(int i=36;i<41;i++) if(empty(planned[i])&&!empty(kit[i])) {
            int needed=kit[i].getAmount();
            for(int j=0;j<36 && needed>0;j++) if(!empty(planned[j])&&planned[j].isSimilar(kit[i])) {
                int moved=Math.min(needed,planned[j].getAmount());needed-=moved;
                planned[j].setAmount(planned[j].getAmount()-moved);
                if(empty(planned[j]))planned[j]=null;
            }
            planned[i]=kit[i].clone();
        }
        List<ItemStack> wanted=new ArrayList<>();
        for(ItemStack item:kit) {
            if(empty(item))continue;
            ItemStack group=wanted.stream().filter(i->i.isSimilar(item)).findFirst().orElse(null);
            if(group==null)wanted.add(item.clone());else group.setAmount(group.getAmount()+item.getAmount());
        }
        List<ItemStack> missing=new ArrayList<>();
        for(ItemStack target:wanted) {
            int owned=0;for(ItemStack item:planned) if(!empty(item)&&item.isSimilar(target))owned+=item.getAmount();
            if(owned<target.getAmount()){ItemStack extra=target.clone();extra.setAmount(target.getAmount()-owned);missing.add(extra);}
        }
        ItemStack[] storage=insert(Arrays.copyOf(planned,36),missing);
        if(storage==null)return null;
        System.arraycopy(storage,0,planned,0,36);return planned;
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
