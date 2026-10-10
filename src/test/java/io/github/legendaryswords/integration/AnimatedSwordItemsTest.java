package io.github.legendaryswords.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.legendaryswords.PluginSettings;
import io.github.legendaryswords.sword.SwordCatalog;
import io.github.legendaryswords.sword.SwordType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Registry;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** Exercises sword identification, copy-on-read metadata, and detached inventory replacements. */
class AnimatedSwordItemsTest {
    private static final NamespacedKey SWORD_KEY = new NamespacedKey("legendaryswords", "sword_id");
    private static MockedStatic<RegistryAccess> registryApi;

    @BeforeAll static void initializePaperMaterialRegistry() {
        // Paper 1.21.11 resolves Material.isAir through its server registry. Sword
        // materials have no block type; empty registries reproduce that lookup.
        registryApi = mockStatic(RegistryAccess.class);
        RegistryAccess access = mock(RegistryAccess.class);
        when(access.getRegistry(any(Class.class))).thenAnswer(call -> mock(Registry.class));
        when(access.getRegistry(any(RegistryKey.class))).thenAnswer(call -> mock(Registry.class));
        registryApi.when(RegistryAccess::registryAccess).thenReturn(access);
    }

    @AfterAll static void closePaperMaterialRegistry() { registryApi.close(); }

    @ParameterizedTest
    @EnumSource(SwordType.class)
    void inventoryAndIndependentDisplayChangeOnlyTheOwnedItemModel(SwordType sword) {
        try (Fixture f = new Fixture()) {
            ItemStack held = f.sword(sword, false);
            f.contents[0] = held;
            f.start();
            Map<String, Object> originalMetadata = f.metadata(held);

            assertTrue(f.items.begin(f.player, sword.id()));
            ItemStack animatedHeld = f.contents[0];
            assertNotSame(held, animatedHeld);
            assertEquals(base(sword), f.model(held), "Queued readers of the original live stack must retain its base model");
            assertEquals(originalMetadata, f.metadata(held));
            verify(held, never()).setItemMeta(any());
            assertEquals(animated(sword), f.model(animatedHeld));
            assertTrue(f.items.begin(f.player, sword.id()));
            assertSame(animatedHeld, f.contents[0], "Beginning an already animated sword must not replace it again");
            verify(animatedHeld, times(1)).setItemMeta(any());
            verify(f.inventory, times(1)).setItemInMainHand(animatedHeld);
            ItemStack display = f.items.displayItem(f.player, sword.id());
            assertNotSame(held, display);
            assertNotSame(animatedHeld, display);
            assertEquals(animated(sword), f.model(display));

            f.items.restorePlayer(f.player);

            ItemStack restoredHeld = f.contents[0];
            assertNotSame(animatedHeld, restoredHeld);
            assertEquals(base(sword), f.model(restoredHeld));
            assertEquals(animated(sword), f.model(animatedHeld), "Queued readers must retain the original animated stack");
            verify(animatedHeld, times(1)).setItemMeta(any());
            assertEquals(animated(sword), f.model(display), "The body display must keep its independent animated copy");
            assertEquals(originalMetadata, f.metadata(held));
            assertEquals(originalMetadata, f.metadata(animatedHeld));
            assertEquals(originalMetadata, f.metadata(restoredHeld));
            assertEquals(originalMetadata, f.metadata(display));
            assertEquals(sword.material(), display.getType());
            assertEquals(held.getAmount(), display.getAmount());
            f.assertOnlyModelWasWritten();
        }
    }

