package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.integration.AnimationBridge;
import io.github.legendaryswords.mechanics.ChargeMeter;
import io.github.legendaryswords.mechanics.CooldownBook;
import io.github.legendaryswords.sword.SwordType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual ultimate damage must survive missing models and interrupted presentation. */
class AbilityControllerTest {
    private static final class Scheduled {
        final int id;
        final Runnable runnable;
        final long period;
        long nextTick;
        boolean cancelled;

        Scheduled(int id, Runnable runnable, long delay, long period) {
            this.id = id;
            this.runnable = runnable;
            this.nextTick = delay;
            this.period = period;
        }
    }

    private LegendarySwordsPlugin plugin;
    private PlayerStateStore states;
    private VisualEffects effects;
    private CombatService combat;
    private AnimationBridge animations;
    private CinematicDirector cinematics;
    private Player player;
    private LivingEntity target;
    private World world;
    private ChargeMeter charge;
    private CooldownBook cooldowns;
    private AbilityController controller;
    private final List<Scheduled> scheduled = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        plugin = mock(LegendarySwordsPlugin.class);
        PluginSettings settings = mock(PluginSettings.class);
        states = mock(PlayerStateStore.class);
        effects = mock(VisualEffects.class);
        combat = mock(CombatService.class);
        animations = mock(AnimationBridge.class);
        cinematics = mock(CinematicDirector.class);
        player = mock(Player.class);
        target = mock(LivingEntity.class);
        world = mock(World.class);
        charge = new ChargeMeter(100);
        cooldowns = new CooldownBook(() -> 0L);
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        when(plugin.getLogger()).thenReturn(Logger.getLogger("LegendarySwords-ultimate-tests"));
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(player.getUniqueId()).thenReturn(ownerId);
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("legendaryswords.use")).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenAnswer(invocation -> new Location(world, 10, 70, 20));
        when(player.getEyeLocation()).thenAnswer(invocation -> new Location(world, 10, 71.62, 20));
        when(states.charge(ownerId)).thenReturn(charge);
        when(settings.stats(any(SwordType.class))).thenReturn(new PluginSettings.SwordStats(10, 1.6, 8, 5, 6, 90));
        when(cinematics.canRun(player)).thenReturn(true);
        when(target.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000004"));
        when(target.getLocation()).thenAnswer(invocation -> new Location(world, 12, 70, 20));
        when(target.getWorld()).thenReturn(world);
        when(target.getHeight()).thenReturn(1.8);
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(target));
        when(combat.eligible(player, target)).thenReturn(true);
        when(combat.visible(any(Location.class), eq(target))).thenReturn(true);
        when(combat.damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt())).thenReturn(true);
        when(world.spawn(any(Location.class), eq(BlockDisplay.class), org.mockito.ArgumentMatchers.<Consumer<BlockDisplay>>any())).thenAnswer(invocation -> {
            BlockDisplay display = mock(BlockDisplay.class);
            when(display.getWorld()).thenReturn(world);
            when(display.isValid()).thenReturn(true);
            Consumer<BlockDisplay> initialize = invocation.getArgument(2);
            initialize.accept(display);
            return display;
        });

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenAnswer(invocation -> {
            int id = scheduled.size() + 1;
            Scheduled frame = new Scheduled(id, invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3));
            scheduled.add(frame);
            BukkitTask task = mock(BukkitTask.class);
            when(task.getTaskId()).thenReturn(id);
            return task;
        });
        doAnswer(invocation -> {
            int id = invocation.getArgument(0);
            scheduled.stream().filter(frame -> frame.id == id).forEach(frame -> frame.cancelled = true);
            return null;
        }).when(scheduler).cancelTask(anyInt());
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        controller = new AbilityController(plugin, settings, states, cooldowns, combat, effects, cinematics, animations);
    }

    @AfterEach
    void tearDown() {
        if (controller != null) controller.close();
        if (bukkit != null) bukkit.close();
    }

    @Test
    void ultimateDeliversDamageEvenWhenCinematicNeverInvokesItsCallback() {
        // The mocked director accepts start() but deliberately never calls the completion callback.
        controller.activate(player, SwordType.FLAMEBLADE, true);
        advanceTicks(120);

        assertEquals(0, charge.value());
        verify(combat, atLeastOnce()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
        verify(player, never()).setGameMode(any(GameMode.class));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void missingAnimationCannotPreventUltimateDamage() {
        doThrow(new IllegalStateException("simulated missing animation")).when(animations).playUltimate(player, SwordType.FLAMEBLADE.id());

        assertDoesNotThrow(() -> controller.activate(player, SwordType.FLAMEBLADE, true));
        advanceTicks(120);

        assertEquals(0, charge.value());
        verify(combat, atLeastOnce()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
    }

    @Test
    void incompatibleOptionalAnimationApiCannotPreventUltimateDamage() {
        doThrow(new NoSuchMethodError("simulated snapshot API change"))
                .when(animations).playUltimate(player, SwordType.FLAMEBLADE.id());

        assertDoesNotThrow(() -> controller.activate(player, SwordType.FLAMEBLADE, true));
        advanceTicks(120);

        assertEquals(0, charge.value());
        verify(combat, atLeastOnce()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
    }

    @Test
    void failedRitualCannotPreventUltimateDamage() {
        doThrow(new IllegalStateException("simulated display failure")).when(cinematics).start(eq(player), eq(SwordType.FLAMEBLADE), any(Runnable.class));

        assertDoesNotThrow(() -> controller.activate(player, SwordType.FLAMEBLADE, true));
        advanceTicks(120);

        assertEquals(0, charge.value());
        verify(combat, atLeastOnce()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
    }

    @Test
    void insufficientChargeDoesNotConsumeChargeOrStartACooldown() {
        charge.set(99);
        int initialTasks = scheduled.size();

        controller.activate(player, SwordType.FLAMEBLADE, true);

        assertEquals(99, charge.value());
        assertEquals(0, cooldowns.remainingMillis(player.getUniqueId(), SwordType.FLAMEBLADE.id() + ":ultimate"));
        assertEquals(initialTasks, scheduled.size(), "The existing display cleanup timer is the only scheduled task");
        verifyNoInteractions(animations, combat);
    }

    @Test
    void cooldownRejectionKeepsARefilledChargeMeterAndDoesNotScheduleMoreDamage() {
        controller.activate(player, SwordType.FLAMEBLADE, true);
        int activeTasks = scheduled.size();
        charge.set(100);

        controller.activate(player, SwordType.FLAMEBLADE, true);

        assertEquals(100, charge.value());
        assertEquals(activeTasks, scheduled.size());
    }

    private void advanceTicks(int duration) {
        for (int tick = 0; tick <= duration; tick++) {
            for (Scheduled frame : List.copyOf(scheduled)) {
                if (!frame.cancelled && frame.nextTick <= tick) {
                    frame.runnable.run();
                    frame.nextTick += frame.period;
                }
            }
        }
    }
}
