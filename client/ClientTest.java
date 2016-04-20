package client;

import static org.junit.Assert.*;

import java.util.List;

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
}
