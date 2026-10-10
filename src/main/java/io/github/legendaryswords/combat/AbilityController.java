package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.integration.AnimationBridge;
import io.github.legendaryswords.mechanics.CooldownBook;
import io.github.legendaryswords.mechanics.Geometry;
import io.github.legendaryswords.sword.SwordType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Eight spells with distinct geometry. Combat never waits for an animation or a camera. */
public final class AbilityController implements AutoCloseable {
    private final LegendarySwordsPlugin plugin;
    private final PluginSettings settings;
    private final PlayerStateStore states;
    private final CooldownBook cooldowns;
    private final CombatService combat;
    private final VisualEffects effects;
    private final CinematicDirector cinematics;
    private final AnimationBridge animations;
    private final EffectDisplay displays;
    private final Map<UUID, Set<BukkitRunnable>> tasks = new HashMap<>();

    public AbilityController(LegendarySwordsPlugin plugin, PluginSettings settings, PlayerStateStore states, CooldownBook cooldowns,
                             CombatService combat, VisualEffects effects, CinematicDirector cinematics, AnimationBridge animations) {
        this.plugin = plugin; this.settings = settings; this.states = states; this.cooldowns = cooldowns;
        this.combat = combat; this.effects = effects; this.cinematics = cinematics; this.animations = animations;
        displays = new EffectDisplay(plugin);
    }

    public void activate(Player player, SwordType sword, boolean ultimate) {
        if (!player.hasPermission("legendaryswords.use") || !player.isOnline() || player.isDead()
                || player.getGameMode() == GameMode.SPECTATOR) return;
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
        if (ultimate && !meter.consumeUltimate()) return;
        Location origin = player.getLocation().clone();
        Vector direction = player.getEyeLocation().getDirection().clone().normalize();
        effects.message(player, ultimate ? sword.ultimate() : sword.ability(), sword);
        effects.sound(origin, sword, ultimate);

        // Even a missing model or interrupted cosmetic sequence cannot discard an ultimate.
        try {
            if (ultimate) animations.playUltimate(player, sword.id());
            else animations.playAbility(player, sword.id());
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().warning("Sword animation unavailable; spell still runs: " + error.getMessage());
        }
        launch(player, sword, origin, direction, ultimate ? stats.ultimateDamage() : stats.abilityDamage(), ultimate);
        if (ultimate) {
            try { cinematics.start(player, sword, () -> { }); }
            catch (RuntimeException | LinkageError error) { plugin.getLogger().warning("Cosmetic ritual unavailable: " + error.getMessage()); }
        }
    }

    private void launch(Player player, SwordType sword, Location origin, Vector direction, double damage, boolean ultimate) {
        switch (sword) {
            case MASTER_SWORD -> laser(player, sword, origin.clone().add(0, 1.25, 0), direction, damage, ultimate);
            case FLAMEBLADE -> fireRing(player, sword, origin, damage, ultimate);
            case FIERCE_DEITY_SWORD -> {
                if (ultimate) fallingMoon(player, sword, origin, direction, damage);
                else crescent(player, sword, origin.clone().add(0, 1.2, 0), direction, damage);
            }
            case FROSTBLADE -> {
                if (ultimate) blizzard(player, sword, origin, damage);
                else iceFangs(player, sword, origin, direction, damage);
            }
            case THUNDERBLADE -> {
                if (ultimate) thunderstorm(player, sword, origin, damage);
                else chainLightning(player, sword, origin.clone().add(0, 1.3, 0), direction, damage);
            }
            case ROYAL_CLAYMORE -> {
                if (ultimate) royalSlam(player, sword, origin, damage);
                else faultline(player, sword, origin, direction, damage);
            }
            case EIGHTFOLD_BLADE -> {
                if (ultimate) eightCuts(player, sword, origin, direction, damage);
                else windStep(player, sword, origin, direction, damage);
            }
            case BIGGORON_SWORD -> {
                if (ultimate) eruption(player, sword, origin, damage);
                else goronSpin(player, sword, origin, damage);
            }
        }
    }

