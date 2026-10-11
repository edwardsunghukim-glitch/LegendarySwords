package io.github.cleardrops;

import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.command.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClearDropsTest {
    @Test void removesValidDropsAndCountsEntitiesNotStackSizes() {
        World world=mock(World.class); Item drop=mock(Item.class), gone=mock(Item.class);
        when(world.getName()).thenReturn("world");when(drop.isValid()).thenReturn(true);
        when(world.getEntitiesByClass(Item.class)).thenReturn(List.of(drop,gone));
        assertEquals(1,ClearDrops.cleanup(List.of(world),Set.of()));
        verify(drop).remove();verify(gone,never()).remove();verify(world,never()).getEntities();
    }
    @Test void excludedWorldIsNeverScanned() {
        World world=mock(World.class);when(world.getName()).thenReturn("protected");
        assertEquals(0,ClearDrops.cleanup(List.of(world),Set.of("protected")));
        verify(world,never()).getEntitiesByClass(Item.class);
    }
    @Test void unauthorizedManagementDoesNotAccessServer() {
        ClearDrops plugin=mock(ClearDrops.class,CALLS_REAL_METHODS);CommandSender sender=mock(CommandSender.class);
        for(String action:List.of("now","reload")) assertTrue(plugin.onCommand(sender,mock(Command.class),"cleardrops",new String[]{action}));
        verify(plugin,never()).getServer();verify(plugin,never()).reloadConfig();
    }
}
