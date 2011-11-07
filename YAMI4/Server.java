/**
 * 
 */
package yami;

import com.inspirel.yami.Agent;
import com.inspirel.yami.Parameters;
import com.inspirel.yami.ValuePublisher;
import com.inspirel.yami.YAMIIOException;

/**
 * @author Jared Harder
 *
 */
public class Server {
	static VideoStream video;

	public Server() {}

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		String serverAddress = "tcp://127.0.0.1:3334";
		byte[] buf = new byte[15000];

		try {
			video = new VideoStream("C:\\movie.Mjpeg");

			Agent serverAgent = new Agent();
			Parameters param = new Parameters();
			ValuePublisher valPub = new ValuePublisher();

			serverAgent.addListener(serverAddress);
			serverAgent.registerValuePublisher("server", valPub);

			while (true) {
				video.getNextFrame(buf);
				//param.setLong("time", System.currentTimeMillis());
				param.setBinary("videoFrame", buf);
				valPub.publish(param);

				Thread.sleep(100);
			}
		} catch (YAMIIOException e) {
			System.out.println("server: "+e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
