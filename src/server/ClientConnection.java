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
		
		Object userInput;
		// Read user list
		userListXMLreader xmlReader = new userListXMLreader();
		userList = xmlReader.parseUserAccountList();
		userIsLoggedIn = false;
		while (!userIsLoggedIn) {
			try {
				//Obtain username and password from client
				String usernameAndPassword = "";
				userInput = readFromObjectStream();
				if (userInput instanceof String){
					usernameAndPassword = (String) userInput;
				}
				userInput = readFromObjectStream();
				if (userInput instanceof String) {
					usernameAndPassword += (String) userInput;
				}
				
				for (UserAccount user : userList) {
					// check for user name
					if ((user.getUserNameID() + user.getPassword()).equals(usernameAndPassword)) {
						System.out.println("LOGIN SUCCEDED"); //TODO put to task bar?
						sendThroughObjectStream("LOGIN SUCCEDED");
						this.userIsLoggedIn = true;
						// sending user-specific account-data to client
						sendThroughObjectStream(user);
						break;
					}
				}
			} catch (IOException e) {
				this.closeConnection();
				break;
			}catch (Exception e) {
				this.closeConnection();
				break;
			}
			if (!userIsLoggedIn && !clientIsConnected) {
				System.out.println("LOGIN FAILED"); //TODO put to task bar?
				sendThroughObjectStream("LOGIN FAILED");
				//TODO give the user feedback on what was wrong, eg was the password or username wrong
			}
		}
	}
	
	private void respondToClientCommands() {
		while (clientIsConnected) {
			String clientOutput = "";
			try {
				clientOutput = (String) readFromObjectStream();
				System.out.println("Message recieved from client: " + clientOutput); //TODO put to task bar?
				
				if (clientOutput == null) {
					continue;
				} else if (clientOutput.equals("PAUSE")) {
					if (mediaPlayer != null && mediaPlayer.isPlaying()) {
						mediaPlayer.pause();
					}
				} else if (clientOutput.equals("PLAY")) {
					if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
						mediaPlayer.play();
					} 
				} else if (clientOutput.equals("STREAM POSITION")) {
					float position = 0;
					if (mediaPlayer != null) {
						position = mediaPlayer.getPosition();
					}
					sendThroughObjectStream(position);
				} else if (clientOutput.equals("GETLIST")) {
					sendVideoListToClient();
				} else if (clientOutput.equals("STREAM")) {
					String videoID = "";
					videoID = (String) readFromObjectStream();
					setUpMediaStream(videoID);
				} else if (clientOutput.equals("STOP")) {
					if (this.mediaPlayerFactory != null) {
						this.mediaPlayer.stop();
					}
				} else if (clientOutput.equals("CLOSE")) {
					break;
				} else if (clientOutput.equals("STREAMPORT")) {
					sendThroughObjectStream(this.streamPort);
				} else if (clientOutput.equals("SKIP")) {
					float videoPosition = 0;
					videoPosition = (float) readFromObjectStream();
					if (mediaPlayer != null && videoPosition > 0 && videoPosition < 1) {
						mediaPlayer.setPosition(videoPosition);
					}
				} else if (clientOutput.equals("GET VIDEO COMMENTS")) {
					String videoID = (String) readFromObjectStream();
					readVideoList();
					for (VideoFile video : videoList) {
						if (video.getID().equals(videoID)) {
							sendThroughObjectStream(video.getPublicCommentsList());
							break;
						}
					}
				} else if (clientOutput.equals("COMMENT")) {
					String videoID = (String) readFromObjectStream();
					String comment = (String) readFromObjectStream();
					readVideoList();
					for (VideoFile video : videoList) {
						if (video.getID().equals(videoID)) {
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
				} else if (clientOutput.equals("CLOSECONNECTION")) {
					this.closeConnection();
				} else {
					// No action appears necessary
				}
			} catch (ClassNotFoundException e) {
				// TODO 
				e.printStackTrace();
			} catch (IOException e) {
				this.closeConnection();
				break;
			}
			continue;
		}
	}

	private void closeConnection() {
		if (this.mediaPlayerFactory != null) {
			try {
				this.connectedClientSocket.close();
			} catch (IOException e) {
				// TODO 
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

	public Object readFromObjectStream() throws ClassNotFoundException, IOException {
		Object inputObject = null;
		
		/*try {*/
			inputObject = inputFromClient.readObject();
		/*} catch (IOException e) {
			//client is disconnected, disconnect clientConnection from everything.
			this.closeConnection();
		}catch (Exception e) {
			
		}*/
		
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
