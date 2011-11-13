package vchat;
/*
 * The Spread Toolkit.
 *     
 * The contents of this file are subject to the Spread Open-Source
 * License, Version 1.0 (the ``License''); you may not use
 * this file except in compliance with the License.  You may obtain a
 * copy of the License at:
 *
 * http://www.spread.org/license/
 *
 * or in the file ``license.txt'' found in this distribution.
 *
 * Software distributed under the License is distributed on an AS IS basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the 
 * License.
 *
 * The Creators of Spread are:
 *  Yair Amir, Michal Miskin-Amir, Jonathan Stanton, John Schultz.
 *
 *  Copyright (C) 1993-2009 Spread Concepts LLC <info@spreadconcepts.com>
 *
 *  All Rights Reserved.
 *
 * Major Contributor(s):
 * ---------------
 *    Ryan Caudy           rcaudy@gmail.com - contributions to process groups.
 *    Claudiu Danilov      claudiu@acm.org - scalable wide area support.
 *    Cristina Nita-Rotaru crisn@cs.purdue.edu - group communication security.
 *    Theo Schlossnagle    jesus@omniti.com - Perl, autoconf, old skiplist.
 *    Dan Schoenblum       dansch@cnds.jhu.edu - Java interface.
 *
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import spread.*;

public class recThread extends Thread implements Runnable {
	//GUI
	//----
	JFrame f = new JFrame("Client");
	JButton setupButton = new JButton("Setup");
	JButton playButton = new JButton("Play");
	JButton pauseButton = new JButton("Pause");
	JButton tearButton = new JButton("Teardown");
	JPanel mainPanel = new JPanel();
	JPanel buttonPanel = new JPanel();
	JLabel iconLabel = new JLabel();
	ImageIcon icon;
	
	Timer timer; //timer used to receive data from the UDP socket
	static int TIMER_INTERVAL = 20;
	
	byte[] buf; //buffer used to store data received from the server

	LinkedList<RTPpacket> playBuf; // Buffer used to store frames received until playback
	Timer playTimer; // Timer used to retrieve data from the buffer

	//Video constants:
	//------------------
	static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
	static int FRAME_PERIOD = 100; // Time for each frame (ms)

	private SpreadConnection connection;
	SpreadGroup group;

	public  boolean threadSuspended;

	public recThread(SpreadConnection aConn, String groupToJoin) {
		connection=aConn;
		group = new SpreadGroup();
		try {
			group.join(aConn, groupToJoin);
		} catch (SpreadException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//build GUI
		//--------------------------

		//Frame
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		//Buttons
		buttonPanel.setLayout(new GridLayout(1,0));
		buttonPanel.add(setupButton);
		buttonPanel.add(playButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(tearButton);

		//Image display label
		iconLabel.setIcon(null);

		//frame layout
		mainPanel.setLayout(null);
		mainPanel.add(iconLabel);
		mainPanel.add(buttonPanel);
		iconLabel.setBounds(0,0,380,280);
		buttonPanel.setBounds(0,280,380,50);

		f.getContentPane().add(mainPanel, BorderLayout.CENTER);
		f.setSize(new Dimension(390,370));
		f.setVisible(true);

/*		//init timers
		//--------------------------
		timer = new Timer(TIMER_INTERVAL, new timerListener());
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		playTimer = new Timer(FRAME_PERIOD, new playTimerListener());
*/
		
		//allocate enough memory for the buffer used to receive data from the server
		buf = new byte[15000];
		
		playBuf = new LinkedList<RTPpacket>();
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

	private void DisplayMessage(SpreadMessage msg)
	{
		try
		{
   	        System.out.println("*****************RECTHREAD Received Message************");
			if(msg.isRegular())
			{
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
				
//				System.out.println("The message is: " + new String(data));
				
				
/*				RTPpacket next_packet = (RTPpacket)data;
				int payload_length = next_packet.getpayload_length();
				byte [] payload = new byte[payload_length];
				next_packet.getpayload(payload);
				
				System.out.println("Playing from RTP packet with SeqNum # "+next_packet.getsequencenumber());
*/
				
				//get an Image object from the buffer
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image image = toolkit.createImage(data, 0, data.length);

				//display the image as an ImageIcon object
				icon = new ImageIcon(image);
				iconLabel.setIcon(icon);

			}
			else if ( msg.isMembership() )
			{
				MembershipInfo info = msg.getMembershipInfo();
				printMembershipInfo(info);
			} else if ( msg.isReject() )
			{
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
	
/*	class timerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			int curSeqNb = 0;
			
			//Construct a DatagramPacket to receive data from the UDP socket
			rcvdp = new DatagramPacket(buf, buf.length);

			try{
				//receive the DP from the socket:
				RTPsocket.receive(rcvdp);

				//create an RTPpacket object from the DP
				RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

				//print important header fields of the RTP packet received:
				curSeqNb = rtp_packet.getsequencenumber();
				System.out.println("Got RTP packet with SeqNum # "+curSeqNb+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

				//print header bitstream:
				//rtp_packet.printheader();

				// Update packet loss rate statistic
				if(firstSeqNb < 0) {
					firstSeqNb = expSeqNb = curSeqNb;
				} else {
					expSeqNb++;
				}
				numdropped = curSeqNb - expSeqNb;
				if(expSeqNb > firstSeqNb && numdropped > 0) {
					lossRate = (double)(numdropped) / (double)(expSeqNb - firstSeqNb);
				}
				
				if(numdropped < 0) {
					// We're getting out of order packets
				}
				
				//get the payload bitstream from the RTPpacket object
				int payload_length = rtp_packet.getpayload_length();

				// Update data rate statistic
				dataRecvd += payload_length;
				pTime += TIMER_INTERVAL;
				if(pTime > 0) {
					dataRate = (dataRecvd * 8) / ((double)(pTime) / 1000);
				}

				// Print stats
				System.out.println("Expected seq. no: " + expSeqNb + " Loss rate: "
						+ lossRate + " Data rate: " + dataRate + " bps");
				
				// Insert the packet in order in the buffer for in-order playback
				// by the playback timer
				int i;
				for(i = 0; i < playBuf.size(); i++) {
					if(playBuf.get(i).getsequencenumber() > curSeqNb) {
						break;
					}
				}
				playBuf.add(i, rtp_packet);
			}
			catch (InterruptedIOException iioe){
				//System.out.println("Nothing to read");
			}
			catch (IOException ioe) {
				System.out.println("Exception caught: "+ioe);
			}
		}
	}

	//------------------------------------
	//Handler for playback timer
	//------------------------------------
	class playTimerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			if(pTime < 1000) {
				return;
			}
			
			RTPpacket next_packet = (RTPpacket)playBuf.poll();
			if(next_packet == null) {
				return;
			}
			
			int payload_length = next_packet.getpayload_length();
			byte [] payload = new byte[payload_length];
			next_packet.getpayload(payload);
			
			System.out.println("Playing from RTP packet with SeqNum # "+next_packet.getsequencenumber());

			//get an Image object from the buffer
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Image image = toolkit.createImage(payload, 0, payload_length);

			//display the image as an ImageIcon object
			icon = new ImageIcon(image);
			iconLabel.setIcon(icon);
		}
	}
*/
	
	public void run() {
		while(true) {
			try {
				DisplayMessage(connection.receive());
				

/*				//start the receive timer
				timer.start();

				// Start the playback timer, which plays back frames from the
				// buffer at a set rate, independent of their rate of arrival
				playTimer.start();
*/
				if (threadSuspended) {
					synchronized(this) {
						while (threadSuspended) {
							wait();
						}
					}
				}
			} catch(Exception e) {

			}
		}
	}
}
