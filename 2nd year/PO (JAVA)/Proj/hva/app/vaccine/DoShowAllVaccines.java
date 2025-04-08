package hva.app.vaccine;

import hva.core.Hotel;
import pt.tecnico.uilib.menus.Command;

/**
 * Show all vaccines.
 **/
class DoShowAllVaccines extends Command<Hotel> {

  DoShowAllVaccines(Hotel receiver) {
    super(Label.SHOW_ALL_VACCINES, receiver);
  }
  
  @Override
  protected final void execute() {

    String[] sortedVaccines = _receiver.get_vaccines(); 
    
    for (String vaccine : sortedVaccines) {
      _display.addLine(vaccine); 
    }
    
    _display.display(); 
  }
}
