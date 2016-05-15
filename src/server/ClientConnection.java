package src.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
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
	private Boolean clientIsConnected;
	private Boolean userIsLoggedIn;
	private Socket connectedClientSocket;
	private ObjectOutputStream outputToClient;
	private ObjectInputStream inputFromClient;
	private String streamingOptions;
	private List<UserAccount> userList = new ArrayList<UserAccount>();
	private List<VideoFile> videoList = new ArrayList<VideoFile>();
	

	private MediaPlayerFactory mediaPlayerFactory;
	private HeadlessMediaPlayer mediaPlayer;
	private String vlcLibraryDatapath = "external_archives/VLC/vlc-2.0.1";
	private String xmlListDatapath = "src/server/video_repository/videoList.xml";
	private String videoRepositoryDatapath = "src/server/video_repository/";

	public ClientConnection(Socket clientSocket, int streamPort, String streamingOptions) {
		this.connectedClientSocket = clientSocket;
		this.streamPort = streamPort;
		this.streamingOptions = streamingOptions;
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), this.vlcLibraryDatapath);
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
	}

	@Override
	public void run() {
		// Read list of user details.
		userListXMLreader xmlReader = new userListXMLreader();
		userList = xmlReader.parseUserAccountList();
		
		this.userLogin();
		this.respondToClientCommands();
	}
	
	private void userLogin() {
		try {
			this.outputToClient = new ObjectOutputStream(connectedClientSocket.getOutputStream());
			this.inputFromClient = new ObjectInputStream(connectedClientSocket.getInputStream());
			this.clientIsConnected = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		userIsLoggedIn = false;
		while (!userIsLoggedIn && clientIsConnected) {
			Object clientOutput = null;
			String usernameAndPassword = "";
			
			clientOutput = readFromObjectStream();
			if (clientOutput == null) {
				System.out.println("Client is prematurely disconnected");
				closeConnection();
				break;
			} else {
				clientOutput = readFromObjectStream();
			 	if (clientOutput instanceof String) {
					usernameAndPassword = (String) clientOutput;
				}
				clientOutput = readFromObjectStream();
				if (clientOutput instanceof String) {
					usernameAndPassword += (String) clientOutput;
				}
			}
			
			for (UserAccount user : userList) {
				// check for user name
				if ((user.getUserNameID() + user.getPassword()).equals(usernameAndPassword)) {
					System.out.println("LOGIN SUCCEDED");
					sendThroughObjectStream("LOGINSUCCEDED");
					this.userIsLoggedIn = true;
					// sending user-specific account-data to client
					sendThroughObjectStream(user);
					break;
				}
			}
			
			if (!userIsLoggedIn && clientIsConnected) {
				System.out.println("LOGIN FAILED");
				sendThroughObjectStream("LOGINFAILED");
			}
				
		}
	}

	private void respondToClientCommands() {
		while (clientIsConnected) {
			String clientCommandString = "";
			Object clientOutput = null;
			clientOutput = readFromObjectStream();
			if (clientOutput == null) {
				continue;
			}
			if(clientOutput instanceof String) {
				clientCommandString = (String) clientOutput;
				//System.out.println("Message recieved from client: " + clientCommandString); //TODO Remove
			}
			
			switch (clientCommandString) {
			case "PLAY":
				if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
					mediaPlayer.play();
				} 
				break;
			case "PAUSE":
				if (mediaPlayer != null && mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
				}
				break;
			case "STOP":
				if (this.mediaPlayerFactory != null) {
					this.mediaPlayer.stop();
				}
				break;
			case "STREAM POSITION":
				float position = 0;
				if (mediaPlayer != null) {
					position = mediaPlayer.getPosition();
				}
				sendThroughObjectStream(position);
				break;
			case "GETLIST":
				sendVideoListToClient();
				break;	
			case "STREAM":
				String videoID = "";
				videoID = (String) readFromObjectStream();
				setUpMediaStream(videoID);
				break;
			case "REQUESTSTREAMPORT":
				sendThroughObjectStream(this.streamPort);
				break;	
			case "SKIP":
				float videoPositionSlider = 0;
				videoPositionSlider = (float) readFromObjectStream();
				if (mediaPlayer != null && videoPositionSlider > 0 && videoPositionSlider < 1) {
					mediaPlayer.setPosition(videoPositionSlider);
				}
				break;
			case "GET VIDEO COMMENTS":
				String selectedVideoID = (String) readFromObjectStream();
				readVideoList();
				for (VideoFile video : videoList) {
					if (video.getID().equals(selectedVideoID)) {
						sendThroughObjectStream(video.getPublicCommentsList());
						break;
					}
				}
				break;	
			case "COMMENT":
				String videoIdToComment = (String) readFromObjectStream();
				String comment = (String) readFromObjectStream();
				readVideoList();
				for (VideoFile video : videoList) {
					if (video.getID().equals(videoIdToComment)) {
						ArrayList<String> commentsList = (ArrayList<String>) video.getPublicCommentsList();
						// if no comments list exists, create one
						if (commentsList == null) {
							commentsList = new ArrayList<String>();
						}
						commentsList.add(comment);
						video.setPublicCommentsList(commentsList);
						break;
					}
				}
				videoListParser parser = new videoListParser(xmlListDatapath);
				parser.writeVideoList(videoList);
				break;
			case "CLOSECONNECTION":
				this.closeConnection();
				break;
			default:
				break;
			}
		}
	}

	private void closeConnection() {
		if (this.mediaPlayerFactory != null) {
			try {
				this.connectedClientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.mediaPlayer.release();
			this.mediaPlayerFactory.release();
		}
		this.clientIsConnected = false;
	}

	private void sendThroughObjectStream(Object outputObject) {
		try {
			outputToClient.writeObject(outputObject);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Object readFromObjectStream() {
		Object inputObject = null;
		try {
			inputObject = inputFromClient.readObject();
		} catch (Exception e) {
			//Catch and leave exception
		}
		return inputObject;
	}

	/**
	 * Reads the xml video list
	 * 
	 * @return
	 */
	protected void readVideoList() {
		readVideoList(this.xmlListDatapath);
	}

	// Used for testing when other lists are used and checked.
	protected void readVideoList(String fileLocation) {  
		videoListParser xmlReader = new videoListParser(fileLocation); 
		List<VideoFile> tempvideoList = xmlReader.parseVideoList();
		this.videoList = tempvideoList;
	}

	public void sendVideoListToClient() { //For testing only..
		readVideoList();
		sendThroughObjectStream(this.videoList);
	}

	private void setUpMediaStream(String desiredVideoID) {
		if (this.mediaPlayerFactory != null) {
			this.mediaPlayer.stop();
			this.mediaPlayerFactory.release();
			this.mediaPlayer.release();
		}
		
		String videoFilename = getVideoNameFromID(desiredVideoID);

		this.mediaPlayerFactory = new MediaPlayerFactory(this.videoRepositoryDatapath + videoFilename);
		this.mediaPlayer = this.mediaPlayerFactory.newHeadlessMediaPlayer();
		this.mediaPlayer.playMedia(this.videoRepositoryDatapath + videoFilename, this.streamingOptions, 
				":no-sout-rtp-sap",
				":no-sout-standardsap", 
				":sout-all", ":sout-keep");
		sendThroughObjectStream(this.mediaPlayer.getLength());
	}

	protected String getVideoNameFromID(String videoID) {
		readVideoList(); //TODO can this be exchanged with this.videoList??
		for (VideoFile video : this.videoList) {
			if (video.getID().equals(videoID)) {
				return video.getFilename();
			}
		}
		return ""; //TODO return an error of some kind?
	}
}
