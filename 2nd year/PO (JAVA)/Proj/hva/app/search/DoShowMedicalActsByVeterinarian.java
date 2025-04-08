package hva.app.search;

import hva.app.exception.UnknownVeterinarianKeyException;
import hva.core.Hotel;
import hva.core.exception.MissingEmployeeException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;


/**
 * Show all medical acts of a given veterinarian.
 **/
class DoShowMedicalActsByVeterinarian extends Command<Hotel> {
  DoShowMedicalActsByVeterinarian(Hotel receiver) {
    super(Label.MEDICAL_ACTS_BY_VET, receiver);
  }
  
  @Override
  protected void execute() throws CommandException {
    String veterinarianId = Form.requestString(hva.app.employee.Prompt.employeeKey());
    try{
    String[] record= _receiver.getVeterinaryVacinations(veterinarianId);
    for (String entry : record)
      _display.addLine(entry);
    // Show the display
    _display.display();
    } catch(MissingEmployeeException e){
      throw new UnknownVeterinarianKeyException(veterinarianId);
    }
  }
}
