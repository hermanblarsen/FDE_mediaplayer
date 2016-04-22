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
					System.out.println("Could not open socket on port: " + port + ".");
					System.exit(-1);
				}
				System.out.println("Successfully opened socket on port: "+ port + ", awaiting connection...");
				
				try {
					//Server stops and awaits client-connection
					clientSocket = serverSocket.accept();
				} catch (IOException e) {
					System.out.println("Could not connect to client.");
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
//		videoList = reader.getList("videoListEmpty.xml"); //TODO Only for testing, remove 
		return videoList;
	}
	
	public void sendVideoListToClient(){
		try {
			outputToClient = new ObjectOutputStream(clientSocket.getOutputStream());
			outputToClient.writeObject(this.videoList);
		} catch (IOException e) {
			System.out.println("Streaming of video list failed.");
		}
	}


	public void closeSockets(){
		try {
			this.clientSocket.close();
			this.serverSocket.close();
		} catch (IOException e) {
			System.out.println("Failed to close server-sockets");
			e.printStackTrace();
		}
	}
}
