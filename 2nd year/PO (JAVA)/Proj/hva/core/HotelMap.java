package hva.core;

import hva.core.exception.*;
import java.util.*;

// 'HotelMap' extends 'HashMap' and overrides 'put' and 'get' methods for extra validation.
class HotelMap<K, V> extends HashMap<K, V> {

    // 'put' checks if the key already exists. If so, it throws an exception.
    @Override
    public V put(K key, V value) throws DuplicateKeyException {
        // Check for duplicates in a case-insensitive manner
        for (K existingKey : this.keySet()) {
            if (existingKey.toString().equalsIgnoreCase(key.toString())) {
                throw new DuplicateKeyException();
            }
        }
        return super.put(key, value);
    }

    // 'get' checks if the key exists. If not, it throws an exception.
    @Override
    public V get(Object key) throws KeyNotFoundException{
        V value = super.get(key);
        if (value == null)
            throw new KeyNotFoundException();
        return value;
    }
}

