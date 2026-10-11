package io.github.legendaryswords.combat;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;

final class BossHealth {
    static final NamespacedKey KEY=new NamespacedKey("legendaryswords","boss_health");
    static void boost(Player p,double bonus){apply(p,p.getAttribute(Attribute.MAX_HEALTH),bonus);}
    static void clear(Player p){remove(p,p.getAttribute(Attribute.MAX_HEALTH));}
    static void apply(Player p,AttributeInstance attribute,double bonus) {
        if(attribute==null||!Double.isFinite(bonus)||bonus<=0)return;
        remove(p,attribute);
        attribute.addModifier(new AttributeModifier(KEY,Math.min(1000,bonus),AttributeModifier.Operation.ADD_NUMBER));
        if(!p.isDead())p.setHealth(Math.min(attribute.getValue(),p.getHealth()+Math.min(1000,bonus)));
    }
    static void remove(Player p,AttributeInstance attribute) {
        if(attribute==null)return;
        for(AttributeModifier modifier:java.util.List.copyOf(attribute.getModifiers()))if(KEY.equals(modifier.getKey()))attribute.removeModifier(modifier);
        if(!p.isDead()&&p.getHealth()>attribute.getValue())p.setHealth(attribute.getValue());
    }
}
