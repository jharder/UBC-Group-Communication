package yami;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.management.ManagementFactory;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	static long startTime, endTime, throughputStart;
	static int throughput;
	static ImageIcon icon;
	static String serverAddress, clientID;
	static Agent clientAgent;
	static Logger loggerVideo, loggerPing, loggerInfo;

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

	private static class Pinger extends Thread {
		public void run() {
			try {
				Parameters pingParam = new Parameters();
				pingParam.setString("clientID", clientID);

				while (true) {
					startTime = System.currentTimeMillis();
					clientAgent.sendOneWay(serverAddress, "ping-client-"+clientID, "publish", pingParam);
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				loggerPing.severe("Error in Client while attempting ping");
			}
		}
	}

	private static class PingHandler implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im) throws Exception {
			loggerPing.fine("Ping RTT received: "+(System.currentTimeMillis() - startTime) +"ms");
		}
	}

	private static class UpdateHandler implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im) throws Exception {
			Parameters content = im.getParameters();
			byte[] frame = content.getBinary("videoFrame");
			int frameNum = content.getInteger("frameNum");
			long crc = content.getLong("crc");
			String throughputCount = "";
			long currentTime = System.currentTimeMillis();

			if ((currentTime - throughputStart) >= 1000) { // new second, restart count
				throughputCount = "Throughput for the last "+(currentTime - throughputStart)+"ms is "+ throughput +" bytes";
				throughput = 0;
				throughputStart = System.currentTimeMillis();
			}

			throughput += content.toString().length();
			loggerVideo.log(Level.FINE, "Frame: "+ frameNum +" ("+crc+"). Size: "+content.toString().length() +" bytes. "+ throughputCount);

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
		try {
			clientID = ManagementFactory.getRuntimeMXBean().getName();
			
			FileHandler clientLogVideo = new FileHandler("client"+clientID+"-video.log");
			FileHandler clientLogPing = new FileHandler("client"+clientID+"-ping.log");
			FileHandler clientLogInfo = new FileHandler("client"+clientID+"-info.log");
			loggerVideo = Logger.getLogger("videoLogger");
			loggerPing = Logger.getLogger("pingLogger");
			loggerInfo = Logger.getLogger("InfoLogger");
			loggerVideo.setLevel(Level.ALL);
			loggerPing.setLevel(Level.ALL);
			loggerInfo.setLevel(Level.ALL);
			loggerVideo.addHandler(clientLogVideo);
			loggerPing.addHandler(clientLogPing);
			loggerInfo.addHandler(clientLogInfo);
			YamiFormatter formatter = new YamiFormatter();
			clientLogVideo.setFormatter(formatter);
			clientLogPing.setFormatter(formatter);
			clientLogInfo.setFormatter(formatter);
		} catch (Exception e) {
			System.out.println("Error setting up the loggers: "+e.getMessage());
			System.exit(1);
		}

		for (int i = 0; i < args.length; i++) {
			System.out.println(args[0]);
			if (args[i].toString().equals("-s")) {
				serverAddress = "tcp://"+args[1];
				loggerInfo.info("Client initialized with server address "+serverAddress);
				i++;
			} else
				System.exit(2);
		}

		new Client();

		try {
			clientAgent = new Agent();
			Parameters param = new Parameters();
			Parameters pingParam = new Parameters();


			clientAgent.registerObject("update_handler", new UpdateHandler());
			loggerInfo.info("Registered update_handler");
			clientAgent.registerObject("ping_handler", new PingHandler());
			loggerInfo.info("Registered ping_handler");

			param.setString("destination_object", "update_handler");
			clientAgent.sendOneWay(serverAddress, "video", "subscribe", param);
			loggerVideo.info("Subscribed to video group");
			pingParam.setString("clientID", clientID);
			clientAgent.sendOneWay(serverAddress, "ping-client", "publish", pingParam);
			loggerPing.info("Published clientID to ping-client group");
			pingParam.clear();
			pingParam.setString("destination_object", "ping_handler");
			clientAgent.sendOneWay(serverAddress, "ping-server-"+clientID, "subscribe", pingParam);
			loggerPing.info("subscribed to ping-server-"+clientID+" group");
			throughputStart = System.currentTimeMillis();

			Thread ping = new Pinger();
			ping.start();
			loggerInfo.info("Started Pinger thread");
		}
		catch (Exception e) {
			loggerInfo.severe("Client encountered an error: "+ e.getMessage());
		}
	}
}
