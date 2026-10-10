package io.github.legendaryswords;

import io.github.legendaryswords.sword.SwordCatalog;
import io.github.legendaryswords.sword.SwordType;
import java.util.UUID;
import java.util.HashSet;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwordBrowserTest {
    private record Fixture(LegendarySwordsPlugin plugin, SwordBrowser browser, Player player,
                           SwordCatalog catalog, InventoryClickEvent event, PlayerInventory inventory) { }
    private Fixture fixture(int page, int slot, ClickType click, boolean admin) {
        var plugin = mock(LegendarySwordsPlugin.class);
        var catalog = mock(SwordCatalog.class);
        when(plugin.swords()).thenReturn(catalog);
        var player = mock(Player.class);
        UUID id = UUID.randomUUID(); when(player.getUniqueId()).thenReturn(id);
        when(player.hasPermission("legendaryswords.admin")).thenReturn(admin);
        var inventory = mock(PlayerInventory.class); when(player.getInventory()).thenReturn(inventory);
        var top = mock(Inventory.class); when(top.getHolder()).thenReturn(new SwordBrowser.View(id, page));
        var view = mock(InventoryView.class); when(view.getTopInventory()).thenReturn(top);
        var event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view); when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(slot); when(event.getClick()).thenReturn(click);
        return new Fixture(plugin, new SwordBrowser(plugin), player, catalog, event, inventory);
    }
    @Test void allFiftyWeaponsAppearOnceAcrossPages() {
        var seen = new HashSet<SwordType>();
        for (int page = 0; page < SwordBrowser.pages(); page++) for (int slot = 0; slot < 45; slot++) {
            var sword = SwordBrowser.selection(page, slot);
            if (sword != null) assertTrue(seen.add(sword));
        }
        assertEquals(50, seen.size()); assertEquals(2, SwordBrowser.pages());
        assertNull(SwordBrowser.selection(1, 5)); assertNull(SwordBrowser.selection(0, 54));
        assertNull(SwordBrowser.selection(-1, 0)); assertNull(SwordBrowser.selection(2, 0));
    }
    @Test void normalPlayersCannotReceiveOrStealDisplayItems() {
        for (ClickType click : ClickType.values()) {
            Fixture f = fixture(0, 0, click, false); f.browser.click(f.event);
            verify(f.event).setCancelled(true); verifyNoInteractions(f.catalog, f.inventory);
        }
    }
    @Test void adminClicksGiveFreshItemsFromTheSecondPage() {
        Fixture f = fixture(1, 4, ClickType.LEFT, true);
        var item = mock(ItemStack.class); when(f.catalog.create(SwordType.DRAGONHEART)).thenReturn(item);
        f.browser.click(f.event);
        verify(f.event).setCancelled(true); verify(f.inventory).addItem(item);
        verify(f.catalog).create(SwordType.DRAGONHEART);
    }
    @Test void revokedPermissionAndFullInventoryPreventGrants() {
        Fixture f = fixture(0, 0, ClickType.LEFT, true);
        when(f.player.hasPermission("legendaryswords.admin")).thenReturn(false);
        f.browser.click(f.event); verifyNoInteractions(f.catalog, f.inventory);
        when(f.player.hasPermission("legendaryswords.admin")).thenReturn(true);
        when(f.inventory.firstEmpty()).thenReturn(-1);
        f.browser.click(f.event); verifyNoInteractions(f.catalog);
        verify(f.inventory, never()).addItem(any(ItemStack.class));
    }
    @Test void adminShiftKeysCloningDropsAndBottomClicksNeverGrant() {
        for (ClickType click : ClickType.values()) {
            if (click == ClickType.LEFT || click == ClickType.RIGHT) continue;
            Fixture f = fixture(0, 0, click, true); f.browser.click(f.event);
            verify(f.event).setCancelled(true); verifyNoInteractions(f.catalog, f.inventory);
        }
        for (int slot : new int[]{-999, 45, 49, 54, 70}) {
            Fixture f = fixture(0, slot, ClickType.LEFT, true); f.browser.click(f.event);
            verify(f.event).setCancelled(true); verifyNoInteractions(f.catalog, f.inventory);
        }
    }
    @Test void menusOwnedByAnotherPlayerCannotGrantItems() {
        Fixture f = fixture(0, 0, ClickType.LEFT, true);
        when(f.player.getUniqueId()).thenReturn(UUID.randomUUID()); f.browser.click(f.event);
        verify(f.event).setCancelled(true); verifyNoInteractions(f.catalog, f.inventory);
    }
    @Test void draggingIsBlockedWhileOrdinaryInventoriesRemainUntouched() {
        Fixture f = fixture(0, 0, ClickType.LEFT, true);
        var drag = mock(InventoryDragEvent.class); var browserView = f.event.getView(); when(drag.getView()).thenReturn(browserView);
        f.browser.drag(drag); verify(drag).setCancelled(true);
        var top = mock(Inventory.class); var view = mock(InventoryView.class); when(view.getTopInventory()).thenReturn(top);
        var event = mock(InventoryClickEvent.class); when(event.getView()).thenReturn(view);
        f.browser.click(event); verify(event, never()).setCancelled(anyBoolean());
    }
    @Test void swordsAndLegendaryswordsCommandsOpenPublicBrowser() {
        var plugin = mock(LegendarySwordsPlugin.class); var browser = mock(SwordBrowser.class);
        when(plugin.browser()).thenReturn(browser); var player = mock(Player.class);
        var command = new SwordCommand(plugin);
        command.onCommand(player, null, "swords", new String[0]);
        command.onCommand(player, null, "legendaryswords", new String[0]);
        verify(browser, times(2)).open(player, 0);
        verify(player, never()).hasPermission("legendaryswords.admin");
    }
}
