package io.github.totemlimit;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class TotemLimit extends JavaPlugin implements Listener {
    @Override public void onEnable() {
        saveDefaultConfig();getServer().getPluginManager().registerEvents(this,this);
        getServer().getScheduler().runTaskTimer(this,()->getServer().getOnlinePlayers().forEach(this::enforce),1,
            Math.max(1,Math.min(20,getConfig().getInt("check-interval-ticks",1))));
    }
    int limit(Material type) { return Math.max(0,getConfig().getInt(type==Material.TOTEM_OF_UNDYING?"limits.totems":"limits.ender-pearls",type==Material.TOTEM_OF_UNDYING?2:32)); }
    static int count(ItemStack[] items,ItemStack cursor,Material type) {
        int count=0;for(ItemStack item:items)if(item!=null&&item.getType()==type)count+=item.getAmount();
        return count+(cursor!=null&&cursor.getType()==type?cursor.getAmount():0);
    }
    // Keeps early slots first; returns exact excess stacks, preserving item metadata.
    static List<ItemStack> trim(ItemStack[] items,Material type,int maximum) {
        int left=Math.max(0,maximum);List<ItemStack> drops=new ArrayList<>();
        for(int slot=0;slot<items.length;slot++) {
            ItemStack item=items[slot];if(item==null||item.getType()!=type)continue;
            int keep=Math.min(left,item.getAmount());left-=keep;
            if(keep<item.getAmount()) { ItemStack extra=item.clone();extra.setAmount(item.getAmount()-keep);drops.add(extra);
                if(keep==0)items[slot]=null;else {items[slot]=item.clone();items[slot].setAmount(keep);} }
        }return drops;
    }
    void enforce(Player player) {
        if(player.hasPermission("totemlimit.bypass")||player.isDead())return;
        ItemStack[] inventory=player.getInventory().getContents();ItemStack cursor=player.getItemOnCursor();
        ItemStack[] carried=Arrays.copyOf(inventory,inventory.length+1);carried[inventory.length]=cursor;
        List<ItemStack> excess=new ArrayList<>();
        for(Material type:List.of(Material.TOTEM_OF_UNDYING,Material.ENDER_PEARL))excess.addAll(trim(carried,type,limit(type)));
        if(excess.isEmpty())return;
        player.getInventory().setContents(Arrays.copyOf(carried,inventory.length));player.setItemOnCursor(carried[inventory.length]);
        for(ItemStack item:excess) { Item drop=player.getWorld().dropItem(player.getLocation(),item);drop.setPickupDelay(40); }
        player.sendMessage("Inventory limit: "+limit(Material.TOTEM_OF_UNDYING)+" totems, "+limit(Material.ENDER_PEARL)+" ender pearls. Excess dropped at your feet.");
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.hasPermission("totemlimit.bypass"))return;
        Material type=event.getItem().getItemStack().getType();
        if((type==Material.TOTEM_OF_UNDYING||type==Material.ENDER_PEARL)&&count(player.getInventory().getContents(),player.getItemOnCursor(),type)>=limit(type))event.setCancelled(true);
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("totemlimit.admin")){sender.sendMessage("No permission.");return true;}
        if(args.length!=1||!args[0].equalsIgnoreCase("reload"))return false;
        reloadConfig();getServer().getOnlinePlayers().forEach(this::enforce);sender.sendMessage("Limits reloaded.");return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String label,String[] args) {
        return sender.hasPermission("totemlimit.admin")&&args.length==1&&"reload".startsWith(args[0].toLowerCase(Locale.ROOT))?List.of("reload"):List.of();
    }
}
