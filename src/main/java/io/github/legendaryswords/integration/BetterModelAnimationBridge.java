package io.github.legendaryswords.integration;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PluginSettings;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneItemMapper;
import kr.toxicity.model.api.bone.BoneRenderContext;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.manager.ReloadInfo;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.platform.PlatformItemTransform;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityHideOption;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import kr.toxicity.model.api.tracker.TrackerUpdateAction;
import kr.toxicity.model.api.util.TransformedItemStack;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.LongSupplier;
import java.util.logging.Level;

/** Direct integration with official BetterModel 3.1; engine classes stay out of the fallback. */
final class BetterModelAnimationBridge implements AnimationBridge, Listener {
    private static final List<String> SWORDS = List.of("master_sword", "flameblade", "fierce_deity_sword",
            "frostblade", "thunderblade", "royal_claymore", "eightfold_blade", "biggoron_sword");
    private static final List<String> LOOPS = List.of("idle", "walk", "run");
    private static final List<String> ACTIONS = List.of("pose", "swing", "ability", "ultimate");
    private static final String PREFIX = "legendaryswords_";
    private static final long VIEW_DISTANCE_SQUARED = 1600L;
    private static final long SWING_THROTTLE_NANOS = TimeUnit.MILLISECONDS.toNanos(220L);
    private static final TrackerModifier BODY_MODIFIER = new TrackerModifier(false, false, true);
    // Equipment hiding also clears the owner's selected inventory slot in BetterModel 3.1.
    private static final UUID NO_VIEWER = new UUID(0L, 0L);
    private static final EntityHideOption INVENTORY_SAFE_HIDE = new EntityHideOption(false, true, true, true);
    private static final AnimationModifier LOOP = AnimationModifier.builder().start(3).end(2).priority(10)
            .type(AnimationIterator.Type.LOOP).build();
    private static final AnimationModifier ACTION = AnimationModifier.builder().start(2).end(3).priority(30)
            .type(AnimationIterator.Type.PLAY_ONCE).override(true).build();
    private static final AnimationModifier ULTIMATE_CAST = AnimationModifier.builder().start(2).end(3).priority(30)
            .type(AnimationIterator.Type.HOLD_ON_LAST).speed(3F).override(true).build();
    private final JavaPlugin plugin;
    private final Plugin engine;
    private final AnimatedSwordItems swordItems;
    private final LongSupplier clock;
    private final Map<UUID, BodySession> bodies = new ConcurrentHashMap<>();
    private final Map<Tracker, Playback> effects = new ConcurrentHashMap<>();
    private final Map<UUID, DummyTracker> cinematicBodies = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSlashSwings = new ConcurrentHashMap<>();
    private final AtomicBoolean reloading = new AtomicBoolean();
    private final BukkitTask watchdog;
    private final BukkitTask startupCheck;
    private volatile String diagnostic = "BetterModel animation models are loading";
    private volatile boolean closed, failed, needsImport;
    private int importAttempts;
    private final List<String> preservedModels = new ArrayList<>();

    BetterModelAnimationBridge(JavaPlugin plugin) {
        this(plugin, ((LegendarySwordsPlugin) plugin).animatedSwordItems());
    }

    BetterModelAnimationBridge(JavaPlugin plugin, AnimatedSwordItems swordItems) {
        this(plugin, swordItems, System::nanoTime);
    }

    BetterModelAnimationBridge(JavaPlugin plugin, AnimatedSwordItems swordItems, LongSupplier clock) {
        this.plugin = plugin;
        this.swordItems = java.util.Objects.requireNonNull(swordItems);
        this.clock = java.util.Objects.requireNonNull(clock);
        this.engine = java.util.Objects.requireNonNull(plugin.getServer().getPluginManager().getPlugin("BetterModel"));
        if (plugin.getConfig().getBoolean("bettermodel.install-bundled-models", true)) installModels();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        watchdog = plugin.getServer().getScheduler().runTaskTimer(plugin, this::expire, 20L, 20L);
        startupCheck = plugin.getServer().getScheduler().runTaskLater(plugin, this::checkModels, 2L);
    }

