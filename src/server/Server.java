package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable {
	private ServerSocket serverSocket;
	private String serverAddress = "127.0.0.1";
	private int communicationPort = 1337;
	private int initialStreamPort = 5555;
	private String streamingOptions = formatRtpStream(serverAddress, initialStreamPort);
	private Socket clientSocket;
	private List<ClientConnection> clientConnectionList = new ArrayList<ClientConnection>();

	public static void main(String[] args) {
		Thread serverThread = new Thread(new Server());
		serverThread.start();
	}

	public void run() {
		// Create general socket for communication with clients.
		try {
			serverSocket = new ServerSocket(communicationPort);
		} catch (IOException e) {
			System.out.println("ERROR: Unable to create server socket");
		}

		// Await client connections, start a ClientConnection if a client
		// connects.
		while (true) {
			try {
				// Wait for client to connect to socket
				System.out.println(
						"Successfully opened socket on port: " + communicationPort + ", awaiting connection...");
				this.clientSocket = this.serverSocket.accept();
				System.out.println("Successfully connected to client.");
				// Assign a streaming port to the client
				int newClientStreamPort = initialStreamPort + this.clientConnectionList.size();
				streamingOptions = formatRtpStream(this.serverAddress, newClientStreamPort);
				// Create a clientConnection in a separate thread that deals
				// with further communication
				ClientConnection connectedClient = new ClientConnection(this.clientSocket, newClientStreamPort,
						streamingOptions);
				this.clientConnectionList.add(connectedClient);
				Thread clientThread = new Thread(connectedClient);
				clientThread.start();
			} catch (IOException e) {
				System.out.println("ERROR! Connection to client failed");
				e.printStackTrace();
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

	/*public void closeSockets(Socket socket) {
		try {
			socket.close();
			System.out.println("Closed server-sockets");
		} catch (IOException e) {
			System.out.println("Failed to close server-sockets");
			e.printStackTrace();
		}
	}*///TODO remove
}
