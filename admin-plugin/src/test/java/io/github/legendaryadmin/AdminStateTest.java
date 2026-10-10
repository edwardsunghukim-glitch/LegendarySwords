package io.github.legendaryadmin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AdminStateTest {
    @TempDir Path directory;
    @Test void muteAndUnmuteSurviveReloadAndDoNotAffectAnotherPlayer() throws Exception {
        Path file = directory.resolve("data/mutes.yml");
        UUID player = UUID.randomUUID(), other = UUID.randomUUID();
        var state = new AdminState(file);
        assertTrue(state.toggleMute(player));
        var restored = new AdminState(file);
        assertTrue(restored.muted(player));
        assertFalse(restored.muted(other));
        assertFalse(restored.toggleMute(player));
        assertFalse(new AdminState(file).muted(player));
    }
    @Test void aFailedSaveRollsBackTheMute() throws Exception {
        Path parent = directory.resolve("not-a-directory");
        Files.writeString(parent, "file");
        UUID player = UUID.randomUUID();
        var state = new AdminState(parent.resolve("mutes.yml"));
        assertThrows(java.io.IOException.class, () -> state.toggleMute(player));
        assertFalse(state.muted(player));
    }
    @Test void malformedIdsDoNotPreventValidMutesLoading() throws Exception {
        UUID player = UUID.randomUUID();
        Path file = directory.resolve("mutes.yml");
        Files.writeString(file, "muted:\n  - invalid\n  - " + player + "\n");
        assertTrue(new AdminState(file).muted(player));
    }
}
