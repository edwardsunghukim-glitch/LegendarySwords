package io.github.legendaryadmin;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.IOException;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class LegendaryAdmin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    static final List<String> ACTIONS = List.of("menu", "kick", "ban", "unban", "mute", "freeze", "heal", "feed", "fly", "gm", "tp", "tphere", "vanish", "invsee", "help");
    static final List<String> DIRECT_COMMANDS = List.of("adminmenu", "adminhelp", "kick", "ban", "unban", "mute", "freeze", "heal", "feed", "fly", "gm", "tp", "tphere", "vanish", "invsee");
    static String[] commandArgs(Command command, String[] args) {
        String name = command == null ? null : command.getName();
        if (name == null || !DIRECT_COMMANDS.contains(name)) return args;
        String[] expanded = new String[args.length + 1];
        expanded[0] = name.equals("adminmenu") ? "menu" : name.equals("adminhelp") ? "help" : name;
        System.arraycopy(args, 0, expanded, 1, args.length);
        return expanded;
    }
    private final Set<UUID> frozen = new HashSet<>(), vanished = new HashSet<>();
    private final Map<UUID, Flight> flights = new HashMap<>();
    private AdminState state;
    private record Flight(boolean allowed, boolean flying) { }
    @Override public void onEnable() {
        state = new AdminState(getDataFolder().toPath().resolve("mutes.yml"));
        for (String name : DIRECT_COMMANDS) {
            Objects.requireNonNull(getCommand(name)).setExecutor(this);
            Objects.requireNonNull(getCommand(name)).setTabCompleter(this);
        }
        Objects.requireNonNull(getCommand("legendaryadmin")).setExecutor(this);
        Objects.requireNonNull(getCommand("legendaryadmin")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        // Refresh visibility when permission plugins grant/revoke see-vanished at runtime.
        getServer().getScheduler().runTaskTimer(this, this::refreshVisibility, 20L, 20L);
    }
    @Override public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            for (Player viewer : getServer().getOnlinePlayers()) viewer.showPlayer(this, player);
            restoreFlight(player);
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof AdminView) player.closeInventory();
        }
        vanished.clear(); frozen.clear();
    }
    static String permission(String action) {
        return "legendaryadmin." + switch (action) {
            case "gm" -> "gamemode";
            case "tp", "tphere" -> "teleport";
            default -> action;
        };
    }
    static boolean allowed(CommandSender sender, String action) {
        return sender.hasPermission(permission(action));
    }
    private void say(CommandSender sender, String text) { sender.sendMessage(Component.text("[Admin] " + text)); }
    private Player online(CommandSender sender, String name) {
        Player player = getServer().getPlayerExact(name);
        if (player == null) say(sender, "That player is not online. Use their exact name.");
        return player;
    }
    private Player target(CommandSender sender, String[] args, int index) {
        if (args.length > index) return online(sender, args[index]);
        if (sender instanceof Player player) return player;
        say(sender, "Specify an online player."); return null;
    }
    private boolean usage(CommandSender sender, String text) { say(sender, "Usage: /" + text.replaceFirst("^menu", "adminmenu").replaceFirst("^help", "adminhelp")); return true; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        args = commandArgs(command, args);
        String action = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("help")) {
            if (!ACTIONS.stream().filter(a -> !a.equals("help")).anyMatch(a -> allowed(sender, a))) {
                say(sender, "You don't have permission to use admin tools."); return true;
            }
            say(sender, "/adminmenu [player] | /heal, /feed, /fly, /vanish [player] | /invsee <player>");
            say(sender, "/kick, /ban <player> [reason] | /unban <name|UUID> | /mute, /freeze <player>");
            say(sender, "/gm <survival|creative|adventure|spectator> [player] | /tp <player> [destination] | /tphere <player>");
            return true;
        }
        if (!ACTIONS.contains(action)) return usage(sender, "help");
        if (!allowed(sender, action)) { say(sender, "You don't have permission for " + action + "."); return true; }
        try {
            if (action.equals("unban")) {
                if (args.length != 2) return usage(sender, "unban <name|UUID>");
                OfflinePlayer offline;
                try { offline = getServer().getOfflinePlayer(UUID.fromString(args[1])); }
                catch (IllegalArgumentException ignored) { offline = getServer().getOfflinePlayerIfCached(args[1]); }
                if (offline == null) { say(sender, "Unknown cached name. Use the banned player's UUID from banned-players.json."); return true; }
                ProfileBanList list = getServer().getBanList(BanList.Type.PROFILE);
                list.pardon(offline.getPlayerProfile());
                say(sender, "Unbanned " + args[1] + "."); audit(sender, action, offline.getUniqueId().toString()); return true;
            }
            if (action.equals("gm")) {
                if (args.length < 2 || args.length > 3) return usage(sender, "gm <survival|creative|adventure|spectator> [player]");
                GameMode mode;
                try { mode = GameMode.valueOf(args[1].toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException error) { return usage(sender, "gm <survival|creative|adventure|spectator> [player]"); }
                Player player = target(sender, args, 2);
                if (player != null) { restoreFlight(player); player.setGameMode(mode); say(sender, player.getName() + " is now " + mode.name().toLowerCase(Locale.ROOT) + "."); audit(sender, action, player.getName()); }
                return true;
            }
            if (action.equals("tp")) {
                if (args.length < 2 || args.length > 3) return usage(sender, "tp <player> [destination]");
                Player first = online(sender, args[1]);
                Player moving = args.length == 3 ? first : sender instanceof Player p ? p : null;
                Player destination = args.length == 3 ? online(sender, args[2]) : first;
                if (moving == null) { if (!(sender instanceof Player)) say(sender, "Console must specify both players."); return true; }
                if (destination != null) teleport(sender, moving, destination);
                return true;
            }
            if (action.equals("tphere")) {
                if (!(sender instanceof Player admin)) { say(sender, "Only players can bring someone to their location."); return true; }
                if (args.length != 2) return usage(sender, "tphere <player>");
                Player player = online(sender, args[1]);
                if (player != null) teleport(sender, player, admin);
                return true;
            }
            boolean required = List.of("kick", "ban", "mute", "freeze", "invsee").contains(action);
            if (required && args.length < 2) return usage(sender, action + " <player>" + (List.of("kick", "ban").contains(action) ? " [reason]" : ""));
            if (!List.of("kick", "ban").contains(action) && args.length > 2) return usage(sender, action + " [player]");
            if (action.equals("menu") && args.length <= 1) {
                if (sender instanceof Player admin) openPlayers(admin, 0);
                else say(sender, "Inventory screens are only available in-game.");
                return true;
            }
            Player player = target(sender, args, 1);
            if (player == null) return true;
            if (action.equals("menu") || action.equals("invsee")) {
                if (!(sender instanceof Player admin)) { say(sender, "Inventory screens are only available in-game."); return true; }
                if (action.equals("menu")) openMenu(admin, player); else openInventory(admin, player);
                return true;
            }
            String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Removed by an administrator";
            perform(sender, action, player, reason);
        } catch (IOException | RuntimeException error) {
            getLogger().log(java.util.logging.Level.WARNING, "Admin action failed: " + action, error);
            say(sender, "The action failed. Check the server console.");
        }
        return true;
    }
    private void teleport(CommandSender sender, Player moving, Player destination) {
        if (moving.teleport(destination.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            say(sender, "Teleported " + moving.getName() + " to " + destination.getName() + "."); audit(sender, "tp", moving.getName());
        } else say(sender, "Teleport was blocked by the server or another plugin.");
    }
    private void perform(CommandSender sender, String action, Player player, String reason) throws IOException {
        // Recheck here: GUI permissions can change while the menu is open.
        if (!allowed(sender, action)) { say(sender, "You don't have permission for " + action + "."); return; }
        switch (action) {
            case "heal" -> { var max = player.getAttribute(Attribute.MAX_HEALTH); if (max != null) player.setHealth(max.getValue()); player.setFireTicks(0); }
            case "feed" -> { player.setFoodLevel(20); player.setSaturation(20); player.setExhaustion(0); }
            case "kick" -> player.kick(Component.text(reason));
            case "ban" -> player.ban(reason, (java.util.Date) null, sender.getName(), true);
            case "mute" -> { boolean enabled = state.toggleMute(player.getUniqueId()); say(sender, player.getName() + (enabled ? " muted." : " unmuted.")); say(player, enabled ? "You have been muted in public chat." : "You have been unmuted."); }
            case "freeze" -> { boolean enabled = frozen.add(player.getUniqueId()); if (!enabled) frozen.remove(player.getUniqueId()); say(sender, player.getName() + (enabled ? " frozen." : " unfrozen.")); say(player, enabled ? "You have been frozen by an administrator." : "You have been unfrozen."); }
            case "fly" -> {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) { say(sender, "That game mode already provides flight."); return; }
                if (flights.containsKey(player.getUniqueId())) { restoreFlight(player); say(sender, "Admin flight disabled for " + player.getName() + "."); }
                else { flights.put(player.getUniqueId(), new Flight(player.getAllowFlight(), player.isFlying())); player.setAllowFlight(true); say(sender, "Admin flight enabled for " + player.getName() + ". Double-jump to fly."); }
            }
            case "vanish" -> {
                boolean enabled = vanished.add(player.getUniqueId()); if (!enabled) {
                    vanished.remove(player.getUniqueId());
                    for (Player viewer : getServer().getOnlinePlayers()) viewer.showPlayer(this, player);
                }
                refreshVisibility(); say(sender, player.getName() + (enabled ? " vanished." : " visible."));
                if (sender != player) say(player, enabled ? "You are vanished." : "You are visible.");
            }
            default -> { return; }
        }
        if (action.equals("heal") || action.equals("feed")) say(sender, action + " applied to " + player.getName() + ".");
        audit(sender, action, player.getName());
    }
    private void audit(CommandSender sender, String action, String target) { getLogger().info(sender.getName() + " used " + action + " on " + target); }
    private void restoreFlight(Player player) {
        Flight original = flights.remove(player.getUniqueId());
        if (original == null) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        player.setFlying(false); player.setAllowFlight(original.allowed());
        if (original.allowed() && original.flying()) player.setFlying(true);
    }
    private void refreshVisibility() {
        for (UUID id : vanished) {
            Player target = getServer().getPlayer(id); if (target == null) continue;
            for (Player viewer : getServer().getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            if (vanished.contains(target.getUniqueId()) && !viewer.hasPermission("legendaryadmin.see-vanished")) viewer.hidePlayer(this, target);
            else viewer.showPlayer(this, target);
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void chat(AsyncChatEvent event) {
        if (state.muted(event.getPlayer().getUniqueId())) { event.setCancelled(true); event.getPlayer().sendMessage(Component.text("[Admin] You are muted in public chat.")); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void move(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || !frozen.contains(event.getPlayer().getUniqueId()) || event.getTo() == null) return;
        Location from = event.getFrom(), to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location fixed = from.clone(); fixed.setYaw(to.getYaw()); fixed.setPitch(to.getPitch()); event.setTo(fixed);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void interact(PlayerInteractEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void attack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && frozen.contains(player.getUniqueId())) event.setCancelled(true);
    }
    @EventHandler public void join(PlayerJoinEvent event) { getServer().getScheduler().runTask(this, this::refreshVisibility); }
    @EventHandler public void quit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId(); frozen.remove(id);
        if (vanished.remove(id)) for (Player viewer : getServer().getOnlinePlayers()) viewer.showPlayer(this, event.getPlayer());
        restoreFlight(event.getPlayer());
    }

    static final class AdminView implements InventoryHolder {
        final UUID target;
        final boolean readOnly;
        final Map<Integer, String> actions = new HashMap<>();
        Inventory inventory;
        AdminView(UUID target, boolean readOnly) { this.target = target; this.readOnly = readOnly; }
        @Override public Inventory getInventory() { return Objects.requireNonNull(inventory); }
    }
    private ItemStack button(Material material, String title) {
        ItemStack item = new ItemStack(material); var meta = item.getItemMeta(); meta.displayName(Component.text(title)); item.setItemMeta(meta); return item;
    }
    private void openPlayers(Player admin, int requestedPage) {
        if (!allowed(admin, "menu")) { say(admin, "You don't have permission for menu."); return; }
        List<? extends Player> players = getServer().getOnlinePlayers().stream().filter(admin::canSee)
                .sorted(Comparator.comparing(Player::getName)).toList();
        int pages = Math.max(1, (players.size() + 44) / 45);
        int page = Math.clamp(requestedPage, 0, pages - 1);
        var view = new AdminView(null, false);
        view.inventory = getServer().createInventory(view, 54, Component.text("Admin players " + (page + 1) + "/" + pages));
        for (int slot = 0; slot < 45 && page * 45 + slot < players.size(); slot++) {
            Player target = players.get(page * 45 + slot);
            view.actions.put(slot, "target:" + target.getUniqueId());
            view.inventory.setItem(slot, button(Material.PLAYER_HEAD, target.getName()));
        }
        if (page > 0) { view.actions.put(45, "page:" + (page - 1)); view.inventory.setItem(45, button(Material.ARROW, "Previous page")); }
        if (page + 1 < pages) { view.actions.put(53, "page:" + (page + 1)); view.inventory.setItem(53, button(Material.ARROW, "Next page")); }
        admin.openInventory(view.inventory);
    }
    private void openMenu(Player admin, Player target) {
        var view = new AdminView(target.getUniqueId(), false);
        view.inventory = getServer().createInventory(view, 27, Component.text("Admin: " + target.getName()));
        String[] actions = {"heal", "feed", "fly", "freeze", "mute", "vanish", "invsee", "tp", "tphere", "survival", "creative", "spectator", "kick", "ban"};
        Material[] materials = {Material.GOLDEN_APPLE, Material.COOKED_BEEF, Material.FEATHER, Material.ICE, Material.PAPER, Material.ENDER_PEARL, Material.CHEST, Material.COMPASS, Material.ENDER_EYE, Material.GRASS_BLOCK, Material.DIAMOND, Material.GLASS, Material.LEATHER_BOOTS, Material.BARRIER};
        for (int i = 0; i < actions.length; i++) {
            String action = actions[i];
            String permissionAction = Set.of("survival", "creative", "spectator").contains(action) ? "gm" : action;
            if (allowed(admin, permissionAction)) { view.actions.put(i, action); view.inventory.setItem(i, button(materials[i], action + " — " + target.getName())); }
        }
        admin.openInventory(view.inventory);
    }
    private void openInventory(Player admin, Player target) {
        var view = new AdminView(target.getUniqueId(), true);
        view.inventory = getServer().createInventory(view, 54, Component.text("Read only: " + target.getName()));
        ItemStack[] storage = target.getInventory().getStorageContents();
        for (int slot = 0; slot < Math.min(storage.length, 36); slot++) if (storage[slot] != null) view.inventory.setItem(slot, storage[slot].clone());
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) if (armor[slot] != null) view.inventory.setItem(36 + slot, armor[slot].clone());
        view.inventory.setItem(40, target.getInventory().getItemInOffHand().clone());
        view.inventory.setItem(49, button(Material.PAPER, "Snapshot; reopen /invsee to refresh"));
        admin.openInventory(view.inventory);
    }
    static boolean protectedInventory(Inventory inventory) { return inventory.getHolder() instanceof AdminView; }
    @EventHandler(priority = EventPriority.HIGHEST) public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AdminView view)) return;
        event.setCancelled(true); // Also cancels shift clicks, hotbar swaps, double clicks and bottom-slot moves.
        if (view.readOnly || !(event.getWhoClicked() instanceof Player admin)) return;
        String action = view.actions.get(event.getRawSlot()); if (action == null) return;
        if (action.startsWith("page:")) {
            getServer().getScheduler().runTask(this, () -> { if (admin.isOnline()) openPlayers(admin, Integer.parseInt(action.substring(5))); }); return;
        }
        UUID targetId = action.startsWith("target:") ? UUID.fromString(action.substring(7)) : view.target;
        Player target = getServer().getPlayer(targetId);
        if (target == null) { say(admin, "That player has left."); getServer().getScheduler().runTask(this, () -> admin.closeInventory()); return; }
        getServer().getScheduler().runTask(this, () -> {
            if (!admin.isOnline() || !target.isOnline()) return;
            if (action.startsWith("target:")) {
                if (allowed(admin, "menu")) openMenu(admin, target);
                else { say(admin, "You don't have permission for menu."); admin.closeInventory(); }
            } else if (action.startsWith("confirm:")) {
                admin.closeInventory(); onCommand(admin, getCommand("legendaryadmin"), "la", new String[]{action.substring(8), target.getName(), "Removed by an administrator"});
            } else if (action.equals("kick") || action.equals("ban")) {
                if (!allowed(admin, action)) { say(admin, "You don't have permission for " + action + "."); admin.closeInventory(); return; }
                var confirmation = new AdminView(target.getUniqueId(), false);
                confirmation.inventory = getServer().createInventory(confirmation, 9, Component.text("Confirm " + action + ": " + target.getName()));
                confirmation.actions.put(4, "confirm:" + action);
                confirmation.inventory.setItem(4, button(Material.RED_CONCRETE, "Click to confirm " + action));
                admin.openInventory(confirmation.inventory);
            } else if (Set.of("survival", "creative", "spectator").contains(action)) {
                onCommand(admin, getCommand("legendaryadmin"), "la", new String[]{"gm", action, target.getName()});
            } else onCommand(admin, getCommand("legendaryadmin"), "la", new String[]{action, target.getName()});
        });
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void drag(InventoryDragEvent event) {
        if (protectedInventory(event.getView().getTopInventory())) event.setCancelled(true);
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        args = commandArgs(command, args);
        List<String> choices = new ArrayList<>();
        if (args.length == 1) ACTIONS.stream().filter(a -> !a.equals("help") && allowed(sender, a)).forEach(choices::add);
        else if (args.length > 1 && allowed(sender, args[0].toLowerCase(Locale.ROOT))) {
            if (args[0].equalsIgnoreCase("gm") && args.length == 2) choices.addAll(List.of("survival", "creative", "adventure", "spectator"));
            else for (Player player : getServer().getOnlinePlayers()) if (!(sender instanceof Player viewer) || viewer.canSee(player)) choices.add(player.getName());
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
