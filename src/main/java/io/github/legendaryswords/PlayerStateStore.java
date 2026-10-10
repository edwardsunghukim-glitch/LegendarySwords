package io.github.legendaryswords;

import io.github.legendaryswords.mechanics.ChargeMeter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerStateStore implements AutoCloseable {
    private static final class State {
        final ChargeMeter charge;
        boolean cinematics;
        State(int charge, boolean cinematics) { this.charge = new ChargeMeter(charge); this.cinematics = cinematics; }
    }
    private final Map<UUID, State> loaded = new HashMap<>();
    private final LegendarySwordsPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    public PlayerStateStore(LegendarySwordsPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "player-data.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }
    private State state(UUID player) {
        return loaded.computeIfAbsent(player, id -> new State(data.getInt(id + ".charge", 0), data.getBoolean(id + ".cinematics", true)));
    }
    public ChargeMeter charge(UUID player) { return state(player).charge; }
    public boolean cinematics(UUID player) { return state(player).cinematics; }
    public boolean toggleCinematics(UUID player) { State state = state(player); return state.cinematics = !state.cinematics; }
    public void unload(UUID player) {
        State state = loaded.remove(player);
        if (state != null) write(player, state);
        saveFile();
    }
    private void write(UUID player, State state) {
        data.set(player + ".charge", state.charge.value());
        data.set(player + ".cinematics", state.cinematics);
    }
    public void save() {
        loaded.forEach(this::write);
        saveFile();
    }
    private void saveFile() {
        try { data.save(file); }
        catch (IOException error) { plugin.getLogger().severe("Could not save player charge: " + error.getMessage()); }
    }
    @Override public void close() { save(); loaded.clear(); }
}
