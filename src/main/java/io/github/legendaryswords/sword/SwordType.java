package io.github.legendaryswords.sword;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;

public enum SwordType {
    MASTER_SWORD("master_sword", "Master Sword", "Skyward Slash", "Sacred Storm", Material.DIAMOND_SWORD, NamedTextColor.AQUA, Color.fromRGB(90, 220, 255)),
    FLAMEBLADE("flameblade", "Flameblade", "Flame Arc", "Inferno Awakening", Material.NETHERITE_SWORD, NamedTextColor.GOLD, Color.fromRGB(255, 120, 30)),
    FIERCE_DEITY_SWORD("fierce_deity_sword", "Fierce Deity Sword", "Crescent Moon", "Lunar Judgment", Material.NETHERITE_SWORD, NamedTextColor.LIGHT_PURPLE, Color.fromRGB(160, 120, 255));

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
    public static Optional<SwordType> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return Arrays.stream(values()).filter(sword -> sword.id.equals(normalized)).findFirst();
    }
}
