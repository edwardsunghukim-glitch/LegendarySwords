package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.integration.AnimationBridge;
import io.github.legendaryswords.sword.SwordType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Lifecycle checks with a mocked scheduler and entities, without a Minecraft server. */
class CinematicDirectorTest {
    private LegendarySwordsPlugin plugin;
    private PluginSettings settings;
    private PlayerStateStore states;
    private VisualEffects effects;
    private AnimationBridge animations;
    private Player player;
    private World world;
    private Location origin;
    private ArmorStand camera;
    private ItemDisplay blade;
    private BukkitTask task;
    private PersistentDataContainer data;
    private AtomicReference<GameMode> mode;
    private AtomicReference<World> currentWorld;
    private AtomicReference<String> recovery;
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
        camera = mock(ArmorStand.class);
        blade = mock(ItemDisplay.class);
        task = mock(BukkitTask.class);
        data = mock(PersistentDataContainer.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID worldId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        mode = new AtomicReference<>(GameMode.SURVIVAL);
        currentWorld = new AtomicReference<>(world);
        recovery = new AtomicReference<>();
        origin = new Location(world, 10, 70, 20, 45, 15);

        when(plugin.getName()).thenReturn("LegendarySwords");
        when(plugin.namespace()).thenReturn("legendaryswords");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LegendarySwords-camera-tests"));
        when(plugin.animations()).thenReturn(animations);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getWorld(worldId)).thenReturn(world);
        when(world.getUID()).thenReturn(worldId);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("CameraTester");
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenAnswer(invocation -> mode.get());
        doAnswer(invocation -> {
            mode.set(invocation.getArgument(0));
            return null;
        }).when(player).setGameMode(any(GameMode.class));
        when(player.getWorld()).thenAnswer(invocation -> currentWorld.get());
        when(player.getLocation()).thenReturn(origin);
        when(player.teleport(any(Location.class))).thenReturn(true);
        when(player.getVelocity()).thenReturn(new Vector(0.1, 0, 0.2));
        when(player.getFallDistance()).thenReturn(2.5f);
        when(player.getPersistentDataContainer()).thenReturn(data);
        when(settings.cinematicsEnabled()).thenReturn(true);
        when(settings.cinematicTicks()).thenReturn(36);
        when(settings.cameraRadius()).thenReturn(4.0);
        when(states.cinematics(playerId)).thenReturn(true);

        doAnswer(invocation -> {
            recovery.set(invocation.getArgument(2));
            return null;
        }).when(data).set(any(NamespacedKey.class), eq(PersistentDataType.STRING), anyString());
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
        when(world.spawn(any(Location.class), eq(ArmorStand.class), org.mockito.ArgumentMatchers.<Consumer<ArmorStand>>any())).thenAnswer(invocation -> {
            Consumer<ArmorStand> initialize = invocation.getArgument(2);
            initialize.accept(camera);
            return camera;
        });
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), org.mockito.ArgumentMatchers.<Consumer<ItemDisplay>>any())).thenAnswer(invocation -> {
            Consumer<ItemDisplay> initialize = invocation.getArgument(2);
            initialize.accept(blade);
            return blade;
        });

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(2L))).thenReturn(task);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        director = new CinematicDirector(plugin, settings, states, effects);
    }

    @AfterEach
    void releaseStaticMock() {
        bukkit.close();
    }

    @Test
    void cancellingAnOrdinaryCameraRestoresControlAndRemovesEveryTemporaryPresentation() {
        startCamera();

        director.cancel(player);

        assertEquals(GameMode.SURVIVAL, mode.get());
        verify(player).teleport(origin);
        verify(player).setAllowFlight(false);
        verify(player).setVelocity(new Vector(0.1, 0, 0.2));
        verify(player).setFallDistance(2.5f);
        assertNull(recovery.get());
        assertCleanedUp();
    }

    @Test
    void deathCleanupDoesNotTeleportTheDeadPlayer() {
        startCamera();
        when(player.isDead()).thenReturn(true);

        director.cancel(player);

        assertEquals(GameMode.SURVIVAL, mode.get());
        verify(player, never()).teleport(any(Location.class));
        assertNull(recovery.get());
        assertCleanedUp();
    }

    @Test
    void worldChangeCleanupPreservesTheNewWorldDestination() {
        startCamera();
        currentWorld.set(mock(World.class));

        director.cancel(player);

        assertEquals(GameMode.SURVIVAL, mode.get());
        verify(player, never()).teleport(any(Location.class));
        assertNull(recovery.get());
        assertCleanedUp();
    }

    @Test
    void externalModeChangesArePreservedDuringCameraCleanup() {
        startCamera();
        mode.set(GameMode.CREATIVE);

        director.cancel(player);

        assertEquals(GameMode.CREATIVE, mode.get());
        verify(player, never()).setGameMode(GameMode.SURVIVAL);
        verify(player, never()).teleport(any(Location.class));
        verify(player, never()).setAllowFlight(anyBoolean());
        assertNull(recovery.get());
        assertCleanedUp();
    }

    @Test
    void externalTeleportCleanupCannotTeleportBackToTheCastingPosition() {
        startCamera();

        director.cancelForTeleport(player);

        assertEquals(GameMode.SURVIVAL, mode.get());
        verify(player, never()).teleport(any(Location.class));
        assertNull(recovery.get());
        assertCleanedUp();
    }

    @Test
    void failedRestorationStillRemovesEntitiesAndKeepsItsRecoveryMarker() {
        startCamera();
        when(player.teleport(any(Location.class))).thenThrow(new IllegalStateException("simulated teleport failure"));

        assertDoesNotThrow(() -> director.cancel(player));

        assertNotNull(recovery.get(), "A failed restoration must retain its recovery marker");
        verify(data, never()).remove(any(NamespacedKey.class));
        assertCleanedUp();
    }

    @Test
    void savedCameraStateCanRestoreASpectatorAfterTheSessionMapIsLost() {
        startCamera();
        assertNotNull(recovery.get());
        CinematicDirector freshDirector = new CinematicDirector(plugin, settings, states, effects);

        freshDirector.recover(player);

        assertEquals(GameMode.SURVIVAL, mode.get());
        verify(player).teleport(origin);
        verify(player).setAllowFlight(false);
        assertNull(recovery.get());
    }

    private void startCamera() {
        director.start(player, SwordType.MASTER_SWORD, () -> fail("An interrupted camera must not launch the ultimate"));
        assertTrue(director.active(player));
        assertEquals(GameMode.SPECTATOR, mode.get());
    }

    private void assertCleanedUp() {
        assertFalse(director.active(player));
        verify(task).cancel();
        verify(camera).remove();
        verify(blade).remove();
        verify(animations).cancelCinematic(player);
    }
}
