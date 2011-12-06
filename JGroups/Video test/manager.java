import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.ArrayList;
import java.util.concurrent.locks.*;

public class manager
{
	public static long ticklength=5; //The number of milliseconds between clock ticks.
	public static long defaultframelength=3000;
	public static long defaultpinglength=1000;
	public static String defaultfilename="movie.Mjpeg";
	public static boolean running=true;
	public static int updatewaitcount=0;
	public static Font labelfont;
	public static Font buttonfont;
 public static Font boxfont;
 public static Font chatfont;
 public static Border standardborder;
 public static Insets defi;
	public static Toolkit deftk;
 public static ArrayList<server> slist;
	public static ReentrantLock slistlock;
	public static JFrame window;
	public static JTextArea cnamearea;
	public static JTextArea unamearea;
	public static JButton launchbutton;
	public static JButton closebutton;
	public static Timer updatetimer;
	
	public static void main(String[] args)
	{
		videostream.setup();
		labelfont=new Font("Arial",Font.BOLD,14);
		buttonfont=new Font("Arial",Font.BOLD,12);
		boxfont=new Font("Courier New",0,14);
		chatfont=new Font("Courier New",0,12);
		standardborder=BorderFactory.createLineBorder(new Color(0f,0f,0.5f),1);
		defi=new Insets(0,0,0,0);
		deftk=Toolkit.getDefaultToolkit();
		slist=new ArrayList<server>();
		slistlock=new ReentrantLock(true);
		window=new JFrame();
		window.setBounds(new Rectangle(48,48,384,192));
		window.setLayout(new GridBagLayout());
		window.setTitle("Video test");
		JLabel cnamelabel=new JLabel();
		cnamelabel.setText("Cluster name:");
		cnamelabel.setFont(labelfont);
		window.add(cnamelabel,new GridBagConstraints(0,0,1,1,0.5,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		JLabel unamelabel=new JLabel();
		unamelabel.setText("User name:");
		unamelabel.setFont(labelfont);
		window.add(unamelabel,new GridBagConstraints(0,1,1,1,0.5,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		cnamearea=new JTextArea();
		cnamearea.setFont(boxfont);
		cnamearea.setEditable(true);
		cnamearea.setBorder(standardborder);
		window.add(cnamearea,new GridBagConstraints(1,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		unamearea=new JTextArea();
		unamearea.setFont(boxfont);
		unamearea.setEditable(true);
		unamearea.setBorder(standardborder);
		window.add(unamearea,new GridBagConstraints(1,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		launchbutton=new JButton();
		launchbutton.setText("Launch");
		launchbutton.setFont(buttonfont);
		launchbutton.addActionListener(new genericlistener(genericlistener.a_launch,null));
		window.add(launchbutton,new GridBagConstraints(1,2,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		closebutton=new JButton();
		closebutton.setText("Close");
		closebutton.setFont(buttonfont);
		closebutton.addActionListener(new genericlistener(genericlistener.a_close,null));
		window.add(closebutton,new GridBagConstraints(1,3,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setEnabled(true);
		window.setVisible(true);
		updatetimer=new Timer((int)ticklength,new genericlistener(genericlistener.a_update,null));
		updatetimer.start();
		while(running)
		{
			try
			{
				Thread.sleep(10);
			}
			catch(InterruptedException e)
			{
			}
		}
	}
	
	public static void launch()
	{
		String ncname=validate(cnamearea.getText());
		String nuname=validate(unamearea.getText());
	 slistlock.lock();
		server ns=new server(ncname,nuname,defaultfilename,defaultframelength,defaultpinglength);
 	slist.add(ns);
 	slistlock.unlock();
	}
	
	public static void close()
	{
		updatetimer.stop();
		window.dispose();
	 slistlock.lock();
	 ArrayList<server> tlist=new ArrayList<server>();
	 tlist.addAll(slist);
 	for(server ns:tlist)
 	{
 		ns.close();
 	}
 	slistlock.unlock();
		running=false;
		System.exit(0);
	}
	
	public static String linize(String s)
	{
		return s.replaceAll("\n"," ");
	}
	
	public static String cutspaces(String s)
	{
		return s.replaceAll(" ","_");
	}
	
	public static String validate(String s)
	{
		String ns=cutspaces(linize(s));
		if(ns.equals("_"))
		{
			ns="__";
		}
		if(ns.length()>127)
		{
			return ns.substring(0,127);
		}
		return ns;
	}
	
	public static void update()
	{
		if(slistlock.tryLock())
		{
	 	for(server ns:slist)
	 	{
	 		ns.update();
	 	}
	 	slistlock.unlock();
		}
	}
}
