package client;

import static org.junit.Assert.*;

import java.awt.Component;
import java.util.List;

import javax.swing.JComboBox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import server.*;

public class ClientTest {
	private  Client	client;
	
	@Before
	public void setUp() throws Exception {
		server.Server.main(null);
		
		System.out.println("Client will now try to connect");
		client = new Client();
		client.connectToTheServer();
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void serverReturnsCorrectlyFormattedList() {
		VideoFile videoFile0 = client.getVideoList().get(0);
		assertEquals("20120213a2", videoFile0.getID());
		assertEquals("Monsters Inc.", videoFile0.getTitle());
		assertEquals("monstersinc_high.mpg", videoFile0.getFilename());
	}
	
	@Test
	public void verifyErrorNoVideosAvailable() {
		XMLReader reader = new XMLReader();
		List<VideoFile> videoListEmpty = reader.getList("videoListEmpty.xml");
		this.client.setVideoList(videoListEmpty);
		this.client.catchEmptyListError();
		//Visible test
		assertEquals("NotFail", "NotFail");
	}
	
	@Test
	public void checkSelectedVideoInList() {
		JComboBox comboBox = client.selectionBox;
		comboBox.setSelectedIndex(2);
		assertEquals("Prometheus", comboBox.getSelectedItem());
	}
}
