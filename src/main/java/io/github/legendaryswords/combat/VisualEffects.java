package io.github.legendaryswords.combat;

import io.github.legendaryswords.sword.SwordType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Dense spell accents; force-visible particles supplement actual display geometry. */
public final class VisualEffects {
    public void dust(Location location, SwordType sword, int count, double spread) {
        try {
            location.getWorld().spawnParticle(Particle.DUST, location, count, spread, spread, spread, 0,
                    new Particle.DustOptions(sword.particleColor(), 1.5f), true);
        } catch (RuntimeException ignored) { /* Cosmetic failure must never discard damage. */ }
    }
    public void particle(Location location, Particle particle, int count, double spread, double speed) {
        try { location.getWorld().spawnParticle(particle, location, count, spread, spread, spread, speed, null, true); }
        catch (RuntimeException ignored) { /* The display and combat sequences remain independent. */ }
    }
    public void line(Location from, Location to, SwordType sword, double spacing) {
        Vector delta = to.toVector().subtract(from.toVector());
        int steps = Math.clamp((int) Math.ceil(delta.length() / Math.max(0.12, spacing)), 1, 100);
        for (int i = 0; i <= steps; i++) dust(from.clone().add(delta.clone().multiply((double) i / steps)), sword, 1, 0.015);
    }
    public void ring(Location center, double radius, SwordType sword) {
        int points = Math.clamp((int) (radius * 16), 24, 88);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            dust(center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), sword, 1, 0);
        }
    }
    public void arc(Location center, Vector direction, double radius, double halfAngle, SwordType sword) {
        Vector forward = direction.clone().setY(0);
        if (forward.lengthSquared() < 1.0e-8) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX());
        for (int i = 0; i <= 40; i++) {
            double angle = Math.toRadians(-halfAngle + 2 * halfAngle * i / 40);
            dust(center.clone().add(forward.clone().multiply(Math.cos(angle) * radius))
                    .add(right.clone().multiply(Math.sin(angle) * radius)), sword, 1, 0.025);
        }
    }
    public void helix(Location center, double radius, double height, double rotation, SwordType sword) {
        for (int i = 0; i < 36; i++) {
            double t = i / 35.0, angle = rotation + t * Math.PI * 5;
            dust(center.clone().add(Math.cos(angle) * radius, t * height, Math.sin(angle) * radius), sword, 1, 0.025);
        }
    }
    public void pulse(Location location, SwordType sword) {
        dust(location, sword, 20, 0.35);
        Particle accent = switch (sword) {
            case MASTER_SWORD -> Particle.END_ROD;
            case FLAMEBLADE -> Particle.FLAME;
            case FIERCE_DEITY_SWORD -> Particle.SOUL_FIRE_FLAME;
            case FROSTBLADE -> Particle.SNOWFLAKE;
            case THUNDERBLADE -> Particle.ELECTRIC_SPARK;
            case ROYAL_CLAYMORE -> Particle.CRIT;
            case EIGHTFOLD_BLADE -> Particle.CLOUD;
            case BIGGORON_SWORD -> Particle.LAVA;
        };
        particle(location, accent, 12, 0.35, 0.025);
    }
    public void sound(Location location, SwordType sword, boolean ultimate) {
        Sound sound = switch (sword) {
            case MASTER_SWORD -> ultimate ? Sound.BLOCK_BEACON_ACTIVATE : Sound.BLOCK_BEACON_POWER_SELECT;
            case FLAMEBLADE -> ultimate ? Sound.ENTITY_BLAZE_SHOOT : Sound.ITEM_FIRECHARGE_USE;
            case FIERCE_DEITY_SWORD -> ultimate ? Sound.ENTITY_ILLUSIONER_CAST_SPELL : Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;
            case FROSTBLADE -> ultimate ? Sound.BLOCK_GLASS_BREAK : Sound.ENTITY_PLAYER_HURT_FREEZE;
            case THUNDERBLADE -> ultimate ? Sound.ENTITY_LIGHTNING_BOLT_THUNDER : Sound.ENTITY_LIGHTNING_BOLT_IMPACT;
            case ROYAL_CLAYMORE -> ultimate ? Sound.ENTITY_IRON_GOLEM_ATTACK : Sound.BLOCK_ANVIL_LAND;
            case EIGHTFOLD_BLADE -> ultimate ? Sound.ENTITY_BREEZE_WIND_BURST : Sound.ENTITY_PLAYER_ATTACK_SWEEP;
            case BIGGORON_SWORD -> ultimate ? Sound.ENTITY_GENERIC_EXPLODE : Sound.ENTITY_IRON_GOLEM_ATTACK;
        };
        try { location.getWorld().playSound(location, sound, ultimate ? 1.2f : 0.9f, ultimate ? 0.8f : 1.15f); }
        catch (RuntimeException ignored) { /* Optional presentation. */ }
    }
    public void message(Player player, String text, SwordType sword) {
        player.sendActionBar(Component.text(text, sword.textColor()));
    }
}
