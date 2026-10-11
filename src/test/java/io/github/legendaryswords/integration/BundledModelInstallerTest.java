package io.github.legendaryswords.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundledModelInstallerTest {
    @TempDir Path folder;
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    @Test void newFileIsInstalledAndUnchangedFileIsNotBackedUp() throws Exception {
        Path destination = folder.resolve("engine/players/master.bbmodel");
        Path backup = folder.resolve("backups/master.bbmodel");
        assertEquals(BundledModelInstaller.Result.NEW,
                BundledModelInstaller.install(destination, bytes("new"), null, null, backup));
        assertEquals(BundledModelInstaller.Result.UNCHANGED,
                BundledModelInstaller.install(destination, bytes("new"), null, null, backup));
        assertFalse(Files.exists(backup.getParent()));
    }

    @Test void recognizedOriginalIsUpgradedWithExactBackup() throws Exception {
        Path destination = folder.resolve("original.bbmodel");
        Path backup = folder.resolve("backups/original.bbmodel");
        Files.write(destination, bytes("old shipped model"));
        String originalHash = BundledModelInstaller.hash(bytes("old shipped model"));
        assertEquals(BundledModelInstaller.Result.UPDATED,
                BundledModelInstaller.install(destination, bytes("new animations"), null, originalHash, backup));
        assertEquals("new animations", Files.readString(destination));
        assertEquals("old shipped model", Files.readString(backup.resolveSibling(
                backup.getFileName() + "." + originalHash + ".bak")));
    }

    @Test void manifestRecognizesPreviouslyInstalledVersion() throws Exception {
        Path destination = folder.resolve("versioned.bbmodel");
        Files.write(destination, bytes("previous release"));
        assertEquals(BundledModelInstaller.Result.UPDATED, BundledModelInstaller.install(destination,
                bytes("current release"), BundledModelInstaller.hash(bytes("previous release")), null,
                folder.resolve("backups/versioned.bbmodel")));
    }

    @Test void editedModelIsNeverOverwrittenOrRecordedAsInstalled() throws Exception {
        Path destination = folder.resolve("edited.bbmodel");
        Path backup = folder.resolve("backups/edited.bbmodel");
        Files.write(destination, bytes("server owner's changes"));
        assertEquals(BundledModelInstaller.Result.PRESERVED, BundledModelInstaller.install(destination,
                bytes("bundled"), BundledModelInstaller.hash(bytes("earlier bundled")), null, backup));
        assertEquals("server owner's changes", Files.readString(destination));
        assertFalse(Files.exists(backup.getParent()));
    }

    @Test void symbolicLinkIsPreserved() throws Exception {
        Path target = folder.resolve("outside.bbmodel");
        Files.write(target, bytes("custom"));
        Path destination = folder.resolve("linked.bbmodel");
        Files.createSymbolicLink(destination, target);
        assertEquals(BundledModelInstaller.Result.PRESERVED,
                BundledModelInstaller.install(destination, bytes("bundled"), null,
                        BundledModelInstaller.hash(bytes("custom")), folder.resolve("backup")));
        assertEquals("custom", Files.readString(target));
    }

    @Test void manuallyInstalledPublishedBossesAreUpgradedWithoutAManifest() throws Exception {
        for (String version : new String[]{"0.5.0", "0.5.1", "0.5.2"}) {
            {
                var archive = publishedBosses(version);
                for (String boss : new String[]{"angel_sovereign", "granite_colossus", "sky_leviathan", "infernal_dragon", "frost_titan"}) {
                    String relative = "models/legendaryswords_" + boss + "_boss.bbmodel";
                    byte[] original = archive.get(relative);
                    assertNotNull(original);
                    Path destination = folder.resolve(version + "/" + relative);
                    Files.createDirectories(destination.getParent()); Files.write(destination, original);
                    Path backup = folder.resolve("backups/" + version + "/" + relative);
                    String trusted = BundledModelInstaller.legacyHash(relative, original);
                    assertEquals(BundledModelInstaller.hash(original), trusted);
                    assertEquals(BundledModelInstaller.Result.UPDATED,
                            BundledModelInstaller.install(destination, bytes("current"), null, trusted, backup));
                    assertArrayEquals(original, Files.readAllBytes(backup.resolveSibling(backup.getFileName() + "." + trusted + ".bak")));
                }
            }
        }
    }

    @Test void editedOrMisnamedBossFilesRemainProtected() throws Exception {
        String relative = "models/legendaryswords_sky_leviathan_boss.bbmodel";
        {
            var archive = publishedBosses("0.5.1");
            byte[] original = archive.get(relative);
            assertNotNull(original);
            byte[] edited = java.util.Arrays.copyOf(original, original.length + 1);
            edited[edited.length - 1] = '\n';
            assertNull(BundledModelInstaller.legacyHash(relative, edited));
            assertNull(BundledModelInstaller.legacyHash("models/legendaryswords_infernal_dragon_boss.bbmodel", original));
            Path destination = folder.resolve("edited-boss.bbmodel"); Files.write(destination, edited);
            assertEquals(BundledModelInstaller.Result.PRESERVED, BundledModelInstaller.install(destination,
                    bytes("current"), null, BundledModelInstaller.legacyHash(relative, edited), folder.resolve("backup")));
            assertArrayEquals(edited, Files.readAllBytes(destination));
        }
    }


    private java.util.Map<String, byte[]> publishedBosses(String version) throws Exception {
        var result = new java.util.HashMap<String, byte[]>();
        try (var input = getClass().getResourceAsStream("/bettermodel-legacy/" + version + ".zip")) {
            assertNotNull(input);
            try (var zip = new java.util.zip.ZipInputStream(input)) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    result.put(entry.getName(), zip.readAllBytes());
                }
            }
        }
        assertEquals(5, result.size());
        return result;
    }

}
