package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.util.*;
final class ArmorCatalog {
    private final JavaPlugin plugin;private final NamespacedKey key;
    ArmorCatalog(JavaPlugin plugin){this.plugin=plugin;key=new NamespacedKey(plugin,"armor_set");}
    ItemStack create(ArmorSet set,int piece) {
        ItemStack item=new ItemStack(ArmorSet.PIECES[piece]);var meta=item.getItemMeta();
        meta.displayName(text(set.title+" "+ArmorSet.NAMES[piece],set.color));
        meta.lore(List.of(text("Full set: "+set.passive,0xc5c9d2),text("Sneak + F: "+set.ability,set.color),text("Wear all four matching pieces.",0x999da5)));
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,set.id);meta.setItemModel(new NamespacedKey("legendaryarmor",set.id+"_"+ArmorSet.NAMES[piece]));
        var equipped=meta.getEquippable();equipped.setSlot(new EquipmentSlot[]{EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD}[piece]);equipped.setEquipSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE);equipped.setEquipOnInteract(true);equipped.setSwappable(true);equipped.setDamageOnHurt(true);equipped.setModel(new NamespacedKey("legendaryarmor",set.id));meta.setEquippable(equipped);
        meta.setUnbreakable(plugin.getConfig().getBoolean("unbreakable",true));meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);return item;
    }
    ArmorSet identify(ItemStack item) {
        if(item==null||!item.hasItemMeta())return null;
        String id=item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);return id==null?null:ArmorSet.find(id).orElse(null);
    }
    ArmorSet fullSet(ItemStack[] armor) {
        if(armor.length!=4)return null;ArmorSet set=identify(armor[0]);if(set==null)return null;
        for(int i=0;i<4;i++)if(armor[i]==null||armor[i].getType()!=ArmorSet.PIECES[i]||identify(armor[i])!=set)return null;
        return set;
    }
    static Component text(String text,int color){return Component.text(text,TextColor.color(color)).decoration(TextDecoration.ITALIC,false);}
}
