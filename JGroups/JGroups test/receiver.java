import java.awt.*;
import javax.swing.*;
import org.jgroups.*;

public class receiver extends ReceiverAdapter
{
	public String title;
	public String username;
	public String clustername;
 public JFrame window;
 public JTextArea chatbox;
 public JScrollPane cbscroll;
 public JTextArea chatline;
 public JScrollPane clscroll;
 public JButton sendb;
 public JButton closeb;
 public JLabel loginl;
 public JChannel channel;
 public static final String t_message="m";
	
	public receiver(String ptitle,String puname,String pcname)
	{
		super();
		title=ptitle;
		username=puname;
		clustername=pcname;
		window=new JFrame();
		window.setBounds(new Rectangle((int)(Math.random()*500),(int)(Math.random()*500),500,500));
		chatbox=new JTextArea();
		chatbox.setEditable(false);
		chatbox.setFont(jgroupstest.chatfont);
		cbscroll=new JScrollPane(chatbox);
		cbscroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		cbscroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		cbscroll.setBorder(jgroupstest.standardborder);
		chatline=new JTextArea();
		chatline.setFont(jgroupstest.chatfont);
		clscroll=new JScrollPane(chatline);
		clscroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		clscroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		clscroll.setBorder(jgroupstest.standardborder);
		sendb=new JButton();
		sendb.setText("Send");
		sendb.addActionListener(new rlistener(this,rlistener.t_send));
		closeb=new JButton();
		closeb.setText("Close");
		closeb.addActionListener(new rlistener(this,rlistener.t_close));
		loginl=new JLabel("Logged in as: "+username);
		JPanel recpanel=new JPanel(new GridBagLayout());
		recpanel.add(loginl,new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,jgroupstest.defi,0,0));
		recpanel.add(closeb,new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,jgroupstest.defi,0,0));
		recpanel.add(cbscroll,new GridBagConstraints(0,2,1,1,1,10,GridBagConstraints.CENTER,GridBagConstraints.BOTH,jgroupstest.defi,0,0));
		recpanel.add(clscroll,new GridBagConstraints(0,3,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,jgroupstest.defi,0,0));
		recpanel.add(sendb,new GridBagConstraints(0,4,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,jgroupstest.defi,0,0));
		window.add(recpanel);
		window.setTitle(title);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setEnabled(true);
		window.setVisible(true);
	}
	
	public void start()
	{
		try
		{
			channel=new JChannel("jgroupstest.xml");
		}
		catch(Exception e)
		{
			System.out.println("An error occurred while creating a channel.\n");
			close();
			return;
		}
		channel.setReceiver(this);
		try
		{
			channel.connect(clustername);
		}
		catch(Exception e)
		{
			System.out.println("An error occurred while connecting on a channel.\n");
		}
	}
	
	public void send()
	{
		String ss=t_message+username+": "+jgroupstest.snl(chatline.getText());
		System.out.print("Trying to send message: "+ss+"\n");
		byte[] sa=ss.getBytes();
		Message sm=new Message(null,null,sa);
		try
		{
			channel.send(sm);
		}
		catch(Exception e)
		{
			System.out.println("An error occurred while sending a message.\n");
		}
		chatline.setText("");
	}
	
	public void close()
	{
		window.setVisible(false);
		window.dispose();
		channel.disconnect();
		channel.close();
		jgroupstest.rlist.remove(this);
	}
	
	public void vewAccepted(View newview)
	{
		System.out.println(newview.toString());
	}
	
	public void receive(Message msg)
	{
		String rs=new String(msg.getRawBuffer());
		System.out.print("Received message: "+rs+"\n");
		if(rs.indexOf(t_message)==0)
		{
			String rt=rs.substring(1);
			chatbox.append(rt+"\n");
		}
	}
}
