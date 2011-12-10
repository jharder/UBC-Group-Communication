/* Receiver.java
 *
 * Contains source from the Spread toolkit, distributed under the following licence:
 * 
 * Copyright (c) 1993-2006 Spread Concepts LLC. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer and request.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer and request in the documentation and/or other materials provided with the distribution.
 * 
 * 3. All advertising materials (including web pages) mentioning features or use of this software, or software that uses this software, must display the following acknowledgment: "This product uses software developed by Spread Concepts LLC for use in the Spread toolkit. For more information about Spread see http://www.spread.org"
 * 
 * 4. The names "Spread" or "Spread toolkit" must not be used to endorse or promote products derived from this software without prior written permission.
 * 
 * 5. Redistributions of any form whatsoever must retain the following acknowledgment:
 * "This product uses software developed by Spread Concepts LLC for use in the Spread toolkit. For more information about Spread, see http://www.spread.org"
 * 
 * 6. This license shall be governed by and construed and enforced in accordance with the laws of the State of Maryland, without reference to its conflicts of law provisions. The exclusive jurisdiction and venue for all legal actions relating to this license shall be in courts of competent subject matter jurisdiction located in the State of Maryland.
 * 
 * TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, SPREAD IS PROVIDED UNDER THIS LICENSE ON AN AS IS BASIS, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES THAT SPREAD IS FREE OF DEFECTS, MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE OR NON-INFRINGING. ALL WARRANTIES ARE DISCLAIMED AND THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE CODE IS WITH YOU. SHOULD ANY CODE PROVE DEFECTIVE IN ANY RESPECT, YOU (NOT THE COPYRIGHT HOLDER OR ANY OTHER CONTRIBUTOR) ASSUME THE COST OF ANY NECESSARY SERVICING, REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. NO USE OF ANY CODE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * 
 * TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE COPYRIGHT HOLDER OR ANY OTHER CONTRIBUTOR BE LIABLE FOR ANY SPECIAL, INCIDENTAL, INDIRECT, OR CONSEQUENTIAL DAMAGES FOR LOSS OF PROFITS, REVENUE, OR FOR LOSS OF INFORMATION OR ANY OTHER LOSS.
 * 
 * YOU EXPRESSLY AGREE TO FOREVER INDEMNIFY, DEFEND AND HOLD HARMLESS THE COPYRIGHT HOLDERS AND CONTRIBUTORS OF SPREAD AGAINST ALL CLAIMS, DEMANDS, SUITS OR OTHER ACTIONS ARISING DIRECTLY OR INDIRECTLY FROM YOUR ACCEPTANCE AND USE OF SPREAD.
 * 
 * Although NOT REQUIRED, we at Spread Concepts would appreciate it if active users of Spread put a link on their web site to Spread's web site when possible. We also encourage users to let us know who they are, how they are using Spread, and any comments they have through either e-mail (spread@spread.org) or our web site at (http://www.spread.org/comments).
 * 
 */

/* ------------------
 Receiver
 usage: java Receiver ....
 ---------------------- */
package vchat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.zip.CRC32;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

public class Receiver extends Thread implements Runnable {
  private static int numThrashers = 0;

  public boolean threadSuspended;

  // Spread
  private SpreadConnection connection;
  SpreadGroup group;        // Name of the group to join (sender we want to listen to)

  // Testing
  boolean thrash;           // If true, leave and join the group regularly 
  int thrashID;             // Identifies the instance for timing
  
  // Logging
  FileOutputStream lStream; // File stream for logging to a file 
  long numMsgs = 0;          // Tally of number of messages received
  int lastSeq  = 0;
  int bytesIn  = 0;          // Tally of number of bytes received
  long tStart  = -1;         // Time of first message received

  /*
   * Constructor
   */
  public Receiver(String user, String address, int port, String groupToJoin,
      String lFile, boolean tMode) {
    thrash = tMode;
    if (tMode) {
      thrashID = numThrashers++;
    }
    
    // Prepare for logging
    try {
      lStream = new FileOutputStream(lFile, false);
      String logMsg = "*****************************************************\n"
                    + "+++ RECEIVER INSTANTIATED +++\n";
      Util.log(lStream, logMsg);
    } catch (Exception e2) {
      e2.printStackTrace();
    }
    
    // Establish the spread connection.
    try {
      connection = new SpreadConnection();
      connection.connect(InetAddress.getByName(address), port, user, false,
          true);
    } catch (SpreadException e) {
      System.err.println("There was an error connecting to the daemon.");
      e.printStackTrace();
      System.exit(1);
    } catch (UnknownHostException e) {
      System.err.println("Can't find the daemon " + address);
      System.exit(1);
    }

    // Join the group to which the intended sender is multicasting. 
    group = new SpreadGroup();
    try {
      group.join(connection, groupToJoin);
    } catch (SpreadException e1) {
      e1.printStackTrace();
    }
  }

