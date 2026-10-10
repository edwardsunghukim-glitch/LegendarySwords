package io.github.legendaryswords.integration;

import static io.github.legendaryswords.integration.AnimationBridge.Locomotion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import io.papermc.paper.plugin.configuration.PluginMeta;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.BetterModelConfig;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bone.BoneItemMapper;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.bukkit.platform.BukkitItemStack;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.config.ModuleConfig;
import kr.toxicity.model.api.manager.PlayerManager;
import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformAdapter;
import kr.toxicity.model.api.platform.PlatformItemStack;
import kr.toxicity.model.api.platform.PlatformItemTransform;
import kr.toxicity.model.api.platform.PlatformLocation;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.nms.ModelDisplay;
import kr.toxicity.model.api.nms.NMS;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import org.bukkit.Server;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

/** Covers the engine's first-viewer startup and persistent locomotion contract. */
class BetterModelAnimationBridgeTest {
    @Test void abilitiesOnlyDefaultsSuppressBodiesOrdinarySwingsAndPosesButKeepAttacks() {
        try (Fixture f = new Fixture()) {
            f.config.set("bettermodel.abilities-only", null); // Existing config, even with body=true.
            f.config.set("bettermodel.cast-body-animations", false);
            for (var movement : List.of(IDLE, WALK, RUN))
                f.bridge.tickMovement(f.player, "master_sword", movement);
            f.bridge.playSwing(f.player, "master_sword");
            f.bridge.playPose(f.player, "master_sword");
            assertFalse(f.bridge.playCinematic(f.player, "master_sword", f.player.getLocation()));
            verifyNoInteractions(f.swordItems, f.tracker, f.slash);
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
            verify(f.renderer, never()).create(any(PlatformLocation.class), any(ModelProfile.class), any(TrackerModifier.class), any());

            f.bridge.playAbility(f.player, "master_sword");
            f.bridge.playUltimate(f.player, "master_sword");
            verify(f.slashRenderer, times(2)).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
            verify(f.slash).animate(eq("ability"), any(AnimationModifier.class), any(Runnable.class));
            verify(f.slash).animate(eq("ultimate"), any(AnimationModifier.class), any(Runnable.class));
            verify(f.slash, never()).animate(eq("swing"), any(AnimationModifier.class), any(Runnable.class));
            verifyNoInteractions(f.swordItems, f.tracker);
            assertTrue(f.bridge.status().contains("attack effects only"));
        }
    }

    @Test void abilitiesOnlyDoesNotRequirePlayerModelsOrPlayerAnimationModule() {
        try (Fixture f = new Fixture()) {
            f.config.set("bettermodel.abilities-only", true);
            f.config.set("bettermodel.cast-body-animations", false);
            BetterModelConfig engineConfig = mock(BetterModelConfig.class);
            ModuleConfig modules = mock(ModuleConfig.class);
            when(engineConfig.module()).thenReturn(modules);
            when(modules.model()).thenReturn(true);
            f.engineApi.when(BetterModel::config).thenReturn(engineConfig);
            f.engineApi.when(() -> BetterModel.limbOrNull(anyString())).thenReturn(null);
            when(f.slashRenderer.animation("swing")).thenReturn(Optional.empty());

            assertTrue(f.bridge.status().contains("attack effects only"));
            f.bridge.playAbility(f.player, "master_sword");
            f.bridge.playUltimate(f.player, "master_sword");
            verify(f.slashRenderer, times(2)).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
            verifyNoInteractions(f.swordItems, f.tracker);
        }
    }

