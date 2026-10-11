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
    private MockedStatic<io.papermc.paper.registry.RegistryAccess> registryApi;

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

        when(plugin.getConfig()).thenReturn(new org.bukkit.configuration.file.YamlConfiguration());
        plugin.getConfig().set("bosses.duration-seconds", 8);
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
        when(cinematics.durationTicks(any(SwordType.class))).thenReturn(64);
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
            doAnswer(call -> { frame.cancelled = true; return null; }).when(task).cancel();
            return task;
        });
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            int id = scheduled.size() + 1;
            Scheduled frame = new Scheduled(id, invocation.getArgument(1), invocation.getArgument(2), 0);
            scheduled.add(frame);
            BukkitTask task = mock(BukkitTask.class);
            when(task.getTaskId()).thenReturn(id);
            doAnswer(call -> { frame.cancelled = true; return null; }).when(task).cancel();
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
        registryApi = mockStatic(io.papermc.paper.registry.RegistryAccess.class);
        var access = mock(io.papermc.paper.registry.RegistryAccess.class);
        when(access.getRegistry(any(Class.class))).thenAnswer(call -> mock(org.bukkit.Registry.class));
        when(access.getRegistry(any(io.papermc.paper.registry.RegistryKey.class))).thenAnswer(call -> mock(org.bukkit.Registry.class));
        registryApi.when(io.papermc.paper.registry.RegistryAccess::registryAccess).thenReturn(access);
        doAnswer(call -> mock(org.bukkit.potion.PotionEffectType.class)).when(org.bukkit.Registry.MOB_EFFECT)
                .getOrThrow(any(net.kyori.adventure.key.Key.class));
        controller = new AbilityController(plugin, settings, states, cooldowns, combat, effects, cinematics, animations);
    }

    @AfterEach
    void tearDown() {
        if (controller != null) controller.close();
        if (bukkit != null) bukkit.close();
        if (registryApi != null) registryApi.close();
    }

    @Test void bossFlightIsGrantedOnlyAfterWindupAndRestoredOnExpiry() {
        controller.activate(player, SwordType.ANGEL_SOVEREIGN, true);
        advanceTicks(30);
        verify(player, never()).setAllowFlight(anyBoolean());
        advanceTicks(90);
        verify(animations).startBoss(player, "angel_sovereign");
        verify(player).setAllowFlight(true); verify(player).setFlying(true);
        advanceTicks(180);
        verify(player).setAllowFlight(false); verify(player).setFlying(false);
        verify(animations).stopBoss(player);
        assertFalse(controller.bossSwing(player));
    }
    @Test void dragonTransformationGrantsAndRestoresFlight() {
        controller.activate(player,SwordType.INFERNAL_DRAGON,true);advanceTicks(120);
        verify(player).setAllowFlight(true);verify(player).setFlying(true);
        advanceTicks(180);verify(player).setAllowFlight(false);verify(player).setFlying(false);
    }

    @Test void endingBossKeepsFlightWhenFlyingArmorIsStillEquipped() {
        when(player.getAllowFlight()).thenReturn(true);when(player.hasMetadata("legendaryarmor_flight")).thenReturn(true);
        var baseline=mock(org.bukkit.metadata.MetadataValue.class);when(baseline.value()).thenReturn(java.util.Map.of("allowed",false,"flying",false));
        when(player.getMetadata("legendaryarmor_flight")).thenReturn(java.util.List.of(baseline));
        controller.activate(player,SwordType.INFERNAL_DRAGON,true);advanceTicks(300);
        verify(player,never()).setAllowFlight(false);
    }
    @Test void armorRemovedDuringBossRestoresPreArmorFlightAtExpiry() {
        when(player.getAllowFlight()).thenReturn(true);
        var baseline=mock(org.bukkit.metadata.MetadataValue.class);when(baseline.value()).thenReturn(java.util.Map.of("allowed",false,"flying",false));
        when(player.getMetadata("legendaryarmor_flight")).thenReturn(java.util.List.of(baseline));
        controller.activate(player,SwordType.INFERNAL_DRAGON,true);advanceTicks(300);
        verify(player).setAllowFlight(false);verify(player).setFlying(false);
    }

    @Test void bossCancellationRestoresFlightAndStopsFurtherDamage() {
        controller.activate(player, SwordType.SKY_LEVIATHAN, true);
        advanceTicks(80);
        controller.cancel(player);
        verify(player).setAllowFlight(false); verify(animations).stopBoss(player);
        clearInvocations(combat);
        advanceTicks(220);
        verify(combat, never()).damage(any(), any(), anyDouble(), any(), anyInt());
    }
    @Test void bossDoesNotRemovePreexistingFlight() {
        when(player.getAllowFlight()).thenReturn(true); when(player.isFlying()).thenReturn(true);
        controller.activate(player, SwordType.ANGEL_SOVEREIGN, true);
        advanceTicks(270);
        verify(player, times(2)).setAllowFlight(true); verify(player, times(2)).setFlying(true);
        verify(player, never()).setAllowFlight(false);
    }
    @Test void bossFlightCanBeDisabledWithoutRemovingTheMorphOrAttacks() {
        plugin.getConfig().set("bosses.flight-enabled", false);
        controller.activate(player, SwordType.ANGEL_SOVEREIGN, true);
        advanceTicks(270);
        verify(player, never()).setAllowFlight(anyBoolean());
        verify(animations).startBoss(player, "angel_sovereign"); verify(animations, times(1)).bossSmash(player);
    }
    @Test void bossFlightRestorationRespectsChangesToCreativeMode() {
        controller.activate(player, SwordType.ANGEL_SOVEREIGN, true);
        advanceTicks(80); when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        controller.cancel(player);
        verify(player, never()).setAllowFlight(false);
    }
    @Test void temporaryBossFlightDoesNotLeakWhenChangingToAdventureMode() {
        controller.activate(player, SwordType.ANGEL_SOVEREIGN, true);
        advanceTicks(80); when(player.getGameMode()).thenReturn(GameMode.ADVENTURE);
        controller.cancel(player);
        verify(player).setAllowFlight(false); verify(player).setFlying(false);
    }
    @Test void transformedSwingsRespectTheirCooldown() {
        when(target.getLocation()).thenAnswer(call -> new Location(world, 10, 70, 23));
        controller.activate(player, SwordType.GRANITE_COLOSSUS, true);
        advanceTicks(70);
        for (int i=0; i<200; i++) assertTrue(controller.bossSwing(player));
        advanceTicks(210);
        verify(combat, atMost(3)).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
        verify(animations, times(1)).bossSmash(player);
    }
    @Test void defaultBossFormLastsFiveMinutesAndThenRestoresThePlayer() {
        plugin.getConfig().set("bosses.duration-seconds", null);
        controller.activate(player, SwordType.ANGEL_SOVEREIGN, true);
        advanceTicks(80);
        assertTrue(controller.bossStatus(player).startsWith("Boss 5:00"));
        advanceTicks(1000);
        assertTrue(controller.bossStatus(player).startsWith("Boss 4:"));
        verify(animations, never()).stopBoss(player);
        advanceTicks(6300);
        assertNull(controller.bossStatus(player));
        verify(animations).stopBoss(player); verify(player).setAllowFlight(false);
    }
    @Test void longBossFormsAllowNewAttacksAfterMoreThanThreeUsesWithoutRecharging() {
        plugin.getConfig().set("bosses.duration-seconds", 300);
        controller.activate(player, SwordType.GRANITE_COLOSSUS, true);
        advanceTicks(100);
        for (int step=1; step<=5; step++) { advanceTicks(100+step*80); assertTrue(controller.bossSwing(player)); }
        verify(animations, atLeast(5)).bossSmash(player);
        assertEquals(0, charge.value()); assertNotNull(controller.bossStatus(player));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=SwordType.class, names={"ANGEL_SOVEREIGN","GRANITE_COLOSSUS","SKY_LEVIATHAN","INFERNAL_DRAGON","FROST_TITAN"})
    void eachFormUnlocksASecondarySkillWithItsOwnCooldown(SwordType sword) {
        var manager=mock(org.bukkit.plugin.PluginManager.class);
        when(plugin.getServer().getPluginManager()).thenReturn(manager);
        when(player.getMaxHealth()).thenReturn(20.0); when(player.getHealth()).thenReturn(12.0);
        plugin.getConfig().set("bosses.duration-seconds", 300);
        controller.activate(player, sword, true); advanceTicks(80);
        controller.activate(player, sword, true);
        controller.activate(player, sword, true);
        advanceTicks(140);
        verify(animations, times(1)).bossSpecial(player);
        assertEquals(0, charge.value()); assertNotNull(controller.bossStatus(player));
        if (sword==SwordType.ANGEL_SOVEREIGN) verify(player).setHealth(18.0);
        controller.cancel(player);
        assertNull(controller.bossStatus(player));
    }

    @Test void dragonBreathHitsAheadAndLeavesTargetsBehindUnharmed() {
        when(target.getLocation()).thenAnswer(call -> new Location(world, 10, 70, 16));
        controller.activate(player, SwordType.INFERNAL_DRAGON, false); advanceTicks(50);
        verify(combat, never()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
        verify(effects, atLeastOnce()).particle(any(Location.class), eq(org.bukkit.Particle.FLAME), anyInt(), anyDouble(), anyDouble());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = SwordType.class,
            names = {"MASTER_SWORD", "FLAMEBLADE", "FIERCE_DEITY_SWORD", "FROSTBLADE", "THUNDERBLADE", "ROYAL_CLAYMORE", "EIGHTFOLD_BLADE", "BIGGORON_SWORD"},
            mode = org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE)
    void everyNewRelicProducesEffectsAndProtectedAbilityAndUltimateDamage(SwordType sword) {
        // Put the target directly ahead, within both beam and area attack footprints.
        when(target.getLocation()).thenAnswer(invocation -> new Location(world, 10, 70, 23));
        controller.activate(player, sword, false);
        advanceTicks(110);
        verify(combat, atLeastOnce()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
        assertTrue(mockingDetails(effects).getInvocations().stream()
                .anyMatch(call -> java.util.Arrays.asList(call.getArguments()).contains(sword)));
        verify(combat, atMost(1)).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
        clearInvocations(combat, effects);
        // The thorn domain leaves its center safe; victims stand on its perimeter.
        if (sword == SwordType.BLOODTHORN) when(target.getLocation()).thenAnswer(invocation -> new Location(world, 14.5, 70, 23));
        charge.set(100);
        controller.activate(player, sword, true);
        advanceTicks(200);
        verify(animations).playRelease(player, sword.id());
        verify(combat, atLeastOnce()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
        verify(combat, atMost(3)).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
        assertEquals(0, charge.value());
        verify(player, never()).teleport(any(Location.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = SwordType.class, names = {"EMBER_HALO", "VOID_REAVER", "ASTRAL_EDGE", "TIDEBREAKER", "STORM_CAGE", "CHRONO_SABER", "ECLIPSE_FANG", "CRYSTAL_OATH", "BLOODTHORN", "SUNSPIRE", "MOONVEIL", "RUNIC_JUDGMENT", "NEBULA_BLADE", "OBSIDIAN_MAW", "SERAPH_WING", "ABYSSAL_TRIDENT", "MIRAGE_SABER", "METEOR_BRAND", "GLACIAL_CROWN", "TEMPEST_RAPIER", "GRAVITY_CLEAVER", "PRISM_SWORD", "THUNDER_DRUM", "WRAITH_SCYTHE", "VERDANT_OATH", "DRAGONHEART"})
    void domainsCannotDamageThroughWallsOrAfterCasterDeath(SwordType sword) {
        when(combat.visible(any(Location.class), eq(target))).thenReturn(false);
        controller.activate(player, sword, false);
        advanceTicks(20);
        verify(combat, never()).damage(any(), any(), anyDouble(), any(), anyInt());
        clearInvocations(effects);
        when(player.isDead()).thenReturn(true);
        advanceTicks(200);
        verifyNoInteractions(effects);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(doubles = {0, 12})
    void fireRimLeavesCenterAndOutsideSafe(double distance) {
        when(target.getLocation()).thenAnswer(invocation -> new Location(world, 10, 70, 20 + distance));
        controller.activate(player, SwordType.EMBER_HALO, false);
        advanceTicks(90);
        verify(combat, never()).damage(any(), any(), anyDouble(), any(), anyInt());
    }

    @Test void crystalMinesWaitUntilArmedBeforeDamage() {
        when(target.getLocation()).thenAnswer(invocation -> new Location(world, 10, 70, 23));
        controller.activate(player, SwordType.CRYSTAL_OATH, false);
        advanceTicks(10);
        verify(combat, never()).damage(any(), any(), anyDouble(), any(), anyInt());
        advanceTicks(90);
        verify(combat, times(1)).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
    }

    @Test void enlargedFireRimReachesSixBlocksAndBurnsTargets() {
        when(target.getLocation()).thenAnswer(invocation -> new Location(world, 10, 70, 26));
        controller.activate(player, SwordType.EMBER_HALO, false);
        advanceTicks(110);
        verify(combat, times(1)).damage(eq(player), eq(target), anyDouble(), any(Vector.class), eq(60));
        verify(effects, atLeastOnce()).particle(any(Location.class), eq(org.bukkit.Particle.FLAME), anyInt(), anyDouble(), anyDouble());
    }

    @Test void voidRingActuallyPullsVictimsTowardItsCenter() {
        when(target.getLocation()).thenAnswer(invocation -> new Location(world, 10, 70, 25));
        controller.activate(player, SwordType.VOID_REAVER, false);
        advanceTicks(100);
        var push = org.mockito.ArgumentCaptor.forClass(Vector.class);
        verify(combat).damage(eq(player), eq(target), anyDouble(), push.capture(), eq(0));
        assertTrue(push.getValue().getZ() < 0, "Void contraction pulls toward the focus");
        assertEquals(0, push.getValue().getY(), .001);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = SwordType.class, names = {"STORM_CAGE", "CHRONO_SABER", "BLOODTHORN", "SERAPH_WING", "TIDEBREAKER", "GLACIAL_CROWN", "VERDANT_OATH", "WRAITH_SCYTHE"})
    void signatureAccentsMatchEachWeaponTheme(SwordType sword) {
        org.bukkit.Particle expected = switch (sword) {
            case STORM_CAGE -> org.bukkit.Particle.ELECTRIC_SPARK;
            case CHRONO_SABER -> org.bukkit.Particle.ENCHANT;
            case BLOODTHORN -> org.bukkit.Particle.CRIMSON_SPORE;
            case SERAPH_WING -> org.bukkit.Particle.END_ROD;
            case TIDEBREAKER -> org.bukkit.Particle.SPLASH;
            case GLACIAL_CROWN -> org.bukkit.Particle.SNOWFLAKE;
            case VERDANT_OATH -> org.bukkit.Particle.HAPPY_VILLAGER;
            case WRAITH_SCYTHE -> org.bukkit.Particle.SOUL;
            default -> throw new AssertionError();
        };
        controller.activate(player, sword, false);
        advanceTicks(12);
        verify(effects, atLeastOnce()).particle(any(Location.class), eq(expected), anyInt(), anyDouble(), anyDouble());
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = SwordType.class, names = {"MASTER_SWORD", "WHITE_SWORD", "TRUE_MASTER_SWORD", "GUARDIAN_SWORD"})
    void beamsFollowVerticalAimFromEyeHeight(SwordType sword) {
        Location eye = new Location(world, 10, 71.62, 20, 0, -45);
        when(player.getEyeLocation()).thenReturn(eye);
        controller.activate(player, sword, false);
        advanceTicks(40);
        var starts = org.mockito.ArgumentCaptor.forClass(Location.class);
        var ends = org.mockito.ArgumentCaptor.forClass(Location.class);
        verify(effects, atLeastOnce()).line(starts.capture(), ends.capture(), eq(sword), anyDouble());
        for (int i = 0; i < starts.getAllValues().size(); i++) {
            Location start = starts.getAllValues().get(i), end = ends.getAllValues().get(i);
            assertEquals(eye.getY(), start.getY(), .001);
            assertTrue(end.getY() > start.getY(), "Upward aim must produce an upward beam");
        }
    }

    @Test void ultimateWaitsForTheSceneThenReleasesExactlyOnceEvenIfCompletionRepeats() {
        java.util.concurrent.atomic.AtomicReference<Runnable> completion = new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(call -> { completion.set(call.getArgument(2)); return null; })
                .when(cinematics).start(eq(player), eq(SwordType.FLAMEBLADE), any(Runnable.class));
        controller.activate(player, SwordType.FLAMEBLADE, true);
        advanceTicks(40);
        verifyNoInteractions(combat);
        assertEquals(0, charge.value());
        int beforeRelease = scheduled.size();
        completion.get().run();
        int afterRelease = scheduled.size();
        assertTrue(afterRelease > beforeRelease);
        completion.get().run();
        assertEquals(afterRelease, scheduled.size());
        advanceTicks(120);
        assertEquals(afterRelease, scheduled.size(), "The cancelled fallback cannot launch another ultimate");
        verify(combat, atLeastOnce()).damage(eq(player), eq(target), anyDouble(), any(Vector.class), anyInt());
    }

    @Test void cancelledOrClosedWindupsCannotDealDamageWhenLateSceneCallbacksArrive() {
        java.util.concurrent.atomic.AtomicReference<Runnable> completion = new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(call -> { completion.set(call.getArgument(2)); return null; })
                .when(cinematics).start(eq(player), eq(SwordType.FLAMEBLADE), any(Runnable.class));
        controller.activate(player, SwordType.FLAMEBLADE, true);
        controller.cancel(player);
        completion.get().run();
        advanceTicks(120);
        verifyNoInteractions(combat);

        charge.set(100);
        cooldowns.clear(player.getUniqueId());
        controller.activate(player, SwordType.FLAMEBLADE, true);
        controller.close();
        completion.get().run();
        advanceTicks(120);
        verifyNoInteractions(combat);
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
                    if (frame.period == 0) frame.cancelled = true;
                    else frame.nextTick += frame.period;
                }
            }
        }
    }
}
