/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */
 package gctestfinal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketAddress;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.swing.Timer;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.Debug;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.common.AppiaThreadFactory;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.group.*;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.ExitEvent;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.sslcomplete.SslRegisterSocketEvent;
import net.sf.appia.protocols.udpsimple.MulticastInitEvent;
import net.sf.appia.protocols.utils.HostUtils;


// TESTING
//import test.PrecisionTime;
//import test.TestOptimized;

/*
 * Change Log:
 * Nuno Carvalho: 7/Aug/2002 - removed deprecated code;
 * Alexandre Pinto: 15/Oct/2002 - Added configuration options.
 */

/**
 * Class ApplicationSession provides the dynamic behaviour for the
 * simple user interface with Appia. This interface accepts a predefined
 * set of commands, forwarding them to Appia. It is mainly used for testing
 * @author Hugo Miranda (altered by Alexandre Pinto and Nuno Carvalho)
 * @see    Session
 */

public class RainSession extends Session {
    
    private final int INVALIDCOMMAND = -1;
    private final int CASTMESSAGE = 1;
    private final int SENDMESSAGE = 2;
    private final int DEBUGMESSAGE = 3;
    private final int HELP = 4;
    private final int LEAVE = 5;
    private final int START = 6;
    private final int STOP = 7;
    
    /* User IO */
    private PrintStream out = System.out;
    
    /* Appia */
    public Channel channel = null;
    private SocketAddress mySocketAddress = null;
    private int myPort = -1;
    private SocketAddress multicast=null;
    private SocketAddress[] initAddrs=null;
    private RainReader reader;

    
    /* Group */
    private Group myGroup=new Group("Appl Group");
    private Endpt myEndpt=null;
    private SocketAddress[] gossips = null;
    private ViewState vs=null;
    private LocalState ls=null;
    private boolean isBlocked = true;
    
    /* SSL */
    private boolean ssl=false;
    private String keystoreFile=null;
    private String keystorePass=null;
    
    /**
     * Mainly used for corresponding layer initialization
     */
    
    public RainSession(RainLayer l) {
        super(l);
    }
    
    public void init(int port, SocketAddress multicast, SocketAddress[] gossips, Group group, SocketAddress[] viewAddrs) {
      this.myPort=port;
      this.multicast=multicast;
      this.gossips=gossips;
      this.initAddrs=viewAddrs;
      if (group != null)
        myGroup=group;
    }
    
    public void initWithSSL(int port, SocketAddress multicast, SocketAddress[] gossips, Group group, SocketAddress[] viewAddrs, String keystoreFile, String keystorePass) {
      init(port,multicast,gossips,group,viewAddrs);
      this.ssl=true;
      this.keystoreFile=keystoreFile;
      this.keystorePass=keystorePass;
    }

      
    private int parseCommand(String command) {
        if (command.equals("cast"))
            return CASTMESSAGE;
        if (command.equals("debug"))
            return DEBUGMESSAGE;
        if (command.equals("help"))
            return HELP;
        if (command.equals("send"))
            return SENDMESSAGE;
        if (command.equals("leave"))
            return LEAVE;
        if (command.equals("start"))
            return START;
        if (command.equals("stop"))
            return START;
        return INVALIDCOMMAND;
    }
    
    private void printHelp() {
        out.println("help\tPrints this message");
        out.println(
        "cast r i m\t Sends message m r times "
        + "with i tens of a second between them");
        out.println(
        "send d r i m\t Sends message m r times "
        + "with i tens of a second between them, to member with rank d");
        out.println("debug {start|stop|now} [outputFile]");
    }
    
