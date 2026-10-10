package io.github.legendaryswords.combat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.legendaryswords.LegendarySwordsPlugin;
import io.github.legendaryswords.integration.AnimatedSwordItems;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

class EffectDisplayTest {
    @Test void spellBladeClonesUseOrdinaryFixedTransformsAndLeaveInventoryUntouched() {
        LegendarySwordsPlugin plugin = mock(LegendarySwordsPlugin.class);
        AnimatedSwordItems items = mock(AnimatedSwordItems.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(plugin.animatedSwordItems()).thenReturn(items);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(2L), eq(2L)))
                .thenReturn(mock(BukkitTask.class));
        World world = mock(World.class);
        ItemDisplay display = mock(ItemDisplay.class);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), org.mockito.ArgumentMatchers.<Consumer<ItemDisplay>>any()))
                .thenAnswer(call -> {
                    Location anchor = call.getArgument(0);
                    assertEquals(0f, anchor.getYaw());
                    assertEquals(0f, anchor.getPitch());
                    Consumer<ItemDisplay> initialize = call.getArgument(2);
                    initialize.accept(display);
                    return display;
                });
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        ItemStack held = mock(ItemStack.class), clone = mock(ItemStack.class);
        when(held.clone()).thenReturn(clone);
        try (var effects = new EffectDisplay(plugin)) {
            assertSame(display, effects.blade(player, new Location(world, 0, 64, 0, 135, -40), held, 5.5, Color.AQUA, 40));
            verify(items).restoreItem(clone);
            verify(items, never()).restoreItem(held);
            verify(display).setItemStack(clone);
            verify(display).setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        }
    }
}
