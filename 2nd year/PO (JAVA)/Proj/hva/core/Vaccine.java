package hva.core;

import java.util.*;

/** 
 * 'Vaccine' class is an interface of a vaccine that extends 'Identifier' and tracks vaccine usages and applicable species. 
*/
class Vaccine extends Identifier {
    private int _usages = 0;     
    private final String[] _speciesNames;
    private final String[] _speciesIds;

    // Constructor to initialize 'Vaccine' with id, name, and species IDs.
    Vaccine(String id, String name, String[] speciesNames,String[] speciesIds) {
        super(id, name);
        _speciesNames = speciesNames;
        _speciesIds = speciesIds;
    }
    
    Boolean vaccinate(Animal animal){
        int damage = 0;
        _usages++;
        for (String speciesName : _speciesNames){
            if (speciesName.equals(animal.get_species().get_name())){
                animal.updateHealth(Health.NORMAL);
                return true;
            }
            int speciesDamage = damage(speciesName, animal.get_species().get_name());
            if (damage < speciesDamage)
                damage = speciesDamage;
        }
        animal.updateHealth(Health.assignHealth(damage));
        return false;
    }

    private int damage(String vaccineSpecies, String animalSpecies){
        return Math.max(vaccineSpecies.length(), animalSpecies.length()) - commonCharacters(vaccineSpecies, animalSpecies);
    }

    private int commonCharacters(String vaccineSpecies, String animalSpecies) {
        Map<Character, Integer> charCount = new HashMap<>();
        for (char c : animalSpecies.toCharArray()) {
            charCount.put(c, charCount.getOrDefault(c, 0) + 1);
        }
        int common = 0;
        for (char c : vaccineSpecies.toCharArray()) {
            if (charCount.getOrDefault(c, 0) > 0) {
                common++;
                charCount.put(c, charCount.get(c) - 1);
            }
        }
        return common;
    }

    // Getter for the number of usages.
    int get_usages() {
        return _usages;
    }

    // Getter for the species IDs.
    String[] get_speciesIds() {
        Arrays.sort(_speciesIds);
        return _speciesIds;
    }


    // Override 'toString' to return a formatted string with vaccine details.
    @Override
    public String toString() {
        if (get_speciesIds().length == 0) {
            return "VACINA" + "|" + get_id() + "|" + get_name() + "|" + get_usages();
        }
        return "VACINA|" + get_id() + "|" + get_name() + "|" + get_usages() + "|" + String.join(",", get_speciesIds());
    }
}
