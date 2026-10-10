package io.github.legendaryswords;

import io.github.legendaryswords.sword.SwordType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PluginSettingsTest {
    @Test void upgradedConfigurationsGetFastWindupsWithoutDeletingTheirConfig() {
        var config = new YamlConfiguration();
        config.set("cinematics.duration-ticks", 64);
        var plugin = mock(LegendarySwordsPlugin.class);
        when(plugin.getConfig()).thenReturn(config);
        var settings = new PluginSettings(plugin);
        assertEquals(24, settings.cinematicTicks());
        config.set("cinematics.windup-ticks", 30);
        assertEquals(30, settings.cinematicTicks());
    }
    @Test void ultimateSpeedSupportsGlobalAndPerSwordOverridesAndDamageBuffsExistingConfigs() {
        var config = new YamlConfiguration();
        var plugin = mock(LegendarySwordsPlugin.class);
        when(plugin.getConfig()).thenReturn(config);
        var settings = new PluginSettings(plugin);
        config.set("swords.master_sword.ultimate-damage", 9.0);
        assertEquals(18.0, settings.stats(SwordType.MASTER_SWORD).ultimateDamage());
        config.set("ultimates.speed", 2.0);
        assertEquals(12, settings.ultimateWindupTicks(SwordType.MASTER_SWORD));
        config.set("swords.master_sword.ultimate-speed", 0.5);
        assertEquals(48, settings.ultimateWindupTicks(SwordType.MASTER_SWORD));
        config.set("swords.master_sword.ultimate-speed", Double.NaN);
        assertEquals(24, settings.ultimateWindupTicks(SwordType.MASTER_SWORD));
        config.set("damage.ultimate-multiplier", 1.0);
        assertEquals(9.0, settings.stats(SwordType.MASTER_SWORD).ultimateDamage());
    }
    @Test
    void oldConfigurationsGetDistinctDefaultsForTheFiveNewSwords() {
        var config = new YamlConfiguration();
        config.set("swords.master_sword.ability-damage", 17.0);
        var plugin = mock(LegendarySwordsPlugin.class);
        when(plugin.getConfig()).thenReturn(config);
        var settings = new PluginSettings(plugin);

        assertEquals(25.5, settings.stats(SwordType.MASTER_SWORD).abilityDamage());
        var heavy = settings.stats(SwordType.BIGGORON_SWORD);
        var quick = settings.stats(SwordType.EIGHTFOLD_BLADE);
        assertTrue(heavy.attackDamage() > quick.attackDamage());
        assertTrue(heavy.attackSpeed() < quick.attackSpeed());
        assertTrue(heavy.abilityCooldown() > quick.abilityCooldown());
        assertNotEquals(settings.stats(SwordType.FROSTBLADE), settings.stats(SwordType.THUNDERBLADE));
    }
}
