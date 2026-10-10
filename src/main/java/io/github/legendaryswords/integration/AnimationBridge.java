package io.github.legendaryswords.integration;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

/** Optional third-person animation support. This public type has no BetterModel linkage. */
public interface AnimationBridge extends AutoCloseable {
    static AnimationBridge create(JavaPlugin plugin) {
        if (!plugin.getConfig().getBoolean("bettermodel.enabled", true)) {
            return new UnavailableAnimationBridge("BetterModel animations disabled in config.yml");
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("BetterModel")) {
            return new UnavailableAnimationBridge("BetterModel is not installed or enabled; abilities still work");
        }
        try {
            // Keep all engine types in a separate class, loaded only after checking the optional plugin.
            return new BetterModelAnimationBridge(plugin);
        } catch (LinkageError | RuntimeException error) {
            var engine = plugin.getServer().getPluginManager().getPlugin("BetterModel");
            String version = engine == null ? "unknown" : engine.getPluginMeta().getVersion();
            String reason = "BetterModel " + version + " API unavailable: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage() + "; integration is built for BetterModel 3.1";
            plugin.getLogger().log(java.util.logging.Level.WARNING, reason, error);
            return new UnavailableAnimationBridge(reason);
        }
    }

    void playSwing(Player player, String swordId);

    void playAbility(Player player, String swordId);

    void playUltimate(Player player, String swordId);

    /** A held-sword body tracker is retained across movement and actions, then restored on release. */
    default void tickMovement(Player player, String swordId, Locomotion movement) { }

    default void stopMovement(Player player) { }

    default void playPose(Player player, String swordId) { }

    /** Ends only the caster's temporary body animation, keeping attack effects alive. */
    default void stopCastAnimation(Player player) { }

    /** Holds the ultimate's final pose until its scene explicitly releases it. */
    default void holdCastAnimation(Player player) { }

    /** Minimum wind-up duration so the ultimate animation can finish before impact. */
    default int ultimateSceneTicks(String swordId) { return 64; }

    enum Locomotion { IDLE, WALK, RUN }


    /** Renders an animated hero at the cast origin; false lets the caller use its fallback scene. */
    default boolean playCinematic(Player player, String swordId, Location origin) {
        return false;
    }

    /** Removes any detached cinematic models when its camera finishes or is cancelled. */
    default void cancelCinematic(Player player) { }

    String status();

    @Override
    void close();
}

final class UnavailableAnimationBridge implements AnimationBridge {
    private final String reason;

    UnavailableAnimationBridge(String reason) {
        this.reason = reason;
    }

    @Override public void playSwing(Player player, String swordId) { }
    @Override public void playAbility(Player player, String swordId) { }
    @Override public void playUltimate(Player player, String swordId) { }
    @Override public String status() { return reason; }
    @Override public void close() { }
}
