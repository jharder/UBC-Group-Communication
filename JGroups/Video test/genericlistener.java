import java.awt.event.*;

public class genericlistener implements ActionListener
{
	public static final int a_launch=0;
	public static final int a_close=1;
	public static final int a_update=2;
	public int type;
	public Object target;
	
	public genericlistener(int ptype,Object ptarget)
	{
		super();
		type=ptype;
		target=ptarget;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		switch(type)
		{
			case a_launch:
				manager.launch();
				break;
			case a_close:
				manager.close();
				break;
			case a_update:
				manager.update();
				break;
			default:
				break;
		}
	}
}
