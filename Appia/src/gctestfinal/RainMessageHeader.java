package gctestfinal;

import net.sf.appia.core.message.Message;
import net.sf.appia.core.message.SerializableObject;

public class RainMessageHeader implements SerializableObject {
    
	public RainMessage message;
	public int number;
	
	public RainMessageHeader() {
		super();
	}

	public RainMessageHeader(Message m) {
		super();
		popMySelf(m);
	}

	public RainMessageHeader(RainMessage m, int n) {
		super();
		message = m;
		number = n;
	}

	public void pushMySelf(Message m) {
		m.pushObject(message);
		m.pushInt(number);
	}

	public void popMySelf(Message m) {
		number = m.popInt();
		message = (RainMessage) m.popObject();
	}

	public void peekMySelf(Message m) {
		number = m.peekInt();
		message = (RainMessage) m.peekObject();
	}

}
