package io.github.legendaryswords;

import io.github.legendaryswords.sword.SwordType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PluginSettingsTest {
    @Test
    void oldConfigurationsGetDistinctDefaultsForTheFiveNewSwords() {
        var config = new YamlConfiguration();
        config.set("swords.master_sword.ability-damage", 17.0);
        var plugin = mock(LegendarySwordsPlugin.class);
        when(plugin.getConfig()).thenReturn(config);
        var settings = new PluginSettings(plugin);

        assertEquals(17.0, settings.stats(SwordType.MASTER_SWORD).abilityDamage());
        var heavy = settings.stats(SwordType.BIGGORON_SWORD);
        var quick = settings.stats(SwordType.EIGHTFOLD_BLADE);
        assertTrue(heavy.attackDamage() > quick.attackDamage());
        assertTrue(heavy.attackSpeed() < quick.attackSpeed());
        assertTrue(heavy.abilityCooldown() > quick.abilityCooldown());
        assertNotEquals(settings.stats(SwordType.FROSTBLADE), settings.stats(SwordType.THUNDERBLADE));
    }
}
