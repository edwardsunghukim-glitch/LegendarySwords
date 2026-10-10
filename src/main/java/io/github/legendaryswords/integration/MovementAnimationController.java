package io.github.legendaryswords.integration;

import io.github.legendaryswords.sword.SwordCatalog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Selects locomotion continuously while a legendary blade is in the main hand. */
public final class MovementAnimationController implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final AnimationBridge animations;
    private final SwordCatalog swords;
    private final Map<UUID, Sample> samples = new HashMap<>();
    private final BukkitTask task;

    public MovementAnimationController(JavaPlugin plugin, AnimationBridge animations, SwordCatalog swords) {
        this.plugin = plugin;
        this.animations = animations;
        this.swords = swords;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            var sword = swords.identify(player.getInventory().getItemInMainHand());
            if (sword.isEmpty() || !player.hasPermission("legendaryswords.use") || player.isDead()
                    || player.getGameMode() == GameMode.SPECTATOR || player.isFlying()
                    || player.isGliding() || player.isSwimming() || player.isInsideVehicle()
                    || player.isInvisible()) {
                release(player);
                continue;
            }
            Location current = player.getLocation();
            Sample previous = samples.get(id);
            double distanceSquared = previous == null || previous.location().getWorld() != current.getWorld()
                    ? 0D : horizontalDistanceSquared(previous.location(), current);
            int stillTicks = distanceSquared < 0.0004D
                    ? previous == null ? 2 : previous.stillTicks() + 2 : 0;
            AnimationBridge.Locomotion movement = select(distanceSquared, player.isSprinting(), stillTicks,
                    previous == null ? AnimationBridge.Locomotion.IDLE : previous.movement());
            samples.put(id, new Sample(current, movement, stillTicks));
            animations.tickMovement(player, sword.orElseThrow().id(), movement);
        }
    }

    /** Two samples of stillness prevent a held pose flashing between network movement updates. */
    static AnimationBridge.Locomotion select(double distanceSquared, boolean sprinting, int stillTicks,
                                             AnimationBridge.Locomotion previous) {
        // Ignore server/plugin teleports: they should not play a sprint cycle.
        if (!Double.isFinite(distanceSquared) || distanceSquared > 16D) return AnimationBridge.Locomotion.IDLE;
        if (distanceSquared >= 0.0004D) return sprinting
                ? AnimationBridge.Locomotion.RUN : AnimationBridge.Locomotion.WALK;
        return stillTicks >= 4 ? AnimationBridge.Locomotion.IDLE : previous;
    }

    private static double horizontalDistanceSquared(Location a, Location b) {
        double x = a.getX() - b.getX(), z = a.getZ() - b.getZ();
        return x * x + z * z;
    }

    private void release(Player player) {
        samples.remove(player.getUniqueId());
        // An attack can create its tracker before this controller's first sample.
        animations.stopMovement(player);
    }

    @EventHandler public void quit(PlayerQuitEvent event) { release(event.getPlayer()); }
    @EventHandler public void died(PlayerDeathEvent event) { release(event.getEntity()); }
    @EventHandler public void worldChanged(PlayerChangedWorldEvent event) { release(event.getPlayer()); }

    @Override public void close() {
        task.cancel();
        HandlerList.unregisterAll(this);
        plugin.getServer().getOnlinePlayers().forEach(animations::stopMovement);
        samples.clear();
    }

    private record Sample(Location location, AnimationBridge.Locomotion movement, int stillTicks) { }
}
