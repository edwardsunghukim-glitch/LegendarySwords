package io.github.legendarybosses;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
class SpawnProtectionTest {
 @Test void protectsWorldSpawnInEveryWorldWithoutWorldGuard(){JavaPlugin plugin=mock(JavaPlugin.class,RETURNS_DEEP_STUBS);YamlConfiguration config=new YamlConfiguration();when(plugin.getConfig()).thenReturn(config);var protection=new SpawnProtection(plugin);World world=mock(World.class);when(world.getSpawnLocation()).thenReturn(new Location(world,100,64,200));assertTrue(protection.protectedAt(new Location(world,104,90,204)));assertFalse(protection.protectedAt(new Location(world,140,64,200)));config.set("spawn-protection.enabled",false);assertFalse(protection.protectedAt(new Location(world,100,64,200)));}
}
