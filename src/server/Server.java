package src.server;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
	private ObjectOutputStream outputToClient;
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
		this(false);
	}
	public Server(boolean startStreamingImmediately){
		//Reading and storing the video List from xml file
		this.videoList = getVideoList();
		openSocket();
		
		if(startStreamingImmediately)
		{
			setUpMediaStream();
		}
	}
	
	private void openSocket(){
		socketThread = new Thread("Socket"){
			public void run(){
				try {
					serverSocket = new ServerSocket(listPort);
				} catch (IOException e) {
					System.out.println("Could not open socket on port: " + listPort + ".");
					System.exit(-1);
				}
				System.out.println("Successfully opened socket on port: "+ listPort + ", awaiting connection...");
				
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
	
	private void setUpMediaStream(){
		this.setUpMediaStream("external_archives/VLC/vlc-2.0.1");
	}
	private void setUpMediaStream(String vlcLibraryPath){
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcLibraryPath);
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		
		MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory("src/server/video_repository/prometheus-featureukFhp.mp4");
		HeadlessMediaPlayer mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
		mediaPlayer.playMedia("src/server/video_repository/prometheus-featureukFhp.mp4", options, ":no-sout-rtp-sap", ":no-sout-standardsap",":sout-all", ":sout-keep");
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			System.out.println("Exception thrown whilst streaming.");
			e.printStackTrace();
		}
	}
	
	protected List<VideoFile> getVideoList(){
		XMLReader reader = new XMLReader();
		videoList = reader.getList("src/server/video_repository/videoList.xml");
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
