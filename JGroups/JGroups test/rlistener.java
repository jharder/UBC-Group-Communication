import java.awt.event.*;

public class rlistener implements ActionListener
{
	public receiver lr;
	public int type;
	
	public static final int t_send=1;
	public static final int t_close=0;
	
	public rlistener(receiver plr,int ptype)
	{
		lr=plr;
		type=ptype;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(type==t_send)
		{
			lr.send();
		}
	 else if(type==t_close)
		{
	 	lr.close();
		}
	}
}