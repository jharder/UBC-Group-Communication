import java.awt.event.*;

public class genericlistener extends Thread implements ActionListener
{
	//Listener constants:
	public static final int a_launch=0;
	public static final int a_close=1;
	public static final int a_update=2;
	public static final int a_serverpause=3;
	public static final int a_serverclose=4;
	public static final int a_serverkill=5;
	public static final int a_launchnv=6;
	public static final int a_snapshot=7;
	public static final int a_timedsnapshot=8;
	//Thread constants:
	public static final int r_updateserver=0;
	public static final int r_closeserver=1;
	//Other:
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
			case a_serverpause:
				((server)target).pause();
				break;
			case a_serverclose:
				((server)target).close();
				break;
			case a_serverkill:
				((server)target).kill();
				break;
			case a_launchnv:
				manager.launch_novideo();
				break;
			case a_snapshot:
				manager.snapshot();
				break;
			case a_timedsnapshot:
				manager.loglock.lock();
				manager.log("TIMED ");
				manager.snapshot();
				manager.print("Took a timed snapshot.\n");
				break;
			default:
				break;
		}
	}
	
	public void run()
	{
		switch(type)
		{
			case r_updateserver:
				((server)target).update();
				break;
			case r_closeserver:
				((server)target).close();
				break;
			default:
				break;
		}
	}
}
