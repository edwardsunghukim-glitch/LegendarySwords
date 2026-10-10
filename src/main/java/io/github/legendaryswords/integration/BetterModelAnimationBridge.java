package io.github.legendaryswords.integration;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlugin;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.manager.ReloadInfo;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.tracker.TrackerUpdateAction;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Compile-time verified against the official BetterModel 1.15.2 API, never shaded into our jar. */
final class BetterModelAnimationBridge implements AnimationBridge, Listener {
    private static final List<String> SWORDS = List.of("master_sword", "flameblade", "fierce_deity_sword");
    private static final String PREFIX = "legendaryswords_";
    private static final long VIEW_DISTANCE_SQUARED = 40L * 40L;

    private final JavaPlugin plugin;
    private final Plugin engine;
    private final Map<UUID, EntityTracker> bodies = new ConcurrentHashMap<>();
    private final Map<Tracker, Playback> owned = new ConcurrentHashMap<>();
    private final BukkitTask watchdog;
    private final BukkitTask startupCheck;
    private volatile String diagnostic = "BetterModel animation models are loading";
    private volatile boolean closed;
    private volatile boolean failed;

    BetterModelAnimationBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.engine = BetterModel.plugin();
        if (plugin.getConfig().getBoolean("bettermodel.install-bundled-models", true)) {
            installModels();
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        watchdog = plugin.getServer().getScheduler().runTaskTimer(plugin, this::expire, 20L, 20L);
        // BetterModel initially imports on ServerLoadEvent. A delayed task observes the finished import.
        startupCheck = plugin.getServer().getScheduler().runTaskLater(plugin, this::checkModels, 2L);
    }

    private void installModels() {
        Path engineFolder = engine.getDataFolder().toPath();
        try {
            for (String sword : SWORDS) {
                copyIfMissing("bettermodel/players/" + PREFIX + sword + ".bbmodel",
                        engineFolder.resolve("players").resolve(PREFIX + sword + ".bbmodel"));
                copyIfMissing("bettermodel/models/" + PREFIX + sword + "_slash.bbmodel",
                        engineFolder.resolve("models").resolve(PREFIX + sword + "_slash.bbmodel"));
            }
            copyIfMissing("bettermodel/LICENSE-BetterModel.txt",
                    engineFolder.resolve("players").resolve("LICENSE-LegendarySwords-template.txt"));
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to install bundled BetterModel files: " + exception.getMessage());
        }
    }

