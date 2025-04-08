package hva.app.search;

import hva.app.exception.UnknownAnimalKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingAnimalException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Show all medical acts applied to a given animal.
 **/
class DoShowMedicalActsOnAnimal extends Command<Hotel> {
  DoShowMedicalActsOnAnimal(Hotel receiver) {
    super(Label.MEDICAL_ACTS_ON_ANIMAL, receiver);
  }

  @Override
  protected void execute() throws CommandException {
    String animalId = Form.requestString(hva.app.animal.Prompt.animalKey());
    try {
      String[] record = _receiver.getAnimalVaccinations(animalId);
      for (String entry : record) {
        _display.addLine(entry);
      }
    } catch (MissingAnimalException e) {
      throw new UnknownAnimalKeyException(animalId);
    }
    _display.display();
  }
}
