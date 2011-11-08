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
	public static final byte space=32;
	
	public String clustername;
	public String username;
	public String filename;
	public videostream vstream;
	public long framelength; //Time between sending frames, in milliseconds.
	public long pinglength; //Time between join messages, in milliseconds.
	public long lastframe; //The time of sending the last frame.
	public ArrayList<client> clist;
	public ReentrantLock clistlock;
	public JChannel lchan;
	public boolean confirmed;
	public JFrame window;
	public JLabel statuslabel;
	
	public server(String pclustername,String pusername,String pfilename,long pframelength,long ppinglength)
	{
		clustername=pclustername;
		username=pusername;
		filename=pfilename;
		vstream=new videostream(filename,null);
		framelength=pframelength;
		pinglength=ppinglength;
		lastframe=System.currentTimeMillis();
		clist=new ArrayList<client>();
		clistlock=new ReentrantLock(true);
		try
		{
			lchan=new JChannel("videotest.xml");
		}
		catch(Exception e)
		{
			System.out.print("An error occurred while creating a channel.\n");
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
				System.out.print("An error occurred while connecting on a channel.\n");
			}
			manager.slist.add(this);
		}
		confirmed=false;
		window=new JFrame();
		window.setBounds(new Rectangle(96,96,512,256));
		window.setLayout(new GridBagLayout());
		window.setTitle("Server: "+username+"@"+clustername);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		statuslabel=new JLabel("Running...");
		window.add(statuslabel);
		window.setEnabled(true);
		window.setVisible(true);
	}
	
	public byte[] constructdata(byte type,byte[] a)
	{
		byte[] data=new byte[username.length()+a.length+2];
		data[0]=type;
		byte[] usdata=username.getBytes();
		System.arraycopy(usdata,0,data,1,usdata.length);
		data[usdata.length+1]=space;
		System.arraycopy(a,0,data,usdata.length+2,a.length);
		return data;
	}
	
	public Message constructmessage(byte type,byte[] a)
	{
		Message msg=new Message(null,null,constructdata(type,a));
		return msg;
	}
	
	public void sendmessage(Message msg,byte type)
	{
		try
		{
			lchan.send(msg);
		}
		catch(Exception e)
		{
			System.out.print("An error occurred while trying to send a message ("+clustername+":"+username+", "+type+").\n");
		}
	}
	
	public void receive(Message msg)
	{
		super.receive(msg);
		byte[] a=msg.getRawBuffer();
		if(a.length<=1)
		{
			return;
		}
		byte[] mua="_".getBytes();
		int i=1;
		while(i<a.length)
		{
			if(a[i]==space)
			{
				i--;
				mua=new byte[i];
				System.arraycopy(a,1,mua,0,i);
				break;
			}
			i++;
		}
		String muname=new String(mua);
		System.out.print("Received a message ("+clustername+", "+username+", "+muname+").\n");
		if(muname.equals(username))
		{
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
		switch(a[0])
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
			default:
				break;
		}
	}
	
	public client receive_join(byte[] a,String muname)
	{
		Message msg=constructmessage(m_confirm,muname.getBytes());
		sendmessage(msg,m_confirm);
		return makeclient(muname);
	}
	
	public void receive_confirm(byte[] a,client tc)
	{
		confirmed=true;
		tc.update();
	}
	
	public void receive_leave(byte[] a,client tc)
	{
		tc.close(client.close_senderexit);
	}
	
	public void receive_vframe(byte[] a,client tc)
	{
		tc.showframe(a,tc.username.length()+2);
	}
	
	public void viewAccepted(View view)
	{
		super.viewAccepted(view);
	}
	
	public void update()
	{
		long currenttime=System.currentTimeMillis();
		long span=currenttime-lastframe;
		if(span>=framelength)
		{
			lastframe=currenttime;
			byte[] a=vstream.getnextframe();
			Message msg=constructmessage(m_vframe,a);
			sendmessage(msg,m_vframe);
		}
		clistlock.lock();
		for(client nc:clist)
		{
			nc.checktimeout();
		}
		clistlock.unlock();
	}
	
	public client makeclient(String cuname)
	{
		clistlock.lock();
		client nc=new client(this,clustername,username,cuname,4000);
		clist.add(nc);
		clistlock.unlock();
		return nc;
	}
	
	public void close()
	{
		Message msg=constructmessage(m_leave,new byte[0]);
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
		window.setVisible(false);
		window.setEnabled(false);
		window.dispose();
		manager.slistlock.lock();
		manager.slist.remove(this);
		manager.slistlock.unlock();
		lchan.disconnect();
		lchan.close();
	}
}
