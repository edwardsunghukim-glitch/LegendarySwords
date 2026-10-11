package io.github.legendaryadmin;
import org.junit.jupiter.api.Test;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.inventory.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EnderChestTest {
    @Test void ecseeIsDirectAndHasItsOwnPermission() {
        assertTrue(LegendaryAdmin.DIRECT_COMMANDS.contains("ecsee"));assertEquals("legendaryadmin.ecsee",LegendaryAdmin.permission("ecsee"));
    }
    @Test void enderChestViewUsesAnEditorAndClonesTheStoredItems() {
        LegendaryAdmin plugin=mock(LegendaryAdmin.class,CALLS_REAL_METHODS);Server server=mock(Server.class);
        Player admin=mock(Player.class),target=mock(Player.class);Inventory ender=mock(Inventory.class),screen=mock(Inventory.class);
        ItemStack original=mock(ItemStack.class),copy=mock(ItemStack.class);when(original.clone()).thenReturn(copy);
        when(plugin.getServer()).thenReturn(server);when(admin.hasPermission("legendaryadmin.ecsee")).thenReturn(true);
        when(server.getPlayerExact("Alex")).thenReturn(target);when(target.getName()).thenReturn("Alex");when(target.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(target.getEnderChest()).thenReturn(ender);when(ender.getContents()).thenReturn(new ItemStack[]{original});
        when(server.createInventory(any(InventoryHolder.class),eq(27),any(net.kyori.adventure.text.Component.class))).thenAnswer(call->{
            var holder=(LegendaryAdmin.AdminView)call.getArgument(0);assertEquals("ecsee",holder.editor);return screen;
        });
        Command cmd=mock(Command.class);when(cmd.getName()).thenReturn("ecsee");
        assertTrue(plugin.onCommand(admin,cmd,"ecsee",new String[]{"Alex"}));verify(screen).setItem(0,copy);verify(admin).openInventory(screen);
    }
    @Test void takingAnItemWritesTheLiveChestAndReturnsItToAdminCursor() {
        LegendaryAdmin plugin=mock(LegendaryAdmin.class,CALLS_REAL_METHODS);Server server=mock(Server.class);
        Player admin=mock(Player.class),target=mock(Player.class);Inventory live=mock(Inventory.class),screen=mock(Inventory.class);
        var holder=new LegendaryAdmin.AdminView(java.util.UUID.randomUUID(),true);holder.editor="ecsee";holder.inventory=screen;
        var event=mock(org.bukkit.event.inventory.InventoryClickEvent.class);var window=mock(InventoryView.class);
        ItemStack item=mock(ItemStack.class),copy=mock(ItemStack.class);when(item.clone()).thenReturn(copy);when(copy.clone()).thenReturn(copy);
        when(plugin.getServer()).thenReturn(server);when(server.getPlayer(holder.target)).thenReturn(target);when(target.isOnline()).thenReturn(true);
        when(admin.hasPermission("legendaryadmin.ecsee")).thenReturn(true);when(target.getEnderChest()).thenReturn(live);
        when(event.getView()).thenReturn(window);when(window.getTopInventory()).thenReturn(screen);when(screen.getHolder()).thenReturn(holder);
        when(screen.getSize()).thenReturn(27);when(event.getWhoClicked()).thenReturn(admin);when(event.isLeftClick()).thenReturn(true);
        when(live.getItem(0)).thenReturn(item);when(server.getOnlinePlayers()).thenReturn(java.util.List.of());
        plugin.click(event);verify(event).setCancelled(true);verify(live).setItem(0,null);verify(admin).setItemOnCursor(copy);
    }
}
