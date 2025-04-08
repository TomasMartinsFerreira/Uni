package hva.core;

import java.util.*;
/**
 *  Class Animal represents the interface of 1 animal
 */

class Animal extends Identifier {

    private final Species _species;
    private Habitat _habitat;
    private final List<Health> _healthHistory;
    private final List<Entry> _record;

    Animal(String animalId, String name, Habitat habitat, Species species) {
        super(animalId, name);
        _species = species;
        _habitat = habitat;
        species.addAnimal(this);
        habitat.addAnimal(this);
        _healthHistory = new ArrayList<>();
        _record = new RecordList<>();
    }

    Species get_species(){
        return _species;
    }

    String getSpeciesId(){
        return _species.get_id();
    }

    Habitat get_habitat() {
        return _habitat;
    }

    void changeHabitat(Habitat habitat) {
        _habitat = habitat;
    }

    int get_animalSatifaction() {
        return Math.round(20 + 3 * _habitat.sameSpecies(_species) - 2 * _habitat.DiferentSpecies(_species) 
            + Math.round((float) _habitat.get_area() / _habitat.total()) + _habitat.getInfluence(_species.get_id()));
    }
    
    

    void updateHealth(Health health){
        _healthHistory.add(health);
    }

    String getHealthHistoryString() {
        String healthHistoryString = "";
        for (Health health : _healthHistory) {
            healthHistoryString += health.toString() + ",";
        }
        healthHistoryString = healthHistoryString.substring(0, healthHistoryString.length() - 1);    
        return healthHistoryString;
    }

    String[] getVaccinationHistory(){
        return StringBuilder.getString(_record.toArray());
    }

    void addVaccineEntry(Entry entry){
        _record.add(entry);
    }



    //Override the 'toString' method to provide method of a formatted string representation of the Animal object.
    @Override
    public String toString() {
        if (_healthHistory.isEmpty()) {
            return "ANIMAL|" + get_id() + "|" + get_name() + "|" + _species.get_id() + "|VOID|" + _habitat.get_id();

        }
        return "ANIMAL|" + get_id() + "|" + get_name() + "|" + _species.get_id() +"|" + getHealthHistoryString()+ "|"  + _habitat.get_id();
    }
}
