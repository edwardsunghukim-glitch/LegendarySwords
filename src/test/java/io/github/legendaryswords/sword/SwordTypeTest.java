package io.github.legendaryswords.sword;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwordTypeTest {
    @Test
    void allPublishedSwordIdsAreDistinctAndAvailableToCommands() {
        var master = SwordType.parse("master_sword").orElseThrow();
        var flame = SwordType.parse("flameblade").orElseThrow();
        var fierce = SwordType.parse("fierce_deity_sword").orElseThrow();
        assertNotEquals(master, flame);
        assertNotEquals(master, fierce);
        assertNotEquals(flame, fierce);
        assertTrue(SwordType.parse("all").isEmpty(), "'all' is a command selector, not a fourth sword");
        assertTrue(SwordType.parse("not_a_sword").isEmpty());
    }
}
