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
    public int cinematicTicks() { return Math.clamp(config().getInt("cinematics.windup-ticks", 24), 12, 80); }
    public int ultimateWindupTicks(SwordType sword) { return ultimateWindupTicks(config(), sword.id()); }
    public static int ultimateWindupTicks(FileConfiguration config, String swordId) {
        double speed = config.getDouble("swords." + swordId + ".ultimate-speed",
                config.getDouble("ultimates.speed", 1.0));
        if (!Double.isFinite(speed) || speed <= 0) speed = 1.0;
        int base = Math.clamp(config.getInt("cinematics.windup-ticks", 24), 12, 80);
        return Math.clamp((int) Math.round(base / Math.clamp(speed, 0.25, 4.0)), 4, 160);
    }
    public double cameraRadius() { return number("cinematics.camera-radius", 4, 2, 8); }
    public SwordStats stats(SwordType sword) {
        String path = "swords." + sword.id() + ".";
        SwordStats defaults = switch (sword) {
            case MASTER_SWORD -> new SwordStats(9, 1.6, 9, 7, 9, 75);
            case FLAMEBLADE -> new SwordStats(10, 1.3, 8, 9, 6, 90);
            case FIERCE_DEITY_SWORD -> new SwordStats(12, 1.15, 10, 8, 10, 90);
            case FROSTBLADE -> new SwordStats(8, 1.5, 8, 8, 5, 80);
            case THUNDERBLADE -> new SwordStats(8, 1.8, 8, 9, 8, 85);
            case ROYAL_CLAYMORE -> new SwordStats(12, 0.95, 12, 10, 14, 95);
            case EIGHTFOLD_BLADE -> new SwordStats(7, 2.3, 7, 6, 6, 70);
            case BIGGORON_SWORD -> new SwordStats(15, 0.7, 14, 12, 16, 100);
        };
        return new SwordStats(number(path + "attack-damage", defaults.attackDamage(), 1, 100),
                number(path + "attack-speed", defaults.attackSpeed(), 0.1, 4),
                number(path + "ability-damage", defaults.abilityDamage(), 0, 100)
                        * number("damage.ability-multiplier", 1.5, 0, 10),
                number(path + "ability-cooldown-seconds", defaults.abilityCooldown(), 0.25, 600),
                number(path + "ultimate-damage", defaults.ultimateDamage(), 0, 100)
                        * number("damage.ultimate-multiplier", 2.0, 0, 10),
                number(path + "ultimate-cooldown-seconds", defaults.ultimateCooldown(), 5, 3600));
    }
    private double number(String path, double fallback, double minimum, double maximum) {
        double value = config().getDouble(path, fallback);
        return Double.isFinite(value) ? Math.clamp(value, minimum, maximum) : fallback;
    }
}
