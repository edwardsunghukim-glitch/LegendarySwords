package io.github.legendarykits;
import java.util.*;

final class KitSlots {
    static Map<String,Integer> normalize(Map<String,Integer> requested,int size) {
        Map<String,Integer> out=new LinkedHashMap<>();Set<Integer> occupied=new HashSet<>();
        for(var entry:requested.entrySet()) {
            int slot=entry.getValue();
            if(slot>=0&&slot<size&&occupied.add(slot))out.put(entry.getKey(),slot);
        }
        for(String id:requested.keySet()) if(!out.containsKey(id)) {
            int slot=0;while(slot<size&&occupied.contains(slot))slot++;
            out.put(id,slot<size?slot:-1);if(slot<size)occupied.add(slot);
        }
        return out;
    }
    static Map<String,Integer> move(Map<String,Integer> current,String id,int target,int size) {
        if(!current.containsKey(id)||target<0||target>=size)throw new IllegalArgumentException("Invalid kit or slot");
        Map<String,Integer> out=new LinkedHashMap<>(current);int old=current.get(id);
        for(var entry:current.entrySet()) if(!entry.getKey().equals(id)&&entry.getValue()==target)out.put(entry.getKey(),old);
        out.put(id,target);return out;
    }
}
