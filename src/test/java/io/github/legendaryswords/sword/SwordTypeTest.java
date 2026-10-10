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
        assertEquals(Set.of("master_sword", "flameblade", "fierce_deity_sword", "frostblade", "thunderblade", "royal_claymore", "eightfold_blade", "biggoron_sword", "white_sword", "goddess_sword", "goddess_longsword", "goddess_white_sword", "true_master_sword", "phantom_sword", "lokomo_sword", "razor_sword", "gilded_sword", "great_fairy_sword", "sword_of_six_sages", "guardian_sword", "ancient_short_sword", "scimitar_of_the_seven", "forest_dweller_sword", "dusk_claymore", "ember_halo", "void_reaver", "astral_edge", "tidebreaker", "storm_cage", "chrono_saber", "eclipse_fang", "crystal_oath", "bloodthorn", "sunspire", "moonveil", "runic_judgment", "nebula_blade", "obsidian_maw", "seraph_wing", "abyssal_trident", "mirage_saber", "meteor_brand", "glacial_crown", "tempest_rapier", "gravity_cleaver", "prism_sword", "thunder_drum", "wraith_scythe", "verdant_oath", "dragonheart"), ids);
        assertEquals(50, SwordType.values().length);
        for (String id : ids) assertEquals(id, SwordType.parse(id).orElseThrow().id());
        assertTrue(SwordType.parse("all").isEmpty(), "'all' is a command selector, not a sword");
        assertTrue(SwordType.parse("not_a_sword").isEmpty());
    }
}