    private void installModels() {
        int changed = 0;
        try {
            BundledModelInstaller installer = new BundledModelInstaller(plugin, engine.getDataFolder().toPath());
            for (String sword : SWORDS) {
                for (String relative : List.of("players/" + PREFIX + sword + ".bbmodel",
                        "models/" + PREFIX + sword + "_slash.bbmodel")) {
                    var result = installer.install(relative);
                    if (result == BundledModelInstaller.Result.NEW || result == BundledModelInstaller.Result.UPDATED) changed++;
                    if (result == BundledModelInstaller.Result.PRESERVED) preservedModels.add(relative);
                }
            }
            installer.install("LICENSE-BetterModel.txt");
            installer.save();
            needsImport = changed > 0;
            if (changed > 0) plugin.getLogger().info("Installed/updated " + changed
                    + " BetterModel files; previous originals backed up in LegendarySwords/bettermodel-backups.");
            if (!preservedModels.isEmpty()) plugin.getLogger().warning("Preserved edited BetterModel files: "
                    + String.join(", ", preservedModels) + "; update their clips manually if /ls status lists missing animations.");
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to install BetterModel files: " + exception.getMessage());
        }
    }

    private void checkModels() {
        if (closed || failed || !engine.isEnabled() || reloading.get()) return;
        try {
            if ((!abilitiesOnly() || playerBodyAnimationsEnabled()) && !BetterModel.config().module().playerAnimation()) {
                diagnostic = "Enable module.player-animation in BetterModel/config.yml, then restart";
                return;
            }
            if (!BetterModel.config().module().model()) {
                diagnostic = "Enable module.model in BetterModel/config.yml for animated slash models";
                return;
            }
            if (!needsImport && missingModels().isEmpty()) {
                diagnostic = readyStatus();
                return;
            }
            if (importAttempts >= 3) {
                diagnostic = "BetterModel missing assets: " + String.join(", ", missingModels())
                        + "; check console import errors, then /bm reload";
                return;
            }
            if (!reloading.compareAndSet(false, true)) return;
            importAttempts++;
            diagnostic = "Importing upgraded LegendarySwords models; regenerate/install BetterModel's build.zip";
            BetterModel.platform().scheduler().asyncTask(() -> {
                try {
                    if (closed || !engine.isEnabled()) return;
                    BetterModelPlatform.ReloadResult result = BetterModel.platform().reload(
                            new ReloadInfo(true, plugin.getServer().getConsoleSender()));
                    if (closed) return;
                    if (result instanceof BetterModelPlatform.ReloadResult.Failure failure) {
                        diagnostic = "BetterModel import failed: " + failure.throwable().getClass().getSimpleName()
                                + "; see BetterModel console errors";
                    } else if (result instanceof BetterModelPlatform.ReloadResult.OnReload) {
                        diagnostic = "BetterModel is reloading; check /ls status when it finishes";
                    } else {
                        needsImport = false;
                        diagnostic = missingModels().isEmpty() ? readyStatus()
                                : "BetterModel missing assets: " + String.join(", ", missingModels());
                    }
                    plugin.getLogger().info(diagnostic);
                } catch (LinkageError | RuntimeException error) {
                    disableAnimation(error);
                } finally {
                    reloading.set(false);
                }
            });
        } catch (LinkageError | RuntimeException error) {
            disableAnimation(error);
        }
    }

    private List<String> missingModels() {
        List<String> missing = new ArrayList<>();
        for (String sword : SWORDS) {
            if (!abilitiesOnly() || playerBodyAnimationsEnabled()) {
                ModelRenderer body = BetterModel.limbOrNull(PREFIX + sword);
                if (body == null) missing.add(sword + ":player");
                else for (String clip : abilitiesOnly() ? List.of("ability", "ultimate")
                        : List.of("idle", "walk", "run", "pose", "swing", "ability", "ultimate")) {
                    if (body.animation(clip).isEmpty()) missing.add(sword + ":" + clip);
                }
            }
            ModelRenderer effect = BetterModel.modelOrNull(PREFIX + sword + "_slash");
            if (effect == null) missing.add(sword + ":slash");
            else for (String clip : abilitiesOnly() ? List.of("ability", "ultimate") : List.of("swing", "ability", "ultimate")) {
                if (effect.animation(clip).isEmpty()) missing.add(sword + ":slash:" + clip);
            }
        }
        return missing;
    }

    private String readyStatus() {
        return "BetterModel " + engine.getPluginMeta().getVersion()
                + (abilitiesOnly()
                ? ": abilities/ultimates only; " + (playerBodyAnimationsEnabled()
                        ? "temporary cast body animations" : "attack effects only")
                        + "; normal movement and swings; existing 0.2.2 combined pack works"
                : playerBodyAnimationsEnabled()
                ? ": 8 motion sets loaded (idle/walk/run/pose/swing/ability/ultimate); install regenerated client pack"
                : ": player body animations paused (bettermodel.player-body-animations=false); slash effects available; existing 0.2.2 combined pack works")
                + (preservedModels.isEmpty() ? "" : "; " + preservedModels.size() + " edited files preserved");
    }

