package vchat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

public class Sender extends Thread implements Runnable {
  public boolean threadSuspended;
  
  // Video
  VideoStream video;             // VideoStream object used to access video frames
  static int FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
  byte[] buf;                    // Buffer used to store the images to send to the client
  static String VideoFileName;   // Video file requested from the client

  // Spread
  private String name;           // The name of the group to which we will multicast
  private SpreadConnection connection;

  // Logging
  FileOutputStream fStream;      // File stream for logging to a file
  int seqnum;                    // Sequence number to append to next message

  /*
   * Constructor
   */
  public Sender(String userName, String address, int port, String lFile) {
    name = userName;
    seqnum = 0;

    try {
      fStream = new FileOutputStream(lFile, true);
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    }

    // Allocate memory for the sending buffer
    buf = new byte[15000];

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

  /*
   * Sends a frame
   */
  private void sendFrame() {
    try {
      // Get next frame to send from the video.
      seqnum++;
      video.getNextFrame(buf);
      
      // Wrap it in a serialized object.
      VideoMsg msgData = new VideoMsg(buf, seqnum);
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
      objectStream.writeObject(msgData);
      objectStream.close();
      byte[] sendData = byteStream.toByteArray();

      // Insert the serialized object into a Spread message.
      SpreadMessage msg = new SpreadMessage();
      msg.setUnreliable();
      msg.addGroup(name);
      msg.setData(sendData);

      // Send the message and log it.
      String logMsg = "Sending frame " + seqnum + "\n";
      System.out.println(logMsg);
      fStream.write(logMsg.getBytes());
      connection.multicast(msg);
    } catch (Exception ex) {
      System.out.println("Exception caught: " + ex);
      System.exit(0);
    }
  }
  
  public String getGroupName() {
    return name;
  }

  public void run() {
    while (true) {
      sendFrame();
      try {
        Thread.sleep(FRAME_PERIOD);
      } catch (InterruptedException e) {
        e.printStackTrace();
        System.exit(1);
      }
      if (threadSuspended) {
        synchronized (this) {
          String logMsg = "\n+++END OF TEST+++\n" +
                          "Sent a total of " + seqnum + " messages.\n";
          try {
            fStream.write(logMsg.getBytes());
            fStream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
          System.exit(0);
        }
      }
    }
  }
}
