package hva.app.habitat;

import hva.app.exception.UnknownHabitatKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingHabitatException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Change the area of a given habitat.
 **/
class DoChangeHabitatArea extends Command<Hotel> {

  DoChangeHabitatArea(Hotel receiver) {
    super(Label.CHANGE_HABITAT_AREA, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("habitatId", Prompt.habitatKey());
    form.addIntegerField("areaId", Prompt.habitatArea());
    form.parse();
    String habitatId = form.stringField("habitatId");
    int newArea = form.integerField("areaId");
    try {
      _receiver.changeHabitatArea(habitatId, newArea);
    } catch (MissingHabitatException e) {
      throw new UnknownHabitatKeyException(habitatId);
    }
  }
}
