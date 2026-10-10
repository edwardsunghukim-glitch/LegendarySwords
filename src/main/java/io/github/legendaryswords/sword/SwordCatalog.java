package io.github.legendaryswords.sword;

import io.github.legendaryswords.PluginSettings;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class SwordCatalog {
    private final NamespacedKey swordKey;
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    public SwordCatalog(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin; this.settings = settings;
        swordKey = new NamespacedKey(plugin, "sword_id");
    }
    public ItemStack create(SwordType sword) {
        ItemStack item = new ItemStack(sword.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(sword.title(), sword.textColor()).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(sword.identity(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Right click: " + sword.ability(), sword.textColor()).decoration(TextDecoration.ITALIC, false),
                Component.text("Sneak + right click: " + sword.ultimate(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("Land melee hits to charge your ultimate.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("/ls pose: show this blade's stance.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(swordKey, PersistentDataType.STRING, sword.id());
        meta.setItemModel(new NamespacedKey("legendaryswords", sword.id()));
        meta.setEnchantmentGlintOverride(true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        PluginSettings.SwordStats stats = settings.stats(sword);
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(plugin, "blade_damage"),
                stats.attackDamage() - 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(new NamespacedKey(plugin, "blade_speed"),
                stats.attackSpeed() - 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        item.setItemMeta(meta);
        return item;
    }
    public Optional<SwordType> identify(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
        Optional<SwordType> type = SwordType.parse(item.getItemMeta().getPersistentDataContainer().get(swordKey, PersistentDataType.STRING));
        return type.filter(sword -> item.getType() == sword.material());
    }
}