    @Override public void tickMovement(Player player, String swordId, Locomotion movement) {
        if (!usable(player, swordId)) return;
        try {
            if (abilitiesOnly()) {
                BodySession cast = bodies.get(player.getUniqueId());
                if (cast != null) {
                    if (!playerBodyAnimationsEnabled() || clock.getAsLong() - cast.actionDeadline >= 0L)
                        closeBody(player.getUniqueId());
                    else { markCastViewers(cast.tracker, player); prime(cast); }
                }
                return;
            }
            if (!playerBodyAnimationsEnabled()) {
                closeBody(player.getUniqueId());
                return;
            }
            BodySession body = body(player, swordId);
            if (body == null) return;
            body.desiredLoop = movement.name().toLowerCase(Locale.ROOT);
            // In 3.1, built-in clips are installed when the first viewer spawns, after create().
            // Reclaim their names once that scheduler starts, rather than resetting every tick.
            prime(body);
            if (body.action != null && clock.getAsLong() - body.actionDeadline >= 0L) finishAction(body);
            if (body.action == null) setLoop(body);
        } catch (LinkageError | RuntimeException error) {
            disableAnimation(error);
        }
    }

    @Override public void stopMovement(Player player) { closePlayer(player.getUniqueId()); }
    @Override public void stopCastAnimation(Player player) { closeBody(player.getUniqueId()); }
    @Override public void holdCastAnimation(Player player) {
        BodySession body = bodies.get(player.getUniqueId());
        if (body != null && abilitiesOnly() && "ultimate".equals(body.action))
            body.actionDeadline = clock.getAsLong() + TimeUnit.SECONDS.toNanos(12);
    }
    @Override public int ultimateSceneTicks(String swordId) {
        try {
            ModelRenderer renderer = BetterModel.limbOrNull(PREFIX + swordId);
            return renderer == null ? 64 : renderer.animation("ultimate")
                    .map(clip -> (int) Math.ceil(Math.min(8D, clip.length()) * 20D)).orElse(64);
        } catch (LinkageError | RuntimeException error) { return 64; }
    }
    @Override public void playSwing(Player player, String swordId) { play(player, swordId, "swing", 2); }
    @Override public void playAbility(Player player, String swordId) { play(player, swordId, "ability", 3); }
    @Override public void playUltimate(Player player, String swordId) { play(player, swordId, "ultimate", 4); }
    @Override public void playPose(Player player, String swordId) { play(player, swordId, "pose", 1); }

    private boolean usable(Player player, String swordId) {
        return !closed && !failed && !reloading.get() && !needsImport && engine.isEnabled()
                && player.isOnline() && !player.isDead() && SWORDS.contains(swordId);
    }

    private boolean playerBodyAnimationsEnabled() {
        return abilitiesOnly() ? plugin.getConfig().getBoolean("bettermodel.cast-body-animations", true)
                : plugin.getConfig().getBoolean("bettermodel.player-body-animations", false);
    }

    private boolean abilitiesOnly() {
        return plugin.getConfig().getBoolean("bettermodel.abilities-only", true);
    }

