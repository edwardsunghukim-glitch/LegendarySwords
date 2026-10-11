package io.github.legendaryarmor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.function.LongSupplier;
/** Timed wing state, separate from native flight leases. */
final class WingPower {
    private record Window(ArmorSet set,long expires,long ready){}
    private final JavaPlugin plugin;private final LongSupplier clock;private final Map<UUID,Window> windows=new HashMap<>();
    WingPower(JavaPlugin plugin){this(plugin,System::currentTimeMillis);}
    WingPower(JavaPlugin plugin,LongSupplier clock){this.plugin=plugin;this.clock=clock;}
    static boolean supports(ArmorSet set){return set==ArmorSet.ANGEL||set==ArmorSet.DRAGON||set==ArmorSet.LEVIATHAN;}
    boolean enabled(ArmorSet set){return supports(set)&&plugin.getConfig().getBoolean("sets."+set.id+".flight-enabled",true);}
    boolean active(UUID id,ArmorSet set){Window window=windows.get(id);return window!=null&&window.set==set&&clock.getAsLong()<window.expires;}
    long remaining(UUID id,ArmorSet set){Window window=windows.get(id);return active(id,set)?Math.max(0,(window.expires-clock.getAsLong()+999)/1000):0;}
    boolean start(UUID id,ArmorSet set) {
        if(!enabled(set))return false;long now=clock.getAsLong();Window old=windows.get(id);
        if(old!=null&&old.ready>now)return false;
        long duration=Math.max(1,Math.min(600,plugin.getConfig().getLong("wings.duration-seconds",45)))*1000;
        long cooldown=Math.max(0,Math.min(3600,plugin.getConfig().getLong("wings.cooldown-seconds",90)))*1000;
        windows.put(id,new Window(set,now+duration,now+duration+cooldown));return true;
    }
    void validateSet(UUID id,ArmorSet set){Window old=windows.get(id);if(old!=null&&old.set!=set)interrupt(id);}
    void interrupt(UUID id){Window old=windows.get(id);if(old!=null)windows.put(id,new Window(old.set,clock.getAsLong(),old.ready));}
    void clear(){windows.clear();}
}
