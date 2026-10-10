package io.github.legendaryswords.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import org.bukkit.plugin.java.JavaPlugin;

/** Updates only files known to be previous bundled assets; edited server models are preserved. */
final class BundledModelInstaller {
    private static final Map<String, String> V01 = Map.of(
            "models/legendaryswords_fierce_deity_sword_slash.bbmodel", "0995a2c13c2419b1154efcdacae66aa64f332a086ad07017b110b14a55e7ad6a",
            "models/legendaryswords_flameblade_slash.bbmodel", "c266ff40946f2962596be88a00ad94e18db87c136fddeeaada1fe1d6e789a329",
            "models/legendaryswords_master_sword_slash.bbmodel", "fd07af2ab3eecd75bb7cb169a70ce172571e68279c4c50dfe61eaa3fb86e3f76",
            "players/legendaryswords_fierce_deity_sword.bbmodel", "670b1e2815a3ae2e362610f66fe0afed9dcaf0989c9fd037047a8aee7cbdfa9d",
            "players/legendaryswords_flameblade.bbmodel", "afecdb4d71822a0ecff6778cf058bfabbd3a132d77fe5997e962c4763bcc5c42",
            "players/legendaryswords_master_sword.bbmodel", "6f4d43dcd1277e4d4bcdb26182e2fa0cf77ceaad6c3c9a374e39ddad454a5d4c");
    private final JavaPlugin plugin;
    private final Path engineFolder;
    private final Path manifest;
    private final Properties installed = new Properties();

    BundledModelInstaller(JavaPlugin plugin, Path engineFolder) throws IOException {
        this.plugin = plugin;
        this.engineFolder = engineFolder;
        manifest = plugin.getDataFolder().toPath().resolve("bettermodel-installed.properties");
        if (Files.isRegularFile(manifest)) try (InputStream input = Files.newInputStream(manifest)) {
            installed.load(input);
        }
    }

    Result install(String relative) throws IOException {
        byte[] bundled;
        try (InputStream input = plugin.getResource("bettermodel/" + relative)) {
            if (input == null) throw new IOException("Missing jar resource bettermodel/" + relative);
            bundled = input.readAllBytes();
        }
        Result result = install(engineFolder.resolve(relative), bundled, installed.getProperty(relative),
                V01.get(relative), plugin.getDataFolder().toPath().resolve("bettermodel-backups").resolve(relative));
        if (result != Result.PRESERVED) installed.setProperty(relative, hash(bundled));
        return result;
    }

    void save() throws IOException {
        Files.createDirectories(manifest.getParent());
        Path temporary = Files.createTempFile(manifest.getParent(), "bettermodel-manifest-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                installed.store(output, "Hashes of models installed by LegendarySwords; edited files are never replaced");
            }
            Files.move(temporary, manifest, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static Result install(Path destination, byte[] bundled, String previousHash, String legacyHash,
                          Path backup) throws IOException {
        Files.createDirectories(destination.getParent());
        if (!Files.exists(destination)) {
            Files.write(destination, bundled, java.nio.file.StandardOpenOption.CREATE_NEW);
            return Result.NEW;
        }
        if (!Files.isRegularFile(destination) || Files.isSymbolicLink(destination)) return Result.PRESERVED;
        String existing = hash(Files.readAllBytes(destination));
        if (existing.equals(hash(bundled))) return Result.UNCHANGED;
        if (!existing.equals(previousHash) && !existing.equals(legacyHash)) return Result.PRESERVED;
        Files.createDirectories(backup.getParent());
        Path specificBackup = backup.resolveSibling(backup.getFileName() + "." + existing + ".bak");
        if (!Files.exists(specificBackup)) Files.copy(destination, specificBackup);
        Path temporary = Files.createTempFile(destination.getParent(), "legendaryswords-upgrade-", ".tmp");
        try {
            Files.write(temporary, bundled);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return Result.UPDATED;
    }

    static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Java runtime does not supply SHA-256", impossible);
        }
    }

    enum Result { NEW, UPDATED, UNCHANGED, PRESERVED }
}
