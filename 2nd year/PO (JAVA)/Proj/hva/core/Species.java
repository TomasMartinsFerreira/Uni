package hva.core;

import java.util.*;;

/**
 *  The class contains a interface of 1 species and extends 'Identifier' and implements 'Responsibility'.
 */
class Species extends Identifier implements Responsibility {
    int _workers;
    List<Animal> _animals;

    // Constructor to initialize 'Species' using 'Identifier' constructor.
    Species(String id, String name) {
        super(id, name);
        _workers = 0;
        _animals = new ArrayList<>();
    }

    void addAnimal(Animal animal){
        _animals.add(animal);
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
    public int getTotalWorkload() {
        return _animals.size();
    }

    @Override
    public int getWorkerAmount(){
        return _workers;
    }

    @Override
    public String get_name(){
        return super.get_name();
    }

    @Override
    public String get_id(){
        return super.get_id();
    }
}
