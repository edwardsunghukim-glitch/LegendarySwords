package io.github.legendaryswords.combat;

import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.PlayerStateStore;
import io.github.legendaryswords.sword.SwordCatalog;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

/** Routes damage through Bukkit so protection plugins can cancel it. */
public final class CombatService {
    private static final class Attempt {
        final Player owner;
        final LivingEntity target;
        boolean accepted;
        Attempt(Player owner, LivingEntity target) { this.owner = owner; this.target = target; }
    }
    private final Deque<Attempt> attempts = new ArrayDeque<>();
    private final PluginSettings settings;
    private final SwordCatalog swords;
    private final PlayerStateStore states;
    private final Consumer<EntityCombustByEntityEvent> combustDispatcher;
    public CombatService(PluginSettings settings, SwordCatalog swords, PlayerStateStore states) {
        this(settings, swords, states, event -> event.callEvent());
    }
    public CombatService(PluginSettings settings, SwordCatalog swords, PlayerStateStore states,
                         Consumer<EntityCombustByEntityEvent> combustDispatcher) {
        this.settings = settings; this.swords = swords; this.states = states;
        this.combustDispatcher = combustDispatcher;
    }
    public boolean eligible(Player actor, LivingEntity target) {
        if(target.hasMetadata(BossHitboxes.MARKER)) {
            Player owner=BossHitboxes.owner(target);return owner!=null&&eligible(actor,owner);
        }
        if (actor.equals(target) || target instanceof ArmorStand || target.isDead() || target.isInvulnerable()) return false;
        if (target instanceof Tameable pet && pet.getOwner() != null && pet.getOwner().getUniqueId().equals(actor.getUniqueId())) return false;
        if (target instanceof Player player) {
            if (!settings.pvpEnabled() || !actor.getWorld().getPVP()
                    || player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) return false;
            var team = actor.getScoreboard().getEntryTeam(actor.getName());
            if (team != null && team.hasEntry(player.getName()) && !team.allowFriendlyFire()) return false;
        }
        return true;
    }
    public boolean visible(Location origin, LivingEntity target) {
        Location destination = target.getLocation().add(0, target.getHeight() * 0.5, 0);
        if (!origin.getWorld().equals(destination.getWorld())) return false;
        Vector delta = destination.toVector().subtract(origin.toVector());
        double distance = delta.length();
        if (distance < 0.05) return true;
        return origin.getWorld().rayTraceBlocks(origin, delta.normalize(), distance, FluidCollisionMode.NEVER, true) == null;
    }
    public boolean damage(Player owner, LivingEntity target, double amount, Vector push, int fireTicks) {
        if (!owner.isOnline() || owner.isDead() || amount <= 0 || !eligible(owner, target)) return false;
        if(target.hasMetadata(BossHitboxes.MARKER)) {
            Player recipient=BossHitboxes.owner(target);
            if(recipient==null||!BossHitboxes.claimHit(target,owner))return false;
            target=recipient;
        }
        Attempt attempt = new Attempt(owner, target);
        attempts.push(attempt);
        // Paper applies armor, armor toughness, enchantments and resistance here.
        // Do not pre-reduce the amount: that would apply armor twice.
        try { target.damage(amount, owner); }
        finally { attempts.pop(); }
        if (!attempt.accepted || target.isDead()) return attempt.accepted;
        if (fireTicks > 0) {
            var combustion = new EntityCombustByEntityEvent(owner, target, fireTicks / 20.0f);
            combustDispatcher.accept(combustion);
            if (!combustion.isCancelled() && Float.isFinite(combustion.getDuration()) && combustion.getDuration() > 0) {
                int ticks = (int) Math.min(Integer.MAX_VALUE, Math.ceil(combustion.getDuration() * 20.0));
                target.setFireTicks(Math.max(target.getFireTicks(), ticks));
            }
        }
        if (push != null) target.setVelocity(target.getVelocity().add(push));
        return true;
    }
    public void protectMelee(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity target
                && swords.identify(player.getInventory().getItemInMainHand()).isPresent() && !eligible(player, target)) event.setCancelled(true);
    }
    public void observeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)
                || event.isCancelled() || event.getFinalDamage() <= 0) return;
        Attempt attempt = attempts.peek();
        if (attempt != null && attempt.owner.equals(player) && attempt.target.equals(target)) {
            attempt.accepted = true;
            return;
        }
        if (!attempts.isEmpty() || !eligible(player, target)
                || (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) return;
        if (swords.identify(player.getInventory().getItemInMainHand()).isPresent() && player.hasPermission("legendaryswords.use")) {
            states.charge(player.getUniqueId()).add(settings.chargePerHit());
        }
    }
    public void observeKill(EntityDeathEvent event) {
        if (!attempts.isEmpty()) return;
        Player player = event.getEntity().getKiller();
        // Only a final normal melee strike grants the bonus, not old attribution or ability kills.
        var last = event.getEntity().getLastDamageCause();
        if (player != null && player.hasPermission("legendaryswords.use") && last instanceof EntityDamageByEntityEvent hit
                && hit.getDamager().equals(player) && !hit.isCancelled()
                && (hit.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || hit.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)
                && swords.identify(player.getInventory().getItemInMainHand()).isPresent()) states.charge(player.getUniqueId()).add(settings.killBonus());
    }
}