    private BodySession body(Player player, String swordId) {
        // Player-attached trackers use BetterModel's native equipment/hide packet path.
        // Keep that path opt-in, and only inspect inventory on the server thread.
        if (!playerBodyAnimationsEnabled() || !plugin.getServer().isPrimaryThread()) return null;
        UUID id = player.getUniqueId();
        BodySession previous = bodies.get(id);
        if (previous != null && !previous.tracker.isClosed() && previous.swordId.equals(swordId)) {
            if (abilitiesOnly() || swordItems.begin(player, swordId)) {
                previous.cachedItem = swordItems.displayItem(player, swordId).clone();
                return previous;
            }
            closePlayer(id);
            return null;
        }
        if (previous != null) closePlayer(id);
        ModelRenderer renderer = BetterModel.limbOrNull(PREFIX + swordId);
        if (renderer == null || !abilitiesOnly() && LOOPS.stream().anyMatch(clip -> renderer.animation(clip).isEmpty())) return null;
        if (!abilitiesOnly() && !swordItems.begin(player, swordId)) return null;
        EntityTracker tracker;
        BodySession body;
        try {
            body = new BodySession(id, swordId, renderer, swordItems.displayItem(player, swordId).clone());
            tracker = renderer.create(BukkitAdapter.adapt(player), BODY_MODIFIER, created -> {
                created.hideOption(abilitiesOnly() ? EntityHideOption.DEFAULT : INVENTORY_SAFE_HIDE);
                if (abilitiesOnly()) markCastViewers(created, player);
                configureSwordDisplay(created, body::displayItem);
            });
        } catch (LinkageError | RuntimeException error) {
            swordItems.restorePlayerSoon(id);
            throw error;
        }
        // Stop the engine's automatic movement predicates; our held-sword state owns these clips.
        for (String clip : List.of("idle", "walk", "idle_fly", "walk_fly", "spawn")) tracker.stopAnimation(clip);
        body.tracker = tracker;
        bodies.put(id, body);
        tracker.handleCloseEvent((closedTracker, reason) -> {
            if (bodies.remove(id, body)) swordItems.restorePlayerSoon(id);
        });
        if (!abilitiesOnly()) setLoop(body);
        return body;
    }

    private static void markCastViewers(EntityTracker tracker, Player owner) {
        // An empty whitelist means everybody in BetterModel, so keep a sentinel entry.
        // Never send the cast body or empty-equipment packets to its owner.
        java.util.Set<UUID> viewers = new java.util.HashSet<>();
        viewers.add(NO_VIEWER);
        for (Player viewer : owner.getLocation().getWorld().getPlayers())
            if (!viewer.getUniqueId().equals(owner.getUniqueId()) && viewer.canSee(owner))
                viewers.add(viewer.getUniqueId());
        tracker.markPlayerForSpawn(viewers);
    }

    private void prime(BodySession body) {
        if (!body.tracker.isScheduled()) body.primed = false;
        else if (!body.primed) {
            for (String clip : List.of("idle", "walk", "idle_fly", "walk_fly", "spawn"))
                body.tracker.stopAnimation(clip);
            body.primed = true;
            body.runningLoop = null;
        }
    }

    private static void configureSwordDisplay(Tracker tracker, Supplier<ItemStack> sword) {
        var hand = tracker.bone("right_item");
        if (hand == null || hand.getDisplay() == null)
            throw new IllegalStateException("BetterModel sword rig has no right_item display");
        hand.setItemMapper(new BoneItemMapper() {
            @Override public PlatformItemTransform transform() { return PlatformItemTransform.FIXED; }
            @Override public TransformedItemStack apply(BoneRenderContext context, TransformedItemStack current) {
                return TransformedItemStack.of(BukkitAdapter.adapt(sword.get()));
            }
        });
        hand.getDisplay().display(PlatformItemTransform.FIXED);
        tracker.update(TrackerUpdateAction.itemMapping(), bone -> bone == hand);
    }

    private void setLoop(BodySession body) {
        if (body.desiredLoop.equals(body.runningLoop)) return;
        for (String clip : LOOPS) body.tracker.stopAnimation(clip);
        if (body.tracker.animate(body.desiredLoop, LOOP)) body.runningLoop = body.desiredLoop;
    }

    private void play(Player player, String swordId, String animation, int priority) {
        if (abilitiesOnly() && !animation.equals("ability") && !animation.equals("ultimate")) return;
        if (!usable(player, swordId)) return;
        try {
            if (!playerBodyAnimationsEnabled()) {
                UUID owner = player.getUniqueId();
                closeBody(owner);
                if (animation.equals("swing")) {
                    long now = clock.getAsLong();
                    Long previous = lastSlashSwings.get(owner);
                    if (previous != null && now - previous < SWING_THROTTLE_NANOS) return;
                    lastSlashSwings.put(owner, now);
                }
                if (!animation.equals("pose")) playSlashAt(player, swordId, animation, player.getLocation());
                return;
            }
            BodySession body = body(player, swordId);
            if (body == null || body.renderer.animation(animation).isEmpty()) return;
            long now = clock.getAsLong();
            if (body.action != null && now - body.actionDeadline >= 0L) {
                if (abilitiesOnly()) {
                    closeBody(player.getUniqueId());
                    body = body(player, swordId);
                    if (body == null) return;
                } else finishAction(body);
            }
            if (body.action != null && (priority < body.actionPriority
                    || animation.equals("swing") && now - body.lastActionStart < SWING_THROTTLE_NANOS)) return;
            for (String clip : ACTIONS) body.tracker.stopAnimation(clip);
            body.action = animation;
            body.actionPriority = priority;
            body.lastActionStart = now;
            double length = body.renderer.animation(animation).orElseThrow().length();
            body.actionDeadline = now + (long) ((Math.min(8D, length) + 0.3D) * TimeUnit.SECONDS.toNanos(1L));
            AnimationModifier modifier = abilitiesOnly() && animation.equals("ultimate") ? ULTIMATE_CAST.toBuilder().speed((float) (length * 20D
                    / PluginSettings.ultimateWindupTicks(plugin.getConfig(), swordId))).build() : ACTION;
            if (!body.tracker.animate(animation, modifier)) { finishAction(body); return; }
            if (!animation.equals("pose")) playSlashAt(player, swordId, animation, player.getLocation());
        } catch (LinkageError | RuntimeException error) {
            disableAnimation(error);
        }
    }

