package src.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
	private Boolean clientIsConnected;

	// variables for the media player
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

		while (clientIsConnected) {
			try {
				String input = "";

				try {
					input = (String) inputFromClient.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				System.out.println("Message recieved from client: " + input);

				if (input == null) {
					continue;
				} else if (input.equals("GETLIST")) {
					sendVideoListToClient();
				} else if (input.equals("STREAM")) {
					String videoID = "";
					try {
						videoID = (String) inputFromClient.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					setUpMediaStream(videoID);
				} else if (input.equals("STOPSTREAM")) {
					stopStream();
				} else if (input.equals("CLOSE")) {
					break;
				} else if (input.equals("STREAMPORT")) {
					outputToClient.writeObject(this.streamPort);
				} else if (input.equals("SKIP")) {
					float videoPosition = 0;
					try {
						videoPosition = (float) inputFromClient.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					mediaPlayer.setPosition(videoPosition);
				} else if (input.equals("CLOSECONNECTION")) {
					this.connectedClientSocket.close();
					this.mediaPlayer.release();
					this.mediaPlayerFactory.release();
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
		try {
			this.outputToClient.writeObject(getVideoList());
		} catch (IOException e) {
			// No action appears necessary
		}
	}

	private void setUpMediaStream(String desiredVideoID) {

		String filenameVideo = getVideoNameFromID(desiredVideoID);
		stopStream();
		mediaPlayerFactory = new MediaPlayerFactory(this.videoRepositoryDatapath + filenameVideo);
		mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
		mediaPlayer.playMedia(videoRepositoryDatapath + filenameVideo, this.streamingOptions, ":no-sout-rtp-sap",
				":no-sout-standardsap", ":sout-all", ":sout-keep");
		try {
			outputToClient.writeObject(mediaPlayer.getLength());
		} catch (IOException e) {
			// No action appears necessary
		}
	}

	/**
	 * Tests if a stream is running and stops it if it is running.
	 */
	private void stopStream() {
		if (mediaPlayerFactory != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayerFactory.release();
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
