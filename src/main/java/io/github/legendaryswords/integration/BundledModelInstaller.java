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
import java.util.Set;
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
    // Exact published originals can be upgraded even when installed manually.
    private static final Map<String, Set<String>> PREVIOUS_BOSS_FILES = Map.of(
            "models/legendaryswords_angel_sovereign_boss.bbmodel", Set.of("2ad25233247cddfae940a914365b663eb73e5cb1b0d91ca1c0721efecb951b19", "bc2f856bc348996a11d789f988351cb4ec9169efddf18f8f5582b245adbff2b0", "b511512e7201fe5a0826ec3c6cc5bfd51d68a08998ead97e05db819315aa5c58"),
            "models/legendaryswords_granite_colossus_boss.bbmodel", Set.of("dd83506e3cc37a78551231178cff24fd0aa244a72c4f4d9b4f1f54f90930f407", "8c98f656e6dcedb00424a8f3f2b577281b8c7a2be22bc673adba95a541cda894", "9797870a0407e728fe90fb28a9626f91e8bfec7bcbef72c4e92c9d39ba2c6ee8"),
            "models/legendaryswords_sky_leviathan_boss.bbmodel", Set.of("e2ab4c6b26f90a25ac18bd69197ce6a06a0f24cd888c7cec29a679e76f9958a1", "ae8906b3fe154580aae381c2285d24ad5021e4429ece2b12b6795bce30edcef1", "9994d543dde1f849e610a737f456ddd7bd92d51172b2e48eed83608eb09bc97b"),
            "models/legendaryswords_infernal_dragon_boss.bbmodel", Set.of("c5bbcc82489f659bc8084dcda99547f2a34e5881e57e0ce493fcf9461a9867ad", "555da9c62c088fa843358f16ad5ffc8eff35e9fe283c57c4e4792537970c598d", "1d9bf71e22cc0aef58432fa492699e702f269c1065e1490cdf70ccdc0ab2ac70"),
            "models/legendaryswords_frost_titan_boss.bbmodel", Set.of("4b45a6714300499c4b914e9f936d721625f0768f04d95df2c6d5d54b5570a6cb", "d0071e8c07d8a4ec7fc8f48dddcc1d475201d54314adb455d66fc63d9447fe47", "c3735cab1dd677bac0934e188e9d768e56ab675e4c4486df371fa7eb5b978736"));
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
        Path destination = engineFolder.resolve(relative);
        String legacy = Files.isRegularFile(destination) && !Files.isSymbolicLink(destination)
                ? legacyHash(relative, Files.readAllBytes(destination)) : V01.get(relative);
        Result result = install(destination, bundled, installed.getProperty(relative),
                legacy, plugin.getDataFolder().toPath().resolve("bettermodel-backups").resolve(relative));
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

    static String legacyHash(String relative, byte[] existing) {
        String actual = hash(existing);
        return PREVIOUS_BOSS_FILES.getOrDefault(relative, Set.of()).contains(actual) ? actual : V01.get(relative);
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
