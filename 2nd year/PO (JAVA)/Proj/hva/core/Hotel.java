package hva.core;



import hva.core.exception.*;
import java.io.*;
import java.util.*; 

/** 
 *  The 'Hotel' class implements 'Serializable', which allows instances of this class to be serialized (converted into a byte stream). 
 * */
public class Hotel implements Serializable, Subject{

  @Serial
  private static final long serialVersionUID = 202407081733L;  // A unique identifier for the serialization of this class.
  
  private final Map<String, Animal> _animals;
  private final Map<String, Species> _species;
  private final Map<String, Habitat> _habitats;
  private final Map<String, Employee> _employees;
  private final Map<String, Tree> _trees;
  private final Map<String, Vaccine> _vaccines;
  private final Map<String,RecordList<Entry>> _veterinaryRecord;
  private final List<Observer> _observers;
  private final RecordList<Entry> _record;
  private Season _season;
  private Boolean _changed;

  Hotel(Season season) {
    _changed = false;
    _animals = new HotelMap<>();
    _species = new HotelMap<>();
    _habitats = new HotelMap<>();
    _trees = new HotelMap<>();
    _vaccines = new HotelMap<>();
    _employees = new HotelMap<>();
    _observers = new ArrayList<>();
    _veterinaryRecord = new HotelMap<>();
    _record = new RecordList<>();
    _season = season; 
  }

  /* /////////////////////////////////// Registers \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ */

  // Registers a new animal by creating an instance of Animal and adding it to the _animals map.
  public Animal registerAnimal(String animalId, String name, String habitatId, String speciesId) 
  throws DuplicateAnimalException, MissingHabitatException, MissingSpeciesException 
  {
    try{
      Habitat habitat = this.get_habitat(habitatId);
      Species species = this.get_species(speciesId); 
      Animal animal = new Animal(animalId, name, habitat, species); 
      _animals.put(animalId, animal);
      this.change();
      return animal;
    } catch(DuplicateKeyException e){
      throw new DuplicateAnimalException();
    }
  }

  // Registers a new species and adds it to the _species map.
  public Species registerSpecies(String speciesId, String name){
    Species species = new Species(speciesId, name);  
    _species.put(speciesId, species);
    this.change();
    for (Habitat habitat: _habitats.values()) {
      habitat.addInfluence(speciesId);
    }
    return species;
  }

  // Creates a new tree (deciduous or evergreen based on type) and adds it to the _trees map.
  Tree createTree(String treeId, String name, int age, int diff, String type) throws DuplicateTreeException {
    try{
      Tree tree = new Tree(treeId, name, age, diff, Leaf.valueOf(type), _season);
      _trees.put(treeId, tree);
      this.attach(tree);
      this.change();
      return tree;
    }catch(DuplicateKeyException e){
      throw new DuplicateTreeException();
    }
  }

  // Registers a new Tree in the habitat given
  public String registerTreeInHabitat(String treeId, String name, int age, int diff, String treeType, String habitatId)
  throws DuplicateTreeException, MissingHabitatException
  {
    Habitat habitat = get_habitat(habitatId);
    Tree tree = createTree(treeId, name, age, diff, treeType);
    habitat.addTree(tree);
    return tree.toString();
  }

  // Registers a new habitat and adds it to the _habitats map.
  public Habitat registerHabitat(String habitatId, String name, int area) throws DuplicateHabitatException {
    try{
      Habitat habitat = new Habitat(habitatId, name, area);
      _habitats.put(habitatId, habitat);
      this.change();
      return habitat;
    }catch(DuplicateKeyException e){
      throw new DuplicateHabitatException();
    }
  }

  // Registers a new employee based on employee type and adds it to the _employees map.
  public void registerVeterinary(String employeeId, String name) throws DuplicateEmployeeException {
    try{
      Employee employee = new Veterinarian(employeeId, name);
      _employees.put(employeeId, employee);
      _veterinaryRecord.put(employeeId, new RecordList<>());
      this.change();
    }catch(DuplicateKeyException e){
      throw new DuplicateEmployeeException();
    }
  }

  public void registerKeeper(String employeeId, String name) throws DuplicateEmployeeException {
    try{
      Employee employee = new Keeper(employeeId, name);
      _employees.put(employeeId, employee);
      this.change();
    }catch(DuplicateKeyException e){
      throw new DuplicateEmployeeException();
    }
  }

  // Adds a responsibility to an employee based on their type.
  public void addVETResponsibility(String employeeId, String responsibilityId) 
  throws MissingEmployeeException, MissingSpeciesException, DuplicateResponsibilityException{
    Employee employee = get_employee(employeeId);
    employee.addResponsibility(this.get_species(responsibilityId));
    this.change();
  }

