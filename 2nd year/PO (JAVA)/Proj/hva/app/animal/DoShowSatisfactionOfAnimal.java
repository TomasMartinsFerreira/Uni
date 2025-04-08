package hva.app.animal;

import hva.app.exception.UnknownAnimalKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingAnimalException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;
/**
 * Shows the satisfaction of a given animal.
 */
class DoShowSatisfactionOfAnimal extends Command<Hotel> {

  DoShowSatisfactionOfAnimal(Hotel receiver) {
    super(Label.SHOW_SATISFACTION_OF_ANIMAL, receiver);
  }
  
  @Override
  protected final void execute() throws CommandException {

    String animalId = Form.requestString(Prompt.animalKey());

    try {
        String satisfaction = String.valueOf(_receiver.getAnimalSatifaction(animalId));
        _display.addLine(satisfaction); 
    } catch (MissingAnimalException e) {
        throw new UnknownAnimalKeyException(animalId);
    }
    _display.display();
  }
}