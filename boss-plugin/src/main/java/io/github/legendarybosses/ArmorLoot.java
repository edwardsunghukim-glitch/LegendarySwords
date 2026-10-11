package io.github.legendarybosses;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
final class ArmorLoot {
 static final Material[] MATERIALS={Material.NETHERITE_BOOTS,Material.NETHERITE_LEGGINGS,Material.NETHERITE_CHESTPLATE,Material.NETHERITE_HELMET};
 static final String[] PIECES={"boots","leggings","chestplate","helmet"};
 static final String[] TITLES={"Seraph Ascendant","Dreadfire Wyrm","Abyssal Sovereign","Runebound Colossus","Winter Revenant"};
 static ItemStack create(BossType type,int piece,boolean unbreakable){
  ItemStack item=new ItemStack(MATERIALS[piece]);var meta=item.getItemMeta();
  meta.displayName(Component.text(TITLES[type.ordinal()]+" "+PIECES[piece]).decoration(TextDecoration.ITALIC,false));
  meta.lore(java.util.List.of(Component.text("Boss trophy • Wear all four matching pieces."),Component.text("Sneak + F: armor ability")));
  meta.getPersistentDataContainer().set(new NamespacedKey("legendaryarmor","armor_set"),PersistentDataType.STRING,type.id());
  meta.setItemModel(new NamespacedKey("legendaryarmor",type.id()+"_"+PIECES[piece]));
  var equipped=meta.getEquippable();equipped.setSlot(new EquipmentSlot[]{EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD}[piece]);
  equipped.setModel(new NamespacedKey("legendaryarmor",type.id()));equipped.setEquipSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE);equipped.setSwappable(true);equipped.setEquipOnInteract(true);equipped.setDamageOnHurt(true);meta.setEquippable(equipped);
  meta.setUnbreakable(unbreakable);meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);item.setItemMeta(meta);return item;
 }
}
