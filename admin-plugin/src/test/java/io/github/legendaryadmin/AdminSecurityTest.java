package io.github.legendaryadmin;

import java.lang.reflect.Field;
import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSecurityTest {
    @Test void everyAdminCommandRejectsUnprivilegedSendersBeforeAccessingServerState() {
        LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        CommandSender sender = mock(CommandSender.class);
        for (String action : LegendaryAdmin.ACTIONS) {
            assertTrue(plugin.onCommand(sender, mock(Command.class), "la", new String[]{action, "target"}));
        }
        verify(plugin, never()).getServer();
        verify(sender, atLeastOnce()).sendMessage(any(net.kyori.adventure.text.Component.class));
        assertTrue(plugin.onTabComplete(sender, mock(Command.class), "la", new String[]{""}).isEmpty());
    }
    @Test void directCommandsKeepArgumentsAndPermissions() {
        LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        CommandSender sender = mock(CommandSender.class);
        for (String name : LegendaryAdmin.DIRECT_COMMANDS) {
            Command command = mock(Command.class);
            when(command.getName()).thenReturn(name);
            String action = name.equals("adminmenu") ? "menu" : name.equals("adminhelp") ? "help" : name;
            assertArrayEquals(new String[]{action, "target", "reason"},
                    LegendaryAdmin.commandArgs(command, new String[]{"target", "reason"}));
            assertTrue(plugin.onCommand(sender, command, name, new String[]{"target"}));
            assertTrue(plugin.onTabComplete(sender, command, name, new String[]{""}).isEmpty());
        }
        verify(plugin, never()).getServer();
    }
    @Test void gamemodeAndBothTeleportCommandsUseTheirActualPermissionNodes() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("legendaryadmin.teleport")).thenReturn(true);
        assertTrue(LegendaryAdmin.allowed(sender, "tp"));
        assertTrue(LegendaryAdmin.allowed(sender, "tphere"));
        assertFalse(LegendaryAdmin.allowed(sender, "gm"));
        when(sender.hasPermission("legendaryadmin.gamemode")).thenReturn(true);
        assertTrue(LegendaryAdmin.allowed(sender, "gm"));
        assertFalse(LegendaryAdmin.allowed(sender, "ban"));
    }
    @Test void inventoryViewerCancelsBottomAndTopClicksRegardlessOfClickType() {
        LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        Inventory inventory = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(inventory.getHolder()).thenReturn(new LegendaryAdmin.AdminView(UUID.randomUUID(), true));
        when(view.getTopInventory()).thenReturn(inventory);
        for (ClickType type : List.of(ClickType.LEFT, ClickType.SHIFT_LEFT, ClickType.NUMBER_KEY, ClickType.DOUBLE_CLICK, ClickType.DROP, ClickType.SWAP_OFFHAND)) {
            for (int rawSlot : List.of(0, 54)) {
                InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER,
                        rawSlot, type, InventoryAction.UNKNOWN);
                plugin.click(event);
                assertTrue(event.isCancelled());
            }
        }
        verify(plugin, never()).getServer();
    }
    @Test void draggingIntoAnyAdminInventoryIsCancelled() {
        LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        Inventory inventory = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(inventory.getHolder()).thenReturn(new LegendaryAdmin.AdminView(UUID.randomUUID(), false));
        when(view.getTopInventory()).thenReturn(inventory);
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(view);
        plugin.drag(event);
        verify(event).setCancelled(true);
    }
    @Test void ordinaryInventoryClicksAreUnaffected() {
        LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        Inventory inventory = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        plugin.click(event);
        verify(event, never()).setCancelled(anyBoolean());
    }
    @Test void aMenuOpenedEarlierCannotKickAfterPermissionIsRevoked() {
        LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        Server server = mock(Server.class);
        var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        Player admin = mock(Player.class), target = mock(Player.class);
        UUID id = UUID.randomUUID();
        doReturn(server).when(plugin).getServer();
        when(server.getScheduler()).thenReturn(scheduler);
        doReturn(mock(PluginCommand.class)).when(plugin).getCommand("legendaryadmin");
        when(server.getPlayer(id)).thenReturn(target);
        when(admin.isOnline()).thenReturn(true); when(target.isOnline()).thenReturn(true);
        when(target.getName()).thenReturn("Target");
        doAnswer(call -> { ((Runnable) call.getArgument(1)).run(); return mock(org.bukkit.scheduler.BukkitTask.class); })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
        Inventory inventory = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        var holder = new LegendaryAdmin.AdminView(id, false);
        when(inventory.getHolder()).thenReturn(holder); when(view.getTopInventory()).thenReturn(inventory);
        for (String action : List.of("kick", "confirm:kick")) {
            holder.actions.put(0, action);
            InventoryClickEvent event = mock(InventoryClickEvent.class);
            when(event.getView()).thenReturn(view); when(event.getWhoClicked()).thenReturn(admin);
            plugin.click(event);
            verify(event).setCancelled(true);
        }
        verify(target, never()).kick(any(net.kyori.adventure.text.Component.class));
        verify(admin, never()).openInventory(any(Inventory.class));
    }
    @Test void freezeStopsPositionChangesButAllowsLookingAround() throws Exception {
        LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID(); when(player.getUniqueId()).thenReturn(id);
        Field field = LegendaryAdmin.class.getDeclaredField("frozen"); field.setAccessible(true); field.set(plugin, new HashSet<>(Set.of(id)));
        World world = mock(World.class);
        Location from = new Location(world, 1, 70, 2, 0, 0), to = new Location(world, 3, 71, 4, 90, 30);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
        plugin.move(event);
        assertEquals(1, event.getTo().getX()); assertEquals(70, event.getTo().getY()); assertEquals(2, event.getTo().getZ());
        assertEquals(90, event.getTo().getYaw()); assertEquals(30, event.getTo().getPitch());
    }
    private static final class VanishFixture {
        final LegendaryAdmin plugin = mock(LegendaryAdmin.class, CALLS_REAL_METHODS);
        final Server server = mock(Server.class);
        final Player admin = mock(Player.class), target = mock(Player.class), viewer = mock(Player.class);
        final UUID targetId = UUID.randomUUID();
        VanishFixture() throws Exception {
            for (String name : List.of("vanished", "frozen", "flights")) {
                Field field = LegendaryAdmin.class.getDeclaredField(name); field.setAccessible(true);
                field.set(plugin, name.equals("flights") ? new HashMap<>() : new HashSet<>());
            }
            doReturn(server).when(plugin).getServer();
            doReturn(java.util.logging.Logger.getAnonymousLogger()).when(plugin).getLogger();
            when(admin.getName()).thenReturn("Admin");
            when(admin.hasPermission("legendaryadmin.vanish")).thenReturn(true);
            when(admin.hasPermission("legendaryadmin.see-vanished")).thenReturn(true);
            when(target.getName()).thenReturn("Target"); when(target.getUniqueId()).thenReturn(targetId);
            when(server.getPlayerExact("Target")).thenReturn(target); when(server.getPlayer(targetId)).thenReturn(target);
            doReturn(List.of(admin, target, viewer)).when(server).getOnlinePlayers();
        }
        void toggle() { plugin.onCommand(admin, mock(Command.class), "la", new String[]{"vanish", "Target"}); }
    }
    @Test void vanishHidesFromNormalPlayersButKeepsOwnerAndAuthorizedAdminsVisible() throws Exception {
        var f = new VanishFixture(); f.toggle();
        verify(f.viewer).hidePlayer(f.plugin, f.target);
        verify(f.admin).showPlayer(f.plugin, f.target);
        verify(f.target, never()).hidePlayer(any(), any());
        f.toggle();
        verify(f.viewer).showPlayer(f.plugin, f.target);
    }
    @Test void quittingVanishRevealsPlayerSoTheyAreNotStillHiddenAfterReconnecting() throws Exception {
        var f = new VanishFixture(); f.toggle();
        PlayerQuitEvent event = mock(PlayerQuitEvent.class); when(event.getPlayer()).thenReturn(f.target);
        f.plugin.quit(event);
        verify(f.viewer).showPlayer(f.plugin, f.target);
        clearInvocations(f.viewer);
        var refresh = LegendaryAdmin.class.getDeclaredMethod("refreshVisibility"); refresh.setAccessible(true);
        refresh.invoke(f.plugin);
        verifyNoInteractions(f.viewer);
    }
}
