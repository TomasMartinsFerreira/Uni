package hva.app.main;

import hva.core.HotelManager;
import hva.core.exception.MissingFileAssociationException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
//import pt.tecnico.uilib.Display;

//import java.io.FileNotFoundException;
import java.io.IOException;
//import java.util.*;

/**
 * Save to file under current name (if unnamed, query for name).
 */
class DoSaveFile extends Command<HotelManager> {
  DoSaveFile(HotelManager receiver) {
    super(Label.SAVE_FILE, receiver, r -> r.getHotel() != null);
  }

  @Override
  protected final void execute() {
    try{
      if (_receiver.get_file() == null)
        _receiver.saveAs(Form.requestString(Prompt.newSaveAs()));
      else
        _receiver.save();
    } catch (MissingFileAssociationException | IOException e){
    }
  }
}
