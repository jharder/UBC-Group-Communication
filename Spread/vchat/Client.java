/* Client.java
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
   Client
   usage: java Client ...
   ---------------------- */
package vchat;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.ImageIcon;

import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;


public class Client extends Thread implements Runnable {
	
	static String VideoFileName; //video file requested from the client
	private SpreadConnection connection;
	SpreadGroup group;
	
	public Client(String user, String address, int port, String groupToJoin) {
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
		
		group = new SpreadGroup();
		try {
			group.join(connection, groupToJoin);
		} catch (SpreadException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	// Print this membership data.  Does so in a generic way so identical
	// function is used in recThread and User. 
	private void printMembershipInfo(MembershipInfo info) 
	{
        SpreadGroup group = info.getGroup();
		if(info.isRegularMembership()) {
			SpreadGroup members[] = info.getMembers();
			MembershipInfo.VirtualSynchronySet virtual_synchrony_sets[] = info.getVirtualSynchronySets();
			MembershipInfo.VirtualSynchronySet my_virtual_synchrony_set = info.getMyVirtualSynchronySet();

			System.out.println("REGULAR membership for group " + group +
					   " with " + members.length + " members:");
			for( int i = 0; i < members.length; ++i ) {
				System.out.println("\t\t" + members[i]);
			}
			System.out.println("Group ID is " + info.getGroupID());

			System.out.print("\tDue to ");
			if(info.isCausedByJoin()) {
				System.out.println("the JOIN of " + info.getJoined());
			}	else if(info.isCausedByLeave()) {
				System.out.println("the LEAVE of " + info.getLeft());
			}	else if(info.isCausedByDisconnect()) {
				System.out.println("the DISCONNECT of " + info.getDisconnected());
			} else if(info.isCausedByNetwork()) {
				System.out.println("NETWORK change");
				for( int i = 0 ; i < virtual_synchrony_sets.length ; ++i ) {
					MembershipInfo.VirtualSynchronySet set = virtual_synchrony_sets[i];
					SpreadGroup         setMembers[] = set.getMembers();
					System.out.print("\t\t");
					if( set == my_virtual_synchrony_set ) {
						System.out.print( "(LOCAL) " );
					} else {
						System.out.print( "(OTHER) " );
					}
					System.out.println( "Virtual Synchrony Set " + i + " has " +
							    set.getSize() + " members:");
					for( int j = 0; j < set.getSize(); ++j ) {
						System.out.println("\t\t\t" + setMembers[j]);
					}
				}
			}
		} else if(info.isTransition()) {
			System.out.println("TRANSITIONAL membership for group " + group);
		} else if(info.isSelfLeave()) {
			System.out.println("SELF-LEAVE message for group " + group);
		}
	}
	
	private void handleMessage(SpreadMessage msg) {
		try
		{
   	        System.out.println("*****************RECTHREAD Received Message************");
			if(msg.isRegular())	{
				System.out.print("Received a ");
				if(msg.isUnreliable())
					System.out.print("UNRELIABLE");
				else if(msg.isReliable())
					System.out.print("RELIABLE");
				else if(msg.isFifo())
					System.out.print("FIFO");
				else if(msg.isCausal())
					System.out.print("CAUSAL");
				else if(msg.isAgreed())
					System.out.print("AGREED");
				else if(msg.isSafe())
					System.out.print("SAFE");
				System.out.println(" message.");
				
				System.out.println("Sent by  " + msg.getSender() + ".");
				
				System.out.println("Type is " + msg.getType() + ".");
				
				if(msg.getEndianMismatch() == true)
					System.out.println("There is an endian mismatch.");
				else
					System.out.println("There is no endian mismatch.");
				
				SpreadGroup groups[] = msg.getGroups();
				System.out.println("To " + groups.length + " groups.");
				
				byte data[] = msg.getData();
				System.out.println("The data is " + data.length + " bytes.");
				
				// TODO:
				// TODO:
				// TODO:
				// TODO: (This is important!)
				// TODO: Pass msg (or data) to the Receiver object created for the sender of this message
			} else if ( msg.isMembership() ) {
				MembershipInfo info = msg.getMembershipInfo();
				printMembershipInfo(info);
			} else if ( msg.isReject() ) {
			    // Received a Reject message 
				System.out.print("Received a ");
				if(msg.isUnreliable())
					System.out.print("UNRELIABLE");
				else if(msg.isReliable())
					System.out.print("RELIABLE");
				else if(msg.isFifo())
					System.out.print("FIFO");
				else if(msg.isCausal())
					System.out.print("CAUSAL");
				else if(msg.isAgreed())
					System.out.print("AGREED");
				else if(msg.isSafe())
					System.out.print("SAFE");
				System.out.println(" REJECTED message.");
				
				System.out.println("Sent by  " + msg.getSender() + ".");
				
				System.out.println("Type is " + msg.getType() + ".");
				
				if(msg.getEndianMismatch() == true)
					System.out.println("There is an endian mismatch.");
				else
					System.out.println("There is no endian mismatch.");
				
				SpreadGroup groups[] = msg.getGroups();
				System.out.println("To " + groups.length + " groups.");
				
				byte data[] = msg.getData();
				System.out.println("The data is " + data.length + " bytes.");
				
				System.out.println("The message is: " + new String(data));
			} else {
			    System.out.println("Message is of unknown type: " + msg.getServiceType() );
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * Monitors for users joining or leaving, and creates and tears down
	 * receiver threads accordingly
	 */
	public void run() {
		while(true) {
			try {
				handleMessage(connection.receive());
			} catch(Exception e) {
				
			}
			
		}
	}
	
	//------------------------------------
	//main
	//------------------------------------
	public static void main(String argv[]) throws Exception
	{
		// Set default values.
		String user = new String("User");
		String address = null;
		int port = 0;
		String groupToJoin = new String("Group");
		
		boolean gotFilename = false;
		
		// Check the args.
		for(int i = 0 ; i < argv.length ; i++)
		{
			// Check for user.
			if((argv[i].compareTo("-u") == 0) && (argv.length > (i + 1)))
			{
				// Set user.
				i++;
				user = argv[i];
			}
			// Check for server.
			else if((argv[i].compareTo("-s") == 0) && (argv.length > (i + 1)))
			{
				// Set the server.
				i++;
				address = argv[i];
			}
			// Check for port.
			else if((argv[i].compareTo("-p") == 0) && (argv.length > (i + 1)))
			{
				// Set the port.
				i++;
				port = Integer.parseInt(argv[i]);
			}
			// Check for group.
			else if((argv[i].compareTo("-g") == 0) && (argv.length > (i + 1)))
			{
				// Set the group.
				i++;
				groupToJoin = argv[i];
			}
			// Check for file name.
			else if((argv[i].compareTo("-v") == 0) && (argv.length > (i + 1)))
			{
				// Set the file name.
				i++;
				VideoFileName = argv[i];
				gotFilename = true;
			}
			else
			{
				printUsage();
				System.exit(0);
			}
		}
		
		if(!gotFilename) {
			System.out.println("Please supply a video file.\n\n");
			printUsage();
			System.exit(0);
		}
		
		Receiver r = new Receiver(user + "r1", address, port, groupToJoin);
		r.start();

		//create a Sender object
		Sender s = new Sender(user, address, port, groupToJoin);
		s.video = new VideoStream(VideoFileName);
		s.start();
		
		Client monitor = new Client(user + "m", address, port, groupToJoin);
		monitor.start();
	}

	private static void printUsage() {
		System.out.print("Usage: user\n" + 
						 "\t[-u <user name>]   : unique user name\n" + 
						 "\t[-s <address>]     : the name or IP for the daemon\n" + 
						 "\t[-p <port>]        : the port for the daemon\n" +
						 "\t[-g <group name>]    : the group to join\n" +
						 "\t-v <video file name>  : the video file to multicast\n");
	}
}//end of Class Client