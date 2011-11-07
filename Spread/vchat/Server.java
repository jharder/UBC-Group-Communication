package vchat;
/*Server.java*/

/* ------------------
   Server
   usage: java Server [RTSP listening port]
   ---------------------- */


import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
//import javax.swing.*;
import javax.swing.Timer;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

public class Server implements ActionListener {

	//RTP variables:
	//----------------
	DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
	DatagramPacket senddp; //UDP packet containing the video frames

	InetAddress ClientIPAddr; //Client IP address
	int RTP_dest_port = 0; //destination port for RTP packets  (given by the RTSP Client)

	//Video variables:
	//----------------
	int imagenb = 0; //image nb of the image currently transmitted
	VideoStream video; //VideoStream object used to access video frames
	static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
	static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
	static int VIDEO_LENGTH = 500; //length of the video in frames

	Timer timer; //timer used to send the images at the video frame rate
	byte[] buf; //buffer used to store the images to send to the client 

	static String VideoFileName; //video file requested from the client

	final static String CRLF = "\r\n";

	//Spread variables:
	//-----------------
	private String group;
	private SpreadConnection connection;
	
	//--------------------------------
	//Constructor
	//--------------------------------
	public Server(String user, String address, int port, String groupToJoin){
		group = groupToJoin;
		
		//Establish the spread connection.
		//--------------------------------
		try
		{
			connection = new SpreadConnection();
			connection.connect(InetAddress.getByName(address), port, user, false, true);
		}
		catch(SpreadException e)
		{
			System.err.println("There was an error connecting to the daemon.");
			e.printStackTrace();
			System.exit(1);
		}
		catch(UnknownHostException e)
		{
			System.err.println("Can't find the daemon " + address);
			System.exit(1);
		}

		//init Timer
		timer = new Timer(FRAME_PERIOD, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		//allocate memory for the sending buffer
		buf = new byte[15000]; 
	}

	//------------------------------------
	//main
	//------------------------------------
	public static void main(String argv[]) throws Exception
	{
		String user = new String("User");
		String address = null;
		int port = 0;
		String groupToJoin = new String("Group");
		
		// Check the args.
		//////////////////
		for(int i = 0 ; i < argv.length ; i++)
		{
			// Check for user.
			//////////////////
			if((argv[i].compareTo("-u") == 0) && (argv.length > (i + 1)))
			{
				// Set user.
				////////////
				i++;
				user = argv[i];
			}
			// Check for server.
			////////////////////
			else if((argv[i].compareTo("-s") == 0) && (argv.length > (i + 1)))
			{
				// Set the server.
				//////////////////
				i++;
				address = argv[i];
			}
			// Check for port.
			//////////////////
			else if((argv[i].compareTo("-p") == 0) && (argv.length > (i + 1)))
			{
				// Set the port.
				////////////////
				i++;
				port = Integer.parseInt(argv[i]);
			}
			// Check for group.
			//////////////////
			else if((argv[i].compareTo("-g") == 0) && (argv.length > (i + 1)))
			{
				// Set the group.
				////////////////
				i++;
				groupToJoin = argv[i];
			}
			// Check for file name.
			//////////////////
			else if((argv[i].compareTo("-v") == 0) && (argv.length > (i + 1)))
			{
				// Set the file name.
				////////////////
				i++;
				VideoFileName = argv[i];
			}
			else
			{
				System.out.print("Usage: user\n" + 
								 "\t[-u <user name>]   : unique user name\n" + 
								 "\t[-s <address>]     : the name or IP for the daemon\n" + 
								 "\t[-p <port>]        : the port for the daemon\n" +
								 "\t-g <group>         : the group to multicast to\n" +
								 "\t-v <video file name>  : the video file to multicast\n");
				System.exit(0);
			}
		}
		
		//create a Server object
		Server theServer = new Server(user, address, port, groupToJoin);

		theServer.video = new VideoStream(VideoFileName);

		//start timer
		theServer.timer.start();
		while(true);
	}


	//------------------------
	//Handler for timer
	//------------------------
	public void actionPerformed(ActionEvent e) {

		//if the current image nb is less than the length of the video
		if (imagenb < VIDEO_LENGTH)
		{
			//update current imagenb
			imagenb++;

			try {
				SpreadMessage msg = new SpreadMessage();
				msg.addGroup(group);
				
				//get next frame to send from the video, as well as its size
				int image_length = video.getnextframe(buf);

				msg.setData(buf);
				
				// Send the message.
				////////////////////
				connection.multicast(msg);

				
			}
			catch(Exception ex)
			{
				System.out.println("Exception caught: "+ex);
				System.exit(0);
			}
		}
		else
		{
			//if we have reached the end of the video file, stop the timer
			timer.stop();
		}
	}
}
