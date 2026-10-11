package io.github.killtokens;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.function.Predicate;

final class SpawnBarrier implements AutoCloseable {
    private final KillTokens plugin;
    private final Predicate<Player> tagged;
    private WorldGuardRegion regions;
    private final Map<UUID,Set<Location>> walls=new HashMap<>();
    private boolean warned;
    private final Map<UUID,Object> cachedRegions=new HashMap<>();
    private long cacheUntil;
    SpawnBarrier(KillTokens plugin,Predicate<Player> tagged) {
        this.plugin=plugin;this.tagged=tagged;
        Plugin wg=plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if(wg!=null&&wg.isEnabled())try {regions=new WorldGuardRegion(wg);}catch(ReflectiveOperationException ex){warn(ex);}
        if(regions==null)plugin.getLogger().warning("Combat spawn protection requires WorldGuard 7 and WorldEdit; spawn barrier is unavailable.");
    }
    private void warn(Exception error){if(!warned){warned=true;plugin.getLogger().warning("Cannot read combat spawn region: "+error.getMessage());}}
    boolean inside(Location location) {
        if(regions==null||location==null||location.getWorld()==null||!plugin.getConfig().getBoolean("combat.spawn.enabled",true))return false;
        try {
            long now=System.currentTimeMillis();if(now>=cacheUntil){cachedRegions.clear();cacheUntil=now+1000;}
            UUID world=location.getWorld().getUID();
            if(!cachedRegions.containsKey(world))cachedRegions.put(world,regions.find(location.getWorld(),plugin.getConfig().getString("combat.spawn.region","spawn")));
            Object region=cachedRegions.get(world);return region!=null&&regions.contains(region,location);
        }
        catch(ReflectiveOperationException ex){warn(ex);return false;}
    }
    boolean blocked(Player player,Location from,Location to) {
        if(!tagged.test(player)||to==null)return false;
        // Tagged players already inside can leave; they cannot re-enter.
        if(inside(to)&&!inside(from))return true;
        if(from.getWorld().equals(to.getWorld())&&!inside(from)) {
            double length=from.distance(to);int steps=(int)Math.min(128,Math.ceil(length*2));
            for(int i=1;i<steps;i++)if(inside(from.clone().add(to.toVector().subtract(from.toVector()).multiply((double)i/steps))))return true;
        }
        return false;
    }
    void tick() {
        for(Player player:plugin.getServer().getOnlinePlayers()) {
            if(!tagged.test(player)||regions==null||!plugin.getConfig().getBoolean("combat.spawn.wall",true)){clear(player);continue;}
            Location origin=player.getLocation();Set<Location> next=new HashSet<>();
            // Only outline nearby air along the horizontal polygon boundary.
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
                Location base=origin.getBlock().getLocation().add(x,0,z);
                if(!inside(base))continue;
                boolean edge=false;for(int[] side:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
                    if(!inside(base.clone().add(side[0],0,side[1]))){edge=true;break;}
                if(edge)for(int y=-1;y<=3;y++) {
                    Location block=base.clone().add(0,y,0);
                    if(block.getWorld().isChunkLoaded(block.getBlockX()>>4,block.getBlockZ()>>4)&&block.getBlock().isPassable())next.add(block);
                }
            }
            Set<Location> previous=walls.getOrDefault(player.getUniqueId(),Set.of());
            for(Location old:previous)if(!next.contains(old)&&old.getWorld().equals(player.getWorld()))player.sendBlockChange(old,old.getBlock().getBlockData());
            for(Location block:next)player.sendBlockChange(block,Material.RED_STAINED_GLASS.createBlockData());
            walls.put(player.getUniqueId(),next);
        }
    }
    void clear(Player player) {
        Set<Location> old=walls.remove(player.getUniqueId());
        if(old!=null)for(Location block:old)if(block.getWorld().equals(player.getWorld()))player.sendBlockChange(block,block.getBlock().getBlockData());
    }
    @Override public void close(){for(Player player:plugin.getServer().getOnlinePlayers())clear(player);walls.clear();}
}
