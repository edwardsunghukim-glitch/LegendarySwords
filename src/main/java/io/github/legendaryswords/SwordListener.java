package io.github.legendaryswords;

import java.time.Duration;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class SwordListener implements Listener {
    private final LegendarySwordsPlugin plugin;
    public SwordListener(LegendarySwordsPlugin plugin) { this.plugin = plugin; }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        // Air interactions with a sword can start pre-cancelled because vanilla has no use action.
        // Respect an explicit block-use denial; damage still goes through protection events.
        if (event.getClickedBlock() != null && event.useInteractedBlock() == Event.Result.DENY) return;
        plugin.swords().identify(event.getItem()).ifPresent(sword -> {
            if (!event.getPlayer().hasPermission("legendaryswords.use")) return;
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            plugin.abilities().activate(event.getPlayer(), sword, event.getPlayer().isSneaking());
        });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING || plugin.cinematics().active(event.getPlayer())
                || !event.getPlayer().hasPermission("legendaryswords.use")) return;
        plugin.swords().identify(event.getPlayer().getInventory().getItemInMainHand()).ifPresent(sword -> {
            if (!plugin.cooldowns().tryUse(event.getPlayer().getUniqueId(), "swing", Duration.ofMillis(300))) return;
            plugin.animations().playSwing(event.getPlayer(), sword.id());
            if (plugin.settings().glowParticles()) plugin.effects().arc(event.getPlayer().getEyeLocation().subtract(0, 0.3, 0),
                    event.getPlayer().getEyeLocation().getDirection(), 1.4, 50, sword);
        });
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageProtection(EntityDamageByEntityEvent event) { plugin.combat().protectMelee(event); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) { plugin.combat().observeDamage(event); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) { plugin.combat().observeKill(event); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) { plugin.cinematics().cancel(event.getEntity()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.cinematics().cancel(event.getPlayer());
        plugin.states().unload(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) { plugin.cinematics().cancel(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE && plugin.cinematics().active(event.getPlayer())) {
            plugin.cinematics().cancelForTeleport(event.getPlayer());
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.cinematics().recover(event.getPlayer());
        if (!plugin.packs().configured() || !plugin.getConfig().getBoolean("resource-pack.send-on-join", true)) return;
        long delay = Math.clamp(plugin.getConfig().getLong("resource-pack.join-delay-ticks", 60), 1, 600);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline() && plugin.packs().configured()) plugin.packs().send(event.getPlayer());
        }, delay);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackResult(PlayerResourcePackStatusEvent event) {
        if (!ResourcePackManager.PACK_ID.equals(event.getID())) return;
        String status = event.getStatus().name();
        if (status.startsWith("FAILED") || status.equals("INVALID_URL")) {
            event.getPlayer().sendMessage(Component.text("Sword pack could not load. Ask an admin to check the direct ZIP URL and SHA-1.", NamedTextColor.RED));
        }
    }
}
