package src.server;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.test.basic.PlayerControlsPanel;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class Server {
	private List<VideoFile> videoList;
	private ServerSocket serverSocket;
	private String serverAddress = "127.0.0.1";
	private int listPort =  1337;
	private int streamPort =  5555;
	private String options = formatRtpStream(serverAddress, streamPort);
	private Socket clientSocket;

	private Thread socketThread;
	
	
	public static void main(String[] args){
		new Server(); //Main for testing
	}
	
	private String formatRtpStream(String serverAddress, int serverPort) {
		StringBuilder sb = new StringBuilder(60);
		sb.append(":sout=#rtp{dst=");
		sb.append(serverAddress);
		sb.append(",port=");
		sb.append(serverPort);
		sb.append(",mux=ts}");
		return sb.toString();
		}

	public Server(){
		//creating the server socket
		try {
			serverSocket = new ServerSocket(listPort);
		} catch (IOException e) {
			System.out.println("ERROR ! Unable to create server socket");
			e.printStackTrace();
		}
		//Awaiting client connections
		while(true){
			try {
				//wait for client to connect to socket
				System.out.println("Successfully opened socket on port: "+ listPort + ", awaiting connection...");
				this.clientSocket = this.serverSocket.accept();
				System.out.println("Successfully connected to client.");
				Thread connectionThread = new Thread("connection"){
					private Socket thisSocket;
					private ObjectOutputStream outputToClient;
					private BufferedReader inputFromClient;
					public void run(){
						thisSocket = clientSocket;
						try {
							outputToClient = new ObjectOutputStream(thisSocket.getOutputStream());
							inputFromClient =  new BufferedReader(new InputStreamReader(thisSocket.getInputStream()));
						} catch (IOException e) {
							e.printStackTrace();
						}
						while(true){
							try {
								String input;
								if(inputFromClient.ready()){
									input = inputFromClient.readLine();
									System.out.println("Message recieved from client: " + input);
								}else{
									continue;
								}
								if (input == null){
									continue;
								}
								else if (input.equals("GETLIST")){
									System.out.println("Sending list to client");
									getVideoList();
									sendVideoListToClient(outputToClient);
								}
								else if (input.equals("STREAM")){
									setUpMediaStream(inputFromClient.readLine());
								}
								else if (input.equals("CLOSE")){
									closeSockets(thisSocket);
									break;
								}
								else{
									continue;
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				};
				//starting the connection thread with client
				connectionThread.start();
				System.out.println("Successfully started client thread");
			} catch (IOException e) {
				System.out.println("ERROR! connection with client failed");
				e.printStackTrace();
				//prevents the start of a new thread if no connection is made.
			}
		}
	}
	
	
	private void setUpMediaStream(){
		this.setUpMediaStream();
	}
	private void setUpMediaStream(String videoID){
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "external_archives/VLC/vlc-2.0.1");
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		
		String filename = getVideoNameFromID(videoID);
		MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory("src/server/video_repository/"+filename);
		HeadlessMediaPlayer mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
		mediaPlayer.playMedia("src/server/video_repository/"+filename, options, ":no-sout-rtp-sap", ":no-sout-standardsap",":sout-all", ":sout-keep");
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			System.out.println("Exception thrown whilst streaming.");
			e.printStackTrace();
		}
	}
	
	protected String getVideoNameFromID(String videoID){
		for (VideoFile video : videoList){
			if (video.getID().equals(videoID)){
				return video.getFilename();
			}
		}
		return "";
	}
	
	protected List<VideoFile> getVideoList(){
		XMLReader reader = new XMLReader();
		videoList = reader.getList("src/server/video_repository/videoList.xml");
		return videoList;
	}
	
	public void sendVideoListToClient(ObjectOutputStream outputToClient){
		try {
			outputToClient.writeObject(this.videoList);
		} catch (IOException e) {
			System.out.println("Streaming of video list failed.");
		}
	}


	public void closeSockets(Socket socket){
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Failed to close server-sockets");
			e.printStackTrace();
		}
	}
}
