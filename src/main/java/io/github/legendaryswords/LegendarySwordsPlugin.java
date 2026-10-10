package io.github.legendaryswords;

import io.github.legendaryswords.combat.AbilityController;
import io.github.legendaryswords.combat.CinematicDirector;
import io.github.legendaryswords.combat.CombatService;
import io.github.legendaryswords.combat.VisualEffects;
import io.github.legendaryswords.integration.AnimationBridge;
import io.github.legendaryswords.integration.MovementAnimationController;
import io.github.legendaryswords.mechanics.CooldownBook;
import io.github.legendaryswords.sword.SwordCatalog;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class LegendarySwordsPlugin extends JavaPlugin {
    private PluginSettings settings;
    private SwordCatalog swords;
    private PlayerStateStore states;
    private CooldownBook cooldowns;
    private VisualEffects effects;
    private CombatService combat;
    private CinematicDirector cinematics;
    private AnimationBridge animations;
    private MovementAnimationController movement;
    private AbilityController abilities;
    private ResourcePackManager packs;

    @Override public void onEnable() {
        saveDefaultConfig();
        settings = new PluginSettings(this);
        swords = new SwordCatalog(this, settings);
        states = new PlayerStateStore(this);
        cooldowns = new CooldownBook();
        effects = new VisualEffects();
        combat = new CombatService(settings, swords, states);
        cinematics = new CinematicDirector(this, settings, states, effects);
        animations = AnimationBridge.create(this);
        movement = new MovementAnimationController(this, animations, swords);
        createAbilityController();
        packs = new ResourcePackManager(this);
        packs.initialize();
        SwordCommand commands = new SwordCommand(this);
        var command = Objects.requireNonNull(getCommand("legendaryswords"), "plugin.yml command is missing");
        command.setExecutor(commands); command.setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(new SwordListener(this), this);
        Bukkit.getOnlinePlayers().forEach(cinematics::recover);
        getServer().getScheduler().runTaskTimer(this, this::updateHeldSwords, 10, 10);
        getServer().getScheduler().runTaskTimer(this, () -> { states.save(); cooldowns.purgeExpired(); }, 600, 600);
        getLogger().info("LegendarySwords enabled for Paper 1.21.11. " + animations.status());
        if (!packs.configured()) getLogger().info("Resource pack exported to " + packs.exportedPath() + "; set resource-pack.url or install it on clients.");
    }
    private void createAbilityController() {
        abilities = new AbilityController(this, settings, states, cooldowns, combat, effects, cinematics, animations);
    }
    public void reloadSettings() {
        cinematics.close(); abilities.close(); movement.close(); animations.close();
        reloadConfig();
        animations = AnimationBridge.create(this);
        movement = new MovementAnimationController(this, animations, swords);
        createAbilityController();
        packs.reload();
    }
    private void updateHeldSwords() {
        for (var player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("legendaryswords.use") || cinematics.active(player)) continue;
            swords.identify(player.getInventory().getItemInMainHand()).ifPresent(sword -> {
                int charge = states.charge(player.getUniqueId()).value();
                long remaining = cooldowns.remainingMillis(player.getUniqueId(), sword.id() + ":ability");
                String skill = remaining == 0 ? sword.ability() : "Ability " + (remaining + 999) / 1000 + "s";
                player.sendActionBar(Component.text(skill + "  |  Ultimate " + charge + "/100", sword.textColor())
                        .append(charge == 100 ? Component.text(" READY", NamedTextColor.GOLD) : Component.empty()));
                if (settings.glowParticles() && !player.isInvisible()) {
                    var point = player.getEyeLocation().subtract(0, 0.55, 0).add(player.getEyeLocation().getDirection().multiply(0.65));
                    Vector right = new Vector(-point.getDirection().getZ(), 0, point.getDirection().getX());
                    if (right.lengthSquared() > 0.001) point.add(right.normalize().multiply(0.25));
                    effects.dust(point, sword, 2, 0.04);
                }
            });
        }
    }
    @Override public void onDisable() {
        if (cinematics != null) cinematics.close();
        if (abilities != null) abilities.close();
        if (movement != null) movement.close();
        if (animations != null) animations.close();
        if (states != null) states.close();
    }
    public PluginSettings settings() { return settings; }
    public SwordCatalog swords() { return swords; }
    public PlayerStateStore states() { return states; }
    public CooldownBook cooldowns() { return cooldowns; }
    public VisualEffects effects() { return effects; }
    public CombatService combat() { return combat; }
    public CinematicDirector cinematics() { return cinematics; }
    public AnimationBridge animations() { return animations; }
    public AbilityController abilities() { return abilities; }
    public ResourcePackManager packs() { return packs; }
}
