package io.github.legendaryarmor;
import org.bukkit.Material;
import java.util.*;
enum ArmorSet {
    ANGEL("angel","Seraph Ascendant","Flight and gentle regeneration","Radiant Sanctuary",0xffda75),
    DRAGON("dragon","Dreadfire Wyrm","Flight and fire/lava immunity","Wyrmfire Breath",0xff713b),
    LEVIATHAN("leviathan","Abyssal Sovereign","Flight, water breathing and swift movement","Tidal Dominion",0x46dce3),
    STONE("stone","Runebound Colossus","Damage reduction and knockback resistance","Earthshatter",0xcab68e),
    FROST("frost","Winter Revenant","Melee attackers are chilled","Glacial Domain",0x9fddff);
    final String id,title,passive,ability;final int color;
    ArmorSet(String id,String title,String passive,String ability,int color){this.id=id;this.title=title;this.passive=passive;this.ability=ability;this.color=color;}
    static Optional<ArmorSet> find(String id){return Arrays.stream(values()).filter(s->s.id.equalsIgnoreCase(id)).findFirst();}
    static final Material[] PIECES={Material.NETHERITE_BOOTS,Material.NETHERITE_LEGGINGS,Material.NETHERITE_CHESTPLATE,Material.NETHERITE_HELMET};
    static final String[] NAMES={"boots","leggings","chestplate","helmet"};
}
