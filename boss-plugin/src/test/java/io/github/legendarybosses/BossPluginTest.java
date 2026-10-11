package io.github.legendarybosses;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
class BossPluginTest {
 static org.mockito.MockedStatic<io.papermc.paper.registry.RegistryAccess> registryApi;
 @org.junit.jupiter.api.BeforeAll static void registry(){
  registryApi=mockStatic(io.papermc.paper.registry.RegistryAccess.class);
  var access=mock(io.papermc.paper.registry.RegistryAccess.class);
  when(access.getRegistry(any(Class.class))).thenAnswer(call->mock(org.bukkit.Registry.class));
  when(access.getRegistry(any(io.papermc.paper.registry.RegistryKey.class))).thenAnswer(call->mock(org.bukkit.Registry.class));
  registryApi.when(io.papermc.paper.registry.RegistryAccess::registryAccess).thenReturn(access);
 }
 @org.junit.jupiter.api.AfterAll static void closeRegistry(){registryApi.close();}

 @Test void hundredHeartsEqualsTwoHundredHealth(){assertEquals(200,BossRules.health(100));assertEquals(200,BossRules.health(Double.NaN));assertEquals(2,BossRules.health(-1));}
 @Test void exactlyFiveMatchingArmorSetsWithFourUniqueAttacksEach(){assertEquals(5,BossType.values().length);for(BossType t:BossType.values()){assertTrue(Set.of("angel","dragon","leviathan","stone","frost").contains(t.id()));assertEquals(4,new HashSet<>(t.attacks).size());}assertEquals(20,Arrays.stream(BossType.values()).flatMap(t->t.attacks.stream()).distinct().count());}
 @Test void playerKillGateProtectsReward(){assertTrue(BossRules.reward(true,true));assertFalse(BossRules.reward(true,false));assertTrue(BossRules.reward(false,false));}
 @Test void configHasThreeFlyingBossesAndHugeModelMultipliers(){var c=YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));assertEquals(100,c.getInt("health-hearts"));for(BossType t:BossType.values()){assertEquals(Set.of("angel","dragon","leviathan").contains(t.id()),c.getBoolean("bosses."+t.id()+".flying"));assertTrue(c.getDouble("bosses."+t.id()+".scale")>1);assertTrue(c.getString("bosses."+t.id()+".model").endsWith("_boss"));}}
 @Test void autocompleteListsBossesFiltersPrefixAndChecksPermission(){LegendaryBosses plugin=mock(LegendaryBosses.class,CALLS_REAL_METHODS);CommandSender admin=mock(CommandSender.class);when(admin.hasPermission("legendarybosses.admin")).thenReturn(true);assertEquals(List.of("angel","dragon","leviathan","stone","frost"),plugin.onTabComplete(admin,null,"boss",new String[]{"summon",""}));assertEquals(List.of("dragon"),plugin.onTabComplete(admin,null,"boss",new String[]{"summon","d"}));assertEquals(List.of("summon"),plugin.onTabComplete(admin,null,"boss",new String[]{"su"}));assertTrue(plugin.onTabComplete(mock(CommandSender.class),null,"boss",new String[]{"summon",""}).isEmpty());}
 @Test void everyLootPieceUsesRealMatchingArmorMetadata(){
  for(BossType type:BossType.values())for(int piece=0;piece<4;piece++){
   var meta=mock(org.bukkit.inventory.meta.ItemMeta.class,RETURNS_DEEP_STUBS);
   try(var construction=mockConstruction(org.bukkit.inventory.ItemStack.class,(item,context)->when(item.getItemMeta()).thenReturn(meta))){
    ArmorLoot.create(type,piece,true);
    verify(meta.getPersistentDataContainer()).set(new org.bukkit.NamespacedKey("legendaryarmor","armor_set"),org.bukkit.persistence.PersistentDataType.STRING,type.id());
    verify(meta).setItemModel(new org.bukkit.NamespacedKey("legendaryarmor",type.id()+"_"+ArmorLoot.PIECES[piece]));
    verify(meta.getEquippable()).setModel(new org.bukkit.NamespacedKey("legendaryarmor",type.id()));
    verify(construction.constructed().getFirst()).setItemMeta(meta);
   }
  }
 }
}
