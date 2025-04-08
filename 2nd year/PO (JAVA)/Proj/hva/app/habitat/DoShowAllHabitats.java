package hva.app.habitat;

import hva.core.Hotel;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;


/**
 * Show all habitats of this zoo hotel.
 **/
class DoShowAllHabitats extends Command<Hotel> {
  DoShowAllHabitats(Hotel receiver) {
    super(Label.SHOW_ALL_HABITATS, receiver);
  }
  
  @Override
  protected void execute() throws CommandException  {
    String[] habitats = _receiver.get_habitats();
    for (String habitat : habitats) {
      _display.addLine(habitat);
    }      
    _display.display(); 
  }  
}
