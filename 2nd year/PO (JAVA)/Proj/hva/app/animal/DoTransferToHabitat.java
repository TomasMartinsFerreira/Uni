package hva.app.animal;

import hva.app.exception.UnknownAnimalKeyException;
import hva.app.exception.UnknownHabitatKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingAnimalException;
import hva.core.exception.MissingHabitatException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;


/**
 * Transfers a given animal to a new habitat of this zoo hotel.
 */
class DoTransferToHabitat extends Command<Hotel> {

  DoTransferToHabitat(Hotel hotel) {
    super(Label.TRANSFER_ANIMAL_TO_HABITAT, hotel);
  }
  
  @Override
  protected final void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("animalId", Prompt.animalKey());
    form.addStringField("habitatId",hva.app.habitat.Prompt.habitatKey());
    form.parse();
    String animalId = form.stringField("animalId");
    String habitatId = form.stringField("habitatId");
    try {
      _receiver.changeAnimalHabitat(animalId , habitatId);
    } catch (MissingAnimalException e) {
      throw new UnknownAnimalKeyException(animalId);
    } catch (MissingHabitatException e) { 
      throw new UnknownHabitatKeyException(habitatId);
    } 
  }
}
