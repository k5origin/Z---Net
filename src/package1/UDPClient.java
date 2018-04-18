package package1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class UDPClient {

	static FileOutputStream logFile = null;
	static InetAddress host = null;

	static int inputPort = 41830;
	static int outputPort = 3000;

	static final int PERCENT_TO_DROP = 5;
	static final int TIMEOUT = 1000;
	static final int SEGMENT_SIZE = 1000;

	public static void main(String[] args) {
		DatagramSocket datagramInputSocket = null;
		DatagramSocket datagramOutputSocket = null;

		try {
			host = InetAddress.getByName("localhost");

			datagramInputSocket = new DatagramSocket(inputPort);
			datagramOutputSocket = new DatagramSocket();


			System.out.print("> Enter command: ");
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			String line;

			while ((line = userInput.readLine()) != null) {
				String[] command = line.split(" ");
				String transportCmd = "";
				String filename = "";

				if (command.length >= 1) {
					transportCmd = command[0];
				}
				if (command.length >= 2) {
					filename = command[1];
				}

				if (transportCmd.toUpperCase().equals("GET")) {
					if (!filename.equals("")) {
						getCmd(filename, datagramInputSocket, datagramOutputSocket, host, outputPort);
					} else {
						System.out.println("> Please enter a filename!");
					}
				}

				System.out.print("> Enter command: ");
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (datagramInputSocket != null) {
				datagramInputSocket.close();
			}

			if (datagramOutputSocket != null) {
				datagramOutputSocket.close();
			}
		}
	}

	private static void getCmd(String filename, DatagramSocket datagramInputSocket, DatagramSocket datagramOutputSocket,
			InetAddress host, int outputPort) throws IOException {
		String command;
		byte[] buffer;
		DatagramPacket response;

		// Send GET command
		sending("get " + filename, datagramOutputSocket);

		// Receive ACK GET
		command = receiving(datagramInputSocket);

		if (command.toUpperCase().equals("ACK GET")) {
			// Send command to start receiving
			sending("START", datagramOutputSocket);

			// Receive random number
			command = receiving(datagramInputSocket);

			// check if really a random number or if it is a fail
			if (command.toUpperCase().equals("FAIL")) {
				return;
			}

			// Send ACK random number
			command = "ACK " + command;
			sending(command, datagramOutputSocket);

			FileOutputStream fos = new FileOutputStream("client/" + filename);
			int count=0;

			while (true) {
				// receive packet
				buffer = new byte[1001];
				response = new DatagramPacket(buffer, buffer.length);
				datagramInputSocket.receive(response);
				command = new String(response.getData()).trim();

				if (!command.equals("STOP")) {
					int packetNum = response.getData()[0];
					byte[] responseData = new byte[response.getData().length - 1];

					for (int i = 1; i < response.getData().length; i++) {
						responseData[i - 1] = response.getData()[i];
					}

					fos.write(responseData, 0, responseData.length);
					System.out.println("Sender: receiving PACKET " + packetNum);
					count++;
					

					// send ACK for packet
					command = "ACK PACKET " + packetNum;
					sending(command, datagramOutputSocket);
				} else {
					command = "ACK STOP";
					sending(command, datagramOutputSocket);
					System.out.println("count: "+count);
					break;
				}
			}

			fos.close();
		} else if (command.toUpperCase().equals("FAIL")) {
			// Could not open the file
			System.out.println("The file " + filename + " does not exist! Please try again.");
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

	private static String receiving(DatagramSocket datagramInputSocket) throws SocketTimeoutException, IOException {
		byte[] buffer = new byte[SEGMENT_SIZE];
		DatagramPacket response = new DatagramPacket(buffer, buffer.length);
		datagramInputSocket.receive(response);
		String command = new String(response.getData()).trim();
		return command;
	}
}
