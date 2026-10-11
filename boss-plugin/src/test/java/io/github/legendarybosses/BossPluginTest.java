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
  var enchantments=org.bukkit.Registry.ENCHANTMENT;
  var values=new java.util.HashMap<String,org.bukkit.enchantments.Enchantment>();
  doAnswer(call->values.computeIfAbsent(call.getArgument(0).toString(),key->mock(org.bukkit.enchantments.Enchantment.class))).when(enchantments).getOrThrow(any(net.kyori.adventure.key.Key.class));
  doAnswer(call->values.computeIfAbsent(call.getArgument(0).toString(),key->mock(org.bukkit.enchantments.Enchantment.class))).when(enchantments).getOrThrow(any(org.bukkit.NamespacedKey.class));
 }
 @org.junit.jupiter.api.AfterAll static void closeRegistry(){registryApi.close();}

 @Test void hundredHeartsEqualsTwoHundredHealth(){assertEquals(200,BossRules.health(100));assertEquals(200,BossRules.health(Double.NaN));assertEquals(2,BossRules.health(-1));}
 @Test void exactlyFiveMatchingArmorSetsWithTenUniqueAttacksEach(){assertEquals(5,BossType.values().length);for(BossType t:BossType.values()){assertTrue(Set.of("angel","dragon","leviathan","stone","frost").contains(t.id()));assertEquals(10,new HashSet<>(t.attacks).size());}assertEquals(50,Arrays.stream(BossType.values()).flatMap(t->t.attacks.stream()).distinct().count());}
 @Test void playerKillGateProtectsReward(){assertTrue(BossRules.reward(true,true));assertFalse(BossRules.reward(true,false));assertTrue(BossRules.reward(false,false));}
 @Test void configHasThreeFlyingBossesAndHugeModelMultipliers(){var c=YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));assertEquals(100,c.getInt("health-hearts"));for(BossType t:BossType.values()){assertEquals(Set.of("angel","dragon","leviathan").contains(t.id()),c.getBoolean("bosses."+t.id()+".flying"));assertTrue(c.getDouble("bosses."+t.id()+".scale")>1);assertTrue(c.getString("bosses."+t.id()+".model").endsWith("_boss"));}}
 @Test void autocompleteListsBossesFiltersPrefixAndChecksPermission(){LegendaryBosses plugin=mock(LegendaryBosses.class,CALLS_REAL_METHODS);CommandSender admin=mock(CommandSender.class);when(admin.hasPermission("legendarybosses.admin")).thenReturn(true);assertEquals(List.of("angel","dragon","leviathan","stone","frost"),plugin.onTabComplete(admin,null,"boss",new String[]{"summon",""}));assertEquals(List.of("dragon"),plugin.onTabComplete(admin,null,"boss",new String[]{"summon","d"}));assertEquals(List.of("summon"),plugin.onTabComplete(admin,null,"boss",new String[]{"su"}));assertTrue(plugin.onTabComplete(mock(CommandSender.class),null,"boss",new String[]{"summon",""}).isEmpty());}
 @Test void everyLootPieceUsesRealMatchingArmorMetadata(){
  for(BossType type:BossType.values())for(int piece=0;piece<4;piece++){
   var meta=mock(org.bukkit.inventory.meta.ItemMeta.class,RETURNS_DEEP_STUBS);
   try(var construction=mockConstruction(org.bukkit.inventory.ItemStack.class,(item,context)->when(item.getItemMeta()).thenReturn(meta))){
    ArmorLoot.create(type,piece,true);
    var protection=org.bukkit.Registry.ENCHANTMENT.getOrThrow(org.bukkit.NamespacedKey.minecraft("protection"));
    var falling=org.bukkit.Registry.ENCHANTMENT.getOrThrow(org.bukkit.NamespacedKey.minecraft("feather_falling"));
    verify(meta.getPersistentDataContainer()).set(new org.bukkit.NamespacedKey("legendaryarmor","armor_set"),org.bukkit.persistence.PersistentDataType.STRING,type.id());
    verify(meta).displayName(ArmorLoot.text(ArmorLoot.TITLES[type.ordinal()]+" "+ArmorLoot.PIECES[piece],ArmorLoot.COLORS[type.ordinal()]));
    verify(meta).lore(List.of(ArmorLoot.text("Full set: "+ArmorLoot.PASSIVES[type.ordinal()],0xc5c9d2),ArmorLoot.text("Sneak + F: "+ArmorLoot.ABILITIES[type.ordinal()],ArmorLoot.COLORS[type.ordinal()]),ArmorLoot.text("Wear all four matching pieces.",0x999da5)));
    verify(meta).setItemModel(new org.bukkit.NamespacedKey("legendaryarmor",type.id()+"_"+ArmorLoot.PIECES[piece]));
    verify(meta.getEquippable()).setModel(new org.bukkit.NamespacedKey("legendaryarmor",type.id()));
    verify(meta).addEnchant(protection,4,true);
    if(piece==0)verify(meta).addEnchant(falling,4,true);
    verify(construction.constructed().getFirst()).setItemMeta(meta);
   }
  }
 }
}
