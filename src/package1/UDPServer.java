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

public class UDPServer {

	static InetAddress host = null;

	static int inputPort = 8007;
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

			// Create log file.

			while (true) {
				String command;
				// receive CMD
				command = receiving(datagramInputSocket);
				String[] commands = command.split(" ");
				String transportCmd = "";
				String filename = "";
				String newFilename = "";

				if (commands.length >= 1) {
					transportCmd = commands[0];
				}
				if (commands.length >= 2) {
					filename = commands[1];
				}
				if (commands.length >= 3) {
					newFilename = commands[2];
				}

				System.out.println("Receiver: " + command);

				if (transportCmd.toUpperCase().equals("GET")) {
					getCmd(filename, datagramInputSocket, datagramOutputSocket, host, outputPort); 
				}
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

	private static void getCmd(String filename, DatagramSocket datagramInputSocket, DatagramSocket datagramOutputSocket,
			InetAddress host, int outputPort) throws IOException {
		String command;
		Integer random;
		byte[] data = null;
		DatagramPacket request;

		File file = new File("server/" + filename);
		if (!file.exists() || file.isDirectory()) {
			System.out.println("Receiver: file " + filename + " does not exist.");
			// Send ACK GET
			command = "FAIL";
			sending(command, datagramOutputSocket);
			return;
		} else {

			// Send ACK GET
			command = "ACK get";
			sending(command, datagramOutputSocket);

			if (command.toUpperCase().equals("ACK GET")) {
				// Receive command to start transfer.
				command = receiving(datagramInputSocket);
				if (!command.toUpperCase().equals("START")) {
					sending("FAIL", datagramOutputSocket);
				}
				// Send random number
				random = new Random().nextInt(255) + 1;
				sending(random.toString(), datagramOutputSocket);

				// Receive ACK random number
				command = receiving(datagramInputSocket);

				if (command.toUpperCase().equals("ACK " + random)) {
					datagramInputSocket.setSoTimeout(TIMEOUT);
					// Transfer phase
					// determine starting sequence number
					Integer seqNum = random % 2;

					// Start sending the data
					FileInputStream fis = new FileInputStream(file);
					byte[] bfile = new byte[(int) file.length()];
					fis.read(bfile, 0, bfile.length);
					fis.close();

					// Determine the number of segments in the file
					int segments = bfile.length / SEGMENT_SIZE;
					int remainder = bfile.length % SEGMENT_SIZE;
					int count=0;
					//ArrayList<Integer> packetsToDrop = null;
					for (int i = 0; i <= segments; i++) {
						if (i < segments) {
							data = new byte[SEGMENT_SIZE + 1];
						} else if (remainder > 0) {
							data = new byte[remainder + 1];
						} else {
							break;
						}

						data[0] = seqNum.byteValue();

						for (int j = 1; j < data.length; j++) {
							data[j] = bfile[(i * SEGMENT_SIZE) + (j - 1)];
						}

						request = new DatagramPacket(data, data.length, host, outputPort);
 
						datagramOutputSocket.send(request);
						
						while (true) {
							try {
								count++;
								command = receiving(datagramInputSocket);
								if (!command.equals("ACK PACKET " + seqNum)) {
									// resend
								} else {
									// Change sequence number
									seqNum = seqNum == 0 ? 1 : 0;
								}
							} catch (SocketTimeoutException e) {
								if (i < segments) {
									datagramOutputSocket.send(request);
								} else if (remainder > 0) {
									datagramOutputSocket.send(request);
								}
								continue;
							}
							break;
						}
					}

					sending("STOP", datagramOutputSocket);
					System.out.println("count: " +  count);
					receiving(datagramInputSocket);

					datagramInputSocket.setSoTimeout(0);
				} else {
				}
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

	private static String receiving(DatagramSocket datagramInputSocket) throws SocketTimeoutException, IOException {
		byte[] buffer = new byte[SEGMENT_SIZE];
		DatagramPacket response = new DatagramPacket(buffer, buffer.length);
		datagramInputSocket.receive(response);
		String command = new String(response.getData()).trim();
		return command;
	}


}
