package io.github.legendarykoth;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.*;

final class WorldGuardRegion {
    private final Object container;
    private final Method adapt,manager,region,vector,contains;
    WorldGuardRegion(Plugin worldGuard) throws ReflectiveOperationException {
        ClassLoader loader=worldGuard.getClass().getClassLoader();
        Class<?> wg=Class.forName("com.sk89q.worldguard.WorldGuard",true,loader);
        Class<?> platformType=Class.forName("com.sk89q.worldguard.internal.platform.WorldGuardPlatform",true,loader);
        Object instance=wg.getMethod("getInstance").invoke(null);
        Object platform=wg.getMethod("getPlatform").invoke(instance);
        container=platformType.getMethod("getRegionContainer").invoke(platform);
        Class<?> adapter=Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter",true,loader);
        Class<?> weWorld=Class.forName("com.sk89q.worldedit.world.World",true,loader);
        Class<?> containerType=Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer",true,loader);
        Class<?> managerType=Class.forName("com.sk89q.worldguard.protection.managers.RegionManager",true,loader);
        Class<?> protectedRegion=Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion",true,loader);
        Class<?> blockVector=Class.forName("com.sk89q.worldedit.math.BlockVector3",true,loader);
        adapt=adapter.getMethod("adapt",World.class);manager=containerType.getMethod("get",weWorld);
        region=managerType.getMethod("getRegion",String.class);vector=blockVector.getMethod("at",int.class,int.class,int.class);
        contains=protectedRegion.getMethod("contains",blockVector);
    }
    Object find(World world,String name) throws ReflectiveOperationException {
        Object rm=manager.invoke(container,adapt.invoke(null,world));return rm==null?null:region.invoke(rm,name);
    }
    boolean contains(Object selected,Location location) throws ReflectiveOperationException {
        return (boolean)contains.invoke(selected,vector.invoke(null,location.getBlockX(),location.getBlockY(),location.getBlockZ()));
    }
}
