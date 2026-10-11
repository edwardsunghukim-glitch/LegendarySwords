package io.github.backroomshorror;
import org.bukkit.util.Vector;
final class NpcRules {
 static boolean watching(Vector direction,Vector towards){return towards.lengthSquared()>0&&direction.clone().normalize().dot(towards.clone().normalize())>.75;}
 private NpcRules(){}
}
