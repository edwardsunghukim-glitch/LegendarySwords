package io.github.tempblocks;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemporaryBlocksTest {
    @Test void originalBrokenBlockGetsRestoredAfterDeadline() {
        var world=java.util.UUID.randomUUID();
        var entry=TemporaryBlocks.brokenEntry(null,world,1,2,3,"minecraft:stone",30000);
        assertEquals("minecraft:air",entry.data());assertEquals("minecraft:stone",entry.restore());
        assertEquals(30000,entry.expiry());
    }
    @Test void breakingTemporaryAdditionDoesNotResurrectIt() {
        var world=java.util.UUID.randomUUID();
        var placed=new TemporaryBlocks.Entry(world,1,2,3,"minecraft:stone","minecraft:air",30000);
        assertNull(TemporaryBlocks.brokenEntry(placed,world,1,2,3,"minecraft:stone",40000));
    }
    @Test void placingAndBreakingInsideTemporaryHolePreservesOriginal() {
        var world=java.util.UUID.randomUUID();
        var replacement=new TemporaryBlocks.Entry(world,1,2,3,"minecraft:dirt","minecraft:oak_log[axis=x]",30000);
        var result=TemporaryBlocks.brokenEntry(replacement,world,1,2,3,"minecraft:dirt",40000);
        assertEquals("minecraft:oak_log[axis=x]",result.restore());assertEquals("minecraft:air",result.data());
    }
    @Test void deniedBypassNeverTouchesPlacementState() {
        TemporaryBlocks plugin = mock(TemporaryBlocks.class, CALLS_REAL_METHODS);
        Player player = mock(Player.class);
        assertTrue(plugin.onCommand(player, mock(Command.class), "tb", new String[]{"bypass"}));
        verify(player).hasPermission("tempblocks.bypass");
        verify(player).sendMessage("You do not have permission to bypass temporary placement.");
        verify(player, never()).getUniqueId();
    }
    @Test void changingDoorStateStillExpiresButDifferentBlockIsProtected() {
        assertTrue(TemporaryBlocks.sameType("minecraft:oak_door[open=true]", "minecraft:oak_door[open=false]"));
        assertFalse(TemporaryBlocks.sameType("minecraft:stone", "minecraft:oak_door[open=false]"));
    }
}