    private void sendDebug(StringTokenizer args) {
        if (args.countTokens() != 1 && args.countTokens() != 2) {
            out.println("Invalid arguments number. Usage:");
            out.println(
            "debug one of this options: start, stop, now and an optional file name");
            return;
        }
        
        try {
            int eq;
            
            String action = args.nextToken();
            
            if (action.equals("start"))
                eq=EventQualifier.ON;
            else if (action.equals("stop"))
                eq=EventQualifier.OFF;
            else if (action.equals("now"))
                eq=EventQualifier.NOTIFY;
            else {
                out.println("Invalid action. Use one of start, stop and now");
                return;
            }
            
            OutputStream debugOut = out;
            
            if (args.hasMoreTokens()) {
                try {
                    debugOut = new FileOutputStream(args.nextToken(), false);
                } catch (FileNotFoundException ex) {
                    out.println("File could not be oppened for output");
                    return;
                }
            }
            
            Debug e =
            new Debug(debugOut);
            e.setChannel(channel);
            e.setDir(Direction.DOWN);
            e.setSourceSession(this);
            e.setQualifierMode(eq);
            
            e.init();
            e.go();
        } catch (AppiaEventException ex) {
            out.println("Unexpected exception when sending debug event");
        }
    }
    
    private void castMessage(StringTokenizer args) {
        int period = 0, resends = 0;
     
        long millis = System.currentTimeMillis();
        String mensagem = "ping " + millis;
        
        try {
            RainTimer msgTimer =
            new RainTimer(channel, this, new RainMessage(mensagem,RainMessage.STRING_MSG), resends, period, null);
            msgTimer.go();
        } catch (AppiaEventException ex) {
            out.println("There was a problem with message sending - castMsg");
        } catch (AppiaException ex) {
            out.println("The time between messages must be >= 0");
        }
    }
    
    private void sendMessage(StringTokenizer args) {
        if (args.countTokens() < 4) {
            out.println("Wrong number of args to cast");
            return;
        }
        
        int period = 0, resends = 0;
        int[] dest = new int[1];
        
        try {
            dest[0] = Integer.parseInt(args.nextToken());
            resends = Integer.parseInt(args.nextToken());
            period = Integer.parseInt(args.nextToken()) * 100;
        } catch (NumberFormatException ex) {
            out.println(
            "The repetitions number and the time  between messages must be "
            + "an integer. Write \"help\" to get more information");
            return;
        }
        
        String mensagem = new String();
        
        while (args.hasMoreElements()) {
            mensagem += args.nextToken() + " ";
        }
        
        try {
            RainTimer msgTimer =
            new RainTimer(channel, this, new RainMessage(mensagem,RainMessage.STRING_MSG), resends, period, dest);
            msgTimer.go();
        } catch (AppiaEventException ex) {
            out.println("There was a problem with message sending - sndMsg");
        } catch (AppiaException ex) {
            out.println("The time between messages must be >= 0");
        }
    }
    
    private void sendPing(long millis) {
        int period = 1, resends  = 1;
        Long mil = millis;
        int dest[] = {0};
        
        try {
            RainTimer msgTimer =
            new RainTimer(channel, this, new RainMessage(mil,RainMessage.PING_MSG), resends, period, dest);
            msgTimer.go();
        } catch (AppiaEventException ex) {
            out.println("There was a problem with message sending - sndMsg");
        } catch (AppiaException ex) {
            out.println("The time between messages must be >= 0");
        }
    }
    
    private void castFrame(FrameMessage fm) {
        
        int period = 1, resends = 1;
        
        try {
            Checksum chsm = new CRC32();
            chsm.update(fm.buf, 0, fm.buf.length);
            fm.timeinmillis = System.nanoTime();
            RainMessage rm = new RainMessage(fm, RainMessage.FRAME_MSG, chsm);
            RainTimer msgTimer =
            new RainTimer(channel, this, rm, resends, period, null);
            msgTimer.go();
        } catch (AppiaEventException ex) {
            out.println("There was a problem with message sending - castFrame" + ex.getMessage());
        } catch (AppiaException ex) {
            out.println("The time between messages must be >= 0");
        }
    }
    
    /**
     * Main Event handler function. Accepts all incoming events and
     * dispatches them to the appropriate functions
     * @param e The incoming event
     * @see Session
     */
    public void handle(Event e) {
        if (e instanceof ChannelInit)
            handleChannelInit((ChannelInit) e);
        else if (e instanceof ChannelClose)
            handleChannelClose((ChannelClose) e);
        else if (e instanceof RainCastEvent)
            receiveData((GroupSendableEvent) e);
        else if (e instanceof RainSendEvent)
            receivePing((GroupSendableEvent) e);
        else if (e instanceof View)
            handleNewView((View) e);
        else if (e instanceof BlockOk)
            handleBlock((BlockOk) e);
        else if (e instanceof RainTimer)
            handleTimer((RainTimer) e);
        else if (e instanceof ExitEvent)
            handleExitEvent((ExitEvent) e);
        else if (e instanceof RainAsyncStringEvent)
            handleRainAsyncStringEvent((RainAsyncStringEvent) e);
        else if (e instanceof RainAsyncFrameEvent) 
            handleRainAsyncFrameEvent((RainAsyncFrameEvent) e);
        else if (e instanceof RegisterSocketEvent)
          handleRSE((RegisterSocketEvent)e);
    }
    
