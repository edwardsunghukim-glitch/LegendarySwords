package io.github.swordfoscoreboard;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitTask;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.*;

public final class SwordFoScoreboard extends JavaPlugin implements Listener {
    private final Map<UUID,Board> boards=new HashMap<>();
    private final Set<UUID> hidden=new HashSet<>();
    private BukkitTask task;
    static final class Board {
        Scoreboard previous,current;Objective objective;Team[] teams=new Team[15];String[] shown=new String[15];
    }
    @Override public void onEnable(){saveDefaultConfig();getServer().getPluginManager().registerEvents(this,this);start();}
    private void start(){if(task!=null)task.cancel();task=getServer().getScheduler().runTaskTimer(this,this::refresh,1,Math.max(10,Math.min(200,getConfig().getLong("refresh-ticks",20))));}
    static String entry(int slot){if(slot<0||slot>=15)throw new IllegalArgumentException();return "§"+Integer.toHexString(slot);}
    private Board create(Player p) {
        Board b=new Board();b.previous=p.getScoreboard();b.current=Bukkit.getScoreboardManager().getNewScoreboard();
        b.objective=b.current.registerNewObjective("swordfo",Criteria.DUMMY,DisplayText.component(getConfig().getString("title","&6&lSWORDFO")));
        b.objective.setDisplaySlot(DisplaySlot.SIDEBAR);b.objective.numberFormat(NumberFormat.blank());
        for(int i=0;i<15;i++){b.teams[i]=b.current.registerNewTeam("line"+i);b.teams[i].addEntry(entry(i));}
        p.setScoreboard(b.current);boards.put(p.getUniqueId(),b);return b;
    }
    private void refresh() {
        List<String> lines=getConfig().getStringList("lines");
        for(Player p:Bukkit.getOnlinePlayers()) {
            if(hidden.contains(p.getUniqueId()))continue;
            try {
            Board b=boards.get(p.getUniqueId());if(b==null)b=create(p);
            if(p.getScoreboard()!=b.current) {
                if(!getConfig().getBoolean("keep-visible",true))continue;
                p.setScoreboard(b.current);
            }
            var values=DisplayText.values(p,getConfig().getString("ip","swordfo.minehut.gg"));
            render(b,lines,values);
            } catch(RuntimeException ex) {
                // Keep one failed player/API call from terminating everyone's refresh timer.
                if(System.currentTimeMillis()-lastWarning>60000){lastWarning=System.currentTimeMillis();getLogger().log(java.util.logging.Level.WARNING,"Scoreboard update failed for "+p.getName(),ex);}
            }
        }
    }
    private long lastWarning;
    static void render(Board b,List<String> lines,Map<String,String> values) {
            for(int i=0;i<15;i++) {
                if(i>=lines.size()){if(b.shown[i]!=null)b.current.resetScores(entry(i));b.shown[i]=null;continue;}
                String text=DisplayText.replace(lines.get(i),values);
                if(!text.equals(b.shown[i])){b.teams[i].prefix(DisplayText.component(text));b.objective.getScore(entry(i)).setScore(15-i);b.shown[i]=text;}
            }
    }
    private void remove(Player p){Board b=boards.remove(p.getUniqueId());if(b!=null&&p.getScoreboard()==b.current)p.setScoreboard(b.previous);}
    @EventHandler public void quit(PlayerQuitEvent e){boards.remove(e.getPlayer().getUniqueId());hidden.remove(e.getPlayer().getUniqueId());}
    @Override public void onDisable(){if(task!=null)task.cancel();for(Player p:Bukkit.getOnlinePlayers())remove(p);boards.clear();hidden.clear();}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        return args.length==1&&sender.hasPermission("swordfoscoreboard.admin")?CommandSuggestions.matching(List.of("reload"),args[0]):List.of();
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(args.length==1&&args[0].equalsIgnoreCase("reload")) {
            if(!sender.hasPermission("swordfoscoreboard.admin")){sender.sendMessage("No permission.");return true;}
            for(Player p:Bukkit.getOnlinePlayers())remove(p);reloadConfig();start();sender.sendMessage("SwordFo scoreboard reloaded.");return true;
        }
        if(args.length!=0)return false;
        if(!(sender instanceof Player p)){sender.sendMessage("Use /swordboard reload.");return true;}
        if(hidden.add(p.getUniqueId())){remove(p);p.sendMessage("Scoreboard hidden.");}else{hidden.remove(p.getUniqueId());refresh();p.sendMessage("Scoreboard shown.");}
        return true;
    }
}
