package io.github.legendaryswords.sword;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;

public enum SwordType {
    MASTER_SWORD("master_sword", "Master Sword", "Skyward Laser", "Triforce Judgment", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(90, 220, 255)),
    FLAMEBLADE("flameblade", "Flameblade", "Ring of Fire", "Inferno Vortex", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 120, 30)),
    FIERCE_DEITY_SWORD("fierce_deity_sword", "Fierce Deity Sword", "Lunar Crescent", "Falling Moon", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(160, 120, 255)),
    FROSTBLADE("frostblade", "Frostblade", "Ice Fang", "Frozen Sanctuary", Material.DIAMOND_SWORD, NamedTextColor.BLUE, Color.fromRGB(125, 215, 255)),
    THUNDERBLADE("thunderblade", "Thunderblade", "Chain Lightning", "Thunderstorm", Material.DIAMOND_SWORD, NamedTextColor.YELLOW, Color.fromRGB(255, 235, 70)),
    ROYAL_CLAYMORE("royal_claymore", "Royal Claymore", "Royal Faultline", "King's Verdict", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 205, 95)),
    EIGHTFOLD_BLADE("eightfold_blade", "Eightfold Blade", "Gale Step", "Eightfold Tempest", Material.IRON_SWORD, NamedTextColor.GREEN, Color.fromRGB(115, 255, 185)),
    BIGGORON_SWORD("biggoron_sword", "Biggoron Sword", "Goron Spin", "Mountain Breaker", Material.NETHERITE_SWORD, NamedTextColor.RED, Color.fromRGB(255, 135, 90)),

    WHITE_SWORD("white_sword", "White Sword", "Radiant Fan", "Dawn Barrage", Material.DIAMOND_SWORD, NamedTextColor.WHITE, Color.fromRGB(230, 245, 255)),
    GODDESS_SWORD("goddess_sword", "Goddess Sword", "Spiral Light", "Sacred Helix", Material.DIAMOND_SWORD, NamedTextColor.BLUE, Color.fromRGB(140, 170, 255)),
    GODDESS_LONGSWORD("goddess_longsword", "Goddess Longsword", "Falling Star", "Celestial Rain", Material.NETHERITE_SWORD, NamedTextColor.AQUA, Color.fromRGB(90, 150, 245)),
    GODDESS_WHITE_SWORD("goddess_white_sword", "Goddess White Sword", "Light Bloom", "Heaven's Garden", Material.DIAMOND_SWORD, NamedTextColor.YELLOW, Color.fromRGB(220, 240, 180)),
    TRUE_MASTER_SWORD("true_master_sword", "True Master Sword", "Triad Beam", "Golden Trinity", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(100, 240, 225)),
    PHANTOM_SWORD("phantom_sword", "Phantom Sword", "Spirit Blades", "Phantom Legion", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(185, 120, 250)),
    LOKOMO_SWORD("lokomo_sword", "Lokomo Sword", "Spirit Rail", "Divine Tracks", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(110, 220, 245)),
    RAZOR_SWORD("razor_sword", "Razor Sword", "Razor Wheel", "Steel Cyclone", Material.IRON_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(225, 140, 190)),
    GILDED_SWORD("gilded_sword", "Gilded Sword", "Gilded Petals", "Golden Tempest", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(250, 195, 65)),
    GREAT_FAIRY_SWORD("great_fairy_sword", "Great Fairy Sword", "Blossom Strike", "Fairy's Awakening", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(255, 125, 205)),
    SWORD_OF_SIX_SAGES("sword_of_six_sages", "Sword of the Six Sages", "Sage Pillars", "Sixfold Seal", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(240, 155, 60)),
    GUARDIAN_SWORD("guardian_sword", "Guardian Sword", "Pulse Burst", "Guardian Overdrive", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(90, 200, 255)),
    ANCIENT_SHORT_SWORD("ancient_short_sword", "Ancient Short Sword", "Hex Blades", "Ancient Array", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(90, 245, 235)),
    SCIMITAR_OF_THE_SEVEN("scimitar_of_the_seven", "Scimitar of the Seven", "Sand Spiral", "Desert Sovereign", Material.IRON_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 205, 110)),
    FOREST_DWELLER_SWORD("forest_dweller_sword", "Forest Dweller's Sword", "Vine Snare", "Forest Dominion", Material.NETHERITE_SWORD, NamedTextColor.GREEN, Color.fromRGB(100, 220, 80)),
    DUSK_CLAYMORE("dusk_claymore", "Dusk Claymore", "Shadow Ridge", "Twilight Rupture", Material.NETHERITE_SWORD, NamedTextColor.DARK_PURPLE, Color.fromRGB(170, 90, 225));

    private final String id, title, ability, ultimate;
    private final Material material;
    private final NamedTextColor textColor;
    private final Color particleColor;
    SwordType(String id, String title, String ability, String ultimate, Material material, NamedTextColor textColor, Color particleColor) {
        this.id = id; this.title = title; this.ability = ability; this.ultimate = ultimate;
        this.material = material; this.textColor = textColor; this.particleColor = particleColor;
    }
    public String id() { return id; }
    public String title() { return title; }
    public String ability() { return ability; }
    public String ultimate() { return ultimate; }
    public Material material() { return material; }
    public NamedTextColor textColor() { return textColor; }
    public Color particleColor() { return particleColor; }
    public SwordType archetype() {
        return switch (this) {
            case WHITE_SWORD -> MASTER_SWORD;
            case GODDESS_SWORD -> FROSTBLADE;
            case GODDESS_LONGSWORD -> ROYAL_CLAYMORE;
            case GODDESS_WHITE_SWORD -> MASTER_SWORD;
            case TRUE_MASTER_SWORD -> MASTER_SWORD;
            case PHANTOM_SWORD -> FIERCE_DEITY_SWORD;
            case LOKOMO_SWORD -> THUNDERBLADE;
            case RAZOR_SWORD -> EIGHTFOLD_BLADE;
            case GILDED_SWORD -> ROYAL_CLAYMORE;
            case GREAT_FAIRY_SWORD -> FIERCE_DEITY_SWORD;
            case SWORD_OF_SIX_SAGES -> BIGGORON_SWORD;
            case GUARDIAN_SWORD -> THUNDERBLADE;
            case ANCIENT_SHORT_SWORD -> FROSTBLADE;
            case SCIMITAR_OF_THE_SEVEN -> EIGHTFOLD_BLADE;
            case FOREST_DWELLER_SWORD -> FLAMEBLADE;
            case DUSK_CLAYMORE -> BIGGORON_SWORD;
            default -> this;
        };
    }
    public String identity() {
        return switch (this) {
            case MASTER_SWORD -> "A sacred blade that channels piercing light.";
            case FLAMEBLADE -> "Molten runes kindle a living ring of flame.";
            case FIERCE_DEITY_SWORD -> "A helix of moonlight calls down the heavens.";
            case FROSTBLADE -> "Crystalline fangs awaken the sleeping winter.";
            case THUNDERBLADE -> "A gilded conductor holds a gathering storm.";
            case ROYAL_CLAYMORE -> "A king's heavy blade rends the earth.";
            case EIGHTFOLD_BLADE -> "A swift curved edge moves with the wind.";
            case BIGGORON_SWORD -> "A mountain-forged blade strikes with a giant's force.";
            default -> "A legendary relic channels " + ability.toLowerCase(Locale.ROOT) + ".";
        };
    }
    public static Optional<SwordType> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return Arrays.stream(values()).filter(sword -> sword.id.equals(normalized)).findFirst();
    }
}
