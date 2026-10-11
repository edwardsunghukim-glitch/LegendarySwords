package io.github.legendarykits;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;

public final class LegendaryKits extends JavaPlugin implements Listener {
    private final Map<String,Kit> kits=new LinkedHashMap<>();
    private final Map<String,Long> claims=new HashMap<>();
    private File file;
    private static final class Kit {
        String id, permission=""; long cooldown=3600; ItemStack icon=new ItemStack(Material.CHEST);
        ItemStack[] items=new ItemStack[41];
        Kit(String id) { this.id=id; }
    }
    private static final class Menu implements InventoryHolder {
        final UUID owner; final String editing; final int page; final Map<Integer,String> slots=new HashMap<>();
        Inventory inventory; boolean deleteArmed;
        Menu(UUID owner,String editing,int page) { this.owner=owner;this.editing=editing;this.page=page; }
        @Override public Inventory getInventory() { return inventory; }
    }
    @Override public void onEnable() {
        getDataFolder().mkdirs(); file=new File(getDataFolder(),"kits.yml");
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        var section=yaml.getConfigurationSection("kits");
        if (section!=null) for (String id:section.getKeys(false)) {
            Kit k=new Kit(id); String path="kits."+id;
            k.cooldown=Math.min(31536000,Math.max(0,yaml.getLong(path+".cooldown",3600))); k.permission=yaml.getString(path+".permission","");
            ItemStack icon=yaml.getItemStack(path+".icon"); if (!KitItems.empty(icon)) k.icon=icon;
            for (int i=0;i<41;i++) k.items[i]=yaml.getItemStack(path+".items."+i);
            kits.put(id,k);
        }
        var times=yaml.getConfigurationSection("claims");
        if (times!=null) for(String key:times.getKeys(false)) claims.put(key,times.getLong(key));
        if (kits.isEmpty() && !file.exists()) {
            Kit starter=new Kit("starter"); starter.items[0]=new ItemStack(Material.IRON_SWORD);
            starter.items[1]=new ItemStack(Material.COOKED_BEEF,16); starter.icon=new ItemStack(Material.IRON_SWORD); kits.put(starter.id,starter); save();
        }
        getServer().getPluginManager().registerEvents(this,this);
    }
    private boolean save() {
        YamlConfiguration out=new YamlConfiguration();
        for(Kit k:kits.values()) {
            String path="kits."+k.id; out.set(path+".cooldown",k.cooldown);out.set(path+".permission",k.permission);out.set(path+".icon",k.icon);
            for(int i=0;i<41;i++) out.set(path+".items."+i,k.items[i]);
        }
        claims.forEach((key,value)->out.set("claims."+key,value));
        try { out.save(file); return true; } catch(IOException ex) {getLogger().severe("Cannot save kits: "+ex.getMessage());return false;}
    }
    @Override public void onDisable() {
        for(Player p:Bukkit.getOnlinePlayers()) if(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
        save();
    }
    private static ItemStack button(Material material,String name,String... lore) {
        ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta(); meta.setDisplayName(name);meta.setLore(List.of(lore)); item.setItemMeta(meta);return item;
    }
    private void browse(Player p,int requested) {
        List<Kit> list=new ArrayList<>(kits.values());int page=Math.max(0,Math.min(requested,Math.max(0,(list.size()-1)/45)));
        Menu m=new Menu(p.getUniqueId(),null,page);m.inventory=Bukkit.createInventory(m,54,"Kits — page "+(page+1));
        for(int i=page*45;i<Math.min(list.size(),page*45+45);i++) {
            Kit k=list.get(i);int slot=i%45;ItemStack icon=k.icon.clone();ItemMeta meta=icon.getItemMeta();
            meta.setDisplayName(k.id);List<String> lore=new ArrayList<>(List.of("Left-click: claim","Cooldown: "+k.cooldown+" seconds"));
            if(!k.permission.isEmpty()) lore.add("Permission: "+k.permission);
            if(p.hasPermission("legendarykits.admin")) lore.add("Right-click: edit");
            meta.setLore(lore);icon.setItemMeta(meta);m.inventory.setItem(slot,icon);m.slots.put(slot,k.id);
        }
        m.inventory.setItem(45,button(Material.ARROW,"Previous page"));m.inventory.setItem(53,button(Material.ARROW,"Next page"));
        p.openInventory(m.inventory);
    }
    private void edit(Player p,Kit k) {
        Menu m=new Menu(p.getUniqueId(),k.id,0);m.inventory=Bukkit.createInventory(m,54,"Edit kit: "+k.id);
        for(int i=0;i<41;i++) if(!KitItems.empty(k.items[i])) m.inventory.setItem(i,k.items[i].clone());
        for(int i=41;i<54;i++) m.inventory.setItem(i,button(Material.GRAY_STAINED_GLASS_PANE," "));
        m.inventory.setItem(45,button(Material.LIME_WOOL,"Save kit","Slots 0–35: items","36: boots, 37: leggings","38: chestplate, 39: helmet","40: offhand"));
        m.inventory.setItem(46,button(Material.RED_WOOL,"Cancel"));
        m.inventory.setItem(47,button(Material.CLOCK,"Cooldown: "+k.cooldown+"s","Left: +60 seconds; right: -60","Shift-click: zero cooldown"));
        m.inventory.setItem(48,button(Material.PAPER,"Kit permission",k.permission.isEmpty()?"Public kit":k.permission,"Use /kits permission "+k.id+" <node|none>"));
        m.inventory.setItem(49,button(Material.ITEM_FRAME,"Set icon","Put the desired icon on your cursor","Then click here"));
        m.inventory.setItem(50,button(Material.BARRIER,"Delete kit","Click twice to confirm"));
        p.openInventory(m.inventory);
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m)) return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(m.owner)) return;
        int slot=e.getRawSlot(); if(slot<0) return;
        if(m.editing==null) {
            if(slot==45) browse(p,m.page-1); else if(slot==53) browse(p,m.page+1);
            else if(m.slots.containsKey(slot)) {
                Kit k=kits.get(m.slots.get(slot));if(k==null)return;
                if(e.isRightClick() && p.hasPermission("legendarykits.admin")) edit(p,k);
                else if(e.isLeftClick()) claim(p,k);
            }
            return;
        }
        if(!p.hasPermission("legendarykits.admin")) {p.closeInventory();return;}
        Kit k=kits.get(m.editing);if(k==null){p.closeInventory();return;}
        if(slot<41 || slot>=54) {
            // Prevent shift moves and collection from reaching the editor's controls.
            if(!e.isShiftClick() && e.getAction()!=InventoryAction.COLLECT_TO_CURSOR) e.setCancelled(false);
            return;
        }
        if(slot==45) {
            ItemStack[] previous=k.items;k.items=KitItems.copy(Arrays.copyOf(m.inventory.getContents(),41));
            if(!save()){k.items=previous;p.sendMessage("Save failed. Please try again.");return;}
            p.sendMessage("Saved kit "+k.id);browse(p,0);
        } else if(slot==46) browse(p,0);
        else if(slot==47) {
            long old=k.cooldown;k.cooldown=e.isShiftClick()?0:Math.min(31536000,Math.max(0,k.cooldown+(e.isRightClick()?-60:60)));
            if(!save()) k.cooldown=old;
            m.inventory.setItem(47,button(Material.CLOCK,"Cooldown: "+k.cooldown+"s","Left: +60; right: -60; shift: zero"));
        } else if(slot==49 && !KitItems.empty(e.getCursor())) {
            ItemStack old=k.icon;k.icon=e.getCursor().clone();k.icon.setAmount(1);if(!save())k.icon=old;p.sendMessage("Icon updated.");
        } else if(slot==50) {
            if(!m.deleteArmed){m.deleteArmed=true;m.inventory.setItem(50,button(Material.BARRIER,"Click again to DELETE "+k.id));}
            else {kits.remove(k.id);if(!save())kits.put(k.id,k);else {p.sendMessage("Deleted kit "+k.id);browse(p,0);}}
        }
    }
    @EventHandler public void drag(InventoryDragEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;
        boolean editor=m.editing!=null && e.getWhoClicked().hasPermission("legendarykits.admin") && e.getWhoClicked().getUniqueId().equals(m.owner);
        if(!editor || e.getRawSlots().stream().anyMatch(i->i>=41 && i<54))e.setCancelled(true);
    }
    private void claim(Player p,Kit k) {
        if(!p.hasPermission("legendarykits.use") || (!k.permission.isEmpty()&&!p.hasPermission(k.permission))) {p.sendMessage("You do not have permission for this kit.");return;}
        String key=p.getUniqueId()+"_"+k.id;long now=System.currentTimeMillis();long last=claims.getOrDefault(key,0L);
        long wait=k.cooldown*1000-(now-last);if(wait>0){p.sendMessage("Wait "+((wait+999)/1000)+" seconds before claiming this kit again.");return;}
        PlayerInventory inv=p.getInventory();ItemStack[] armor=KitItems.copy(inv.getArmorContents());ItemStack off=inv.getItemInOffHand().clone();List<ItemStack> extra=new ArrayList<>();
        for(int i=0;i<36;i++)extra.add(k.items[i]);
        for(int i=0;i<4;i++)if(!KitItems.empty(k.items[36+i])) {
            if(KitItems.empty(armor[i]))armor[i]=k.items[36+i].clone();else extra.add(k.items[36+i]);
        }
        if(!KitItems.empty(k.items[40])) {if(KitItems.empty(off))off=k.items[40].clone();else extra.add(k.items[40]);}
        ItemStack[] storage=KitItems.insert(inv.getStorageContents(),extra);
        if(storage==null){p.sendMessage("Make room in your inventory first. Nothing was given.");return;}
        claims.put(key,now);if(!save()){if(last==0)claims.remove(key);else claims.put(key,last);p.sendMessage("Kit storage is unavailable. Try again later.");return;}
        inv.setStorageContents(storage);inv.setArmorContents(armor);inv.setItemInOffHand(off);p.sendMessage("Claimed kit "+k.id+"!");
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if(!(sender instanceof Player p)){sender.sendMessage("Use this command in game.");return true;}
        if(args.length==0){if(p.hasPermission("legendarykits.use"))browse(p,0);else p.sendMessage("No permission.");return true;}
        String action=args[0].toLowerCase(Locale.ROOT);
        if(Set.of("create","cooldown","permission").contains(action)) {
            if(!p.hasPermission("legendarykits.admin")){p.sendMessage("No permission to edit kits.");return true;}
            if(args.length<2)return false;String id=args[1].toLowerCase(Locale.ROOT);
            if(!id.matches("[a-z0-9_-]{1,32}")){p.sendMessage("Use a name with 1–32 letters, numbers, underscores or dashes.");return true;}
            if(action.equals("create")) {
                if(kits.containsKey(id)){p.sendMessage("That kit already exists.");return true;}
                Kit k=new Kit(id);k.items=KitItems.copy(p.getInventory().getContents());
                if(!KitItems.empty(p.getInventory().getItemInMainHand())){k.icon=p.getInventory().getItemInMainHand().clone();k.icon.setAmount(1);}
                kits.put(id,k);if(!save()){kits.remove(id);p.sendMessage("Save failed.");return true;}edit(p,k);return true;
            }
            Kit k=kits.get(id);if(k==null){p.sendMessage("Unknown kit.");return true;}if(args.length!=3)return false;
            if(action.equals("cooldown")) {
                try {long value=Long.parseLong(args[2]);if(value<0||value>31536000)throw new NumberFormatException();long old=k.cooldown;k.cooldown=value;if(!save())k.cooldown=old;}
                catch(NumberFormatException ex){p.sendMessage("Cooldown must be 0–31536000 seconds.");return true;}
            } else {String old=k.permission;k.permission=args[2].equalsIgnoreCase("none")?"":args[2];if(!save())k.permission=old;}
            p.sendMessage("Kit settings updated.");return true;
        }
        Kit k=kits.get(action);if(k==null){p.sendMessage("Unknown kit. Use /kits.");return true;}claim(p,k);return true;
    }
}
