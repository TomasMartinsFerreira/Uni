package hva.app.employee;

import hva.core.Hotel;
import pt.tecnico.uilib.menus.Command;

/**
 * Show all employees of this zoo hotel.
 **/
class DoShowAllEmployees extends Command<Hotel> {

  DoShowAllEmployees(Hotel receiver) {
    super(Label.SHOW_ALL_EMPLOYEES, receiver);
  }
  
  @Override
  protected void execute() {

    String[] sortedEmployees = _receiver.get_employees(); 

    for (String employee : sortedEmployees) {
      _display.addLine(employee); 
    }
    _display.display(); 
  }  
}
