package hva.app.animal;

import hva.core.Hotel;
import pt.tecnico.uilib.menus.Command;

/**
 * Show all animals registered in this zoo hotel.
 */
class DoShowAllAnimals extends Command<Hotel> {
  DoShowAllAnimals(Hotel receiver) {
    super(Label.SHOW_ALL_ANIMALS, receiver);

  }

  @Override
  protected final void execute() {

    String[] sortedAnimals = _receiver.get_animals(); 

    for (String animal : sortedAnimals) {    
      _display.addLine(animal); 
    }
    
    _display.display();  
  }
}
