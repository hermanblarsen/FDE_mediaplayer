package server;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Checking that the Video list parser works to specifications
 */
public class VideoListXmlParserTest {

	private VideoListXmlParser xmlParser;
	private List<VideoFile> videoList;
	private VideoFile videoFile0;
	private VideoFile videoFile1;
	private VideoFile videoFile2;

	@Before
	public void setUp() throws Exception {
		xmlParser = new VideoListXmlParser("testLists/videoList.xml");
		videoList = xmlParser.parseVideoList();
		videoFile0 = videoList.get(0);
		videoFile1 = videoList.get(1);
		videoFile2 = videoList.get(2);
	}

	@Test
	public void verifyCreationOfVideoList() {
		assertTrue(videoList instanceof List);
	}

	@Test
	public void verifyParsedObjectTypes() {
		assertTrue(videoList.get(0) instanceof VideoFile);
		assertTrue(videoList.get(1) instanceof VideoFile);
		assertTrue(videoList.get(2) instanceof VideoFile);
	}

	@Test
	public void verifyNotNullVideoFileContents() {
		assertNotNull(videoFile0.getID());
		assertNotNull(videoFile0.getTitle());
		assertNotNull(videoFile0.getFilename());
	}

	@Test
	public void verifyContentsOfVideoFiles() {
		assertTrue(videoFile0.getID().equals("20120213a2"));
		assertTrue(videoFile0.getTitle().equals("Monsters Inc."));
		assertTrue(videoFile0.getFilename().equals("monstersinc_high.mpg"));

		assertTrue(videoFile1.getID().equals("20120102b7"));
		assertTrue(videoFile1.getTitle().equals("Avengers"));
		assertTrue(videoFile1.getFilename().equals("avengers-featurehp.mp4"));

		assertTrue(videoFile2.getID().equals("20120102b4"));
		assertTrue(videoFile2.getTitle().equals("Prometheus"));
		assertTrue(videoFile2.getFilename().equals("prometheus-featureukFhp.mp4"));
	}
}
