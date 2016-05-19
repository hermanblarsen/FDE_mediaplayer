package client;

import static org.junit.Assert.*;

import java.awt.Color;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import server.*;

/**
 * The ClientTest tests various functions in the client, including working login
 * features, verification if correct list and content, and correct behaviour of
 * media player components and features (play, pause, stop, stream, stream from
 * last position(continue), etc. 
 */
public class ClientTest {
	private Client testClient;
	private static Thread serverThread;

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
		testClient.closeConnection();
	}

	@BeforeClass
	public static void setUpServer() {
		Server server = new Server();
		serverThread = new Thread(server);
		serverThread.start();
	}

	@Test
	public void clientDeniedAccessWithWrongCredentials() {
		testClient = new Client(false);
		assertFalse(testClient.login("wrongUser", "WrongPassword"));
	}

	@Test
	public void clientGrantedAccessWithCorrectCredentials() throws InterruptedException {
		testClient = new Client(false);
		assertTrue(testClient.login("UserName1", "Password1"));
	}

	@Test
	public void clientReceivesCorrectlyFormattedList() {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		assertTrue(testClient.validateVideoListContentsAndFormat());
	}

	@Test
	public void verifyVideoSelectionAndStreaming() {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
	}

	@Test
	public void verifyPlayStop() {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
		// video streams,click stop
		testClient.stopButton.doClick();
		assertFalse(testClient.mediaPlayer.isPlaying());
	}

	@Test
	public void verifyPlayPauseAndResumedPlayback() throws InterruptedException {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
		// video streams, click pause
		testClient.playPauseButton.doClick();
		Thread.sleep(100);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.playPauseButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
	}

	@Test
	public void verifyStopAndResumedPlayback() throws InterruptedException {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());
		// video streams, click stop
		testClient.stopButton.doClick();
		Thread.sleep(100);
		assertFalse(testClient.mediaPlayer.isPlaying());
		testClient.playPauseButton.doClick();
		Thread.sleep(100);
		assertTrue(testClient.mediaPlayer.isPlaying());
	}

	@Test
	public void verifyPausedVideoWhenExitingVideoTab() throws InterruptedException {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		testClient.listTable.setRowSelectionInterval(0, 0);
		testClient.streamSelectedVideoButton.doClick();
		Thread.sleep(100);
		testClient.tabbedPane.setSelectedIndex(0);
		assertFalse(testClient.mediaPlayer.isPlaying());
	}

	@Test
	public void verifyCorrectOutputStatusBar() {
		// start client in test mode so that it does not automatically connect
		// to server, so no interference inputs fails the test
		testClient = new Client(true);
		testClient.writeStatus("", Color.WHITE);
		assertTrue(testClient.clientStatusBar.getBackground() == Color.WHITE);
		assertTrue(testClient.clientStatusBar.getText().equals(""));

		testClient.writeStatus("THIS IS !(not) A TEST", Color.RED);
		assertTrue(testClient.clientStatusBar.getBackground() == Color.RED);
		assertTrue(testClient.clientStatusBar.getText().equals("THIS IS !(not) A TEST"));

		testClient.writeStatus("THIS IS NOT A SPELLING MISTAKE, BUT A TEST! HUE HUE HUE", Color.GREEN);
		assertTrue(testClient.clientStatusBar.getBackground() == Color.GREEN);
		assertTrue(
				testClient.clientStatusBar.getText().equals("THIS IS NOT A SPELLING MISTAKE, BUT A TEST! HUE HUE HUE"));
	}

	@Test
	public void verifyThatCorrectUserIsLoggedIn() {
		testClient = new Client(false);
		// make sure that the logged in user is null before login
		assertNull(testClient.currentUser);

		testClient.login("123", "123");
		// now test that the user is actually logged in
		assertTrue(testClient.currentUser.getUserNameID().equals("123"));
	}

	@Test
	public void verifySkipppingToVideoPosition() throws InterruptedException {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		testClient.listTable.setRowSelectionInterval(0, 0);
		assertFalse(testClient.mediaPlayer.isPlaying());

		testClient.streamSelectedVideoButton.doClick();
		assertTrue(testClient.mediaPlayer.isPlaying());

		Thread.sleep(3000);// wait for the stream position to update
		float oldPosition = testClient.severStreamPosition;
		// set the value of the position slider which should fire the skip
		// event.
		testClient.positionTimeSlider.setValue(80);
		Thread.sleep(6000);// wait for the stream position to update (slightly
							// laggy, hence the larger delay)
		assertTrue(oldPosition < testClient.severStreamPosition);
	}

	@Test
	public void verifyContinuePlayback() throws InterruptedException {
		testClient = new Client(false);
		testClient.login("UserName1", "Password1");

		testClient.listTable.setRowSelectionInterval(0, 0);
		testClient.streamSelectedVideoButton.doClick();

		// skip to some position
		testClient.positionTimeSlider.setValue(80);
		float oldStreamPosition = testClient.severStreamPosition;
		// switch back to the video tab
		testClient.tabbedPane.setSelectedIndex(0);
		testClient.listTable.setRowSelectionInterval(0, 0);
		Thread.sleep(6000);// wait for the stream position to update
		testClient.streamSelectedVideoFromLastPositionButton.doClick();
		Thread.sleep(6000);// wait for the stream position to update
		float currentStreamPosition = testClient.severStreamPosition;
		assertTrue(currentStreamPosition >= oldStreamPosition);
	}
}
