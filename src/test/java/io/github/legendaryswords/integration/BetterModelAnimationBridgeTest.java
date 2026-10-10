package io.github.legendaryswords.integration;

import static io.github.legendaryswords.integration.AnimationBridge.Locomotion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformAdapter;
import kr.toxicity.model.api.platform.PlatformItemStack;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Covers the engine's first-viewer startup and persistent locomotion contract. */
class BetterModelAnimationBridgeTest {
    @Test void firstViewerReclaimsEngineLoopsOnceAndWalkingDoesNotRestartEveryTick() {
        try (Fixture f = new Fixture()) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            clearInvocations(f.tracker);

            // BetterModel installs its default idle/walk when the first viewer spawns.
            when(f.tracker.isScheduled()).thenReturn(true);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker).animate(eq("walk"), argThat(modifier ->
                    modifier.type() == AnimationIterator.Type.LOOP && modifier.priority() == 10));
            clearInvocations(f.tracker);

            for (int tick = 0; tick < 30; tick++) f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker, never()).stopAnimation(anyString());
            verify(f.tracker, never()).animate(anyString(), any(AnimationModifier.class));
            verify(f.renderer, times(1)).create(any(PlatformEntity.class), any(TrackerModifier.class));

            f.bridge.tickMovement(f.player, "master_sword", RUN);
            verify(f.tracker).animate(eq("run"), any(AnimationModifier.class));
            f.bridge.stopMovement(f.player);
            verify(f.tracker).close();
        }
    }

    @Test void viewerSchedulerRestartReclaimsLoopWithoutReplacingTheBodyTracker() {
        try (Fixture f = new Fixture()) {
            when(f.tracker.isScheduled()).thenReturn(true);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            clearInvocations(f.tracker);
            when(f.tracker.isScheduled()).thenReturn(false);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            when(f.tracker.isScheduled()).thenReturn(true);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker).animate(eq("walk"), any(AnimationModifier.class));
            verify(f.renderer, times(1)).create(any(PlatformEntity.class), any(TrackerModifier.class));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Player player = mock(Player.class);
        final MockedStatic<BetterModel> engineApi = mockStatic(BetterModel.class);
        final ModelRenderer renderer;
        final EntityTracker tracker;
        final BetterModelAnimationBridge bridge;

        Fixture() {
            // BoneTags initialize their air item through the platform singleton.
            BetterModelPlatform platform = mock(BetterModelPlatform.class);
            PlatformAdapter adapter = mock(PlatformAdapter.class);
            when(platform.adapter()).thenReturn(adapter);
            when(adapter.air()).thenReturn(mock(PlatformItemStack.class));
            engineApi.when(BetterModel::platform).thenReturn(platform);
            renderer = mock(ModelRenderer.class);
            tracker = mock(EntityTracker.class);
            JavaPlugin plugin = mock(JavaPlugin.class);
            Server server = mock(Server.class);
            PluginManager pluginManager = mock(PluginManager.class);
            Plugin engine = mock(Plugin.class);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            FileConfiguration config = mock(FileConfiguration.class);
            when(plugin.getServer()).thenReturn(server);
            when(plugin.getConfig()).thenReturn(config);
            when(server.getPluginManager()).thenReturn(pluginManager);
            when(server.getScheduler()).thenReturn(scheduler);
            when(pluginManager.getPlugin("BetterModel")).thenReturn(engine);
            when(engine.isEnabled()).thenReturn(true);
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(20L), eq(20L)))
                    .thenReturn(mock(BukkitTask.class));
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(2L)))
                    .thenReturn(mock(BukkitTask.class));
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            when(renderer.animation(anyString())).thenReturn(Optional.of(mock(BlueprintAnimation.class)));
            when(renderer.create(any(PlatformEntity.class), any(TrackerModifier.class))).thenReturn(tracker);
            when(tracker.animate(anyString(), any(AnimationModifier.class))).thenReturn(true);
            engineApi.when(() -> BetterModel.limbOrNull("legendaryswords_master_sword")).thenReturn(renderer);
            bridge = new BetterModelAnimationBridge(plugin);
        }

        @Override public void close() {
            bridge.close();
            engineApi.close();
        }
    }
}
