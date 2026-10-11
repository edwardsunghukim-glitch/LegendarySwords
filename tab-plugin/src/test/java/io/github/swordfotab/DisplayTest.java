package io.github.swordfotab;
import org.junit.jupiter.api.Test;
import org.bukkit.command.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DisplayTest {
    @Test void headerFooterFormattingPreservesLineBreaksAndIp() {
        assertEquals("&6SWORDFO\n&7Online 5/20\nswordfo.minehut.gg",DisplayText.replace("&6SWORDFO\n&7Online {online}/{maxplayers}\n{ip}",Map.of("online","5","maxplayers","20","ip","swordfo.minehut.gg")));
    }
    @Test void colorCodesAreConvertedToComponents() {
        assertEquals(net.kyori.adventure.text.format.NamedTextColor.GOLD,DisplayText.component("&6SwordFo").color());
    }
    @Test void reloadRequiresAdminPermission() {
        SwordFoTab plugin=mock(SwordFoTab.class,CALLS_REAL_METHODS);CommandSender sender=mock(CommandSender.class);
        assertTrue(plugin.onCommand(sender,mock(Command.class),"swordtab",new String[]{"reload"}));verify(plugin,never()).reloadConfig();verify(sender).sendMessage("No permission.");
    }
}
