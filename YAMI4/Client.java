/**
 * 
 */
package yami;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.inspirel.yami.Agent;
import com.inspirel.yami.IncomingMessage;
import com.inspirel.yami.IncomingMessageCallback;
import com.inspirel.yami.Parameters;

/**
 * @author Jared Harder
 *
 */
public class Client {
	//GUI
	//----
	JFrame f = new JFrame("Client");
	JPanel mainPanel = new JPanel();
	static JLabel iconLabel = new JLabel();
	static ImageIcon icon;

	public Client() {
		//build GUI
		//--------------------------

		//Frame
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		//Image display label
		iconLabel.setIcon(null);

		//frame layout
		mainPanel.setLayout(null);
		mainPanel.add(iconLabel);
		iconLabel.setBounds(0,0,380,280);

		f.getContentPane().add(mainPanel, BorderLayout.CENTER);
		f.setSize(new Dimension(380,315));
		f.setVisible(true);
	}

	private static class UpdateHandler implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im)
				throws Exception {

			Parameters content = im.getParameters();

			byte[] frame = content.getBinary("videoFrame");
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Image imageFrame = toolkit.createImage(frame, 0, frame.length);
			icon = new ImageIcon(imageFrame);
			iconLabel.setIcon(icon);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Client();
		String serverAddress = "tcp://192.168.1.65:3334";

		try {
			Agent clientAgent = new Agent();
			Parameters param = new Parameters();

			clientAgent.registerObject("update_handler", new UpdateHandler());
			param.setString("destination_object", "update_handler");
			clientAgent.sendOneWay(serverAddress, "server", "subscribe", param);

			while (true)
				Thread.sleep(10000);
		}
		catch (Exception e) {
			System.out.println("client: "+e.getMessage());
		}
	}
}
