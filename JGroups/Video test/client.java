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
	public JFrame window;
	public JLabel imagelabel;
	public long lastupdate;
	public long timeout;
	public long closeout;
	public boolean active;
	
	public client(server pparent,String pclustername,String pusername,String pservername,long ptimeout)
	{
		parent=pparent;
		clustername=pclustername;
		username=pusername;
		servername=pservername;
		lastupdate=System.currentTimeMillis();
		timeout=ptimeout;
		closeout=timeout*closeconstant;
		window=new JFrame();
		window.setBounds(new Rectangle(256,256,480,400));
		window.setLayout(new GridBagLayout());
		window.setTitle("Client: "+pservername+"@"+clustername+" -> "+username);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		imagelabel=new JLabel();
		window.add(imagelabel);
		window.setEnabled(true);
		window.setVisible(true);
		active=true;
	}
	
	public void update()
	{
 	lastupdate=System.currentTimeMillis();
		active=true;
		imagelabel.setText("");
	}
	
	public void checktimeout()
	{
		long span=System.currentTimeMillis()-lastupdate;
		if(span>=closeout)
		{
			close(close_sendercrash);
		}
		else if(span>=timeout)
		{
			imagelabel.setIcon(null);
			imagelabel.setText("Sender has gone inactive, waiting for response...");
		}
	}
	
	public void showframe(byte[] a,int offset)
	{
		update();
		Image nimg=manager.deftk.createImage(a,offset,a.length-offset);
		Icon nic=new ImageIcon(nimg);
		imagelabel.setIcon(nic);
		imagelabel.setText("");
	}
	
	public void close(int reason)
	{
		if(!active)
		{
			return;
		}
		active=false;
		window.setVisible(false);
		window.setEnabled(false);
		window.dispose();
	}
}
