package hva.core;

import java.io.*;

public class Entry implements Serializable, Comparable<Entry> {
    private final String _vaccineId, _vetId, _speciesId;
    private boolean _erroneous;

    Entry(String vaccineId, Employee veterinarian, Responsibility species){
        _vaccineId = vaccineId;
        _vetId = veterinarian.get_id();
        _speciesId = species.get_id();
    }

    boolean setErroneous(boolean erroneous){
        _erroneous = erroneous;
        return erroneous;
    }

    boolean checkErroneous(){
        return _erroneous;
    }

    @Override
    public int compareTo(Entry entry){
        return 0;
    }

    @Override
    public String toString(){
        return "REGISTO-VACINA|" +_vaccineId + "|" + _vetId + "|" + _speciesId;
    }
}
