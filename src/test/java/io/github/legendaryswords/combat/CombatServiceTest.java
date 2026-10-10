package io.github.legendaryswords.combat;

import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.mechanics.ChargeMeter;
import io.github.legendaryswords.sword.SwordCatalog;
import io.github.legendaryswords.sword.SwordType;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Service-level checks with mocked Bukkit objects; these do not run a server. */
class CombatServiceTest {
    private PluginSettings settings;
    private SwordCatalog swords;
    private PlayerStateStore states;
    private CombatService service;
    private Player owner;
    private LivingEntity target;
    private ChargeMeter charge;

    @BeforeEach
    void setUp() {
        settings = mock(PluginSettings.class);
        swords = mock(SwordCatalog.class);
        states = mock(PlayerStateStore.class);
        owner = mock(Player.class);
        target = mock(LivingEntity.class);
        charge = new ChargeMeter(0);
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(owner.isOnline()).thenReturn(true);
        when(owner.hasPermission("legendaryswords.use")).thenReturn(true);
        when(settings.chargePerHit()).thenReturn(10);
        when(settings.killBonus()).thenReturn(20);
        when(states.charge(ownerId)).thenReturn(charge);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack sword = mock(ItemStack.class);
        when(owner.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(sword);
        when(swords.identify(sword)).thenReturn(Optional.of(SwordType.MASTER_SWORD));
        service = new CombatService(settings, swords, states, event -> {});
    }

    @Test
    void cancelledAbilityDamageCannotApplyFireKnockbackOrEarnCharge() {
        var cancelled = damageEvent(target, true, 8, DamageCause.ENTITY_ATTACK);
        doAnswer(invocation -> {
            service.observeDamage(cancelled);
            return null;
        }).when(target).damage(8, owner);

        assertFalse(service.damage(owner, target, 8, new Vector(1, 0, 0), 80));

        verify(target).damage(8, owner);
        verify(target, never()).setFireTicks(anyInt());
        verify(target, never()).setVelocity(any(Vector.class));
        verifyNoInteractions(states);
        assertEquals(0, charge.value());
    }

    @Test
    void acceptedAbilityDamageAppliesEffectsWithoutFeedingMeleeCharge() {
        var accepted = damageEvent(target, false, 8, DamageCause.ENTITY_ATTACK);
        when(target.getVelocity()).thenReturn(new Vector(0, 0.5, 0));
        when(target.getFireTicks()).thenReturn(100);
        doAnswer(invocation -> {
            service.observeDamage(accepted);
            return null;
        }).when(target).damage(8, owner);

        assertTrue(service.damage(owner, target, 8, new Vector(1, 0, 0), 80));

        verify(target).setFireTicks(100);
        verify(target).setVelocity(new Vector(1, 0.5, 0));
        verifyNoInteractions(states);
        assertEquals(0, charge.value());
    }

    @Test
    void cancellingIgnitionPreservesAcceptedDamageAndKnockbackWithoutStartingFire() {
        AtomicInteger ignitionEvents = new AtomicInteger();
        service = new CombatService(settings, swords, states, event -> {
            ignitionEvents.incrementAndGet();
            assertSame(owner, event.getCombuster());
            assertSame(target, event.getEntity());
            assertEquals(4.0f, event.getDuration());
            event.setCancelled(true);
        });
        var accepted = damageEvent(target, false, 8, DamageCause.ENTITY_ATTACK);
        when(target.getVelocity()).thenReturn(new Vector());
        doAnswer(invocation -> {
            service.observeDamage(accepted);
            return null;
        }).when(target).damage(8, owner);

        assertTrue(service.damage(owner, target, 8, new Vector(1, 0, 0), 80));

        assertEquals(1, ignitionEvents.get());
        verify(target).damage(8, owner);
        verify(target, never()).setFireTicks(anyInt());
        verify(target).setVelocity(new Vector(1, 0, 0));
        verifyNoInteractions(states);
    }

    @Test
    void acceptedIgnitionUsesTheDurationSelectedByProtectionPlugins() {
        AtomicInteger ignitionEvents = new AtomicInteger();
        service = new CombatService(settings, swords, states, event -> {
            ignitionEvents.incrementAndGet();
            event.setDuration(1.5f);
        });
        var accepted = damageEvent(target, false, 8, DamageCause.ENTITY_ATTACK);
        doAnswer(invocation -> {
            service.observeDamage(accepted);
            return null;
        }).when(target).damage(8, owner);

        assertTrue(service.damage(owner, target, 8, null, 80));

        assertEquals(1, ignitionEvents.get());
        verify(target).setFireTicks(30);
    }

    @Test
    void anAbilityKillCannotRecursivelyAwardTheMeleeKillBonus() {
        var accepted = damageEvent(target, false, 8, DamageCause.ENTITY_ATTACK);
        var death = mock(EntityDeathEvent.class);
        when(death.getEntity()).thenReturn(target);
        when(target.getKiller()).thenReturn(owner);
        when(target.getLastDamageCause()).thenReturn(accepted);
        doAnswer(invocation -> {
            service.observeDamage(accepted);
            service.observeKill(death);
            return null;
        }).when(target).damage(8, owner);

        assertTrue(service.damage(owner, target, 8, null, 0));

        verifyNoInteractions(states);
        assertEquals(0, charge.value());
    }

    @Test
    void onlySuccessfulNormalMeleeHitsAndFinalMeleeKillsEarnCharge() {
        service.observeDamage(damageEvent(target, true, 8, DamageCause.ENTITY_ATTACK));
        service.observeDamage(damageEvent(target, false, 0, DamageCause.ENTITY_ATTACK));
        service.observeDamage(damageEvent(target, false, 8, DamageCause.PROJECTILE));
        assertEquals(0, charge.value());

        var melee = damageEvent(target, false, 8, DamageCause.ENTITY_ATTACK);
        service.observeDamage(melee);
        assertEquals(10, charge.value());

        var death = mock(EntityDeathEvent.class);
        when(death.getEntity()).thenReturn(target);
        when(target.getKiller()).thenReturn(owner);
        var projectile = damageEvent(target, false, 8, DamageCause.PROJECTILE);
        when(target.getLastDamageCause()).thenReturn(projectile);
        service.observeKill(death);
        assertEquals(10, charge.value(), "Old killer attribution must not reward a projectile kill");

        when(target.getLastDamageCause()).thenReturn(melee);
        service.observeKill(death);
        assertEquals(30, charge.value());
    }

    @Test
    void selfInvulnerableTargetsArmorStandsAndOwnedPetsAreProtected() {
        assertFalse(service.eligible(owner, owner));
        assertFalse(service.eligible(owner, mock(ArmorStand.class)));
        when(target.isInvulnerable()).thenReturn(true);
        assertFalse(service.eligible(owner, target));

        Tameable pet = mock(Tameable.class);
        when(pet.getOwner()).thenReturn(owner);
        assertFalse(service.eligible(owner, pet));

        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        when(pet.getOwner()).thenReturn(other);
        assertTrue(service.eligible(owner, pet));
    }

    @Test
    void pvpRequiresPluginAndWorldPermissionAndRespectsFriendlyFire() {
        Player victim = mock(Player.class);
        when(victim.getGameMode()).thenReturn(GameMode.SURVIVAL);
        assertFalse(service.eligible(owner, victim), "Plugin defaults must block PvP");

        World world = mock(World.class);
        when(owner.getWorld()).thenReturn(world);
        when(settings.pvpEnabled()).thenReturn(true);
        assertFalse(service.eligible(owner, victim), "A world with PvP disabled must stay protected");

        when(world.getPVP()).thenReturn(true);
        Scoreboard board = mock(Scoreboard.class);
        when(owner.getScoreboard()).thenReturn(board);
        when(owner.getName()).thenReturn("Alice");
        when(victim.getName()).thenReturn("Bob");
        assertTrue(service.eligible(owner, victim));

        Team team = mock(Team.class);
        when(board.getEntryTeam("Alice")).thenReturn(team);
        when(team.hasEntry("Bob")).thenReturn(true);
        assertFalse(service.eligible(owner, victim));
        when(team.allowFriendlyFire()).thenReturn(true);
        assertTrue(service.eligible(owner, victim));

        when(victim.getGameMode()).thenReturn(GameMode.CREATIVE);
        assertFalse(service.eligible(owner, victim));
        when(victim.getGameMode()).thenReturn(GameMode.SPECTATOR);
        assertFalse(service.eligible(owner, victim));
    }

    @Test
    void deniedMeleeTargetsAreCancelledBeforeChargeCanBeAwarded() {
        Player victim = mock(Player.class);
        var melee = damageEvent(victim, false, 8, DamageCause.ENTITY_ATTACK);

        service.protectMelee(melee);

        verify(melee).setCancelled(true);
        verifyNoInteractions(states);
    }

    private EntityDamageByEntityEvent damageEvent(LivingEntity entity, boolean cancelled, double damage, DamageCause cause) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(owner);
        when(event.getEntity()).thenReturn(entity);
        when(event.isCancelled()).thenReturn(cancelled);
        when(event.getFinalDamage()).thenReturn(damage);
        when(event.getCause()).thenReturn(cause);
        return event;
    }
}
