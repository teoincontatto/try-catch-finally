package teoincontatto;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;

public class TryCatchFinally {

  public static class Simple {
    
    /**
     * How exception works.
     */
    public static void main(String[] args) throws CustomException {
      try {
        run();
      } catch (SQLException | IOException ex) {
        throw new RuntimeException(ex); // Have to wrap
      } catch (CustomException ex) {
        throw ex; // Can be thrown as is!
      } finally {
        //Stuff that have to be done even when exception occur
      }
    }
    
    /**
     * Don't have fear! Someone will write the noisy try-catch block to handle problems! :P
     */
    public static void run() throws IOException, SQLException, CustomException {
      // Forget the problems and live happily ＼（＾○＾）人（＾○＾）／
    }
    
  }
  
  public static class WithResources {
    
    /**
     * Try-catch-with-resources
     */
    public static void main(String[] args) throws CustomException {
      try (Resource resource = getResource(); // <- Will throw an Exception on close()
          Resource otherResource = getOtherResource(resource)) {
      } catch (IOException ex) {
        throw new CustomException(ex); // <- Will catch the close() exception
      } // <- Here an hidden special finally that run close() code (also caught!)
    }
    
    /**
     * Let's return some useful resources.
     */
    public static Resource getResource() throws IOException {
      return new Resource();
    }
    
    /**
     * Let's return some useful resources.
     */
    public static Resource getOtherResource(Resource resource)
        throws IOException {
      return new Resource();
    }
    
  }
  
  public static class Resource implements Closeable {
    @Override
    public void close() throws IOException {
      throw new IOException("Oops");
    }
  }
  
  public static class CustomException extends Exception {  // Create your own
    private static final long serialVersionUID = 1960302944660375508L;
    
    public CustomException(String message) {
      super(message);
    }

    public CustomException(Throwable cause) {
      super(cause);
    }
  }
  
  public static class OtherCustomException extends Exception {  // Create your own
    private static final long serialVersionUID = 1960302944660375508L;
    
    public OtherCustomException(String message) {
      super(message);
    }
  }
}
