package io.github.legendarykits;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;

public final class LegendaryKits extends JavaPlugin implements Listener {
    private final Map<String,Kit> kits=new LinkedHashMap<>();
    private final Map<String,Long> claims=new HashMap<>();
    private File file;
    private String defaultKit="";
    static final int MENU_SIZE=27;
    private static final class Kit {
        String id, permission=""; int slot=-1; long cooldown=3600; ItemStack icon=new ItemStack(Material.CHEST);
        ItemStack[] items=new ItemStack[41];
        Kit(String id) { this.id=id; }
    }
    private static final class Menu implements InventoryHolder {
        final UUID owner; final String editing; final Map<Integer,String> slots=new HashMap<>();
        Inventory inventory; boolean deleteArmed;
        Menu(UUID owner,String editing) { this.owner=owner;this.editing=editing; }
        @Override public Inventory getInventory() { return inventory; }
    }
    @Override public void onEnable() {
        getDataFolder().mkdirs(); file=new File(getDataFolder(),"kits.yml");
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        defaultKit=yaml.getString("default-kit", "");
        var section=yaml.getConfigurationSection("kits");
        if (section!=null) for (String id:section.getKeys(false)) {
            Kit k=new Kit(id); String path="kits."+id;
            k.cooldown=Math.min(31536000,Math.max(0,yaml.getLong(path+".cooldown",3600))); k.permission=yaml.getString(path+".permission","");
            ItemStack icon=yaml.getItemStack(path+".icon"); if (!KitItems.empty(icon)) k.icon=icon;
            for (int i=0;i<41;i++) k.items[i]=yaml.getItemStack(path+".items."+i);
            k.slot=yaml.getInt(path+".slot",-1);
            kits.put(id,k);
        }
        var times=yaml.getConfigurationSection("claims");
        if (times!=null) for(String key:times.getKeys(false)) claims.put(key,times.getLong(key));
        if (kits.isEmpty() && !file.exists()) {
            Kit starter=new Kit("starter"); starter.items[0]=new ItemStack(Material.IRON_SWORD);
            starter.items[1]=new ItemStack(Material.COOKED_BEEF,16); starter.icon=new ItemStack(Material.IRON_SWORD); kits.put(starter.id,starter); save();
        }
        applySlots(KitSlots.normalize(slotMap(),MENU_SIZE));
        getServer().getPluginManager().registerEvents(this,this);
    }
    private boolean save() {
        YamlConfiguration out=new YamlConfiguration();
        out.set("default-kit",defaultKit);
        for(Kit k:kits.values()) {
            String path="kits."+k.id; out.set(path+".slot",k.slot);out.set(path+".cooldown",k.cooldown);out.set(path+".permission",k.permission);out.set(path+".icon",k.icon);
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
    private Map<String,Integer> slotMap() {
        Map<String,Integer> slots=new LinkedHashMap<>();kits.forEach((id,k)->slots.put(id,k.slot));return slots;
    }
    private void applySlots(Map<String,Integer> slots) {slots.forEach((id,slot)->kits.get(id).slot=slot);}
    private void browse(Player p) {
        Menu m=new Menu(p.getUniqueId(),null);m.inventory=Bukkit.createInventory(m,MENU_SIZE,Component.text("Kits",NamedTextColor.GOLD));
        for(Kit k:kits.values()) {
            int slot=k.slot;if(slot<0||slot>=MENU_SIZE)continue;
            ItemStack icon=k.icon.clone();ItemMeta meta=icon.getItemMeta();
            meta.displayName(Component.text(k.id,NamedTextColor.GOLD));
            List<String> lore=new ArrayList<>(List.of("Left-click: claim","Cooldown: "+k.cooldown+" seconds"));
            if(!k.permission.isEmpty()) lore.add("Permission: "+k.permission);
            if(k.id.equals(defaultKit)) lore.add("Default spawn kit");
            if(p.hasPermission("legendarykits.admin")) lore.add("Right-click: edit");
            meta.setLore(lore);icon.setItemMeta(meta);m.inventory.setItem(slot,icon);m.slots.put(slot,k.id);
        }
        p.openInventory(m.inventory);
    }
    private void edit(Player p,Kit k) {
        Menu m=new Menu(p.getUniqueId(),k.id);m.inventory=Bukkit.createInventory(m,54,Component.text("Edit kit: "+k.id,NamedTextColor.GOLD));
        for(int i=0;i<41;i++) if(!KitItems.empty(k.items[i])) m.inventory.setItem(i,k.items[i].clone());
        for(int i=41;i<54;i++) m.inventory.setItem(i,button(Material.GRAY_STAINED_GLASS_PANE," "));
        m.inventory.setItem(45,button(Material.LIME_WOOL,"Save kit","Slots 0–35: items","36: boots, 37: leggings","38: chestplate, 39: helmet","40: offhand"));
        m.inventory.setItem(46,button(Material.RED_WOOL,"Cancel"));
        m.inventory.setItem(47,button(Material.CLOCK,"Cooldown: "+k.cooldown+"s","Left: +60 seconds; right: -60","Shift-click: zero cooldown"));
        m.inventory.setItem(48,button(Material.PAPER,"Kit permission",k.permission.isEmpty()?"Public kit":k.permission,"Use /kits permission "+k.id+" <node|none>"));
        m.inventory.setItem(49,button(Material.ITEM_FRAME,"Set icon","Put the desired icon on your cursor","Then click here"));
        m.inventory.setItem(50,button(Material.BARRIER,"Delete kit","Click twice to confirm"));
        m.inventory.setItem(51,button(Material.NETHER_STAR,"Set default spawn kit", "Automatically equip on join and respawn"));
        m.inventory.setItem(52,button(Material.COMPASS,"Menu slot: "+(k.slot+1),"Use /kits slot "+k.id+" <1–27>"));
        p.openInventory(m.inventory);
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m)) return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(m.owner)) return;
        int slot=e.getRawSlot(); if(slot<0) return;
        if(m.editing==null) {
            if(m.slots.containsKey(slot)) {
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
            p.sendMessage("Saved kit "+k.id);browse(p);
        } else if(slot==46) browse(p);
        else if(slot==47) {
            long old=k.cooldown;k.cooldown=e.isShiftClick()?0:Math.min(31536000,Math.max(0,k.cooldown+(e.isRightClick()?-60:60)));
            if(!save()) k.cooldown=old;
            m.inventory.setItem(47,button(Material.CLOCK,"Cooldown: "+k.cooldown+"s","Left: +60; right: -60; shift: zero"));
        } else if(slot==49 && !KitItems.empty(e.getCursor())) {
            ItemStack old=k.icon;k.icon=e.getCursor().clone();k.icon.setAmount(1);if(!save())k.icon=old;p.sendMessage("Icon updated.");
        } else if(slot==52) {
            p.sendMessage("Use /kits slot "+k.id+" <1–27> to move this kit.");
        } else if(slot==51) {
            String old=defaultKit;defaultKit=k.id;
            if(!save()){defaultKit=old;p.sendMessage("Save failed.");}else p.sendMessage("Default spawn kit: "+k.id);
        } else if(slot==50) {
            if(!m.deleteArmed){m.deleteArmed=true;m.inventory.setItem(50,button(Material.BARRIER,"Click again to DELETE "+k.id));}
            else {String previousDefault=defaultKit;kits.remove(k.id);if(k.id.equals(defaultKit))defaultKit="";if(!save()){kits.put(k.id,k);defaultKit=previousDefault;}else {p.sendMessage("Deleted kit "+k.id);browse(p);}}
        }
    }
    @EventHandler public void drag(InventoryDragEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;
        boolean editor=m.editing!=null && e.getWhoClicked().hasPermission("legendarykits.admin") && e.getWhoClicked().getUniqueId().equals(m.owner);
        if(!editor || e.getRawSlots().stream().anyMatch(i->i>=41 && i<54))e.setCancelled(true);
    }
    @EventHandler public void joined(PlayerJoinEvent e) { scheduleSpawnKit(e.getPlayer()); }
    @EventHandler public void respawned(PlayerRespawnEvent e) { scheduleSpawnKit(e.getPlayer()); }
    private void scheduleSpawnKit(Player p) {
        getServer().getScheduler().runTaskLater(this,()->{if(p.isOnline()&&!p.isDead()) equipDefault(p);},1L);
    }
    private void equipDefault(Player p) {
        Kit k=kits.get(defaultKit);if(k==null)return;
        ItemStack[] planned=KitItems.spawnLoadout(p.getInventory().getContents(),k.items);
        if(planned==null){p.sendMessage("Make room in your inventory for the default kit.");return;}
        p.getInventory().setContents(planned);
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
        if(args.length==0){if(p.hasPermission("legendarykits.use"))browse(p);else p.sendMessage("No permission.");return true;}
        String action=args[0].toLowerCase(Locale.ROOT);
        if(Set.of("create","cooldown","permission","default","slot").contains(action)) {
            if(!p.hasPermission("legendarykits.admin")){p.sendMessage("No permission to edit kits.");return true;}
            if(args.length<2)return false;
            if(action.equals("default")) {
                if(args.length!=2)return false;
                String id=args[1].toLowerCase(Locale.ROOT);
                if(!id.equals("none")&&!kits.containsKey(id)){p.sendMessage("Unknown kit.");return true;}
                String old=defaultKit;defaultKit=id.equals("none")?"":id;
                if(!save()){defaultKit=old;p.sendMessage("Save failed.");}else p.sendMessage(defaultKit.isEmpty()?"Default spawn kit disabled.":"Default spawn kit: "+defaultKit);
                return true;
            }
            String id=args[1].toLowerCase(Locale.ROOT);
            if(!id.matches("[a-z0-9_-]{1,32}")){p.sendMessage("Use a name with 1–32 letters, numbers, underscores or dashes.");return true;}
            if(action.equals("create")) {
                if(kits.containsKey(id)){p.sendMessage("That kit already exists.");return true;}
                if(kits.size()>=MENU_SIZE){p.sendMessage("The single-page menu holds 27 kits. Delete a kit before creating another.");return true;}
                Kit k=new Kit(id);k.items=KitItems.copy(p.getInventory().getContents());
                if(!KitItems.empty(p.getInventory().getItemInMainHand())){k.icon=p.getInventory().getItemInMainHand().clone();k.icon.setAmount(1);}
                kits.put(id,k);applySlots(KitSlots.normalize(slotMap(),MENU_SIZE));if(!save()){kits.remove(id);p.sendMessage("Save failed.");return true;}edit(p,k);return true;
            }
            Kit k=kits.get(id);if(k==null){p.sendMessage("Unknown kit.");return true;}if(args.length!=3)return false;
            if(action.equals("slot")) {
                try {
                    int target=Integer.parseInt(args[2])-1;
                    Map<String,Integer> previous=slotMap();applySlots(KitSlots.move(previous,id,target,MENU_SIZE));
                    if(!save()){applySlots(previous);p.sendMessage("Save failed.");return true;}
                    p.sendMessage("Moved kit "+id+" to menu slot "+(target+1)+". Occupied slots swap kits.");
                } catch(IllegalArgumentException ex){p.sendMessage("Slot must be a number from 1 to 27.");}
                return true;
            }
            if(action.equals("cooldown")) {
                try {long value=Long.parseLong(args[2]);if(value<0||value>31536000)throw new NumberFormatException();long old=k.cooldown;k.cooldown=value;if(!save())k.cooldown=old;}
                catch(NumberFormatException ex){p.sendMessage("Cooldown must be 0–31536000 seconds.");return true;}
            } else {String old=k.permission;k.permission=args[2].equalsIgnoreCase("none")?"":args[2];if(!save())k.permission=old;}
            p.sendMessage("Kit settings updated.");return true;
        }
        Kit k=kits.get(action);if(k==null){p.sendMessage("Unknown kit. Use /kits.");return true;}claim(p,k);return true;
    }
}
