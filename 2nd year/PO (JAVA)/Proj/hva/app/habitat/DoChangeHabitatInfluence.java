package hva.app.habitat;

import hva.app.exception.UnknownHabitatKeyException;
import hva.app.exception.UnknownSpeciesKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingHabitatException;
import hva.core.exception.MissingSpeciesException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Associate (positive or negatively) a species to a given habitat.
 **/
class DoChangeHabitatInfluence extends Command<Hotel> {
  final String[] options = {"NEG", "NEU", "POS"};

  DoChangeHabitatInfluence(Hotel receiver) {
    super(Label.CHANGE_HABITAT_INFLUENCE, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("habitatId", Prompt.habitatKey());
    form.addStringField("SpeciesId", hva.app.animal.Prompt.speciesKey());
    form.addOptionField("influenceType", Prompt.habitatInfluence(), options);
    form.parse();
    String  SpeciesId = form.stringField("SpeciesId");
    String habitatId = form.stringField("habitatId");
    String influenceType = form.optionField("influenceType");

    try{
      _receiver.changeHabitatInfluence(habitatId, SpeciesId, influenceType);
    } catch (MissingHabitatException e) {
      throw new UnknownHabitatKeyException(habitatId);
    } catch (MissingSpeciesException e) {
      throw new UnknownSpeciesKeyException(SpeciesId);
    }
  }
}
