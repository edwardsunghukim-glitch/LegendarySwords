package io.github.swordfotab;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import java.util.*;

public final class SwordFoTab extends JavaPlugin {
    private BukkitTask task;
    @Override public void onEnable(){saveDefaultConfig();start();}
    private void start(){if(task!=null)task.cancel();task=getServer().getScheduler().runTaskTimer(this,this::refresh,1,Math.max(10,Math.min(200,getConfig().getLong("refresh-ticks",40))));}
    private void refresh() {
        String header=String.join("\n",getConfig().getStringList("header"));String footer=String.join("\n",getConfig().getStringList("footer"));
        for(Player p:Bukkit.getOnlinePlayers()) {
            var values=DisplayText.values(p,getConfig().getString("ip","swordfo.minehut.gg"));
            p.sendPlayerListHeaderAndFooter(DisplayText.component(DisplayText.replace(header,values)),DisplayText.component(DisplayText.replace(footer,values)));
        }
    }
    @Override public void onDisable(){if(task!=null)task.cancel();for(Player p:Bukkit.getOnlinePlayers())p.sendPlayerListHeaderAndFooter(Component.empty(),Component.empty());}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(args.length!=1||!args[0].equalsIgnoreCase("reload"))return false;
        if(!sender.hasPermission("swordfotab.admin")){sender.sendMessage("No permission.");return true;}
        reloadConfig();start();sender.sendMessage("SwordFo tab list reloaded.");return true;
    }
}
