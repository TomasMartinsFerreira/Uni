package hva.app.main;

import hva.app.exception.FileOpenFailedException;
import hva.core.Hotel;
import hva.core.HotelManager;
import hva.core.exception.UnavailableFileException;
import pt.tecnico.uilib.forms.Form;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Command to open a file.
 */
class DoOpenFile extends Command<HotelManager> {
  DoOpenFile(HotelManager receiver) {
    super(Label.OPEN_FILE, receiver);
  }

  @Override
  protected final void execute() throws CommandException {
    try{
      Hotel hotel = _receiver.getHotel();
      if (hotel.hasChanged())
        if (Form.confirm(Prompt.saveBeforeExit()))
          new DoSaveFile(_receiver).execute();
        
      _receiver.load(Form.requestString(Prompt.openFile()));
    } catch (UnavailableFileException efe){
      throw new FileOpenFailedException(efe);
    }
  }
}
