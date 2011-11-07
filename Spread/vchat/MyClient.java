/*Client.java*/

/* ------------------
   Client
   usage: java Client [Server hostname] [Server RTSP listening port] [Video file requested]
   ---------------------- */
package vchat;

import java.net.*;


import spread.SpreadConnection;
import spread.SpreadException;

public class MyClient{
	
	final static String CRLF = "\r\n";
	private SpreadConnection connection;
	private recThread rt;

	//--------------------------
	//Constructor
	//--------------------------
	public MyClient(String user, String address, int port, String groupToJoin) {

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
		
		rt = new recThread(connection, groupToJoin);
		rt.start();
	}
	
	//------------------------------------
	//main
	//------------------------------------
	public static void main(String argv[]) throws Exception
	{
		// Default values.
		//////////////////
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
			else
			{
				System.out.print("Usage: user\n" + 
								 "\t[-u <user name>]   : unique user name\n" + 
								 "\t[-s <address>]     : the name or IP for the daemon\n" + 
								 "\t[-p <port>]        : the port for the daemon\n" +
								 "\t-g <group name>    : the group to join\n");
				System.exit(0);
			}
		}
		
		MyClient u = new MyClient(user, address, port, groupToJoin);
	}
}//end of Class Client