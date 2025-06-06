package hva.app.vaccine;

import hva.core.Hotel;
import pt.tecnico.uilib.menus.Command;

/**
 * Show all applied vacines by all veterinarians of this zoo hotel.
 **/
class DoShowVaccinations extends Command<Hotel> {

  DoShowVaccinations(Hotel receiver) {
    super(Label.SHOW_VACCINATIONS, receiver);
  }
  
  @Override
  protected final void execute() {
    for (String vaccine : _receiver.getVaccinationHistory()) {    
      _display.addLine(vaccine);
    }
    _display.display();
  }
}
