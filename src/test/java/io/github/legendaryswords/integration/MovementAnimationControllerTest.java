package io.github.legendaryswords.integration;

import static io.github.legendaryswords.integration.AnimationBridge.Locomotion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.legendaryswords.sword.SwordCatalog;
import io.github.legendaryswords.sword.SwordType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

class MovementAnimationControllerTest {
    @Test void walkRunAndStillnessHysteresisUseActualHorizontalMovement() {
        assertEquals(WALK, MovementAnimationController.select(.10, false, 0, IDLE));
        assertEquals(RUN, MovementAnimationController.select(.10, true, 0, WALK));
        assertEquals(RUN, MovementAnimationController.select(0, true, 2, RUN));
        assertEquals(IDLE, MovementAnimationController.select(0, true, 4, RUN));
        assertEquals(IDLE, MovementAnimationController.select(25, true, 0, RUN));
        assertEquals(IDLE, MovementAnimationController.select(Double.NaN, true, 0, RUN));
    }

    @Test void heldSwordGetsPersistentUpdatesAndRemovingItRestoresThePlayer() {
        Fixture f = new Fixture();
        f.tick.get().run();
        verify(f.bridge).tickMovement(f.player, "master_sword", IDLE);
        f.location.set(new Location(f.world, .45, 64, 0));
        when(f.player.isSprinting()).thenReturn(true);
        f.tick.get().run();
        verify(f.bridge).tickMovement(f.player, "master_sword", RUN);
        when(f.swords.identify(any())).thenReturn(Optional.empty());
        f.tick.get().run();
        verify(f.bridge).stopMovement(f.player);
        f.controller.close();
        verify(f.timer).cancel();
    }

    @Test void ineligiblePlayerIsRestoredEvenBeforeFirstMovementSample() {
        Fixture f = new Fixture();
        when(f.player.getGameMode()).thenReturn(GameMode.SPECTATOR);
        f.tick.get().run();
        verify(f.bridge).stopMovement(f.player);
        verify(f.bridge, never()).tickMovement(any(), anyString(), any());
        f.controller.close();
    }

    private static final class Fixture {
        final AnimationBridge bridge = mock(AnimationBridge.class);
        final SwordCatalog swords = mock(SwordCatalog.class);
        final Player player = mock(Player.class);
        final World world = mock(World.class);
        final BukkitTask timer = mock(BukkitTask.class);
        final AtomicReference<Runnable> tick = new AtomicReference<>();
        final AtomicReference<Location> location = new AtomicReference<>(new Location(world, 0, 64, 0));
        final MovementAnimationController controller;
        Fixture() {
            JavaPlugin plugin = mock(JavaPlugin.class);
            Server server = mock(Server.class);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(plugin.getServer()).thenReturn(server);
            when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
            when(server.getScheduler()).thenReturn(scheduler);
            doReturn(List.of(player)).when(server).getOnlinePlayers();
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(2L), eq(2L))).thenAnswer(call -> {
                tick.set(call.getArgument(1));
                return timer;
            });
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
            when(player.hasPermission("legendaryswords.use")).thenReturn(true);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(player.getLocation()).thenAnswer(call -> location.get().clone());
            when(swords.identify(any())).thenReturn(Optional.of(SwordType.MASTER_SWORD));
            controller = new MovementAnimationController(plugin, bridge, swords);
        }
    }
}
