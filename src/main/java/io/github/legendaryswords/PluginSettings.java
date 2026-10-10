package io.github.legendaryswords;

import io.github.legendaryswords.sword.SwordType;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {
    public record SwordStats(double attackDamage, double attackSpeed, double abilityDamage,
                             double abilityCooldown, double ultimateDamage, double ultimateCooldown) {}
    private final LegendarySwordsPlugin plugin;
    public PluginSettings(LegendarySwordsPlugin plugin) { this.plugin = plugin; }
    public FileConfiguration config() { return plugin.getConfig(); }
    public boolean pvpEnabled() { return config().getBoolean("pvp-enabled", false); }
    public boolean glowParticles() { return config().getBoolean("glow-particles", true); }
    public int chargePerHit() { return Math.clamp(config().getInt("charge.per-melee-hit", 10), 0, 100); }
    public int killBonus() { return Math.clamp(config().getInt("charge.melee-kill-bonus", 20), 0, 100); }
    public boolean cinematicsEnabled() { return config().getBoolean("cinematics.enabled", true); }
    public int cinematicTicks() { return Math.clamp(config().getInt("cinematics.duration-ticks", 36), 12, 80); }
    public double cameraRadius() { return number("cinematics.camera-radius", 4, 2, 8); }
    public SwordStats stats(SwordType sword) {
        String path = "swords." + sword.id() + ".";
        return new SwordStats(number(path + "attack-damage", 9, 1, 100),
                number(path + "attack-speed", 1.6, 0.1, 4),
                number(path + "ability-damage", 8, 0, 100),
                number(path + "ability-cooldown-seconds", 8, 0.25, 600),
                number(path + "ultimate-damage", 6, 0, 100),
                number(path + "ultimate-cooldown-seconds", 90, 5, 3600));
    }
    private double number(String path, double fallback, double minimum, double maximum) {
        double value = config().getDouble(path, fallback);
        return Double.isFinite(value) ? Math.clamp(value, minimum, maximum) : fallback;
    }
}
