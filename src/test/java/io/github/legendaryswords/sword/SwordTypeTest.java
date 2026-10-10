package io.github.legendaryswords.sword;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SwordTypeTest {
    @Test
    void allPublishedSwordIdsAreDistinctAndAvailableToCommands() {
        var ids = Arrays.stream(SwordType.values()).map(SwordType::id).collect(Collectors.toSet());
        assertEquals(Set.of("master_sword", "flameblade", "fierce_deity_sword", "frostblade", "thunderblade", "royal_claymore", "eightfold_blade", "biggoron_sword"), ids);
        assertEquals(8, SwordType.values().length);
        for (String id : ids) assertEquals(id, SwordType.parse(id).orElseThrow().id());
        assertTrue(SwordType.parse("all").isEmpty(), "'all' is a command selector, not a sword");
        assertTrue(SwordType.parse("not_a_sword").isEmpty());
    }
}
