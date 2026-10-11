package io.github.backroomshorror;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.bukkit.util.Vector;
import org.bukkit.command.CommandSender;
import java.util.*;
class BackroomsTest {
 @Test void roomDoorsConnectEveryRoomIncludingNegativeCoordinates(){for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){assertFalse(Rooms.wall(x*8,z*8+3));assertFalse(Rooms.wall(x*8+3,z*8));assertTrue(Rooms.wall(x*8,z*8));assertFalse(Rooms.wall(x*8+4,z*8+4));}}
 @Test void canReachExitFromEntryWithoutCrossingWalls(){Set<String> seen=new HashSet<>();ArrayDeque<int[]> queue=new ArrayDeque<>();queue.add(new int[]{4,4});boolean reached=false;while(!queue.isEmpty()){int[] at=queue.removeFirst();String key=at[0]+":"+at[1];if(!seen.add(key))continue;if(Rooms.exit(at[0],at[1])){reached=true;break;}for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=at[0]+d[0],z=at[1]+d[1];if(x>=0&&x<=60&&z>=0&&z<=60&&!Rooms.wall(x,z))queue.add(new int[]{x,z});}}assertTrue(reached);}
 @Test void watcherUsesViewConeWithoutMutatingDirection(){Vector direction=new Vector(0,0,1);assertTrue(NpcRules.watching(direction,new Vector(0,0,10)));assertFalse(NpcRules.watching(direction,new Vector(0,0,-10)));assertFalse(NpcRules.watching(direction,new Vector(10,0,0)));assertEquals(new Vector(0,0,1),direction);}
 @Test void adminSummonAutocompleteFiltersNamesAndPermissions(){var plugin=mock(BackroomsHorror.class,CALLS_REAL_METHODS);CommandSender admin=mock(CommandSender.class);when(admin.hasPermission("backrooms.admin")).thenReturn(true);assertEquals(List.of("stalker","smiler","hound","watcher","bacteria"),plugin.onTabComplete(admin,null,"br",new String[]{"summon",""}));assertEquals(List.of("smiler"),plugin.onTabComplete(admin,null,"br",new String[]{"summon","sm"}));assertTrue(plugin.onTabComplete(mock(CommandSender.class),null,"br",new String[]{"summon",""}).isEmpty());}
}
