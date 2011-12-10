package vchat;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import spread.SpreadGroup;
import spread.SpreadMessage;

public class Util {
  public static final int PROBLEM     = -1;
  public static final int VIDEO_FRAME = 0;
  public static final int PING_MSG    = 1;
  public static final int TERM_MSG    = 2;
  public static final int IRREG_MSG   = 3;

  // Thrashing
  public static final long THRASH_PERIOD_NS = 1000000000;
  public static final int NUM_THRASHERS     = 10;
  public static final int LEAVE_LENGTH_MS   = 100;

  public static void printMsg(SpreadMessage msg, String msgType, boolean printData) {
    System.out.print("Received a ");
    if (msg.isUnreliable())
      System.out.print("UNRELIABLE");
    else if (msg.isReliable())
      System.out.print("RELIABLE");
    else if (msg.isFifo())
      System.out.print("FIFO");
    else if (msg.isCausal())
      System.out.print("CAUSAL");
    else if (msg.isAgreed())
      System.out.print("AGREED");
    else if (msg.isSafe())
      System.out.print("SAFE");
    System.out.println(msgType + " message.");

    System.out.println("Sent by  " + msg.getSender() + ".");

    System.out.println("Type is " + msg.getType() + ".");

    if (msg.getEndianMismatch() == true)
      System.out.println("There is an endian mismatch.");
    else
      System.out.println("There is no endian mismatch.");

    SpreadGroup groups[] = msg.getGroups();
    System.out.println("To " + groups.length + " groups.");

    byte data[] = msg.getData();
    System.out.println("The data is " + data.length + " bytes.");

    if (printData) {
      System.out.println("The message is: " + new String(data));
    }
  }
  
  public static SpreadMessage getTermMsg(String gName) {
    // Create a termination message and wrap it in a serialized object.
    VideoMsg msgData = new VideoMsg(Util.TERM_MSG);
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    ObjectOutputStream objectStream;
    try {
      objectStream = new ObjectOutputStream(byteStream);
      objectStream.writeObject(msgData);
      objectStream.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    byte[] sendData = byteStream.toByteArray();

    // Insert the serialized object into a Spread message.
    SpreadMessage msg = new SpreadMessage();
    msg.setSafe();
    msg.addGroup(gName);
    // TODO: Use setObject()
    msg.setData(sendData);
    
    return msg;
  }
  
  /**
   * Logs a <code>msg</code> to the provided file stream and prints it to the console.
   * @param stream
   * @param msg
   * @return true on success, false on failure
   */
  public static boolean log(FileOutputStream stream, String msg) {
    return log(stream, msg, true);
  }
  
  /**
   * Logs <code>msg</code> to the provided file stream. Optionally prints it to
   * the console (depending on the value of noPrint).
   * @param stream
   * @param msg
   * @param noPrint Set to true to cancel printing to the console.
   * @return true on success, false on failure
   */
  public static boolean log(FileOutputStream stream, String msg, boolean noPrint) {
    if (noPrint) {
      System.out.print(msg);
    }

    try {
      stream.write(msg.getBytes());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    
    return true;
  }
  
  public static void closeLog(FileOutputStream stream) {
    try {
      stream.flush();
      stream.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
