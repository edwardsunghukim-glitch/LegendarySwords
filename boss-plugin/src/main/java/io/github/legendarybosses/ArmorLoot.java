package io.github.legendarybosses;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextColor;
final class ArmorLoot {
 static final Material[] MATERIALS={Material.NETHERITE_BOOTS,Material.NETHERITE_LEGGINGS,Material.NETHERITE_CHESTPLATE,Material.NETHERITE_HELMET};
 static final String[] PIECES={"boots","leggings","chestplate","helmet"};
 static final String[] TITLES={"Seraph Ascendant","Dreadfire Wyrm","Abyssal Sovereign","Runebound Colossus","Winter Revenant"};
 static final int[] COLORS={0xffda75,0xff713b,0x46dce3,0xcab68e,0x9fddff};
 static final String[] PASSIVES={"Temporary wings and gentle regeneration","Temporary wings and fire/lava immunity","Temporary wings, water breathing and speed","Damage reduction and knockback resistance","Melee attackers are chilled"};
 static final String[] ABILITIES={"Radiant Sanctuary","Wyrmfire Breath","Tidal Dominion","Earthshatter","Glacial Domain"};
 static Component text(String value,int color){return Component.text(value,TextColor.color(color)).decoration(TextDecoration.ITALIC,false);}
 static ItemStack create(BossType type,int piece,boolean unbreakable){
  ItemStack item=new ItemStack(MATERIALS[piece]);var meta=item.getItemMeta();
  meta.displayName(text(TITLES[type.ordinal()]+" "+PIECES[piece],COLORS[type.ordinal()]));
  meta.lore(java.util.List.of(text("Full set: "+PASSIVES[type.ordinal()],0xc5c9d2),text("Sneak + F: "+ABILITIES[type.ordinal()],COLORS[type.ordinal()]),text("Wear all four matching pieces.",0x999da5)));
  meta.getPersistentDataContainer().set(new NamespacedKey("legendaryarmor","armor_set"),PersistentDataType.STRING,type.id());
  meta.setItemModel(new NamespacedKey("legendaryarmor",type.id()+"_"+PIECES[piece]));
  var equipped=meta.getEquippable();equipped.setSlot(new EquipmentSlot[]{EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD}[piece]);
  equipped.setModel(new NamespacedKey("legendaryarmor",type.id()));equipped.setEquipSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE);equipped.setSwappable(true);equipped.setEquipOnInteract(true);equipped.setDamageOnHurt(true);meta.setEquippable(equipped);
  meta.setUnbreakable(unbreakable);meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);item.setItemMeta(meta);return item;
 }
}
