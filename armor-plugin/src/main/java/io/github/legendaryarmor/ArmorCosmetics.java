package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
final class ArmorCosmetics implements AutoCloseable {
    private record Worn(ArmorSet set,ItemDisplay back,ItemDisplay crown,ItemDisplay helmet){}
    private final JavaPlugin plugin;private final Map<UUID,Worn> worn=new HashMap<>();
    ArmorCosmetics(JavaPlugin plugin){this.plugin=plugin;}
    static ItemStack model(String name) {ItemStack item=new ItemStack(Material.PAPER);var meta=item.getItemMeta();meta.setItemModel(new NamespacedKey("legendaryarmor",name));item.setItemMeta(meta);return item;}
    private ItemDisplay spawn(Player player,String model) {
        ItemDisplay display=player.getWorld().spawn(player.getLocation(),ItemDisplay.class,d->{
            d.setItemStack(model(model));d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setPersistent(false);d.setGravity(false);d.setViewRange(1);d.setTeleportDuration(0);d.setInterpolationDuration(0);
            // Worn metal uses normal world lighting rather than full-bright illumination.
        });
        if(!plugin.getConfig().getBoolean("accessories-visible-to-owner",true))player.hideEntity(plugin,display);return display;
    }
    void update(Player player,ArmorSet set) {
        if(set==null||player.isDead()||!player.isOnline()||!plugin.getConfig().getBoolean("accessories-enabled",false)){clear(player.getUniqueId());return;}
        Worn current=worn.get(player.getUniqueId());
        if(current==null||current.set!=set||!current.back.isValid()||!current.helmet.isValid()||!current.back.getWorld().equals(player.getWorld())) {
            clear(player.getUniqueId());current=new Worn(set,spawn(player,set.id+"_back"),spawn(player,set.id+"_crown"),spawn(player,set.id+"_helmet_shell"));worn.put(player.getUniqueId(),current);
            player.hideEntity(plugin,current.helmet);
        }
        Location at=player.getLocation();float yaw=at.getYaw();at.setPitch(0);
        double radians=Math.toRadians(yaw);Location back=at.clone().add(Math.sin(radians)*.22,player.isSneaking()?1.15:1.4,-Math.cos(radians)*.22);
        current.helmet.teleport(player.getEyeLocation());
        current.back.teleport(back);current.crown.teleport(at.clone().add(0,player.isSneaking()?1.8:2.03,0));
        // Hide attachments while the actual player is invisible (including admin vanish).
        for(Player viewer:plugin.getServer().getOnlinePlayers())if(!viewer.getUniqueId().equals(player.getUniqueId())) {
            if(viewer.canSee(player)&&!player.isInvisible()){viewer.showEntity(plugin,current.back);viewer.showEntity(plugin,current.crown);viewer.showEntity(plugin,current.helmet);}
            else {viewer.hideEntity(plugin,current.back);viewer.hideEntity(plugin,current.crown);viewer.hideEntity(plugin,current.helmet);}
        }
    }
    void clear(UUID id){Worn old=worn.remove(id);if(old!=null){old.back.remove();old.crown.remove();old.helmet.remove();}}
    @Override public void close(){for(UUID id:new ArrayList<>(worn.keySet()))clear(id);}
}
