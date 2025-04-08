package hva.app.search;

import hva.app.exception.UnknownHabitatKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingHabitatException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Show all animals of a given habitat.
 **/
class DoShowAnimalsInHabitat extends Command<Hotel> {

  DoShowAnimalsInHabitat(Hotel receiver) {
    super(Label.ANIMALS_IN_HABITAT, receiver);
  }

  @Override
  protected void execute() throws CommandException {
    String habitatId = Form.requestString(hva.app.habitat.Prompt.habitatKey());
      try {
        String[] animals = _receiver.getAnimalsInHabitat(habitatId);
        for (String animal : animals) {
          _display.addLine(animal); 
        }
        _display.display();
      } catch (MissingHabitatException e) {
        throw new UnknownHabitatKeyException(habitatId);
      }  
  }
}
