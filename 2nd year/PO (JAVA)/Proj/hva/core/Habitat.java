package hva.core;

import java.util.*;


/**  
 * The 'Habitat' class represents the interface of 1 Habitat
 */
public class Habitat extends Identifier implements Responsibility {
    private int _workers;
    private int _area;
    private final Map<String, Tree> _trees;
    private final Map<String, Integer > _influence; // first the id of the species then the influence
    private final  List<Animal> _animals;

    Habitat(String habitatId, String name, int area) {
        super(habitatId, name);
        _area = area;  
        _trees = new HotelMap<>();
        _animals = new ArrayList<>();
        _influence = new HashMap<>();
        _workers = 0;
    }

    // Method to add a tree to the habitat.
    // Throws an IllegalStateException if something goes wrong during the process (though the exception isn't explicitly handled here).
    void addTree(Tree tree){
        _trees.put(tree.get_id(), tree); 
        tree.plant(this);
    }

    void addAnimal(Animal animal){
        _animals.add(animal);
    }

    void addInfluence(String speciesId){
        _influence.put(speciesId, 0);
    }

    void changeInfluence(String SpeciesId, int influence) {
        _influence.put(SpeciesId, influence);
    }

    Object[] get_trees() {
        Collection<Tree> trees = _trees.values();
        return trees.toArray(); 
    }

    Object[] get_animals() {
        return _animals.toArray(); 
    }

    int get_area() {
        return _area;
    }

    int totalCleaningEffort(){
        int total = 0;
        for(Tree tree: _trees.values())
            total += tree.getCleaningEffort();
        return total;
    }

    int sameSpecies(Species species) {
        int equalSpecies = -1;
        for (Animal animalInHabitat : _animals) {
            if (animalInHabitat.get_species() == species)
                equalSpecies += 1;
        }
        return equalSpecies;
    }

    int total() {
        return _animals.size();
    }

    int DiferentSpecies(Species species) {
        int equalSpecies = 0;
        for (Animal animalInHabitat : _animals) {
            if (animalInHabitat.get_species() != species)
                equalSpecies += 1;
        }
        return equalSpecies;
    }

    int getInfluence(String speciesId) {
        return _influence.getOrDefault(speciesId, 0);
    }

    void removeAnimal(Animal animal) {
        _animals.remove(animal);
    }

    public void changeArea(int newArea) {
        _area = newArea;
    }

    @Override
    public void addWorker(){
        _workers += 1;
    }

    @Override
    public void removeWorker(){
        _workers -= 1;
    }

    @Override
    public int getTotalWorkload(){
        return _area + 3 * _animals.size() + totalCleaningEffort();
    }

    @Override
    public int getWorkerAmount(){
        return _workers;
    }


    @Override
    public String get_id(){
        return super.get_id();
    }

    // Override the 'toString' method to provide a string representation of the Habitat object.
    @Override
    public String toString() {
        return "HABITAT|" + get_id() + "|" + get_name() + 
        "|" + get_area() +"|"+ get_trees().length;
    }
}
