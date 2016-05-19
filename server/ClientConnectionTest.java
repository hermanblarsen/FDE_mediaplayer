package server;

import static org.junit.Assert.*;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;

public class ClientConnectionTest {
	private Server server;
	private boolean serverRunning = false;
	private Socket clientSocket;
	private ClientConnection connection;
	private Client client;
	private Thread t;
	private ServerSocket serverSocket;
	
	@Before
	public void startServer() throws InterruptedException, IOException{	
		//we had problems with testing the clientConnection when using the
		//server to spawn clientConnection threads so we decided to emulate
		//the server functionality within this test class. This allowed us 
		//to do automated testing for the clientConnection class
		try {
			serverSocket = new ServerSocket(1337);
		} catch (IOException e) {
			fail();
		}
		String streamingOptions = formatRtpStream("127.0.0.1", 5555);
		//starting client
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					client = new Client(false);
					//client.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		//waiting for client to automatically connect
		clientSocket = serverSocket.accept();
		//starting server (ClientConnection)
		connection = new ClientConnection(clientSocket, 5555,streamingOptions);
		Thread connectionThread = new Thread(connection);
		connectionThread.start();
		//delay is needed for ClientConnection to receive and act on command
		Thread.sleep(500);
		client.login("123","123");
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
	public void closeStuff() throws IOException, InterruptedException{
		serverSocket.close();
		client.closeConnection();
		Thread.sleep(200);
	}
	
	@Test
	public void testClientConnectsToServer() {
		assertNotNull(connection);
	}
	
	@Test
	public void testThatCorrectUserLoggedIn(){
		assertTrue(connection.loggedInUser.getUserNameID().equals("123"));
	}
	
	@Test
	public void testThatServerHasTheCorrectUserList(){
		UserAccount user = connection.userList.get(0);
		assertTrue(user.getUserNameID().equals("UserName1"));
		assertTrue(user.getPassword().equals("Password1"));
		user = connection.userList.get(1);
		assertTrue(user.getUserNameID().equals("UserName2"));
		assertTrue(user.getPassword().equals("Password2"));
		user = connection.userList.get(2);
		assertTrue(user.getUserNameID().equals("UserName3"));
		assertTrue(user.getPassword().equals("Password3"));
	}
	
	@Test
	public void testThatServerHasTheCorrectVideoList(){
		VideoFile video = connection.videoList.get(0);
		assertTrue(video.getID().equals("20120213a2"));
		assertTrue(video.getTitle().equals("Monsters Inc."));
		assertTrue(video.getFilename().equals("monstersinc_high.mpg"));
		video = connection.videoList.get(1);
		assertTrue(video.getID().equals("20120102b7"));
		assertTrue(video.getTitle().equals("Avengers"));
		assertTrue(video.getFilename().equals("avengers-featurehp.mp4"));
		video = connection.videoList.get(2);
		assertTrue(video.getID().equals("20120102b4"));
		assertTrue(video.getTitle().equals("Prometheus"));
		assertTrue(video.getFilename().equals("prometheus-featureukFhp.mp4"));
	}
	
	@Test
	public void testThatServerStartsStreamingWhenClientSendsRequest() throws InterruptedException{
		//emulating the user picking a video to stream
		client.sendToServer("STREAM");
		Thread.sleep(200);
		assertTrue(connection.clientCommandString.equals("STREAM"));
		client.sendToServer("20120213a2");
		Thread.sleep(200);
		assertTrue(connection.currentlyStreamingvideoID.equals("20120213a2"));
		Thread.sleep(200);
		assertTrue(connection.mediaPlayer.isPlaying());
	}
	
	@Test
	public void testThePAUSEandResumePlaying() throws InterruptedException{
		//starting stream
		client.sendToServer("STREAM");
		client.sendToServer("20120213a2");
		Thread.sleep(400);
		assertTrue(connection.mediaPlayer.isPlaying());
		float position = connection.mediaPlayer.getPosition();
		//pausing stream
		client.sendToServer("PAUSE");
		Thread.sleep(300);
		assertFalse(connection.mediaPlayer.isPlaying());
		//now test that the player resumes from the same position
		client.sendToServer("PLAY");
		Thread.sleep(300);
		assertTrue(connection.mediaPlayer.isPlaying());
		assertTrue(connection.mediaPlayer.getPosition() >= position);
	}
	
	@Test
	public void testChangingVideoMidStream() throws InterruptedException{
		client.sendToServer("STREAM");
		client.sendToServer("20120213a2");
		Thread.sleep(300);
		assertTrue(connection.mediaPlayer.isPlaying());
		assertTrue(connection.currentlyStreamingvideoID.equals("20120213a2"));
		client.sendToServer("STREAM");
		client.sendToServer("20120102b4");
		Thread.sleep(300);
		assertTrue(connection.mediaPlayer.isPlaying());
		assertTrue(connection.currentlyStreamingvideoID.equals("20120102b4"));
	}
	
	@Test
	public void testThatpercentageWatchedIsRecorded() throws InterruptedException{
		//starts video from position 0
		client.sendToServer("STREAM");
		client.sendToServer("20120213a2");
		assertTrue(connection.loggedInUser.getVideos().get(0).getID().equals("20120213a2"));
		float initialPercentage = connection.loggedInUser.getVideos().get(0).getPercentageWatched();
		Thread.sleep(300);
		Thread.sleep(2000);//stream for a while so that position updates
		client.sendToServer("STREAM POSITION");//will cause list update
		client.sendToServer("PAUSE");
		Thread.sleep(300);
		float finalPercentage = connection.loggedInUser.getVideos().get(0).getPercentageWatched();
		assertTrue(initialPercentage < finalPercentage);
	}
	
	

}
