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
    DUSK_CLAYMORE("dusk_claymore", "Dusk Claymore", "Shadow Ridge", "Twilight Rupture", Material.NETHERITE_SWORD, NamedTextColor.DARK_PURPLE, Color.fromRGB(170, 90, 225)),

    EMBER_HALO("ember_halo", "Ember Halo", "Burning Circlet", "Domain: Furnace Crown", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 85, 35)),
    VOID_REAVER("void_reaver", "Void Reaver", "Collapsing Ring", "Domain: Event Horizon", Material.DIAMOND_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(105, 55, 220)),
    ASTRAL_EDGE("astral_edge", "Astral Edge", "Orbiting Stars", "Domain: Astral Observatory", Material.DIAMOND_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(140, 195, 255)),
    TIDEBREAKER("tidebreaker", "Tidebreaker", "Tidal Wall", "Domain: Drowned Court", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(35, 195, 230)),
    STORM_CAGE("storm_cage", "Storm Cage", "Conductive Prison", "Domain: Storm Tribunal", Material.DIAMOND_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(200, 225, 255)),
    CHRONO_SABER("chrono_saber", "Chrono Saber", "Clockwork Seal", "Domain: Broken Hourglass", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(245, 205, 125)),
    ECLIPSE_FANG("eclipse_fang", "Eclipse Fang", "Twin Eclipse", "Domain: Black Sun", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(200, 70, 165)),
    CRYSTAL_OATH("crystal_oath", "Crystal Oath", "Prism Mines", "Domain: Shattered Cathedral", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(110, 255, 220)),
    BLOODTHORN("bloodthorn", "Bloodthorn", "Thorn Procession", "Domain: Crimson Garden", Material.DIAMOND_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(220, 40, 80)),
    SUNSPIRE("sunspire", "Sunspire", "Solar Descent", "Domain: Noonstar Sanctuary", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 205, 65)),
    MOONVEIL("moonveil", "Moonveil", "Returning Crescent", "Domain: Lunar Tides", Material.DIAMOND_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(175, 185, 255)),
    RUNIC_JUDGMENT("runic_judgment", "Runic Judgment", "Cross Seal", "Domain: Rune Labyrinth", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(245, 140, 80)),
    NEBULA_BLADE("nebula_blade", "Nebula Blade", "Star Serpent", "Domain: Nebula Vault", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(160, 105, 255)),
    OBSIDIAN_MAW("obsidian_maw", "Obsidian Maw", "Magma Teeth", "Domain: Volcanic Throne", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(250, 80, 50)),
    SERAPH_WING("seraph_wing", "Seraph Wing", "Wingbreak", "Domain: Seraph Ascension", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 240, 190)),
    ABYSSAL_TRIDENT("abyssal_trident", "Abyssal Trident", "Depth Charge", "Domain: Abyssal Whirlpool", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(40, 165, 180)),
    MIRAGE_SABER("mirage_saber", "Mirage Saber", "Echo Traps", "Domain: Hall of Mirages", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 165, 125)),
    METEOR_BRAND("meteor_brand", "Meteor Brand", "Meteor Mark", "Domain: Falling Firmament", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 125, 65)),
    GLACIAL_CROWN("glacial_crown", "Glacial Crown", "Frost Coronation", "Domain: Winter Palace", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(155, 235, 255)),
    TEMPEST_RAPIER("tempest_rapier", "Tempest Rapier", "Storm Lance", "Domain: Eye of the Tempest", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(120, 255, 210)),
    GRAVITY_CLEAVER("gravity_cleaver", "Gravity Cleaver", "Graviton Well", "Domain: Singularity", Material.DIAMOND_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(165, 75, 245)),
    PRISM_SWORD("prism_sword", "Prism Sword", "Triangular Verdict", "Domain: Crystal Prism", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(245, 115, 240)),
    THUNDER_DRUM("thunder_drum", "Thunder Drum", "Resonance Wave", "Domain: Thunder Chamber", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 225, 60)),
    WRAITH_SCYTHE("wraith_scythe", "Wraith Scythe", "Reaping Arc", "Domain: Soul Harvest", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(130, 245, 185)),
    VERDANT_OATH("verdant_oath", "Verdant Oath", "Seedburst", "Domain: Ancient Grove", Material.NETHERITE_SWORD, NamedTextColor.AQUA, Color.fromRGB(90, 220, 110)),
    DRAGONHEART("dragonheart", "Dragonheart", "Dragon Breath", "Domain: Dragon Furnace", Material.DIAMOND_SWORD, NamedTextColor.GOLD, Color.fromRGB(245, 65, 35)),
    ANGEL_SOVEREIGN("angel_sovereign", "Angel Sovereign", "Heavenly Cleave", "Ascension: Angel Sovereign", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 235, 155)),
    GRANITE_COLOSSUS("granite_colossus", "Granite Colossus Fists", "Boulder Smash", "Awakening: Rock Colossus", Material.NETHERITE_SWORD, NamedTextColor.GRAY, Color.fromRGB(185, 160, 110)),
    SKY_LEVIATHAN("sky_leviathan", "Sky Leviathan Fang", "Leviathan Bite", "Awakening: Flying Leviathan", Material.NETHERITE_SWORD, NamedTextColor.AQUA, Color.fromRGB(70, 215, 245)),
    INFERNAL_DRAGON("infernal_dragon", "Infernal Dragon Claw", "Dragon Breath", "Awakening: Infernal Dragon", Material.NETHERITE_SWORD, NamedTextColor.RED, Color.fromRGB(255, 85, 45)),
    FROST_TITAN("frost_titan", "Frost Titan Hammer", "Glacier Smash", "Awakening: Frost Titan", Material.NETHERITE_SWORD, NamedTextColor.BLUE, Color.fromRGB(170, 225, 255));

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
            case EMBER_HALO -> FLAMEBLADE;
            case VOID_REAVER -> FIERCE_DEITY_SWORD;
            case ASTRAL_EDGE -> MASTER_SWORD;
            case TIDEBREAKER -> FROSTBLADE;
            case STORM_CAGE -> THUNDERBLADE;
            case CHRONO_SABER -> ROYAL_CLAYMORE;
            case ECLIPSE_FANG -> FIERCE_DEITY_SWORD;
            case CRYSTAL_OATH -> FROSTBLADE;
            case BLOODTHORN -> FLAMEBLADE;
            case SUNSPIRE -> MASTER_SWORD;
            case MOONVEIL -> FIERCE_DEITY_SWORD;
            case RUNIC_JUDGMENT -> ROYAL_CLAYMORE;
            case NEBULA_BLADE -> MASTER_SWORD;
            case OBSIDIAN_MAW -> BIGGORON_SWORD;
            case SERAPH_WING -> MASTER_SWORD;
            case ABYSSAL_TRIDENT -> FROSTBLADE;
            case MIRAGE_SABER -> EIGHTFOLD_BLADE;
            case METEOR_BRAND -> BIGGORON_SWORD;
            case GLACIAL_CROWN -> FROSTBLADE;
            case TEMPEST_RAPIER -> EIGHTFOLD_BLADE;
            case GRAVITY_CLEAVER -> BIGGORON_SWORD;
            case PRISM_SWORD -> MASTER_SWORD;
            case THUNDER_DRUM -> THUNDERBLADE;
            case WRAITH_SCYTHE -> FIERCE_DEITY_SWORD;
            case VERDANT_OATH -> FLAMEBLADE;
            case DRAGONHEART -> FLAMEBLADE;
            case ANGEL_SOVEREIGN -> MASTER_SWORD;
            case GRANITE_COLOSSUS -> BIGGORON_SWORD;
            case SKY_LEVIATHAN -> FROSTBLADE;
            case INFERNAL_DRAGON -> FLAMEBLADE;
            case FROST_TITAN -> FROSTBLADE;
            default -> this;
        };
    }
    public boolean boss() { return ordinal() >= ANGEL_SOVEREIGN.ordinal(); }
    public boolean bossFlight() { return this == ANGEL_SOVEREIGN || this == SKY_LEVIATHAN; }
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
