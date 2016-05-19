package server;

import client.Client;
import static org.junit.Assert.*;
import java.awt.EventQueue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The ClientConnectionTest tests for: 
 * - connection to client, 
 * - login, 
 * - correct user and video list stored in server, 
 * - mediaplayer commands such as play, stop, stream, 
 * - and changing video while streaming.
 */
public class ClientConnectionTest {
	private Socket clientSocket;
	private ClientConnection clientConnection;
	private Client client;
	private ServerSocket serverSocket;

	@Before
	public void startServer() throws InterruptedException, IOException {
		// we had problems with testing the clientConnection when using the
		// server to spawn clientConnection threads. We decided to 'emulate'
		// the server functionality within this test class. This allowed us
		// to do automated testing for the clientConnection class
		try {
			serverSocket = new ServerSocket(1337);
		} catch (IOException e) {
			fail();
		}
		String streamingOptions = formatRtpStream("127.0.0.1", 5555);
		// starting client
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					client = new Client(false);
					// client.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		// waiting for client to automatically connect
		clientSocket = serverSocket.accept();
		// starting server (ClientConnection)
		clientConnection = new ClientConnection(clientSocket, 5555, streamingOptions);
		Thread connectionThread = new Thread(clientConnection);
		connectionThread.start();
		// delay is needed for ClientConnection to receive and act on command
		Thread.sleep(500);
		client.login("123", "123"); //login with valid username/password
		Thread.sleep(500);
	}

	private String formatRtpStream(String serverAddress, int streamPort) {
		StringBuilder sb = new StringBuilder(60);
		sb.append(":sout=#rtp{dst=");
		sb.append(serverAddress);
		sb.append(",port=");
		sb.append(streamPort);
		sb.append(",mux=ts}");
		return sb.toString();
	}

	@After
	public void closeSockets() throws IOException, InterruptedException {
		serverSocket.close();
		client.closeConnection();
		Thread.sleep(200);
	}

	@Test
	public void testClientConnectsToServer() {
		assertNotNull(clientConnection);
	}

	@Test
	public void testThatCorrectUserLoggedIn() {
		assertTrue(clientConnection.loggedInUser.getUserNameID().equals("123"));
	}

	@Test
	public void testThatServerHasTheCorrectUserList() {
		UserAccount user = clientConnection.userList.get(0);
		assertTrue(user.getUserNameID().equals("UserName1"));
		assertTrue(user.getPassword().equals("Password1"));
		user = clientConnection.userList.get(1);
		assertTrue(user.getUserNameID().equals("UserName2"));
		assertTrue(user.getPassword().equals("Password2"));
		user = clientConnection.userList.get(2);
		assertTrue(user.getUserNameID().equals("UserName3"));
		assertTrue(user.getPassword().equals("Password3"));
	}

	@Test
	public void testThatServerHasTheCorrectVideoList() {
		VideoFile video = clientConnection.videoList.get(0);
		assertTrue(video.getID().equals("20120213a2"));
		assertTrue(video.getTitle().equals("Monsters Inc."));
		assertTrue(video.getFilename().equals("monstersinc_high.mpg"));
		video = clientConnection.videoList.get(1);
		assertTrue(video.getID().equals("20120102b7"));
		assertTrue(video.getTitle().equals("Avengers"));
		assertTrue(video.getFilename().equals("avengers-featurehp.mp4"));
		video = clientConnection.videoList.get(2);
		assertTrue(video.getID().equals("20120102b4"));
		assertTrue(video.getTitle().equals("Prometheus"));
		assertTrue(video.getFilename().equals("prometheus-featureukFhp.mp4"));
	}

	@Test
	public void testThatServerStartsStreamingWhenClientSendsRequest() throws InterruptedException {
		// emulating the user picking a video to stream
		client.sendToServer("STREAM");
		Thread.sleep(200);
		assertTrue(clientConnection.clientCommandString.equals("STREAM"));
		client.sendToServer("20120213a2");
		Thread.sleep(200);
		assertTrue(clientConnection.currentlyStreamingvideoID.equals("20120213a2"));
		Thread.sleep(200);
		assertTrue(clientConnection.mediaPlayer.isPlaying());
	}

	@Test
	public void testThePAUSEandResumePlaying() throws InterruptedException {
		// starting stream
		client.sendToServer("STREAM");
		client.sendToServer("20120213a2");
		Thread.sleep(400);
		assertTrue(clientConnection.mediaPlayer.isPlaying());
		float position = clientConnection.mediaPlayer.getPosition();
		// pausing stream
		client.sendToServer("PAUSE");
		Thread.sleep(300);
		assertFalse(clientConnection.mediaPlayer.isPlaying());
		// now test that the player resumes from the same position
		client.sendToServer("PLAY");
		Thread.sleep(300);
		assertTrue(clientConnection.mediaPlayer.isPlaying());
		assertTrue(clientConnection.mediaPlayer.getPosition() >= position);
	}

	@Test
	public void testChangingVideoMidStream() throws InterruptedException {
		client.sendToServer("STREAM");
		client.sendToServer("20120213a2");
		Thread.sleep(300);
		assertTrue(clientConnection.mediaPlayer.isPlaying());
		assertTrue(clientConnection.currentlyStreamingvideoID.equals("20120213a2"));
		client.sendToServer("STREAM");
		client.sendToServer("20120102b4");
		Thread.sleep(300);
		assertTrue(clientConnection.mediaPlayer.isPlaying());
		assertTrue(clientConnection.currentlyStreamingvideoID.equals("20120102b4"));
	}
}
