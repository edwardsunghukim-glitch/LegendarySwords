package io.github.legendaryswords;

import io.github.legendaryswords.sword.SwordType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Public catalogue. Only a fresh admin permission check can create a playable copy. */
public final class SwordBrowser implements Listener {
    static final int PAGE_SIZE = 45;
    static final class View implements InventoryHolder {
        final UUID owner;
        final int page;
        Inventory inventory;
        View(UUID owner, int page) { this.owner = owner; this.page = page; }
        @Override public Inventory getInventory() { return inventory; }
    }
    private final LegendarySwordsPlugin plugin;
    public SwordBrowser(LegendarySwordsPlugin plugin) { this.plugin = plugin; }
    static int pages() { return (SwordType.values().length + PAGE_SIZE - 1) / PAGE_SIZE; }
    static SwordType selection(int page, int slot) {
        if (page < 0 || page >= pages() || slot < 0 || slot >= PAGE_SIZE) return null;
        int index = page * PAGE_SIZE + slot;
        return index < SwordType.values().length ? SwordType.values()[index] : null;
    }
    public void open(Player player, int requested) {
        int page = Math.clamp(requested, 0, pages() - 1);
        View view = new View(player.getUniqueId(), page);
        view.inventory = Bukkit.createInventory(view, 54,
                Component.text("Legendary Swords " + (page + 1) + "/" + pages()));
        boolean admin = player.hasPermission("legendaryswords.admin");
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            SwordType sword = selection(page, slot);
            if (sword == null) break;
            ItemStack icon = plugin.swords().create(sword);
            var meta = icon.getItemMeta();
            List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
            lore.add(Component.empty());
            lore.add(Component.text(admin ? "Click to receive this weapon" : "Browse only — admin required to receive",
                    admin ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            meta.lore(lore); icon.setItemMeta(meta); view.inventory.setItem(slot, icon);
        }
        if (page > 0) view.inventory.setItem(48, button(Material.ARROW, "Previous page"));
        view.inventory.setItem(49, button(Material.BOOK, SwordType.values().length + " weapons | " + (admin ? "Admin" : "Browse only")));
        if (page + 1 < pages()) view.inventory.setItem(50, button(Material.ARROW, "Next page"));
        player.openInventory(view.inventory);
    }
    private ItemStack button(Material material, String title) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta(); meta.displayName(Component.text(title, NamedTextColor.AQUA)); item.setItemMeta(meta);
        return item;
    }
    @EventHandler public void click(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof View view)) return;
        // Includes lower-inventory shift clicks, number keys, creative cloning, drops and double clicks.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !view.owner.equals(player.getUniqueId())) return;
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        int slot = event.getRawSlot();
        if (slot == 48 && view.page > 0 || slot == 50 && view.page + 1 < pages()) {
            int next = view.page + (slot == 48 ? -1 : 1);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(top)) open(player, next);
            });
            return;
        }
        SwordType sword = selection(view.page, slot);
        if (sword == null) return;
        if (!player.hasPermission("legendaryswords.admin")) {
            player.sendMessage(Component.text("Only admins can receive swords from this menu.", NamedTextColor.RED));
            return;
        }
        if (player.getInventory().firstEmpty() < 0) {
            player.sendMessage(Component.text("Make room in your inventory first.", NamedTextColor.YELLOW)); return;
        }
        // Never move the displayed stack. Give a clean item without menu lore.
        player.getInventory().addItem(plugin.swords().create(sword));
        player.sendMessage(Component.text("Received " + sword.title() + ".", sword.textColor()));
    }
    @EventHandler public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof View) event.setCancelled(true);
    }
    public void close() {
        for (Player player : plugin.getServer().getOnlinePlayers())
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof View) player.closeInventory();
    }
}
