package io.github.killtokens;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

final class TokenBank {
    private final Path path;
    private final Map<UUID,Long> balances=new HashMap<>();
    double multiplier=1;long multiplierUntil=0;
    TokenBank(Path path) throws IOException, org.bukkit.configuration.InvalidConfigurationException {
        this.path=path;YamlConfiguration saved=new YamlConfiguration();
        if(Files.exists(path))saved.load(path.toFile());
        var section=saved.getConfigurationSection("balances");
        if(section!=null)for(String key:section.getKeys(false))try{balances.put(UUID.fromString(key),Math.max(0,section.getLong(key)));}catch(IllegalArgumentException ignored){}
        multiplier=saved.getDouble("multiplier.factor",1);multiplierUntil=saved.getLong("multiplier.until",0);
        if(!Double.isFinite(multiplier)||multiplier<1||multiplier>100)multiplier=1;
    }
    long balance(UUID player){return balances.getOrDefault(player,0L);}
    boolean change(UUID player,long delta) throws IOException {
        long previous=balance(player),next;
        try{next=Math.addExact(previous,delta);}catch(ArithmeticException ex){return false;}
        if(next<0)return false;balances.put(player,next);
        try{save();return true;}catch(IOException ex){if(previous==0)balances.remove(player);else balances.put(player,previous);throw ex;}
    }
    void activate(double factor,long until) throws IOException {
        double old=multiplier;long oldUntil=multiplierUntil;multiplier=factor;multiplierUntil=until;
        try{save();}catch(IOException ex){multiplier=old;multiplierUntil=oldUntil;throw ex;}
    }
    double activeMultiplier(long now){return multiplierUntil>0&&now>=multiplierUntil?1:multiplier;}
    private void save() throws IOException {
        YamlConfiguration out=new YamlConfiguration();balances.forEach((id,value)->out.set("balances."+id,value));
        out.set("multiplier.factor",multiplier);out.set("multiplier.until",multiplierUntil);
        Files.createDirectories(path.getParent());Path temp=path.resolveSibling(path.getFileName()+".tmp");out.save(temp.toFile());
        try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException ex){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
}