  public void addTRTResponsibility(String employeeId, String responsibilityId) 
  throws MissingEmployeeException, MissingHabitatException, DuplicateResponsibilityException{
    Employee employee = get_employee(employeeId);
    employee.addResponsibility(this.get_habitat(responsibilityId));
    this.change();
  }

  public void removeResponsibility(String employeeId,String responsibilityId) 
  throws MissingEmployeeException, MissingResponsibilityException{
    Employee employee = get_employee(employeeId);
    employee.removeResponsibility(responsibilityId);
    this.change();
  }

  // Registers a new vaccine and adds it to the _vaccines map.
  public void registerVaccine(String vaccineId, String name, String[] speciesIds) 
  throws DuplicateVaccineException, MissingSpeciesException
  {
    try{
      String[] speciesNames = new String[speciesIds.length];
      int i = 0;
      for (String speciesId : speciesIds){
        speciesNames[i] = get_species(speciesId).get_name();
        i++;
      }
      Vaccine vaccine = new Vaccine(vaccineId, name, speciesNames,speciesIds);
      _vaccines.put(vaccineId, vaccine);
      this.change();
    } catch(DuplicateKeyException e){
      throw new DuplicateVaccineException();
    }
  }

  /* ///////////////////////////////// Gets \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ */

  // Gets a habitat by its ID.
  Habitat get_habitat(String habitatId) throws MissingHabitatException {
    try{
      return _habitats.get(habitatId);
    }catch(KeyNotFoundException e){
      throw new MissingHabitatException();
    }
  }

  // Gets a vaccine by its ID.
  Vaccine get_vaccine(String vaccineId) throws MissingVaccineException {
    try{
      return _vaccines.get(vaccineId);
    }catch(KeyNotFoundException e){
      throw new MissingVaccineException();
    }
  }

  // Gets a species by its ID.
  Species get_species(String speciesId) throws MissingSpeciesException {
    try{
      return _species.get(speciesId); 
    }catch(KeyNotFoundException e){
      throw new MissingSpeciesException(speciesId);
    }
  }

  // Gets a tree by its ID.
  Tree get_tree(String treeId) throws MissingTreeException {
    try{
      return _trees.get(treeId);
    }catch(KeyNotFoundException e){
      throw new MissingTreeException();
    }
  }

  // Gets an animal by its ID.
  Animal get_animal(String animalId) throws MissingAnimalException{
    try{
      return _animals.get(animalId);
    }catch(KeyNotFoundException e){
      throw new MissingAnimalException();
    } 
  }

  // Gets an employee by their ID.
  Employee get_employee(String employeeId) throws MissingEmployeeException {
    try{
      return _employees.get(employeeId);  
    }catch(KeyNotFoundException e){
      throw new MissingEmployeeException();
    }
  }

  // Gets all animals in the hotel.
  public String[] get_animals() {
    Collection<Animal> animals =  _animals.values(); 
    return StringBuilder.getString(animals.toArray());
  }

  // Gets a animal's Satifaction by their Id
  public int getAnimalSatifaction(String animalId)throws MissingAnimalException{
    Animal animal = get_animal(animalId);
    return animal.get_animalSatifaction();
  }

  // Gets all habitats in the hotel.
  public String[] get_habitats() {
    List<Object> objects = new ArrayList<>();
    Collection<Habitat> habitats = _habitats.values();
    Habitat[] habitatArray = habitats.toArray(Habitat[]::new);
    Arrays.sort(habitatArray);
    Object[] trees;
    for (Habitat habitat : habitatArray){
      objects.add(habitat);
      trees = habitat.get_trees();
      Arrays.sort(trees);
      objects.addAll(Arrays.asList(trees));
    }
    String[] objectStrings = new String[objects.size()];
    int i = 0;
    for (Object object : objects) {    
      objectStrings[i] = object.toString();
      i++;
    }
    return objectStrings;
    }

  // Gets all trees in the habitat given
  public String[] getTreesInHabitat(String habitatId) throws MissingHabitatException{
    Habitat habitat = get_habitat(habitatId);
    return StringBuilder.getString(habitat.get_trees());
  }

  // Gets all vaccines in the hotel.
  public String[] get_vaccines() {
    Collection<Vaccine> vaccines = _vaccines.values();
    return StringBuilder.getString(vaccines.toArray());
  }

  // Gets all vaccinations
  public String[] getVaccinationHistory(){
    return StringBuilder.getString(_record.toArray());
  }

  // Gets all employees in the hotel.
  public String[] get_employees() {
    Collection<Employee> employees = _employees.values();
    return StringBuilder.getString(employees.toArray());
  }

