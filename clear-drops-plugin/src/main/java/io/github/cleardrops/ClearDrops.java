package io.github.cleardrops;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public final class ClearDrops extends JavaPlugin {
    private BukkitTask task;
    private long interval, remaining, warning;
    private Set<String> excluded = Set.of();
    @Override public void onEnable() { saveDefaultConfig(); startTimer(); }
    @Override public void onDisable() { if (task != null) task.cancel(); }
    private void startTimer() {
        if (task != null) task.cancel();
        interval = Math.min(86400, Math.max(10, getConfig().getLong("interval-seconds", 600)));
        warning = Math.min(interval-1, Math.max(0, getConfig().getLong("warning-seconds", 30)));
        excluded = Set.copyOf(getConfig().getStringList("excluded-worlds"));
        remaining = interval;
        task = getServer().getScheduler().runTaskTimer(this, this::tick, 20, 20);
    }
    private void tick() {
        remaining--;
        if (warning > 0 && remaining == warning)
            Bukkit.broadcastMessage("[ClearDrops] Dropped items will be cleared in " + warning + " seconds. Pick up anything you want to keep!");
        if (remaining <= 0) {
            int removed = cleanup(getServer().getWorlds(), excluded);
            Bukkit.broadcastMessage("[ClearDrops] Cleared " + removed + " dropped item entities.");
            remaining = interval;
        }
    }
    static int cleanup(Collection<World> worlds, Set<String> excluded) {
        int removed = 0;
        for (World world : worlds) {
            if (excluded.contains(world.getName())) continue;
            // Only loaded dropped item entities; no mobs, displays, frames or inventories.
            for (Entity entity : world.getEntitiesByClass(Item.class)) {
                if (entity instanceof Item item && item.isValid()) { item.remove(); removed++; }
            }
        }
        return removed;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        return args.length==1&&sender.hasPermission("cleardrops.admin")?CommandSuggestions.matching(List.of("now","reload"),args[0]):List.of();
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("[ClearDrops] Next cleanup in " + remaining + " seconds."); return true;
        }
        if (!sender.hasPermission("cleardrops.admin")) {
            sender.sendMessage("You do not have permission to manage dropped-item cleanup."); return true;
        }
        if (args.length != 1) return false;
        if (args[0].equalsIgnoreCase("now")) {
            int removed = cleanup(getServer().getWorlds(), excluded);
            Bukkit.broadcastMessage("[ClearDrops] Cleared " + removed + " dropped item entities.");
            remaining = interval; return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig(); startTimer(); sender.sendMessage("ClearDrops configuration reloaded; timer restarted."); return true;
        }
        return false;
    }
}
