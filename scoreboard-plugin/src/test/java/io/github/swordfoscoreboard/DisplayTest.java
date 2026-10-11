package io.github.swordfoscoreboard;
import org.junit.jupiter.api.Test;
import org.bukkit.command.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DisplayTest {
    @Test void lineEntriesAreUniqueIncludingDuplicateBlankLines() {
        Set<String> entries=new HashSet<>();for(int i=0;i<15;i++)assertTrue(entries.add(SwordFoScoreboard.entry(i)));
        assertThrows(IllegalArgumentException.class,()->SwordFoScoreboard.entry(15));
    }
    @Test void scoreboardFormattingKeepsValuesAndColorCodes() {
        assertEquals("&6Wallet &f1,234",DisplayText.replace("&6Wallet &f{wallet}",Map.of("wallet","1,234")));
        assertEquals("swordfo.minehut.gg",DisplayText.replace("{ip}",Map.of("ip","swordfo.minehut.gg")));
    }
    @Test void reloadRequiresAdminBeforeChangingScoreboards() {
        SwordFoScoreboard plugin=mock(SwordFoScoreboard.class,CALLS_REAL_METHODS);CommandSender sender=mock(CommandSender.class);
        assertTrue(plugin.onCommand(sender,mock(Command.class),"swordboard",new String[]{"reload"}));
        verify(sender).sendMessage("No permission.");verify(plugin,never()).reloadConfig();
    }
}
