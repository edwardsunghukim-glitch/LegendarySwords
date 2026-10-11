package io.github.tempblocks;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemporaryBlocksTest {
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
