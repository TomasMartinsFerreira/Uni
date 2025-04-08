package hva.core;

import hva.core.exception.*;
import java.io.*;

/**
 * Class representing the manager of this application. It manages the current
 * zoo hotel.
 **/
public class HotelManager {
  /** The current zoo hotel */ // Should we initialize this field?
  private Hotel _hotel;
  private String _filename;
  private Season _season;

  public HotelManager(){
    _season = Season.SPRING;
    _hotel = new Hotel(_season);
    _filename = null;
  }
  
  /**
   * Saves the serialized application's state into the file associated to the current network.
   *
   * @throws FileNotFoundException if for some reason the file cannot be created or opened. 
   * @throws MissingFileAssociationException if the current network does not have a file.
   * @throws IOException if there is some error while serializing the state of the network to disk.
   **/
  public void save() throws FileNotFoundException, MissingFileAssociationException, IOException {
    ObjectOutputStream obOut = null;
    try {
      FileOutputStream fOut = new FileOutputStream(_filename);
      obOut = new ObjectOutputStream(fOut);
      _hotel.noChange();
      obOut.writeObject(_hotel);
    } finally{
      if (obOut != null)
        obOut.close();
    }
  }
  
  /**
   * Saves the serialized application's state into the specified file. The current network is
   * associated to this file.
   *
   * @param filename the name of the file.
   * @throws FileNotFoundException if for some reason the file cannot be created or opened.
   * @throws MissingFileAssociationException if the current network does not have a file.
   * @throws IOException if there is some error while serializing the state of the network to disk.
   **/
  public void saveAs(String filename) throws FileNotFoundException, MissingFileAssociationException, IOException {
    _filename = filename;
    save();
  }
  
  /**
   * @param filename name of the file containing the serialized application's state
   *        to load.
   * @throws UnavailableFileException if the specified file does not exist or there is
   *         an error while processing this file.
   **/
  public void load(String filename) throws UnavailableFileException {
    ObjectInputStream objIn = null;
    try {
      objIn = new ObjectInputStream(new FileInputStream(filename));
      _hotel = (Hotel) objIn.readObject();
    } catch (IOException e) {
        throw new UnavailableFileException(" to load file: " + filename);
    } catch (ClassNotFoundException e){
        throw new UnavailableFileException("Hotel class not found: " + e.getMessage());
    }
    finally {
      if (objIn != null)
        try{
          objIn.close();
        } catch(IOException e){
          throw new UnavailableFileException("Failed to close file stream: " + filename);
        }
    } 
  }
  
  /**
   * Read text input file and initializes the current zoo hotel (which should be empty)
   * with the domain entitiesi representeed in the import file.
   *
   * @param filename name of the text input file
   * @throws ImportFileException if some error happens during the processing of the
   * import file.
   **/
  public void importFile(String filename) throws ImportFileException {
    try {
      _hotel.importFile(filename);
    } catch (IOException | UnrecognizedEntryException e) {
      throw new ImportFileException(filename, e);
    }
  }

  public int advanceSeason(){
    _season = _season.advanceSeason();
    return _hotel.advanceSeason();
  }

  public String get_file(){
    return _filename;
  }
  
  /**
   * Returns the zoo hotel managed by this instance.
   *
   * @return the current zoo hotel
   **/
  public final Hotel getHotel() {
    return _hotel;
  }
  public void resetHotel() {
    _hotel = new Hotel(_season);
    _filename = null;
  }
  public int get_GlobalSatifaction() {
    return _hotel.get_GlobalSatifaction();
  }
}
