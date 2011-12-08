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
import java.util.zip.CRC32;

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
	static long pingStartTime, pingEndTime, throughputStartTime, clientStartTime;
	static int lastFrameNumber, framesReceived, bytesReceived, pingCount, bytesReceivedTotal = 0;
	static ImageIcon icon;
	static String serverAddress, clientID;
	static Agent clientAgent;
	static Logger loggerVideo, loggerPing, loggerInfo;
	static CRC32 crc32;
	static double pingTotal;

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
				double bps;

				while (true) {
					pingStartTime = System.currentTimeMillis();
					clientAgent.sendOneWay(serverAddress, "ping-client-"+clientID, "publish", pingParam);
					pingCount++;

					// calculate running throughput
					bps = bytesReceivedTotal / (double)((System.currentTimeMillis() - clientStartTime) / 1000);
					loggerInfo.fine("Average bps: "+ bps +". Total bytes received: "+bytesReceivedTotal +". Total messages received: "+framesReceived);
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				loggerPing.severe("Error in Client while attempting ping: "+e.getMessage());
			}
		}
	}

	private static class PingHandler implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im) throws Exception {
			long currentPingTime = System.currentTimeMillis() - pingStartTime;
			pingTotal += currentPingTime;
			loggerPing.fine("Ping RTT received: "+currentPingTime +"ms. Ping average: "+(pingTotal/pingCount) +"ms");
		}
	}

	private static class UpdateHandler implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im) throws Exception {
			framesReceived++;
			Parameters content = im.getParameters();
			byte[] frame = new byte[15000];
			frame = content.getBinary("videoFrame");
			int frameNum = content.getInteger("frameNum");
			long serverCRC = content.getLong("crc");
			String throughputCount = "";
			String crcMismatch = "";
			long currentTime = System.currentTimeMillis();

			if (lastFrameNumber > 0 && lastFrameNumber > frameNum && lastFrameNumber != 500 && frameNum != 1) // out of order frame received
				loggerVideo.severe("Out of order frame received.  Previous frame "+lastFrameNumber+", current frame "+frameNum);

			if ((currentTime - throughputStartTime) >= 1000) { // new second, restart count
				throughputCount = "Throughput for the last "+(currentTime - throughputStartTime)+"ms is "+ bytesReceived +" bytes";
				bytesReceived = 0;
				throughputStartTime = System.currentTimeMillis();
			}

			crc32.reset();
			crc32.update(frame);
			long clientCRC = crc32.getValue();

			if (clientCRC != serverCRC) // message is corrupt
				crcMismatch = ". Corrupt frame received.  Frame number "+frameNum+", server CRC "+ serverCRC +", client CRC "+clientCRC;

			bytesReceivedTotal += content.toString().length();
			bytesReceived += content.toString().length();
			loggerVideo.fine("Frame: "+ frameNum +" ("+clientCRC+"). Size: "+content.toString().length() +" bytes. "+ throughputCount + crcMismatch);
			lastFrameNumber = frameNum;

			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Image imageFrame = toolkit.createImage(frame, 0, frame.length);
			icon = new ImageIcon(imageFrame);
			iconLabel.setIcon(icon);
			crc32.reset();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean gui = true;

		if (args.length == 0) {
			System.out.println("Usage:");
			System.out.println("-s\t\tBroker address to connect to, without tcp://");
			System.out.println("--nogui\t\tDisable GUI");
			System.exit(2);
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i].toString().equals("-s")) {
				i++;
				serverAddress = "tcp://"+args[i];
				
			} else if (args[i].toString().equals("--nogui"))
				gui = false;
		}
		
		try {
			pingTotal = 0;
			clientID = ManagementFactory.getRuntimeMXBean().getName();
			clientStartTime = System.currentTimeMillis();
			crc32 = new CRC32();

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

		loggerInfo.info("Client initialized with server address tcp://"+serverAddress);

		if (gui)
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
			throughputStartTime = System.currentTimeMillis();

			Thread ping = new Pinger();
			ping.start();
			loggerInfo.info("Started Pinger thread");
		}
		catch (Exception e) {
			loggerInfo.severe("Client encountered an error: "+ e.getMessage());
		}
	}
}
