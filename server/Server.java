package server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Server {
	private List<VideoFile> videoList;
	private ServerSocket serverSocket;
	private int port =  1238;
	private Socket clientSocket;
	private ObjectOutputStream outputToClient;
	private Thread socketThread;
	
	public static void main(String[] args){
		new Server(); //Main for testing
	}
	
	public Server(){
		//Reading and storing the video List from xml file
		this.videoList = getVideoList();
		openSocket();
	}
	
	private void openSocket(){
		socketThread = new Thread("Socket"){
			public void run(){
				try {
					serverSocket = new ServerSocket(port);
				} catch (IOException e) {
					System.out.println("Could not open socket on port " + port);
					System.exit(-1);
				}
				System.out.println("Opened Socket on port "+ port + ", Waiting");
				try {
					//server stops and waits for client to connect.
					clientSocket = serverSocket.accept();
				} catch (IOException e) {
					System.out.println("Could not connect to client");
					System.exit(-1);
				}
				sendVideoListToClient();
				closeSockets();
			}
		};
		socketThread.start();
	}
	
	protected List<VideoFile> getVideoList(){
		XMLReader reader = new XMLReader();
		videoList = reader.getList("videoList.xml");
		return videoList;
	}
	
	public void sendVideoListToClient(){
		try {
			outputToClient = new ObjectOutputStream(clientSocket.getOutputStream());
			outputToClient.writeObject(this.videoList);
		} catch (IOException e) {
			System.out.println("Sending video list failed");
		}
	}


	public void closeSockets(){
		try {
			this.clientSocket.close();
			this.serverSocket.close();
			System.out.println("Closed server side sockets");
		} catch (IOException e) {
			System.out.println("Failed to close server side sockets");
			e.printStackTrace();
		}
	}
}
