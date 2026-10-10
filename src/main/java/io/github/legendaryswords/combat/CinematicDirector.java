package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.sword.SwordType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A brief orbiting camera around an animated, full-bright sword display. */
public final class CinematicDirector implements AutoCloseable {
    private final class Session {
        final Player player;
        final Location returnTo;
        final GameMode gameMode;
        final boolean allowFlight, flying;
        final Vector velocity;
        final float fallDistance;
        ArmorStand camera;
        ItemDisplay blade;
        BukkitTask task;
        Session(Player player) {
            this.player = player; returnTo = player.getLocation().clone(); gameMode = player.getGameMode();
            allowFlight = player.getAllowFlight(); flying = player.isFlying();
            velocity = player.getVelocity().clone(); fallDistance = player.getFallDistance();
        }
        void restore(boolean returnPosition) {
            if (task != null) task.cancel();
            try {
                // Restore only state still owned by this camera, preserving an admin's new mode.
                if (player.isOnline()) {
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        player.setSpectatorTarget(null);
                        player.setGameMode(gameMode);
                        if (player.getGameMode() == GameMode.SPECTATOR) {
                            plugin.getLogger().warning("Another plugin prevented camera mode restoration for " + player.getName());
                            return;
                        }
                        player.setAllowFlight(allowFlight);
                        if (allowFlight) player.setFlying(flying);
                        // Respect death/respawn and a legitimate teleport to a different world.
                        if (returnPosition && !player.isDead() && player.getWorld().equals(returnTo.getWorld())) {
                            player.teleport(returnTo);
                            player.setVelocity(velocity);
                            player.setFallDistance(fallDistance);
                        }
                    }
                    player.getPersistentDataContainer().remove(recoveryKey);
                }
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Camera restoration was interrupted for " + player.getName() + ": " + error.getMessage());
            } finally {
                if (camera != null) cleanup(camera::remove);
                if (blade != null) cleanup(blade::remove);
                cleanup(() -> plugin.animations().cancelCinematic(player));
            }
        }
    }
    private final LegendarySwordsPlugin plugin;
    private final PluginSettings settings;
    private final PlayerStateStore states;
    private final VisualEffects effects;
    private final NamespacedKey recoveryKey;
    private final Map<UUID, Session> sessions = new HashMap<>();
    public CinematicDirector(LegendarySwordsPlugin plugin, PluginSettings settings, PlayerStateStore states, VisualEffects effects) {
        this.plugin = plugin; this.settings = settings; this.states = states; this.effects = effects;
        recoveryKey = new NamespacedKey(plugin, "cinematic_recovery");
    }
    public boolean active(Player player) { return sessions.containsKey(player.getUniqueId()); }
    public boolean canRun(Player player) {
        return settings.cinematicsEnabled() && states.cinematics(player.getUniqueId())
                && player.getGameMode() != GameMode.SPECTATOR && !player.isInsideVehicle() && !player.isGliding();
    }

    public void start(Player player, SwordType sword, Runnable completed) {
        if (!canRun(player)) {
            completed.run(); return;
        }
        if (active(player)) return;
        Session session = new Session(player);
        sessions.put(player.getUniqueId(), session);
        // If the server saves during a camera and then crashes, the next join can undo spectator mode.
        player.getPersistentDataContainer().set(recoveryKey, PersistentDataType.STRING,
                String.join("|", "1", session.gameMode.name(), session.returnTo.getWorld().getUID().toString(),
                        Double.toString(session.returnTo.getX()), Double.toString(session.returnTo.getY()), Double.toString(session.returnTo.getZ()),
                        Float.toString(session.returnTo.getYaw()), Float.toString(session.returnTo.getPitch()),
                        Boolean.toString(session.allowFlight), Boolean.toString(session.flying)));
        Location focus = session.returnTo.clone().add(0, 1.5, 0);
        try {
            session.camera = focus.getWorld().spawn(focus, ArmorStand.class, camera -> {
                camera.setInvisible(true); camera.setMarker(true); camera.setGravity(false);
                camera.setInvulnerable(true); camera.setSilent(true); camera.setPersistent(false);
            });
            session.blade = focus.getWorld().spawn(focus, ItemDisplay.class, blade -> {
                blade.setItemStack(player.getInventory().getItemInMainHand().clone());
                blade.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                blade.setBrightness(new Display.Brightness(15, 15));
                blade.setGlowing(true); blade.setGlowColorOverride(sword.particleColor());
                blade.setInvulnerable(true); blade.setPersistent(false);
            });
            player.setGameMode(GameMode.SPECTATOR);
            if (player.getGameMode() != GameMode.SPECTATOR) {
                finish(session, completed); return;
            }
            player.setSpectatorTarget(session.camera);
            player.showTitle(Title.title(Component.text(sword.ultimate(), sword.textColor()),
                    Component.text("The blade awakens", NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(settings.cinematicTicks() * 50L), Duration.ofMillis(250))));
            int duration = settings.cinematicTicks();
            double radius = settings.cameraRadius();
            session.task = new BukkitRunnable() {
                int tick;
                @Override public void run() {
                    try {
                    if (!player.isOnline() || player.isDead() || player.getGameMode() != GameMode.SPECTATOR
                            || !player.getWorld().equals(focus.getWorld()) || !session.camera.isValid()
                            || player.getSpectatorTarget() != session.camera) {
                        CinematicDirector.this.cancel(player); return;
                    }
                    if (tick >= duration) { finish(session, completed); return; }
                    double progress = (double) tick / duration;
                    double angle = Math.toRadians(session.returnTo.getYaw()) + Math.PI + progress * Math.PI * 0.65;
                    Location position = focus.clone().add(Math.cos(angle) * radius, 0.9 + Math.sin(progress * Math.PI) * 0.7, Math.sin(angle) * radius);
                    Vector offset = position.toVector().subtract(focus.toVector());
                    var obstruction = focus.getWorld().rayTraceBlocks(focus, offset.clone().normalize(), offset.length(), FluidCollisionMode.NEVER, true);
                    if (obstruction != null) {
                        double distance = Math.max(0.3, focus.toVector().distance(obstruction.getHitPosition()) - 0.3);
                        position = focus.clone().add(offset.normalize().multiply(distance));
                    }
                    position.setDirection(focus.toVector().subtract(position.toVector()));
                    session.camera.teleport(position);
                    session.blade.setTransformation(new Transformation(new Vector3f(0, (float) Math.sin(progress * Math.PI) * 0.25f, 0),
                            new Quaternionf().rotationY((float) (progress * Math.PI * 2)), new Vector3f(1.6f), new Quaternionf()));
                    effects.ring(session.returnTo.clone().add(0, 0.15, 0), 1.1 + progress * 2, sword);
                    effects.pulse(focus, sword);
                    tick += 2;
                    } catch (RuntimeException error) {
                        plugin.getLogger().warning("Stopped an interrupted sword camera: " + error.getMessage());
                        CinematicDirector.this.cancel(player);
                    }
                }
            }.runTaskTimer(plugin, 0, 2);
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Camera unavailable; running the ultimate without it: " + error.getMessage());
            finish(session, completed);
        }
    }
    private void finish(Session session, Runnable completed) {
        if (sessions.remove(session.player.getUniqueId()) != session) return;
        session.restore(true);
        if (session.player.isOnline() && !session.player.isDead()) completed.run();
    }
    private void cleanup(Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException error) { plugin.getLogger().warning("Camera cleanup: " + error.getMessage()); }
    }
    public void cancel(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) session.restore(true);
    }
    public void cancelForTeleport(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) session.restore(false);
    }
    public void recover(Player player) {
        String encoded = player.getPersistentDataContainer().get(recoveryKey, PersistentDataType.STRING);
        if (encoded == null || active(player)) return;
        if (player.getGameMode() != GameMode.SPECTATOR) { player.getPersistentDataContainer().remove(recoveryKey); return; }
        try {
            String[] parts = encoded.split("\\|", -1);
            if (parts.length != 10 || !parts[0].equals("1")) throw new IllegalArgumentException("Unknown recovery data");
            GameMode original = GameMode.valueOf(parts[1]);
            if (original == GameMode.SPECTATOR) throw new IllegalArgumentException("Invalid original mode");
            var world = plugin.getServer().getWorld(UUID.fromString(parts[2]));
            double x = Double.parseDouble(parts[3]), y = Double.parseDouble(parts[4]), z = Double.parseDouble(parts[5]);
            float yaw = Float.parseFloat(parts[6]), pitch = Float.parseFloat(parts[7]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch)) throw new IllegalArgumentException("Invalid recovery location");
            player.setSpectatorTarget(null); player.setGameMode(original);
            if (player.getGameMode() == GameMode.SPECTATOR) return;
            boolean flight = Boolean.parseBoolean(parts[8]);
            player.setAllowFlight(flight);
            if (flight) player.setFlying(Boolean.parseBoolean(parts[9]));
            if (world != null && !player.isDead()) player.teleport(new Location(world, x, y, z, yaw, pitch));
            player.getPersistentDataContainer().remove(recoveryKey);
            plugin.getLogger().info("Restored an interrupted camera for " + player.getName());
        } catch (RuntimeException error) {
            plugin.getLogger().warning("Could not restore saved camera for " + player.getName() + ": " + error.getMessage());
        }
    }
    @Override public void close() {
        for (Session session : sessions.values().toArray(Session[]::new)) {
            sessions.remove(session.player.getUniqueId()); session.restore(true);
        }
    }
}
