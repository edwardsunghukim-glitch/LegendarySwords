package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.integration.AnimationBridge;
import io.github.legendaryswords.mechanics.CooldownBook;
import io.github.legendaryswords.mechanics.Geometry;
import io.github.legendaryswords.sword.SwordType;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class AbilityController implements AutoCloseable {
    private final LegendarySwordsPlugin plugin;
    private final PluginSettings settings;
    private final PlayerStateStore states;
    private final CooldownBook cooldowns;
    private final CombatService combat;
    private final VisualEffects effects;
    private final CinematicDirector cinematics;
    private final AnimationBridge animations;
    private final Set<BukkitRunnable> tasks = new HashSet<>();
    public AbilityController(LegendarySwordsPlugin plugin, PluginSettings settings, PlayerStateStore states, CooldownBook cooldowns,
                             CombatService combat, VisualEffects effects, CinematicDirector cinematics, AnimationBridge animations) {
        this.plugin = plugin; this.settings = settings; this.states = states; this.cooldowns = cooldowns;
        this.combat = combat; this.effects = effects; this.cinematics = cinematics; this.animations = animations;
    }
    public void activate(Player player, SwordType sword, boolean ultimate) {
        if (!player.hasPermission("legendaryswords.use") || player.isDead() || player.getGameMode() == GameMode.SPECTATOR || cinematics.active(player)) return;
        String key = sword.id() + (ultimate ? ":ultimate" : ":ability");
        var meter = states.charge(player.getUniqueId());
        if (ultimate && !meter.isFull()) {
            effects.message(player, "Ultimate needs 100 charge (" + meter.value() + "/100)", sword); return;
        }
        var stats = settings.stats(sword);
        double seconds = ultimate ? stats.ultimateCooldown() : stats.abilityCooldown();
        if (!cooldowns.tryUse(player.getUniqueId(), key, Duration.ofMillis((long) (seconds * 1000)))) {
            effects.message(player, "Ready in " + Math.ceil(cooldowns.remainingMillis(player.getUniqueId(), key) / 100.0) / 10.0 + "s", sword); return;
        }
        Location origin = player.getLocation().clone();
        Vector direction = player.getEyeLocation().getDirection().clone().normalize();
        if (ultimate) {
            if (!meter.consumeUltimate()) return;
            effects.sound(origin, sword, true);
            if (cinematics.canRun(player)) animations.playCinematic(player, sword.id(), origin);
            else animations.playUltimate(player, sword.id());
            cinematics.start(player, sword, () -> ultimate(player, sword, origin, direction, stats.ultimateDamage()));
        } else {
            animations.playAbility(player, sword.id());
            effects.sound(origin, sword, false);
            effects.message(player, sword.ability(), sword);
            switch (sword) {
                case MASTER_SWORD -> beam(player, sword, player.getEyeLocation(), direction, stats.abilityDamage());
                case FLAMEBLADE -> cone(player, sword, player.getEyeLocation(), direction, 6, 55, stats.abilityDamage(), 80);
                case FIERCE_DEITY_SWORD -> cone(player, sword, player.getEyeLocation(), direction, 7, 80, stats.abilityDamage(), 0);
            }
        }
    }
    private boolean valid(Player player, Location origin) {
        return player.isOnline() && !player.isDead() && player.getWorld().equals(origin.getWorld());
    }
    private List<LivingEntity> nearby(Player owner, Location origin, double radius) {
        return origin.getWorld().getNearbyEntities(origin, radius, radius, radius).stream()
                .filter(entity -> entity instanceof LivingEntity target && combat.eligible(owner, target))
                .map(entity -> (LivingEntity) entity).limit(64).toList();
    }
    private void beam(Player player, SwordType sword, Location start, Vector direction, double damage) {
        var hit = start.getWorld().rayTraceBlocks(start, direction, 14, FluidCollisionMode.NEVER, true);
        double length = hit == null ? 14 : start.toVector().distance(hit.getHitPosition());
        Set<UUID> struck = new HashSet<>();
        repeat(0, 1, frame -> {
            if (!valid(player, start) || frame > Math.ceil(length / 1.1)) return false;
            double distance = Math.min(length, (frame + 1) * 1.1);
            Location end = start.clone().add(direction.clone().multiply(distance));
            for (int i = 0; i < 3; i++) effects.dust(end.clone().subtract(direction.clone().multiply(i * 0.25)), sword, 3, 0.08);
            end.getWorld().spawnParticle(Particle.END_ROD, end, 2, 0.08, 0.08, 0.08, 0.01);
            for (LivingEntity target : nearby(player, end, 2)) {
                Vector middle = target.getLocation().add(0, target.getHeight() * 0.5, 0).toVector();
                if (!struck.contains(target.getUniqueId()) && Geometry.distanceSquaredToSegment(middle, start.toVector(), end.toVector()) <= 1.1
                        && combat.visible(start, target)) {
                    struck.add(target.getUniqueId());
                    combat.damage(player, target, damage, direction.clone().multiply(0.3).add(new Vector(0, 0.12, 0)), 0);
                }
            }
            return distance < length;
        });
    }
    private void cone(Player player, SwordType sword, Location start, Vector direction, double radius, double halfAngle, double damage, int fireTicks) {
        for (LivingEntity target : nearby(player, start, radius)) {
            Vector delta = target.getLocation().add(0, target.getHeight() * 0.5, 0).toVector().subtract(start.toVector());
            if (Geometry.inCone(delta, direction, radius, halfAngle) && combat.visible(start, target)) {
                boolean landed = combat.damage(player, target, damage, direction.clone().multiply(0.35).add(new Vector(0, 0.15, 0)), fireTicks);
                if (landed && sword == SwordType.FIERCE_DEITY_SWORD && !target.isDead()) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
            }
        }
        repeat(0, 1, frame -> {
            if (!valid(player, start) || frame >= 7) return false;
            double distance = radius * (frame + 1) / 7;
            effects.arc(start, direction, distance, halfAngle, sword);
            if (sword == SwordType.FLAMEBLADE) effects.pulse(start.clone().add(direction.clone().multiply(distance)), sword);
            return frame < 6;
        });
    }
    private void ultimate(Player player, SwordType sword, Location origin, Vector direction, double damage) {
        if (!valid(player, origin)) return;
        effects.message(player, sword.ultimate(), sword);
        if (sword == SwordType.MASTER_SWORD) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0));
        Location center = origin.clone().add(0, 0.3, 0);
        repeat(0, 22, wave -> {
            if (!valid(player, center) || wave >= 4) return false;
            double radius = 2 + wave * 2;
            effects.ring(center, radius, sword);
            effects.ring(center.clone().add(0, 0.6, 0), radius * 0.85, sword);
            effects.sound(center, sword, true);
            for (LivingEntity target : nearby(player, center, radius)) {
                if (target.getLocation().distanceSquared(center) > radius * radius || !combat.visible(origin.clone().add(0, 1, 0), target)) continue;
                Vector outward = target.getLocation().toVector().subtract(center.toVector()).setY(0);
                if (outward.lengthSquared() > 0.001) outward.normalize().multiply(0.25);
                outward.setY(0.18);
                boolean landed = combat.damage(player, target, damage, outward, sword == SwordType.FLAMEBLADE ? 120 : 0);
                effects.pulse(target.getLocation().add(0, 1, 0), sword);
                if (landed && sword == SwordType.FIERCE_DEITY_SWORD && !target.isDead()) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45, 1));
            }
            if (sword == SwordType.MASTER_SWORD) {
                for (int column = 0; column < 6; column++) {
                    double angle = column * Math.PI / 3 + wave * 0.35;
                    Location bottom = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                    for (int y = 0; y < 8; y++) effects.dust(bottom.clone().add(0, y * 0.5, 0), sword, 2, 0.08);
                }
            } else if (sword == SwordType.FIERCE_DEITY_SWORD) {
                effects.arc(center.clone().add(0, 1.2, 0), direction, radius, 180, sword);
            }
            return wave < 3;
        });
    }
    private void repeat(long delay, long period, IntPredicate action) {
        BukkitRunnable task = new BukkitRunnable() {
            int frame;
            @Override public void run() {
                try {
                    if (!action.test(frame++)) { cancel(); tasks.remove(this); }
                } catch (RuntimeException error) {
                    cancel(); tasks.remove(this);
                    plugin.getLogger().warning("Stopped a sword effect: " + error.getMessage());
                }
            }
        };
        tasks.add(task);
        task.runTaskTimer(plugin, delay, period);
    }
    @Override public void close() {
        tasks.forEach(BukkitRunnable::cancel);
        tasks.clear();
    }
}
