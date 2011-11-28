package vchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;
import sun.awt.PeerEvent;

public class Client extends Thread implements Runnable {
  static String VideoFileName; // video file requested from the client
  private String logFile;
  private String name; // Spread private group name (user name)
  private static int numRcvrs = 0;
  private ArrayList<String> groups;
  private String dAddr;
  private int dPort;
  private SpreadConnection connection;
  private SpreadGroup group;
  protected HashMap<String, Receiver> receivers;
//  private ArrayList<Receiver> rcvrs;
  private static Sender s;

  public Client(String user, String address, int port, ArrayList<String> g,
      String lFile) {
    name = user;
    groups = g;
    dAddr = address;
    dPort = port;
    receivers = new HashMap<String, Receiver>();
    logFile = lFile;

    // Establish the spread connection.
    // --------------------------------
    try {
      connection = new SpreadConnection();
      connection
          .connect(InetAddress.getByName(dAddr), dPort, name, false, true);
    } catch (SpreadException e) {
      System.err.println("There was an error connecting to the daemon.");
      e.printStackTrace();
      System.exit(1);
    } catch (UnknownHostException e) {
      System.err.println("Can't find the daemon " + address);
      System.exit(1);
    }

    for(String gName : groups) {
      group = new SpreadGroup();
      try {
        group.join(connection, gName);
      } catch (SpreadException e1) {
        e1.printStackTrace();
      }
    }
  }

  /*
   * Makes adjustments to our knowledge of the group according to membership
   * messages.
   */
  private void handleMembership(MembershipInfo info) {
    SpreadGroup group = info.getGroup();
    if (info.isRegularMembership()) {
      SpreadGroup members[] = info.getMembers();
      MembershipInfo.VirtualSynchronySet virtual_synchrony_sets[] = info
          .getVirtualSynchronySets();
      MembershipInfo.VirtualSynchronySet my_virtual_synchrony_set = info
          .getMyVirtualSynchronySet();

      System.out.println("REGULAR membership for group " + group + " with "
          + members.length + " members:");
      for (int i = 0; i < members.length; ++i) {
        System.out.println("\t\t" + members[i]);
      }
      System.out.println("Group ID is " + info.getGroupID());

      System.out.print("\tDue to ");
      if (info.isCausedByJoin()) {
        System.out.println("the JOIN of " + info.getJoined());
      } else if (info.isCausedByLeave()) {
        System.out.println("the LEAVE of " + info.getLeft());
      } else if (info.isCausedByDisconnect()) {
        System.out.println("the DISCONNECT of " + info.getDisconnected());

        // Tear down the receiver we had for the peer that has left.
        Receiver r = (Receiver) receivers.get(info.getLeft().toString());
        if (r.threadSuspended) {
          synchronized (r) {
            r.notify();
            r.threadSuspended = false;
          }
        }
        receivers.remove(info.getLeft().toString());
      } else if (info.isCausedByNetwork()) {
        System.out.println("NETWORK change");
        for (int i = 0; i < virtual_synchrony_sets.length; ++i) {
          MembershipInfo.VirtualSynchronySet set = virtual_synchrony_sets[i];
          SpreadGroup setMembers[] = set.getMembers();
          System.out.print("\t\t");
          if (set == my_virtual_synchrony_set) {
            System.out.print("(LOCAL) ");
          } else {
            System.out.print("(OTHER) ");
          }
          System.out.println("Virtual Synchrony Set " + i + " has "
              + set.getSize() + " members:");
          for (int j = 0; j < set.getSize(); ++j) {
            System.out.println("\t\t\t" + setMembers[j]);
          }
        }
      }
    } else if (info.isTransition()) {
      System.out.println("TRANSITIONAL membership for group " + group);
    } else if (info.isSelfLeave()) {
      System.out.println("SELF-LEAVE message for group " + group);
    }
  }

