package io.github.legendaryswords.integration;

import io.github.legendaryswords.sword.SwordCatalog;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;

/** Owns the temporary item model used while a BetterModel body holds a sword. */
public final class AnimatedSwordItems implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final SwordCatalog swords;
    private final Map<UUID, Integer> pendingUpdates = new ConcurrentHashMap<>();

    public AnimatedSwordItems(JavaPlugin plugin, SwordCatalog swords) {
        this.plugin = plugin;
        this.swords = swords;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getOnlinePlayers().forEach(this::restorePlayer);
    }

    /** Selects an animated model only for a recognized sword with our own item model. */
    public boolean begin(Player player, String swordId) {
        if (pendingUpdates.containsKey(player.getUniqueId())) return false;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!owned(item, swordId)) return false;
        var meta = item.getItemMeta();
        NamespacedKey animated = animated(swordId);
        if (!animated.equals(meta.getItemModel())) {
            // A Craft inventory stack can share its native handle with queued equipment packets.
            // Finish all component changes on a detached copy before replacing the inventory slot.
            item = item.clone();
            meta = item.getItemMeta();
            meta.setItemModel(animated);
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
        }
        return true;
    }

    /** Main-thread capture of a detached display; inventory cleanup cannot change this snapshot. */
    public ItemStack displayItem(Player player, String swordId) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!owned(held, swordId)) return new ItemStack(Material.AIR);
        ItemStack display = held.clone();
        var meta = display.getItemMeta();
        meta.setItemModel(animated(swordId));
        display.setItemMeta(meta);
        return display;
    }

    private boolean owned(ItemStack item, String swordId) {
        return swords.identify(item).filter(sword -> sword.id().equals(swordId)).isPresent()
                && item.hasItemMeta() && (base(swordId).equals(item.getItemMeta().getItemModel())
                || animated(swordId).equals(item.getItemMeta().getItemModel()));
    }

    /** Restores a detached visual clone in place. Inventory and event stacks must use restoredCopy. */
    public boolean restoreItem(ItemStack item) {
        NamespacedKey model = restorationModel(item);
        if (model == null) return false;
        var meta = item.getItemMeta();
        meta.setItemModel(model);
        item.setItemMeta(meta);
        return true;
    }

    private NamespacedKey restorationModel(ItemStack item) {
        var sword = swords.identify(item);
        if (sword.isEmpty() || !item.hasItemMeta()) return null;
        String id = sword.orElseThrow().id();
        return animated(id).equals(item.getItemMeta().getItemModel()) ? base(id) : null;
    }

    private ItemStack restoredCopy(ItemStack item) {
        NamespacedKey model = restorationModel(item);
        if (model == null) return null;
        ItemStack copy = item.clone();
        var meta = copy.getItemMeta();
        meta.setItemModel(model);
        copy.setItemMeta(meta);
        return copy;
    }

    public void restorePlayer(Player player) {
        var inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack restored = restoredCopy(contents[slot]);
            if (restored != null) inventory.setItem(slot, restored);
        }
        ItemStack cursor = restoredCopy(player.getItemOnCursor());
        if (cursor != null) player.setItemOnCursor(cursor);
    }

    /** BetterModel close callbacks can arrive on its worker threads. */
    public void restorePlayerSoon(UUID id) {
        Runnable restore = () -> {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) restorePlayer(player);
        };
        if (plugin.getServer().isPrimaryThread()) restore.run();
        else if (plugin.isEnabled()) queueCleanup(id, restore);
    }

    private void queueCleanup(UUID id, Runnable cleanup) {
        pendingUpdates.merge(id, 1, Integer::sum);
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try { cleanup.run(); }
                finally { finishCleanup(id); }
            });
        } catch (RuntimeException error) {
            finishCleanup(id);
            throw error;
        }
    }

    private void finishCleanup(UUID id) {
        pendingUpdates.computeIfPresent(id, (owner, count) -> count > 1 ? count - 1 : null);
    }

    private void afterInventoryUpdate(Player player, InventoryView view, Collection<Integer> rawSlots) {
        if (!plugin.isEnabled()) return;
        queueCleanup(player.getUniqueId(), () -> {
            // Paper applies drag destinations after the event; modifying event snapshots is insufficient.
            for (int slot : rawSlots) {
                if (slot < 0 || slot >= view.countSlots()) continue;
                ItemStack item = restoredCopy(view.getItem(slot));
                if (item != null) view.setItem(slot, item);
            }
            restorePlayer(player);
        });
    }

    private static NamespacedKey base(String id) { return new NamespacedKey("legendaryswords", id); }
    private static NamespacedKey animated(String id) { return new NamespacedKey("legendaryswords", id + "_animated"); }

    @EventHandler public void join(PlayerJoinEvent event) { restorePlayer(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) { restorePlayer(event.getPlayer()); }
    @EventHandler public void held(PlayerItemHeldEvent event) { restorePlayer(event.getPlayer()); }
    @EventHandler public void swapped(PlayerSwapHandItemsEvent event) {
        restorePlayer(event.getPlayer());
        ItemStack main = restoredCopy(event.getMainHandItem()), off = restoredCopy(event.getOffHandItem());
        if (main != null) event.setMainHandItem(main);
        if (off != null) event.setOffHandItem(off);
    }
    @EventHandler public void dropped(PlayerDropItemEvent event) {
        ItemStack item = restoredCopy(event.getItemDrop().getItemStack());
        if (item != null) event.getItemDrop().setItemStack(item);
        restorePlayer(event.getPlayer());
    }
    @EventHandler public void pickedUp(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            ItemStack item = restoredCopy(event.getItem().getItemStack());
            if (item != null) event.getItem().setItemStack(item);
        }
    }
    @EventHandler public void died(PlayerDeathEvent event) {
        var drops = event.getDrops().listIterator();
        while (drops.hasNext()) {
            ItemStack item = restoredCopy(drops.next());
            if (item != null) drops.set(item);
        }
        restorePlayer(event.getEntity());
    }
    @EventHandler public void clicked(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event instanceof InventoryCreativeEvent creative) {
                ItemStack cursor = restoredCopy(creative.getCursor());
                if (cursor != null) creative.setCursor(cursor);
            }
            ItemStack item = restoredCopy(event.getCurrentItem());
            if (item != null) event.setCurrentItem(item);
            restorePlayer(player);
            afterInventoryUpdate(player, event.getView(), List.of(event.getRawSlot()));
        }
    }
    @EventHandler public void dragged(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack cursor = restoredCopy(event.getCursor());
            if (cursor != null) event.setCursor(cursor);
            restorePlayer(player);
            afterInventoryUpdate(player, event.getView(), Set.copyOf(event.getRawSlots()));
        }
    }

    @Override public void close() {
        plugin.getServer().getOnlinePlayers().forEach(this::restorePlayer);
        pendingUpdates.clear();
        HandlerList.unregisterAll(this);
    }
}
