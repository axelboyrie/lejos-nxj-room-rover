package org.lejos.rover;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("unchecked")
public class MessageCoder {
	private static int KEEPALIVE=1;
	
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private ArrayList listeners=new ArrayList();
	
	public MessageCoder(DataInputStream inputStream,
			DataOutputStream outputStream) {
		super();
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}
	
	public void addMessageListener(MessageListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	public void removeMessageListener(MessageListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

	
	public void decodeMessage() throws IOException {
		int messageType=inputStream.readUnsignedByte();
		
		if(messageType==KEEPALIVE) {
			synchronized(listeners) {
				for(Object listener :  listeners) {
					((MessageListener)listener).keepalive();
				}
			}
		}
		
	}
	
	public void encodeKeepalive() throws IOException {
		synchronized(this) {
			outputStream.writeByte(KEEPALIVE);
		}
	}
		
}