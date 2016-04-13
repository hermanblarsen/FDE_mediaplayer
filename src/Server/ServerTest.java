package Server;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ServerTest {
	private List<VideoFile> videoList;
	private Server server;
	private VideoFile videoFile0;

	@Before
	public void setUp() throws Exception {
		server= new Server();
		videoList = server.getVideoList();
		videoFile0 = videoList.get(0);
		
	}

	@Test
	public void testIfServerHasList() {
		assertNotNull(videoList);
		assertTrue(videoList.size() > 0);
	}
	
	@Test
	public void verifyContentsVideos(){
		assertTrue(videoFile0.getID().equals("20120213a2"));
		assertTrue(videoFile0.getTitle().equals("Monsters Inc."));
		assertTrue(videoFile0.getFilename().equals("monstersinc_high.mpg"));
	}

}