  private void handleMessage(SpreadMessage msg) {
    try {
      if (msg.isRegular()) {
//        // Create a new Receiver object for this new peer.
//        String pName = msg.getSender().toString();
//        String rName = name + "_r_" + numRcvrs;
//        if (!peers.containsKey(pName) && !rcvrs.contains(rName)) {
//          Receiver newrcvr = new Receiver(rName, dAddr, dPort, groupName,
//              logFile);
//          peers.put(pName, newrcvr);
//          rcvrs.add(rName);
//          newrcvr.start();
//          numRcvrs++;
//        } else {
//          // Discard the message and let the Receiver object take care of
//          // it
//        }
      } else if (msg.isMembership()) {
        System.out
            .println("*****************CLIENT Received Message************");
        MembershipInfo info = msg.getMembershipInfo();
        handleMembership(info);
      } else if (msg.isReject()) {
        // Received a Reject message
        System.out
            .println("*****************CLIENT Received Message************");
        printMsg(msg, "Reject", false);
      } else {
        System.out.println("Message is of unknown type: "
            + msg.getServiceType());
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void printMsg(SpreadMessage msg, String msgType, boolean printData) {
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
    System.out.println(" REJECTED message.");

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

  /*
   * Monitors for users joining or leaving, and creates and tears down receiver
   * threads accordingly
   */
  public void run(){
    BufferedReader inRdr = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      try {
        if (inRdr.ready()) {
          if ((char)inRdr.read() == 's') {
            // User wants to stop a receiver.
            
            // Print menu.
//            boolean gotChoice = false;
//            Integer choice = Integer.getInteger(inRdr.readLine());
//            while(!gotChoice) {
//              System.out.println("Receiver threads running:");
//              int i = 0;
//              for(String rName : receivers.keySet()) {
//                i++;
//                System.out.println(i + ". " + rName);
//              }
//              System.out.println("Stop which thread? (0 to cancel)");
//              
//              // Get response.
//              if (choice == null || choice < 0 || choice > i) {
//                System.out.println("Enter a number.");
//              } else {
//                gotChoice = true;
//              }
//            }
            
            // Stop all receivers.
            for(Receiver r : receivers.values()) {
              r.threadSuspended = true;
            }
          }
        }
        handleMessage(connection.receive());
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }

  // ------------------------------------
  // main
  // ------------------------------------
  public static void main(String argv[]) throws Exception {
    // Set default values.
    String userName = new String("u1");
    String address = null;
    int port = 0;
    ArrayList<String> groupsToJoin = new ArrayList<String>();
    String lFileBase = "vchat_log";

    boolean gotVideo = false;

    // Check the args.
    for (int i = 0; i < argv.length; i++) {
      // Check for user name.
      if ((argv[i].compareTo("-u") == 0) && (argv.length > (i + 1))) {
        // Set user name.
        i++;
        userName = argv[i];
      }
      // Check for server.
      else if ((argv[i].compareTo("-da") == 0) && (argv.length > (i + 1))) {
        // Set the server.
        i++;
        address = argv[i];
      }
      // Check for port.
      else if ((argv[i].compareTo("-dp") == 0) && (argv.length > (i + 1))) {
        // Set the port.
        i++;
        port = Integer.parseInt(argv[i]);
      }
      // Check for group to join.
      else if ((argv[i].compareTo("-r") == 0) && (argv.length > (i + 1))) {
        // Set the group.
        i++;
        groupsToJoin.add(argv[i]);
      }
      // Check for log file name.
      else if ((argv[i].compareTo("-l") == 0) && (argv.length > (i + 1))) {
        // Set the log file.
        i++;
        lFileBase = argv[i];
      }
      // Check for video file name.
      else if ((argv[i].compareTo("-s") == 0) && (argv.length > (i + 1))) {
        // Set the file name.
        i++;
        VideoFileName = argv[i];
        gotVideo = true;
      } else {
        printUsage();
        System.exit(0);
      }
    }

    Client monitor = new Client(userName + "_m", address, port, groupsToJoin,
        lFileBase);
    monitor.start();

    if (gotVideo) {
      // Create a Sender object.
      s = new Sender(userName, address, port, lFileBase + "_s");
      s.video = new VideoStream(VideoFileName);
      s.start();
    }
    if (!groupsToJoin.isEmpty()) {
      for(String g : groupsToJoin) {
        // Create a Receiver object.
        String rName = userName + "_R" + g;
        Receiver r = new Receiver(rName, address, port, g, lFileBase + "_r");
        monitor.receivers.put(rName, r);
        r.start();
        numRcvrs++;
      }  
    }
  }

  // TODO: Update this
  private static void printUsage() {
    System.out.print("Usage: Client\n"
        + "\t[-u <user name>]   : unique user name\n"
        + "\t[-s <address>]     : the name or IP for the daemon\n"
        + "\t[-p <port>]        : the port for the daemon\n"
        + "\t[-g <group name>]    : the group to join\n"
        + "\t-v <video file name>  : the video file to multicast\n");
  }
}