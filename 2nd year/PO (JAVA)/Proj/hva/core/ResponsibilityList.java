package hva.core;

import java.util.*;

import hva.core.exception.DuplicateKeyException;

class ResponsibilityList<V> extends ArrayList<V>{

    @Override
    public boolean add(V value) throws DuplicateKeyException{
        if (contains(value))
            throw new DuplicateKeyException();
        return super.add(value);
    }
    
}