  // gets the satifaction of the given employee
  public int getEmployeeSatifaction(String employeeId)throws MissingEmployeeException{
    Employee employee = get_employee(employeeId);
    return employee.getSatisfaction();
  }

  // gets the global satisfaction of the hotel
  public int get_GlobalSatifaction() {
    int satisfation = 0;
    for (Animal animal : _animals.values()) {
      satisfation += animal.get_animalSatifaction();
    }
    for (Employee employee : _employees.values()) {
      satisfation += employee.getSatisfaction();
    }
    return satisfation;
  }

  public String[] getAnimalsInHabitat(String habitatId) throws MissingHabitatException{
    Habitat habitat = get_habitat(habitatId);
    return StringBuilder.getString(habitat.get_animals());
  }

  public String[] getErroneousVaccinations(){
    return StringBuilder.getString(_record.getErroneus());
  }

  public String[] getAnimalVaccinations(String animalId) throws MissingAnimalException{
    Animal animal = get_animal(animalId);
    return animal.getVaccinationHistory();
  }

  public String[] getVeterinaryVacinations(String veterinaryId) throws MissingEmployeeException{
    try{
      return StringBuilder.getString(_veterinaryRecord.get(veterinaryId).toArray());
    } catch(KeyNotFoundException e){
      throw new MissingEmployeeException();
    }
  }
  

  public String getEmployeeType(String employeeId) throws MissingEmployeeException{
    return get_employee(employeeId).get_type();
  }

  /* ///////////////////////////// Checks \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ */

  // Checks if hotel has been changed
  public Boolean hasChanged(){
    return _changed;
  }

  public Boolean hasSpecies(String speciesId){
    return _species.containsKey(speciesId);
  }

  /*//////////////////////////// Functionalities \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ */

  // Advances the season of the hotel and returns the ordinal value of the new season.
  int advanceSeason() {
    _season = _season.advanceSeason();
    notifyObservers();
    change();
    return _season.ordinal();
  }

  /* Sets _changed to true */
  void change(){
    _changed = true;
  }

  /* Sets _changed to false */
  void noChange(){
    _changed = false;
  }

  public void changeAnimalHabitat(String animalId,String habitatId) throws MissingAnimalException, MissingHabitatException{
    Animal animal = get_animal(animalId);
    Habitat habitat = get_habitat(habitatId);
    Habitat oldHabitat = animal.get_habitat();
    animal.changeHabitat(habitat);
    oldHabitat.removeAnimal(animal);
    habitat.addAnimal(animal);
  }

  public void changeHabitatArea(String habitatId, int newArea) throws MissingHabitatException{
    Habitat habitat = get_habitat(habitatId);
    habitat.changeArea(newArea);
  }

  public void changeHabitatInfluence(String habitatId, String speciesId, String influence) throws MissingHabitatException, MissingSpeciesException{
    Habitat habitat = get_habitat(habitatId);
    Species species = get_species(speciesId);
    habitat.changeInfluence(species.get_id(), Influence.valueOf(influence).get_value());
  }

  //if vaccine was adequated return true else return false
  public boolean vaccinateAnimal(String vaccineId, String animalId, String vetId)
  throws MissingEmployeeException, MissingResponsibilityException, MissingAnimalException, MissingVaccineException
  {
    Animal animal;
    try{
      Employee veterinarian = get_employee(vetId);
      List<Entry> thisVeterinariansRecord = _veterinaryRecord.get(vetId);
      Vaccine vaccine = get_vaccine(vaccineId);
      animal = get_animal(animalId);
      Responsibility species = veterinarian.get_responsibility(animal.getSpeciesId());
      Entry entry = new Entry(vaccineId, veterinarian, species);
      thisVeterinariansRecord.add(entry);
      animal.addVaccineEntry(entry);
      _record.add(entry);
      return ! entry.setErroneous(! vaccine.vaccinate(animal));
    } catch(KeyNotFoundException e){
      throw new MissingEmployeeException();
    } 
    }

  /*///////////////////////////////////////////Implementations\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ */

  @Override
  public void notifyObservers(){
    for(Observer observer: _observers)
      observer.update(_season);
  }

  @Override
  public void attach(Observer observer){
    _observers.add(observer);
  }

  @Override
  public void detach(Observer observer){
    _observers.remove(observer);
  }

  /**
   * Read text input file and create corresponding domain entities.
   * 
   * @param filename name of the text input file
   * @throws UnrecognizedEntryException if some entry is not correct
   * @throws IOException if there is an IO erro while processing the text file
   **/
  void importFile(String filename) throws UnrecognizedEntryException, IOException{
    try{
      Parser _parser = new Parser(this);
      _parser.parseFile(filename);
    } catch(UnrecognizedEntryException| IOException e){
      throw new IOException();
    }
  }
}
