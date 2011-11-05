/**
 * 
 */
package yami;

import com.inspirel.yami.Agent;
import com.inspirel.yami.OutgoingMessage;
import com.inspirel.yami.Parameters;

/**
 * @author Jared Harder
 *
 */
public class Client {

	public Client() {}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String serverAddress = "tcp://127.0.0.1:3334";

		try {
			Agent clientAgent = new Agent();
			Parameters param = new Parameters();
			param.setLong("theTime", System.currentTimeMillis());

			OutgoingMessage message = clientAgent.send(serverAddress, "server", "test", param);
			message.waitForCompletion();

			OutgoingMessage.MessageState state = message.getState();

			if (state == OutgoingMessage.MessageState.REPLIED) {
				Parameters reply = message.getReply();
				
				long[] replyArray = reply.getLongArray("reply");

				System.out.print("The time we sent was "+ replyArray[0] +" and the time we received was "+ replyArray[1]);
			} else if (state ==
					OutgoingMessage.MessageState.REJECTED) {

				System.out.println(
						"The message has been rejected: " +
								message.getExceptionMsg());
			} else {
				System.out.println(
						"The message has been abandoned.");
			}
			
			message.close();
			clientAgent.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
