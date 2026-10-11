package io.github.legendarykoth;
import java.util.*;

final class CaptureState {
    UUID holder;
    int seconds;
    boolean contested;
    void reset(){holder=null;seconds=0;contested=false;}
    UUID tick(Set<UUID> inside,int required,boolean pause) {
        contested=inside.size()>1;
        if(holder!=null&&!inside.contains(holder)){holder=null;seconds=0;}
        if(inside.size()==1) {
            UUID player=inside.iterator().next();
            if(!player.equals(holder)){holder=player;seconds=0;}
            seconds++;
            if(seconds>=required)return holder;
        } else if(contested&&!pause)seconds=0;
        return null;
    }
}
