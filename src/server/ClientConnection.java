package src.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class ClientConnection implements Runnable {

	private int streamPort;
	private Socket thisSocket;
	private ObjectOutputStream outputToClient;
	private ObjectInputStream inputFromClient;
	private String streamingOptions;
	private List<UserAccount> userList = new ArrayList<UserAccount>();
	
	//variables for the media player
	private MediaPlayerFactory mediaPlayerFactory;
	private HeadlessMediaPlayer mediaPlayer;
	
	public ClientConnection(Socket socket, int streamPort,String Options) {
		this.thisSocket= socket;
		this.streamPort = streamPort;
		this.streamingOptions = Options;
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "external_archives/VLC/vlc-2.0.1");
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
	}
	
	@Override
	public void run(){
		try {
			outputToClient = new ObjectOutputStream(thisSocket.getOutputStream());
			inputFromClient =  new ObjectInputStream(thisSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		String input ="";
		//Read user list
		userListXMLreader reader = new userListXMLreader();
		userList = reader.parseUserAccountList();
		//Attempt login
		Boolean loggedin = true;
		while(true){
			try {
				input = (String) inputFromClient.readObject();
				String pass = input;
				input = (String) inputFromClient.readObject();
				pass += input;
				
				for(UserAccount user : userList){
					//check for user name
					if((user.getUserNameID()+ user.getPassword()).equals(pass)){
						System.out.println("LOGIN SUCCEDED");
						loggedin = false;
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!loggedin) {
				break;
			}
			System.out.println("LOGIN FAILED");
		}
		while(true){
			try {
				try {
					input = (String) inputFromClient.readObject();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Message recieved from client: " + input);
				if (input == null){
					continue;
				}
				else if (input.equals("GETLIST")){
					System.out.println("Sending list to client");
					sendVideoListToClient();
				}
				else if (input.equals("STREAM")){
					String videoID = "";
					try {
						videoID = (String) inputFromClient.readObject();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("Client requests " + videoID + " stream.");
					setUpMediaStream(videoID);
				}
				else if (input.equals("STOPSTREAM")){
					stopStream();
				}
				else if (input.equals("CLOSE")){
					
					break;
				}
				else if (input.equals("STREAMPORT")){
					outputToClient.writeObject(this.streamPort);
					System.out.println("Set client streamport as :" + this.streamPort);
				}
				else if(input.equals("SKIP")){
					float position = 0;
					try {
						position = (float) inputFromClient.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					System.out.println("Skipping to :" + position);
					mediaPlayer.setPosition(position);
				}
				else{
					continue;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads the xml video list
	 * @return
	 */
	protected List<VideoFile> getVideoList(){
		XMLReader reader = new XMLReader();
		List<VideoFile> videoList = reader.getList("src/server/video_repository/videoList.xml");
		return videoList;
	}
	
	public void sendVideoListToClient(){
		try {
			this.outputToClient.writeObject(getVideoList());
		} catch (IOException e) {
			System.out.println("Streaming of video list failed.");
		}
	}
	
	private void setUpMediaStream(String videoID){
		
		String filename = getVideoNameFromID(videoID);
		stopStream();
		mediaPlayerFactory = new MediaPlayerFactory("src/server/video_repository/"+filename);
		mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
		mediaPlayer.playMedia("src/server/video_repository/"+filename, this.streamingOptions, ":no-sout-rtp-sap", ":no-sout-standardsap",":sout-all", ":sout-keep");
		try {
			outputToClient.writeObject(mediaPlayer.getLength());
		} catch (IOException e) {
			System.out.println("ERROR: cant send media length to client");
		}
	}

	/**
	 * Tests if a stream is running and stops it if it is running.
	 */
	private void stopStream() {
		if(mediaPlayerFactory != null){
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayerFactory.release();
		}
	}
	
	protected String getVideoNameFromID(String videoID){
		for (VideoFile video : getVideoList()){
			if (video.getID().equals(videoID)){
				return video.getFilename();
			}
		}
		return "";
	}
}
