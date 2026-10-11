package io.github.killtokens;
import org.junit.jupiter.api.Test;
import org.bukkit.command.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CommandSuggestionsTest {
    @Test void playerSuggestionsDoNotRevealHiddenPlayers() {
        KillTokens plugin=mock(KillTokens.class,CALLS_REAL_METHODS);org.bukkit.Server server=mock(org.bukkit.Server.class);
        org.bukkit.entity.Player viewer=mock(org.bukkit.entity.Player.class),visible=mock(org.bukkit.entity.Player.class),hidden=mock(org.bukkit.entity.Player.class);
        when(plugin.getServer()).thenReturn(server);doReturn(List.of(visible,hidden)).when(server).getOnlinePlayers();
        when(viewer.hasPermission("killtokens.admin")).thenReturn(true);when(viewer.canSee(visible)).thenReturn(true);
        when(visible.getName()).thenReturn("Alex");when(hidden.getName()).thenReturn("HiddenAdmin");
        Command cmd=mock(Command.class);when(cmd.getName()).thenReturn("killtokengive");
        assertEquals(List.of("Alex"),plugin.onTabComplete(viewer,cmd,"killtokengive",new String[]{""}));
    }
    @Test void multiplierAndDepositSuggestionsRespectPermissions() {
        KillTokens plugin=mock(KillTokens.class,CALLS_REAL_METHODS);CommandSender sender=mock(CommandSender.class);Command cmd=mock(Command.class);
        when(cmd.getName()).thenReturn("killtokens");
        assertTrue(plugin.onTabComplete(sender,cmd,"killtoken",new String[]{"multiplier","o"}).isEmpty());
        when(sender.hasPermission("killtokens.admin")).thenReturn(true);
        assertEquals(List.of("off"),plugin.onTabComplete(sender,cmd,"killtoken",new String[]{"multiplier","o"}));
        when(cmd.getName()).thenReturn("deposit");when(sender.hasPermission("killtokens.use")).thenReturn(true);
        assertEquals(List.of("all"),plugin.onTabComplete(sender,cmd,"deposit",new String[]{"a"}));
    }
    @Test void filtersPrefixesCaseInsensitivelyAndRemovesDuplicates() {
        assertEquals(List.of("reload"),CommandSuggestions.matching(List.of("reload","reload","start"),"RE"));
        assertTrue(CommandSuggestions.matching(List.of("reload"),"z").isEmpty());
    }
    @Test void unknownOrExtraArgumentsDoNotFallBackToPlayerNameSuggestions() {
        KillTokens plugin=mock(KillTokens.class,CALLS_REAL_METHODS);CommandSender sender=mock(CommandSender.class);Command cmd=mock(Command.class);
        when(cmd.getName()).thenReturn("unrecognized");
        assertNotNull(plugin.onTabComplete(sender,cmd,"alias",new String[]{"unknown","too","many","arguments",""}));
        assertTrue(plugin.onTabComplete(sender,cmd,"alias",new String[]{"unknown","too","many","arguments",""}).isEmpty());
        verify(plugin,never()).getServer();
    }
}
