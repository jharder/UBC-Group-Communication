package yami;

import java.lang.management.ManagementFactory;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import com.inspirel.yami.Agent;
import com.inspirel.yami.IncomingMessage;
import com.inspirel.yami.IncomingMessageCallback;
import com.inspirel.yami.Parameters;

/**
 * @author Jared Harder
 *
 */
public class Server {
	static VideoStream video;
	static int frameNum;
	static Agent serverAgent;
	static String serverAddress;
	static Logger loggerVideo, loggerPing, loggerInfo;

	private static class PingHandler implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im) throws Exception {
			Parameters content = im.getParameters();
			String clientID = content.getString("clientID");
			serverAgent.sendOneWay(serverAddress, "ping-server-"+clientID, "publish", null);
			loggerPing.fine("Ping request from ping-client-"+clientID+" group. Replied to ping-server"+clientID+" group.");
		}
	}

	private static class UpdateHandler implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im) throws Exception {
			Parameters content = im.getParameters();
			Parameters pingParam = new Parameters();
			String clientID = content.getString("clientID");
			pingParam.setString("destination_object", "ping_handler");
			serverAgent.sendOneWay(serverAddress, "ping-client-"+clientID, "subscribe", pingParam);
			loggerPing.info("Subscribe to ping-client-"+clientID+" group");
		}
	}

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		try {
			String serverID = ManagementFactory.getRuntimeMXBean().getName();

			FileHandler serverLogVideo = new FileHandler("server-"+serverID+"-video.log");
			FileHandler serverLogPing = new FileHandler("server-"+serverID+"-ping.log");
			FileHandler serverLogInfo = new FileHandler("server-"+serverID+"-info.log");
			loggerVideo = Logger.getLogger("videoLogger");
			loggerPing = Logger.getLogger("pingLogger");
			loggerInfo = Logger.getLogger("InfoLogger");
			loggerVideo.setLevel(Level.ALL);
			loggerPing.setLevel(Level.ALL);
			loggerInfo.setLevel(Level.ALL);
			loggerVideo.addHandler(serverLogVideo);
			loggerPing.addHandler(serverLogPing);
			loggerInfo.addHandler(serverLogInfo);
			YamiFormatter formatter = new YamiFormatter();
			serverLogVideo.setFormatter(formatter);
			serverLogPing.setFormatter(formatter);
			serverLogInfo.setFormatter(formatter);
		} catch (Exception e) {
			System.out.println("Server: Error setting up the loggers: "+e.getMessage());
			System.exit(1);
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i].toString().equals("-s")) {
				i++;
				serverAddress = "tcp://"+args[i];
				loggerInfo.fine("Server initialized with server address "+serverAddress);
			} else {
				System.exit(2);
			}
		}

		byte[] buf = new byte[15000];

		try {
			video = new VideoStream("C:\\movie.Mjpeg");

			serverAgent = new Agent();
			Parameters param = new Parameters();
			Parameters pingParam = new Parameters();
			serverAgent.registerObject("update_handler", new UpdateHandler());
			loggerInfo.info("Registered update_handler");
			serverAgent.registerObject("ping_handler", new PingHandler());
			loggerInfo.info("Registered ping_handler");
			pingParam.setString("destination_object", "update_handler");
			serverAgent.sendOneWay(serverAddress, "ping-client", "subscribe", pingParam);
			loggerPing.info("Subscribe to ping-client group");
			pingParam.clear();

			CRC32 crc = new CRC32();
			long crcValue;

			while (true) {
				frameNum = video.getNextFrame(buf);
				crc.reset();
				crc.update(buf);
				crcValue = crc.getValue();
				crc.reset();
				param.setLong("crc", crcValue);
				param.setBinary("videoFrame", buf);
				param.setInteger("frameNum", frameNum);

				loggerVideo.fine("Frame: "+ frameNum +" ("+crcValue+"). Size: "+param.toString().length() +" bytes");
				serverAgent.sendOneWay(serverAddress, "video", "publish", param);

				Thread.sleep(40);
			}
		} catch (Exception e) {
			loggerInfo.severe("Server encountered an error: "+ e.getMessage());
		}
	}
}
