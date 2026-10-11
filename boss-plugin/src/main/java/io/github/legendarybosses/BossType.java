package io.github.legendarybosses;
import java.util.*;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
enum BossType {
 ANGEL("Seraph Sovereign",Particle.END_ROD,BarColor.YELLOW,"Radiant Cleave","Judgment Circle","Falling Stars","Sanctuary Pulse","Halo Cage","Seraph Cross","Feather Rain","Celestial Spiral","Golden Wings","Dawn Nova"),
 DRAGON("Infernal Dragon",Particle.FLAME,BarColor.RED,"Wyrmfire Breath","Claw Sweep","Meteor Storm","Inferno Ring","Ember Vortex","Dragon Fang","Cinder Minefield","Volcanic Pillars","Wingfire Sweep","Apocalypse Bloom"),
 LEVIATHAN("Sky Leviathan",Particle.SPLASH,BarColor.BLUE,"Tidal Surge","Whirlpool","Sky Bite","Storm Domain","Abyssal Coil","Tsunami Wall","Maelstrom Orbs","Trident Current","Thunderfin Spiral","Ocean Crown"),
 STONE("Granite Colossus",Particle.BLOCK,BarColor.GREEN,"Earthshatter","Boulder Barrage","Titan Fist","Quake Domain","Stone Cage","Fault Line","Boulder Orbit","Obsidian Teeth","Titan Footfall","Mountain Collapse"),
 FROST("Frost Titan",Particle.SNOWFLAKE,BarColor.WHITE,"Frozen Crescent","Ice Prison","Hailstorm","Glacial Domain","Crystal Cage","Frost Spear","Blizzard Orbs","Frozen Star","Avalanche Fan","Winter Crown");
 final String title; final Particle particle;final BarColor color;final List<String> attacks;
 BossType(String title,Particle particle,BarColor color,String...attacks){this.title=title;this.particle=particle;this.color=color;this.attacks=List.of(attacks);}
 String id(){return name().toLowerCase(Locale.ROOT);}
 static Optional<BossType> find(String id){return Arrays.stream(values()).filter(t->t.id().equalsIgnoreCase(id)).findFirst();}
}
