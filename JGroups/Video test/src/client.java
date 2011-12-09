import java.awt.*;
import javax.swing.*;

public class client
{
	public static final int close_senderexit=0;
	public static final int close_sendercrash=1;
	public static final int close_serverexit=2;
	public static int closeconstant=5; //closeout = timeout*closeconstant
	
	public server parent;
	public String clustername;
	public String username;
	public String servername;
	public long framelength; //The expected time between receiving frames.
	public long lastframe; //The time of receiving the last frame.
	public long lastseq; //The sequence number of the last frame.
	public boolean hasseq;
	public long lastlat;
	public JFrame window;
	public JLabel imagelabel;
	public JLabel latencylabel;
	public long lastupdate;
	public long timeout;
	public long closeout;
	public boolean active;
	public boolean showvideo;
	
	public client(server pparent,String pclustername,String pusername,String pservername,long ptimeout,long pframelength,boolean pshowvideo)
	{
		parent=pparent;
		clustername=pclustername;
		username=pusername;
		servername=pservername;
		framelength=pframelength;
		lastupdate=System.currentTimeMillis();
		lastframe=System.currentTimeMillis();
		lastseq=0;
		hasseq=false;
		lastlat=0;
		timeout=ptimeout;
		closeout=timeout*closeconstant;
		showvideo=pshowvideo;
		if(manager.graphics)
		{
			window=new JFrame();
			window.setLayout(new GridBagLayout());
			window.setTitle("Client: "+pservername+"@"+clustername+" -> "+username);
			window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			if(showvideo)
			{
				window.setBounds(new Rectangle(256,256,480,480));
				imagelabel=new JLabel();
				window.add(imagelabel,new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
				latencylabel=new JLabel();
				window.add(latencylabel,new GridBagConstraints(0,1,1,1,1,0.2,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
			}
			else
			{
				window.setBounds(new Rectangle(256,256,384,128));
				latencylabel=new JLabel();
				window.add(latencylabel,new GridBagConstraints(0,0,1,1,1,0.2,GridBagConstraints.CENTER,GridBagConstraints.BOTH,manager.defi,0,0));
			}
			window.setEnabled(true);
			window.setVisible(true);
		}
		active=true;
	}
	
	public void update()
	{
 	lastupdate=System.currentTimeMillis();
		active=true;
		if(showvideo && manager.graphics)
		{
		 imagelabel.setText("");
		}
	}
	
	public void checktimeout()
	{
		long span=System.currentTimeMillis()-lastupdate;
		if(span>=closeout)
		{
			close(close_sendercrash);
		}
		else if(span>=timeout && manager.graphics)
		{
			if(showvideo)
			{
			 imagelabel.setIcon(null);
			 imagelabel.setText("Sender has gone inactive, waiting for response...");
			}
			else
			{
			 latencylabel.setText("Sender has gone inactive, waiting for response...");
			}
		}
	}
	
	public void showframe(byte[] a,int offset,long seq)
	{
		update();
		long span=System.currentTimeMillis()-lastframe;
		if(span<framelength)
		{
			try
			{
				Thread.sleep(framelength-span);
			}
			catch(InterruptedException e)
			{
				System.out.print("An error occurred while sleeping in client.showframe().\n");
			}
		}
		if(showvideo && manager.graphics)
		{
			Image nimg=manager.deftk.createImage(a,offset,a.length-offset);
			Icon nic=new ImageIcon(nimg);
			imagelabel.setIcon(nic);
			imagelabel.setText("");
		}
		if(seq-lastseq!=1 && (seq!=Long.MIN_VALUE || lastseq!=Long.MAX_VALUE) && hasseq)
		{
			manager.print("^-- "+username+"@"+clustername+" < - "+servername+": Frame had bad seqno (expected "+(lastseq+1)+", got "+seq+").\n");
			manager.datalock.lock();
			manager.data_badseqno++;
			manager.datalock.unlock();
		}
		lastseq=seq;
		hasseq=true;
	}
	
	public void close(int reason)
	{
		if(!active)
		{
			return;
		}
		active=false;
		if(manager.graphics)
		{
			window.setVisible(false);
			window.setEnabled(false);
			window.dispose();
		}
	}
}