    /**
     * Method handleApplAsyncEvent.
     * @param applAsyncEvent
     */
    
    boolean vidsend = false;

    private void handleRainAsyncStringEvent(RainAsyncStringEvent rainAsyncEvent) {
        String sComLine = rainAsyncEvent.getComLine();
        StringTokenizer comLine = new StringTokenizer(sComLine);
        
        if (comLine.hasMoreTokens()) {
            String command = comLine.nextToken();
            
            switch (parseCommand(command)) {
                case HELP :
                    printHelp();
                    break;
                case CASTMESSAGE :
                    castMessage(comLine);
                    break;
                case DEBUGMESSAGE :
                    sendDebug(comLine);
                    break;
                case SENDMESSAGE :
                    sendMessage(comLine);
                    break;
                case LEAVE :
                    sendLeave();
                    break;
                case START :
                    vidsend = true;
                    start_serving_file();
                    break;
                default :
                    System.out.println("Invalid command");
            }
        }
    }
    
    private void handleRainAsyncFrameEvent(RainAsyncFrameEvent rainAsyncEvent) {
        FrameMessage fm = rainAsyncEvent.getComLine();
        
        castFrame(fm);
    }
    
    private void handleNewView(View e) {
        vs = e.vs;
        ls = e.ls;
        isBlocked = false;
        
//        stop_serving_file();
//        stop_playing_file();
        
        out.println("New view delivered:");
        out.println("View members (IP:port):");
        for (int i = 0; i < vs.addresses.length; i++)
            out.println("{" + vs.addresses[i] + "} ");
        out.println(
        (e.ls.am_coord ? "I am" : "I am not") + " the group coordinator");
                /*
                  out.println(e.vs.toString());
                  out.println(e.ls.toString());
                 */
        try {
            e.go();
        } catch (AppiaEventException ex) {
            out.println("Exception while sending the new view event");
        }
        
        if (e.ls.am_coord && vidsend) {
            System.out.println("start sending video...");
            start_serving_file();
        } else if (!e.ls.am_coord) {
            System.out.println("start receiving video...");
            start_playing_file();
        }
    }
    
    private void handleBlock(BlockOk e) {
        stop_serving_file();
        stop_playing_file();
        out.println("The group was blocked. Impossible to send messages.");
        isBlocked = true;
        try {
            e.go();
        } catch (AppiaEventException ex) {
            out.println("Exception while forwarding the block ok event");
        }
    }
    
