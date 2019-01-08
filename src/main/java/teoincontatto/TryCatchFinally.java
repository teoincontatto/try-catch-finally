package teoincontatto;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

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
  
  public static class WrappedException {
    
    /**
     * Try-catch-and-wrap
     */
    public static void main(String[] args) throws CustomException {
      try {
        throw new RuntimeException(new IOException());
      } catch (RuntimeException ex) {
        if (ex.getCause() instanceof IOException) {
          throw new CustomException(ex.getCause());
        }
        
        throw ex;
      }
    }
  }
  
  public static class CatchThrowable {
    /**
     * Try-to-catch-a-throwable
     */
    public static void main(String[] args) throws CustomException {
      try {
        infiniteRecursion();
      } catch (Throwable ex) {
        if (ex instanceof StackOverflowError) {
          System.out.println("Who say I can not recover from a stack overflow");
          infiniteRecursion();
        }
        
        throw ex;
      }
    }
    
    public static void infiniteRecursion() {
      infiniteRecursion();
    }
  }
  
  public static class HandleInterruptedException {
    /**
     * Try-to-catch-a-throwable
     */
    public static void main(String[] args) {
      while (true) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex); 
        }
      }
    }
  }
  
  public static class LambdaAndException {
    /**
     * Try-catch-finally-in-a-lambda
    public static void main(String[] args) {
      try {
        CompletableFuture.runAsync(() -> {
          throw new Exception();
        }).join();
      } catch (Exception ex) {
        throw new RuntimeException(ex); 
      }
    }
     */
    public static void main(String[] args) {
      try {
        CompletableFuture.runAsync(Unchecked.runnable(() -> {
          throw new Exception();
        })).join();
      } catch (Exception ex) {
        throw new RuntimeException(ex); 
      }
    }
  }
  
  public static class SneakyException {
    /**
     * Try-catch-of-sneaky-exception
     */
    public static void main(String[] args) {
      throwSneaky(new Exception());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwSneaky(Throwable throwable) throws E {
      throw (E) throwable;
    }
  }
  
  public static class DontCatchOutOfMemoryError {
    /**
     * Do-not-catch-OOM
     */
    public static void main(String[] args) {
      byte[] hope = new byte[1_000_000];
      for (int i = 0; i < Integer.MAX_VALUE; i += 100_000_000) {
        try {
          byte[] buffer = new byte[i];
          System.out.println("Allocated " + buffer.length + " bytes");
        } catch(OutOfMemoryError ex) {
          System.out.println("Freeing " + hope.length + " bytes and try to recover!");
          hope = null;
          Runtime.getRuntime().gc();
          hope = new byte[1_000_000];
        }
      }
    }
  }
  
  public static class HowHandleOutOfMemoryError {
    /**
     * How-handle-OOM
     */
    public static void main(String[] args) {
      for (int i = 0; i< Integer.MAX_VALUE; i++) {
        System.out.println(fibonacci(tupleOf(Integer.valueOf(i))).join());
      }
    }
    
    private static CompletableFuture<Tuple2<byte[], Integer>> fibonacci(Tuple2<byte[], Integer> n) {
      if (n.v2.equals(ZERO)) {
        return CompletableFuture.completedFuture(Tuple.tuple(new byte[0], ZERO));
      }
      if (n.v2.equals(ONE)) {
        return CompletableFuture.completedFuture(Tuple.tuple(new byte[1], ONE));
      }
      
      CompletableFuture<Tuple2<byte[], Integer>> fibonacciN2 = CompletableFuture
          .supplyAsync(() -> tupleOf(subtract(n.v2, TWO)), EXECUTOR)
          .thenComposeAsync(n2 -> fibonacci(n2), EXECUTOR);
      
      return CompletableFuture.supplyAsync(() -> tupleOf(subtract(n.v2, ONE)), EXECUTOR)
          .thenComposeAsync(n1 -> fibonacci(n1), EXECUTOR)
          .thenComposeAsync(n1 -> fibonacciN2
              .thenApplyAsync(n2 -> tupleOf(add(n1.v2, n2.v2)), EXECUTOR));
    }
    
    private static ExecutorService EXECUTOR = new ForkJoinPool(
        100, 
        ForkJoinPool.defaultForkJoinWorkerThreadFactory, 
        null, 
        false);
    
    private static Integer ZERO = Integer.valueOf(0);
    private static Integer ONE = Integer.valueOf(1);
    private static Integer TWO = Integer.valueOf(2);
    
    private static Tuple2<byte[], Integer> tupleOf(Integer n) {
      byte[] buffer = new byte[0];
      for (int i = 10; i < n; i+=10) {
        buffer = new byte[i * 1_000_000];
        Unchecked.runnable(() -> Thread.sleep(100)).run();
        System.out.println(Thread.currentThread().getId() + " allocated " 
            + (buffer.length / 1_000_000) + " mega bytes");
      }
      
      return Tuple.tuple(buffer, n);
    }
    
    private static Integer add(Integer n1, Integer n2) {
      return n1 + n2;
    }
    
    private static Integer subtract(Integer n1, Integer n2) {
      return n1 - n2;
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
