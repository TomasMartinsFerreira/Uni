package hva.app.employee;

import hva.app.exception.DuplicateEmployeeKeyException;
import hva.core.Hotel;
import hva.core.exception.DuplicateEmployeeException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Adds a new employee to this zoo hotel.
 **/
class DoRegisterEmployee extends Command<Hotel> {
  final String[] options = {"VET", "TRT"};

  DoRegisterEmployee(Hotel receiver) {
    super(Label.REGISTER_EMPLOYEE, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    Form form =  new Form();
    form.addStringField("employeeId", Prompt.employeeKey());
    form.addStringField("name", Prompt.employeeName());
    form.addOptionField("employeeType",Prompt.employeeType(), options);
    form.parse();
    String employeeType = form.optionField("employeeType");
    String name = form.stringField("name");
    String employeeId = form.stringField("employeeId");
    try {
      switch(employeeType){
        case "VET" -> _receiver.registerVeterinary(employeeId, name);
        case "TRT" -> _receiver.registerKeeper(employeeId, name);
      }
    } catch (DuplicateEmployeeException e) {
      throw new DuplicateEmployeeKeyException(employeeId);
    }
  }
}
