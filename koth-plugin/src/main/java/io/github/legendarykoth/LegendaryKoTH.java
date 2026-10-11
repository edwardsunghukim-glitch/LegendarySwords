package io.github.legendarykoth;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.boss.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class LegendaryKoTH extends JavaPlugin implements Listener {
    public String getDisplayStatus(){
        if(!active)return "Inactive";
        if(capture.contested)return "Contested ("+capture.seconds+"/"+required()+"s)";
        return capture.holder==null?"Waiting":capture.seconds+"/"+required()+"s";
    }
    private final CaptureState capture=new CaptureState();
    private WorldGuardRegion regions;
    private BossBar bar;
    private boolean active;
    private World world;
    private Object selected;
    @Override public void onEnable() {
        saveDefaultConfig();
        try{regions=new WorldGuardRegion(getServer().getPluginManager().getPlugin("WorldGuard"));}
        catch(ReflectiveOperationException|RuntimeException ex){getLogger().severe("Cannot connect to WorldGuard 7: "+ex);getServer().getPluginManager().disablePlugin(this);return;}
        bar=Bukkit.createBossBar("KoTH",BarColor.YELLOW,BarStyle.SOLID);bar.setVisible(false);
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getScheduler().runTaskTimer(this,this::tick,20,20);
        if(getConfig().getBoolean("start-on-server-start",false))getServer().getScheduler().runTaskLater(this,()->start(getServer().getConsoleSender()),1);
    }
    @Override public void onDisable(){if(bar!=null)bar.removeAll();capture.reset();active=false;}
    private int required(){return Math.min(86400,Math.max(1,getConfig().getInt("capture.seconds",100)));}
    private void clearHolder(UUID player){if(player.equals(capture.holder))capture.reset();}
    @EventHandler public void died(PlayerDeathEvent e){clearHolder(e.getEntity().getUniqueId());}
    @EventHandler public void quit(PlayerQuitEvent e){clearHolder(e.getPlayer().getUniqueId());}
    @EventHandler public void changedWorld(PlayerChangedWorldEvent e){clearHolder(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void teleported(PlayerTeleportEvent e){moved(e);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void moved(PlayerMoveEvent e) {
        if(!active||!e.getPlayer().getUniqueId().equals(capture.holder)||e.getTo()==null)return;
        if(e.getFrom().getWorld()==e.getTo().getWorld()&&e.getFrom().getBlockX()==e.getTo().getBlockX()&&e.getFrom().getBlockY()==e.getTo().getBlockY()&&e.getFrom().getBlockZ()==e.getTo().getBlockZ())return;
        try{if(e.getTo().getWorld()!=world||!regions.contains(selected,e.getTo()))clearHolder(e.getPlayer().getUniqueId());}
        catch(ReflectiveOperationException ex){getLogger().severe("WorldGuard movement check failed: "+ex);stop(true);}
    }
    private boolean selectRegion(CommandSender sender) {
        String id=getConfig().getString("region.name","kothwin");String name=getConfig().getString("region.world","");
        List<World> candidates=name.isBlank()?getServer().getWorlds():Optional.ofNullable(Bukkit.getWorld(name)).map(List::of).orElse(List.of());
        World foundWorld=null;Object found=null;
        try {
            for(World w:candidates){Object r=regions.find(w,id);if(r!=null){if(found!=null){sender.sendMessage("Region exists in multiple worlds. Use /koth setregion "+id+" <world>.");return false;}foundWorld=w;found=r;}}
        }catch(ReflectiveOperationException ex){sender.sendMessage("WorldGuard lookup failed; check console.");getLogger().severe(ex.toString());return false;}
        if(found==null){sender.sendMessage("WorldGuard region '"+id+"' was not found. Use /koth setregion <region> <world>.");return false;}
        world=foundWorld;selected=found;return true;
    }
    private void start(CommandSender sender) {
        if(active){sender.sendMessage("KoTH is already running.");return;}
        if(!selectRegion(sender))return;
        capture.reset();active=true;bar.setVisible(true);broadcast("started", "&6KoTH started! Hold for %seconds% seconds.","");
    }
    private void stop(boolean announce) {
        active=false;capture.reset();bar.removeAll();bar.setVisible(false);
        if(announce)broadcast("stopped","&cKoTH stopped.","");
    }
    private boolean eligible(Player p) {
        if(!p.isOnline()||p.isDead()||p.getGameMode()==GameMode.SPECTATOR)return false;
        if(p.getGameMode()==GameMode.CREATIVE&&!getConfig().getBoolean("capture.allow-creative",false))return false;
        if(p.hasMetadata("vanished")&&p.getMetadata("vanished").stream().anyMatch(value->value.asBoolean()))return false;
        if(p.hasMetadata("legendaryadmin:vanished")&&p.getMetadata("legendaryadmin:vanished").stream().anyMatch(value->value.asBoolean()))return false;
        for(String perm:getConfig().getStringList("capture.excluded-permissions"))if(p.hasPermission(perm))return false;
        return true;
    }
    private void tick() {
        if(!active)return;
        Set<UUID> inside=new LinkedHashSet<>();
        try {
            // Refresh the region so WorldGuard edits/deletion are respected during an event.
            selected=regions.find(world,getConfig().getString("region.name","kothwin"));
            if(selected==null){getLogger().warning("KoTH region disappeared; event stopped.");stop(true);return;}
            for(Player p:world.getPlayers())if(eligible(p)&&regions.contains(selected,p.getLocation()))inside.add(p.getUniqueId());
        }catch(ReflectiveOperationException ex){getLogger().severe("WorldGuard region check failed: "+ex);stop(true);return;}
        UUID before=capture.holder;UUID winner=capture.tick(inside,required(),getConfig().getBoolean("capture.pause-when-contested",true));
        if(capture.holder!=null&&!capture.holder.equals(before)) {
            Player p=Bukkit.getPlayer(capture.holder);if(p!=null)broadcast("captured","&e%player% is capturing KoTH!",p.getName());
        }
        for(Player p:Bukkit.getOnlinePlayers())if(!bar.getPlayers().contains(p))bar.addPlayer(p);
        for(Player p:new ArrayList<>(bar.getPlayers()))if(!p.isOnline())bar.removePlayer(p);
        Player holder=capture.holder==null?null:Bukkit.getPlayer(capture.holder);
        bar.setTitle("KoTH | "+(capture.contested?"Contested":holder==null?"Waiting for a player":holder.getName())+" | "+capture.seconds+"/"+required()+"s");
        bar.setProgress(Math.min(1,(double)capture.seconds/required()));bar.setColor(capture.contested?BarColor.RED:BarColor.YELLOW);
        if(winner!=null) {
            Player p=Bukkit.getPlayer(winner);stop(false);
            if(p!=null){broadcast("won","&6%player% won KoTH!",p.getName());p.sendTitle(ChatColor.GOLD+"KoTH Winner!",ChatColor.YELLOW+"You held the hill.",10,60,10);
                for(String reward:getConfig().getStringList("rewards.commands")) {
                    String command=rewardCommand(reward,p.getName());if(command.isBlank())continue;
                    try{if(!Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command))getLogger().warning("Reward command returned false: "+command);}
                    catch(RuntimeException ex){getLogger().severe("Reward command failed: "+ex.getMessage());}
                }
            }
        }
    }
    static String rewardCommand(String command,String player) {
        String out=command.trim();if(out.startsWith("/"))out=out.substring(1);
        return out.replace("%player%",player).replace("{player}",player);
    }
    private void broadcast(String key,String fallback,String player) {
        String message=getConfig().getString("messages."+key,fallback).replace("%player%",player).replace("%seconds%",String.valueOf(required()));
        if(!message.isBlank())Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',message));
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(args.length==0)return List.of();
        List<String> choices=new ArrayList<>();boolean admin=sender.hasPermission("legendarykoth.admin");
        if(args.length==1){choices.add("status");if(admin)choices.addAll(List.of("start","stop","reload","setregion","setreward","reward"));}
        else if(admin) {
            String action=args[0].toLowerCase(Locale.ROOT);
            if(action.equals("setregion")&&args.length==2){choices.add("kothwin");choices.add(getConfig().getString("region.name","kothwin"));}
            else if(action.equals("setregion")&&args.length==3)for(World w:getServer().getWorlds())choices.add(w.getName());
            else if(action.equals("reward")&&args.length==2)choices.addAll(List.of("add","clear","list"));
            else {
                int offset=action.equals("setreward")?1:action.equals("reward")&&args.length>2&&args[1].equalsIgnoreCase("add")?2:-1;
                if(offset>0) {
                    int part=args.length-offset;
                    if(part==1)choices.addAll(List.of("killtokengive","give","xp"));
                    else if(part==2)choices.addAll(List.of("%player%","{player}"));
                    else if(part==3&&args[offset].equalsIgnoreCase("give"))choices.addAll(List.of("diamond","emerald","gold_ingot"));
                    else if(part==3&&args[offset].equalsIgnoreCase("killtokengive"))choices.addAll(List.of("1","10","100"));
                }
            }
        }
        return CommandSuggestions.matching(choices,args[args.length-1]);
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(args.length==0||args[0].equalsIgnoreCase("status")){
            sender.sendMessage("KoTH: "+(active?"running ("+capture.seconds+"/"+required()+"s)":"stopped")+" | region: "+getConfig().getString("region.name","kothwin"));return true;
        }
        if(!sender.hasPermission("legendarykoth.admin")){sender.sendMessage("No permission to manage KoTH.");return true;}
        String action=args[0].toLowerCase(Locale.ROOT);
        if(action.equals("start")&&args.length==1){start(sender);return true;}
        if(action.equals("stop")&&args.length==1){stop(true);return true;}
        if(action.equals("reload")&&args.length==1){stop(false);reloadConfig();sender.sendMessage("KoTH config reloaded. Use /koth start to begin.");return true;}
        if(action.equals("setregion")&&(args.length==2||args.length==3)) {
            String oldRegion=getConfig().getString("region.name"),oldWorld=getConfig().getString("region.world");
            getConfig().set("region.name",args[1].toLowerCase(Locale.ROOT));getConfig().set("region.world",args.length==3?args[2]:"");
            if(!selectRegion(sender)){getConfig().set("region.name",oldRegion);getConfig().set("region.world",oldWorld);return true;}
            stop(false);saveConfig();sender.sendMessage("KoTH region set to "+args[1]+" in "+world.getName()+".");return true;
        }
        if(action.equals("setreward")&&args.length>=2) {
            getConfig().set("rewards.commands",List.of(String.join(" ",Arrays.copyOfRange(args,1,args.length))));saveConfig();sender.sendMessage("Winner reward set.");return true;
        }
        if(action.equals("reward")&&args.length>=2) {
            String sub=args[1].toLowerCase(Locale.ROOT);List<String> rewards=new ArrayList<>(getConfig().getStringList("rewards.commands"));
            if(sub.equals("list")){sender.sendMessage("Reward commands: "+rewards);return true;}
            if(sub.equals("clear")&&args.length==2)rewards.clear();
            else if(sub.equals("add")&&args.length>=3)rewards.add(String.join(" ",Arrays.copyOfRange(args,2,args.length)));
            else return false;
            getConfig().set("rewards.commands",rewards);saveConfig();sender.sendMessage("Winner rewards saved.");return true;
        }
        return false;
    }
}
