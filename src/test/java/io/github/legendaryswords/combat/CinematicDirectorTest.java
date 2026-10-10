package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.integration.AnimationBridge;
import io.github.legendaryswords.integration.AnimatedSwordItems;
import io.github.legendaryswords.sword.SwordType;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Regression coverage for the spectator snap-back and interrupted ultimate problems. */
class CinematicDirectorTest {
    @Test void nearbyVictimsAndObserversSeeTheSameSceneAndTheCasterPoseEndsAtCompletion() {
        Player observer = mock(Player.class), distant = mock(Player.class), hidden = mock(Player.class);
        when(observer.canSee(player)).thenReturn(true);
        when(observer.getLocation()).thenReturn(origin.clone().add(10, 0, 0));
        when(distant.canSee(player)).thenReturn(true);
        when(distant.getLocation()).thenReturn(origin.clone().add(50, 0, 0));
        when(world.getPlayers()).thenReturn(java.util.List.of(player, observer, distant, hidden));

        startRitual();
        var sharedTitle = org.mockito.ArgumentCaptor.forClass(net.kyori.adventure.title.Title.class);
        verify(player).showTitle(sharedTitle.capture());
        verify(observer).showTitle(sharedTitle.getValue());
        verify(distant, never()).showTitle(any(net.kyori.adventure.title.Title.class));
        verify(hidden, never()).showTitle(any(net.kyori.adventure.title.Title.class));
        verify(animations).holdCastAnimation(player);
        for (int frameIndex = 0; frameIndex < 18; frameIndex++) frame.get().run();
        assertEquals(0, completions.get());
        verify(animations, never()).stopCastAnimation(player);
        frame.get().run();
        assertEquals(1, completions.get());
        verify(animations).stopCastAnimation(player);
        assertPlayerUntouched();
    }
    @Test void ritualIsPresentedInFrontOfTheCasterInsteadOfInsideTheirCamera() {
        startRitual();
        var position = org.mockito.ArgumentCaptor.forClass(Location.class);
        verify(world).spawn(position.capture(), eq(ItemDisplay.class), org.mockito.ArgumentMatchers.<Consumer<ItemDisplay>>any());
        Location displayed = position.getValue();
        Location facing = origin.clone();
        facing.setPitch(0);
        Vector offset = displayed.toVector().subtract(origin.toVector());
        assertEquals(1.35, offset.getY(), 0.0001);
        offset.setY(0);
        assertEquals(2.5, offset.length(), 0.0001);
        assertEquals(2.5, offset.dot(facing.getDirection()), 0.0001);
        assertEquals(10, origin.getX());
        assertEquals(70, origin.getY());
        assertEquals(20, origin.getZ());
        assertPlayerUntouched();
    }
    private LegendarySwordsPlugin plugin;
    private PluginSettings settings;
    private PlayerStateStore states;
    private VisualEffects effects;
    private AnimationBridge animations;
    private Player player;
    private World world;
    private Location origin;
    private ItemDisplay blade;
    private BukkitTask task;
    private PersistentDataContainer data;
    private AtomicReference<GameMode> mode;
    private AtomicReference<World> currentWorld;
    private AtomicReference<String> recovery;
    private AtomicReference<Runnable> frame;
    private AtomicInteger completions;
    private CinematicDirector director;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        plugin = mock(LegendarySwordsPlugin.class);
        settings = mock(PluginSettings.class);
        states = mock(PlayerStateStore.class);
        effects = mock(VisualEffects.class);
        animations = mock(AnimationBridge.class);
        player = mock(Player.class);
        world = mock(World.class);
        blade = mock(ItemDisplay.class);
        task = mock(BukkitTask.class);
        data = mock(PersistentDataContainer.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID worldId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        mode = new AtomicReference<>(GameMode.SURVIVAL);
        currentWorld = new AtomicReference<>(world);
        recovery = new AtomicReference<>();
        frame = new AtomicReference<>();
        completions = new AtomicInteger();
        origin = new Location(world, 10, 70, 20, 45, 15);

        when(plugin.getName()).thenReturn("LegendarySwords");
        when(plugin.namespace()).thenReturn("legendaryswords");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LegendarySwords-ritual-tests"));
        when(plugin.animations()).thenReturn(animations);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getWorld(worldId)).thenReturn(world);
        when(world.getUID()).thenReturn(worldId);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("RitualTester");
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenAnswer(invocation -> mode.get());
        doAnswer(invocation -> {
            mode.set(invocation.getArgument(0));
            return null;
        }).when(player).setGameMode(any(GameMode.class));
        when(player.getWorld()).thenAnswer(invocation -> currentWorld.get());
        when(player.getLocation()).thenReturn(origin);
        when(player.teleport(any(Location.class))).thenReturn(true);
        when(player.getPersistentDataContainer()).thenReturn(data);
        when(settings.cinematicsEnabled()).thenReturn(true);
        when(settings.cinematicTicks()).thenReturn(36);
        when(states.cinematics(playerId)).thenReturn(true);
        when(blade.isValid()).thenReturn(true);

