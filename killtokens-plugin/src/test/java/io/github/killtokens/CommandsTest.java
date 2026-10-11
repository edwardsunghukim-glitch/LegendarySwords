package io.github.killtokens;
import org.bukkit.command.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CommandsTest {
    @Test void namespacedEscapeCommandsUseSameBlockedRoot() {
        assertEquals("home",KillTokens.commandRoot("/essentials:HOME base"));assertEquals("spawn",KillTokens.commandRoot("/spawn"));
    }
    @Test void adminActionsRejectUnauthorizedSenderBeforeAccessingBank() {
        KillTokens plugin=mock(KillTokens.class,CALLS_REAL_METHODS);Command cmd=mock(Command.class);CommandSender sender=mock(CommandSender.class);
        when(cmd.getName()).thenReturn("killtokens");
        assertTrue(plugin.onCommand(sender,cmd,"kt",new String[]{"multiplier","2"}));
        assertTrue(plugin.onCommand(sender,cmd,"kt",new String[]{"reload"}));verify(plugin,never()).reloadConfig();
        verify(sender,times(2)).sendMessage("No permission to manage KillTokens.");
    }
    @Test void walletGiveAndSetRequireAdminPermission() {
        KillTokens plugin=mock(KillTokens.class,CALLS_REAL_METHODS);Command cmd=mock(Command.class);CommandSender sender=mock(CommandSender.class);
        for(String command:java.util.List.of("killtokengive","killtokenset")) {
            when(cmd.getName()).thenReturn(command);assertTrue(plugin.onCommand(sender,cmd,command,new String[]{"player","10"}));
        }
        verify(sender,times(2)).sendMessage("No permission to manage KillTokens.");
    }
    @Test void rewardMultipliersScaleDropsAndRespectConfiguredCap() {
        assertEquals(1,KillTokens.rewardAmount(1,1,1,2304));
        assertEquals(6,KillTokens.rewardAmount(1,2,3,2304));
        assertEquals(10,KillTokens.rewardAmount(100,100,100,10));
        assertEquals(0,KillTokens.rewardAmount(-1,2,2,2304));
    }
}
