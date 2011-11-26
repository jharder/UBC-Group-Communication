package vchat;

/*Sender.java*/

import java.net.InetAddress;
import java.net.UnknownHostException;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

public class Sender extends Thread implements Runnable {
  // Video variables:
  // ----------------
  VideoStream video; // VideoStream object used to access video frames
  static int FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
  byte[] buf; // buffer used to store the images to send to the client
  static String VideoFileName; // video file requested from the client
  int seqnum;

  // Spread variables:
  // -----------------
  private String name;
  private SpreadConnection connection;

  // --------------------------------
  // Constructor
  // --------------------------------
  public Sender(String userName, String address, int port) {
    name = userName;

    // Establish the spread connection.
    // --------------------------------
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

    // allocate memory for the sending buffer
    buf = new byte[15000];

    seqnum = 0;
  }

  // --------------------------------
  // Sends a frame
  // --------------------------------
  private void sendFrame() {
    try {
      SpreadMessage msg = new SpreadMessage();
      msg.setUnreliable();
      msg.addGroup(name);

      // get next frame to send from the video
      seqnum++;
      video.getNextFrame(buf);

      VideoMsg msgData = new VideoMsg(buf, seqnum);
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
      objectStream.writeObject(msgData);
      objectStream.close();
      byte[] sendData = byteStream.toByteArray();
      msg.setData(sendData);

      // Send the message.
      System.out.println("Sending frame " + seqnum);
      connection.multicast(msg);
    } catch (Exception ex) {
      System.out.println("Exception caught: " + ex);
      System.exit(0);
    }
  }

  // ------------------------------------
  // main
  // ------------------------------------
  public void run() {
    while (true) {
      sendFrame();
      try {
        Thread.sleep(FRAME_PERIOD);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.exit(1);
      }
    }
  }
}
