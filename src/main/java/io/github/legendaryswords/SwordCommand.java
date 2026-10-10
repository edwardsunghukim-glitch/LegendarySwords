package io.github.legendaryswords;

import io.github.legendaryswords.sword.SwordType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SwordCommand implements CommandExecutor, TabCompleter {
    private final LegendarySwordsPlugin plugin;
    public SwordCommand(LegendarySwordsPlugin plugin) { this.plugin = plugin; }
    private void say(CommandSender sender, String text) { sender.sendMessage(Component.text(text, NamedTextColor.AQUA)); }
    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("legendaryswords.admin")) return true;
        sender.sendMessage(Component.text("You need legendaryswords.admin for that command.", NamedTextColor.RED)); return false;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            say(sender, "/ls list | /ls cinematics | /ls pack | /ls status");
            if (sender.hasPermission("legendaryswords.admin")) say(sender, "/ls give [player] <sword|all> | /ls charge [player] <0-100> | /ls reload");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                for (SwordType sword : SwordType.values()) sender.sendMessage(Component.text(sword.id() + ": " + sword.ability() + " / " + sword.ultimate(), sword.textColor()));
            }
            case "give" -> {
                if (!admin(sender)) return true;
                if (args.length != 2 && args.length != 3) { say(sender, "Usage: /ls give [player] <sword|all>"); return true; }
                Player target = args.length == 2 ? (sender instanceof Player player ? player : null) : Bukkit.getPlayerExact(args[1]);
                if (target == null) { say(sender, "Choose an online player: /ls give <player> <sword|all>"); return true; }
                String requested = args[args.length - 1];
                List<SwordType> choices = requested.equalsIgnoreCase("all") ? List.of(SwordType.values()) : SwordType.parse(requested).map(List::of).orElse(List.of());
                if (choices.isEmpty()) { say(sender, "Unknown sword. Use /ls list."); return true; }
                for (SwordType sword : choices) {
                    var leftovers = target.getInventory().addItem(plugin.swords().create(sword));
                    leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                }
                say(sender, "Gave " + choices.size() + " sword(s) to " + target.getName() + ".");
                if (!target.equals(sender)) say(target, "You received a legendary sword. Right click for its ability; sneak + right click for its ultimate.");
            }
            case "charge" -> {
                if (!admin(sender)) return true;
                if (args.length != 2 && args.length != 3) { say(sender, "Usage: /ls charge [player] <0-100>"); return true; }
                Player target = args.length == 2 ? (sender instanceof Player player ? player : null) : Bukkit.getPlayerExact(args[1]);
                if (target == null) { say(sender, "Choose an online player."); return true; }
                try {
                    int value = Integer.parseInt(args[args.length - 1]);
                    if (value < 0 || value > 100) throw new NumberFormatException();
                    plugin.states().charge(target.getUniqueId()).set(value);
                    say(sender, target.getName() + " now has " + value + "/100 ultimate charge.");
                } catch (NumberFormatException error) { say(sender, "Charge must be a whole number from 0 to 100."); }
            }
            case "cinematics" -> {
                if (sender instanceof Player player) {
                    if (plugin.cinematics().active(player)) plugin.cinematics().cancel(player);
                    say(sender, "Ultimate cameras " + (plugin.states().toggleCinematics(player.getUniqueId()) ? "enabled" : "disabled") + " for you.");
                } else say(sender, "Players can toggle their own cameras with /ls cinematics.");
            }
            case "pack" -> {
                if (sender instanceof Player player) plugin.packs().send(player);
                if (sender.hasPermission("legendaryswords.admin")) {
                    say(sender, "Exported pack: " + plugin.packs().exportedPath());
                    say(sender, "Bundled SHA-1: " + plugin.packs().sha1());
                    say(sender, "BetterModel animations also need its generated pack; see docs/BETTERMODEL.md.");
                }
            }
            case "status" -> {
                say(sender, "LegendarySwords " + plugin.getPluginMeta().getVersion() + " | Paper 1.21.11 / Java 21");
                say(sender, plugin.animations().status());
                say(sender, "Automatic sword pack delivery: " + (plugin.packs().configured() ? "configured" : "off (install locally or set a URL)"));
                say(sender, "PvP: " + plugin.settings().pvpEnabled() + "; cameras: " + plugin.settings().cinematicsEnabled());
            }
            case "reload" -> {
                if (!admin(sender)) return true;
                plugin.reloadSettings();
                say(sender, "Configuration reloaded. Give fresh swords to apply changed base item damage/speed.");
            }
            default -> say(sender, "Unknown command. Use /ls help.");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            candidates.addAll(List.of("help", "list", "cinematics", "pack", "status"));
            if (sender.hasPermission("legendaryswords.admin")) candidates.addAll(List.of("give", "charge", "reload"));
        } else if (sender.hasPermission("legendaryswords.admin")) {
            if (args[0].equalsIgnoreCase("give") && (args.length == 2 || args.length == 3)) {
                candidates.add("all"); Arrays.stream(SwordType.values()).forEach(sword -> candidates.add(sword.id()));
                if (args.length == 2) Bukkit.getOnlinePlayers().forEach(player -> candidates.add(player.getName()));
            } else if (args[0].equalsIgnoreCase("charge") && (args.length == 2 || args.length == 3)) {
                candidates.addAll(List.of("0", "50", "100"));
                if (args.length == 2) Bukkit.getOnlinePlayers().forEach(player -> candidates.add(player.getName()));
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
