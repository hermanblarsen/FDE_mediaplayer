package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
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
	protected UserAccount loggedInUser;
	private Socket connectedClientSocket;
	private ObjectOutputStream outputToClient;
	private ObjectInputStream inputFromClient;
	private String streamingOptions;
	private List<UserAccount> userList = new ArrayList<UserAccount>();
	private List<VideoFile> videoList = new ArrayList<VideoFile>();
	

	private MediaPlayerFactory mediaPlayerFactory;
	private HeadlessMediaPlayer mediaPlayer;
	private String vlcLibraryDatapath = "external_archives/VLC/vlc-2.0.1";
	private String xmlListDatapath = "serverRepository/videoList.xml";
	private String videoRepositoryDatapath = "serverRepository/";
	private String currentlyStreamingvideoID;
	protected String clientCommandString;

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
		UserListXmlParser userXmlParser = new UserListXmlParser();
		this.userList = userXmlParser.parseUserAccountList();
		this.readVideoList();
		for (VideoFile video : this.videoList){
			String videoID = video.getID();
			for(UserAccount userAccount : userList){
				boolean userHasVideoInList = false;
				for(VideoFile userVideo : userAccount.getVideos()){
					if(userVideo.getID().equals(videoID)){
						userHasVideoInList = true;
						break;
					}
				}
				if(!userHasVideoInList){
					ArrayList<VideoFile> modifiedUserVideoList = (ArrayList<VideoFile>) userAccount.getVideos();
					modifiedUserVideoList.add(video);
					userAccount.setVideos(modifiedUserVideoList);
				}
			}
		}
		userXmlParser.writeUserListToXML((ArrayList<UserAccount>) userList);
		
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
					this.loggedInUser = user;
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
			clientCommandString = "";
			Object clientOutput = null;
			clientOutput = readFromObjectStream();
			if (clientOutput == null) {
				continue;
			}else if (clientOutput instanceof String) {
				clientCommandString = (String) clientOutput;
				System.out.println("Message recieved from client: " + clientCommandString); //TODO Remove before hand in
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
				updateUserWatchedPercentage(position);
				break;
			case "GETLIST":
				sendVideoListToClient();
				break;	
			case "STREAM":
				currentlyStreamingvideoID = "";
				currentlyStreamingvideoID = (String) readFromObjectStream();
				setUpMediaStream(currentlyStreamingvideoID);
				break;
			case "REQUESTSTREAMPORT":
				sendThroughObjectStream(this.streamPort);
				break;	
			case "SKIPTOPOSITION":
				float videoPositionSlider = 0;
				videoPositionSlider = (float) readFromObjectStream();
				if (mediaPlayer != null && videoPositionSlider > 0 && videoPositionSlider < 1) {
					mediaPlayer.setPosition(videoPositionSlider);
				}
				break;
			case "CLOSECONNECTION":
				this.closeConnection();
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
				String commentedVideoId = (String) readFromObjectStream();
				String comment = (String) readFromObjectStream();
				readVideoList();
				for (VideoFile video : videoList) {
					if (video.getID().equals(commentedVideoId)) {
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
				updateVideoListXML();
				break;
			case "RATE":
				//server now expects the videoID of the video that is being rated
				String ratedVideoID = (String) readFromObjectStream();
				//server then expects the rating the user has given
				int rating = (int) readFromObjectStream();
				//server now updates the UserAccount with the rating
				ArrayList<VideoFile> modifiedUserVideoList = (ArrayList<VideoFile>) loggedInUser.getVideos();
				for (Iterator iterator = videoList.iterator(); iterator.hasNext();) {
					VideoFile serverVideo = (VideoFile) iterator.next();
					//get the right video file from the video list
					if (serverVideo.getID().equals(ratedVideoID)) {
						//now check if the user allready has this video in his list
						boolean userVideoListContainsVideo = false;
						for (Iterator iterator2 = modifiedUserVideoList.iterator(); iterator2.hasNext();) {
							VideoFile userVideo = (VideoFile) iterator2.next();
							if(userVideo.getID().equals(ratedVideoID)){
								userVideoListContainsVideo = true;
								break;
							}
						}
						if (!userVideoListContainsVideo) {
							modifiedUserVideoList.add(serverVideo);
						}
						break;
					}
				}
				for (VideoFile userVideo : modifiedUserVideoList) {
					if (userVideo.getID().equals(ratedVideoID)) {
						userVideo.setUserRating(rating);
					}
				}
				loggedInUser.setVideos(modifiedUserVideoList);
				//refresh the user account in the user list
				for (Iterator iterator = this.userList.iterator(); iterator.hasNext();) {
					UserAccount account = (UserAccount) iterator.next();
					if (account.getUserNameID().equals(loggedInUser.getUserNameID())) {
						account = loggedInUser;
					}
				}
				UserListXmlParser userListXmlParser = new UserListXmlParser();
				userListXmlParser.writeUserListToXML((ArrayList<UserAccount>) userList);
				//update the overall video rating 
				updateVideoRating(ratedVideoID);
				break;
			default:
				break;
			}
		}
	}

	//update the percentage watched for the current video and user
	private void updateUserWatchedPercentage(float newPercentage) {
		
		for (UserAccount user : userList){
			if (user.getUserNameID().equals(loggedInUser.getUserNameID())){
				for(VideoFile tempVideo : user.getVideos()){
					if(tempVideo.getID().equals(currentlyStreamingvideoID)){
						tempVideo.setPercentageWatched(newPercentage);
						UserListXmlParser userListXmlParser = new UserListXmlParser();
						userListXmlParser.writeUserListToXML((ArrayList<UserAccount>) userList);
						break;
					}
				}
			}
		}
		
	}

	private void updateVideoListXML() {
		VideoListXmlParser parser = new VideoListXmlParser(xmlListDatapath);
		parser.writeVideoList(videoList);
	}

	private void updateVideoRating(String ratedVideoID) {
		float globalRating = 0 ;
		int numberOfUsersThatRatedVideo = 0;
		for (UserAccount user : userList) {
			//check if the user has rated this video
			for (VideoFile userVideo : user.getVideos()) {
				if (userVideo.getID().equals(ratedVideoID)) {
					globalRating += userVideo.getUserRating();
					numberOfUsersThatRatedVideo += 1;
				}
			}
		}
		//now find the average public rating of the video
		if (numberOfUsersThatRatedVideo > 0) {//prevent dividing by 0 error
			globalRating = globalRating/((float)numberOfUsersThatRatedVideo);
			//now change the rating of the video
			for (Iterator video = videoList.iterator(); video.hasNext();) {
				VideoFile currentVideo = (VideoFile) video.next();
				if (currentVideo.getID().equals(ratedVideoID)) {
					currentVideo.setPublicRating(globalRating);
				}
			}
		}
		updateVideoListXML();
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
		VideoListXmlParser xmlReader = new VideoListXmlParser(fileLocation); 
		List<VideoFile> tempvideoList = xmlReader.parseVideoList();
		this.videoList = tempvideoList;
	}

	public void sendVideoListToClient() {
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
		readVideoList();
		for (VideoFile video : this.videoList) {
			if (video.getID().equals(videoID)) {
				return video.getFilename();
			}
		}
		return "";
	}
}
