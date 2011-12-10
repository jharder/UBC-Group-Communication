import java.awt.*;
import javax.swing.*;
import org.jgroups.*;
import java.util.ArrayList;
import java.util.concurrent.locks.*;

public class server extends ReceiverAdapter
{
	public static final byte m_join=0;
	public static final byte m_confirm=1;
	public static final byte m_leave=2;
	public static final byte m_vframe=3;
	public static final byte m_ping=4;
	public static final byte m_pingack=5;
	public static final byte space=32;
	
	public String clustername;
	public String username;
	public String filename;
	public videostream vstream;
	public long framelength; //Time between sending frames, in milliseconds.
	public long lastframe; //The time of sending the last frame.
	public long targetfps; //1000 divided by framelength.
	public long pinglength; //Time between ping messages, in milliseconds.
	public long lastping; //The time of sending the last ping.
	public long lastseq; //The last sequence number sent.
	public ArrayList<client> clist;
	public ReentrantLock clistlock;
	public ReentrantLock rlistlock;
	public JChannel lchan;
	public boolean confirmed;
	public boolean active;
	public boolean going;
	public ArrayList<Long> outtimes;
	public ArrayList<Integer> outsizes;
	public long outspeed;
	public genericlistener updater;
	public boolean updating;
	public JFrame window;
	public JLabel statuslabel;
	public JLabel speedlabel;
	public JLabel latencylabel;
	public JButton pausebutton;
	public JButton closebutton;
	public JButton killbutton;
	public boolean showvideo;
	
