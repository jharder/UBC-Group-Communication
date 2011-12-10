import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.*;

public class manager
{
	public static boolean printing=true;
	public static boolean graphics=true;
	public static long ticklength=1; //The number of milliseconds between clock ticks.
	public static long defaultframelength=40;
	public static long defaultpinglength=1000;
	public static int minmsize=0;
	public static long snapshotlength=0; //The number of milliseconds between timed snapshots.
	public static String defaultfilename="movie.Mjpeg";
	public static String configfilename="config.txt";
	public static FileInputStream configstream;
	public static Scanner configscanner;
	public static FileOutputStream logout;
	public static ArrayList<String> defaultservers;
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
	public static long data_received=0;
	public static long data_vfreceived=0;
	public static long data_badseqno=0;
	public static long data_badchecksum=0;
	public static long data_pingsum=0;
	public static long data_pingcount=0;
	public static long data_bytes=0;
	public static long data_starttime;
	public static ReentrantLock datalock;
	public static JFrame window;
	public static JTextArea cnamearea;
	public static JTextArea unamearea;
	public static JButton launchbutton;
	public static JButton launchbuttonnv;
	public static JButton closebutton;
	public static JTextArea snapshotarea;
	public static JButton snapshotbutton;
	public static Timer updatetimer;
	public static Timer snapshottimer;
	public static ReentrantLock printlock;
	public static ReentrantLock loglock;
	