    private void copyIfMissing(String resource, Path destination) throws IOException {
        if (Files.exists(destination)) return;
        Files.createDirectories(destination.getParent());
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) throw new IOException("Missing jar resource " + resource);
            try {
                Files.copy(input, destination); // No REPLACE_EXISTING: preserve every user model.
            } catch (FileAlreadyExistsException ignored) {
                // Another startup/import installed it first.
            }
        }
    }

    private void checkModels() {
        if (closed || !engine.isEnabled()) return;
        try {
            if (!BetterModel.config().module().playerAnimation()) {
                diagnostic = "Enable module.player-animation in BetterModel/config.yml, then restart";
                plugin.getLogger().warning(diagnostic);
                return;
            }
            if (modelsReady()) {
                diagnostic = "BetterModel ready: custom third-person swings, abilities, and ultimates";
                plugin.getLogger().info(diagnostic);
                return;
            }
            // This also supports enabling LegendarySwords after BetterModel has already imported assets.
            // The engine's own reload command uses its async scheduler, so use the same documented API.
            diagnostic = "Importing LegendarySwords animation models into BetterModel";
            BetterModel.plugin().scheduler().asyncTask(() -> {
                if (closed || !engine.isEnabled()) return;
                try {
                    BetterModelPlugin.ReloadResult result = BetterModel.plugin().reload(
                            new ReloadInfo(true, plugin.getServer().getConsoleSender()));
                    if (closed) return;
                    if (result instanceof BetterModelPlugin.ReloadResult.Failure failure) {
                        diagnostic = "BetterModel import failed: " + failure.throwable().getClass().getSimpleName();
                    } else if (result instanceof BetterModelPlugin.ReloadResult.OnReload) {
                        diagnostic = "BetterModel is reloading; check /legendaryswords status after it finishes";
                    } else {
                        diagnostic = modelsReady() ? "BetterModel ready: custom third-person animations"
                                : "BetterModel models are missing; verify BetterModel/players files and module.player-animation";
                    }
                    plugin.getLogger().info(diagnostic);
                } catch (LinkageError | RuntimeException error) {
                    disableAnimation(error);
                }
            });
        } catch (LinkageError | RuntimeException error) {
            disableAnimation(error);
        }
    }

    private boolean modelsReady() {
        return SWORDS.stream().allMatch(id -> BetterModel.limb(PREFIX + id)
                .filter(renderer -> renderer.animation("swing").isPresent()
                        && renderer.animation("ability").isPresent()
                        && renderer.animation("ultimate").isPresent()).isPresent());
    }

    @Override public void playSwing(Player player, String swordId) { play(player, swordId, "swing"); }
    @Override public void playAbility(Player player, String swordId) { play(player, swordId, "ability"); }
    @Override public void playUltimate(Player player, String swordId) { play(player, swordId, "ultimate"); }

    @Override public boolean playCinematic(Player player, String swordId, Location origin) {
        if (closed || failed || !engine.isEnabled() || !player.isOnline() || !SWORDS.contains(swordId)) return false;
        try {
            ModelRenderer renderer = BetterModel.limbOrNull(PREFIX + swordId);
            if (renderer == null || renderer.animation("ultimate").isEmpty()) return false;
            closePlayer(player.getUniqueId());
            // A detached skin model stays at the cast origin while the spectator camera orbits it.
            var sword = player.getInventory().getItemInMainHand().clone();
            DummyTracker tracker = renderer.create(origin.clone(), ModelProfile.of(player), TrackerModifier.DEFAULT,
                    created -> created.update(TrackerUpdateAction.itemStack(TransformedItemStack.of(sword)),
                            bone -> bone.name().name().equals("right_item")));
            track(player.getUniqueId(), tracker, 4D);
            spawnNearby(tracker, player, origin);
            if (!tracker.animate("ultimate", AnimationModifier.DEFAULT_WITH_PLAY_ONCE, tracker::close)) {
                tracker.close();
                return false;
            }
            playSlashAt(player, swordId, "ultimate", origin);
            return true;
        } catch (LinkageError | RuntimeException error) {
            disableAnimation(error);
            return false;
        }
    }

    @Override public void cancelCinematic(Player player) {
        closePlayer(player.getUniqueId());
    }

    private void play(Player player, String swordId, String animation) {
        if (closed || failed || !engine.isEnabled() || !player.isOnline() || !SWORDS.contains(swordId)) return;
        try {
            ModelRenderer renderer = BetterModel.limbOrNull(PREFIX + swordId);
            if (renderer == null || renderer.animation(animation).isEmpty()) return;
            EntityTracker previous = bodies.remove(player.getUniqueId());
            if (previous != null) previous.close();
            EntityTracker tracker = renderer.create(player, TrackerModifier.DEFAULT);
            bodies.put(player.getUniqueId(), tracker);
            double length = renderer.animation(animation).orElseThrow().length();
            track(player.getUniqueId(), tracker, Math.min(8D, length) + 1D);
            if (!tracker.animate(animation, AnimationModifier.DEFAULT_WITH_PLAY_ONCE, tracker::close)) {
                tracker.close();
                return;
            }
            playSlash(player, swordId, animation);
        } catch (LinkageError | RuntimeException error) {
            disableAnimation(error);
        }
    }

    private void playSlash(Player player, String swordId, String animation) {
        playSlashAt(player, swordId, animation, player.getLocation());
    }

    private void playSlashAt(Player player, String swordId, String animation, Location origin) {
        ModelRenderer renderer = BetterModel.modelOrNull(PREFIX + swordId + "_slash");
        if (renderer == null) return;
        Location location = origin.clone().add(0D, 1.1D, 0D)
                .add(origin.getDirection().setY(0D).multiply(0.9D));
        location.setPitch(0F);
        DummyTracker tracker = renderer.create(location, TrackerModifier.DEFAULT,
                created -> created.update(TrackerUpdateAction.brightness(15, 15)));
        track(player.getUniqueId(), tracker, 3D);
        spawnNearby(tracker, player, location);
        if (!tracker.animate(animation, AnimationModifier.DEFAULT_WITH_PLAY_ONCE, tracker::close)) tracker.close();
    }

    private void spawnNearby(DummyTracker tracker, Player player, Location location) {
        for (Player viewer : location.getWorld().getPlayers()) {
            if (viewer.canSee(player) && viewer.getLocation().distanceSquared(location) <= VIEW_DISTANCE_SQUARED) {
                tracker.spawn(viewer);
            }
        }
    }

    private void track(UUID owner, Tracker tracker, double maxSeconds) {
        long deadline = System.nanoTime() + (long) (maxSeconds * TimeUnit.SECONDS.toNanos(1L));
        owned.put(tracker, new Playback(owner, deadline));
        tracker.handleCloseEvent((closedTracker, reason) -> {
            owned.remove(closedTracker);
            if (closedTracker instanceof EntityTracker entityTracker) bodies.remove(owner, entityTracker);
        });
    }

    private void expire() {
        long now = System.nanoTime();
        owned.forEach((tracker, playback) -> {
            if (tracker.isClosed() || now - playback.deadline() >= 0L) {
                owned.remove(tracker);
                tracker.close();
            }
        });
    }

    private void closePlayer(UUID owner) {
        owned.forEach((tracker, playback) -> {
            if (playback.owner().equals(owner)) {
                owned.remove(tracker);
                tracker.close();
            }
        });
        bodies.remove(owner);
    }

    private void disableAnimation(Throwable error) {
        failed = true;
        diagnostic = "BetterModel animation unavailable (" + error.getClass().getSimpleName()
                + "); use the compatible BetterModel 1.15.2 release";
        plugin.getLogger().warning(diagnostic);
        owned.keySet().forEach(Tracker::close);
        owned.clear();
        bodies.clear();
    }

    @Override public String status() {
        if (!closed && !failed && engine.isEnabled()) {
            try {
                if (modelsReady()) return "BetterModel ready: third-person animation models loaded";
            } catch (LinkageError | RuntimeException error) {
                disableAnimation(error);
            }
        }
        return diagnostic;
    }

    @EventHandler public void quit(PlayerQuitEvent event) { closePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler public void died(PlayerDeathEvent event) { closePlayer(event.getEntity().getUniqueId()); }
    @EventHandler public void worldChanged(PlayerChangedWorldEvent event) { closePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler public void pluginDisabled(PluginDisableEvent event) {
        if (event.getPlugin() == engine) close();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        startupCheck.cancel();
        watchdog.cancel();
        HandlerList.unregisterAll(this);
        owned.keySet().forEach(Tracker::close);
        owned.clear();
        bodies.clear();
        diagnostic = "BetterModel animations stopped";
    }

    private record Playback(UUID owner, long deadline) { }
}
