package io.github.legendaryswords;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.legendaryswords.combat.CinematicDirector;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SwordCommandTest {
    @TempDir Path directory;

    @Test void enablingCinematicsIsIdempotentAndPersistsWithoutCancellingAnActiveScene() {
        LegendarySwordsPlugin plugin = mock(LegendarySwordsPlugin.class);
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LegendarySwords-command-tests"));
        PluginSettings settings = mock(PluginSettings.class);
        when(settings.cinematicsEnabled()).thenReturn(true);
        when(plugin.settings()).thenReturn(settings);
        CinematicDirector cinematic = mock(CinematicDirector.class);
        when(cinematic.active(player)).thenReturn(true);
        when(plugin.cinematics()).thenReturn(cinematic);
        PlayerStateStore states = new PlayerStateStore(plugin);
        when(plugin.states()).thenReturn(states);
        states.setCinematics(id, false);
        SwordCommand commands = new SwordCommand(plugin);

        commands.onCommand(player, null, "ls", new String[]{"cinematics", "on"});
        commands.onCommand(player, null, "ls", new String[]{"cinematics", "on"});
        commands.onCommand(player, null, "ls", new String[]{"cinematics", "status"});
        assertTrue(states.cinematics(id));
        verifyNoInteractions(cinematic);
        states.close();
        PlayerStateStore reloaded = new PlayerStateStore(plugin);
        assertTrue(reloaded.cinematics(id));
        when(plugin.states()).thenReturn(reloaded);

        commands.onCommand(player, null, "ls", new String[]{"cinematics", "off"});
        assertFalse(reloaded.cinematics(id));
        verify(cinematic).cancel(player);
        commands.onCommand(player, null, "ls", new String[]{"cinematics", "status"});
        commands.onCommand(player, null, "ls", new String[]{"cinematics", "invalid"});
        assertFalse(reloaded.cinematics(id));
        verify(cinematic, times(1)).cancel(player);
        reloaded.close();
    }
}