    private void finishAction(BodySession body) {
        if (abilitiesOnly()) { closeBody(body.owner); return; }
        for (String clip : ACTIONS) body.tracker.stopAnimation(clip);
        body.action = null;
        body.actionPriority = 0;
        body.runningLoop = null;
        setLoop(body);
    }

    @Override public boolean playCinematic(Player player, String swordId, Location origin) {
        if (abilitiesOnly() || !plugin.getServer().isPrimaryThread() || !usable(player, swordId)) return false;
        try {
            ModelRenderer renderer = BetterModel.limbOrNull(PREFIX + swordId);
            if (renderer == null || renderer.animation("ultimate").isEmpty()) return false;
            cancelCinematic(player);
            var sword = swordItems.displayItem(player, swordId);
            DummyTracker tracker = renderer.create(BukkitAdapter.adapt(origin.clone()),
                    ModelProfile.of(BukkitAdapter.adapt(player)), BODY_MODIFIER,
                    created -> configureSwordDisplay(created, sword::clone));
            cinematicBodies.put(player.getUniqueId(), tracker);
            trackEffect(player.getUniqueId(), tracker, 5D);
            spawnNearby(tracker, player, origin);
            if (!tracker.animate("ultimate", ACTION, tracker::close)) { tracker.close(); return false; }
            return true;
        } catch (LinkageError | RuntimeException error) {
            disableAnimation(error);
            return false;
        }
    }

    @Override public void cancelCinematic(Player player) {
        DummyTracker tracker = cinematicBodies.remove(player.getUniqueId());
        if (tracker != null) tracker.close();
    }

    private void playSlashAt(Player player, String swordId, String animation, Location origin) {
        ModelRenderer renderer = BetterModel.modelOrNull(PREFIX + swordId + "_slash");
        if (renderer == null || renderer.animation(animation).isEmpty()) return;
        Location location = origin.clone().add(0D, 1.1D, 0D)
                .add(origin.getDirection().setY(0D).multiply(0.9D));
        location.setPitch(0F);
        DummyTracker tracker = renderer.create(BukkitAdapter.adapt(location), BODY_MODIFIER,
                created -> created.update(TrackerUpdateAction.brightness(15, 15)));
        double length = renderer.animation(animation).orElseThrow().length();
        trackEffect(player.getUniqueId(), tracker, Math.min(8D, length) + 1D);
        spawnNearby(tracker, player, location);
        if (!tracker.animate(animation, ACTION, tracker::close)) tracker.close();
    }

    private void spawnNearby(DummyTracker tracker, Player player, Location location) {
        for (Player viewer : location.getWorld().getPlayers()) {
            if (viewer.canSee(player) && viewer.getLocation().distanceSquared(location) <= VIEW_DISTANCE_SQUARED)
                tracker.spawn(BukkitAdapter.adapt(viewer));
        }
    }

    private void trackEffect(UUID owner, Tracker tracker, double maxSeconds) {
        effects.put(tracker, new Playback(owner, clock.getAsLong()
                + (long) (maxSeconds * TimeUnit.SECONDS.toNanos(1L))));
        tracker.handleCloseEvent((closedTracker, reason) -> {
            effects.remove(closedTracker);
            if (closedTracker instanceof DummyTracker dummy) cinematicBodies.remove(owner, dummy);
        });
    }

