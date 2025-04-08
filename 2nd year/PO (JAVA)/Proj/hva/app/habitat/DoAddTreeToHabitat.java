package hva.app.habitat;

import hva.app.exception.DuplicateTreeKeyException;
import hva.app.exception.UnknownHabitatKeyException;
import hva.core.Hotel;
import hva.core.exception.DuplicateTreeException;
import hva.core.exception.MissingHabitatException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Add a new tree to a given habitat of the current zoo hotel.
 **/
class DoAddTreeToHabitat extends Command<Hotel> {
  final String[] options = {"CADUCA", "PERENE"};
  DoAddTreeToHabitat(Hotel receiver) {
    super(Label.ADD_TREE_TO_HABITAT, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("habitatId", Prompt.habitatKey());
    form.addStringField("treeId", Prompt.treeKey());
    form.addStringField("treeName", Prompt.treeName());
    form.addIntegerField("treeAge", Prompt.treeAge());
    form.addIntegerField("treeDifficulty", Prompt.treeDifficulty());
    form.addOptionField("treeType", Prompt.treeType(), options);
    form.parse();
    String habitatId = form.stringField("habitatId");
    String treeId = form.stringField("treeId");
    String treeName = form.stringField("treeName");
    int diff = form.integerField("treeDifficulty");
    int treeAge = form.integerField("treeAge");
    String treeType = form.optionField("treeType");

    try {
      _display.addLine(_receiver.registerTreeInHabitat(treeId, treeName, treeAge, diff, treeType, habitatId));
    } catch (MissingHabitatException e) {
      throw new UnknownHabitatKeyException(habitatId);
    } catch (DuplicateTreeException e) {
      throw new DuplicateTreeKeyException(treeId);
      }
    _display.display();
  }
}
