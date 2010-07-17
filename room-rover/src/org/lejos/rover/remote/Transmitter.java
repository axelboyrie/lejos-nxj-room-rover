package org.lejos.rover.remote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.robotics.Colors.Color;

import org.lejos.rover.RoomRover;
import org.lejos.rover.remote.message.Message;
import org.lejos.rover.remote.message.MessageFactory;

@SuppressWarnings("unchecked")
public class Transmitter implements Runnable {

	private boolean stopRequest = false;

	private Thread thread;
	private BTConnection connection;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private ArrayList listeners = new ArrayList();

	public Transmitter() {
		thread = new Thread(this);
	}

	public void addMessageListener(MessageListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeMessageListener(MessageListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void listen() {
		thread.start();
	}

	@Override
	public void run() {
		RoomRover.getInstance().getLightSensor().setFloodlight(Color.BLUE);

		connection = Bluetooth.waitForConnection();

		if (connection == null) {
			return;
		}

		inputStream = connection.openDataInputStream();
		outputStream = connection.openDataOutputStream();

		RoomRover.getInstance().getLightSensor().setFloodlight(Color.GREEN);

		while (!stopRequest) {

			try {
				
				int messageType = inputStream.readUnsignedByte();
				
				Message message = MessageFactory.constructMessage(messageType);
				if(message!=null) {
					message.read(inputStream);
	
					synchronized (listeners) {
						for (Object listener : listeners) {
							((MessageListener) listener).messageReceived(message);
						}
					}
				}
				else {
					stopRequest=true; // Unknown message type. Probably transmission error.
				}
				
			} catch (IOException e) {
				stopRequest=true;
			}

		}
		
		try {
			inputStream.close();
			outputStream.close();
			connection.close();
		} catch (IOException e) {
		}

		RoomRover.getInstance().getLightSensor().setFloodlight(Color.RED);
	}

	public void encodeMessage(Message message) {
		if(isConnected()) {
			try {
				outputStream.writeByte(message.getType());
				message.write(outputStream);
			} catch (IOException e) {
				stopRequest=true;
			}
		}
	}
	
	public boolean isListening() {
		return !stopRequest && connection == null;
	}

	public boolean isConnected() {
		return !stopRequest && connection != null;
	}

	public void disconnect() {
		stopRequest = true;
		
		if(connection!=null) {
			connection.close();
		}

		while (thread != null && thread.isAlive()) {
			thread.interrupt();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

}
