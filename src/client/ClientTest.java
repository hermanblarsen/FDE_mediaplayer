package src.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import javax.swing.JComboBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import src.server.*;

public class ClientTest {
	private  Client	testClient;
	
	//TODO IMPORTANT: When testing, streaming and mediaplayer must be commented out from server//client
	
	@Before 
	public void setUp() throws Exception {
		System.out.println("NB:NB:NB:NB:NB:NB:NB: TURN OFF MEDIAPLAYER AND STREAMING BEFORE RUNNING THIS TEST, OR ELSE...: WON'T RUN!");
		src.server.Server.main(null);
		testClient = new Client();
		testClient.connectToTheServer();
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void serverReturnsCorrectlyFormattedList() {
		VideoFile videoFile0 = testClient.getVideoList().get(0);
		assertEquals("20120213a2", videoFile0.getID());
		assertEquals("Monsters Inc.", videoFile0.getTitle());
		assertEquals("monstersinc_high.mpg", videoFile0.getFilename());
	}
		
	@Test
	public void verifySelectedVideoInList() {
		JComboBox comboBox = testClient.selectionBox;
		comboBox.setSelectedIndex(2);
		assertEquals("Prometheus", comboBox.getSelectedItem());
	}
	
	//Unnecessary, see combined test case below*2
	/*@Test 
	public void verifyErrorNoVideosAvailable() {
		XMLReader reader = new XMLReader();
		List<VideoFile> videoListEmpty = reader.getList("testLists/videoListEmpty.xml");
		this.testClient.setVideoList(videoListEmpty);
		this.testClient.validateVideoListContentsAndFormat();
		assertNotNull(this.testClient.errorOptionPane);
	}
	//Unnecessary, see combined test case below
	@Test
	public void verifyCorrectListFormat(){
		XMLReader reader = new XMLReader();
		List<VideoFile> videoListInvalid = reader.getList("testLists/videoListInvalid.xml");
		testClient.setVideoList(videoListInvalid);
		assertFalse(testClient.validateVideoListContentsAndFormat());
	}*/
	
	@Test
	public void verifyErrorListContentOrFormat() {
		XMLReader reader = new XMLReader();
		
		//Asserts that no errors spawn with a correct list with videos in. errorOptionPane is still null
		List<VideoFile> videoListCorrect = reader.getList("testLists/videoList.xml");
		testClient.setVideoList(videoListCorrect);
		assertTrue(testClient.validateVideoListContentsAndFormat());
		assertNull(this.testClient.errorOptionPane);
		
		//Asserts that an invalid formatted video list will make the validation method false 
		List<VideoFile> videoListInvalid = reader.getList("testLists/videoListInvalid.xml");
		testClient.setVideoList(videoListInvalid);
		assertFalse(testClient.validateVideoListContentsAndFormat());
		assertNull(this.testClient.errorOptionPane);
		
		//Asserts that an empty video list will spawn an error message/popup box
		List<VideoFile> videoListEmpty = reader.getList("testLists/videoListEmpty.xml");
		this.testClient.setVideoList(videoListEmpty);
		this.testClient.validateVideoListContentsAndFormat();
		assertNotNull(this.testClient.errorOptionPane);
	}
	
	@Test
	public void verifyClientLoginworks(){
		Boolean loginStatus = testClient.login("TestUser","password");
		assertTrue(loginStatus);
		
		loginStatus = testClient.login("TestUser","wrongPassword");
		assertFalse(loginStatus);
		
	}
}
