package io.github.swordfoscoreboard;
import org.junit.jupiter.api.Test;
import org.bukkit.command.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DisplayTest {
    public static class TokenSource extends org.bukkit.plugin.java.JavaPlugin {
        public long getWalletBalance(UUID player){return 0;}
        public long getCombatSeconds(UUID player){return 0;}
    }
    public static class KothSource extends org.bukkit.plugin.java.JavaPlugin {
        public String getDisplayStatus(){return "Inactive";}
    }
    @Test void liveProviderDataIsReadAgainOnEveryRefresh() {
        var manager=mock(org.bukkit.plugin.PluginManager.class);var tokens=mock(TokenSource.class);var koth=mock(KothSource.class);var p=mock(org.bukkit.entity.Player.class);
        UUID id=UUID.randomUUID();when(p.getUniqueId()).thenReturn(id);when(p.getName()).thenReturn("Alex");
        when(tokens.isEnabled()).thenReturn(true);when(koth.isEnabled()).thenReturn(true);
        when(manager.getPlugin("KillTokens")).thenReturn(tokens);when(manager.getPlugin("LegendaryKoTH")).thenReturn(koth);
        when(tokens.getWalletBalance(id)).thenReturn(10L,25L);when(tokens.getCombatSeconds(id)).thenReturn(5L);
        when(koth.getDisplayStatus()).thenReturn("50/100s");
        try(var bukkit=mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getPluginManager).thenReturn(manager);bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenReturn(List.of(p));
            assertEquals("10",DisplayText.values(p,"swordfo.minehut.gg").get("wallet"));
            var next=DisplayText.values(p,"swordfo.minehut.gg");assertEquals("25",next.get("wallet"));assertEquals("5s",next.get("combat"));assertEquals("50/100s",next.get("koth"));
        }
    }
    @Test void changingLiveWalletUpdatesExistingTeamWithoutRecreatingBoard() {
        var b=new SwordFoScoreboard.Board();b.current=mock(org.bukkit.scoreboard.Scoreboard.class);b.objective=mock(org.bukkit.scoreboard.Objective.class);
        for(int i=0;i<15;i++)b.teams[i]=mock(org.bukkit.scoreboard.Team.class);
        when(b.objective.getScore(anyString())).thenReturn(mock(org.bukkit.scoreboard.Score.class));
        SwordFoScoreboard.render(b,List.of("&6Wallet &f{wallet}"),Map.of("wallet","10"));
        SwordFoScoreboard.render(b,List.of("&6Wallet &f{wallet}"),Map.of("wallet","25"));
        assertEquals("&6Wallet &f25",b.shown[0]);verify(b.teams[0],times(2)).prefix(any(net.kyori.adventure.text.Component.class));
        verify(b.current,never()).registerNewTeam(anyString());
    }
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
