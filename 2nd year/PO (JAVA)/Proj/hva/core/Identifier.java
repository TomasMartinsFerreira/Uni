package hva.core;

import java.io.Serializable;

/**  
 * 'Identifier' class implements 'Comparable' and 'Serializable' for comparison and serialization. 
 * Holds the ID and the Name of other classes
*/
class Identifier implements Comparable<Identifier>, Serializable {
    private final String _id;
    private final String _name; 

    // Constructor to initialize 'id' and 'name'.
    Identifier(String id, String name) {
        _id = id;
        _name = name;
    }

    // Getter for '_id'.
    String get_id() {
        return _id;
    }

    // Getter for '_name'.
    String get_name() {
        return _name;
    }

    // Override 'compareTo' to compare based on '_id' (case-insensitive).
    @Override
    public int compareTo(Identifier other) {
        return _id.compareToIgnoreCase(other.get_id());
    }
}

