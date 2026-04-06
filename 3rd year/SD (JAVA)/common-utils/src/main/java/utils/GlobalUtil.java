package utils;


/**
 * Debug logging logic
 * prints the provided message to the standard error stream only if the system's debug flag is enabled
*/
public class GlobalUtil {
  
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  public static void debug(String debugMessage) {
    if (DEBUG_FLAG)
      System.err.println("[DEBUG] " + debugMessage);
  }
}