    private boolean valid(Player player, Location origin) {
        return player.isOnline() && !player.isDead() && player.getWorld().equals(origin.getWorld());
    }
    private Vector flat(Vector direction) {
        Vector forward = direction.clone().setY(0);
        return forward.lengthSquared() < 0.001 ? new Vector(0, 0, 1) : forward.normalize();
    }
    private List<LivingEntity> nearby(Player owner, Location origin, double radius) {
        return origin.getWorld().getNearbyEntities(origin, radius, radius, radius).stream()
                .filter(entity -> entity instanceof LivingEntity target && combat.eligible(owner, target))
                .map(entity -> (LivingEntity) entity)
                .sorted(Comparator.comparingDouble(target -> target.getLocation().distanceSquared(origin)))
                .limit(64).toList();
    }
    private Location middle(LivingEntity target) { return target.getLocation().add(0, target.getHeight() * 0.5, 0); }
    private double unobstructed(Location start, Vector direction, double maximum) {
        var hit = start.getWorld().rayTraceBlocks(start, direction, maximum, FluidCollisionMode.NEVER, true);
        return hit == null ? maximum : start.toVector().distance(hit.getHitPosition());
    }
    private boolean strike(Player player, LivingEntity target, Location source, double damage, double push, int fire, int slow, int amplifier) {
        if (!combat.visible(source, target)) return false;
        Vector outward = middle(target).toVector().subtract(source.toVector()).setY(0);
        if (outward.lengthSquared() > 0.001) outward.normalize().multiply(push);
        outward.setY(push > 0 ? Math.min(0.55, push * 0.6) : 0);
        boolean landed = combat.damage(player, target, damage, outward, fire);
        if (landed && slow > 0 && !target.isDead()) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, amplifier));
        return landed;
    }
    private void hitArea(Player player, Location center, double radius, double damage, double push, int fire,
                         int slow, int amplifier, Map<UUID, Integer> hits, int maximumHits) {
        for (LivingEntity target : nearby(player, center, radius + 1)) {
            if (middle(target).distanceSquared(center.clone().add(0, 0.8, 0)) > radius * radius
                    || hits.getOrDefault(target.getUniqueId(), 0) >= maximumHits) continue;
            if (strike(player, target, player.getEyeLocation(), damage, push, fire, slow, amplifier)) hits.merge(target.getUniqueId(), 1, Integer::sum);
        }
    }

    private void laser(Player player, SwordType sword, Location start, Vector direction, double damage, boolean ultimate) {
        double length = unobstructed(start, direction, ultimate ? 28 : 18);
        Location end = start.clone().add(direction.clone().multiply(length));
        Map<UUID, Integer> hits = new HashMap<>();
        int duration = ultimate ? 30 : 17, channel = ultimate ? 4 : 2;
        displays.line(player, start, end, Material.CYAN_STAINED_GLASS, 0.08, sword.particleColor(), channel * 2 + 2);
        repeat(player, start, 0, 2, frame -> {
            if (frame >= duration) return false;
            if (frame < channel) {
                effects.ring(start, 0.35 + frame * 0.12, sword);
                effects.particle(start, Particle.END_ROD, 6, 0.18, 0.02);
                return true;
            }
            if (frame == channel) {
                displays.line(player, start, end, Material.CYAN_STAINED_GLASS, ultimate ? 0.9 : 0.38, sword.particleColor(), (duration - channel) * 2);
                displays.line(player, start, end, Material.SEA_LANTERN, ultimate ? 0.3 : 0.12, sword.particleColor(), (duration - channel) * 2);
            }
            effects.line(start, end, sword, ultimate ? 0.42 : 0.5);
            double travel = (frame - channel) * 1.35 % Math.max(0.5, length);
            effects.pulse(start.clone().add(direction.clone().multiply(travel)), sword);
            if (ultimate && frame % 3 == 0) effects.helix(start, 1.4, 3.5, frame * 0.3, sword);
            if (!ultimate || frame == channel || frame == channel + 10 || frame == channel + 20) {
                Location center = start.clone().add(direction.clone().multiply(length / 2));
                for (LivingEntity target : nearby(player, center, length / 2 + 2)) {
                    if (hits.getOrDefault(target.getUniqueId(), 0) >= (ultimate ? 3 : 1)
                            || Geometry.distanceSquaredToSegment(middle(target).toVector(), start.toVector(), end.toVector()) > (ultimate ? 2.5 : 0.9)) continue;
                    if (strike(player, target, start, damage, 0.16, 0, 0, 0)) hits.merge(target.getUniqueId(), 1, Integer::sum);
                }
            }
            return true;
        });
    }

    private void fireRing(Player player, SwordType sword, Location origin, double damage, boolean ultimate) {
        Map<UUID, Integer> hits = new HashMap<>();
        Location base = origin.clone().add(0, 0.12, 0);
        double outer = ultimate ? 8 : 4.5;
        // These FIRE displays are flame models, never real fire blocks.
        displays.ring(player, base, outer, Material.ORANGE_STAINED_GLASS, 0.16, sword.particleColor(), ultimate ? 92 : 72);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            displays.box(player, base.clone().add(Math.cos(angle) * outer, 1.0, Math.sin(angle) * outer),
                    Material.FIRE, 1.15, ultimate ? 3.3 : 2.0, 1.15, sword.particleColor(), ultimate ? 92 : 72);
        }
        repeat(player, origin, 0, 2, frame -> {
            if (frame >= (ultimate ? 45 : 35)) return false;
            double radius = ultimate ? Math.min(outer, 1.0 + frame * 0.3) : outer;
            effects.ring(base, radius, sword);
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12 + frame * 0.15;
                Location jet = base.clone().add(Math.cos(angle) * radius, 0.4 + (frame % 4) * 0.35, Math.sin(angle) * radius);
                effects.particle(jet, Particle.FLAME, ultimate ? 5 : 3, 0.12, 0.04);
            }
            if (ultimate && frame % 3 == 0) effects.helix(base, Math.max(1, outer - frame * 0.035), 4.5, frame * 0.24, sword);
            if (ultimate ? frame == 10 || frame == 22 || frame == 34 : frame % 5 == 0)
                hitArea(player, origin, ultimate ? outer : outer + 0.3, damage, 0.14, ultimate ? 160 : 100, 0, 0, hits, ultimate ? 3 : 1);
            return true;
        });
    }

    private void crescent(Player player, SwordType sword, Location start, Vector direction, double damage) {
        Vector forward = flat(direction), right = new Vector(-forward.getZ(), 0, forward.getX());
        double length = unobstructed(start, forward, 16);
        Map<UUID, Integer> hits = new HashMap<>();
        repeat(player, start, 0, 2, frame -> {
            double travel = Math.min(length, (frame + 1) * 0.85);
            Location center = start.clone().add(forward.clone().multiply(travel));
            Location previous = null;
            for (int i = 0; i <= 10; i++) {
                double angle = -Math.PI * 0.8 + Math.PI * 1.6 * i / 10;
                Location point = center.clone().add(right.clone().multiply(Math.sin(angle) * 1.7)).add(0, Math.cos(angle) * 1.7, 0);
                if (previous != null && frame % 2 == 0) displays.line(player, previous, point, Material.PURPLE_STAINED_GLASS, 0.18, sword.particleColor(), 5);
                effects.particle(point, Particle.SOUL_FIRE_FLAME, 2, 0.045, 0.01);
                effects.dust(point, sword, 2, 0.035); previous = point;
            }
            hitArea(player, center.clone().subtract(0, 0.8, 0), 2.0, damage, 0.25, 0, 50, 0, hits, 1);
            return travel < length;
        });
    }

    private void fallingMoon(Player player, SwordType sword, Location origin, Vector direction, double damage) {
        Vector forward = flat(direction);
        Location aimed = origin.clone().add(0, 1, 0);
        aimed.add(forward.clone().multiply(Math.min(10, unobstructed(aimed, forward, 10))));
        List<Location> impactPoints = new ArrayList<>();
        for (LivingEntity target : nearby(player, aimed, 7)) {
            if (combat.visible(player.getEyeLocation(), target)) impactPoints.add(target.getLocation().clone());
            if (impactPoints.size() == 4) break;
        }
        while (impactPoints.size() < 4) {
            double angle = impactPoints.size() * Math.PI / 2;
            impactPoints.add(aimed.clone().add(Math.cos(angle) * 2.6, -1, Math.sin(angle) * 2.6));
        }
        Map<UUID, Integer> hits = new HashMap<>();
        for (int i = 0; i < impactPoints.size(); i++) {
            Location impact = impactPoints.get(i);
            repeat(player, origin, i * 10L, 2, frame -> {
                if (frame == 0) displays.ring(player, impact.clone().add(0, 0.15, 0), 2.8, Material.PURPLE_STAINED_GLASS, 0.09, sword.particleColor(), 38);
                if (frame < 10) {
                    effects.ring(impact.clone().add(0, 0.15, 0), 2.8, sword);
                    effects.helix(impact, 1.2, 3, frame * 0.4, sword);
                } else if (frame < 18) {
                    Location moon = impact.clone().add(0, (18 - frame) * 1.05, 0);
                    displays.box(player, moon, Material.SEA_LANTERN, 1.35, 1.35, 1.35, sword.particleColor(), 4);
                    displays.line(player, moon, moon.clone().add(0, 1.8, 0), Material.PURPLE_STAINED_GLASS, 0.4, sword.particleColor(), 5);
                    effects.pulse(moon, sword);
                } else if (frame == 18) {
                    effects.particle(impact.clone().add(0, 1, 0), Particle.EXPLOSION, 1, 0, 0);
                    effects.pulse(impact.clone().add(0, 0.8, 0), sword);
                    displays.ring(player, impact.clone().add(0, 0.2, 0), 3.5, Material.PURPLE_STAINED_GLASS, 0.23, sword.particleColor(), 16);
                    hitArea(player, impact, 3.8, damage, 0.5, 0, 60, 1, hits, 2);
                    effects.sound(impact, sword, true);
                } else effects.ring(impact.clone().add(0, 0.2, 0), 3.5 + (frame - 18) * 0.2, sword);
                return frame < 24;
            });
        }
    }

    private void iceSpike(Player player, SwordType sword, Location point, double height, int lifetime) {
        displays.box(player, point.clone().add(0, height * 0.23, 0), Material.PACKED_ICE, 0.85, height * 0.46, 0.85, sword.particleColor(), lifetime);
        displays.box(player, point.clone().add(0, height * 0.62, 0), Material.LIGHT_BLUE_STAINED_GLASS, 0.48, height * 0.34, 0.48, sword.particleColor(), lifetime);
        displays.box(player, point.clone().add(0, height * 0.9, 0), Material.ICE, 0.2, height * 0.22, 0.2, sword.particleColor(), lifetime);
        effects.particle(point.clone().add(0, 1, 0), Particle.SNOWFLAKE, 12, 0.45, 0.015);
    }
    private void iceFangs(Player player, SwordType sword, Location origin, Vector direction, double damage) {
        Vector forward = flat(direction);
        double length = unobstructed(origin.clone().add(0, 0.75, 0), forward, 12);
        Map<UUID, Integer> hits = new HashMap<>();
        repeat(player, origin, 0, 3, frame -> {
            double distance = Math.min(length, 1.5 + frame * 1.5);
            Location point = origin.clone().add(forward.clone().multiply(distance));
            iceSpike(player, sword, point, 1.7 + frame * 0.14, 56 - frame * 3);
            hitArea(player, point, 1.85, damage, 0.12, 0, 75, 1, hits, 1);
            return frame < 7 && distance < length;
        });
    }
    private void blizzard(Player player, SwordType sword, Location origin, double damage) {
        Map<UUID, Integer> hits = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            iceSpike(player, sword, origin.clone().add(Math.cos(angle) * 6.5, 0, Math.sin(angle) * 6.5), 3.7, 88);
        }
        displays.ring(player, origin.clone().add(0, 0.1, 0), 6.5, Material.LIGHT_BLUE_STAINED_GLASS, 0.12, sword.particleColor(), 88);
        repeat(player, origin, 0, 2, frame -> {
            if (frame >= 42) return false;
            effects.helix(origin, 5.8, 5, frame * 0.24, sword);
            effects.helix(origin, 3.1, 3.5, -frame * 0.35, sword);
            effects.particle(origin.clone().add(0, 2.2, 0), Particle.SNOWFLAKE, 22, 3.2, 0.02);
            if (frame == 6 || frame == 16 || frame == 26 || frame == 36)
                hitArea(player, origin, 7, damage, 0.08, 0, 55, 2, hits, 4);
            return true;
        });
    }

    private void bolt(Player player, SwordType sword, Location from, Location to, int lifetime) {
        Vector delta = to.toVector().subtract(from.toVector());
        Location last = from;
        for (int i = 1; i <= 9; i++) {
            Location next = from.clone().add(delta.clone().multiply(i / 9.0));
            if (i < 9) next.add(Math.sin(i * 2.4) * 0.32, Math.cos(i * 1.7) * 0.3, Math.cos(i * 2.1) * 0.32);
            displays.line(player, last, next, Material.YELLOW_STAINED_GLASS, 0.17, sword.particleColor(), lifetime);
            displays.line(player, last, next, Material.SEA_LANTERN, 0.045, sword.particleColor(), lifetime);
            effects.line(last, next, sword, 0.2); last = next;
        }
        effects.particle(to, Particle.ELECTRIC_SPARK, 25, 0.45, 0.03);
    }
    private void chainLightning(Player player, SwordType sword, Location start, Vector direction, double damage) {
        List<LivingEntity> chain = new ArrayList<>();
        Location cursor = start;
        for (int jump = 0; jump < 6; jump++) {
            LivingEntity next = null;
            for (LivingEntity target : nearby(player, cursor, jump == 0 ? 16 : 7)) {
                if (chain.contains(target) || !combat.visible(cursor, target)) continue;
                if (jump == 0 && !Geometry.inCone(middle(target).toVector().subtract(start.toVector()), direction, 16, 55)) continue;
                next = target; break;
            }
            if (next == null) break;
            chain.add(next); cursor = middle(next);
        }
        Location[] previous = {start};
        repeat(player, start, 4, 4, frame -> {
            if (chain.isEmpty()) {
                Location end = start.clone().add(direction.clone().multiply(unobstructed(start, direction, 13)));
                bolt(player, sword, start, end, 22); return false;
            }
            if (frame >= chain.size()) return false;
            LivingEntity target = chain.get(frame);
            if (!target.isDead() && target.getWorld().equals(start.getWorld())
                    && middle(target).distanceSquared(previous[0]) <= (frame == 0 ? 16 * 16 : 7 * 7)
                    && combat.visible(previous[0], target)) {
                Location destination = middle(target);
                bolt(player, sword, previous[0], destination, 20);
                strike(player, target, previous[0], damage * Math.pow(0.88, frame), 0.09, 0, 20, 0);
                previous[0] = destination;
            }
            return frame + 1 < chain.size();
        });
    }
    private void thunderstorm(Player player, SwordType sword, Location origin, double damage) {
        Map<UUID, Integer> hits = new HashMap<>();
        displays.ring(player, origin.clone().add(0, 0.15, 0), 7, Material.YELLOW_STAINED_GLASS, 0.11, sword.particleColor(), 96);
        repeat(player, origin, 0, 2, frame -> {
            if (frame >= 45) return false;
            effects.ring(origin.clone().add(0, 0.2, 0), 7, sword);
            effects.helix(origin.clone().add(0, 5.5, 0), 4.5, 1.5, frame * 0.2, sword);
            if (frame >= 6 && (frame - 6) % 6 == 0) {
                List<LivingEntity> candidates = nearby(player, origin, 7).stream()
                        .filter(target -> hits.getOrDefault(target.getUniqueId(), 0) < 3 && combat.visible(player.getEyeLocation(), target)).toList();
                double angle = (frame - 6) * 0.65;
                Location impact = candidates.isEmpty() ? origin.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4) : candidates.get(0).getLocation();
                bolt(player, sword, impact.clone().add(0, 9, 0), impact.clone().add(0, 0.8, 0), 20);
                effects.pulse(impact.clone().add(0, 1, 0), sword);
                hitArea(player, impact, 2.8, damage, 0.22, 0, 30, 0, hits, 3);
            }
            return true;
        });
    }

    private void crack(Player player, SwordType sword, Location from, Location to, int lifetime) {
        displays.line(player, from.clone().add(0, 0.06, 0), to.clone().add(0, 0.06, 0), Material.ORANGE_STAINED_GLASS, 0.16, sword.particleColor(), lifetime);
        effects.line(from.clone().add(0, 0.15, 0), to.clone().add(0, 0.15, 0), sword, 0.22);
        displays.box(player, to.clone().add(0, 0.65, 0), Material.DEEPSLATE, 0.55, 1.3, 0.55, sword.particleColor(), lifetime);
        effects.particle(to.clone().add(0, 0.7, 0), Particle.CRIT, 14, 0.4, 0.02);
    }
    private void faultline(Player player, SwordType sword, Location origin, Vector direction, double damage) {
        Vector forward = flat(direction), right = new Vector(-forward.getZ(), 0, forward.getX());
        double length = unobstructed(origin.clone().add(0, 0.8, 0), forward, 11);
        Map<UUID, Integer> hits = new HashMap<>();
        repeat(player, origin, 0, 3, frame -> {
            double distance = Math.min(length, (frame + 1) * 1.8);
            Location previous = origin.clone().add(forward.clone().multiply(Math.max(0, distance - 1.8)));
            Location point = origin.clone().add(forward.clone().multiply(distance)).add(right.clone().multiply(frame % 2 == 0 ? 0.32 : -0.32));
            crack(player, sword, previous, point, 52 - frame * 3);
            hitArea(player, point, 2.1, damage, 0.55, 0, 20, 0, hits, 1);
            return frame < 6 && distance < length;
        });
    }
    private void royalSlam(Player player, SwordType sword, Location origin, double damage) {
        ItemDisplay blade = displays.blade(player, origin.clone().add(0, 7, 0), player.getInventory().getItemInMainHand(), 5.5, sword.particleColor(), 50);
        Map<UUID, Integer> hits = new HashMap<>();
        repeat(player, origin, 0, 2, frame -> {
            if (frame < 10) {
                if (blade != null && blade.isValid()) {
                    blade.teleport(origin.clone().add(0, 7 - frame * 0.57, 0));
                    blade.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotationZ((float) Math.PI), new Vector3f(5.5f), new Quaternionf()));
                }
                effects.ring(origin.clone().add(0, 0.1, 0), 2 + frame * 0.5, sword);
            } else if (frame == 10) {
                if (blade != null) blade.remove();
                hitArea(player, origin, 8, damage * 2, 0.75, 0, 40, 1, hits, 1);
                effects.particle(origin.clone().add(0, 0.7, 0), Particle.EXPLOSION, 1, 0, 0);
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));
                    for (int segment = 0; segment < 3; segment++) crack(player, sword,
                            origin.clone().add(direction.clone().multiply(segment * 2.4)), origin.clone().add(direction.clone().multiply((segment + 1) * 2.4)), 30);
                }
                effects.sound(origin, sword, true);
            } else {
                effects.ring(origin.clone().add(0, 0.25, 0), 2 + (frame - 10) * 0.6, sword);
                effects.particle(origin.clone().add(0, 0.7, 0), Particle.CRIT, 14, 2.0, 0.035);
            }
            return frame < 23;
        });
    }

    private void windStep(Player player, SwordType sword, Location origin, Vector direction, double damage) {
        Vector forward = flat(direction), right = new Vector(-forward.getZ(), 0, forward.getX());
        Location start = origin.clone().add(0, 1, 0);
        double length = unobstructed(start, forward, 14);
        // A small ordinary velocity burst, never a teleport through blocks or off a high ledge.
        Location step = origin.clone().add(forward.clone().multiply(2.5));
        if (player.isOnGround() && !player.isInsideVehicle() && !player.isFlying() && !player.isGliding()
                && unobstructed(origin.clone().add(0, 0.75, 0), forward, 2.5) >= 2.45
                && origin.getWorld().getWorldBorder().isInside(step)
                && origin.getWorld().rayTraceBlocks(step.clone().add(0, 0.8, 0), new Vector(0, -1, 0),
                        2.2, FluidCollisionMode.NEVER, true) != null) {
            player.setVelocity(forward.clone().multiply(0.95).setY(Math.clamp(player.getVelocity().getY(), 0, 0.12)));
        }
        Map<UUID, Integer> hits = new HashMap<>();
        repeat(player, origin, 0, 2, frame -> {
            double distance = Math.min(length, (frame + 1) * 0.8);
            Location point = start.clone().add(forward.clone().multiply(distance));
            for (int strand = 0; strand < 3; strand++) {
                double phase = frame * 0.5 + strand * Math.PI * 2 / 3;
                Location from = point.clone().add(right.clone().multiply(Math.cos(phase) * 0.75)).add(0, Math.sin(phase) * 0.6, 0);
                Location to = from.clone().subtract(forward.clone().multiply(1.6));
                if (frame % 2 == 0) displays.line(player, from, to, Material.WHITE_STAINED_GLASS, 0.09, sword.particleColor(), 6);
                effects.line(from, to, sword, 0.2);
                effects.particle(from, Particle.CLOUD, 2, 0.08, 0.01);
            }
            hitArea(player, point.clone().subtract(0, 0.8, 0), 1.8, damage, 0.35, 0, 0, 0, hits, 1);
            return distance < length;
        });
    }
    private void eightCuts(Player player, SwordType sword, Location origin, Vector direction, double damage) {
        Vector forward = flat(direction);
        Location center = origin.clone().add(forward.clone().multiply(4));
        List<LivingEntity> candidates = nearby(player, center, 5).stream().filter(target -> combat.visible(player.getEyeLocation(), target)).toList();
        if (!candidates.isEmpty()) center = candidates.get(0).getLocation().clone();
        Location focus = center;
        Map<UUID, Integer> hits = new HashMap<>();
        displays.ring(player, focus.clone().add(0, 0.1, 0), 4.3, Material.WHITE_STAINED_GLASS, 0.1, sword.particleColor(), 96);
        repeat(player, origin, 0, 10, cut -> {
            if (cut >= 8) return false;
            double angle = cut * Math.PI / 4;
            Vector crossing = new Vector(Math.cos(angle), 0, Math.sin(angle));
            Location from = focus.clone().add(0, 0.8 + cut % 3 * 0.45, 0).add(crossing.clone().multiply(-4));
            Location to = from.clone().add(crossing.clone().multiply(8)).add(0, cut % 2 == 0 ? 1.4 : -0.6, 0);
            displays.line(player, from, to, Material.WHITE_STAINED_GLASS, 0.24, sword.particleColor(), 12);
            effects.line(from, to, sword, 0.13);
            effects.arc(focus.clone().add(0, 0.6, 0), crossing, 4.2, 100, sword);
            effects.particle(focus.clone().add(0, 1, 0), Particle.SWEEP_ATTACK, 3, 1.0, 0);
            hitArea(player, focus, 4.5, damage * 0.5, 0.12, 0, 25, 0, hits, 8);
            effects.sound(focus, sword, false);
            return cut < 7;
        });
    }

    private void goronSpin(Player player, SwordType sword, Location origin, double damage) {
        ItemDisplay blade = displays.blade(player, origin.clone().add(0, 1, 0), player.getInventory().getItemInMainHand(), 3.2, sword.particleColor(), 42);
        displays.ring(player, origin.clone().add(0, 0.2, 0), 4.6, Material.ORANGE_STAINED_GLASS, 0.16, sword.particleColor(), 40);
        Map<UUID, Integer> hits = new HashMap<>();
        repeat(player, origin, 0, 2, frame -> {
            if (frame >= 19) return false;
            double angle = frame * Math.PI / 4;
            if (blade != null && blade.isValid()) {
                blade.teleport(origin.clone().add(Math.cos(angle) * 2.7, 1, Math.sin(angle) * 2.7));
                blade.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotationY((float) -angle).rotateZ((float) Math.PI / 2), new Vector3f(3.2f), new Quaternionf()));
            }
            effects.arc(origin.clone().add(0, 0.8, 0), new Vector(Math.cos(angle), 0, Math.sin(angle)), 4.5, 65, sword);
            effects.particle(origin.clone().add(Math.cos(angle) * 4, 0.5, Math.sin(angle) * 4), Particle.CRIT, 8, 0.3, 0.04);
            hitArea(player, origin, 4.8, damage, 0.65, 0, 0, 0, hits, 1);
            return true;
        });
    }
    private void eruption(Player player, SwordType sword, Location origin, double damage) {
        ItemDisplay blade = displays.blade(player, origin.clone().add(0, 1.8, 0), player.getInventory().getItemInMainHand(), 4.8, sword.particleColor(), 30);
        Map<UUID, Integer> hits = new HashMap<>();
        repeat(player, origin, 0, 2, frame -> {
            if (frame >= 40) return false;
            if (frame < 10) {
                effects.helix(origin, 2.8, 4, frame * 0.45, sword);
                if (blade != null && blade.isValid()) blade.setTransformation(new Transformation(new Vector3f(),
                        new Quaternionf().rotationY(frame * 0.5f).rotateZ((float) Math.PI / 3), new Vector3f(4.8f), new Quaternionf()));
            }
            if (frame == 10 || frame == 20 || frame == 30) {
                int wave = (frame - 10) / 10;
                double radius = 3 + wave * 3;
                displays.ring(player, origin.clone().add(0, 0.15, 0), radius, Material.MAGMA_BLOCK, 0.28, sword.particleColor(), 22);
                for (int i = 0; i < 12; i++) {
                    double angle = Math.PI * 2 * i / 12 + wave * 0.2;
                    Location stone = origin.clone().add(Math.cos(angle) * radius, 1.1 + wave * 0.35, Math.sin(angle) * radius);
                    displays.box(player, stone, i % 2 == 0 ? Material.DEEPSLATE : Material.MAGMA_BLOCK,
                            0.75, 2.2 + wave * 0.55, 0.75, sword.particleColor(), 24);
                    effects.particle(stone, Particle.LAVA, 3, 0.2, 0);
                    effects.particle(stone, Particle.CRIT, 8, 0.5, 0.03);
                }
                hitArea(player, origin, radius + 0.7, damage, 0.8, 0, 25, 0, hits, 3);
                effects.particle(origin.clone().add(0, 0.7, 0), Particle.EXPLOSION, 1, 0, 0);
                effects.sound(origin, sword, true);
            }
            if (frame >= 10) effects.ring(origin.clone().add(0, 0.3, 0), Math.min(10, (frame - 10) * 0.4 + 1), sword);
            return true;
        });
    }

    private void repeat(Player player, Location origin, long delay, long period, IntPredicate action) {
        UUID owner = player.getUniqueId();
        Set<BukkitRunnable> owned = tasks.computeIfAbsent(owner, ignored -> new HashSet<>());
        if (owned.size() >= 16) return;
        BukkitRunnable task = new BukkitRunnable() {
            int frame;
            private void stop() {
                cancel();
                owned.remove(this);
                if (owned.isEmpty()) tasks.remove(owner, owned);
            }
            @Override public void run() {
                try {
                    if (!valid(player, origin) || !action.test(frame++)) stop();
                } catch (RuntimeException error) {
                    stop();
                    plugin.getLogger().warning("Stopped a sword effect: " + error.getMessage());
                }
            }
        };
        owned.add(task);
        task.runTaskTimer(plugin, delay, period);
    }

    public void cancel(Player player) {
        Set<BukkitRunnable> owned = tasks.remove(player.getUniqueId());
        if (owned != null) owned.forEach(BukkitRunnable::cancel);
        displays.cancel(player);
    }
    @Override public void close() {
        tasks.values().forEach(owned -> owned.forEach(BukkitRunnable::cancel));
        tasks.clear(); displays.close();
    }
}
