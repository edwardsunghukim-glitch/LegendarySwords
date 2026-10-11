package io.github.backroomshorror;
import org.bukkit.entity.EntityType;
import java.util.*;
enum NpcType {
 STALKER(EntityType.ZOMBIE,"The Stalker",0xa18e64),SMILER(EntityType.WITHER_SKELETON,"The Smiler",0xeeeecc),HOUND(EntityType.WOLF,"The Hound",0xba663e),WATCHER(EntityType.HUSK,"The Watcher",0xb672be);
 final EntityType entity;final String title;final int color;
 NpcType(EntityType entity,String title,int color){this.entity=entity;this.title=title;this.color=color;}
 String id(){return name().toLowerCase(Locale.ROOT);}
 static Optional<NpcType> find(String name){return Arrays.stream(values()).filter(t->t.id().equalsIgnoreCase(name)).findFirst();}
}
