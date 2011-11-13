package vchat;
/*Server.java*/

/* ------------------
   Server
   usage: java Server [RTSP listening port]
   ---------------------- */


import java.net.InetAddress;
import java.net.UnknownHostException;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

public class Sender extends Thread implements Runnable {
	//Video variables:
	//----------------
	VideoStream video; //VideoStream object used to access video frames
	static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
	byte[] buf; //buffer used to store the images to send to the client 
	static String VideoFileName; //video file requested from the client

	//Spread variables:
	//-----------------
	private String group;
	private SpreadConnection connection;
	
	//--------------------------------
	//Constructor
	//--------------------------------
	public Sender(String user, String address, int port, String groupToJoin) {
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

		//allocate memory for the sending buffer
		buf = new byte[15000]; 
	}

	//--------------------------------
	//Sends a frame
	//--------------------------------
	private void sendFrame() {
		try {
			SpreadMessage msg = new SpreadMessage();
			msg.addGroup(group);
			
			//get next frame to send from the video
			video.getNextFrame(buf);

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
	
	//------------------------------------
	//main
	//------------------------------------
	public void run()
	{
		while(true) {
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

