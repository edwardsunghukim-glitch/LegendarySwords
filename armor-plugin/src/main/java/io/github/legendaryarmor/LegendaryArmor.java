package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class LegendaryArmor extends JavaPlugin implements Listener {
    private ArmorCatalog catalog;private ArmorCosmetics cosmetics;private ArmorAbilities abilities;private int ticks;
    private static final NamespacedKey SPEED=new NamespacedKey("legendaryarmor","set_speed"),RESIST=new NamespacedKey("legendaryarmor","set_resist");
    @Override public void onEnable(){saveDefaultConfig();catalog=new ArmorCatalog(this);cosmetics=new ArmorCosmetics(this);abilities=new ArmorAbilities(this,catalog);
        getServer().getPluginManager().registerEvents(this,this);getServer().getOnlinePlayers().forEach(this::clean);
        getServer().getScheduler().runTaskTimer(this,this::tick,2,2);
    }
    @Override public void onDisable(){if(abilities!=null)abilities.close();if(cosmetics!=null)cosmetics.close();getServer().getOnlinePlayers().forEach(this::clean);}
    private void clean(Player player){modifier(player,Attribute.MOVEMENT_SPEED,SPEED,0,AttributeModifier.Operation.ADD_SCALAR);modifier(player,Attribute.KNOCKBACK_RESISTANCE,RESIST,0,AttributeModifier.Operation.ADD_NUMBER);}
    private void modifier(Player player,Attribute attribute,NamespacedKey key,double amount,AttributeModifier.Operation operation) {
        updateModifier(player.getAttribute(attribute),key,amount,operation);
    }
    static void updateModifier(AttributeInstance instance,NamespacedKey key,double amount,AttributeModifier.Operation operation) {
        if(instance==null)return;
        var old=instance.getModifiers().stream().filter(m->m.getKey().equals(key)).findFirst().orElse(null);
        if(old!=null&&old.getAmount()==amount)return;if(old!=null)instance.removeModifier(old);
        if(amount!=0)instance.addModifier(new AttributeModifier(key,amount,operation));
    }
    private double setting(ArmorSet set,String name,double fallback){double value=getConfig().getDouble("sets."+set.id+"."+name,fallback);return Double.isFinite(value)?Math.clamp(value,0,100):fallback;}
    private ArmorSet worn(Player player){return player.isDead()||player.getGameMode()==GameMode.SPECTATOR?null:catalog.fullSet(player.getInventory().getArmorContents());}
    private void tick(){ticks+=2;for(Player player:getServer().getOnlinePlayers()) {
        ArmorSet set=worn(player);cosmetics.update(player,set);
        modifier(player,Attribute.MOVEMENT_SPEED,SPEED,set==ArmorSet.LEVIATHAN?Math.min(.5,setting(set,"speed-bonus",.08)):0,AttributeModifier.Operation.ADD_SCALAR);
        modifier(player,Attribute.KNOCKBACK_RESISTANCE,RESIST,set==ArmorSet.STONE?Math.min(1,setting(set,"knockback-resistance",.35)):0,AttributeModifier.Operation.ADD_NUMBER);
        if(set==ArmorSet.ANGEL&&ticks%80==0)ArmorAbilities.heal(player,setting(set,"passive-healing",1));
        if(set==ArmorSet.LEVIATHAN&&ticks%20==0&&player.isInWater()) {
            PotionEffect old=player.getPotionEffect(PotionEffectType.WATER_BREATHING);
            if(old==null||old.getDuration()<40)player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,60,0,true,false));
        }
    }}
    @EventHandler public void join(PlayerJoinEvent event){clean(event.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent event){clean(event.getPlayer());cosmetics.clear(event.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent event){clean(event.getEntity());cosmetics.clear(event.getEntity().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hurt(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player player))return;ArmorSet set=worn(player);
        if(set==ArmorSet.DRAGON&&Set.of(EntityDamageEvent.DamageCause.FIRE,EntityDamageEvent.DamageCause.FIRE_TICK,EntityDamageEvent.DamageCause.LAVA,EntityDamageEvent.DamageCause.HOT_FLOOR).contains(event.getCause()))event.setCancelled(true);
        if(set==ArmorSet.STONE)event.setDamage(event.getDamage()*(1-Math.min(.8,setting(set,"damage-reduction",.12))));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void retaliate(EntityDamageByEntityEvent event) {
        if(event.getFinalDamage()>0&&event.getEntity() instanceof Player player&&worn(player)==ArmorSet.FROST&&event.getDamager() instanceof LivingEntity attacker&&abilities.eligible(player,attacker))
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,0));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent event) {
        ArmorSet set=worn(event.getPlayer());if(set!=null&&event.getPlayer().isSneaking()&&event.getPlayer().hasPermission("legendaryarmor.use")) {event.setCancelled(true);abilities.activate(event.getPlayer(),set);}
    }
    static final class Menu implements InventoryHolder {Inventory inventory;@Override public Inventory getInventory(){return inventory;}}
    private void menu(Player player) {
        Menu menu=new Menu();menu.inventory=getServer().createInventory(menu,27,ArmorCatalog.text("Legendary Armor",0xffd15b));
        int[] slots={10,11,13,15,16};int index=0;
        for(ArmorSet set:ArmorSet.values()) {
            ItemStack item=catalog.create(set,3);var meta=item.getItemMeta();var lore=new ArrayList<>(meta.lore());
            lore.add(ArmorCatalog.text(player.hasPermission("legendaryarmor.admin")?"Click to receive all four pieces":"Ask an admin to obtain this set",0xffd15b));meta.lore(lore);item.setItemMeta(meta);menu.inventory.setItem(slots[index++],item);
        }player.openInventory(menu.inventory);
    }
    private void give(Player player,ArmorSet set,CommandSender sender) {
        int empty=0;for(ItemStack item:player.getInventory().getStorageContents())if(item==null||item.getType().isAir())empty++;
        if(empty<4){sender.sendMessage("The player needs four empty inventory slots.");return;}
        for(int i=0;i<4;i++)player.getInventory().addItem(catalog.create(set,i));sender.sendMessage("Gave "+set.title+" to "+player.getName()+".");
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu))return;event.setCancelled(true);
        if(event.getWhoClicked() instanceof Player player&&player.hasPermission("legendaryarmor.admin")&&event.getRawSlot()>=0&&event.getRawSlot()<27) {
            ArmorSet set=catalog.identify(event.getCurrentItem());if(set!=null)give(player,set,player);
        }
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof Menu)event.setCancelled(true);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(args.length==1&&args[0].equalsIgnoreCase("reload")) {
            if(!sender.hasPermission("legendaryarmor.admin")){sender.sendMessage("No permission.");return true;}
            reloadConfig();abilities.close();cosmetics.close();getServer().getOnlinePlayers().forEach(this::clean);sender.sendMessage("Armor config reloaded.");return true;
        }
        if(args.length==3&&args[0].equalsIgnoreCase("give")) {
            if(!sender.hasPermission("legendaryarmor.admin")){sender.sendMessage("No permission.");return true;}
            Player target=getServer().getPlayerExact(args[1]);ArmorSet set=ArmorSet.find(args[2]).orElse(null);
            if(target==null||set==null){sender.sendMessage("Use an online player and angel, dragon, leviathan, stone or frost.");return true;}
            give(target,set,sender);return true;
        }
        if(!(sender instanceof Player player)){sender.sendMessage("Use /armor give <player> <set> or /armor reload.");return true;}
        if(!sender.hasPermission("legendaryarmor.use")){sender.sendMessage("No permission.");return true;}
        if(args.length==0){menu(player);return true;}
        if(args.length==1&&args[0].equalsIgnoreCase("ability")) {ArmorSet set=worn(player);if(set==null)player.sendMessage("Wear all four pieces of one armor set.");else abilities.activate(player,set);return true;}
        return false;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String label,String[] args) {
        List<String> choices=new ArrayList<>();if(args.length==1){if(sender.hasPermission("legendaryarmor.use"))choices.add("ability");if(sender.hasPermission("legendaryarmor.admin"))choices.addAll(List.of("give","reload"));}
        if(sender.hasPermission("legendaryarmor.admin")&&args.length>1&&args[0].equalsIgnoreCase("give")) {
            if(args.length==2)for(Player player:getServer().getOnlinePlayers())if(!(sender instanceof Player viewer)||viewer.canSee(player))choices.add(player.getName());
            if(args.length==3)for(ArmorSet set:ArmorSet.values())choices.add(set.id);
        }String prefix=args.length==0?"":args[args.length-1].toLowerCase(Locale.ROOT);return choices.stream().filter(c->c.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
