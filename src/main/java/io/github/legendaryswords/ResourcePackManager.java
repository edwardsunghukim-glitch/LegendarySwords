package io.github.legendaryswords;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class ResourcePackManager {
    public static final UUID PACK_ID = UUID.nameUUIDFromBytes("LegendarySwords:resource-pack".getBytes(StandardCharsets.UTF_8));
    private final LegendarySwordsPlugin plugin;
    private Path exported;
    private String bundledHash = "";
    private String url = "";
    private byte[] hash;
    public ResourcePackManager(LegendarySwordsPlugin plugin) { this.plugin = plugin; }
    public void initialize() {
        try (var input = plugin.getResource("resource-pack.zip")) {
            if (input == null) throw new IOException("Bundled resource pack is missing; build with mvn package");
            byte[] content = input.readAllBytes();
            bundledHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(content));
            Path defaultFile = plugin.getDataFolder().toPath().resolve("resource-pack.zip");
            exported = defaultFile;
            if (Files.exists(defaultFile) && !MessageDigest.isEqual(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(defaultFile)),
                    MessageDigest.getInstance("SHA-1").digest(content))) {
                exported = plugin.getDataFolder().toPath().resolve("resource-pack-" + bundledHash.substring(0, 12) + ".zip");
                plugin.getLogger().info("Preserved existing resource-pack.zip; bundled pack exported as " + exported.getFileName());
            }
            if (!Files.exists(exported)) Files.write(exported, content);
            Path digestFile = exported.resolveSibling(exported.getFileName() + ".sha1");
            if (!Files.exists(digestFile)) Files.writeString(digestFile, bundledHash + System.lineSeparator());
        } catch (IOException | NoSuchAlgorithmException error) {
            plugin.getLogger().severe("Resource pack export failed: " + error.getMessage());
        }
        reload();
    }
    public void reload() {
        url = plugin.getConfig().getString("resource-pack.url", "").trim();
        hash = null;
        if (url.isBlank()) return;
        try {
            URI parsed = URI.create(url);
            if (!("https".equalsIgnoreCase(parsed.getScheme()) || "http".equalsIgnoreCase(parsed.getScheme()))
                    || parsed.getHost() == null || parsed.getUserInfo() != null || parsed.getFragment() != null) throw new IllegalArgumentException("Use a direct HTTP(S) ZIP URL");
            String configured = plugin.getConfig().getString("resource-pack.sha1", "").trim();
            String digest = configured.isBlank() ? bundledHash : configured;
            if (!digest.matches("[a-fA-F0-9]{40}")) throw new IllegalArgumentException("SHA-1 must contain 40 hexadecimal characters");
            hash = HexFormat.of().parseHex(digest);
        } catch (IllegalArgumentException error) {
            plugin.getLogger().warning("Resource pack delivery disabled: " + error.getMessage());
            url = "";
        }
    }
    public boolean configured() { return !url.isBlank() && hash != null; }
    public String sha1() { return bundledHash; }
    public String exportedPath() { return exported == null ? "not exported" : exported.toString(); }
    public void send(Player player) {
        if (!configured()) {
            player.sendMessage(Component.text("Pack URL is not configured. Install the supplied pack locally, or ask an admin to host it.", NamedTextColor.YELLOW));
            return;
        }
        // Add a pack instead of clearing BetterModel's existing pack stack.
        player.addResourcePack(PACK_ID, url, hash, "LegendarySwords models and textures", plugin.getConfig().getBoolean("resource-pack.required", false));
    }
}