    @ParameterizedTest
    @EnumSource(SwordType.class)
    void aDetachedDisplayCanBeRestoredWithoutChangingTheHeldSword(SwordType sword) {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack original = f.sword(sword, false);
            f.contents[0] = original;
            assertTrue(f.items.begin(f.player, sword.id()));
            ItemStack held = f.contents[0];
            ItemStack display = f.items.displayItem(f.player, sword.id());

            assertTrue(f.items.restoreItem(display));

            assertSame(held, f.contents[0]);
            assertEquals(animated(sword), f.model(held));
            assertEquals(base(sword), f.model(original));
            assertEquals(base(sword), f.model(display));
            assertEquals(f.metadata(original), f.metadata(display));
            verify(original, never()).setItemMeta(any());
            verify(held, times(1)).setItemMeta(any());
            verify(display, times(2)).setItemMeta(any());
            f.assertOnlyModelWasWritten();
        }
    }

    @Test void recoveryWorksAtStartupAndJoinWithoutBetterModel() {
        try (Fixture f = new Fixture()) {
            ItemStack held = f.sword(SwordType.MASTER_SWORD, true);
            ItemStack offHand = f.sword(SwordType.FLAMEBLADE, true);
            ItemStack cursor = f.sword(SwordType.FROSTBLADE, true);
            f.contents[0] = held;
            f.contents[40] = offHand;
            f.cursor = cursor;
            f.start(); // No BetterModel plugin is installed in this fixture.
            f.assertRestoredCopy(held, f.contents[0], SwordType.MASTER_SWORD);
            f.assertRestoredCopy(offHand, f.contents[40], SwordType.FLAMEBLADE);
            f.assertRestoredCopy(cursor, f.cursor, SwordType.FROSTBLADE);

            ItemStack joined = f.sword(SwordType.EIGHTFOLD_BLADE, true);
            f.contents[7] = joined;
            PlayerJoinEvent join = mock(PlayerJoinEvent.class);
            when(join.getPlayer()).thenReturn(f.player);
            f.items.join(join);
            f.assertRestoredCopy(joined, f.contents[7], SwordType.EIGHTFOLD_BLADE);
            verify(f.manager).registerEvents(f.items, f.plugin);
            verify(f.manager, never()).getPlugin(anyString());
        }
    }

    @Test void unrelatedModelsForgedIdentifiersAndWrongMaterialsAreUntouched() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack foreignModel = f.sword(SwordType.MASTER_SWORD, true);
            f.states.get(foreignModel).model = new NamespacedKey("another_pack", "master_sword");
            ItemStack forgedNamespace = f.sword(SwordType.MASTER_SWORD, true);
            f.states.get(forgedNamespace).pdc.remove(SWORD_KEY);
            f.states.get(forgedNamespace).pdc.put(new NamespacedKey("another_plugin", "sword_id"), "master_sword");
            ItemStack unknownId = f.sword(SwordType.MASTER_SWORD, true);
            f.states.get(unknownId).pdc.put(SWORD_KEY, "forged_sword");
            ItemStack wrongMaterial = f.sword(SwordType.MASTER_SWORD, true);
            when(wrongMaterial.getType()).thenReturn(Material.STONE);
            for (ItemStack item : List.of(foreignModel, forgedNamespace, unknownId, wrongMaterial)) {
                NamespacedKey before = f.model(item);
                f.contents[0] = item;
                assertFalse(f.items.begin(f.player, "master_sword"));
                assertFalse(f.items.restoreItem(item));
                f.items.restorePlayer(f.player);
                assertSame(item, f.contents[0]);
                assertEquals(before, f.model(item));
                verify(item, never()).setItemMeta(any());
            }
            verify(f.inventory, never()).setItem(anyInt(), any());
            verify(f.inventory, never()).setItemInMainHand(any());
            f.contents[0] = f.sword(SwordType.FLAMEBLADE, false);
            assertFalse(f.items.begin(f.player, "master_sword"), "An actual different sword cannot borrow this body's model");
            assertFalse(f.items.restoreItem(null));
        }
    }

    @Test void selectionAndQuitRestoreEveryInventorySlotAndCursor() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack heldSword = f.sword(SwordType.MASTER_SWORD, true);
            ItemStack stored = f.sword(SwordType.ROYAL_CLAYMORE, true);
            ItemStack offHand = f.sword(SwordType.FLAMEBLADE, true);
            ItemStack cursor = f.sword(SwordType.FROSTBLADE, true);
            f.contents[0] = heldSword;
            f.contents[8] = stored;
            f.contents[40] = offHand;
            f.cursor = cursor;
            PlayerItemHeldEvent held = mock(PlayerItemHeldEvent.class);
            when(held.getPlayer()).thenReturn(f.player);
            f.items.held(held);
            f.assertRestoredCopy(heldSword, f.contents[0], SwordType.MASTER_SWORD);
            f.assertRestoredCopy(stored, f.contents[8], SwordType.ROYAL_CLAYMORE);
            f.assertRestoredCopy(offHand, f.contents[40], SwordType.FLAMEBLADE);
            f.assertRestoredCopy(cursor, f.cursor, SwordType.FROSTBLADE);

            ItemStack quitting = f.sword(SwordType.THUNDERBLADE, true);
            f.contents[0] = quitting;
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(f.player);
            f.items.quit(quit);
            f.assertRestoredCopy(quitting, f.contents[0], SwordType.THUNDERBLADE);
        }
    }

    @Test void swappingRestoresTheEventItemsBeforeTheyBecomeTheNewHands() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack main = f.sword(SwordType.FIERCE_DEITY_SWORD, true);
            ItemStack off = f.sword(SwordType.BIGGORON_SWORD, true);
            PlayerSwapHandItemsEvent event = mock(PlayerSwapHandItemsEvent.class);
            when(event.getPlayer()).thenReturn(f.player);
            when(event.getMainHandItem()).thenReturn(main);
            when(event.getOffHandItem()).thenReturn(off);
            f.items.swapped(event);
            ArgumentCaptor<ItemStack> mainReplacement = ArgumentCaptor.forClass(ItemStack.class);
            ArgumentCaptor<ItemStack> offReplacement = ArgumentCaptor.forClass(ItemStack.class);
            verify(event).setMainHandItem(mainReplacement.capture());
            verify(event).setOffHandItem(offReplacement.capture());
            f.assertRestoredCopy(main, mainReplacement.getValue(), SwordType.FIERCE_DEITY_SWORD);
            f.assertRestoredCopy(off, offReplacement.getValue(), SwordType.BIGGORON_SWORD);
        }
    }

    @Test void droppingAndPickingUpRestoreTheWorldEntityStack() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack dropped = f.sword(SwordType.THUNDERBLADE, true);
            Item entity = mock(Item.class);
            when(entity.getItemStack()).thenReturn(dropped);
            PlayerDropItemEvent drop = mock(PlayerDropItemEvent.class);
            when(drop.getPlayer()).thenReturn(f.player);
            when(drop.getItemDrop()).thenReturn(entity);
            f.items.dropped(drop);
            ArgumentCaptor<ItemStack> replacement = ArgumentCaptor.forClass(ItemStack.class);
            verify(entity).setItemStack(replacement.capture());
            f.assertRestoredCopy(dropped, replacement.getValue(), SwordType.THUNDERBLADE);

            ItemStack picked = f.sword(SwordType.FROSTBLADE, true);
            when(entity.getItemStack()).thenReturn(picked);
            EntityPickupItemEvent pickup = mock(EntityPickupItemEvent.class);
            when(pickup.getEntity()).thenReturn(f.player);
            when(pickup.getItem()).thenReturn(entity);
            f.items.pickedUp(pickup);
            verify(entity, times(2)).setItemStack(replacement.capture());
            f.assertRestoredCopy(picked, replacement.getValue(), SwordType.FROSTBLADE);
        }
    }

    @Test void deathRestoresDropsAndRetainedInventoryAndDisableRestoresTheCursor() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack drop = f.sword(SwordType.BIGGORON_SWORD, true);
            ItemStack retained = f.sword(SwordType.MASTER_SWORD, true);
            f.contents[0] = retained;
            PlayerDeathEvent death = mock(PlayerDeathEvent.class);
            List<ItemStack> drops = new ArrayList<>(List.of(drop));
            when(death.getEntity()).thenReturn(f.player);
            when(death.getDrops()).thenReturn(drops);
            f.items.died(death);
            f.assertRestoredCopy(drop, drops.get(0), SwordType.BIGGORON_SWORD);
            f.assertRestoredCopy(retained, f.contents[0], SwordType.MASTER_SWORD);

            ItemStack offHand = f.sword(SwordType.EIGHTFOLD_BLADE, true);
            ItemStack cursor = f.sword(SwordType.FIERCE_DEITY_SWORD, true);
            f.contents[40] = offHand;
            f.cursor = cursor;
            f.enabled.set(false);
            f.items.close();
            f.assertRestoredCopy(offHand, f.contents[40], SwordType.EIGHTFOLD_BLADE);
            f.assertRestoredCopy(cursor, f.cursor, SwordType.FIERCE_DEITY_SWORD);
        }
    }

    @Test void clickingRestoresOutgoingAndNextTickDestinationStacks() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack outgoing = f.sword(SwordType.MASTER_SWORD, true);
            View destination = f.view(54);
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getWhoClicked()).thenReturn(f.player);
            when(click.getView()).thenReturn(destination.view);
            when(click.getRawSlot()).thenReturn(4);
            when(click.getCurrentItem()).thenReturn(outgoing);
            f.items.clicked(click);
            ArgumentCaptor<ItemStack> replacement = ArgumentCaptor.forClass(ItemStack.class);
            verify(click).setCurrentItem(replacement.capture());
            f.assertRestoredCopy(outgoing, replacement.getValue(), SwordType.MASTER_SWORD);

            // Paper applies the actual destination stack after returning from the event.
            ItemStack landed = f.sword(SwordType.FLAMEBLADE, true);
            destination.slots.put(4, landed);
            verify(destination.view, never()).setItem(anyInt(), any());
            f.nextTick();
            f.assertRestoredCopy(landed, destination.slots.get(4), SwordType.FLAMEBLADE);
            verify(destination.view).setItem(4, destination.slots.get(4));
            assertEquals(animated(SwordType.MASTER_SWORD), f.model(outgoing));
        }
    }

    @Test void creativeTransfersRestoreTheirDistinctCursorAndRealDestination() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack transfer = f.sword(SwordType.ROYAL_CLAYMORE, true);
            View destination = f.view(46);
            InventoryCreativeEvent creative = mock(InventoryCreativeEvent.class);
            when(creative.getWhoClicked()).thenReturn(f.player);
            when(creative.getCursor()).thenReturn(transfer);
            when(creative.getView()).thenReturn(destination.view);
            when(creative.getRawSlot()).thenReturn(9);
            f.items.clicked(creative);
            ArgumentCaptor<ItemStack> replacement = ArgumentCaptor.forClass(ItemStack.class);
            verify(creative).setCursor(replacement.capture());
            f.assertRestoredCopy(transfer, replacement.getValue(), SwordType.ROYAL_CLAYMORE);

            ItemStack landed = f.sword(SwordType.ROYAL_CLAYMORE, true);
            destination.slots.put(9, landed);
            f.nextTick();
            f.assertRestoredCopy(landed, destination.slots.get(9), SwordType.ROYAL_CLAYMORE);
            verify(destination.view).setItem(9, destination.slots.get(9));
            assertEquals(animated(SwordType.ROYAL_CLAYMORE), f.model(transfer));
        }
    }

    @Test void dragRestoresRealDestinationsAfterPaperAppliesItsSeparateSnapshots() {
        try (Fixture f = new Fixture()) {
            f.start();
            View destination = f.view(54);
            ItemStack snapshot = f.sword(SwordType.FROSTBLADE, true);
            ItemStack cursor = f.sword(SwordType.EIGHTFOLD_BLADE, true);
            InventoryDragEvent drag = mock(InventoryDragEvent.class);
            when(drag.getWhoClicked()).thenReturn(f.player);
            when(drag.getView()).thenReturn(destination.view);
            when(drag.getRawSlots()).thenReturn(Set.of(3, 7, -1, 99));
            when(drag.getNewItems()).thenReturn(Map.of(3, snapshot));
            when(drag.getCursor()).thenReturn(cursor);
            f.items.dragged(drag);
            ArgumentCaptor<ItemStack> replacement = ArgumentCaptor.forClass(ItemStack.class);
            verify(drag).setCursor(replacement.capture());
            f.assertRestoredCopy(cursor, replacement.getValue(), SwordType.EIGHTFOLD_BLADE);
            assertEquals(animated(SwordType.FROSTBLADE), f.model(snapshot), "A snapshot does not represent the server's eventual destination");
            verify(destination.view, never()).setItem(anyInt(), any());

            ItemStack first = f.sword(SwordType.FROSTBLADE, true);
            ItemStack second = f.sword(SwordType.THUNDERBLADE, true);
            destination.slots.put(3, first);
            destination.slots.put(7, second);
            ItemStack held = f.sword(SwordType.FLAMEBLADE, true);
            f.contents[0] = held;
            f.nextTick();
            f.assertRestoredCopy(first, destination.slots.get(3), SwordType.FROSTBLADE);
            f.assertRestoredCopy(second, destination.slots.get(7), SwordType.THUNDERBLADE);
            f.assertRestoredCopy(held, f.contents[0], SwordType.FLAMEBLADE);
            verify(destination.view).setItem(3, destination.slots.get(3));
            verify(destination.view).setItem(7, destination.slots.get(7));
            assertEquals(animated(SwordType.FROSTBLADE), f.model(snapshot));
            verify(destination.view, never()).getItem(-1);
            verify(destination.view, never()).getItem(99);
        }
    }

    @Test void workerCloseQueuesRestorationBeforeLookingUpOrTouchingAPlayer() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack held = f.sword(SwordType.MASTER_SWORD, true);
            f.contents[0] = held;
            f.primaryThread.set(false);
            f.items.restorePlayerSoon(f.id);
            assertEquals(animated(SwordType.MASTER_SWORD), f.model(f.contents[0]));
            verify(f.server, never()).getPlayer(f.id);
            assertEquals(1, f.deferred.size());
            assertFalse(f.items.begin(f.player, "master_sword"), "An old body's queued cleanup must finish before a new animation starts");

            f.primaryThread.set(true);
            f.nextTick();
            f.assertRestoredCopy(held, f.contents[0], SwordType.MASTER_SWORD);
            verify(f.server).getPlayer(f.id);
            assertTrue(f.items.begin(f.player, "master_sword"));
        }
    }

    @Test void everyQueuedInventoryUpdateMustFinishBeforeTheHeldSwordCanAnimateAgain() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack original = f.sword(SwordType.MASTER_SWORD, false);
            f.contents[0] = original;
            assertTrue(f.items.begin(f.player, "master_sword"));
            ItemStack animatedHeld = f.contents[0];
            ItemStack display = f.items.displayItem(f.player, "master_sword");
            View destination = f.view(54);
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getWhoClicked()).thenReturn(f.player);
            when(click.getView()).thenReturn(destination.view);
            when(click.getRawSlot()).thenReturn(4);
            InventoryDragEvent drag = mock(InventoryDragEvent.class);
            when(drag.getWhoClicked()).thenReturn(f.player);
            when(drag.getView()).thenReturn(destination.view);
            when(drag.getRawSlots()).thenReturn(Set.of(7));

            f.items.clicked(click);
            f.items.dragged(drag);
            assertEquals(2, f.deferred.size());
            ItemStack restoredHeld = f.contents[0];
            assertNotSame(animatedHeld, restoredHeld);
            assertEquals(base(SwordType.MASTER_SWORD), f.model(restoredHeld));
            assertEquals(base(SwordType.MASTER_SWORD), f.model(original));
            assertEquals(animated(SwordType.MASTER_SWORD), f.model(animatedHeld));
            assertEquals(animated(SwordType.MASTER_SWORD), f.model(display));
            assertFalse(f.items.begin(f.player, "master_sword"));

            f.deferred.remove(0).run();
            assertFalse(f.items.begin(f.player, "master_sword"), "One completed event must not release a second event's cleanup gate");
            assertSame(restoredHeld, f.contents[0]);
            f.deferred.remove(0).run();
            assertTrue(f.items.begin(f.player, "master_sword"));
            assertEquals(animated(SwordType.MASTER_SWORD), f.model(f.contents[0]));
            assertNotSame(restoredHeld, f.contents[0]);
            assertEquals(base(SwordType.MASTER_SWORD), f.model(restoredHeld));
            assertEquals(base(SwordType.MASTER_SWORD), f.model(original));
            assertEquals(animated(SwordType.MASTER_SWORD), f.model(animatedHeld));
            assertEquals(animated(SwordType.MASTER_SWORD), f.model(display));
        }
    }

    @Test void releaseOnMainThreadRestoresImmediatelyAndDisabledWorkerDoesNotSchedule() {
        try (Fixture f = new Fixture()) {
            f.start();
            ItemStack held = f.sword(SwordType.FLAMEBLADE, true);
            f.contents[0] = held;
            f.items.restorePlayerSoon(f.id);
            f.assertRestoredCopy(held, f.contents[0], SwordType.FLAMEBLADE);
            assertTrue(f.deferred.isEmpty());

            f.primaryThread.set(false);
            f.enabled.set(false);
            f.items.restorePlayerSoon(f.id);
            assertTrue(f.deferred.isEmpty());
            verify(f.scheduler, never()).runTask(eq(f.plugin), any(Runnable.class));
        }
    }

    private static NamespacedKey base(SwordType sword) { return new NamespacedKey("legendaryswords", sword.id()); }
    private static NamespacedKey animated(SwordType sword) { return new NamespacedKey("legendaryswords", sword.id() + "_animated"); }

    private record View(InventoryView view, Map<Integer, ItemStack> slots) {}

    private static final class ItemState {
        NamespacedKey model;
        final Map<NamespacedKey, String> pdc = new HashMap<>();
        final String displayName = "Owner's custom name";
        final List<String> lore = List.of("Retain this custom lore");
        final int damage = 13;
        final int customModelData = 123456;
        final boolean unbreakable = true;

        ItemState copy() {
            ItemState copy = new ItemState();
            copy.model = model;
            copy.pdc.putAll(pdc);
            return copy;
        }
    }

    private static final class Fixture implements AutoCloseable {
        final JavaPlugin plugin = mock(JavaPlugin.class);
        final Server server = mock(Server.class);
        final PluginManager manager = mock(PluginManager.class);
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000007");
        final AtomicBoolean primaryThread = new AtomicBoolean(true);
        final AtomicBoolean enabled = new AtomicBoolean(true);
        final ItemStack[] contents = new ItemStack[41];
        final List<Runnable> deferred = new ArrayList<>();
        final Map<ItemStack, ItemState> states = new IdentityHashMap<>();
        final Map<ItemMeta, ItemState> metaStates = new IdentityHashMap<>();
        final List<PersistentDataContainer> containers = new ArrayList<>();
        final SwordCatalog swords;
        ItemStack cursor;
        AnimatedSwordItems items;

        Fixture() {
            when(plugin.getName()).thenReturn("LegendarySwords");
            when(plugin.namespace()).thenReturn("legendaryswords");
            when(plugin.getServer()).thenReturn(server);
            when(plugin.isEnabled()).thenAnswer(call -> enabled.get());
            when(server.getPluginManager()).thenReturn(manager);
            when(server.getScheduler()).thenReturn(scheduler);
            doReturn(List.of(player)).when(server).getOnlinePlayers();
            when(server.getPlayer(id)).thenReturn(player);
            when(server.isPrimaryThread()).thenAnswer(call -> primaryThread.get());
            when(player.getUniqueId()).thenReturn(id);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getContents()).thenAnswer(call -> contents.clone());
            when(inventory.getItemInMainHand()).thenAnswer(call -> contents[0]);
            doAnswer(call -> { contents[0] = call.getArgument(0); return null; }).when(inventory).setItemInMainHand(any());
            doAnswer(call -> { contents[(int) call.getArgument(0)] = call.getArgument(1); return null; }).when(inventory).setItem(anyInt(), any());
            when(player.getItemOnCursor()).thenAnswer(call -> cursor);
            doAnswer(call -> { cursor = call.getArgument(0); return null; }).when(player).setItemOnCursor(any());
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
                deferred.add(call.getArgument(1));
                return mock(BukkitTask.class);
            });
            swords = new SwordCatalog(plugin, mock(PluginSettings.class));
        }

        void start() { items = new AnimatedSwordItems(plugin, swords); }

        ItemStack sword(SwordType sword, boolean animated) {
            ItemState state = new ItemState();
            state.model = animated ? animated(sword) : base(sword);
            state.pdc.put(SWORD_KEY, sword.id());
            state.pdc.put(new NamespacedKey("custom_plugin", "owner"), "Keep this custom PDC");
            return stack(sword.material(), 1, state);
        }

        ItemStack stack(Material material, int amount, ItemState initial) {
            ItemStack item = mock(ItemStack.class);
            states.put(item, initial.copy());
            when(item.getType()).thenReturn(material);
            when(item.getAmount()).thenReturn(amount);
            when(item.hasItemMeta()).thenReturn(true);
            // Bukkit returns a copy: modifying metadata without setItemMeta must have no effect.
            when(item.getItemMeta()).thenAnswer(call -> meta(states.get(item).copy()));
            when(item.setItemMeta(any(ItemMeta.class))).thenAnswer(call -> {
                states.put(item, metaStates.get(call.getArgument(0)).copy());
                return true;
            });
            when(item.clone()).thenAnswer(call -> stack(material, amount, states.get(item)));
            return item;
        }

        ItemMeta meta(ItemState state) {
            Damageable meta = mock(Damageable.class);
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            metaStates.put(meta, state);
            containers.add(pdc);
            when(meta.getItemModel()).thenAnswer(call -> state.model);
            doAnswer(call -> { state.model = call.getArgument(0); return null; }).when(meta).setItemModel(any());
            when(meta.getPersistentDataContainer()).thenReturn(pdc);
            when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.STRING)))
                    .thenAnswer(call -> state.pdc.get(call.getArgument(0)));
            when(meta.getDamage()).thenReturn(state.damage);
            when(meta.getCustomModelData()).thenReturn(state.customModelData);
            when(meta.isUnbreakable()).thenReturn(state.unbreakable);
            return meta;
        }

        NamespacedKey model(ItemStack item) { return states.get(item).model; }

        Map<String, Object> metadata(ItemStack item) {
            ItemState state = states.get(item);
            return Map.of("pdc", Map.copyOf(state.pdc), "name", state.displayName, "lore", state.lore,
                    "damage", state.damage, "customModelData", state.customModelData, "unbreakable", state.unbreakable);
        }

        void assertRestoredCopy(ItemStack source, ItemStack replacement, SwordType sword) {
            assertNotSame(source, replacement, "Restoration must replace the live stack with a detached copy");
            assertEquals(animated(sword), model(source), "A queued source reference must retain its animated model");
            assertEquals(base(sword), model(replacement));
            assertEquals(metadata(source), metadata(replacement));
            assertEquals(source.getType(), replacement.getType());
            assertEquals(source.getAmount(), replacement.getAmount());
            verify(source, never()).setItemMeta(any());
        }

        void assertOnlyModelWasWritten() {
            for (ItemMeta meta : metaStates.keySet()) {
                for (var invocation : mockingDetails(meta).getInvocations()) {
                    String name = invocation.getMethod().getName();
                    if (name.startsWith("set") || name.startsWith("add") || name.startsWith("remove")
                            || (invocation.getArguments().length == 1 && (name.equals("displayName") || name.equals("lore"))))
                        assertEquals("setItemModel", name, "Other metadata must not be changed");
                }
            }
            for (PersistentDataContainer pdc : containers) {
                verify(pdc, never()).set(any(), any(), any());
                verify(pdc, never()).remove(any());
            }
        }

        View view(int count) {
            InventoryView view = mock(InventoryView.class);
            Map<Integer, ItemStack> slots = new HashMap<>();
            when(view.countSlots()).thenReturn(count);
            when(view.getItem(anyInt())).thenAnswer(call -> slots.get(call.getArgument(0)));
            doAnswer(call -> { slots.put(call.getArgument(0), call.getArgument(1)); return null; }).when(view).setItem(anyInt(), any());
            return new View(view, slots);
        }

        void nextTick() {
            List<Runnable> work = List.copyOf(deferred);
            deferred.clear();
            work.forEach(Runnable::run);
        }

        @Override public void close() { if (items != null) items.close(); }
    }
}
