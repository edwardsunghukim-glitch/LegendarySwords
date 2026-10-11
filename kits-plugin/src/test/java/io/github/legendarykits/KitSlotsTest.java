package io.github.legendarykits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KitSlotsTest {
    @Test void migrationRespectsExplicitSlotsAndFillsRemainingGaps() {
        Map<String,Integer> requested=new LinkedHashMap<>();requested.put("old",-1);requested.put("fixed",13);requested.put("other",-1);
        var result=KitSlots.normalize(requested,27);
        assertEquals(13,result.get("fixed"));assertEquals(0,result.get("old"));assertEquals(1,result.get("other"));
    }
    @Test void occupiedSlotSwapsWithoutChangingOtherKitsOrSourceMap() {
        var original=Map.of("a",0,"b",13,"c",26);var moved=KitSlots.move(original,"a",13,27);
        assertEquals(13,moved.get("a"));assertEquals(0,moved.get("b"));assertEquals(26,moved.get("c"));assertEquals(0,original.get("a"));
    }
    @Test void emptySlotMoveLeavesGapAndInvalidSlotsAreRejected() {
        var original=Map.of("a",0);assertEquals(26,KitSlots.move(original,"a",26,27).get("a"));
        assertThrows(IllegalArgumentException.class,()->KitSlots.move(original,"a",27,27));
        assertThrows(IllegalArgumentException.class,()->KitSlots.move(original,"a",-1,27));
    }
    @Test void duplicatesAndOverflowNeverOverwriteAnotherIcon() {
        Map<String,Integer> requested=new LinkedHashMap<>();requested.put("a",0);requested.put("b",0);requested.put("c",99);
        var result=KitSlots.normalize(requested,2);assertEquals(0,result.get("a"));assertEquals(1,result.get("b"));assertEquals(-1,result.get("c"));
    }
}
