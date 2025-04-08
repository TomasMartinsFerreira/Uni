package hva.app.habitat;

import hva.app.exception.UnknownHabitatKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingHabitatException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Show all trees in a given habitat.
 **/
class DoShowAllTreesInHabitat extends Command<Hotel> {
  
  DoShowAllTreesInHabitat(Hotel receiver) {
    super(Label.SHOW_TREES_IN_HABITAT, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    String habitatId = Form.requestString(Prompt.habitatKey());
    try {
      String[] trees = _receiver.getTreesInHabitat(habitatId);
      for (String tree : trees) {
        _display.addLine(tree);
      }
      _display.display();  
    } catch (MissingHabitatException e) {
      throw new UnknownHabitatKeyException(habitatId);
    }
  }
}
