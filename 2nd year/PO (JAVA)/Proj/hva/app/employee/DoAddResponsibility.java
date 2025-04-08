package hva.app.employee;

import hva.app.exception.NoResponsibilityException;
import hva.app.exception.UnknownEmployeeKeyException;
import hva.core.Hotel;
import hva.core.exception.DuplicateResponsibilityException;
import hva.core.exception.MissingEmployeeException;
import hva.core.exception.MissingHabitatException;
import hva.core.exception.MissingSpeciesException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Add a new responsability to an employee: species to veterinarians and 
 * habitats to zookeepers.
 **/
class DoAddResponsibility extends Command<Hotel> {

  DoAddResponsibility(Hotel receiver) {
    super(Label.ADD_RESPONSABILITY, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("employeeId", Prompt.employeeKey());
    form.addStringField("newResponsibility", Prompt.responsibilityKey());
    form.parse();
    String employeeId = form.stringField("employeeId");
    String responsibilitytId = form.stringField("newResponsibility");
    try {
      switch(_receiver.getEmployeeType(employeeId)){
        case "TRT" -> _receiver.addTRTResponsibility(employeeId, responsibilitytId);
        case "VET" -> _receiver.addVETResponsibility(employeeId, responsibilitytId);
      }
    } catch (MissingEmployeeException e) {
      throw new UnknownEmployeeKeyException(employeeId);
    } catch (MissingSpeciesException | MissingHabitatException e) {
      throw new NoResponsibilityException(employeeId, responsibilitytId);
    } catch (DuplicateResponsibilityException e){}
  }
}
