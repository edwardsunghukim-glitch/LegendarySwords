package io.github.legendaryarmor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class StableRenderingTest {
    @Test void stableModeRemovesOldAttachmentsAndNeverSpawnsNewOnes() throws Exception {
        JavaPlugin plugin=mock(JavaPlugin.class);YamlConfiguration config=new YamlConfiguration();config.set("accessories-enabled",false);when(plugin.getConfig()).thenReturn(config);
        ArmorCosmetics cosmetics=new ArmorCosmetics(plugin);Player player=mock(Player.class);UUID id=UUID.randomUUID();when(player.getUniqueId()).thenReturn(id);when(player.isOnline()).thenReturn(true);
        ItemDisplay back=mock(ItemDisplay.class),crown=mock(ItemDisplay.class),helmet=mock(ItemDisplay.class);
        Class<?> wornType=Class.forName("io.github.legendaryarmor.ArmorCosmetics$Worn");var constructor=wornType.getDeclaredConstructors()[0];constructor.setAccessible(true);
        var field=ArmorCosmetics.class.getDeclaredField("worn");field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<UUID,Object> worn=(Map<UUID,Object>)field.get(cosmetics);
        worn.put(id,constructor.newInstance(ArmorSet.ANGEL,back,crown,helmet,null));
        cosmetics.update(player,ArmorSet.ANGEL);verify(back).remove();verify(crown).remove();verify(helmet).remove();assertTrue(worn.isEmpty());verify(player,never()).getWorld();
        cosmetics.update(player,ArmorSet.ANGEL);verify(plugin,never()).getServer();
    }
}