    @Test void switchingToAbilitiesOnlyRestoresThePlayerWithoutCancellingAttackEffects() {
        try (Fixture f = new Fixture()) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            f.bridge.playAbility(f.player, "master_sword");
            f.config.set("bettermodel.abilities-only", true);
            f.config.set("bettermodel.cast-body-animations", false);
            f.bridge.tickMovement(f.player, "master_sword", RUN);
            verify(f.tracker).close();
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
            verify(f.slash, never()).close();
            clearInvocations(f.renderer, f.swordItems);
            f.bridge.playSwing(f.player, "master_sword");
            f.bridge.playPose(f.player, "master_sword");
            f.bridge.playUltimate(f.player, "master_sword");
            verifyNoInteractions(f.swordItems, f.renderer);
            verify(f.slashRenderer, times(2)).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
        }
    }

    @Test void castBodyHidesObserverEquipmentButNeverSpawnsForOwnerOrChangesTheirSlot() {
        try (Fixture f = new Fixture()) {
            f.config.set("bettermodel.abilities-only", true);
            Player observer = mock(Player.class), arriving = mock(Player.class);
            when(observer.getUniqueId()).thenReturn(UUID.randomUUID());
            when(arriving.getUniqueId()).thenReturn(UUID.randomUUID());
            when(observer.canSee(f.player)).thenReturn(true);
            when(arriving.canSee(f.player)).thenReturn(true);
            World world = f.player.getLocation().getWorld();
            Location observerLocation = f.player.getLocation().clone();
            when(observer.getLocation()).thenReturn(observerLocation);
            when(arriving.getLocation()).thenReturn(observerLocation);
            when(world.getPlayers()).thenReturn(List.of(f.player, observer));
            f.bridge.playAbility(f.player, "master_sword");
            verify(f.tracker).hideOption(argThat(option -> option.equipment() && option.visibility()));
            verify(f.tracker).markPlayerForSpawn(argThat((java.util.Set<UUID> viewers) ->
                    !viewers.contains(f.player.getUniqueId()) && viewers.contains(observer.getUniqueId())));
            verify(f.swordItems, never()).begin(any(), anyString());
            when(world.getPlayers()).thenReturn(List.of(f.player, observer, arriving));
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker).markPlayerForSpawn(argThat((java.util.Set<UUID> viewers) ->
                    !viewers.contains(f.player.getUniqueId()) && viewers.contains(arriving.getUniqueId())));
        }
    }

    @Test void characterAnimationsExistOnlyDuringCastsAndRestoreWithoutStartingIdleOrWalkLoops() {
        try (Fixture f = new Fixture()) {
            f.config.set("bettermodel.abilities-only", null);
            for (var movement : List.of(IDLE, WALK, RUN)) f.bridge.tickMovement(f.player, "master_sword", movement);
            f.bridge.playSwing(f.player, "master_sword");
            f.bridge.playPose(f.player, "master_sword");
            verifyNoInteractions(f.swordItems, f.tracker);

            f.bridge.playAbility(f.player, "master_sword");
            f.bridge.tickMovement(f.player, "master_sword", RUN);
            verify(f.tracker).animate(eq("ability"), any(AnimationModifier.class));
            for (String clip : List.of("idle", "walk", "run", "pose", "swing"))
                verify(f.tracker, never()).animate(eq(clip), any(AnimationModifier.class));
            verify(f.tracker, never()).close();
            f.nanos.addAndGet(TimeUnit.SECONDS.toNanos(4));
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker).close();
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
            verify(f.slash, never()).close();
            clearInvocations(f.swordItems, f.renderer);
            f.bridge.tickMovement(f.player, "master_sword", IDLE);
            verifyNoInteractions(f.swordItems, f.renderer);
            assertTrue(f.bridge.status().contains("temporary cast body animations"));
        }
    }

    @Test void ultimateHoldsItsLastPoseUntilSceneEndWithoutLeavingMovementAnimationsBehind() {
        try (Fixture f = new Fixture()) {
            f.config.set("bettermodel.abilities-only", true);
            f.bridge.playUltimate(f.player, "master_sword");
            verify(f.tracker).animate(eq("ultimate"), argThat(modifier -> modifier.type() == AnimationIterator.Type.HOLD_ON_LAST));
            f.bridge.holdCastAnimation(f.player);
            f.nanos.addAndGet(TimeUnit.SECONDS.toNanos(6));
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker, never()).close();
            f.bridge.stopCastAnimation(f.player);
            f.bridge.stopCastAnimation(f.player);
            verify(f.tracker).close();
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
            verify(f.slash, never()).close();
            for (String clip : List.of("idle", "walk", "run"))
                verify(f.tracker, never()).animate(eq(clip), any(AnimationModifier.class));
        }
    }

    @Test void releaseAnimatesAtImpactThenRestoresWithoutAHiddenOwnerInventory() {
        try (Fixture f = new Fixture()) {
            f.config.set("bettermodel.abilities-only", true);
            f.bridge.playRelease(f.player, "master_sword");
            verify(f.tracker).animate(eq("release"), any(AnimationModifier.class));
            verify(f.swordItems, never()).begin(any(), anyString());
            verify(f.slashRenderer, never()).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
            f.nanos.addAndGet(TimeUnit.SECONDS.toNanos(2));
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker).close();
            for (String loop : List.of("idle", "walk", "run")) verify(f.tracker, never()).animate(eq(loop), any(AnimationModifier.class));
        }
    }

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
            verify(f.renderer, times(1)).create(any(PlatformEntity.class), any(TrackerModifier.class), any());

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
            verify(f.renderer, times(1)).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
        }
    }

    @Test void playerBodyKeepsInventoryEquipmentVisibleBeforeAnimationsStart() {
        try (Fixture f = new Fixture()) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            var order = inOrder(f.tracker);
            order.verify(f.tracker).hideOption(argThat(option -> !option.equipment()
                    && option.visibility() && option.fire() && option.glowing()));
            order.verify(f.tracker).animate(eq("idle"), any(AnimationModifier.class));
        }
    }

    @Test void onlyAnimatedHeldItemUsesFixedContextAndItsSeparateDisplayClone() {
        try (Fixture f = new Fixture()) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            var mapper = ArgumentCaptor.forClass(BoneItemMapper.class);
            verify(f.hand).setItemMapper(mapper.capture());
            assertEquals(PlatformItemTransform.FIXED, mapper.getValue().transform());
            verify(f.handDisplay).display(PlatformItemTransform.FIXED);
            var displayed = mapper.getValue().apply(null, null);
            assertNotSame(f.displayItem, ((BukkitItemStack) displayed.itemStack()).source());
            assertNotSame(f.cachedItem, ((BukkitItemStack) displayed.itemStack()).source());
            verify(f.displayItem).clone();
            verify(f.cachedItem).clone();
            verify(f.swordItems).displayItem(f.player, "master_sword");
        }
    }

    @Test void playerBodyAnimationsDefaultToPausedBeforeInventoryOrEntityTrackerCreation() {
        try (Fixture f = new Fixture(false)) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            f.bridge.playPose(f.player, "master_sword");
            f.bridge.playSwing(f.player, "master_sword");
            f.bridge.playAbility(f.player, "master_sword");
            f.bridge.playUltimate(f.player, "master_sword");
            verifyNoInteractions(f.swordItems);
            verify(f.player, never()).getInventory();
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
            verify(f.tracker, never()).hideOption(any());
            assertTrue(f.bridge.status().contains("player body animations paused"));
            assertTrue(f.bridge.status().contains("slash effects available"));
            assertTrue(f.bridge.status().contains("install the 0.5.0 pack and updated BetterModel build"));
        }
    }

    @Test void pausedPlayerBodiesStillCreateCinematicActorsAndSlashEffects() {
        try (Fixture f = new Fixture(false)) {
            Location origin = f.player.getLocation();
            DummyTracker actor = mock(DummyTracker.class);
            when(actor.bone("right_item")).thenReturn(f.hand);
            when(actor.animate(eq("ultimate"), any(AnimationModifier.class), any(Runnable.class))).thenReturn(true);
            when(f.renderer.create(any(PlatformLocation.class), any(ModelProfile.class),
                    any(TrackerModifier.class), any())).thenAnswer(call -> {
                Consumer<DummyTracker> preUpdate = call.getArgument(3);
                preUpdate.accept(actor);
                return actor;
            });
            assertTrue(f.bridge.playCinematic(f.player, "master_sword", origin));
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            f.bridge.playSwing(f.player, "master_sword");
            f.bridge.playAbility(f.player, "master_sword");
            f.bridge.playUltimate(f.player, "master_sword");
            verify(f.renderer).create(any(PlatformLocation.class), any(ModelProfile.class),
                    any(TrackerModifier.class), any());
            verify(f.slashRenderer, times(3)).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
            verify(actor).animate(eq("ultimate"), any(AnimationModifier.class), any(Runnable.class));
            for (String clip : List.of("swing", "ability", "ultimate"))
                verify(f.slash).animate(eq(clip), any(AnimationModifier.class), any(Runnable.class));
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
            verify(f.swordItems, never()).begin(any(), anyString());
            f.bridge.close();
            verify(actor).close();
            verify(f.slash).close();
        }
    }

    @Test void pausedBodySlashSwingsThrottleRapidClicksUntilThePlayerSessionCloses() {
        try (Fixture f = new Fixture(false)) {
            f.bridge.playSwing(f.player, "master_sword");
            for (int click = 0; click < 4; click++) f.bridge.playSwing(f.player, "master_sword");
            verify(f.slashRenderer, times(1)).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
            verify(f.slash).animate(eq("swing"), any(AnimationModifier.class), any(Runnable.class));
            verifyNoInteractions(f.swordItems);

            f.bridge.stopMovement(f.player);
            verify(f.slash).close();
            f.bridge.playSwing(f.player, "master_sword");
            verify(f.slashRenderer, times(2)).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
            verify(f.slash, times(2)).animate(eq("swing"), any(AnimationModifier.class), any(Runnable.class));
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
        }
    }

    @Test void workerMapperClonesTheLatestServerThreadSnapshotWithoutTouchingInventory() throws Exception {
        try (Fixture f = new Fixture()) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            var mapper = ArgumentCaptor.forClass(BoneItemMapper.class);
            verify(f.hand).setItemMapper(mapper.capture());
            var first = ((BukkitItemStack) mapper.getValue().apply(null, null).itemStack()).source();

            ItemStack refreshedDisplay = mock(ItemStack.class);
            ItemStack refreshedCache = mock(ItemStack.class);
            when(refreshedDisplay.clone()).thenReturn(refreshedCache);
            when(refreshedCache.clone()).thenAnswer(call -> mock(ItemStack.class));
            when(f.swordItems.displayItem(f.player, "master_sword")).thenReturn(refreshedDisplay);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(refreshedDisplay).clone();
            clearInvocations(f.swordItems, f.player, f.cachedItem, refreshedCache);

            var displayed = CompletableFuture.supplyAsync(() -> mapper.getValue().apply(null, null))
                    .get(5L, TimeUnit.SECONDS);
            ItemStack copy = ((BukkitItemStack) displayed.itemStack()).source();
            copy.setAmount(1);
            ItemStack nextCopy = ((BukkitItemStack) mapper.getValue().apply(null, null).itemStack()).source();
            assertNotSame(first, copy);
            assertNotSame(refreshedDisplay, copy);
            assertNotSame(refreshedCache, copy);
            assertNotSame(copy, nextCopy);
            verifyNoInteractions(f.swordItems, f.player, f.cachedItem);
            verify(refreshedCache, times(2)).clone();
            verifyNoMoreInteractions(refreshedCache);
            verify(f.renderer, times(1)).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
        }
    }

    @Test void pausingAnExistingBodyRestoresInventoryAndKeepsItsSlashEffects() {
        try (Fixture f = new Fixture()) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            f.bridge.playSwing(f.player, "master_sword");
            f.config.set("bettermodel.player-body-animations", false);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.tracker).close();
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
            verify(f.slash, never()).close();
            clearInvocations(f.swordItems, f.renderer);

            f.bridge.playAbility(f.player, "master_sword");
            verifyNoInteractions(f.swordItems);
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
            verify(f.slashRenderer, times(2)).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
        }
    }

    @Test void bodyAndCinematicInventorySnapshotsAreNeverCapturedOffTheServerThread() {
        try (Fixture f = new Fixture()) {
            when(f.server.isPrimaryThread()).thenReturn(false);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            f.bridge.playSwing(f.player, "master_sword");
            assertFalse(f.bridge.playCinematic(f.player, "master_sword", f.player.getLocation()));
            verifyNoInteractions(f.swordItems);
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
            verify(f.renderer, never()).create(any(PlatformLocation.class), any(ModelProfile.class),
                    any(TrackerModifier.class), any());
        }
    }

    @Test void unavailableCustomItemModelsKeepTheirOrdinaryPlayerAndEquipment() {
        try (Fixture f = new Fixture()) {
            when(f.swordItems.begin(f.player, "master_sword")).thenReturn(false);
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
            verify(f.tracker, never()).animate(anyString(), any(AnimationModifier.class));
        }
    }

    @Test void failedEngineCreationRestoresTheRealSwordModel() {
        try (Fixture f = new Fixture()) {
            when(f.renderer.create(any(PlatformEntity.class), any(TrackerModifier.class), any()))
                    .thenThrow(new NoSuchMethodError("snapshot API mismatch"));
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
            assertTrue(f.bridge.status().contains("API failed"));
        }
    }

    @Test void failedInitialDisplaySnapshotRestoresTheRealSwordModelBeforeTrackerCreation() {
        try (Fixture f = new Fixture()) {
            when(f.swordItems.displayItem(f.player, "master_sword"))
                    .thenThrow(new IllegalStateException("display snapshot unavailable"));
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
            verify(f.renderer, never()).create(any(PlatformEntity.class), any(TrackerModifier.class), any());
            assertTrue(f.bridge.status().contains("API failed"));
        }
    }

    @Test void externalTrackerCloseAndInventoryTransferReleaseTemporaryModels() {
        try (Fixture f = new Fixture()) {
            var closed = new java.util.concurrent.atomic.AtomicReference<BiConsumer<Tracker, Tracker.CloseReason>>();
            doAnswer(call -> { closed.set(call.getArgument(0)); return null; }).when(f.tracker).handleCloseEvent(any());
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            closed.get().accept(f.tracker, Tracker.CloseReason.REMOVE);
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
            clearInvocations(f.swordItems);

            f.bridge.tickMovement(f.player, "master_sword", WALK);
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getWhoClicked()).thenReturn(f.player);
            f.bridge.clicked(click);
            verify(f.tracker).close();
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
        }
    }

    @Test void pluginShutdownRestoresRemainingTemporaryModels() {
        try (Fixture f = new Fixture()) {
            f.bridge.tickMovement(f.player, "master_sword", WALK);
            f.bridge.close();
            verify(f.tracker).close();
            verify(f.swordItems).restorePlayerSoon(f.player.getUniqueId());
        }
    }

    @Test void bossMorphUsesOrdinaryMeshesAndNeverHidesTheOwnersEquipment() {
        try (Fixture f = new Fixture()) {
            f.engineApi.when(() -> BetterModel.modelOrNull("legendaryswords_granite_colossus_boss")).thenReturn(f.renderer);
            assertTrue(f.bridge.startBoss(f.player, "granite_colossus"));
            verify(f.tracker).hideOption(kr.toxicity.model.api.tracker.EntityHideOption.DEFAULT);
            @SuppressWarnings("unchecked") var viewers = org.mockito.ArgumentCaptor.forClass(java.util.Set.class);
            verify(f.tracker).markPlayerForSpawn(viewers.capture());
            assertFalse(viewers.getValue().contains(f.player.getUniqueId()));
            assertFalse(viewers.getValue().isEmpty());
            verify(f.tracker, never()).bone("right_item");
            verify(f.player, never()).getInventory(); verifyNoInteractions(f.swordItems);
            verify(f.tracker).animate(eq("idle"), any(AnimationModifier.class));
            verify(f.tracker).animate(eq("transform"), any(AnimationModifier.class));
            f.bridge.stopBoss(f.player); verify(f.tracker).close();
        }
    }
    @Test void dragonBossUsesBreathAnimationAndASeparatePhysicalAttackModel() {
        try (Fixture f = new Fixture()) {
            f.engineApi.when(() -> BetterModel.modelOrNull("legendaryswords_infernal_dragon_boss")).thenReturn(f.renderer);
            assertTrue(f.bridge.startBoss(f.player, "infernal_dragon"));
            f.bridge.bossSmash(f.player);
            verify(f.tracker).animate(eq("breath"), any(AnimationModifier.class));
            verify(f.tracker, never()).animate(eq("smash"), any(AnimationModifier.class));
            verify(f.slashRenderer).create(any(PlatformLocation.class), any(TrackerModifier.class), any());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Player player = mock(Player.class);
        final MockedStatic<BetterModel> engineApi = mockStatic(BetterModel.class);
        final ModelRenderer renderer;
        final ModelRenderer slashRenderer;
        final EntityTracker tracker;
        final DummyTracker slash;
        final RenderedBone hand;
        final ModelDisplay handDisplay;
        final ItemStack displayItem = mock(ItemStack.class);
        final ItemStack cachedItem = mock(ItemStack.class);
        final AnimatedSwordItems swordItems = mock(AnimatedSwordItems.class);
        final Server server = mock(Server.class);
        final FileConfiguration config = new YamlConfiguration();
        final java.util.concurrent.atomic.AtomicLong nanos = new java.util.concurrent.atomic.AtomicLong(1);
        final BetterModelAnimationBridge bridge;

        Fixture() {
            this(true);
        }

        Fixture(boolean playerBodyAnimations) {
            // BoneTags initialize their air item through the platform singleton.
            BetterModelPlatform platform = mock(BetterModelPlatform.class);
            PlatformAdapter adapter = mock(PlatformAdapter.class);
            when(platform.adapter()).thenReturn(adapter);
            when(adapter.air()).thenReturn(mock(PlatformItemStack.class));
            engineApi.when(BetterModel::platform).thenReturn(platform);
            BetterModelConfig engineConfig = mock(BetterModelConfig.class);
            when(engineConfig.module()).thenReturn(ModuleConfig.DEFAULT);
            engineApi.when(BetterModel::config).thenReturn(engineConfig);
            when(platform.playerManager()).thenReturn(mock(PlayerManager.class));
            NMS nms = mock(NMS.class);
            when(nms.profile(any(PlatformPlayer.class))).thenReturn(mock(ModelProfile.class));
            engineApi.when(BetterModel::nms).thenReturn(nms);
            renderer = mock(ModelRenderer.class);
            slashRenderer = mock(ModelRenderer.class);
            tracker = mock(EntityTracker.class);
            slash = mock(DummyTracker.class);
            hand = mock(RenderedBone.class);
            handDisplay = mock(ModelDisplay.class);
            when(tracker.bone("right_item")).thenReturn(hand);
            when(hand.getDisplay()).thenReturn(handDisplay);
            JavaPlugin plugin = mock(JavaPlugin.class);
            PluginManager pluginManager = mock(PluginManager.class);
            Plugin engine = mock(Plugin.class);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(plugin.getServer()).thenReturn(server);
            when(plugin.getConfig()).thenReturn(config);
            config.set("bettermodel.install-bundled-models", false);
            config.set("bettermodel.abilities-only", false); // Explicit legacy-mode coverage.
            if (playerBodyAnimations) config.set("bettermodel.player-body-animations", true);
            when(plugin.getLogger()).thenReturn(Logger.getLogger("LegendarySwords-animation-tests"));
            when(server.getPluginManager()).thenReturn(pluginManager);
            when(server.getScheduler()).thenReturn(scheduler);
            when(server.isPrimaryThread()).thenReturn(true);
            when(pluginManager.getPlugin("BetterModel")).thenReturn(engine);
            when(engine.isEnabled()).thenReturn(true);
            PluginMeta engineMeta = mock(PluginMeta.class);
            when(engine.getPluginMeta()).thenReturn(engineMeta);
            when(engineMeta.getVersion()).thenReturn("3.1.0");
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(20L), eq(20L)))
                    .thenReturn(mock(BukkitTask.class));
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(2L)))
                    .thenReturn(mock(BukkitTask.class));
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            World world = mock(World.class);
            when(world.getPlayers()).thenReturn(List.of());
            when(player.getLocation()).thenReturn(new Location(world, 0D, 64D, 0D));
            when(swordItems.begin(player, "master_sword")).thenReturn(true);
            when(swordItems.displayItem(player, "master_sword")).thenReturn(displayItem);
            when(displayItem.clone()).thenReturn(cachedItem);
            when(cachedItem.clone()).thenAnswer(call -> mock(ItemStack.class));
            when(renderer.animation(anyString())).thenReturn(Optional.of(mock(BlueprintAnimation.class)));
            when(slashRenderer.animation(anyString())).thenReturn(Optional.of(mock(BlueprintAnimation.class)));
            when(slashRenderer.create(any(PlatformLocation.class), any(TrackerModifier.class), any()))
                    .thenAnswer(call -> {
                        Consumer<DummyTracker> preUpdate = call.getArgument(2);
                        preUpdate.accept(slash);
                        return slash;
                    });
            when(slash.animate(anyString(), any(AnimationModifier.class), any(Runnable.class))).thenReturn(true);
            when(renderer.create(any(PlatformEntity.class), any(TrackerModifier.class), any())).thenAnswer(call -> {
                Consumer<EntityTracker> preUpdate = call.getArgument(2);
                preUpdate.accept(tracker);
                return tracker;
            });
            when(tracker.animate(anyString(), any(AnimationModifier.class))).thenReturn(true);
            engineApi.when(() -> BetterModel.limbOrNull("legendaryswords_master_sword")).thenReturn(renderer);
            for (String sword : java.util.Arrays.stream(io.github.legendaryswords.sword.SwordType.values()).map(io.github.legendaryswords.sword.SwordType::id).toList()) {
                engineApi.when(() -> BetterModel.limbOrNull("legendaryswords_" + sword)).thenReturn(renderer);
                engineApi.when(() -> BetterModel.modelOrNull("legendaryswords_" + sword + "_slash"))
                        .thenReturn(slashRenderer);
            }
            bridge = new BetterModelAnimationBridge(plugin, swordItems, nanos::get);
        }

        @Override public void close() {
            bridge.close();
            engineApi.close();
        }
    }
}
