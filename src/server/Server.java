package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Server: (Acts more like a switchboard than a server, but we kept the name
 * since it handles the initial communication request from all clients.) Its
 * only job is to delegate a streaming port and a socket to all clients
 * connecting, after which further communication is dealt with by the
 * ClientConnection.
 */
public class Server implements Runnable {
	protected ClientConnection connectedClient;
	private Socket clientSocket;
	private ServerSocket serverSocket;
	private String serverAddress = "127.0.0.1";
	private int communicationPort = 1337;
	private int initialStreamPort = 5555;
	private String streamingOptions = formatRtpStream(serverAddress, initialStreamPort);
	protected List<ClientConnection> clientConnectionList = new CopyOnWriteArrayList<>();

	public static void main(String[] args) {
		Thread serverThread = new Thread(new Server());
		serverThread.start();
	}

	public void run() {
		// Create general socket for initial communication with clients.
		try {
			serverSocket = new ServerSocket(communicationPort);
		} catch (IOException e) {
			System.out.println("ERROR: Unable to create server socket");
		}

		// Await client connections, start a ClientConnection if a client
		// connects and hand over socket and streaming port to respective
		// ClientConnection.
		while (true) {
			try {
				// Wait for client to connect to socket
				System.out.println(
						"Successfully opened socket on port: " + communicationPort + ", awaiting connection...");
				this.clientSocket = this.serverSocket.accept();
				System.out.println("Successfully connected to client.");

				// Assign an available streaming port to the client
				int newClientStreamPort = initialStreamPort + this.clientConnectionList.size();
				streamingOptions = formatRtpStream(this.serverAddress, newClientStreamPort);
				connectedClient = new ClientConnection(this.clientSocket, newClientStreamPort, streamingOptions);
				this.clientConnectionList.add(connectedClient);

				Thread clientThread = new Thread(connectedClient);
				clientThread.start();
			} catch (IOException e) {
				System.out.println("ERROR! Connection to client failed");
				e.printStackTrace();
			}
		}
		// Sockets are handed over to ClientConnection where they are closed
		// after use. Closing sockets in this class is thus not needed.
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

	protected synchronized ClientConnection getClientConnection(int index) {
		ClientConnection client = this.clientConnectionList.get(index);
		return client;
	}
}