    private void expire() {
        if (closed || failed) return;
        long now = clock.getAsLong();
        effects.forEach((tracker, playback) -> {
            if (tracker.isClosed() || now - playback.deadline() >= 0L) {
                effects.remove(tracker);
                tracker.close();
            }
        });
        bodies.forEach((id, body) -> {
            Player player = plugin.getServer().getPlayer(id);
            if (body.tracker.isClosed() || player == null || !player.isOnline()) closePlayer(id);
            else if (abilitiesOnly() && now - body.actionDeadline >= 0L) closeBody(id);
        });
        if (!reloading.get()) checkModels();
    }

    private void closeBody(UUID owner) {
        BodySession body = bodies.remove(owner);
        if (body != null) {
            body.tracker.close(); // Engine restores vanilla body/equipment visibility.
            swordItems.restorePlayerSoon(owner);
        }
    }

    private void closePlayer(UUID owner) {
        lastSlashSwings.remove(owner);
        closeBody(owner);
        effects.forEach((tracker, playback) -> {
            if (playback.owner().equals(owner)) { effects.remove(tracker); tracker.close(); }
        });
        cinematicBodies.remove(owner);
    }

    private void disableAnimation(Throwable error) {
        failed = true;
        diagnostic = "BetterModel " + engine.getPluginMeta().getVersion() + " API failed: "
                + error.getClass().getSimpleName() + ": " + error.getMessage()
                + "; integration checked against 3.1; abilities still work";
        plugin.getLogger().log(Level.WARNING, diagnostic, error);
        bodies.values().forEach(body -> body.tracker.close());
        bodies.keySet().forEach(swordItems::restorePlayerSoon);
        bodies.clear();
        effects.keySet().forEach(Tracker::close);
        effects.clear();
        cinematicBodies.clear();
        lastSlashSwings.clear();
    }

    @Override public String status() {
        if (!closed && !failed && engine.isEnabled() && !reloading.get() && !needsImport) {
            try {
                if ((!abilitiesOnly() || playerBodyAnimationsEnabled()) && !BetterModel.config().module().playerAnimation())
                    return "Enable module.player-animation in BetterModel/config.yml, then restart";
                if (!BetterModel.config().module().model())
                    return "Enable module.model in BetterModel/config.yml for animated slash models";
                List<String> missing = missingModels();
                return missing.isEmpty() ? readyStatus() : "BetterModel missing assets: " + String.join(", ", missing);
            } catch (LinkageError | RuntimeException error) { disableAnimation(error); }
        }
        return diagnostic;
    }

    @EventHandler public void quit(PlayerQuitEvent event) { closePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler public void died(PlayerDeathEvent event) { closePlayer(event.getEntity().getUniqueId()); }
    @EventHandler public void worldChanged(PlayerChangedWorldEvent event) { closePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler(priority = EventPriority.MONITOR) public void held(PlayerItemHeldEvent event) {
        closePlayer(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR) public void dropped(PlayerDropItemEvent event) {
        closePlayer(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR) public void swapped(PlayerSwapHandItemsEvent event) {
        closePlayer(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR) public void clicked(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) closePlayer(player.getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR) public void dragged(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) closePlayer(player.getUniqueId());
    }
    @EventHandler public void pluginDisabled(PluginDisableEvent event) { if (event.getPlugin() == engine) close(); }

    @Override public void close() {
        if (closed) return;
        closed = true;
        startupCheck.cancel();
        watchdog.cancel();
        HandlerList.unregisterAll(this);
        bodies.values().forEach(body -> body.tracker.close());
        bodies.keySet().forEach(swordItems::restorePlayerSoon);
        bodies.clear();
        effects.keySet().forEach(Tracker::close);
        effects.clear();
        cinematicBodies.clear();
        lastSlashSwings.clear();
        diagnostic = "BetterModel animations stopped";
    }

    private record Playback(UUID owner, long deadline) { }
    private static final class BodySession {
        final UUID owner;
        final String swordId;
        final ModelRenderer renderer;
        // Initialized before the session is published to bodies.
        EntityTracker tracker;
        // Owned snapshots are replaced on the server thread and never exposed to the mapper.
        private volatile ItemStack cachedItem;
        String desiredLoop = "idle", runningLoop, action;
        int actionPriority;
        boolean primed;
        long actionDeadline, lastActionStart;
        BodySession(UUID owner, String swordId, ModelRenderer renderer, ItemStack cachedItem) {
            this.owner = owner; this.swordId = swordId; this.renderer = renderer; this.cachedItem = cachedItem;
        }
        ItemStack displayItem() { return cachedItem.clone(); }
    }
}
