package hva.app.vaccine;

import hva.app.exception.*;
import hva.core.Hotel;
import hva.core.exception.MissingAnimalException;
import hva.core.exception.MissingEmployeeException;
import hva.core.exception.MissingResponsibilityException;
import hva.core.exception.MissingVaccineException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;


/**
 * Vaccinate by a given veterinarian a given animal with a given vaccine.
 **/
class DoVaccinateAnimal extends Command<Hotel> {
  DoVaccinateAnimal(Hotel receiver) {
    super(Label.VACCINATE_ANIMAL, receiver);
  }

  @Override
  protected final void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("VaccineId", Prompt.vaccineKey());
    form.addStringField("VetId", Prompt.veterinarianKey());
    form.addStringField("animalId", hva.app.animal.Prompt.animalKey());
    form.parse();
    String vaccineId = form.stringField("VaccineId");
    String vetId = form.stringField("VetId");  
    String animalId = form.stringField("animalId");
    try {
      if (!_receiver.vaccinateAnimal(vaccineId, animalId, vetId)) {
        _display.addLine(Message.wrongVaccine(vaccineId,animalId));
      }
    } catch (MissingEmployeeException  e) {
      throw new UnknownVeterinarianKeyException(vetId);
    } catch (MissingVaccineException e) {
      throw new UnknownVaccineKeyException(vaccineId);
    } catch (MissingAnimalException e) {
      throw new UnknownAnimalKeyException(animalId);
    } catch (MissingResponsibilityException e){
      throw new VeterinarianNotAuthorizedException(vetId, e.getSpecies());
    }
    _display.display();
  }
}
