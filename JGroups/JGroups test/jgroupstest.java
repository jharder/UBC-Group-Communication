import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.ArrayList;

public class jgroupstest
{
 public static Font mainfont;
 public static Font chatfont;
 public static Border standardborder;
 public static ArrayList<receiver> rlist;
 public static JFrame window;
 public static JTextArea launchn;
 public static JTextArea username;
 public static JButton launchb;
 public static Insets defi;
 
	public static void main(String[] args)
	{
		//Initialize stuff
		mainfont=new Font("Courier New",0,14);
		chatfont=new Font("Courier New",0,12);
		standardborder=BorderFactory.createLineBorder(new Color(0f,0f,0.5f),1);
		rlist=new ArrayList<receiver>();
		//Set up GUI
		window=new JFrame();
		window.setBounds(new Rectangle(200,200,400,120));
		launchn=new JTextArea();
		launchn.setFont(mainfont);
		launchn.setBorder(standardborder);
		username=new JTextArea();
		username.setFont(mainfont);
		username.setBorder(standardborder);
		launchb=new JButton();
		launchb.setText("Launch");
		launchb.addActionListener(new launchlistener());
		JPanel launchp=new JPanel(new GridBagLayout());
		defi=new Insets(0,0,0,0);
		launchp.add(new JLabel("Cluster name:"),new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		launchp.add(new JLabel("User name:"),new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		launchp.add(launchn,new GridBagConstraints(1,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		launchp.add(username,new GridBagConstraints(1,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		launchp.add(launchb,new GridBagConstraints(1,2,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,defi,0,0));
		window.add(launchp);
		window.setTitle("JGroups test");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setEnabled(true);
		window.setVisible(true);
		while(true)
		{
			try
			{
				Thread.sleep(10);
			}
			catch(InterruptedException never_used_exception)
			{
				System.out.print("There was an InterruptedException, but we don't care.\n");
			}
		}
	}
	
	public static String snl(String s)
	{
		return s.replaceAll("\n"," ");
	}
	
	public static void launch()
	{
		String nstring=snl(launchn.getText());
		int id=1;
		for(receiver nr:rlist)
		{
			if(nr.clustername.equals(nstring))
			{
				id++;
			}
		}
		receiver newr=new receiver(nstring+" ("+id+")",snl(username.getText()),nstring);
		rlist.add(newr);
		newr.start();
	}
}