	public server(String pclustername,String pusername,String pfilename,long pframelength,long ppinglength)
	{
		clustername=pclustername;
		username=pusername;
		filename=pfilename;
		vstream=new videostream(filename,null);
		framelength=pframelength;
		targetfps=1000/framelength;
		lastframe=System.currentTimeMillis();
		pinglength=ppinglength;
		lastping=System.currentTimeMillis();
		lastseq=0;
		clist=new ArrayList<client>();
		clistlock=new ReentrantLock(true);
		rlistlock=new ReentrantLock(true);
		try
		{
			lchan=new JChannel("videotest.xml");
		}
		catch(Exception e)
		{
			manager.print("An error occurred while creating a channel.\n");
			lchan=null;
		}
		if(lchan!=null)
		{
			try
			{
				lchan.setReceiver(this);
				lchan.connect(clustername);
			}
			catch(Exception e)
			{
				manager.print("An error occurred while connecting on a channel.\n");
			}
		}
		confirmed=false;
		active=true;
		going=true;
		outtimes=new ArrayList<Long>();
		outsizes=new ArrayList<Integer>();
		outspeed=0;
		updater=null;
		updating=false;
		if(manager.graphics)
		{
			window=new JFrame();
			window.setBounds(new Rectangle(96,96,512,256));
			window.setLayout(new GridBagLayout());
			window.setTitle("Server: "+username+"@"+clustername);
			window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			statuslabel=new JLabel("Running...");
			window.add(statuslabel,new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
			speedlabel=new JLabel(datahtml());
			window.add(speedlabel,new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
			pausebutton=new JButton();
			pausebutton.setText(going?"Pause":"Resume");
			pausebutton.setFont(manager.buttonfont);
			pausebutton.addActionListener(new genericlistener(genericlistener.a_serverpause,this));
			window.add(pausebutton,new GridBagConstraints(0,2,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
			closebutton=new JButton();
			closebutton.setText("Close");
			closebutton.setFont(manager.buttonfont);
			closebutton.addActionListener(new genericlistener(genericlistener.a_serverclose,this));
			window.add(closebutton,new GridBagConstraints(0,3,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
			killbutton=new JButton();
			killbutton.setText("Kill");
			killbutton.setFont(manager.buttonfont);
			killbutton.addActionListener(new genericlistener(genericlistener.a_serverkill,this));
			window.add(killbutton,new GridBagConstraints(0,4,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
			window.setEnabled(true);
			window.setVisible(true);
		}
		showvideo=true;
	}
	
	public byte[] constructdata(byte type,byte[] a,int minsize)
	{
		byte[] data=new byte[Math.max(username.length()+a.length+3,minsize)];
		data[1]=type;
		byte[] usdata=username.getBytes();
		System.arraycopy(usdata,0,data,2,usdata.length);
		data[usdata.length+2]=space;
		System.arraycopy(a,0,data,usdata.length+3,a.length);
		byte checksum=0;
		for(int i=1;i<data.length;i++)
		{
			checksum^=data[i];
		}
		data[0]=checksum;
		return data;
	}
	
	public Message constructmessage(byte type,byte[] a,int minsize)
	{
		Message msg=new Message(null,null,constructdata(type,a,minsize));
		return msg;
	}
	
	public void sendmessage(Message msg,byte type)
	{
		try
		{
			lchan.send(msg);
			rlistlock.lock();
			outtimes.add(new Long(System.currentTimeMillis()));
			int outsize=msg.getRawBuffer().length;
			outsizes.add(new Integer(outsize));
			outspeed+=(long)outsize;
			rlistlock.unlock();
			manager.datalock.lock();
			manager.data_bytes+=(long)outsize;
			manager.datalock.unlock();
			manager.print(servertext()+": Sent a message ("+type+", "+outsize+" bytes).\n");
		}
		catch(Exception e)
		{
			manager.print("An error occurred while trying to send a message ("+clustername+":"+username+", "+type+").\n");
		}
	}
	
	public void sendmessage_stealthy(Message msg,byte type)
	{
		try
		{
			lchan.send(msg);
			manager.print(servertext()+": Sent a stealthy message ("+type+", "+msg.getRawBuffer().length+" bytes).\n");
		}
		catch(Exception e)
		{
			manager.print("An error occurred while trying to send a message ("+clustername+":"+username+", "+type+").\n");
		}
	}
	
	public void receive(Message msg)
	{
		super.receive(msg);
		manager.datalock.lock();
		manager.data_received++;
		manager.datalock.unlock();
		byte[] a=msg.getRawBuffer();
		if(a.length<=1)
		{
			manager.print("Got a message with 0 length. Something very strange must have happened. The message has been discarded.\n");
			return;
		}
		byte checksum=0;
		for(int j=1;j<a.length;j++)
		{
			checksum^=a[j];
		}
		if(checksum!=a[0])
		{
			manager.print("Got a message with a bad checksum (expected "+checksum+", got "+a[0]+"). The message has been discarded.\n");
			manager.datalock.lock();
			manager.data_badchecksum++;
			manager.datalock.unlock();
			return;
		}
		byte[] mua="_".getBytes();
		int i=2;
		while(i<a.length)
		{
			if(a[i]==space)
			{
				i-=2;
				mua=new byte[i];
				System.arraycopy(a,2,mua,0,i);
				break;
			}
			i++;
		}
		String muname=new String(mua);
		manager.printlock.lock();
		manager.print(servertext()+": Received a message from "+muname+" ("+a[1]+").\n");
		if(muname.equals(username))
		{
			manager.print("^-- reflection\n");
			manager.printlock.unlock();
			return;
		}
		client tc=null;
		clistlock.lock();
		for(client nc:clist)
		{
			if(nc.servername.equals(muname))
			{
				tc=nc;
				break;
			}
		}
		clistlock.unlock();
		if(tc==null)
		{
			tc=receive_join(a,muname);
		}
		switch(a[1])
		{
			case m_confirm:
				receive_confirm(a,tc);
				break;
			case m_leave:
				receive_leave(a,tc);
				break;
			case m_vframe:
				receive_vframe(a,tc);
				break;
			case m_ping:
				receive_ping(a,tc,msg.getSrc());
				break;
			case m_pingack:
				receive_pingack(a,tc);
				break;
			default:
				break;
		}
		manager.printlock.unlock();
	}
	
	public client receive_join(byte[] a,String muname)
	{
		manager.print("^-- join\n");
		Message msg=constructmessage(m_confirm,muname.getBytes(),0);
		sendmessage(msg,m_confirm);
		return makeclient(muname);
	}
	
	public void receive_confirm(byte[] a,client tc)
	{
		manager.print("^-- confirm\n");
		confirmed=true;
		tc.update();
	}
	
	public void receive_leave(byte[] a,client tc)
	{
		manager.print("^-- leave\n");
		tc.close(client.close_senderexit);
	}
	
	public void receive_vframe(byte[] a,client tc)
	{
		int ln=tc.username.length()+3;
		manager.print("^-- video frame - "+(a.length-ln)+" bytes\n");
		byte[] ll=new byte[8];
		System.arraycopy(a,ln,ll,0,8);
		long seq=byte_long(ll,0);
		manager.datalock.lock();
		manager.data_vfreceived++;
		manager.datalock.unlock();
		tc.showframe(a,ln+8,seq);
	}
	
	public void receive_ping(byte[] a,client tc,Address src)
	{
		long rtime=byte_long(a,tc.username.length()+3);
		manager.print("^-- ping - "+rtime+"\n");
		Message msg=constructmessage(m_pingack,long_byte(rtime),0);
		msg.setDest(src);
		sendmessage_stealthy(msg,m_pingack);
	}
	
	public void receive_pingack(byte[] a,client tc)
	{
		long rtime=byte_long(a,tc.username.length()+3);
		long span=System.currentTimeMillis()-rtime;
		long lat=span/2;
		manager.datalock.lock();
		manager.data_pingsum+=lat;
		manager.data_pingcount++;
		manager.datalock.unlock();
		manager.print("^-- ping ack - "+rtime+", "+span+"\n");
		if(manager.graphics)
		{
			tc.lastlat=lat;
		 tc.latencylabel.setText("Latency: "+lat+" milliseconds");
		}
	}
	
	public void viewAccepted(View view)
	{
		manager.print(servertext()+": Accepted a view.\n");
	}
	
	public void update()
	{
		if(!active || updating)
		{
			return;
		}
		manager.print(servertext()+": Updating.\n");
		updating=true;
		if(manager.graphics)
		{
	 	speedlabel.setText(datahtml());
		}
		if(going)
		{
			long currenttime=System.currentTimeMillis();
			long span=currenttime-lastframe;
			if(span>=framelength)
			{
				lastframe=currenttime;
				byte[] a=vstream.getnextframe();
				byte[] b=new byte[a.length+8];
				System.arraycopy(a,0,b,8,a.length);
				lastseq++;
				byte[] ll=long_byte(lastseq);
				System.arraycopy(ll,0,b,0,8);
				Message msg=constructmessage(m_vframe,b,manager.minmsize);
				sendmessage(msg,m_vframe);
			}
			span=currenttime-lastping;
			if(span>=pinglength)
			{
				lastping=currenttime;
				Message msg=constructmessage(m_ping,long_byte(System.currentTimeMillis()),0);
				sendmessage(msg,m_ping);
			}
		}
		clistlock.lock();
		for(client nc:clist)
		{
			nc.checktimeout();
		}
		clistlock.unlock();
		rlistlock.lock();
		if(outtimes.size()>0)
		{
			while(outtimes.get(0)<System.currentTimeMillis()-1000)
			{
				outtimes.remove(0);
				outspeed-=outsizes.remove(0).longValue();
			}
		}
		rlistlock.unlock();
		if(manager.graphics)
		{
	 	speedlabel.setText(datahtml());
		}
		updating=false;
	}
	
	public void threadedupdate()
	{
		updater=new genericlistener(genericlistener.r_updateserver,this);
		updater.start();
	}
	
	public client makeclient(String cuname)
	{
		clistlock.lock();
		client nc=new client(this,clustername,username,cuname,4000l,1l,showvideo);
		clist.add(nc);
		clistlock.unlock();
		return nc;
	}
	
	public void pause()
	{
		going=!going;
		if(manager.graphics)
		{
		 pausebutton.setText(going?"Pause":"Resume");
		}
		manager.print(servertext()+(going?": Resumed.\n":": Paused.\n"));
	}
	
	public void close()
	{
		while(updating)
		{
		}
		if(!active)
		{
			return;
		}
		active=false;
		Message msg=constructmessage(m_leave,new byte[0],0);
		sendmessage(msg,m_leave);
		clistlock.lock();
		ArrayList<client> tlist=new ArrayList<client>();
		tlist.addAll(clist);
		for(client nc:tlist)
		{
			nc.close(client.close_senderexit);
			clist.remove(nc);
		}
		clistlock.unlock();
		if(manager.graphics)
		{
			window.setVisible(false);
			window.setEnabled(false);
			window.dispose();
		}
		manager.slistlock.lock();
		manager.slist.remove(this);
		manager.slistlock.unlock();
		lchan.disconnect();
		lchan.close();
		manager.print(servertext()+": Closed.\n");
	}
	
	public void threadedclose()
	{
		genericlistener closer=new genericlistener(genericlistener.r_closeserver,this);
		closer.start();
	}
	
	public void kill()
	{
		while(updating)
		{
		}
		if(!active)
		{
			return;
		}
		clistlock.lock();
		ArrayList<client> tlist=new ArrayList<client>();
		tlist.addAll(clist);
		for(client nc:tlist)
		{
			nc.close(client.close_senderexit);
			clist.remove(nc);
		}
		clistlock.unlock();
		if(manager.graphics)
		{
			window.setVisible(false);
			window.setEnabled(false);
			window.dispose();
		}
		manager.slistlock.lock();
		manager.slist.remove(this);
		manager.slistlock.unlock();
		lchan.disconnect();
		lchan.close();
		manager.print(servertext()+": Killed.\n");
	}
	
	public String servertext()
	{
		return (username+"@"+clustername);
	}
	
	public String datahtml()
	{
		double tspeed=(double)outspeed;
		String modifier=" ";
		if(tspeed>1024)
		{
			tspeed/=1024;
			modifier=" kilo";
		}
		if(tspeed>1024)
		{
			tspeed/=1024;
			modifier=" mega";
		}
		if(tspeed>1024)
		{
			tspeed/=1024;
			modifier=" giga";
		}
		return ("<html>Data out: "+((int)tspeed)+modifier+"bytes/second<br>FPS: "+outtimes.size()+"/"+targetfps+"</html>");
	}
	
	public String datatext()
	{
		double tspeed=(double)outspeed;
		String modifier=" ";
		if(tspeed>1024)
		{
			tspeed/=1024;
			modifier=" kilo";
		}
		if(tspeed>1024)
		{
			tspeed/=1024;
			modifier=" mega";
		}
		if(tspeed>1024)
		{
			tspeed/=1024;
			modifier=" giga";
		}
		return ("Data out: "+((int)tspeed)+modifier+"bytes/second\nFPS: "+outtimes.size()+"/"+targetfps);
	}
	
	public static byte[] long_byte(long n)
	{
		byte[] a=new byte[8];
		for(int x=0;x<8;x++)
		{
			a[x]=(byte)(n>>>((7-x)*8));
		}
		return a;
	}
	
	public static long byte_long(byte[] a,int offset)
	{
		long n=0;
		for(int x=0;x<8;x++)
		{
			n=n|((((long)(a[x+offset]))&255)<<((7-x)*8));
		}
		return n;
	}
}
