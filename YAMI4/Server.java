/**
 * 
 */
package yami;

import com.inspirel.yami.Agent;
import com.inspirel.yami.IncomingMessage;
import com.inspirel.yami.IncomingMessageCallback;
import com.inspirel.yami.Parameters;
import com.inspirel.yami.YAMIIOException;

/**
 * @author Jared Harder
 *
 */
public class Server {
	public Server() {}
	
	private static class Messager implements IncomingMessageCallback {
		@Override
		public void call(IncomingMessage im) throws Exception {
			Parameters param = im.getParameters();
			
			long time = param.getLong("theTime");
			
			System.out.println("The server received a time of "+ time +". Replying...");
			Thread.sleep(2000);
			
			Parameters replyParam = new Parameters();
			long[] reply = {time, System.currentTimeMillis()};
			replyParam.setLongArray("reply", reply);
			
			im.reply(replyParam);
		}
	}

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		String serverAddress = "tcp://127.0.0.1:3334";
		
		try {
			Agent serverAgent = new Agent();
			serverAgent.addListener(serverAddress);
			serverAgent.registerObject("server", new Messager());
			
			while (true) {
				Thread.sleep(10000);
			}
		} catch (YAMIIOException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		}
	}
}
