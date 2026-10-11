package io.github.killtokens;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.block.Crafter;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.io.IOException;
import java.util.*;

public final class KillTokens extends JavaPlugin implements Listener {
    public long getWalletBalance(UUID player){return bank==null?0:bank.balance(player);}
    public long getCombatSeconds(UUID player){return Math.max(0,(combat.getOrDefault(player,0L)-System.currentTimeMillis()+999)/1000);}
    private NamespacedKey marker;
    private TokenBank bank;
    private final Map<UUID,Long> combat=new HashMap<>();
    private final Map<String,Long> lastRewards=new HashMap<>();
    private final Map<UUID,UUID> opponents=new HashMap<>();
    @Override public void onEnable() {
        saveDefaultConfig();marker=new NamespacedKey(this,"killtoken");
        try{bank=new TokenBank(getDataFolder().toPath().resolve("balances.yml"));}
        catch(IOException|org.bukkit.configuration.InvalidConfigurationException ex){getLogger().severe("Cannot load token balances: "+ex.getMessage());getServer().getPluginManager().disablePlugin(this);return;}
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getScheduler().runTaskTimer(this,this::tick,20,20);
    }
    private void tick() {
        long now=System.currentTimeMillis();
        for(var entry:new HashMap<>(combat).entrySet()) {
            Player p=Bukkit.getPlayer(entry.getKey());
            if(entry.getValue()<=now){combat.remove(entry.getKey());opponents.remove(entry.getKey());if(p!=null)p.sendActionBar(Component.text("Combat ended",NamedTextColor.GREEN));}
            else if(p!=null&&getConfig().getBoolean("combat.action-bar",true))p.sendActionBar(Component.text("Combat: "+((entry.getValue()-now+999)/1000)+"s",NamedTextColor.RED));
        }
        lastRewards.entrySet().removeIf(e->e.getValue()<now);
    }
    private boolean isToken(ItemStack item) {
        return item!=null&&item.getType()==Material.SUNFLOWER&&item.hasItemMeta()
            &&item.getItemMeta().getPersistentDataContainer().has(marker,PersistentDataType.BYTE);
    }
    private ItemStack token() {
        ItemStack item=new ItemStack(Material.SUNFLOWER);ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(color(getConfig().getString("token.name","&6&lKillToken")));
        meta.setLore(getConfig().getStringList("token.lore").stream().map(KillTokens::color).toList());
        meta.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;
    }
    private static String color(String text){return ChatColor.translateAlternateColorCodes('&',text);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void place(BlockPlaceEvent e) {
        if(isToken(e.getItemInHand())){e.setCancelled(true);e.getPlayer().sendMessage("KillTokens cannot be placed. Use /deposit.");}
    }
    @EventHandler public void crafting(PrepareItemCraftEvent e) {
        for(ItemStack item:e.getInventory().getMatrix())if(isToken(item)){e.getInventory().setResult(null);return;}
    }
    @EventHandler(ignoreCancelled=true) public void automaticCraft(CrafterCraftEvent e) {
        if(e.getBlock().getState() instanceof Crafter crafter)
            for(ItemStack item:crafter.getInventory().getContents())if(isToken(item)){e.setCancelled(true);return;}
    }
    private boolean tagged(Player p){return combat.getOrDefault(p.getUniqueId(),0L)>System.currentTimeMillis();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void hit(EntityDamageByEntityEvent e) {
        if(!getConfig().getBoolean("combat.enabled",true)||e.getFinalDamage()<=0||!(e.getEntity() instanceof Player victim))return;
        Player attacker=e.getDamager() instanceof Player p?p:e.getDamager() instanceof Projectile projectile&&projectile.getShooter() instanceof Player p?p:null;
        if(attacker==null||attacker.getUniqueId().equals(victim.getUniqueId()))return;
        long until=System.currentTimeMillis()+Math.min(3600,Math.max(1,getConfig().getLong("combat.duration-seconds",20)))*1000L;
        for(Player p:List.of(attacker,victim))if(!p.hasPermission("killtokens.combat-bypass"))combat.put(p.getUniqueId(),until);
        opponents.put(attacker.getUniqueId(),victim.getUniqueId());opponents.put(victim.getUniqueId(),attacker.getUniqueId());
    }
    static String commandRoot(String message) {
        String root=message.toLowerCase(Locale.ROOT).split("\\s+",2)[0];if(root.startsWith("/"))root=root.substring(1);
        int colon=root.lastIndexOf(':');return colon<0?root:root.substring(colon+1);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void command(PlayerCommandPreprocessEvent e) {
        if(!tagged(e.getPlayer()))return;String root=commandRoot(e.getMessage());
        if(getConfig().getStringList("combat.blocked-commands").stream().map(KillTokens::commandRoot).anyMatch(root::equals)) {
            e.setCancelled(true);e.getPlayer().sendMessage("You cannot use that command during combat.");
        }
    }
    @EventHandler public void quit(PlayerQuitEvent e) {
        Player p=e.getPlayer();
        if(tagged(p)&&getConfig().getBoolean("combat.kill-on-logout",false)&&!p.isDead())p.setHealth(0);
        combat.remove(p.getUniqueId());opponents.remove(p.getUniqueId());
    }
    @EventHandler(priority=EventPriority.MONITOR) public void died(PlayerDeathEvent e) {
        Player victim=e.getEntity(),killer=victim.getKiller();combat.remove(victim.getUniqueId());
        opponents.remove(victim.getUniqueId());
        if(killer==null||killer.getUniqueId().equals(victim.getUniqueId()))return;
        if(getConfig().getStringList("rewards.excluded-worlds").contains(victim.getWorld().getName()))return;
        long now=System.currentTimeMillis();String pair=killer.getUniqueId()+":"+victim.getUniqueId();
        if(lastRewards.getOrDefault(pair,0L)>now){killer.sendMessage("No KillTokens: this opponent was rewarded recently.");return;}
        double factor=bank.activeMultiplier(now)*safeFactor(getConfig().getDouble("rewards.base-multiplier",1));
        double permissionFactor=1;var perms=getConfig().getConfigurationSection("rewards.permission-multipliers");
        if(perms!=null)for(var entry:perms.getValues(false).entrySet())if(entry.getValue() instanceof Number number&&killer.hasPermission(entry.getKey()))permissionFactor=Math.max(permissionFactor,safeFactor(number.doubleValue()));
        int cap=Math.min(2304,Math.max(1,getConfig().getInt("rewards.max-tokens-per-kill",2304)));
        int count=rewardAmount(getConfig().getInt("rewards.tokens-per-kill",1),factor,permissionFactor,cap);if(count<=0)return;
        int left=count;
        while(left>0){ItemStack drop=token();int stack=Math.min(left,drop.getMaxStackSize());drop.setAmount(stack);
            if(e.getKeepInventory())victim.getWorld().dropItem(victim.getLocation(),drop);else e.getDrops().add(drop);
            left-=stack;
        }
        long cooldown=Math.min(86400,Math.max(0,getConfig().getLong("rewards.same-victim-cooldown-seconds",0)));
        lastRewards.put(pair,now+cooldown*1000);
        killer.sendMessage(color("&6"+count+" KillToken"+(count==1?"":"s")+" dropped by "+victim.getName()+"."));
    }
    private static double safeFactor(double value){return Double.isFinite(value)?Math.min(100,Math.max(0,value)):1;}
    static int rewardAmount(int base,double global,double permission,int cap) {
        return (int)Math.min(cap,Math.floor(Math.max(0,base)*(Double.isFinite(global)?Math.max(0,global):1)*safeFactor(permission)));
    }
    private int limit(){return Math.min(2304,Math.max(1,getConfig().getInt("banking.max-per-transaction",2304)));}
    private void failed(CommandSender sender,IOException ex){getLogger().severe("Token save failed: "+ex.getMessage());sender.sendMessage("Token storage is unavailable. No transaction was completed.");}
    private boolean manageWallet(CommandSender sender,boolean set,String[] args) {
        if(!sender.hasPermission("killtokens.admin")){sender.sendMessage("No permission to manage KillTokens.");return true;}
        if(args.length!=2){sender.sendMessage("Use /"+(set?"killtokenset":"killtokengive")+" <player> <amount>");return true;}
        long amount;
        try{amount=Long.parseLong(args[1]);if(amount<0||(!set&&amount==0))throw new NumberFormatException();}
        catch(NumberFormatException ex){sender.sendMessage("Use a positive whole amount (set also accepts zero).");return true;}
        OfflinePlayer target=Bukkit.getOfflinePlayerIfCached(args[0]);
        if(target==null){sender.sendMessage("That player must have joined this server before.");return true;}
        try {
            long delta=set?amount-bank.balance(target.getUniqueId()):amount;
            if(!bank.change(target.getUniqueId(),delta)){sender.sendMessage("Wallet limit reached.");return true;}
            sender.sendMessage("Wallet for "+target.getName()+": "+bank.balance(target.getUniqueId())+" KillTokens.");
            if(target.getPlayer()!=null)target.getPlayer().sendMessage("Your KillToken wallet is now "+bank.balance(target.getUniqueId())+".");
        }catch(IOException ex){failed(sender,ex);}return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(args.length==0)return List.of();
        boolean admin=sender.hasPermission("killtokens.admin"),use=sender.hasPermission("killtokens.use");
        String name=command.getName();List<String> choices=new ArrayList<>();
        if(name.equals("deposit")||name.equals("withdraw")) {
            if(use&&args.length==1)choices.addAll(List.of("all","1","10","64"));
        } else if(name.equals("killtokengive")||name.equals("killtokenset")) {
            if(admin&&args.length==1)for(Player p:getServer().getOnlinePlayers())if(!(sender instanceof Player viewer)||viewer.canSee(p))choices.add(p.getName());
            if(admin&&args.length==2)choices.addAll(List.of("1","10","100"));
            if(admin&&args.length==2&&name.equals("killtokenset"))choices.add("0");
        } else if(name.equals("killtokens")) {
            if(args.length==1){if(use)choices.add("balance");if(admin)choices.addAll(List.of("give","set","reload","multiplier"));}
            else if(admin) {
                String action=args[0].toLowerCase(Locale.ROOT);
                if(args.length==2&&Set.of("give","set").contains(action)){for(Player p:getServer().getOnlinePlayers())if(!(sender instanceof Player viewer)||viewer.canSee(p))choices.add(p.getName());}
                else if(args.length==2&&action.equals("multiplier"))choices.addAll(List.of("off","1","2","3","5","10"));
                else if(args.length==3&&action.equals("multiplier"))choices.addAll(List.of("0","15","30","60"));
                else if(args.length==3&&Set.of("give","set").contains(action)){choices.addAll(List.of("1","10","100"));if(action.equals("set"))choices.add("0");}
            }
        }
        return CommandSuggestions.matching(choices,args[args.length-1]);
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        String command=cmd.getName();
        if(command.equals("killtokengive")||command.equals("killtokenset"))return manageWallet(sender,command.equals("killtokenset"),args);
        if(command.equals("killtokens")&&args.length>0&&(args[0].equalsIgnoreCase("give")||args[0].equalsIgnoreCase("set")))
            return manageWallet(sender,args[0].equalsIgnoreCase("set"),Arrays.copyOfRange(args,1,args.length));
        if(command.equals("killtokens")&&args.length>0&&!args[0].equalsIgnoreCase("balance")) {
            if(!sender.hasPermission("killtokens.admin")){sender.sendMessage("No permission to manage KillTokens.");return true;}
            if(args[0].equalsIgnoreCase("reload")&&args.length==1){reloadConfig();if(!getConfig().getBoolean("combat.enabled",true)){combat.clear();opponents.clear();}sender.sendMessage("KillTokens config reloaded.");return true;}
            if(args[0].equalsIgnoreCase("multiplier")&&args.length>=2&&args.length<=3) {
                try {
                    double factor=args[1].equalsIgnoreCase("off")?1:Double.parseDouble(args[1]);
                    long minutes=args.length==3?Long.parseLong(args[2]):0;
                    if(!Double.isFinite(factor)||factor<1||factor>100||minutes<0||minutes>10080)throw new IllegalArgumentException();
                    bank.activate(factor,minutes==0?0:System.currentTimeMillis()+minutes*60000L);
                    Bukkit.broadcastMessage(color("&6KillToken multiplier: "+factor+"x"+(minutes>0?" for "+minutes+" minutes":"")));
                } catch(IllegalArgumentException ex){sender.sendMessage("Use /killtokens multiplier <1–100|off> [minutes: 0–10080]");}catch(IOException ex){failed(sender,ex);}return true;
            }
            return false;
        }
        if(!(sender instanceof Player p)){sender.sendMessage("Use banking commands in game.");return true;}
        if(!p.hasPermission("killtokens.use")){p.sendMessage("No permission.");return true;}
        if(command.equals("combat")){p.sendMessage("Combat timer: "+Math.max(0,(combat.getOrDefault(p.getUniqueId(),0L)-System.currentTimeMillis()+999)/1000)+" seconds.");return true;}
        if(command.equals("killtokens")||command.equals("wallet")){p.sendMessage(color("&6Banked KillTokens: "+bank.balance(p.getUniqueId())+" &7| Multiplier: "+bank.activeMultiplier(System.currentTimeMillis())+"x"));return true;}
        if(args.length>1)return false;
        boolean deposit=command.equals("deposit");long available=deposit?TokenInventory.count(p.getInventory().getContents(),this::isToken):bank.balance(p.getUniqueId());
        long amount;
        try{amount=args.length==0?(deposit?available:1):args[0].equalsIgnoreCase("all")?(deposit?available:Math.min(available,limit())):Long.parseLong(args[0]);}
        catch(NumberFormatException ex){p.sendMessage("Use a whole token amount or all.");return true;}
        if(amount<=0||(!deposit&&amount>limit())||amount>available){p.sendMessage("Not enough tokens, or invalid amount. Withdrawal maximum: "+limit());return true;}
        ItemStack template=token();
        TokenInventory.Grant grant=deposit?null:TokenInventory.giveWithOverflow(p.getInventory().getStorageContents(),(int)amount,template,this::isToken);
        ItemStack[] planned=deposit?TokenInventory.take(p.getInventory().getContents(),amount,this::isToken):grant.items();
        if(planned==null){p.sendMessage("Not enough physical KillTokens.");return true;}
        try {
            if(!bank.change(p.getUniqueId(),deposit?amount:-amount)){p.sendMessage("Transaction rejected: balance limit.");return true;}
            if(deposit)p.getInventory().setContents(planned);else p.getInventory().setStorageContents(planned);
            if(!deposit&&grant.overflow()>0) {
                int left=grant.overflow();
                while(left>0){ItemStack drop=template.clone();int stack=Math.min(left,drop.getMaxStackSize());drop.setAmount(stack);p.getWorld().dropItem(p.getLocation(),drop);left-=stack;}
                p.sendMessage(grant.overflow()+" KillTokens dropped at your feet because your inventory was full.");
            }
            p.sendMessage(color("&6"+(deposit?"Deposited ":"Withdrew ")+amount+" KillTokens. Balance: "+bank.balance(p.getUniqueId())));
        }catch(IOException ex){failed(p,ex);}return true;
    }
}
