package io.github.tempblocks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class TemporaryBlocks extends JavaPlugin implements Listener {
    private final Map<String, Entry> pending = new HashMap<>();
    private final Set<UUID> bypass = new HashSet<>();
    private record Entry(UUID world, int x, int y, int z, String data, long expiry) {}
    private File storage;
    private static String key(Block b) {
        return b.getWorld().getUID()+":"+b.getX()+":"+b.getY()+":"+b.getZ();
    }
    @Override public void onEnable() {
        saveDefaultConfig();
        storage = new File(getDataFolder(), "pending.yml");
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(storage);
        for (String k : saved.getKeys(false)) {
            try {
                pending.put(k, new Entry(UUID.fromString(saved.getString(k+".world")),
                    saved.getInt(k+".x"), saved.getInt(k+".y"), saved.getInt(k+".z"),
                    saved.getString(k+".data"), saved.getLong(k+".expiry")));
            } catch (RuntimeException ex) { getLogger().warning("Invalid saved block: "+k); }
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::expire, 20, 10);
    }
    @Override public void onDisable() { savePending(); }
    private void savePending() {
        YamlConfiguration out = new YamlConfiguration();
        pending.forEach((k,e) -> {
            out.set(k+".world", e.world.toString()); out.set(k+".x",e.x);
            out.set(k+".y",e.y); out.set(k+".z",e.z);
            out.set(k+".data",e.data); out.set(k+".expiry",e.expiry);
        });
        try { out.save(storage); }
        catch (IOException ex) { getLogger().severe("Could not save temporary blocks: "+ex.getMessage()); }
    }
    static boolean sameType(String current, String placed) {
        return current.split("\\[", 2)[0].equals(placed.split("\\[", 2)[0]);
    }
    @EventHandler(ignoreCancelled=true)
    public void falling(BlockPhysicsEvent e) {
        if (pending.containsKey(key(e.getBlock())) && e.getBlock().getType().hasGravity()) e.setCancelled(true);
    }
    private void expire() {
        boolean changed = false;
        Iterator<Entry> it = pending.values().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.expiry > System.currentTimeMillis()) continue;
            World w = Bukkit.getWorld(e.world);
            if (w == null || !w.isChunkLoaded(e.x >> 4, e.z >> 4)) continue;
            Block b = w.getBlockAt(e.x,e.y,e.z);
            // Allow state changes (doors, stairs, crops); player replacement resets the timer.
            if (sameType(b.getBlockData().getAsString(), e.data)) b.setType(Material.AIR);
            it.remove(); changed = true;
        }
        if (changed) savePending();
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void place(BlockPlaceEvent event) {
        List<Block> blocks = event instanceof BlockMultiPlaceEvent multi
            ? multi.getReplacedBlockStates().stream().map(s -> s.getBlock()).toList()
            : List.of(event.getBlockPlaced());
        Player p = event.getPlayer();
        boolean permanent = bypass.contains(p.getUniqueId()) && p.hasPermission("tempblocks.bypass");
        long expiry = System.currentTimeMillis()+Math.max(1,getConfig().getLong("duration-seconds",30))*1000L;
        for (Block b : blocks) {
            if (permanent) pending.remove(key(b));
            else pending.put(key(b),new Entry(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ(),b.getBlockData().getAsString(),expiry));
        }
        savePending();
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void broken(BlockBreakEvent e) {
        if (pending.remove(key(e.getBlock())) != null) savePending();
    }
    @EventHandler(ignoreCancelled=true)
    public void piston(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> pending.containsKey(key(b)))) e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true)
    public void piston(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> pending.containsKey(key(b)))) e.setCancelled(true);
    }
    @EventHandler public void quit(PlayerQuitEvent e) { bypass.remove(e.getPlayer().getUniqueId()); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Use this command in game."); return true; }
        if (args.length != 1 || !args[0].equalsIgnoreCase("bypass")) return false;
        if (!p.hasPermission("tempblocks.bypass")) { p.sendMessage("You do not have permission to bypass temporary placement."); return true; }
        boolean enabled = bypass.add(p.getUniqueId());
        if (!enabled) bypass.remove(p.getUniqueId());
        p.sendMessage(enabled ? "Bypass ON: newly placed blocks are permanent." : "Bypass OFF: newly placed blocks disappear after "+getConfig().getLong("duration-seconds",30)+" seconds.");
        return true;
    }
}
