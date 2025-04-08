package hva.core;

import java.util.*;

class RecordList<V extends Entry> extends ArrayList<V> {

    Object[] getErroneus(){
        List<V> erroneous = new ArrayList<>();
        for (V entry : this){
            if (entry.checkErroneous())
                erroneous.add(entry);
        }
        return erroneous.toArray();
    }
}