    private void handleTimer(RainTimer e) {
        
        if (isBlocked) {
            try {
                e.setSourceSession(this);
                e.setDir(Direction.invert(e.getDir()));
                e.setQualifierMode(EventQualifier.ON);
                e.setTimeout(e.getChannel().getTimeProvider().currentTimeMillis() + e.period);
                e.init();
                e.go();
            } catch (AppiaEventException ex) {
                out.println(
                "Exception while sending the timer event in a blocked group");
            } catch (AppiaException ex) {
                out.println(
                "Exception while sending the timer event in a blocked group");
            }
            return;
        }
        try {
            GroupSendableEvent msgEvent;
            
            if (e.dest != null)
                msgEvent =
                new RainSendEvent(channel, this, vs.group, vs.id, e.dest);
            else
                msgEvent = new RainCastEvent(channel, this, vs.group, vs.id);
            
            msgEvent.source = vs.view[ls.my_rank];
            
            RainMessageHeader header = new RainMessageHeader(e.msg,e.thisResend);
//            System.out.print("sending something... ");
//            if(e.msg.type == RainMessage.STRING_MSG) System.out.println("sending string cp 1 = pass");
//            if(e.msg.type == RainMessage.FRAME_MSG) System.out.println("sending frame cp 1 = fail");
            header.pushMySelf(msgEvent.getMessage());
            msgEvent.go();


            
            if (e.hasMore()) {
                e.prepareNext();
                e.go();
            }
        } catch (AppiaEventException ex) {
            System.err.println("Unexpected exception in Application Session");
            switch (ex.type) {
                case AppiaEventException.NOTINITIALIZED :
                    System.err.println(
                    "Event not initialized in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.ATTRIBUTEMISSING :
                    System.err.println(
                    "Missing attribute in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.UNKNOWNQUALIFIER :
                    System.err.println(
                    "Unknown qualifier (impossible) in "
                    + " message sending (Application Session)");
                    break;
                case AppiaEventException.UNKNOWNSESSION :
                    System.err.println(
                    "Unknown session in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.UNWANTEDEVENT :
                    System.err.println(
                    "Unwanted event in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.CLOSEDCHANNEL :
                    System.err.println(
                    "Channel closed in message "
                    + "sending (Application Session)");
                    break;
            }
        } catch (AppiaException ex) {
            out.println(
            "Exception while sending the timer for the next message");
        }
    }
    
    long avgping = 0;
    long numping = 0;
    
    private void receivePing(GroupSendableEvent e) {
        
        if (e.getDir() == Direction.UP) {
            Message m = e.getMessage();
            if (e instanceof RainSendEvent){
                System.out.print("ping from " + ((Endpt) e.source).toString() + " = ");
             
            }
            RainMessageHeader header = new RainMessageHeader(m);
            
            if (header.message.type == RainMessage.PING_MSG) {
                Long millis = (Long) header.message.payload;
                long timenow = System.nanoTime();
                Long diff = timenow - millis.longValue();
                out.println(diff/2);
                avgping += diff/2;
                numping++;
            }
        }
        try {
            e.go();
        } catch (AppiaEventException ex) {
            System.err.println(
            "Unexpected exception in Application " + "session");
        }
    }
    
    private void receiveData(GroupSendableEvent e) {
        
        /* Echoes received messages to the user */
        if (e.getDir() == Direction.UP) {
            Message m = e.getMessage();
            
            if (e instanceof RainSendEvent)
                out.print("Message (pt2pt)");
            else
                out.print("Message (multicast)");
            
            out.println(" received from " + ((Endpt) e.source).toString());
            RainMessageHeader header = new RainMessageHeader(m);
            // OR
            // header.popMySelf(m);
            if (header.message.type == RainMessage.STRING_MSG) {
                System.out.println("("+header.number+") "+ header.message.payload.toString());
            } else if (header.message.type == RainMessage.FRAME_MSG) {
                FrameMessage blah = (FrameMessage) header.message.payload;
                sendPing(blah.timeinmillis);
                Checksum cs = new CRC32();
                cs.update(blah.buf, 0, blah.buf.length);
                System.out.println("size recvd = " + blah.buf.length);
                if(cs.getValue() == header.message.chsm){}
                else {
                    System.out.println("checksum not correct - corrupt data");
                }
                queue.add((FrameMessage) header.message.payload);
//                System.out.println("Frame received");
            }
            
        }
        try {
            e.go();
        } catch (AppiaEventException ex) {
            System.err.println(
            "Unexpected exception in Application " + "session");
        }
    }
    
    private void handleChannelInit(ChannelInit e) {
        
        final Thread t = e.getChannel().getThreadFactory().newThread(new RainReader(this));
        t.setName("Appl Reader Thread");
        t.start();
         
                /* Forwards channel init event. New events must follow this
                   one */
        try {
            e.go();
        } catch (AppiaEventException ex) {
            System.err.println("Unexpected exception in Application " + "session");
        }
        
        channel = e.getChannel();
        
                /* Informs layers bellow of the port
                   where messages will be received and sent */
        
        try {
            RegisterSocketEvent rse;
            
            if (ssl) {
              if ((keystoreFile != null) && (keystorePass != null))
                rse=new SslRegisterSocketEvent(channel,Direction.DOWN,this,keystoreFile,keystorePass.toCharArray());
              else
                rse=new SslRegisterSocketEvent(channel,Direction.DOWN,this);
            } else {
              rse=new RegisterSocketEvent(channel,Direction.DOWN,this,(myPort < 0) ? RegisterSocketEvent.FIRST_AVAILABLE : myPort);
            }
            
            rse.go();
        } catch (AppiaEventException ex) {
            switch (ex.type) {
                case AppiaEventException.UNWANTEDEVENT :
                    System.err.println(
                    "The QoS definition doesn't satisfy the "
                    + "application session needs. "
                    + "RegisterSocketEvent, received by "
                    + "UdpSimpleSession is not being acepted");
                    break;
                default :
                    System.err.println(
                    "Unexpected exception in " + "Application session");
                    break;
            }
        }
        
        if (multicast != null) {
            try {
                MulticastInitEvent amie =
                new MulticastInitEvent(multicast,false,channel,Direction.DOWN,this);
                amie.go();
            } catch (AppiaEventException ex) {
                System.err.println(
                "EventException while launching MulticastInitEvent");
            } catch (NullPointerException ex) {
                System.err.println(
                "EventException while launching MulticastInitEvent");
            }
        }
        
        out.println("Open channel with name " + e.getChannel().getChannelID());
    }

    private void handleChannelClose(ChannelClose ev) {
        out.println("Channel Closed");
        try {
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }
    
    private void sendLeave() {
        try {
            LeaveEvent ev = new LeaveEvent(channel,Direction.DOWN,this,vs.group,vs.id);
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }
    
    private void handleExitEvent(ExitEvent ev) {
        out.println("Exit");
        try {
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }
    
    private void handleRSE(RegisterSocketEvent ev) {
      if (ev.error) {
        System.err.println("Error while registering socket: "+ev.port);
        System.exit(1);
      }
      
      if (myPort < 0) {
        myPort=ev.port;
        try {
            mySocketAddress = ev.getLocalSocketAddress();
            sendGroupInit();
        } catch (AppiaException e) {
            e.printStackTrace();
        }
      }  
    }
      
    private void sendGroupInit() {
      try {
        myEndpt=new Endpt("Appl@"+mySocketAddress);
        
        Endpt[] view=null;
        SocketAddress[] addrs=null;
        if (initAddrs == null) {
          addrs=new SocketAddress[1];
          addrs[0]=mySocketAddress;
          view=new Endpt[1];
          view[0]=myEndpt;
        } else {
          addrs=initAddrs;
          view=new Endpt[addrs.length];
          for (int i=0 ; i < view.length ; i++) {
            view[i]=new Endpt("Appl@"+addrs[i]);
          }
        }
        
        vs=new ViewState("1", myGroup, new ViewID(0,view[0]), new ViewID[0], view, addrs);
        
//        if (gossips != null) {
//          String s="GOSSIPS: ";
//          for (int i=0 ; i < gossips.length ; i++)
//            s+=(gossips[i].toString()+" ");
//          System.out.println(s+"\n");
//        }
//        System.out.println("INITIAL_VIEW: "+vs.toString());
        
        GroupInit gi =
        new GroupInit(vs,myEndpt,multicast,gossips,channel,Direction.DOWN,this);
        gi.go();
      } catch (AppiaEventException ex) {
        System.err.println("EventException while launching GroupInit: "+ex.getMessage());
      } catch (NullPointerException ex) {
        System.err.println("EventException while launching GroupInit: "+ex.getMessage());
      } catch (AppiaGroupException ex) {
        System.err.println("EventException while launching GroupInit: "+ex.getMessage());
        
      }
    }
 
    int totalbytes = 0;
    
    SendFile sending_thread = null;
    RecvFile frame_display = null;
    
    ConcurrentLinkedQueue<FrameMessage> queue = null;

    // My functions
    public void start_serving_file() {
//
        SendFile sendThread = new SendFile(this);
        sending_thread = sendThread;
        sendThread.start();

    }

    public void stop_serving_file() {

        if(sending_thread != null) {
            sending_thread.end();
            sending_thread = null;
        }

    }

    class SendFile extends Thread implements ActionListener {

        RainSession session; // The Appia session info
        Timer timer; //timer used to send the images at the video frame rate
        byte[] buf; //buffer used to store the images to send to the client 
        int imagenb = 0; //image nb of the image currently transmitted
        VideoStream video; //VideoStream object used to access video frames
        int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
        int FRAME_PERIOD = 42; //Frame period of the video to stream, in ms
        int VIDEO_LENGTH = 500;

        public SendFile(RainSession sesssion) {

            this.session = sesssion;
            //init Timer
            timer = new Timer(FRAME_PERIOD, this);
            timer.setInitialDelay(0);
            timer.setCoalesce(true);

            //allocate memory for the sending buffer
            buf = new byte[15000];

        }

        public void run() {
            try {
                this.video = new VideoStream("movie.Mjpeg");
                timer.start();
            } catch (Exception ex) {
                Logger.getLogger(RainSession.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        public void end() {
            timer.stop();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            
            //if the current image nb is less than the length of the video
            if (imagenb < VIDEO_LENGTH) {
                try {
                    //update current imagenb
                    imagenb++;

                    //get next frame to send from the video, as well as its size
                    int image_length = video.getnextframe(buf);
                
                    FrameMessage mess = new FrameMessage(buf, image_length);
                    //send the packet as a DatagramPacket over the UDP socket 
//                    castFrame(mess);
                    RainAsyncFrameEvent asyn = new RainAsyncFrameEvent(mess);
                    asyn.asyncGo(session.channel,Direction.DOWN);
//                    StringTokenizer comLine = new StringTokenizer("cast 1 1 framesent");
//                    castMessage(comLine);
//				senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
//				RTPsocket.send(senddp);

                    System.out.println("Send frame #" + imagenb + "size = " + buf.length);
                    totalbytes += buf.length;
                    
//                    if(imagenb % 10 == 0) {
//                        
//                        RainAsyncStringEvent ping = new RainAsyncStringEvent("ping ");
//                    }
                    //print the header bitstream
//				rtp_packet.printheader();
                } catch (Exception ex) {
                    Logger.getLogger(RainSession.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
                //if we have reached the end of the video file, stop the timer
                System.out.println("bytes sent = " + totalbytes + " bytes");
                System.out.println("avg ping = " + avgping/numping + " ms");
                timer.stop();
            }
        }

    }
    
    public void stop_playing_file() {
        
        if(frame_display != null) {
            frame_display.end();
            frame_display = null;
            queue = null;
        }
        
    }
    
    public void start_playing_file() {
        
        queue = new ConcurrentLinkedQueue<FrameMessage>();
        frame_display = new RecvFile();
        frame_display.start();
        
    }
    
    class RecvFile extends Thread implements ActionListener {
        
        Timer playTimer;
        int FRAME_PERIOD = 42;
        
//        JFrame f = new JFrame("Client");
//	JPanel mainPanel = new JPanel();
//	JLabel iconLabel = new JLabel();
//	ImageIcon icon;
        
        public RecvFile() {
            
            //build GUI
		//--------------------------

		//Frame
//		f.addWindowListener(new WindowAdapter() {
//			public void windowClosing(WindowEvent e) {
//				System.exit(0);
//			}
//		});
//
//		//Image display label
//		iconLabel.setIcon(null);
//
//		//frame layout
//		mainPanel.setLayout(null);
//		mainPanel.add(iconLabel);
//		iconLabel.setBounds(0,0,380,280);
//
//		f.getContentPane().add(mainPanel, BorderLayout.CENTER);
//		f.setSize(new Dimension(400,300));
//		f.setVisible(true);
            
            playTimer = new Timer(FRAME_PERIOD, this);
        }
        
        public void run() {
            playTimer.start();
        }
        
        public void end() {
            playTimer.stop();
//            f.dispose();
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            
            FrameMessage fm;
            
            if(!queue.isEmpty()) {
                fm = queue.poll();
              
//			//get an Image object from the buffer
//			Toolkit toolkit = Toolkit.getDefaultToolkit();
//			Image image = toolkit.createImage(fm.buf, 0, fm.image_length);
//
//			//display the image as an ImageIcon object
//			icon = new ImageIcon(image);
//			iconLabel.setIcon(icon);
                
            } else {
                System.out.println("no frame available");
            }
            
        }
        
        
    }
    
    
}