  /*
   * Sends a pong.
   */
  private void sendPong(SpreadGroup sender) {
      SpreadMessage msg = new SpreadMessage();
      msg.setSafe();
      msg.addGroup(sender);

      // Send the message and log it.
      Util.log(lStream, "Sending pong.\n");
      try {
        connection.multicast(msg);
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

  /*
   * Handles regular (non-membership) messages.
   */
  private int handleMessage(SpreadMessage msg) {
    try {
      if (msg.isRegular()) {
//        Util.printMsg(msg, "Regular", false);
        // Extract data from the message.
        // TODO: Use getObject()?
        byte spreadPayload[] = msg.getData();
        ByteArrayInputStream byteStream = new ByteArrayInputStream(spreadPayload);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        VideoMsg vMsg = (VideoMsg) objectStream.readObject();
        objectStream.close();
        
        // Check for a ping.
        if (vMsg.type == Util.PING_MSG) {
          if (!thrash) {
            // Respond only if we are not riff-raff.
            sendPong(msg.getSender());
          }
          return Util.PING_MSG;
        }
        if (vMsg.type == Util.TERM_MSG) {
          return Util.TERM_MSG;
        }
        
        // If we got here, it must be a video frame. Log receipt of the message.
        if (tStart < 0) {
          tStart = System.nanoTime();
        }
        numMsgs ++;
        bytesIn += spreadPayload.length;
        int seq = vMsg.seqnum;
        Util.log(lStream, "Received frame from " + msg.getSender() + " seq. #," + seq + ", size ," + spreadPayload.length + ", bytes\n");
        
        // Check for and log an out-of-order message. 
        if(lastSeq > seq) {
          String logMsg = "*** OUT-OF-ORDER ***\n"
                        + "   Message # " + seq + " came after " + lastSeq + "\n";
          Util.log(lStream, logMsg);
        }
        
        // Check for and log message corruption.
        CRC32 checksum = new CRC32();
        checksum.update(vMsg.payload);
        if (checksum.getValue() != vMsg.checksum) {
          String logMsg = "*** CORRUPT ***\n" 
                 + "   Message #" + seq + " is not the same as when it was sent.\n";
          Util.log(lStream, logMsg);
        }
        return Util.VIDEO_FRAME;
      }
      return Util.IRREG_MSG;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return Util.PROBLEM;
  }

  private void leaveJoin() throws Exception {
    String gName = group.toString();
    group.leave();
    Thread.sleep(Util.LEAVE_LENGTH_MS);
    group.join(connection, gName);
  }
  
  private void end() {
    Util.closeLog(lStream);
    System.exit(0);
  }
  
  public void run() {
    boolean stop = false;
    // Start receivers thrashing in a staggered fashion.
    long tJoined = System.nanoTime() + thrashID * (Util.THRASH_PERIOD_NS / Util.NUM_THRASHERS);
    
    while (!stop) {
      try {
        // Receive a message if there is one.
        if (connection.poll()) {
          switch (handleMessage(connection.receive())) {
            case Util.TERM_MSG: {
              long tRcv = System.nanoTime() - tStart;
              double bps = bytesIn / (double)(tRcv / 1000000000);
              String logMsg = "\n*** TEST COMPLETED ***\n"
                            + "Received a total of " + numMsgs + " messages.\n"
                            + "Received a total of " + bytesIn + " bytes.\n"
                            + "Receive rate: " + bps + " bytes per second.\n"
                            ;
              Util.log(lStream, logMsg);
              stop = true;
              break;
            }
          }
        }
        
        // Periodically leave and join the group.
        if (thrash && (System.nanoTime() - tJoined) >= Util.THRASH_PERIOD_NS) {
          leaveJoin();
          tJoined = System.nanoTime();
        }

        // Check whether the monitor wants to stop us.
        if (threadSuspended) {
          synchronized (this) {
            group.leave();
            String logMsg = "\n*** TEST INTERRUPTED ***\n"
                          + "Received a total of " + numMsgs + " messages.\n";
            Util.log(lStream, logMsg);
            end();
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    end();
  }
}