        when(data.get(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenAnswer(invocation -> recovery.get());
        doAnswer(invocation -> {
            recovery.set(null);
            return null;
        }).when(data).remove(any(NamespacedKey.class));

        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack sword = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(sword);
        when(sword.clone()).thenReturn(sword);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), org.mockito.ArgumentMatchers.<Consumer<ItemDisplay>>any())).thenAnswer(invocation -> {
            Consumer<ItemDisplay> initialize = invocation.getArgument(2);
            initialize.accept(blade);
            return blade;
        });

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(2L))).thenAnswer(invocation -> {
            frame.set(invocation.getArgument(1));
            return task;
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        director = new CinematicDirector(plugin, settings, states, effects);
    }

    @AfterEach
    void releaseStaticMock() { bukkit.close(); }

    @Test void detachedRitualUsesTheOrdinaryFixedModelWithoutChangingTheHeldSword() {
        AnimatedSwordItems items = mock(AnimatedSwordItems.class);
        ItemStack held = mock(ItemStack.class), visual = mock(ItemStack.class);
        when(plugin.animatedSwordItems()).thenReturn(items);
        when(player.getInventory().getItemInMainHand()).thenReturn(held);
        when(held.clone()).thenReturn(visual);
        startRitual();
        verify(items).restoreItem(visual);
        verify(items, never()).restoreItem(held);
        verify(blade).setItemStack(visual);
        director.close();
    }

    @Test
    void ordinaryCompletionShowsTheRitualWithoutChangingPlayerControl() {
        startRitual();
        for (int i = 0; i < 19; i++) frame.get().run();

        assertEquals(1, completions.get());
        verify(blade, atLeastOnce()).setTransformation(any(Transformation.class));
        verify(effects, atLeastOnce()).ring(any(Location.class), anyDouble(), eq(SwordType.MASTER_SWORD));
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void repeatedCancellationCompletesExactlyOnceAndReleasesPresentation() {
        startRitual();

        director.cancel(player);
        director.cancel(player);
        director.close();
        frame.get().run(); // A queued scheduler frame must not resurrect a cancelled scene.

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void quittingCompletesAndRemovesTheRitualWithoutTouchingAnOfflinePlayer() {
        startRitual();
        when(player.isOnline()).thenReturn(false);

        frame.get().run();

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void deathCompletesAndCleansUpWithoutAnyRestorationTeleport() {
        startRitual();
        when(player.isDead()).thenReturn(true);

        frame.get().run();

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void worldChangesKeepTheDestinationAndFinishTheScene() {
        startRitual();
        currentWorld.set(mock(World.class));

        frame.get().run();

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void externalTeleportCancelsWithoutSnappingBack() {
        startRitual();

        director.cancelForTeleport(player);

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void walkingOutOfTheSceneKeepsTheNewPosition() {
        startRitual();
        when(player.getLocation()).thenReturn(origin.clone().add(9, 0, 0));

        frame.get().run();

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void pluginDisableFinishesEveryActiveSessionExactlyOnce() {
        startRitual();

        director.close();
        director.close();

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void interruptedEffectsStillCompleteTheUltimate() {
        startRitual();
        doThrow(new IllegalStateException("simulated particle error")).when(effects).pulse(any(Location.class), any(SwordType.class));

        assertDoesNotThrow(() -> frame.get().run());

        assertEquals(1, completions.get());
        assertPlayerUntouched();
        assertCleanedUp();
    }

    @Test
    void failedDisplayInitializationRemovesThePartialEntityAndCompletes() {
        doThrow(new IllegalStateException("simulated display error")).when(blade).setGlowing(true);

        assertDoesNotThrow(() -> director.start(player, SwordType.MASTER_SWORD, completions::incrementAndGet));

        assertFalse(director.active(player));
        assertEquals(1, completions.get());
        verify(blade).remove();
        verify(animations).cancelCinematic(player);
        assertPlayerUntouched();
    }

    @Test
    void cleanupFailuresDoNotSwallowCompletionOrLeaveTheSessionLocked() {
        startRitual();
        doThrow(new IllegalStateException("simulated remove error")).when(blade).remove();
        doThrow(new IllegalStateException("simulated bridge error")).when(animations).cancelCinematic(player);

        assertDoesNotThrow(() -> director.cancel(player));

        assertEquals(1, completions.get());
        assertCleanedUp();
    }

    @Test
    void disabledCinematicsCompleteImmediatelyWithoutSpawningEntities() {
        when(settings.cinematicsEnabled()).thenReturn(false);

        director.start(player, SwordType.MASTER_SWORD, completions::incrementAndGet);

        assertEquals(1, completions.get());
        assertFalse(director.active(player));
        verifyNoInteractions(blade, task);
        assertPlayerUntouched();
    }

    @Test
    void aSecondStartCannotLoseItsCallbackOrReplaceTheFirstSession() {
        startRitual();
        AtomicInteger second = new AtomicInteger();

        director.start(player, SwordType.MASTER_SWORD, second::incrementAndGet);

        assertEquals(1, second.get());
        assertEquals(0, completions.get());
        assertTrue(director.active(player));
        director.cancel(player);
        assertEquals(1, completions.get());
        assertCleanedUp();
    }

    @Test
    void savedLegacyCameraStateRestoresASpectatorAfterTheSessionMapWasLost() {
        mode.set(GameMode.SPECTATOR);
        recovery.set(legacyMarker());

        director.recover(player);

        assertEquals(GameMode.SURVIVAL, mode.get());
        verify(player).setSpectatorTarget(null);
        verify(player).teleport(origin);
        verify(player).setAllowFlight(false);
        assertNull(recovery.get());
    }

    @Test
    void anOldMarkerDoesNotOverrideANormalPlayersModeOrPosition() {
        recovery.set(legacyMarker());

        director.recover(player);

        assertNull(recovery.get());
        verify(player, never()).setGameMode(any(GameMode.class));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void invalidLegacyRecoveryDataDoesNotMoveThePlayerAndRemainsForRepair() {
        mode.set(GameMode.SPECTATOR);
        recovery.set(legacyMarker().replace("10.0", "NaN"));

        assertDoesNotThrow(() -> director.recover(player));

        assertNotNull(recovery.get());
        verify(player, never()).setGameMode(any(GameMode.class));
        verify(player, never()).teleport(any(Location.class));
    }

    private String legacyMarker() {
        return "1|SURVIVAL|" + world.getUID() + "|10.0|70.0|20.0|45.0|15.0|false|false";
    }

    private void startRitual() {
        director.start(player, SwordType.MASTER_SWORD, completions::incrementAndGet);
        assertTrue(director.active(player));
        assertEquals(GameMode.SURVIVAL, mode.get());
        assertNull(recovery.get(), "New rituals must never write spectator recovery data");
    }

    private void assertPlayerUntouched() {
        verify(player, never()).setGameMode(any(GameMode.class));
        verify(player, never()).setSpectatorTarget(any());
        verify(player, never()).teleport(any(Location.class));
        verify(player, never()).setAllowFlight(anyBoolean());
        verify(player, never()).setFlying(anyBoolean());
        verify(player, never()).setVelocity(any(Vector.class));
        verify(player, never()).setFallDistance(anyFloat());
        verify(data, never()).set(any(NamespacedKey.class), eq(PersistentDataType.STRING), anyString());
    }

    private void assertCleanedUp() {
        assertFalse(director.active(player));
        verify(task).cancel();
        verify(blade).remove();
        verify(animations).cancelCinematic(player);
    }
}
