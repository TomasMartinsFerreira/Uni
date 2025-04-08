package hva.app.vaccine;

import hva.app.exception.DuplicateVaccineKeyException;
import hva.app.exception.UnknownSpeciesKeyException;
import hva.core.Hotel;
import hva.core.exception.DuplicateVaccineException;
import hva.core.exception.MissingSpeciesException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Apply a vaccine to a given animal.
 **/
class DoRegisterVaccine extends Command<Hotel> {

  DoRegisterVaccine(Hotel receiver) {
    super(Label.REGISTER_VACCINE, receiver);
  }

  @Override
  protected final void execute() throws CommandException {
    Form form =  new Form();

    form.addStringField("vaccineId", Prompt.vaccineKey());
    form.addStringField("vaccineName", Prompt.vaccineName());
    form.addStringField("speciesIds",Prompt.listOfSpeciesKeys());
    form.parse();
    String[] speciesIds = form.stringField("speciesIds").split(",");
    String vaccineId = form.stringField("vaccineId");
    String vaccineName = form.stringField("vaccineName");
    try {
      _receiver.registerVaccine(vaccineId, vaccineName, speciesIds);
    } catch (DuplicateVaccineException e) {
      throw new DuplicateVaccineKeyException(vaccineId);
    } catch(MissingSpeciesException e) {
      throw new UnknownSpeciesKeyException(e.getSpecies());
    }
  }
}
