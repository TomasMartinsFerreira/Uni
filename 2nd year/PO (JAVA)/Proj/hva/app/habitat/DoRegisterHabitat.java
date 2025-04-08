package hva.app.habitat;

import hva.app.exception.DuplicateHabitatKeyException;
import hva.core.Hotel;
import hva.core.exception.DuplicateHabitatException;
import pt.tecnico.uilib.forms.*;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Add a new habitat to this zoo hotel.
 **/
class DoRegisterHabitat extends Command<Hotel> {

  DoRegisterHabitat(Hotel receiver) {
    super(Label.REGISTER_HABITAT, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    Form form = new Form();
    String habitatId, name;
    int area;
    form.addStringField("habitatId", Prompt.habitatKey());
    form.addStringField("name", Prompt.habitatName());
    form.addIntegerField("area", Prompt.habitatArea());
    form.parse();
    
    habitatId = form.stringField("habitatId");
    name = form.stringField("name");
    area = form.integerField("area");
    try{
      _receiver.registerHabitat(habitatId, name, area);
    }catch(DuplicateHabitatException e){
      throw new DuplicateHabitatKeyException(habitatId);
    }
  }
}
