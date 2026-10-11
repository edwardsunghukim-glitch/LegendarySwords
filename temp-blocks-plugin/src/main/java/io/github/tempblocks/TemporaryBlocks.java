package io.github.tempblocks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Bed;
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
    record Entry(UUID world, int x, int y, int z, String data, String restore, long expiry) {}
    private boolean dirty;
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
                    saved.getString(k+".data"), saved.getString(k+".restore", "minecraft:air"), saved.getLong(k+".expiry")));
            } catch (RuntimeException ex) { getLogger().warning("Invalid saved block: "+k); }
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::expire, 20, 10);
        getServer().getScheduler().runTaskTimer(this, ()->{if(dirty)savePending();},100,100);
    }
    @Override public void onDisable() { savePending(); }
    private void savePending() {
        YamlConfiguration out = new YamlConfiguration();
        pending.forEach((k,e) -> {
            out.set(k+".world", e.world.toString()); out.set(k+".x",e.x);
            out.set(k+".y",e.y); out.set(k+".z",e.z);
            out.set(k+".data",e.data); out.set(k+".restore",e.restore); out.set(k+".expiry",e.expiry);
        });
        try { out.save(storage); dirty=false; }
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
            if (sameType(b.getBlockData().getAsString(), e.data)) b.setBlockData(Bukkit.createBlockData(e.restore),false);
            it.remove(); changed = true;
        }
        if (changed) dirty=true;
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void place(BlockPlaceEvent event) {
        var states = event instanceof BlockMultiPlaceEvent multi
            ? multi.getReplacedBlockStates() : List.of(event.getBlockReplacedState());
        boolean permanent = permanent(event.getPlayer());
        long expiry = deadline();
        for (var state : states) {
            Block b=state.getBlock();String id=key(b);
            if (permanent) pending.remove(id);
            else {
                Entry old=pending.get(id);
                String restore=old==null?state.getBlockData().getAsString():old.restore;
                pending.put(id,new Entry(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ(),b.getBlockData().getAsString(),restore,expiry));
            }
        }
        dirty=true;
    }
    private long deadline() { return System.currentTimeMillis()+Math.min(86400,Math.max(1,getConfig().getLong("duration-seconds",30)))*1000L; }
    private boolean permanent(Player p) { return bypass.contains(p.getUniqueId()) && p.hasPermission("tempblocks.bypass"); }
    static Entry brokenEntry(Entry old,UUID world,int x,int y,int z,String original,long expiry) {
        String restore=old==null?original:old.restore;
        // Breaking a temporary addition must not resurrect that addition.
        if(sameType(restore,"minecraft:air") || sameType(restore,"minecraft:cave_air") || sameType(restore,"minecraft:void_air")) return null;
        return new Entry(world,x,y,z,"minecraft:air",restore,expiry);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void protectContainers(BlockBreakEvent e) {
        if(!permanent(e.getPlayer()) && e.getBlock().getState() instanceof TileState) {
            e.setCancelled(true);e.getPlayer().sendMessage("Use bypass to permanently break containers or special data blocks.");
        } else if(!permanent(e.getPlayer())) {e.setDropItems(false);e.setExpToDrop(0);}
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void broken(BlockBreakEvent e) {
        List<Block> blocks=new ArrayList<>();Block b=e.getBlock();blocks.add(b);
        if(b.getBlockData() instanceof Door door) {
            Block other=b.getRelative(0,door.getHalf()==Bisected.Half.BOTTOM?1:-1,0);
            if(other.getType()==b.getType())blocks.add(other);
        } else if(b.getBlockData() instanceof Bed bed) {
            Block other=b.getRelative(bed.getPart()==Bed.Part.FOOT?bed.getFacing():bed.getFacing().getOppositeFace());
            if(other.getType()==b.getType())blocks.add(other);
        }
        for(Block part:blocks) {
            String id=key(part);
            if(permanent(e.getPlayer()))pending.remove(id);
            else {
                Entry entry=brokenEntry(pending.get(id),part.getWorld().getUID(),part.getX(),part.getY(),part.getZ(),part.getBlockData().getAsString(),deadline());
                if(entry==null)pending.remove(id);else pending.put(id,entry);
            }
        }
        dirty=true;
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
        p.sendMessage(enabled ? "Bypass ON: placing and breaking blocks is permanent." : "Bypass OFF: placing and breaking blocks resets after "+getConfig().getLong("duration-seconds",30)+" seconds.");
        return true;
    }
}
