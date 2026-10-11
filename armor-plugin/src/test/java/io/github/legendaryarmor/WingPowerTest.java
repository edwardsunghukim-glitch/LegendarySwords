package io.github.legendaryarmor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WingPowerTest {
    @Test void wingsExpireAfterFortyFiveSecondsAndCooldownIsSharedAcrossSets() {
        JavaPlugin plugin=mock(JavaPlugin.class);when(plugin.getConfig()).thenReturn(new YamlConfiguration());AtomicLong clock=new AtomicLong(1000);WingPower wings=new WingPower(plugin,clock::get);UUID player=UUID.randomUUID();
        assertTrue(wings.start(player,ArmorSet.ANGEL));assertEquals(45,wings.remaining(player,ArmorSet.ANGEL));assertFalse(wings.start(player,ArmorSet.DRAGON));
        clock.addAndGet(45000);assertFalse(wings.active(player,ArmorSet.ANGEL));assertFalse(wings.start(player,ArmorSet.ANGEL));clock.addAndGet(90000);assertTrue(wings.start(player,ArmorSet.DRAGON));
    }
    @Test void durationConfigAndTakingOffArmorAreRespected() {
        JavaPlugin plugin=mock(JavaPlugin.class);YamlConfiguration config=new YamlConfiguration();config.set("wings.duration-seconds",60);when(plugin.getConfig()).thenReturn(config);AtomicLong clock=new AtomicLong(1000);WingPower wings=new WingPower(plugin,clock::get);UUID player=UUID.randomUUID();
        assertTrue(wings.start(player,ArmorSet.LEVIATHAN));assertEquals(60,wings.remaining(player,ArmorSet.LEVIATHAN));wings.validateSet(player,null);assertFalse(wings.active(player,ArmorSet.LEVIATHAN));assertFalse(wings.start(player,ArmorSet.LEVIATHAN));
    }
    @Test void stoneFrostAndDisabledFlightCannotStartWings() {
        JavaPlugin plugin=mock(JavaPlugin.class);YamlConfiguration config=new YamlConfiguration();config.set("sets.dragon.flight-enabled",false);when(plugin.getConfig()).thenReturn(config);WingPower wings=new WingPower(plugin);UUID player=UUID.randomUUID();
        assertFalse(wings.start(player,ArmorSet.STONE));assertFalse(wings.start(player,ArmorSet.FROST));assertFalse(wings.start(player,ArmorSet.DRAGON));assertFalse(wings.enabled(null));
    }
}
