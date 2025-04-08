package hva.core;

/**  '
 * Tree' is an abstract class that interfaces a Tress and extends 'Identifier'. 
 * */
class Tree extends Identifier implements Observer {
    private  float _age;        
    private final int _diff;
    private final Leaf _leaf; 
    private int _seasonalEffort;
    private Season _season;

    // Constructor to initialize 'Tree' with id, name, age, and difficulty.
    Tree(String id, String name, int age, int diff, Leaf leaf, Season season) {
        super(id, name);
        _age = age;
        _diff = diff;
        _leaf = leaf;
        _seasonalEffort = _leaf.getSeasonalEffort(season);
        _season = season;
    }

    // Method to set the habitat where the tree is planted.
    void plant(Habitat habitat) {
    }


    // Getter for the age of the tree.
    int get_age() {
        return (int) _age; //somos capazes de ter que mudar a lógica deste para tirar o casting
    }

    // Getter for the difficulty rating.
    int get_diff() {
        return _diff;
    }

    int getCleaningEffort(){
        return get_diff() * getSeasonalEffort() * ((int) Math.log(get_age()+1));
    }

    int getSeasonalEffort(){
        return _seasonalEffort;
    }

    @Override
    public String toString() {
        return "ÁRVORE|" + get_id() +"|" + get_name() + "|" + get_age() + "|" + get_diff() +"|" +
        get_type() + "|" + get_cycle(_season);
    }

    // method to get the string of the type of tree
    String get_type(){
        return _leaf.toString();
    }

    String get_cycle(Season season){
        Cycle  cycle = _leaf.getCycle(season);
        return cycle.toString();
    }

    @Override
    public void update(Season season){
        _season = season;
        _seasonalEffort = _leaf.getSeasonalEffort(season);
        _age += 0.25;
    }
}

