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

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

public class Receiver extends Thread implements Runnable {
	private SpreadConnection connection;
	SpreadGroup group;
	
	public  boolean threadSuspended;

	//--------------------------
	//Constructor
	//--------------------------
	public Receiver(String user, String address, int port, String groupToJoin) {

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

	private void handleMessage(SpreadMessage msg)
	{
		try
		{
   	        //System.out.println("*****************RECEIVER Received Message************");
			if(msg.isRegular())	{
				//printMsg(msg, "Regular", false);
				
				byte data[] = msg.getData();
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        VideoMsg msgData = (VideoMsg)objectStream.readObject();
        System.out.println("Received frame " + msgData.seqnum);
        objectStream.close();
      } else {
        // Discard the message and let the Client object take care of it
      }
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void printMsg(SpreadMessage msg, String msgType, boolean printData) {
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
		System.out.println(msgType + " message.");
		
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
		
		if(printData) {
			System.out.println("The message is: " + new String(data));
		}
	}
	
	public void run() {
		while(true) {
			try {
				handleMessage(connection.receive());

				if (threadSuspended) {
					synchronized(this) {
						while (threadSuspended) {
							//wait();
							group.leave();
							System.exit(0);
						}
					}
				}
			} catch(Exception e) {

			}
		}
	}
}//end of Class Receiver