	public static void main(String[] args)
	{
		videostream.setup();
		boolean configuring=true;
		try
		{
			configstream=new FileInputStream(configfilename);
		}
		catch(FileNotFoundException e)
		{
			print("An error occurred while opening the config file.\n");
			configuring=false;
		}
		try
		{
			logout=new FileOutputStream("log.txt");
		}
		catch(FileNotFoundException e)
		{
			print("An error occurred while opening the log file.\n");
		}
		if(configuring)
		{
			configscanner=new Scanner(configstream);
			configscanner.nextLine();
			printing=(configscanner.nextInt()!=0);
			configscanner.nextLine();
			configscanner.nextLine();
			graphics=(configscanner.nextInt()!=0);
			configscanner.nextLine();
			configscanner.nextLine();
			ticklength=configscanner.nextLong();
			configscanner.nextLine();
			configscanner.nextLine();
			defaultframelength=configscanner.nextLong();
			configscanner.nextLine();
			configscanner.nextLine();
			defaultpinglength=configscanner.nextLong();
			configscanner.nextLine();
			configscanner.nextLine();
			minmsize=configscanner.nextInt();
			configscanner.nextLine();
			configscanner.nextLine();
			snapshotlength=configscanner.nextLong();
			configscanner.nextLine();
			configscanner.nextLine();
			defaultservers=new ArrayList<String>();
			while(true)
			{
				String nis=configscanner.nextLine();
				if(nis.indexOf(" ")<0)
				{
					break;
				}
				if(nis.indexOf("//")<0)
				{
				 defaultservers.add(nis);
				}
			}
		}
		slist=new ArrayList<server>();
		slistlock=new ReentrantLock(true);
		printlock=new ReentrantLock(true);
		datalock=new ReentrantLock(true);
		loglock=new ReentrantLock(true);
		if(graphics)
		{
			labelfont=new Font("Arial",Font.BOLD,14);
			buttonfont=new Font("Arial",Font.BOLD,12);
			boxfont=new Font("Courier New",0,14);
			chatfont=new Font("Courier New",0,12);
			standardborder=BorderFactory.createLineBorder(new Color(0f,0f,0.5f),1);
			defi=new Insets(0,0,0,0);
			deftk=Toolkit.getDefaultToolkit();
			window=new JFrame();
			window.setBounds(new Rectangle(48,48,512,256));
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
			launchbutton=new JButton("Launch");
			launchbutton.setFont(buttonfont);
			launchbutton.addActionListener(new genericlistener(genericlistener.a_launch,null));
			window.add(launchbutton,new GridBagConstraints(1,2,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
			launchbuttonnv=new JButton("Launch (videoless)");
			launchbuttonnv.setFont(buttonfont);
			launchbuttonnv.addActionListener(new genericlistener(genericlistener.a_launchnv,null));
			window.add(launchbuttonnv,new GridBagConstraints(1,3,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
			closebutton=new JButton("Close");
			closebutton.setFont(buttonfont);
			closebutton.addActionListener(new genericlistener(genericlistener.a_close,null));
			window.add(closebutton,new GridBagConstraints(1,4,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
			snapshotarea=new JTextArea();
			snapshotarea.setFont(labelfont);
			snapshotarea.setEditable(false);
			JScrollPane snapshotpane=new JScrollPane(snapshotarea);
			snapshotpane.setBorder(standardborder);
			snapshotpane.setEnabled(true);
			snapshotpane.setVisible(true);
			window.add(snapshotpane,new GridBagConstraints(2,0,1,4,5,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
			snapshotbutton=new JButton("Snapshot");
			snapshotbutton.setFont(buttonfont);
			snapshotbutton.addActionListener(new genericlistener(genericlistener.a_snapshot,null));
			window.add(snapshotbutton,new GridBagConstraints(2,4,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
			window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			window.setEnabled(true);
			window.setVisible(true);
		}
		if(snapshotlength>0)
		{
			snapshottimer=new Timer((int)snapshotlength,new genericlistener(genericlistener.a_timedsnapshot,null));
			snapshottimer.start();
		}
		updatetimer=new Timer((int)ticklength,new genericlistener(genericlistener.a_update,null));
		updatetimer.start();
		if(configuring)
		{
			for(String ndss:defaultservers)
			{
				int si=ndss.indexOf(" ");
				if(si<0)
				{
					continue;
				}
				String ncname=ndss.substring(0,si);
				String nuname=ndss.substring(si+1);
				server ns=launchcustom(ncname,nuname);
				ns.showvideo=false;
			}
		}
		datalock.lock();
		data_starttime=System.currentTimeMillis();
		data_received=0;
		data_vfreceived=0;
		data_badseqno=0;
		data_badchecksum=0;
		data_pingsum=0;
		data_pingcount=0;
		data_bytes=0;
		datalock.unlock();
		System.out.print("All default servers are now running.\n");
		System.out.print("####################################\n");
		mainloop();
		close();
	}
	
	public static void mainloop()
	{
		Scanner s=new Scanner(System.in);
		while(true)
		{
			String is=s.nextLine();
			if(is.equalsIgnoreCase("quit") || is.equalsIgnoreCase("close"))
			{
				System.out.print("Closing...\n");
				break;
			}
			else if(is.indexOf("s")==0)
			{
				System.out.print("Taking a snapshot...");
				loglock.lock();
				snapshot();
				System.out.print("done.\n");
			}
			else if(is.indexOf("l ")==0)
			{
				System.out.print("Launching a server...");
				int si=is.indexOf(" ",2);
				if(si<0)
				{
					continue;
				}
				String ncname=is.substring(2,si);
				String nuname=is.substring(si+1);
				server ns=launchcustom(ncname,nuname);
				ns.showvideo=false;
				System.out.print("done.\n");
			}
			else if(is.indexOf("c ")==0 || is.indexOf("k ")==0)
			{
				System.out.print("Closing a server...");
				int si=is.indexOf(" ",2);
				if(si<0)
				{
					continue;
				}
				String ncname=is.substring(2,si);
				String nuname=is.substring(si+1);
				server founds=null;
				slistlock.lock();
				for(server ns:slist)
				{
					if(ns.clustername.equals(ncname) && ns.username.equals(nuname))
					{
						founds=ns;
						break;
					}
				}
				slistlock.unlock();
				if(founds!=null)
				{
					if(is.indexOf("k ")==0)
					{
						founds.kill();
					}
					else
					{
			 		founds.close();
					}
				}
				System.out.print("done.\n");
			}
		}
	}
	
	public static String plural(int n)
	{
		return (n==1?"":"s");
	}
	
	public static void print(String s)
	{
		if(printing)
		{
			printlock.lock();
			System.out.print(s);
			printlock.unlock();
		}
	}
	
	public static void println(String s)
	{
		if(printing)
		{
			printlock.lock();
			System.out.println(s);
			printlock.unlock();
		}
	}
	
	public static void log(String logstring)
	{
		try
		{
			logout.write(logstring.getBytes());
		}
		catch(IOException e)
		{
			print("There was an error logging a string.\n");
		}
		try
		{
			logout.flush();
		}
		catch(IOException e)
		{
			print("There was an error flushing the log stream.\n");
		}
	}
	
	public static server launch()
	{
		String ncname=validate(cnamearea.getText());
		String nuname=validate(unamearea.getText());
	 slistlock.lock();
		server ns=new server(ncname,nuname,defaultfilename,defaultframelength,defaultpinglength);
 	slist.add(ns);
 	slistlock.unlock();
 	return ns;
	}
	
	public static server launch_novideo()
	{
		server ns=launch();
		ns.showvideo=false;
		return ns;
	}
	
	public static server launchcustom(String ncname,String nuname)
	{
	 slistlock.lock();
		server ns=new server(ncname,nuname,defaultfilename,defaultframelength,defaultpinglength);
 	slist.add(ns);
 	slistlock.unlock();
 	return ns;
	}
	
	public static void close()
	{
		updatetimer.stop();
		if(graphics)
		{
	 	window.dispose();
		}
	 slistlock.lock();
	 loglock.lock();
	 log("FINAL ");
	 snapshot();
	 loglock.lock();
	 ArrayList<server> tlist=new ArrayList<server>();
	 tlist.addAll(slist);
 	for(server ns:tlist)
 	{
 		ns.threadedclose();
 	}
 	slistlock.unlock();
		running=false;
		long data_endtime=System.currentTimeMillis();
		long data_runspan=data_endtime-data_starttime;
		double data_throughput=(data_runspan==0?0:((double)data_bytes)/(((double)data_runspan)/1000));
		double data_averageping=(data_pingcount==0?0:((double)data_pingsum)/((double)data_pingcount));
		String modifier=" ";
		if(data_throughput>1024)
		{
			data_throughput/=1024;
			modifier=" kilo";
		}
		if(data_throughput>1024)
		{
			data_throughput/=1024;
			modifier=" mega";
		}
		if(data_throughput>1024)
		{
			data_throughput/=1024;
			modifier=" giga";
		}
		log("TOTALS:\n\nRunning time: "+data_runspan+" milliseconds\n");
		log("Average throughput: "+data_throughput+modifier+"bytes/second\n");
		log("Average ping: "+data_averageping+" milliseconds\n");
		loglock.unlock();
		try
		{
			configstream.close();
		}
		catch(IOException e)
		{
			print("An error occurred while closing the config file.\n");
		}
		try
		{
			logout.close();
		}
		catch(IOException e)
		{
			System.out.print("An error occurred while closing the log file.\n");
		}
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
	 		if(!ns.updating)
	 		{
	 			ns.threadedupdate();
	 		}
	 	}
	 	slistlock.unlock();
		}
	}
	
	public static void snapshot()
	{
		ArrayList<String> sstext=new ArrayList<String>();
		sstext.add("Messages received: "+data_received+"\n");
		sstext.add("Video frames received: "+data_vfreceived+"\n");
		sstext.add("Bad checksums: "+data_badchecksum+" ("+(data_received==0?0:(((double)data_badchecksum)/((double)data_received)))+")\n");
		sstext.add("Bad frame seqnos: "+data_badseqno+" ("+(data_vfreceived==0?0:(((double)data_badseqno)/((double)data_vfreceived)))+")\n");
  int servercount=slist.size();
		sstext.add("\n"+servercount+" server"+plural(servercount)+" running\n");
		slistlock.lock();
 	for(server ns:slist)
 	{
 		sstext.add("\n"+ns.username+"@"+ns.clustername+":\n");
 		sstext.add(ns.datatext());
 		sstext.add("\n");
 		int clientcount=ns.clist.size();
 		sstext.add(clientcount+" client"+plural(clientcount)+" under this server\n");
			ns.clistlock.lock();
			for(client nc:ns.clist)
			{
				sstext.add("From "+nc.servername+":\n");
				sstext.add("Latency: "+nc.lastlat+" milliseconds");
				sstext.add("\n");
			}
			ns.clistlock.unlock();
 	}
		slistlock.unlock();
		if(graphics)
		{
			snapshotarea.setText("");
		}
		log("SNAPSHOT:\n\n");
		for(String s:sstext)
		{
			if(graphics)
			{
				snapshotarea.append(s);
			}
			log(s);
		}
		log("\n\n");
		loglock.unlock();
	}
}
