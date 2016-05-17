package client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import javax.swing.JComboBox;
import org.junit.After;
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
	//TODO IMPORTANT: When testing, streaming and mediaplayer must be commented out from server//client
	
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
		Thread T = new Thread(server);
		T.start();
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
	

}
