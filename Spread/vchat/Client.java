package vchat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

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
        // Let the appropriate Receiver object handle the message.
      } else if (msg.isMembership()) {
        System.out
            .println("*****************CLIENT Received Message************");
        MembershipInfo info = msg.getMembershipInfo();
        handleMembership(info);
      } else if (msg.isReject()) {
        // Received a Reject message
        System.out
            .println("*****************CLIENT Received Message************");
        Util.printMsg(msg, "Reject", false);
      } else {
        System.out.println("Message is of unknown type: "
            + msg.getServiceType());
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
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
    String thrashGroup = "dummy_group";
    String lFileBase = "vchat_log";

    boolean gotVideo   = false;
    boolean pingMode   = false;
    boolean thrashMode = false;

    // Check the args.
    if (argv.length == 0){
      printUsage();
      System.exit(0);
    }
    
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
      // Check for ping mode.
      else if ((argv[i].compareTo("-p") == 0)/* && (argv.length > (i + 1))*/) {
        // Set ping mode.
        pingMode = true;
      }
      // Check for thrash mode.
      else if ((argv[i].compareTo("-t") == 0)/* && (argv.length > (i + 1))*/) {
        // Set thrash mode.
        i++;
        thrashMode = true;
        thrashGroup = argv[i];
      }
      // Check for group to join as a receiver.
      else if ((argv[i].compareTo("-r") == 0) && (argv.length > (i + 1))) {
        // Set the group.
        i++;
        groupsToJoin.add(argv[i]);
      }
      // Check for video file name. 
      else if ((argv[i].compareTo("-s") == 0) && (argv.length > (i + 1))) {
        // Set the file name.
        i++;
        VideoFileName = argv[i];
        gotVideo = true;
      }
      // Check for log file name.
      else if ((argv[i].compareTo("-l") == 0) && (argv.length > (i + 1))) {
        // Set the log file.
        i++;
        lFileBase = argv[i];
      }
      else {
        printUsage();
        System.exit(0);
      }
    }

    Client monitor = new Client(userName + "_m", address, port, groupsToJoin,
        lFileBase);
    monitor.start();

    // Create objects to provide the requested functionality.
    if (gotVideo) {
      // Create a Sender object.
      s = new Sender(userName, address, port, lFileBase + "_s");
      s.video = new VideoStream(VideoFileName);
      s.start();
    }
    if (!groupsToJoin.isEmpty()) {
      // Create a Receiver object for each group we are subscribing to.
      for (String g : groupsToJoin) {
        String rName = userName + "_R" + g;
        Receiver r = new Receiver(rName, address, port, g, lFileBase + rName, false);
        monitor.receivers.put(rName, r);
        r.start();
        numRcvrs++;
      }  
    } else if (groupsToJoin.isEmpty() && pingMode) {
      // Create a Pinger object.
      Pinger p = new Pinger(userName, address, port, lFileBase + "_p");
      p.start();
    } else if (thrashMode) {
      // Create many Receiver objects with the thrash variable set on all but one.
      for (int i = 0; i < Util.NUM_THRASHERS + 1; i++) {
        String rName = userName + "_RT_" + i + "_" + thrashGroup;
        boolean isThrasher = (i != Util.NUM_THRASHERS ? true : false);
        Receiver r = new Receiver(rName, address, port, thrashGroup, lFileBase + rName, isThrasher);
        monitor.receivers.put(rName, r);
        r.start();
        numRcvrs++;
      }
    }
  }

  private static void printUsage() {
    System.out.print("Usage: Client\n"
        + "\t[-u <user_name>]        : Sets the unique user name.\n"
        + "\t[-da <address>]         : Sets the name or IP of the daemon to be used.\n"
        + "\t[-dp <port>]            : Sets the port for the daemon to be used.\n"
        + "\t[-p]                    : Sets ping mode. If -r is specified, it\n"
        + "\t                          will respond to any message to the specified\n"
        + "\t                          group with a pong). Otherwise a ping test\n"
        + "\t                          will be started.\n"
        + "\t[-t <group_name>        : Sets thrash mode. Will start many receivers\n"
        + "\t                          and cause them to frequently leave and join\n"
        + "\t                          the group\n"
        + "\t[-r <group_name>]       : Starts a receiver that listens for messages\n"
        + "\t                          sent to the specified group.\n"
        + "\t[-s <video_file_name>]  : Starts a sender that multicasts the\n"
        + "\t                          specified file.\n"
        + "\t[-l <log_file_name>]    : Sets the base name for log files.\n"); 
  }
}
