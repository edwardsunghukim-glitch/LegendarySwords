package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ArmorAbilitiesTest {
    @Test void breathTracksTheAimIncludingVerticalDirections() {
        assertTrue(ArmorAbilities.inCone(new Vector(),new Vector(0,0,1),new Vector(0,0,10),17,.85));
        assertFalse(ArmorAbilities.inCone(new Vector(),new Vector(0,0,1),new Vector(0,0,-10),17,.85));
        assertFalse(ArmorAbilities.inCone(new Vector(),new Vector(0,0,1),new Vector(10,0,0),17,.85));
        assertFalse(ArmorAbilities.inCone(new Vector(),new Vector(0,0,1),new Vector(0,0,20),17,.85));
        assertTrue(ArmorAbilities.inCone(new Vector(),new Vector(0,1,0),new Vector(0,10,0),17,.85));
    }
    @Test void knockbackAtExactCenterIsFinite(){Vector result=ArmorAbilities.outward(new Vector(),new Vector());assertEquals(.55,result.getY());assertTrue(Double.isFinite(result.getX()));}
    @Test void pvpAndFriendlyTeamsAreRespected() {
        JavaPlugin plugin=mock(JavaPlugin.class);YamlConfiguration config=new YamlConfiguration();when(plugin.getConfig()).thenReturn(config);
        ArmorAbilities abilities=new ArmorAbilities(plugin,mock(ArmorCatalog.class));Player owner=mock(Player.class),target=mock(Player.class);World world=mock(World.class);
        var board=mock(org.bukkit.scoreboard.Scoreboard.class);when(owner.getWorld()).thenReturn(world);when(owner.getScoreboard()).thenReturn(board);when(owner.getName()).thenReturn("A");when(target.getName()).thenReturn("B");when(owner.canSee(target)).thenReturn(true);when(target.getGameMode()).thenReturn(GameMode.SURVIVAL);
        assertFalse(abilities.eligible(owner,target));when(world.getPVP()).thenReturn(true);assertTrue(abilities.eligible(owner,target));config.set("pvp",false);assertFalse(abilities.eligible(owner,target));config.set("pvp",true);
        var team=mock(org.bukkit.scoreboard.Team.class);when(board.getEntryTeam("A")).thenReturn(team);when(team.hasEntry("B")).thenReturn(true);assertFalse(abilities.eligible(owner,target));config.set("friendly-fire",true);assertTrue(abilities.eligible(owner,target));
        assertFalse(abilities.eligible(owner,owner));when(target.getGameMode()).thenReturn(GameMode.SPECTATOR);assertFalse(abilities.eligible(owner,target));
    }
}
