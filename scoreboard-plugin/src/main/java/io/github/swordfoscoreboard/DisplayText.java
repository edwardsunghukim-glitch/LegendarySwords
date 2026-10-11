package io.github.swordfoscoreboard;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class DisplayText {
    static Component component(String text){return LegacyComponentSerializer.legacyAmpersand().deserialize(text);}
    static String replace(String text,Map<String,String> values) {
        for(var entry:values.entrySet())text=text.replace("{"+entry.getKey()+"}",entry.getValue());return text;
    }
    private static Object api(String plugin,String method,Player player) {
        Plugin target=Bukkit.getPluginManager().getPlugin(plugin);if(target==null||!target.isEnabled())return null;
        try{return player==null?target.getClass().getMethod(method).invoke(target):target.getClass().getMethod(method,UUID.class).invoke(target,player.getUniqueId());}
        catch(ReflectiveOperationException ex){return null;}
    }
    static Map<String,String> values(Player p,String ip) {
        Map<String,String> values=new HashMap<>();values.put("player",p.getName());values.put("ip",ip);
        values.put("online",String.valueOf(Bukkit.getOnlinePlayers().size()));values.put("maxplayers",String.valueOf(Bukkit.getMaxPlayers()));
        values.put("ping",String.valueOf(p.getPing()));values.put("kills",String.valueOf(p.getStatistic(Statistic.PLAYER_KILLS)));
        values.put("deaths",String.valueOf(p.getStatistic(Statistic.DEATHS)));
        Object wallet=api("KillTokens","getWalletBalance",p),combat=api("KillTokens","getCombatSeconds",p),koth=api("LegendaryKoTH","getDisplayStatus",null);
        values.put("wallet",wallet instanceof Number n?java.text.NumberFormat.getIntegerInstance(Locale.US).format(n.longValue()):"—");
        values.put("combat",combat instanceof Number n&&n.longValue()>0?n.longValue()+"s":"Safe");
        values.put("koth",koth==null?"Inactive":String.valueOf(koth));return values;
    }
}
