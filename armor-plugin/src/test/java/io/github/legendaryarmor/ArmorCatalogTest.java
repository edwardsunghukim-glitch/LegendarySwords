package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ArmorCatalogTest {
    private ArmorCatalog catalog(){JavaPlugin plugin=mock(JavaPlugin.class);when(plugin.namespace()).thenReturn("legendaryarmor");return spy(new ArmorCatalog(plugin));}
    private ItemStack piece(Material type){ItemStack item=mock(ItemStack.class);when(item.getType()).thenReturn(type);return item;}
    @Test void allFourMatchingPiecesUnlockSet() {
        ArmorCatalog catalog=catalog();ItemStack[] items=new ItemStack[4];
        for(int i=0;i<4;i++){items[i]=piece(ArmorSet.PIECES[i]);doReturn(ArmorSet.ANGEL).when(catalog).identify(items[i]);}
        assertEquals(ArmorSet.ANGEL,catalog.fullSet(items));
    }
    @Test void mixedMissingOrWrongSlotPiecesNeverUnlockPassive() {
        ArmorCatalog catalog=catalog();ItemStack[] items=new ItemStack[4];
        for(int i=0;i<4;i++){items[i]=piece(ArmorSet.PIECES[i]);doReturn(ArmorSet.DRAGON).when(catalog).identify(items[i]);}
        doReturn(ArmorSet.FROST).when(catalog).identify(items[2]);assertNull(catalog.fullSet(items));
        doReturn(ArmorSet.DRAGON).when(catalog).identify(items[2]);when(items[1].getType()).thenReturn(Material.NETHERITE_HELMET);assertNull(catalog.fullSet(items));
        items[0]=null;assertNull(catalog.fullSet(items));assertNull(catalog.fullSet(new ItemStack[3]));
    }
    @Test void plainAndRenamedItemsAreNotCustomArmor(){ArmorCatalog catalog=catalog();assertNull(catalog.identify(null));assertNull(catalog.identify(mock(ItemStack.class)));}
}
