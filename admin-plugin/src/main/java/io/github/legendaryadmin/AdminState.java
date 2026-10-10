package io.github.legendaryadmin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

/** Mutes survive restarts; chat threads only read the concurrent set. */
final class AdminState {
    private final Path file;
    private final Set<UUID> muted = ConcurrentHashMap.newKeySet();
    AdminState(Path file) {
        this.file = file;
        for (String id : YamlConfiguration.loadConfiguration(file.toFile()).getStringList("muted")) {
            try { muted.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
        }
    }
    boolean muted(UUID id) { return muted.contains(id); }
    boolean toggleMute(UUID id) throws IOException {
        boolean enabled = !muted.contains(id);
        if (enabled) muted.add(id); else muted.remove(id);
        try {
            java.nio.file.Files.createDirectories(file.getParent());
            var yaml = new YamlConfiguration();
            yaml.set("muted", muted.stream().map(UUID::toString).sorted().toList());
            // Write a complete temporary file before replacing the persistent state.
            Path temporary = java.nio.file.Files.createTempFile(file.getParent(), "mutes-", ".yml");
            try {
                yaml.save(temporary.toFile());
                java.nio.file.Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally { java.nio.file.Files.deleteIfExists(temporary); }
        } catch (IOException error) {
            if (enabled) muted.remove(id); else muted.add(id);
            throw error;
        }
        return enabled;
    }
}
