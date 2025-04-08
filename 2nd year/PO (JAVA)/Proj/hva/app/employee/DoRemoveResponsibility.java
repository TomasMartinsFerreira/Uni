package hva.app.employee;

import hva.app.exception.NoResponsibilityException;
import hva.app.exception.UnknownEmployeeKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingEmployeeException;
import hva.core.exception.MissingResponsibilityException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Remove a given responsability from a given employee of this zoo hotel.
 **/
class DoRemoveResponsibility extends Command<Hotel> {

  DoRemoveResponsibility(Hotel receiver) {
    super(Label.REMOVE_RESPONSABILITY, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("employeeId", Prompt.employeeKey());
    form.addStringField("Responsibility", Prompt.responsibilityKey());
    form.parse();
    String employeeId = form.stringField("employeeId");
    String responsibilitytId = form.stringField("Responsibility");
    try {
      _receiver.removeResponsibility(employeeId, responsibilitytId);
    } catch (MissingEmployeeException e) {
      throw new UnknownEmployeeKeyException(employeeId);
    } catch (MissingResponsibilityException e) {
      throw new NoResponsibilityException(employeeId, responsibilitytId);
    }

  }
}
