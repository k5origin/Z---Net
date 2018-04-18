package package1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class UDPRouter {

	static InetAddress host = null;

	static int inputPort = 3000;
	static int outputPort = 8007;
	static int clientPort = 41830;

	static final int PERCENT_TO_DROP = 5;
	static final int TIMEOUT = 1000;
	static final int SEGMENT_SIZE = 1000;

	public static void main(String[] args) {
		DatagramSocket datagramInputSocket = null;
		DatagramSocket datagramOutputSocket = null;
		DatagramSocket datagramClientSocket = null;

		try {
			host = InetAddress.getByName("localhost");

			datagramInputSocket = new DatagramSocket(inputPort);
			datagramOutputSocket = new DatagramSocket();

			// Create log file.

			while (true) {
				String command;
				command = receiving(datagramInputSocket);
				System.out.println("command: "+command);
				sending(command, datagramOutputSocket);//sends ACK commands only
				
				if (command.toUpperCase().contains("STOP"))
					continue; //TODO: Break this while loop causes it to terminate
				
				byte[] buffer = new byte[SEGMENT_SIZE+1]; //hardcoded receive
				DatagramPacket response = new DatagramPacket(buffer, buffer.length);
				datagramInputSocket.receive(response);
				
				//datagramInputSocket.send(response);
				//response = receiving(datagramInputSocket);
				
				DatagramPacket request = new DatagramPacket(response.getData(), response.getLength(), host, clientPort); //copy packet to new port
				datagramOutputSocket.send(request);
				
				
				
				
			}
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			if (datagramInputSocket != null) {
				datagramInputSocket.close();
			}

			if (datagramOutputSocket != null) {
				datagramOutputSocket.close();
			}
		}
	}

	private static void sending(String command, DatagramSocket datagramOutputSocket) {
		try {
			byte[] data = command.getBytes();
			DatagramPacket request = new DatagramPacket(data, data.length, host, outputPort);
			datagramOutputSocket.send(request);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static void sendingClient(DatagramPacket packet, DatagramSocket datagramOutputSocket) {
		try {
			//byte[] data = command.getBytes();
			DatagramPacket request = new DatagramPacket(packet.getData(), packet.getLength(), host, clientPort);
			datagramOutputSocket.send(request);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private static String receiving(DatagramSocket datagramInputSocket) throws SocketTimeoutException, IOException {
		byte[] buffer = new byte[SEGMENT_SIZE];
		DatagramPacket response = new DatagramPacket(buffer, buffer.length);
		datagramInputSocket.receive(response);

		String command = new String(response.getData()).trim();
		
		return command;
	}

	/**
	 * Assumes there will never be consecutive numbers in the returning array
	 * which will contain the indexes to drop in the data.
	 * 
	 * @param numPackets
	 * @param dataLength
	 * @return
	 */
	private static ArrayList<Integer> packetsToDrop(int dataLength) {
		// Determine how many packets need to be dropped based on the
		// percentage.
		int numPackets = (PERCENT_TO_DROP * dataLength) / 100;
		ArrayList<Integer> toDrop = new ArrayList<Integer>();
		ArrayList<Integer> list = new ArrayList<Integer>();

		for (int i = 0; i < dataLength; i++) {
			list.add(new Integer(i));
		}
		Collections.shuffle(list);
		for (int i = 0; i < numPackets; i++) {
			toDrop.add(list.get(i));
		}

		return toDrop;
	}

}
