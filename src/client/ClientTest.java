package client;

import static org.junit.Assert.*;

import java.awt.Color;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import javax.swing.JComboBox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import com.sun.jmx.snmp.tasks.ThreadService;

import junit.framework.Assert;
import server.*;

public class ClientTest {
	private  Client	testClient;
	private Server server;
	private static Thread t;
	
	@Before 
	public void setUp() throws Exception {
		
	}
	
	@After
	public void tearDown() throws Exception{
		testClient.closeConnection();
	}
	
	@BeforeClass
	public static void setUpServer(){
		Server server = new Server();
		t = new Thread(server);
		t.start();
	}

	@Test
	public void ClientLoginWithWrongPassword(){
		testClient = new Client(false);
		assertFalse(testClient.login("wrongUser", "WrongPassword"));
	}
	
	@Test
	public void ClientLoginWithRightPassword() throws InterruptedException {

		testClient = new Client(false);
		assertTrue(testClient.login("UserName1", "Password1"));
	}	
	
	@Test
	public void ClientReceivedCorrectlyFormattedList(){
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		assertTrue(testClient.validateVideoListContentsAndFormat());
	}
	
	@Test
	public void SelectingVideoAndStreaming(){
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
	}
	
	@Test
	public void SelectingVideoStreamingAndStopping(){
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
		//video streams now
		testClient.stopButton.doClick();
		//stream should be stopped
		assertFalse(testClient.mediaPlayer.isPlaying());
	}
	
	@Test
	public void SelectingVideoStreamingAndPausingThenResuming() throws InterruptedException{
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
		//video streams now
		testClient.playPauseButton.doClick();
		//stream should be paused
		Thread.sleep(100);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.playPauseButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
	}
	
	@Test
	public void SelectingVideoStreamingAndStoppingThenRestarting() throws InterruptedException{
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
		//video streams now
		testClient.stopButton.doClick();
		//stream should be paused
		Thread.sleep(100);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.playPauseButton.doClick();
		Thread.sleep(100);
		assertTrue(testClient.mediaPlayer.isPlaying());
	}
	
	@Test
	public void VideoPausesWhenTabChanged() throws InterruptedException{
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		testClient.listTable.setRowSelectionInterval(0, 0);
		testClient.streamSelectedVideoButton.doClick();
		Thread.sleep(100);
		testClient.tabbedPane.setSelectedIndex(0);
		assertFalse(testClient.mediaPlayer.isPlaying());
	}
	
	@Test
	public void TestTheStatusBar(){
		//start client in test mode so that it does not automatically connect.
		testClient = new Client(true);
		testClient.writeStatus("", Color.WHITE);
		assertTrue(testClient.clientStatusBar.getBackground() == Color.WHITE);
		assertTrue(testClient.clientStatusBar.getText().equals(""));
		
		testClient.writeStatus("TEST TEST TEST", Color.RED);
		assertTrue(testClient.clientStatusBar.getBackground() == Color.RED);
		assertTrue(testClient.clientStatusBar.getText().equals("TEST TEST TEST"));
		
		testClient.writeStatus("abcdefghijklmnopqrstuvwxyz", Color.GREEN);
		assertTrue(testClient.clientStatusBar.getBackground() == Color.GREEN);
		assertTrue(testClient.clientStatusBar.getText().equals("abcdefghijklmnopqrstuvwxyz"));
	}
	
	@Test
	public void TestThatTheCorrectUserLogsIn(){
		testClient = new Client(false);
		//make sure that the logged in user is null before login
		assertNull(testClient.currentUser);
		//login to useraccount with the most creative username and the most secure password
		testClient.login("123", "123");
		//now test that the user is actually logged in
		assertTrue(testClient.currentUser.getUserNameID().equals("123"));
	}
	
	@Test
	public void SkipToVideoPosition() throws InterruptedException{
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
		Thread.sleep(3000);//wait for the stream position to update
		float oldPosition = testClient.severStreamPosition;
		//set the value of the position slider which should fire the skip event.
		testClient.positionTimeSlider.setValue(80);
		Thread.sleep(6000);//wait for the stream position to update
		assertTrue(oldPosition < testClient.severStreamPosition);
	}
	
	@Test
	public void resumeFromPreviousPosition() throws InterruptedException{
		testClient = new Client(false);
		//log in 
		testClient.login("UserName1", "Password1");
		testClient.listTable.setRowSelectionInterval(0, 0);
		testClient.streamSelectedVideoButton.doClick();
		//skip to some position
		testClient.positionTimeSlider.setValue(80);
		float oldStreamPosition = testClient.severStreamPosition;
		//switch back to the video tab
		testClient.tabbedPane.setSelectedIndex(0);
		testClient.listTable.setRowSelectionInterval(0, 0);
		Thread.sleep(6000);//wait for the stream position to update
		testClient.streamSelectedVideoButton.doClick();
		Thread.sleep(6000);//wait for the stream position to update
		float currentStreamPosition = testClient.severStreamPosition;
		assertTrue(currentStreamPosition >= oldStreamPosition);
	}
	
	

}
