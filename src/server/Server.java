package src.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
	private ServerSocket serverSocket;
	private String serverAddress = "127.0.0.1";
	private int communicationPort = 1337;
	private int initialStreamPort = 5555;

	private String streamingOptions = formatRtpStream(serverAddress, initialStreamPort);
	private Socket clientSocket;
	private List<ClientConnection> clientConnectionList = new ArrayList<ClientConnection>();

	public static void main(String[] args) {
		new Server(); // Main for testing
	}

	public Server() {
		// creating the server socket
		try {
			serverSocket = new ServerSocket(communicationPort);
		} catch (IOException e) {
			System.out.println("ERROR! Unable to create server socket"); // Leave
																			// out?
			e.printStackTrace();
		}
		// Awaiting client connections
		while (true) {
			try {
				// wait for client to connect to socket
				System.out.println(
						"Successfully opened socket on port: " + communicationPort + ", awaiting connection..."); // TODO
																													// change
																													// to
																													// status
																													// bar
				this.clientSocket = this.serverSocket.accept();
				System.out.println("Successfully connected to client."); // TODO
																			// change
																			// to
																			// status
																			// bar
				// streamPortList.add(initialStreamPort +
				// streamPortList.size());

				int newClientStreamPort = initialStreamPort + this.clientConnectionList.size();
				streamingOptions = formatRtpStream(this.serverAddress, newClientStreamPort);

				// creating and starting client thread
				ClientConnection connectedClient = new ClientConnection(this.clientSocket, newClientStreamPort,
						streamingOptions);
				this.clientConnectionList.add(connectedClient);
				Thread clientThread = new Thread(connectedClient);
				clientThread.start();

			} catch (IOException e) {
				System.out.println("ERROR! Connection to client failed"); // TODO
																			// change
																			// to
																			// status
																			// bar
				e.printStackTrace();
				// prevents the start of a new thread if no connection is made.
			}
		}
	}

	private String formatRtpStream(String serverAddress, int streamPort) {
		StringBuilder sb = new StringBuilder(60);
		sb.append(":sout=#rtp{dst=");
		sb.append(serverAddress);
		sb.append(",port=");
		sb.append(streamPort);
		sb.append(",mux=ts}");
		return sb.toString();
	}

	public void closeSockets(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Failed to close server-sockets"); // TODO change
																	// to status
																	// bar
			e.printStackTrace();
		}
	}
}
