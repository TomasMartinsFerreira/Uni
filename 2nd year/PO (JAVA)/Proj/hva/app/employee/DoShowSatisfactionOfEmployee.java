package hva.app.employee;

import hva.app.exception.UnknownEmployeeKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingEmployeeException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Show the satisfaction of a given employee.
 **/
class DoShowSatisfactionOfEmployee extends Command<Hotel> {

  DoShowSatisfactionOfEmployee(Hotel receiver) {
    super(Label.SHOW_SATISFACTION_OF_EMPLOYEE, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    
    String employeeId = Form.requestString(Prompt.employeeKey());
    try {
      String satisfaction = String.valueOf(_receiver.getEmployeeSatifaction(employeeId));
      _display.addLine(satisfaction);
      _display.display();
    } catch (MissingEmployeeException e) {
      throw new UnknownEmployeeKeyException(employeeId);
    }
  }
}
