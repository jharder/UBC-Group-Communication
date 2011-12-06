package vchat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

public class Pinger extends Thread implements Runnable {
  private static final int NUM_PINGS = 50;
  private static final int NUM_TESTS = 10;
  private static final int WAIT_PERIOD = 5 * 1000;
  public boolean threadSuspended;
  
  // Spread
  private String name;           // The name of the group to which we will multicast
  private SpreadConnection connection;

  // Logging
  FileOutputStream fStream;      // File stream for logging to a file

  /*
   * Constructor
   */
  public Pinger(String userName, String address, int port, String lFile) {
    name = userName;

    try {
      fStream = new FileOutputStream(lFile, false);
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }

    // Establish the spread connection.
    try {
      connection = new SpreadConnection();
      connection.connect(InetAddress.getByName(address), port, userName, false,
          true);
    } catch (SpreadException e) {
      System.err.println("There was an error connecting to the daemon.");
      e.printStackTrace();
      System.exit(1);
    } catch (UnknownHostException e) {
      System.err.println("Can't find the daemon " + address);
      System.exit(1);
    }
  }

  /**
   * Sends pings
   * 
   * @return The average RTT time in nanoseconds over all the pings.
   */
  public long doPings(int numPings) {
    long sumTime = 0;
    long avgTime = 0;
    
    try {
      // Create a ping message
      VideoMsg pMsg = new VideoMsg(Util.PING_MSG);
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
      objectStream.writeObject(pMsg);
      objectStream.close();
      byte[] sendData = byteStream.toByteArray();

      // Create the Spread message.
      SpreadMessage msg = new SpreadMessage();
      // TODO: Use different service levels.
      msg.setUnreliable();
      msg.addGroup(name);
      msg.setData(sendData);

      Util.log(fStream, "Sending " + numPings + " pings\n");
      
      // Send the given number of ping messages.
      for(int i = 0; i < numPings; i++) {
        // Send a ping and receive it, noting the time elapsed between the events.
        long sTime = System.nanoTime();
        connection.multicast(msg);
        SpreadMessage response = connection.receive();
        long rTime = System.nanoTime();
        long pingTime = rTime - sTime;
        sumTime += pingTime;
        
        // Log the result.
        Util.log(fStream, "Reply from " + response.getSender() + " time = " + pingTime/1000 + " us.\n"); 
      }
      
      avgTime = sumTime / numPings;
    } catch (Exception ex) {
      System.err.println("Exception caught: " + ex);
      System.exit(0);
    }
    return avgTime;
  }
  
  public String getGroupName() {
    return name;
  }
  
  
  public void run() {
    long sum = 0;
    long averages[] = new long[NUM_TESTS];
    
    Util.log(fStream, "+++ STARTING PING TEST +++\n\n" +
                    "Performing " + NUM_TESTS + " tests of " + NUM_PINGS + " pings.\n");
    
    for(int i = 0; i < (NUM_TESTS + 1); i++) {
      long result = doPings(NUM_PINGS);
      double thisAvgMS = ((double) result) / 1000000; 
      Util.log(fStream, "\nTest #" + (i) + ": Averaged " + thisAvgMS + " ms.\n\n");
      if (i > 0) {
        averages[i-1] = result;
      } else {
        Util.log(fStream, "Discarding test #0 as a warm-up.\n\n");
      }

      // Check whether the monitor wants to stop us.
      if (threadSuspended) {
        synchronized (this) {
          Util.log(fStream, "\n*** TEST INTERRUPTED ***\n");
          Util.closeLog(fStream);
          System.exit(0);
        }
      }
    }
    
    // End the test.
    SpreadMessage msg = Util.getTermMsg(name);
    try {
      connection.multicast(msg);
    } catch (SpreadException e1) {
      e1.printStackTrace();
    }

    Util.log(fStream, "Sending termination message.\n");
    
    // Log results.
    String logMsg = "\n+++ END OF TEST +++\n\n";
    for(int i = 0; i < NUM_TESTS; i++) {
      long r = averages[i];
      sum += r;
      double thisAvgMS = ((double) r) / 1000000;
      logMsg += String.format("Test #" + (i + 1) + ": Averaged %.3f ms.\n", thisAvgMS);
    }
    
    double avgMS = ((double)sum) / ((double)NUM_TESTS) / 1000000;
    logMsg += String.format("Average of tests: %.3f ms.\n", avgMS);  

    Util.log(fStream, logMsg);
    Util.closeLog(fStream);
    System.exit(0);
  }
}
