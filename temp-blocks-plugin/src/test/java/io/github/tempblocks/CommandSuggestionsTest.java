package io.github.tempblocks;
import org.junit.jupiter.api.Test;
import org.bukkit.command.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CommandSuggestionsTest {
    @Test void filtersPrefixesCaseInsensitivelyAndRemovesDuplicates() {
        assertEquals(List.of("reload"),CommandSuggestions.matching(List.of("reload","reload","start"),"RE"));
        assertTrue(CommandSuggestions.matching(List.of("reload"),"z").isEmpty());
    }
    @Test void unknownOrExtraArgumentsDoNotFallBackToPlayerNameSuggestions() {
        TemporaryBlocks plugin=mock(TemporaryBlocks.class,CALLS_REAL_METHODS);CommandSender sender=mock(CommandSender.class);Command cmd=mock(Command.class);
        when(cmd.getName()).thenReturn("unrecognized");
        assertNotNull(plugin.onTabComplete(sender,cmd,"alias",new String[]{"unknown","too","many","arguments",""}));
        assertTrue(plugin.onTabComplete(sender,cmd,"alias",new String[]{"unknown","too","many","arguments",""}).isEmpty());
        verify(plugin,never()).getServer();
    }
}
