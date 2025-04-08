package hva.app.animal;

import hva.app.exception.*;
import hva.core.Hotel;
import hva.core.exception.*;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;


/**
 * Register a new animal in this zoo hotel.
 */
class DoRegisterAnimal extends Command<Hotel> {

  DoRegisterAnimal(Hotel receiver) {
    super(Label.REGISTER_ANIMAL, receiver);
  }
  
  @Override
  protected final void execute() throws CommandException {
    Form form =  new Form();
    String speciesId, habitatId, animalId, name;

    form.addStringField("animalId", Prompt.animalKey());
    form.addStringField("name", Prompt.animalName());
    form.addStringField("speciesId",Prompt.speciesKey());
    form.addStringField("habitatId",hva.app.habitat.Prompt.habitatKey());
    form.parse();
    speciesId = form.stringField("speciesId");
    habitatId = form.stringField("habitatId");
    animalId = form.stringField("animalId");
    name = form.stringField("name");

    try{
      if (!_receiver.hasSpecies(speciesId))
        _receiver.registerSpecies(speciesId, Form.requestString(Prompt.speciesName()));
      _receiver.registerAnimal(animalId, name, habitatId, speciesId);
    }catch(MissingHabitatException e){
      throw new UnknownHabitatKeyException(habitatId);
    } catch(DuplicateAnimalException e){
      throw new DuplicateAnimalKeyException(animalId);
    } catch(MissingSpeciesException e){
      throw new UnknownSpeciesKeyException(speciesId);
    }
  }
}
