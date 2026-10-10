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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Fifty spell sets with bounded displays, staged ultimates and protected damage. */
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
    private final Map<UUID, PendingUltimate> pendingUltimates = new HashMap<>();
    private boolean closed;

    private static final class PendingUltimate {
        BukkitTask fallback;
    }

    public AbilityController(LegendarySwordsPlugin plugin, PluginSettings settings, PlayerStateStore states, CooldownBook cooldowns,
                             CombatService combat, VisualEffects effects, CinematicDirector cinematics, AnimationBridge animations) {
        this.plugin = plugin; this.settings = settings; this.states = states; this.cooldowns = cooldowns;
        this.combat = combat; this.effects = effects; this.cinematics = cinematics; this.animations = animations;
        displays = new EffectDisplay(plugin);
    }

    public void activate(Player player, SwordType sword, boolean ultimate) {
        if (closed || !player.hasPermission("legendaryswords.use") || !player.isOnline() || player.isDead()
                || player.getGameMode() == GameMode.SPECTATOR) return;
        if (pendingUltimates.containsKey(player.getUniqueId())) {
            effects.message(player, "An ultimate is still charging", sword);
            return;
        }
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
        if (!ultimate) {
            launch(player, sword, origin, direction, stats.abilityDamage(), false);
            return;
        }
        PendingUltimate pending = new PendingUltimate();
        UUID owner = player.getUniqueId();
        pendingUltimates.put(owner, pending);
        Runnable release = () -> {
            if (!pendingUltimates.remove(owner, pending)) return;
            if (pending.fallback != null) pending.fallback.cancel();
            if (!valid(player, origin)) return;
            effects.sound(origin, sword, true);
            try { animations.playRelease(player, sword.id()); }
            catch (RuntimeException | LinkageError error) { plugin.getLogger().warning("Ultimate follow-through unavailable; damage still runs: " + error.getMessage()); }
            launch(player, sword, player.getLocation().clone(), player.getEyeLocation().getDirection().clone().normalize(), stats.ultimateDamage(), true);
        };
        try {
            pending.fallback = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // A stalled/failed visual scene must still release exactly one attack.
                try { cinematics.cancel(player); }
                finally { release.run(); }
            }, Math.max(12, cinematics.durationTicks(sword)) + 4L);
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Ultimate scene timer unavailable: " + error.getMessage());
            release.run();
            return;
        }
        try { cinematics.start(player, sword, release); }
        catch (RuntimeException | LinkageError error) {
            plugin.getLogger().warning("Cosmetic ritual unavailable: " + error.getMessage());
            release.run();
        }
    }

    private void launch(Player player, SwordType sword, Location origin, Vector direction, double damage, boolean ultimate) {
        switch (sword) {
            case MASTER_SWORD -> laser(player, sword, player.getEyeLocation().clone(), direction, damage, ultimate);
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
            default -> {
                if (sword.ordinal() >= 24) domainAttack(player, sword, origin, direction, damage, ultimate);
                else relicAttack(player, sword, origin, direction, damage, ultimate);
            }
        }
    }

    /** New relics share lifecycle safeguards, with separate spatial attack choreography. */
    private void relicAttack(Player player, SwordType sword, Location origin, Vector direction, double damage, boolean ultimate) {
        int pattern = sword.ordinal() - 8;
        boolean aimedBeam = pattern == 0 || pattern == 4 || pattern == 11;
        Vector forward = aimedBeam ? direction.clone().normalize() : flat(direction);
        Vector side = new Vector(-forward.getZ(), 0, forward.getX());
        Location source = aimedBeam ? player.getEyeLocation().clone() : origin.clone().add(0, 1.1, 0);
        double reach = unobstructed(source, forward, ultimate ? 14 : 9);
        Location focus = origin.clone().add(forward.clone().multiply(Math.min(reach, ultimate ? 5 : 3)));
        Map<UUID, Integer> hits = new HashMap<>();
        int duration = ultimate ? 32 + pattern % 4 * 4 : 16 + pattern % 3 * 3;
        int maximumHits = ultimate ? 3 : 1;
        Material glass = switch (pattern % 6) {
            case 0 -> Material.WHITE_STAINED_GLASS;
            case 1 -> Material.LIGHT_BLUE_STAINED_GLASS;
            case 2 -> Material.YELLOW_STAINED_GLASS;
            case 3 -> Material.PINK_STAINED_GLASS;
            case 4 -> Material.LIME_STAINED_GLASS;
            default -> Material.PURPLE_STAINED_GLASS;
        };
        repeat(player, origin, 0, 2, frame -> {
            if (frame >= duration) return false;
            double progress = (frame + 1D) / duration, angle = frame * .25;
            boolean impact = frame % (ultimate ? 6 : 3) == 0;
            switch (pattern) {
                case 0, 4 -> { // Radiant fan and the focused Triforce triad.
                    int rays = pattern == 4 ? 3 : ultimate ? 7 : 3;
                    for (int ray = 0; ray < rays; ray++) {
                        Vector heading = forward.clone().rotateAroundY((ray - (rays - 1) / 2D) * (pattern == 4 ? .15 : .25));
                        double distance = unobstructed(source, heading, reach);
                        Location end = source.clone().add(heading.clone().multiply(distance));
                        effects.line(source, end, sword, .55);
                        if (frame % 6 == 0) displays.line(player, source, end, glass, ultimate ? .25 : .12, sword.particleColor(), 10);
                        if (impact) for (LivingEntity target : nearby(player, source, distance + 1)) {
                            if (hits.getOrDefault(target.getUniqueId(), 0) < maximumHits
                                    && Geometry.distanceSquaredToSegment(middle(target).toVector(), source.toVector(), end.toVector()) < .9
                                    && strike(player, target, source, damage, .18, 0, 0, 0)) hits.merge(target.getUniqueId(), 1, Integer::sum);
                        }
                    }
                }
                case 1, 13 -> { // Advancing light corkscrew / tall sand tornado.
                    Location center = origin.clone().add(forward.clone().multiply(reach * progress));
                    double radius = pattern == 13 ? (ultimate ? 3 : 1.8) : (ultimate ? 2 : 1);
                    effects.helix(center, radius, pattern == 13 ? 5 : 2.5, angle, sword);
                    effects.ring(center.clone().add(0, .2, 0), radius, sword);
                    if (frame % 3 == 0) displays.box(player, center.clone().add(0, 1, 0), glass, .35, 2, .35, sword.particleColor(), 5);
                    if (impact) hitArea(player, center, radius + .6, damage, .2, 0, pattern == 13 ? 40 : 0, 0, hits, maximumHits);
                }
                case 2, 10 -> { // Falling stars / six stationary sealing pillars.
                    int points = pattern == 10 ? 6 : ultimate ? 8 : 3;
                    double radius = ultimate ? 4 : 2;
                    for (int point = 0; point < points; point++) {
                        double theta = point * Math.PI * 2 / points + (pattern == 2 ? angle * .3 : 0);
                        Location landing = focus.clone().add(Math.cos(theta) * radius, .15, Math.sin(theta) * radius);
                        Location top = landing.clone().add(0, pattern == 10 ? 6 : 8 * (1 - progress) + 2, 0);
                        effects.line(landing, top, sword, .4);
                        if (frame % 6 == point % 6) displays.line(player, landing, top, glass, .28, sword.particleColor(), 10);
                        if (impact) hitArea(player, landing, 2.2, damage, .25, 0, 25, 0, hits, maximumHits);
                    }
                    effects.ring(focus.clone().add(0, .2, 0), radius, sword);
                    if (ultimate && impact) hitArea(player, focus, radius, damage, .25, 0, 25, 0, hits, maximumHits);
                }
                case 3, 9 -> { // Concentric light bloom / layered fairy flower petals.
                    Location center = pattern == 3 ? origin : focus;
                    double radius = (ultimate ? 7 : 4) * (.25 + progress * .75);
                    effects.ring(center.clone().add(0, .3, 0), radius, sword);
                    int petals = pattern == 3 ? 4 : 7;
                    for (int petal = 0; petal < petals; petal++) {
                        double theta = angle + petal * Math.PI * 2 / petals;
                        Location end = center.clone().add(Math.cos(theta) * radius, .5 + Math.sin(angle) * .3, Math.sin(theta) * radius);
                        effects.line(center.clone().add(0, .6, 0), end, sword, .4);
                        if (frame % 6 == 0) displays.line(player, center.clone().add(0, .6, 0), end, glass, .2, sword.particleColor(), 8);
                    }
                    if (impact) hitArea(player, center, radius, damage, .25, 0, 0, 0, hits, maximumHits);
                }
                case 5, 8 -> { // Sweeping phantom blades / rotating golden petal volley.
                    int blades = ultimate ? 5 : 3;
                    for (int blade = 0; blade < blades; blade++) {
                        double spread = (blade - (blades - 1) / 2D) * (pattern == 5 ? .7 : 1.1);
                        Location center = source.clone().add(forward.clone().multiply(reach * progress)).add(side.clone().multiply(spread));
                        effects.arc(center, forward, pattern == 5 ? 1.2 : .8, 85, sword);
                        if (frame % 4 == 0) displays.line(player, center.clone().add(side.clone().multiply(-.7)), center.clone().add(side.clone().multiply(.7)), glass, .18, sword.particleColor(), 6);
                        if (impact) hitArea(player, center.clone().subtract(0, 1.1, 0), 1.8, damage, .2, 0, 0, 0, hits, maximumHits);
                    }
                }
                case 6, 15 -> { // Parallel spirit tracks / branching twilight fracture.
                    Location center = origin.clone().add(forward.clone().multiply(reach * progress));
                    int lanes = ultimate ? 3 : 2;
                    for (int lane = 0; lane < lanes; lane++) {
                        double offset = (lane - (lanes - 1) / 2D) * (pattern == 15 ? progress * 4 : 1.2);
                        Location point = center.clone().add(side.clone().multiply(offset)).add(0, .2, 0);
                        effects.helix(point, .3, pattern == 15 ? 2 : 1, angle, sword);
                        if (frame % 3 == 0) displays.box(player, point.clone().add(0, .5, 0), glass, .4, pattern == 15 ? 1.6 : .3, .5, sword.particleColor(), 7);
                        if (impact) hitArea(player, point, 1.7, damage, .25, 0, 20, 0, hits, maximumHits);
                    }
                }
                case 7, 12 -> { // Wide razor wheel / focused hexagonal blade array.
                    Location center = pattern == 7 ? origin : focus;
                    double radius = pattern == 7 ? 1.5 + progress * (ultimate ? 4.5 : 2) : ultimate ? 4 : 2.5;
                    int points = pattern == 7 ? 8 : 6;
                    for (int point = 0; point < points; point++) {
                        double theta = angle + point * Math.PI * 2 / points;
                        Location from = center.clone().add(Math.cos(theta) * radius, 1, Math.sin(theta) * radius);
                        Location to = center.clone().add(Math.cos(theta + .5) * radius, 1.3, Math.sin(theta + .5) * radius);
                        effects.line(from, to, sword, .22);
                        if (frame % 4 == 0) displays.line(player, from, to, glass, .18, sword.particleColor(), 6);
                    }
                    if (impact) hitArea(player, center, radius + .5, damage, .25, 0, 0, 0, hits, maximumHits);
                }
                case 11 -> { // Rapid ancient energy pulses rather than a sustained beam.
                    Location end = source.clone().add(forward.clone().multiply(reach));
                    if (frame % 3 == 0) {
                        effects.line(source, end, sword, .25); effects.pulse(end, sword);
                        displays.line(player, source, end, glass, ultimate ? .5 : .25, sword.particleColor(), 3);
                        for (LivingEntity target : nearby(player, source, reach + 1)) {
                            if (hits.getOrDefault(target.getUniqueId(), 0) < maximumHits
                                    && Geometry.distanceSquaredToSegment(middle(target).toVector(), source.toVector(), end.toVector()) < 1.1
                                    && strike(player, target, source, damage, .15, 0, 0, 0)) hits.merge(target.getUniqueId(), 1, Integer::sum);
                        }
                    }
                }
                case 14 -> { // Rising vine cage: visible roots and a real slowing snare.
                    double radius = ultimate ? 4 : 2.5;
                    effects.ring(focus.clone().add(0, .1, 0), radius, sword);
                    for (int vine = 0; vine < 7; vine++) {
                        double theta = vine * Math.PI * 2 / 7;
                        Location point = focus.clone().add(Math.cos(theta) * radius, 0, Math.sin(theta) * radius);
                        effects.helix(point, .25, 1 + progress * 3, angle, sword);
                        if (frame % 5 == 0) displays.box(player, point.clone().add(0, 1, 0), Material.LIME_STAINED_GLASS, .25, 2, .25, sword.particleColor(), 8);
                    }
                    if (impact) hitArea(player, focus, radius, damage, .05, 0, ultimate ? 100 : 60, 1, hits, maximumHits);
                }
                default -> throw new IllegalArgumentException("Unknown relic pattern: " + sword.id());
            }
            return true;
        });
    }

    /** Second collection: spatial hazards with their own footprints, timings and hit rules. */
    private void domainAttack(Player player, SwordType sword, Location origin, Vector direction, double damage, boolean ultimate) {
        Vector forward = flat(direction), right = new Vector(-forward.getZ(), 0, forward.getX());
        double distance = unobstructed(player.getEyeLocation(), forward, ultimate ? 16 : 10);
        Location focus = origin.clone().add(forward.clone().multiply(Math.min(3, distance)));
        Map<UUID, Integer> hits = new HashMap<>();
        int duration = ultimate ? 80 : 40, limit = ultimate ? 3 : 1;
        double radius = ultimate ? 8 : 5;
        // Every mine has its own armed/triggered state, rather than an immediate area strike.
        boolean[] triggered = new boolean[9];
        repeat(player, origin, 0, 2, frame -> {
            if (frame >= duration) return false;
            double progress = frame / (double) (duration - 1), angle = frame * .14;
            boolean impact = frame >= 4 && frame % 12 == 4;
            boolean draw = frame % 4 == 0;
            if (frame % 6 == 0) signatureVisuals(player, sword, origin, focus, forward, right, radius, progress, frame, ultimate);
            switch (sword) {
                case EMBER_HALO -> {
                    double wave = 1.2 + ((frame % 24) / 23D) * radius;
                    effects.ring(origin.clone().add(0, .4, 0), wave, sword);
                    if (draw) ringMesh(player, origin.clone().add(0, .4, 0), wave, 12, sword);
                    // Only the visible rim burns, leaving a safe hole in its center.
                    if (frame >= 4) ringHit(player, origin, wave, .8, damage, .35, 60, 0, hits, limit);
                    if (ultimate && draw) dome(focus, radius, progress, sword);
                }
                case VOID_REAVER -> {
                    double wave = radius * (1 - (frame % 30) / 30D);
                    effects.ring(focus.clone().add(0, .5, 0), wave, sword);
                    if (draw) ringMesh(player, focus.clone().add(0, .5, 0), wave, 10, sword);
                    if (frame >= 4) ringHit(player, focus, wave, .85, damage, -.3, 0, 35, hits, limit);
                    if (ultimate && draw) dome(focus, radius, -progress, sword);
                }
                case ASTRAL_EDGE -> {
                    int satellites = ultimate ? 5 : 3;
                    for (int i = 0; i < satellites; i++) {
                        double theta = angle + i * Math.PI * 2 / satellites;
                        Location star = focus.clone().add(Math.cos(theta) * radius * .6 * (1 - progress * .7), .8 + Math.sin(theta * 2) * .5, Math.sin(theta) * radius * .6 * (1 - progress * .7));
                        effects.pulse(star, sword);
                        if (draw) displays.box(player, star, Material.SEA_LANTERN, .25, .25, .25, sword.particleColor(), 8);
                        if (impact) hitArea(player, star, 2.3, damage, .15, 0, 0, 0, hits, limit);
                    }
                    if (ultimate && draw) dome(focus, radius, progress, sword);
                }
                case TIDEBREAKER -> {
                    Location wall = origin.clone().add(forward.clone().multiply(distance * progress));
                    for (int height = 0; height < (ultimate ? 4 : 2); height++) {
                        Location a = wall.clone().add(right.clone().multiply(-radius)).add(0, .6 + height, 0);
                        Location b = wall.clone().add(right.clone().multiply(radius)).add(0, .6 + height, 0);
                        segment(player, a, b, .8, damage, .65, 0, 25, sword, hits, limit, draw, frame >= 4);
                    }
                    if (ultimate && draw) effects.ring(focus.clone().add(0, .2, 0), radius, sword);
                }
                case STORM_CAGE -> {
                    if (draw) {
                        ringMesh(player, focus.clone().add(0, .2, 0), radius, 6, sword);
                        for (int i = 0; i < 6; i++) {
                            Location pole = focus.clone().add(Math.cos(i * Math.PI / 3) * radius, 0, Math.sin(i * Math.PI / 3) * radius);
                            effects.line(pole, pole.clone().add(0, 4, 0), sword, .45);
                        }
                    }
                    if (impact) {
                        Location bolt = focus.clone().add(0, 6, 0);
                        effects.line(bolt, focus, sword, .3); effects.pulse(focus, sword);
                        hitArea(player, focus, radius, damage, .1, 0, 35, 1, hits, limit);
                    }
                }
                case CHRONO_SABER -> {
                    effects.ring(focus.clone().add(0, .2, 0), radius, sword);
                    Location hand = focus.clone().add(Math.cos(angle) * radius, 1, Math.sin(angle) * radius);
                    segment(player, focus.clone().add(0, 1, 0), hand, .85, damage, 0, 0, 70, sword, hits, limit, draw, impact);
                    if (ultimate && draw) dome(focus, radius, progress, sword);
                    if (frame == duration - 2) hitArea(player, focus, radius, damage, .25, 0, 50, 1, hits, limit);
                }
                case ECLIPSE_FANG -> {
                    double separation = radius * (1 - (frame % 24) / 24D);
                    for (int sign : new int[]{-1, 1}) {
                        Location orb = focus.clone().add(right.clone().multiply(sign * separation)).add(0, .8, 0);
                        effects.helix(orb, .65, 1.5, angle * sign, sword);
                        if (draw) displays.box(player, orb, Material.PURPLE_STAINED_GLASS, .6, .6, .6, sword.particleColor(), 8);
                        if (impact || (frame >= 4 && separation < 1)) hitArea(player, orb, 2, damage, -.15, 0, 0, 0, hits, limit);
                    }
                    if (ultimate && draw) dome(focus, radius, -progress, sword);
                }
                case CRYSTAL_OATH -> {
                    int mines = ultimate ? 9 : 3;
                    for (int i = 0; i < mines; i++) {
                        Location mine = focus.clone().add((i % 3 - 1) * 2.2, .1, (i / 3 - (ultimate ? 1 : 0)) * 2.2);
                        if (triggered[i]) continue;
                        if (draw) { effects.ring(mine, .7, sword); displays.box(player, mine.clone().add(0, .5, 0), Material.LIGHT_BLUE_STAINED_GLASS, .25, 1, .25, sword.particleColor(), 8); }
                        if (frame >= 6 && (hasTarget(player, mine, 1.9) || frame == duration - 2)) {
                            triggered[i] = true; effects.helix(mine, 1.8, 3, angle, sword);
                            hitArea(player, mine, 2.6, damage, .4, 0, 40, 0, hits, limit);
                        }
                    }
                }
                case BLOODTHORN -> {
                    int thorns = ultimate ? 8 : 4;
                    for (int i = 0; i < thorns; i++) {
                        double theta = i * Math.PI * 2 / thorns;
                        Location root = ultimate ? focus.clone().add(Math.cos(theta) * radius * .75, 0, Math.sin(theta) * radius * .75)
                                : origin.clone().add(forward.clone().multiply((i + 1) * 1.8));
                        if (draw) effects.helix(root, .35, .5 + (i + frame % 8) * .3, angle, sword);
                        if (frame >= 4 && frame % 12 == i % 12) hitArea(player, root, 2.2, damage, .05, 0, 60, 1, hits, limit);
                    }
                    if (ultimate && draw) ringMesh(player, focus, radius, 8, sword);
                }
                case SUNSPIRE -> {
                    int rays = ultimate ? 6 : 1;
                    for (int i = 0; i < rays; i++) {
                        Location mark = i == 0 ? focus : focus.clone().add(Math.cos(i * Math.PI / 3) * 3, 0, Math.sin(i * Math.PI / 3) * 3);
                        effects.ring(mark.clone().add(0, .15, 0), 1.3, sword);
                        if (frame % 16 == 8) {
                            segment(player, mark.clone().add(0, 9, 0), mark, 1.4, damage, .3, 50, 0, sword, hits, limit, true, true);
                            effects.pulse(mark, sword);
                        }
                    }
                }
                case MOONVEIL -> {
                    double travel = Math.sin(progress * Math.PI) * distance;
                    for (int i = 0; i < (ultimate ? 3 : 1); i++) {
                        Vector heading = forward.clone().rotateAroundY((i - (ultimate ? 1 : 0)) * .45);
                        Location blade = origin.clone().add(heading.clone().multiply(travel)).add(0, 1, 0);
                        effects.arc(blade, heading, 1.4, 100, sword);
                        if (draw) ringMesh(player, blade, 1.3, 7, sword);
                        if (frame >= 4) hitArea(player, blade, 2.2, damage, .15, 0, 0, 0, hits, limit);
                    }
                }
                case RUNIC_JUDGMENT -> {
                    if (draw) effects.ring(focus, radius, sword);
                    for (int i = -(ultimate ? 2 : 0); i <= (ultimate ? 2 : 0); i++) {
                        Location row = focus.clone().add(right.clone().multiply(i * 2)).add(0, 1, 0);
                        segment(player, row.clone().add(forward.clone().multiply(-radius)), row.clone().add(forward.clone().multiply(radius)), .65, damage, .2, 0, 30, sword, hits, limit, draw, frame % 16 == 8);
                        Location column = focus.clone().add(forward.clone().multiply(i * 2)).add(0, 1, 0);
                        segment(player, column.clone().add(right.clone().multiply(-radius)), column.clone().add(right.clone().multiply(radius)), .65, damage, .2, 0, 30, sword, hits, limit, draw, frame % 16 == 8);
                    }
                }
                case NEBULA_BLADE -> {
                    Vector aim = direction.clone().normalize();
                    Vector tangent = aim.clone().crossProduct(new Vector(0, 1, 0));
                    if (tangent.lengthSquared() < .001) tangent = new Vector(1, 0, 0); else tangent.normalize();
                    Vector second = tangent.clone().crossProduct(aim).normalize();
                    double reach = unobstructed(player.getEyeLocation(), aim, 12);
                    Location head = player.getEyeLocation().clone().add(aim.clone().multiply(reach * progress));
                    for (int i = 0; i < (ultimate ? 3 : 1); i++) {
                        double theta = angle * 3 + i * Math.PI * 2 / 3;
                        Location orb = head.clone().add(tangent.clone().multiply(Math.cos(theta))).add(second.clone().multiply(Math.sin(theta)));
                        effects.pulse(orb, sword);
                        if (draw) displays.box(player, orb, Material.PURPLE_STAINED_GLASS, .35, .35, .35, sword.particleColor(), 8);
                        if (frame >= 4) hitArea(player, orb, 1.8, damage, .15, 0, 0, 0, hits, limit);
                    }
                    if (ultimate && draw) dome(focus, radius, progress, sword);
                }
                case OBSIDIAN_MAW -> {
                    int tooth = frame / 6;
                    Location vent = origin.clone().add(forward.clone().multiply(Math.min(distance, tooth * 1.3)));
                    if (draw) effects.ring(vent, 1, sword);
                    if (frame % 6 == 4) {
                        effects.helix(vent, 1.2, 3, angle, sword);
                        displays.box(player, vent.clone().add(0, 1, 0), Material.MAGMA_BLOCK, .8, 2, .8, sword.particleColor(), 6);
                        hitArea(player, vent, 2.4, damage, .45, 50, 0, 0, hits, limit);
                    }
                    if (ultimate) { double rim = 1 + progress * radius; if (draw) ringMesh(player, focus, rim, 12, sword); if (impact) ringHit(player, focus, rim, 1, damage, .5, 60, 0, hits, limit); }
                }
                case SERAPH_WING -> {
                    for (int sign : new int[]{-1, 1}) {
                        Location tip = focus.clone().add(right.clone().multiply(sign * radius * progress)).add(forward.clone().multiply(-2 * progress)).add(0, 1 + progress * 2, 0);
                        segment(player, focus.clone().add(0, 1, 0), tip, 1, damage, .35, 0, 0, sword, hits, limit, draw, frame >= 4);
                    }
                    if (ultimate && frame % 16 == 8) {
                        segment(player, focus.clone().add(0, 7, 0), focus, 1.6, damage, .4, 0, 0, sword, hits, limit, true, true);
                    }
                }
                case ABYSSAL_TRIDENT -> {
                    if (!ultimate) {
                        for (int i = -1; i <= 1; i++) {
                            Location spear = origin.clone().add(forward.clone().multiply(distance * progress)).add(right.clone().multiply(i * 1.2)).add(0, 1, 0);
                            segment(player, spear, spear.clone().add(forward), .8, damage, .4, 0, 25, sword, hits, limit, draw, frame >= 4);
                        }
                    } else {
                        if (draw) { effects.helix(focus, radius * (1 - progress * .6), 4, -angle, sword); ringMesh(player, focus, radius, 12, sword); }
                        if (impact) domainHit(player, focus, radius, damage, -.45, 0, 45, hits, limit);
                    }
                }
                case MIRAGE_SABER -> {
                    int echoes = ultimate ? 5 : 3;
                    for (int i = 0; i < echoes; i++) {
                        Location echo = focus.clone().add(Math.cos(i * 2.4) * i * .8, 0, Math.sin(i * 2.4) * i * .8);
                        if (draw && frame < 8 + i * 5) { effects.ring(echo, 1, sword); effects.arc(echo.clone().add(0, 1, 0), forward, 1.5, 80, sword); }
                        if (frame == 8 + i * 5) { effects.pulse(echo, sword); hitArea(player, echo, 2.5, damage, .2, 0, 25, 0, hits, limit); }
                    }
                }
                case METEOR_BRAND -> {
                    int count = ultimate ? 9 : 3;
                    for (int i = 0; i < count; i++) {
                        Location mark = i == 0 ? focus : focus.clone().add(Math.cos(i * 2.4) * 3, 0, Math.sin(i * 2.4) * 3);
                        int landing = 10 + i * 5;
                        if (frame <= landing && frame >= landing - 8) {
                            effects.ring(mark, 1.4, sword);
                            Location rock = mark.clone().add(0, (landing - frame) * .9 + .8, 0);
                            effects.pulse(rock, sword);
                            if (draw) displays.box(player, rock, Material.MAGMA_BLOCK, .7, .7, .7, sword.particleColor(), 4);
                        }
                        if (frame == landing) { effects.ring(mark, 2.8, sword); hitArea(player, mark, 2.8, damage, .5, 60, 0, 0, hits, limit); }
                    }
                }
                case GLACIAL_CROWN -> {
                    double rim = ultimate ? radius * (1 - progress) : .15 + progress * radius;
                    if (draw) {
                        ringMesh(player, focus, rim, 10, sword);
                        for (int i = 0; i < 10; i++) {
                            Location tooth = focus.clone().add(Math.cos(i * Math.PI / 5) * rim, 0, Math.sin(i * Math.PI / 5) * rim);
                            effects.line(tooth, tooth.clone().add(0, 1.5, 0), sword, .4);
                        }
                    }
                    if (frame >= 4) ringHit(player, focus, rim, .9, damage, .1, 0, 70, hits, limit);
                }
                case TEMPEST_RAPIER -> {
                    Vector aim = direction.clone().normalize();
                    double reach = unobstructed(player.getEyeLocation(), aim, ultimate ? 18 : 12);
                    Location tip = player.getEyeLocation().clone().add(aim.clone().multiply(reach * progress));
                    segment(player, tip, tip.clone().add(aim.clone().multiply(Math.min(2, reach * (1 - progress)))), 1, damage, .45, 0, 0, sword, hits, limit, draw, frame >= 4);
                    if (ultimate && draw) { effects.ring(focus.clone().add(0, .4, 0), radius, sword); dome(focus, radius, progress, sword); }
                    if (ultimate && impact) ringHit(player, focus, radius, 1.2, damage, .6, 0, 0, hits, limit);
                }
                case GRAVITY_CLEAVER -> {
                    double shell = radius * (1 - progress * .75);
                    if (draw) { dome(focus, shell, -progress, sword); effects.helix(focus, shell * .5, 3, -angle, sword); }
                    if (impact && frame < duration - 12) domainHit(player, focus, radius, damage * .65, -.4, 0, 35, hits, limit - 1);
                    if (frame == duration - 8) { effects.ring(focus, radius, sword); domainHit(player, focus, radius, damage, .7, 0, 0, hits, limit); }
                }
                case PRISM_SWORD -> {
                    for (int level = 0; level < (ultimate ? 3 : 1); level++) {
                        for (int i = 0; i < 3; i++) {
                            double theta = angle + i * Math.PI * 2 / 3;
                            double prismRadius = .4 + radius * (1 - progress);
                            Location a = focus.clone().add(Math.cos(theta) * prismRadius, .6 + level * 1.2, Math.sin(theta) * prismRadius);
                            Location b = focus.clone().add(Math.cos(theta + Math.PI * 2 / 3) * prismRadius, .6 + level * 1.2, Math.sin(theta + Math.PI * 2 / 3) * prismRadius);
                            segment(player, a, b, 1.1, damage, .25, 0, 25, sword, hits, limit, draw, frame >= 4);
                        }
                    }
                }
                case THUNDER_DRUM -> {
                    Location center = ultimate ? focus : origin.clone().add(forward.clone().multiply(progress * distance));
                    double shell = .5 + (frame % 20) / 19D * radius;
                    if (draw) { dome(center, shell, progress, sword); effects.ring(center.clone().add(0, .5, 0), shell, sword); }
                    if (frame >= 4) ringHit(player, center, shell, 1, damage, .5, 0, 0, hits, limit);
                }
                case WRAITH_SCYTHE -> {
                    Location center = ultimate ? focus : origin.clone().add(forward.clone().multiply(distance * progress));
                    double theta = -Math.PI * .8 + progress * Math.PI * 1.6;
                    for (int sign : ultimate ? new int[]{-1, 1} : new int[]{1}) {
                        Location edge = center.clone().add(forward.clone().multiply(Math.cos(theta) * radius)).add(right.clone().multiply(Math.sin(theta) * radius * sign)).add(0, 1, 0);
                        segment(player, center.clone().add(0, 1, 0), edge, .9, damage, .25, 0, 40, sword, hits, limit, draw, frame >= 4);
                    }
                }
                case VERDANT_OATH -> {
                    if (!ultimate) {
                        for (int i = 0; i < 3; i++) {
                            Location seed = focus.clone().add(right.clone().multiply((i - 1) * 1.8));
                            if (draw) effects.ring(seed, .6 + progress, sword);
                            if (frame == 8 + i * 6) { effects.helix(seed, 1.5, 2, angle, sword); hitArea(player, seed, 2.3, damage, .05, 0, 70, 1, hits, limit); }
                        }
                    } else {
                        for (int i = 0; i < 5; i++) {
                            Location a = focus.clone().add(-radius + i * radius * .4, .8, (i % 2 == 0 ? -1 : 1) * radius);
                            Location b = focus.clone().add(-radius + (i + 1) * radius * .4, .8, (i % 2 == 0 ? 1 : -1) * radius);
                            segment(player, a, b, 1.1, damage, .05, 0, 70, sword, hits, limit, draw, impact);
                        }
                    }
                }
                case DRAGONHEART -> {
                    int breaths = ultimate ? 5 : 1;
                    if (ultimate && impact) { effects.pulse(focus.clone().add(0, 1, 0), sword); hitArea(player, focus, 1.4, damage, .2, 80, 0, 0, hits, limit); }
                    for (int i = 0; i < breaths; i++) {
                        Vector heading = forward.clone().rotateAroundY(ultimate ? angle + i * Math.PI * 2 / breaths : 0);
                        double reach = unobstructed(origin.clone().add(0, 1, 0), heading, radius);
                        Location mouth = ultimate ? focus.clone().add(0, 1, 0) : origin.clone().add(0, 1, 0);
                        if (draw) { effects.arc(mouth, heading, reach, 28, sword); effects.line(mouth, mouth.clone().add(heading.clone().multiply(reach)), sword, .45); }
                        if (impact) for (LivingEntity target : nearby(player, mouth, reach + 1)) {
                            if (hits.getOrDefault(target.getUniqueId(), 0) < limit
                                    && Geometry.inCone(middle(target).toVector().subtract(mouth.toVector()), heading, reach, 32)
                                    && combat.visible(player.getEyeLocation(), target)
                                    && strike(player, target, mouth, damage, .25, 80, 0, 0)) hits.merge(target.getUniqueId(), 1, Integer::sum);
                        }
                    }
                }
                default -> throw new IllegalArgumentException("Unknown domain weapon: " + sword.id());
            }
            return true;
        });
    }

    private boolean hasTarget(Player player, Location center, double radius) {
        return nearby(player, center, radius + 1).stream().anyMatch(target ->
                middle(target).distanceSquared(center.clone().add(0, .8, 0)) <= radius * radius);
    }

    /** Solid-looking outline uses short-lived displays; actual world blocks never change. */
    private void ringMesh(Player player, Location center, double radius, int sides, SwordType sword) {
        for (int i = 0; i < sides; i++) {
            double a = i * Math.PI * 2 / sides, b = (i + 1) * Math.PI * 2 / sides;
            displays.line(player, center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                    center.clone().add(Math.cos(b) * radius, 0, Math.sin(b) * radius), domainMaterial(sword),
                    .09, sword.particleColor(), 8);
        }
    }

    /** Distinct manifestations: these supplement each weapon's own collision/damage branch. */
    private void signatureVisuals(Player player, SwordType sword, Location origin, Location focus, Vector forward,
                                  Vector right, double radius, double progress, int frame, boolean ultimate) {
        double spin = frame * .15;
        double reach = unobstructed(player.getEyeLocation(), forward, ultimate ? 16 : 10);
        double size = ultimate ? 1 : .65;
        switch (sword) {
            case EMBER_HALO -> {
                for (int i=0;i<8;i++) {
                    double theta=i*Math.PI/4+spin;
                    Location p=origin.clone().add(Math.cos(theta)*radius, .5, Math.sin(theta)*radius);
                    effects.particle(p, Particle.FLAME, 5, .15, .035);
                    displays.line(player,p,p.clone().add(0,1.5*size,0),Material.ORANGE_STAINED_GLASS,.18,sword.particleColor(),12);
                }
            }
            case VOID_REAVER -> {
                portal(player,focus.clone().add(0,2,0),radius*.65,spin,sword);
                for(int i=0;i<6;i++) {double theta=spin+i*Math.PI/3;Location p=focus.clone().add(Math.cos(theta)*radius,1+Math.sin(spin)*.5,Math.sin(theta)*radius);displays.box(player,p,Material.OBSIDIAN,.45,.45,.45,sword.particleColor(),12);effects.line(p,focus.clone().add(0,1,0),sword,.7);}
            }
            case ASTRAL_EDGE -> {
                for(int i=0;i<(ultimate?5:3);i++) {double theta=spin+i*Math.PI*2/(ultimate?5:3);Location star=focus.clone().add(Math.cos(theta)*radius*.6,2+Math.sin(theta)*1.2,Math.sin(theta)*radius*.6);manifestBlade(player,star,sword,.9*size);effects.particle(star,Particle.END_ROD,5,.12,.02);if(frame%12==0)effects.line(star,focus.clone().add(0,1,0),sword,.4);}
            }
            case TIDEBREAKER -> {
                Location wave=origin.clone().add(forward.clone().multiply(reach*progress));
                for(int i=-3;i<=3;i++) {Location crest=wave.clone().add(right.clone().multiply(i*radius/3)).add(0,1.3+Math.cos(i*.5)*size,0);displays.box(player,crest,Material.CYAN_STAINED_GLASS,radius/3,.7+size, .35,sword.particleColor(),12);effects.particle(crest,Particle.SPLASH,8,.2,.04);}
            }
            case STORM_CAGE -> {
                for(int i=0;i<6;i++) {double theta=i*Math.PI/3;Location pole=focus.clone().add(Math.cos(theta)*radius,0,Math.sin(theta)*radius);jaggedBolt(player,pole.clone().add(0,7*size,0),pole,sword);}
                if(frame%12==0)jaggedBolt(player,focus.clone().add(0,10,0),focus,sword);
            }
            case CHRONO_SABER -> {
                for(int i=0;i<12;i++) {double theta=i*Math.PI/6;Location tick=focus.clone().add(Math.cos(theta)*radius,.25,Math.sin(theta)*radius);displays.box(player,tick,Material.GOLD_BLOCK,.18,.2,i%3==0?.8:.4,sword.particleColor(),12);}
                Location center=focus.clone().add(0,.35,0);
                displays.line(player,center,center.clone().add(Math.cos(spin)*radius,0,Math.sin(spin)*radius),Material.YELLOW_STAINED_GLASS,.2,sword.particleColor(),12);
                effects.particle(center,Particle.ENCHANT,8,.4,0);
            }
            case ECLIPSE_FANG -> {
                for(int sign:new int[]{-1,1}) {Location sun=focus.clone().add(right.clone().multiply(sign*radius*(1-progress))).add(0,2,0);portal(player,sun,1.2*size,spin*sign,sword);displays.box(player,sun,Material.BLACK_STAINED_GLASS,.9,.9,.9,sword.particleColor(),12);}
            }
            case CRYSTAL_OATH -> {
                int count=ultimate?9:3;
                for(int i=0;i<count;i++) {Location mine=focus.clone().add((i%3-1)*2.2,0,(i/3-(ultimate?1:0))*2.2);double height=.6+(frame%12)/12D*1.2;displays.box(player,mine.clone().add(0,height/2,0),Material.AMETHYST_BLOCK,.4,height,.4,sword.particleColor(),12);for(int sign:new int[]{-1,1})displays.line(player,mine.clone().add(sign*.6,0,0),mine.clone().add(0,height,0),Material.LIGHT_BLUE_STAINED_GLASS,.15,sword.particleColor(),12);}
            }
            case BLOODTHORN -> {
                for(int i=0;i<(ultimate?8:4);i++) {double theta=i*Math.PI/4;Location thorn=ultimate?focus.clone().add(Math.cos(theta)*radius*.75,0,Math.sin(theta)*radius*.75):origin.clone().add(forward.clone().multiply((i+1)*1.8));Location tip=thorn.clone().add(Math.sin(theta)*.5,2.5*size,Math.cos(theta)*.5);displays.line(player,thorn,tip,Material.RED_STAINED_GLASS,.24,sword.particleColor(),12);effects.particle(tip,Particle.CRIMSON_SPORE,6,.25,0);}
            }
            case SUNSPIRE -> {
                Location top=focus.clone().add(0,8,0);portal(player,top,2*size,spin,sword);
                displays.line(player,top,focus,Material.YELLOW_STAINED_GLASS,.3*size,sword.particleColor(),12);
                effects.particle(focus,Particle.END_ROD,12,.8,.025);
            }
            case MOONVEIL -> {
                double travel=Math.sin(progress*Math.PI)*reach;
                for(int i=0;i<(ultimate?3:1);i++) {Vector heading=forward.clone().rotateAroundY((i-(ultimate?1:0))*.45);Location crescent=origin.clone().add(heading.multiply(travel)).add(0,1.4,0);portalArc(player,crescent,right,2*size,-120,120,sword);manifestBlade(player,crescent,sword,1.1*size);}
            }
            case RUNIC_JUDGMENT -> {
                int rows=ultimate?5:3;
                for(int i=0;i<rows;i++) {double offset=(i-(rows-1)/2D)*2;Location seal=focus.clone().add(right.clone().multiply(offset));displays.box(player,seal.clone().add(0,.12,0),Material.ORANGE_STAINED_GLASS,.65,.08,.65,sword.particleColor(),12);effects.particle(seal,Particle.ENCHANT,6,.4,0);}
                if(ultimate)portal(player,focus.clone().add(0,4,0),radius*.7,spin,sword);
            }
            case NEBULA_BLADE -> {
                // Star clouds are a constellation trail, distinct from the physical corkscrew.
                for(int i=0;i<5;i++) {Location star=focus.clone().add(Math.cos(spin+i*2.4)*radius*.6,2+Math.sin(i*2.4+spin)*1.5,Math.sin(spin+i*2.4)*radius*.6);effects.particle(star,Particle.END_ROD,3,.1,.015);if(i>0)effects.line(star,focus.clone().add(0,3,0),sword,.65);}
            }
            case OBSIDIAN_MAW -> {
                for(int i=0;i<8;i++) {double theta=i*Math.PI/4;Location vent=focus.clone().add(Math.cos(theta)*radius*.7,0,Math.sin(theta)*radius*.7);displays.box(player,vent.clone().add(0,.6,0),Material.OBSIDIAN,.6,1.2,.6,sword.particleColor(),12);effects.particle(vent,Particle.LAVA,4,.2,.02);if(ultimate)displays.line(player,vent,vent.clone().add(0,3,0),Material.ORANGE_STAINED_GLASS,.22,sword.particleColor(),12);}
            }
            case SERAPH_WING -> {
                for(int sign:new int[]{-1,1}) for(int i=0;i<6;i++) {Location feather=focus.clone().add(right.clone().multiply(sign*(1+i*.5))).add(0,1.8+i*.25,0);displays.line(player,feather,feather.clone().add(forward.clone().multiply(-1.8*size)).add(0,1,0),Material.WHITE_STAINED_GLASS,.2,sword.particleColor(),12);effects.particle(feather,Particle.END_ROD,3,.1,.01);}
            }
            case ABYSSAL_TRIDENT -> {
                if(ultimate)for(int i=0;i<3;i++) {double theta=spin+i*Math.PI*2/3;Location p=focus.clone().add(Math.cos(theta)*radius*.6,.8,Math.sin(theta)*radius*.6);manifestBlade(player,p,sword,1.3);effects.particle(p,Particle.SPLASH,8,.25,.03);}
                else for(int i=-1;i<=1;i++) {Location p=origin.clone().add(forward.clone().multiply(reach*progress)).add(right.clone().multiply(i*1.2)).add(0,1,0);displays.line(player,p,p.clone().subtract(forward.clone().multiply(1.8)),Material.CYAN_STAINED_GLASS,.22,sword.particleColor(),12);}
            }
            case MIRAGE_SABER -> {
                for(int i=0;i<(ultimate?5:3);i++) {Location echo=focus.clone().add(Math.cos(i*2.4)*i*.8,1,Math.sin(i*2.4)*i*.8);manifestBlade(player,echo,sword,.9);effects.particle(echo,Particle.ENCHANT,8,.3,0);}
            }
            case METEOR_BRAND -> {
                for(int i=0;i<(ultimate?9:3);i++) {int landing=10+i*5;if(frame>landing||frame<landing-8)continue;Location mark=i==0?focus:focus.clone().add(Math.cos(i*2.4)*3,0,Math.sin(i*2.4)*3);Location rock=mark.clone().add(0,(landing-frame)*.9+1,0);displays.box(player,rock,Material.MAGMA_BLOCK,1.1,1.1,1.1,sword.particleColor(),12);effects.line(rock,rock.clone().add(0,3,0),sword,.35);effects.particle(rock,Particle.FLAME,10,.3,.05);}
            }
            case GLACIAL_CROWN -> {
                for(int i=0;i<10;i++) {double theta=i*Math.PI/5;double rim=ultimate?radius*(1-progress):.15+progress*radius;Location ice=focus.clone().add(Math.cos(theta)*rim,0,Math.sin(theta)*rim);displays.box(player,ice.clone().add(0,1,0),Material.BLUE_ICE,.3,2.2*size,.3,sword.particleColor(),12);effects.particle(ice,Particle.SNOWFLAKE,5,.2,.03);}
            }
            case TEMPEST_RAPIER -> {
                // Vortex ribs show the wind field rather than another static dome.
                for(int i=0;i<4;i++) {double theta=spin+i*Math.PI/2;Location a=focus.clone().add(Math.cos(theta)*radius,.3,Math.sin(theta)*radius);Location b=focus.clone().add(Math.cos(theta+.8)*radius*.5,4*size,Math.sin(theta+.8)*radius*.5);displays.line(player,a,b,Material.LIME_STAINED_GLASS,.1,sword.particleColor(),12);effects.particle(a,Particle.CLOUD,4,.1,.015);}
            }
            case GRAVITY_CLEAVER -> {
                double shell=radius*(1-progress*.8);portal(player,focus.clone().add(0,2,0),shell,spin,sword);
                for(int i=0;i<6;i++) {double theta=i*Math.PI/3+spin;Location shard=focus.clone().add(Math.cos(theta)*shell,1+i%3,Math.sin(theta)*shell);displays.box(player,shard,Material.OBSIDIAN,.4,.4,.4,sword.particleColor(),12);effects.line(shard,focus.clone().add(0,1,0),sword,.65);}
            }
            case PRISM_SWORD -> {
                Location apex=focus.clone().add(0,5*size,0);
                for(int i=0;i<3;i++) {double theta=spin+i*Math.PI*2/3;Location corner=focus.clone().add(Math.cos(theta)*radius,0,Math.sin(theta)*radius);displays.line(player,corner,apex,Material.MAGENTA_STAINED_GLASS,.2,sword.particleColor(),12);effects.particle(corner,Particle.END_ROD,4,.2,.02);}
            }
            case THUNDER_DRUM -> {
                double shell=1+(frame%20)/19D*radius;
                portal(player,focus.clone().add(0,1,0),shell,spin,sword);
                if(frame%12==0)jaggedBolt(player,focus.clone().add(0,6,0),focus,sword);
            }
            case WRAITH_SCYTHE -> {
                for(int i=0;i<(ultimate?4:2);i++) {double theta=spin+i*Math.PI/2;Location ghost=focus.clone().add(Math.cos(theta)*radius*.6,1.2,Math.sin(theta)*radius*.6);manifestBlade(player,ghost,sword,1.2*size);effects.particle(ghost,Particle.SOUL,4,.15,.02);}
            }
            case VERDANT_OATH -> {
                for(int i=0;i<(ultimate?7:3);i++) {Location root=focus.clone().add(Math.cos(i*2.4)*i*.6,0,Math.sin(i*2.4)*i*.6);displays.line(player,root,root.clone().add(Math.sin(spin+i)*.5,2.8*size,Math.cos(spin+i)*.5),Material.MOSS_BLOCK,.2,sword.particleColor(),12);effects.particle(root.clone().add(0,2,0),Particle.HAPPY_VILLAGER,5,.35,0);}
            }
            case DRAGONHEART -> {
                Location mouth=ultimate?focus.clone().add(0,1.5,0):origin.clone().add(0,1.5,0);
                for(int i=-3;i<=3;i++) {Vector heading=forward.clone().rotateAroundY(i*.12+(ultimate?spin:0));Location tip=mouth.clone().add(heading.multiply(radius));effects.particle(tip,Particle.FLAME,8,.25,.06);displays.line(player,mouth,tip,Material.ORANGE_STAINED_GLASS,.07,sword.particleColor(),12);}
            }
            default -> { }
        }
    }
    private void manifestBlade(Player player, Location location, SwordType sword, double scale) {
        if(plugin.swords()!=null) displays.blade(player,location,plugin.swords().create(sword),scale,sword.particleColor(),12);
    }
    private void portal(Player player, Location center, double radius, double turn, SwordType sword) {
        Vector horizontal=new Vector(Math.cos(turn),0,Math.sin(turn));
        portalArc(player,center,horizontal,radius,0,360,sword);
    }
    private void portalArc(Player player, Location center, Vector horizontal, double radius, double start, double end, SwordType sword) {
        for(int i=0;i<16;i++) {
            double a=Math.toRadians(start+(end-start)*i/16),b=Math.toRadians(start+(end-start)*(i+1)/16);
            Location from=center.clone().add(horizontal.clone().multiply(Math.cos(a)*radius)).add(0,Math.sin(a)*radius,0);
            Location to=center.clone().add(horizontal.clone().multiply(Math.cos(b)*radius)).add(0,Math.sin(b)*radius,0);
            displays.line(player,from,to,domainMaterial(sword),.12,sword.particleColor(),12);effects.line(from,to,sword,.5);
        }
    }
    private void jaggedBolt(Player player, Location from, Location to, SwordType sword) {
        Location previous=from;
        for(int i=1;i<=7;i++) {
            Location next=from.clone().add(to.toVector().subtract(from.toVector()).multiply(i/7D));
            if(i<7)next.add(i%2==0?.35:-.35,0,i%3==0?.3:-.15);
            displays.line(player,previous,next,Material.LIGHT_BLUE_STAINED_GLASS,.16,sword.particleColor(),12);
            effects.particle(next,Particle.ELECTRIC_SPARK,4,.1,.025); previous=next;
        }
    }

    private Material domainMaterial(SwordType sword) {
        return switch (sword) {
            case EMBER_HALO, OBSIDIAN_MAW, DRAGONHEART -> Material.MAGMA_BLOCK;
            case VOID_REAVER, ECLIPSE_FANG, GRAVITY_CLEAVER -> Material.PURPLE_STAINED_GLASS;
            case ASTRAL_EDGE, SUNSPIRE, SERAPH_WING -> Material.SEA_LANTERN;
            case TIDEBREAKER, ABYSSAL_TRIDENT -> Material.CYAN_STAINED_GLASS;
            case GLACIAL_CROWN -> Material.BLUE_ICE;
            case CRYSTAL_OATH, PRISM_SWORD -> Material.AMETHYST_BLOCK;
            case BLOODTHORN -> Material.RED_STAINED_GLASS;
            case VERDANT_OATH -> Material.MOSS_BLOCK;
            case CHRONO_SABER, THUNDER_DRUM -> Material.YELLOW_STAINED_GLASS;
            case MOONVEIL, WRAITH_SCYTHE -> Material.LIGHT_BLUE_STAINED_GLASS;
            case MIRAGE_SABER, RUNIC_JUDGMENT, METEOR_BRAND -> Material.ORANGE_STAINED_GLASS;
            case NEBULA_BLADE -> Material.MAGENTA_STAINED_GLASS;
            case TEMPEST_RAPIER -> Material.LIME_STAINED_GLASS;
            default -> Material.WHITE_STAINED_GLASS;
        };
    }

    private void dome(Location center, double radius, double turn, SwordType sword) {
        for (int rib = 0; rib < 4; rib++) {
            double theta = rib * Math.PI / 4 + turn;
            for (int step = 0; step < 16; step++) {
                double arc = step * Math.PI / 16;
                effects.dust(center.clone().add(Math.cos(theta) * Math.cos(arc) * radius,
                        .2 + Math.sin(arc) * radius, Math.sin(theta) * Math.cos(arc) * radius), sword, 1, 0);
            }
        }
        effects.ring(center.clone().add(0, .2, 0), radius, sword);
    }

    private void segment(Player player, Location from, Location to, double width, double damage, double push, int fire,
                         int slow, SwordType sword, Map<UUID, Integer> hits, int limit, boolean draw, boolean impact) {
        if (draw) { effects.line(from, to, sword, .4); displays.line(player, from, to, domainMaterial(sword), .12, sword.particleColor(), 8); }
        if (!impact) return;
        Location center = from.clone().add(to).multiply(.5);
        double length = from.distance(to);
        for (LivingEntity target : nearby(player, center, length / 2 + width + 1)) {
            if (hits.getOrDefault(target.getUniqueId(), 0) < limit
                    && Geometry.distanceSquaredToSegment(middle(target).toVector(), from.toVector(), to.toVector()) <= width * width
                    && strike(player, target, player.getEyeLocation(), damage, push, fire, slow, 0)) hits.merge(target.getUniqueId(), 1, Integer::sum);
        }
    }

    private void ringHit(Player player, Location center, double radius, double thickness, double damage, double push,
                         int fire, int slow, Map<UUID, Integer> hits, int limit) {
        for (LivingEntity target : nearby(player, center, radius + thickness + 1)) {
            Vector offset = middle(target).toVector().subtract(center.toVector());
            double horizontal = Math.hypot(offset.getX(), offset.getZ());
            if (Math.abs(horizontal - radius) <= thickness && Math.abs(offset.getY() - .8) <= 2
                    && hits.getOrDefault(target.getUniqueId(), 0) < limit
                    && combat.visible(player.getEyeLocation(), target)
                    && strike(player, target, center.clone().add(0, .8, 0), damage, push, fire, slow, 0)) hits.merge(target.getUniqueId(), 1, Integer::sum);
        }
    }

    private void domainHit(Player player, Location center, double radius, double damage, double push,
                           int fire, int slow, Map<UUID, Integer> hits, int limit) {
        for (LivingEntity target : nearby(player, center, radius + 1)) {
            if (middle(target).distanceSquared(center.clone().add(0, .8, 0)) <= radius * radius
                    && hits.getOrDefault(target.getUniqueId(), 0) < limit
                    && combat.visible(player.getEyeLocation(), target)
                    && strike(player, target, center.clone().add(0, .8, 0), damage, push, fire, slow, 0)) hits.merge(target.getUniqueId(), 1, Integer::sum);
        }
    }

    private boolean valid(Player player, Location origin) {
        return !closed && player.isOnline() && !player.isDead() && player.getWorld().equals(origin.getWorld());
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
        PendingUltimate pending = pendingUltimates.remove(player.getUniqueId());
        if (pending != null && pending.fallback != null) pending.fallback.cancel();
        Set<BukkitRunnable> owned = tasks.remove(player.getUniqueId());
        if (owned != null) owned.forEach(BukkitRunnable::cancel);
        displays.cancel(player);
    }
    @Override public void close() {
        closed = true;
        pendingUltimates.values().forEach(pending -> { if (pending.fallback != null) pending.fallback.cancel(); });
        pendingUltimates.clear();
        tasks.values().forEach(owned -> owned.forEach(BukkitRunnable::cancel));
        tasks.clear(); displays.close();
    }
}
