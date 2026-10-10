package io.github.legendaryswords.combat;

import io.github.legendaryswords.sword.SwordType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class VisualEffects {
    public void dust(Location location, SwordType sword, int count, double spread) {
        location.getWorld().spawnParticle(Particle.DUST, location, count, spread, spread, spread, 0,
                new Particle.DustOptions(sword.particleColor(), 1.1f));
    }
    public void ring(Location center, double radius, SwordType sword) {
        int points = Math.clamp((int) (radius * 12), 16, 72);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            dust(center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), sword, 1, 0);
        }
    }
    public void arc(Location center, Vector direction, double radius, double halfAngle, SwordType sword) {
        Vector forward = direction.clone();
        if (forward.lengthSquared() < 1.0e-8) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        if (right.lengthSquared() < 1.0e-8) right = new Vector(1, 0, 0);
        right.normalize();
        for (int i = 0; i <= 28; i++) {
            double angle = Math.toRadians(-halfAngle + 2 * halfAngle * i / 28);
            Location spot = center.clone().add(forward.clone().multiply(Math.cos(angle) * radius))
                    .add(right.clone().multiply(Math.sin(angle) * radius));
            dust(spot, sword, 1, 0.03);
        }
    }
    public void pulse(Location location, SwordType sword) {
        dust(location, sword, 14, 0.25);
        switch (sword) {
            case MASTER_SWORD -> location.getWorld().spawnParticle(Particle.END_ROD, location, 5, 0.15, 0.2, 0.15, 0.025);
            case FLAMEBLADE -> location.getWorld().spawnParticle(Particle.FLAME, location, 6, 0.2, 0.2, 0.2, 0.025);
            case FIERCE_DEITY_SWORD -> location.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, location, 5, 0.15, 0.2, 0.15, 0.02);
        }
    }
    public void sound(Location location, SwordType sword, boolean ultimate) {
        Sound sound = switch (sword) {
            case MASTER_SWORD -> ultimate ? Sound.BLOCK_BEACON_ACTIVATE : Sound.ENTITY_PLAYER_ATTACK_SWEEP;
            case FLAMEBLADE -> ultimate ? Sound.ENTITY_BLAZE_SHOOT : Sound.ITEM_FIRECHARGE_USE;
            case FIERCE_DEITY_SWORD -> ultimate ? Sound.ENTITY_ILLUSIONER_CAST_SPELL : Sound.ENTITY_PLAYER_ATTACK_STRONG;
        };
        location.getWorld().playSound(location, sound, ultimate ? 1.2f : 0.8f, ultimate ? 0.8f : 1.2f);
    }
    public void message(Player player, String text, SwordType sword) {
        player.sendActionBar(Component.text(text, sword.textColor()));
    }
}
