package io.github.legendaryswords.combat;

import io.github.legendaryswords.LegendarySwordsPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Actual spell geometry. Displays are visual only: they never place or break blocks. */
final class EffectDisplay implements AutoCloseable {
    private static final int PER_PLAYER_LIMIT = 120;
    private static final int GLOBAL_LIMIT = 640;
    private record Entry(Display entity, Player owner, int expires) { }
    private final LegendarySwordsPlugin plugin;
    private final Map<UUID, List<Entry>> entries = new HashMap<>();
    private final BukkitTask cleanup;
    private int tick, count;
    private long nextWarning;

    EffectDisplay(LegendarySwordsPlugin plugin) {
        this.plugin = plugin;
        cleanup = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            tick += 2;
            entries.values().forEach(list -> list.removeIf(entry -> {
                if (tick < entry.expires && entry.owner.isOnline() && !entry.owner.isDead()
                        && entry.owner.getWorld().equals(entry.entity.getWorld()) && entry.entity.isValid()) return false;
                remove(entry.entity); count--; return true;
            }));
            entries.values().removeIf(List::isEmpty);
        }, 2, 2);
    }

    private boolean space(Player owner) {
        return count < GLOBAL_LIMIT && entries.getOrDefault(owner.getUniqueId(), List.of()).size() < PER_PLAYER_LIMIT;
    }

    private void track(Player owner, Display entity, int lifetime) {
        entries.computeIfAbsent(owner.getUniqueId(), ignored -> new ArrayList<>())
                .add(new Entry(entity, owner, tick + Math.clamp(lifetime, 2, 140)));
        count++;
    }

    private void initialize(Display display, Color color) {
        display.setBrightness(new Display.Brightness(15, 15));
        display.setViewRange(1.5f);
        display.setShadowRadius(0);
        display.setInterpolationDuration(2);
        display.setTeleportDuration(2);
        display.setInvulnerable(true);
        display.setPersistent(false);
        display.setSilent(true);
        display.setGlowing(true);
        display.setGlowColorOverride(color);
    }

    private <T extends Display> T spawn(Location location, Class<T> type, Consumer<T> initialize) {
        Display[] partial = new Display[1];
        try {
            return location.getWorld().spawn(location, type, entity -> {
                partial[0] = entity;
                initialize.accept(entity);
            });
        } catch (RuntimeException error) {
            if (partial[0] != null) remove(partial[0]);
            long now = System.nanoTime();
            if (now >= nextWarning) {
                plugin.getLogger().warning("Spell display unavailable; particles and combat still run: " + error.getMessage());
                nextWarning = now + 10_000_000_000L;
            }
            return null;
        }
    }

    BlockDisplay box(Player owner, Location center, Material material, double x, double y, double z, Color color, int lifetime) {
        if (!space(owner)) return null;
        BlockDisplay display = spawn(center, BlockDisplay.class, entity -> {
            initialize(entity, color);
            entity.setBlock(material.createBlockData());
            entity.setTransformation(new Transformation(new Vector3f((float) -x / 2, (float) -y / 2, (float) -z / 2),
                    new Quaternionf(), new Vector3f((float) x, (float) y, (float) z), new Quaternionf()));
        });
        if (display != null) track(owner, display, lifetime);
        return display;
    }

    BlockDisplay line(Player owner, Location from, Location to, Material material, double width, Color color, int lifetime) {
        if (!space(owner)) return null;
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < 0.02 || !from.getWorld().equals(to.getWorld())) return null;
        Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 0, 1),
                new Vector3f((float) delta.getX(), (float) delta.getY(), (float) delta.getZ()).normalize());
        Vector3f translation = rotation.transform(new Vector3f((float) -width / 2, (float) -width / 2, 0));
        BlockDisplay display = spawn(from, BlockDisplay.class, entity -> {
            initialize(entity, color);
            entity.setBlock(material.createBlockData());
            entity.setTransformation(new Transformation(translation, rotation,
                    new Vector3f((float) width, (float) width, (float) length), new Quaternionf()));
        });
        if (display != null) track(owner, display, lifetime);
        return display;
    }

    void ring(Player owner, Location center, double radius, Material material, double width, Color color, int lifetime) {
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24, b = Math.PI * 2 * (i + 1) / 24;
            line(owner, center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                    center.clone().add(Math.cos(b) * radius, 0, Math.sin(b) * radius), material, width, color, lifetime);
        }
    }

    ItemDisplay blade(Player owner, Location location, ItemStack item, double scale, Color color, int lifetime) {
        if (!space(owner)) return null;
        ItemDisplay display = spawn(location, ItemDisplay.class, entity -> {
            initialize(entity, color);
            var visualSword = item.clone();
            if (plugin.animatedSwordItems() != null) plugin.animatedSwordItems().restoreItem(visualSword);
            entity.setItemStack(visualSword);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f((float) scale), new Quaternionf()));
        });
        if (display != null) track(owner, display, lifetime);
        return display;
    }

    void cancel(Player owner) {
        List<Entry> removed = entries.remove(owner.getUniqueId());
        if (removed == null) return;
        removed.forEach(entry -> remove(entry.entity));
        count -= removed.size();
    }

    private void remove(Display display) {
        try { display.remove(); }
        catch (RuntimeException error) { plugin.getLogger().warning("Removing a spell display: " + error.getMessage()); }
    }

    @Override public void close() {
        cleanup.cancel();
        entries.values().forEach(list -> list.forEach(entry -> remove(entry.entity)));
        entries.clear(); count = 0;
    }
}
