package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.sword.SwordType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A short weapon ritual that leaves the player in control at their current position. */
public final class CinematicDirector implements AutoCloseable {
    private static final long FRAME_TICKS = 2;

    private static final class Session {
        final Player player;
        final Runnable completed;
        Location origin;
        ItemDisplay blade;
        BukkitTask task;
        boolean finished;

        Session(Player player, Runnable completed) {
            this.player = player;
            this.completed = completed;
        }
    }

    private final LegendarySwordsPlugin plugin;
    private final PluginSettings settings;
    private final PlayerStateStore states;
    private final VisualEffects effects;
    private final NamespacedKey recoveryKey;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public CinematicDirector(LegendarySwordsPlugin plugin, PluginSettings settings, PlayerStateStore states, VisualEffects effects) {
        this.plugin = plugin;
        this.settings = settings;
        this.states = states;
        this.effects = effects;
        // Read old releases' markers on join; the new ritual never writes one.
        recoveryKey = new NamespacedKey(plugin, "cinematic_recovery");
    }

    public boolean active(Player player) { return sessions.containsKey(player.getUniqueId()); }

    public boolean canRun(Player player) {
        return settings.cinematicsEnabled() && states.cinematics(player.getUniqueId())
                && player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                && !player.isInsideVehicle() && !player.isGliding();
    }

    public void start(Player player, SwordType sword, Runnable completed) {
        Objects.requireNonNull(completed, "completed");
        if (!canRun(player) || active(player)) {
            complete(completed);
            return;
        }
        Session session = new Session(player, completed);
        sessions.put(player.getUniqueId(), session);
        try {
            session.origin = player.getLocation().clone();
            Location focus = session.origin.clone().add(0, 1.65, 0);
            session.blade = focus.getWorld().spawn(focus, ItemDisplay.class, blade -> {
                // Retain the entity even if another initializer operation fails.
                session.blade = blade;
                var visualSword = player.getInventory().getItemInMainHand().clone();
                if (plugin.animatedSwordItems() != null) plugin.animatedSwordItems().restoreItem(visualSword);
                blade.setItemStack(visualSword);
                blade.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                blade.setBrightness(new Display.Brightness(15, 15));
                blade.setGlowing(true);
                blade.setGlowColorOverride(sword.particleColor());
                blade.setInvulnerable(true);
                blade.setPersistent(false);
                blade.setGravity(false);
                blade.setInterpolationDuration((int) FRAME_TICKS);
            });
            player.showTitle(Title.title(Component.text(sword.ultimate(), sword.textColor()),
                    Component.text("The blade awakens", NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(settings.cinematicTicks() * 50L), Duration.ofMillis(250))));
            int duration = settings.cinematicTicks();
            session.task = new BukkitRunnable() {
                int tick;

                @Override public void run() {
                    if (session.finished) return;
                    try {
                        if (!player.isOnline() || player.isDead()
                                || !player.getWorld().equals(focus.getWorld()) || !session.blade.isValid()
                                || player.getLocation().distanceSquared(session.origin) > 64) {
                            finish(session);
                            return;
                        }
                        if (tick >= duration) {
                            finish(session);
                            return;
                        }
                        double progress = (double) tick / duration;
                        float lift = (float) Math.sin(progress * Math.PI) * 0.65f;
                        session.blade.setInterpolationDelay(0);
                        session.blade.setTransformation(new Transformation(new Vector3f(0, lift, 0),
                                new Quaternionf().rotationY((float) (progress * Math.PI * 2)),
                                new Vector3f(1.4f + (float) Math.sin(progress * Math.PI) * 0.35f), new Quaternionf()));
                        effects.ring(session.origin.clone().add(0, 0.15, 0), 1.1 + progress * 1.5, sword);
                        effects.pulse(focus.clone().add(0, lift, 0), sword);
                        tick += FRAME_TICKS;
                    } catch (RuntimeException error) {
                        plugin.getLogger().warning("Stopped an interrupted sword ritual: " + error.getMessage());
                        finish(session);
                    }
                }
            }.runTaskTimer(plugin, 0, FRAME_TICKS);
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Sword ritual unavailable; continuing the ultimate: " + error.getMessage());
            finish(session);
        }
    }

    private void finish(Session session) {
        if (session.finished) return;
        session.finished = true;
        sessions.remove(session.player.getUniqueId(), session);
        if (session.task != null) cleanup(session.task::cancel);
        if (session.blade != null) cleanup(session.blade::remove);
        cleanup(() -> plugin.animations().cancelCinematic(session.player));
        // Combat must never depend on a successful visual scene or on spectator restoration.
        complete(session.completed);
    }

    private void complete(Runnable completed) {
        try { completed.run(); }
        catch (RuntimeException error) { plugin.getLogger().warning("Sword ritual completion: " + error.getMessage()); }
    }

    private void cleanup(Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException error) { plugin.getLogger().warning("Sword ritual cleanup: " + error.getMessage()); }
    }

    public void cancel(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session != null) finish(session);
    }

    public void cancelForTeleport(Player player) { cancel(player); }

    /** Repairs a spectator left behind by the old camera implementation after a crash. */
    public void recover(Player player) {
        String encoded = player.getPersistentDataContainer().get(recoveryKey, PersistentDataType.STRING);
        if (encoded == null || active(player)) return;
        if (player.getGameMode() != GameMode.SPECTATOR) {
            player.getPersistentDataContainer().remove(recoveryKey);
            return;
        }
        try {
            String[] parts = encoded.split("\\|", -1);
            if (parts.length != 10 || !parts[0].equals("1")) throw new IllegalArgumentException("Unknown recovery data");
            GameMode original = GameMode.valueOf(parts[1]);
            if (original == GameMode.SPECTATOR) throw new IllegalArgumentException("Invalid original mode");
            var world = plugin.getServer().getWorld(UUID.fromString(parts[2]));
            double x = Double.parseDouble(parts[3]), y = Double.parseDouble(parts[4]), z = Double.parseDouble(parts[5]);
            float yaw = Float.parseFloat(parts[6]), pitch = Float.parseFloat(parts[7]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) throw new IllegalArgumentException("Invalid recovery location");
            player.setSpectatorTarget(null);
            player.setGameMode(original);
            if (player.getGameMode() == GameMode.SPECTATOR) return;
            boolean flight = Boolean.parseBoolean(parts[8]);
            player.setAllowFlight(flight);
            if (flight) player.setFlying(Boolean.parseBoolean(parts[9]));
            if (world != null && !player.isDead()) player.teleport(new Location(world, x, y, z, yaw, pitch));
            player.getPersistentDataContainer().remove(recoveryKey);
            plugin.getLogger().info("Restored an interrupted legacy camera for " + player.getName());
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Could not restore saved legacy camera for " + player.getName() + ": " + error.getMessage());
        }
    }

    @Override public void close() {
        for (Session session : sessions.values().toArray(Session[]::new)) finish(session);
    }
}
