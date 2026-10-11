package io.github.legendaryarmor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
final class ArmorFlight {
    private record Before(boolean allowed,boolean flying){}
    private final JavaPlugin plugin;private final Map<UUID,Before> leases=new HashMap<>();
    ArmorFlight(JavaPlugin plugin){this.plugin=plugin;}
    void update(Player player,boolean enabled) {
        if(enabled&&!player.isDead()&&player.getGameMode()!=GameMode.SPECTATOR&&player.getGameMode()!=GameMode.CREATIVE) {
            if(!leases.containsKey(player.getUniqueId())) {
                var boss=player.getMetadata("legendaryswords_boss_flight").stream().map(MetadataValue::value).filter(v->v instanceof Map<?,?>).map(v->(Map<?,?>)v).findFirst().orElse(Map.of());
                Before before=new Before(boss.get("allowed") instanceof Boolean b?b:player.getAllowFlight(),boss.get("flying") instanceof Boolean b?b:player.isFlying());
                leases.put(player.getUniqueId(),before);player.setMetadata("legendaryarmor_flight",new FixedMetadataValue(plugin,Map.of("allowed",before.allowed,"flying",before.flying)));
            }
            if(!player.getAllowFlight())player.setAllowFlight(true);
        } else clear(player);
    }
    void clear(Player player) {
        Before before=leases.remove(player.getUniqueId());player.removeMetadata("legendaryarmor_flight",plugin);
        if(before==null||player.hasMetadata("legendaryswords_boss_flight")||player.getGameMode()==GameMode.CREATIVE||player.getGameMode()==GameMode.SPECTATOR)return;
        player.setFlying(before.allowed&&before.flying&&!player.isDead());player.setAllowFlight(before.allowed);
    }
}
