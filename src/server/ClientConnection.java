package src.server;

import java.io.IOException;
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
	private Socket connectedClientSocket;
	private ObjectOutputStream outputToClient;
	private ObjectInputStream inputFromClient;
	private String streamingOptions;
	private List<UserAccount> userList = new ArrayList<UserAccount>();
	private Boolean clientIsConnected;
	//variables for the media player

	private MediaPlayerFactory mediaPlayerFactory;
	private HeadlessMediaPlayer mediaPlayer;
	private String vlcLibraryDatapath = "external_archives/VLC/vlc-2.0.1";
	private String xmlListDatapath = "src/server/video_repository/videoList.xml";
	private String videoRepositoryDatapath = "src/server/video_repository/";

	public ClientConnection(Socket socket, int streamPort, String streamingOptions) {
		this.connectedClientSocket = socket;
		this.streamPort = streamPort;
		this.streamingOptions = streamingOptions;
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), this.vlcLibraryDatapath);
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
	}

	@Override
	public void run() {
		try {
			outputToClient = new ObjectOutputStream(connectedClientSocket.getOutputStream());
			inputFromClient = new ObjectInputStream(connectedClientSocket.getInputStream());
			this.clientIsConnected = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		String userInput ="";
		//Read user list
		userListXMLreader reader = new userListXMLreader();
		userList = reader.parseUserAccountList();
		//Attempt login
		Boolean isLoggedIn = false;
		while(!isLoggedIn){
			try {
				userInput = (String) read();
				String password = userInput;
				userInput = (String) read();
				password += userInput;
				
				for(UserAccount user : userList){
					//check for user name
					if((user.getUserNameID()+ user.getPassword()).equals(password)){
						System.out.println("LOGIN SUCCEDED");
						send("LOGIN SUCCEDED");
						isLoggedIn = true;
						//sending user account data to client
						send(user);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!isLoggedIn) {
				System.out.println("LOGIN FAILED");
				send("LOGIN FAILED");
			}
		}
		while(clientIsConnected){
			try {

				userInput = (String) read();
				System.out.println("Message recieved from client: " + userInput);

				if (userInput == null) {
					continue;
				} else if (userInput.equals("GETLIST")) {
					sendVideoListToClient();
				} else if (userInput.equals("STREAM")) {
					String videoID = "";
					videoID = (String) read();
					setUpMediaStream(videoID);
				} else if (userInput.equals("STOPSTREAM")) {
					stopStream();
				} else if (userInput.equals("CLOSE")) {
					break;
				} else if (userInput.equals("STREAMPORT")) {
					send(this.streamPort);
				} else if (userInput.equals("SKIP")) {
					float videoPosition = 0;
					videoPosition = (float) read();
					mediaPlayer.setPosition(videoPosition);
				}else if (userInput.equals("GET VIDEO COMMENTS")){
					
				}else if (userInput.equals("COMMENT")){
					
				} else if (userInput.equals("CLOSECONNECTION")) {
					if (this.mediaPlayerFactory != null) {
						this.connectedClientSocket.close();
						this.mediaPlayer.release();
						this.mediaPlayerFactory.release();
					}

					this.clientIsConnected = false;
				} else {
					// No action appears necessary
				}
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void send(Object output) {
		try {
			outputToClient.writeObject(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Object read(){
		Object obj = null;
		try {
			obj = inputFromClient.readObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

	/**
	 * Reads the xml video list
	 * 
	 * @return
	 */
	protected List<VideoFile> getVideoList() {
		return this.getVideoList(this.xmlListDatapath);
	}

	// Used for testing when other lists are used and checked.
	protected List<VideoFile> getVideoList(String fileLocation) {
		XMLReader reader = new XMLReader();
		List<VideoFile> videoList = reader.getList(fileLocation);
		return videoList;
	}

	public void sendVideoListToClient() {
		send(getVideoList());
	}

	private void setUpMediaStream(String desiredVideoID) {

		String filenameVideo = getVideoNameFromID(desiredVideoID);
		stopStream();
		mediaPlayerFactory = new MediaPlayerFactory(this.videoRepositoryDatapath + filenameVideo);
		mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
		mediaPlayer.playMedia(videoRepositoryDatapath + filenameVideo, this.streamingOptions, ":no-sout-rtp-sap",
				":no-sout-standardsap", ":sout-all", ":sout-keep");
		send(mediaPlayer.getLength());
	}

	/**
	 * Tests if a stream is running and stops it if it is running.
	 */
	private void stopStream() {
		if (this.mediaPlayerFactory != null) {
			this.mediaPlayer.stop();
			this.mediaPlayer.release();
			this.mediaPlayerFactory.release();
		}
	}

	protected String getVideoNameFromID(String videoID) {
		for (VideoFile video : getVideoList()) {
			if (video.getID().equals(videoID)) {
				return video.getFilename();
			}
		}
		return "";
	}
}
