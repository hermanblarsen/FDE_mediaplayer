package server;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

/**
 * Testing the operation of the UserListXmlParser, including list contents, list
 * parsing, and writing to the list.
 */
public class UserListXmlParserTest {

	private ArrayList<UserAccount> userList;
	private UserListXmlParser reader;

	@Before
	public void setup() {
		reader = new UserListXmlParser();
		userList = (ArrayList<UserAccount>) reader.parseUserAccountList();
	}

	@Test
	public void testCorrectAccountsInList() {
		// testing the first 3 accounts that should be in the list
		UserAccount user = userList.get(0);
		assertTrue(user.getUserNameID().equals("UserName1"));
		assertTrue(user.getPassword().equals("Password1"));
		user = userList.get(1);
		assertTrue(user.getUserNameID().equals("UserName2"));
		assertTrue(user.getPassword().equals("Password2"));
		user = userList.get(2);
		assertTrue(user.getUserNameID().equals("UserName3"));
		assertTrue(user.getPassword().equals("Password3"));
	}

	@Test
	public void verifyListIsParsedCorrectly() {
		UserAccount user = userList.get(0);
		assertFalse(user.getVideos().isEmpty());
		ArrayList<VideoFile> videoList = (ArrayList<VideoFile>) user.getVideos();
		boolean MonstersInc = false, Avengers = false, Prometheus = false;
		for (VideoFile video : videoList) {
			String videoID = video.getID();
			if (videoID.equals("20120213a2")) {
				MonstersInc = true;
			}
			if (videoID.equals("20120102b7")) {
				Avengers = true;
			}
			if (videoID.equals("20120102b4")) {
				Prometheus = true;
			}
		}
		assertTrue(MonstersInc && Avengers && Prometheus);
	}

	@Test
	public void verifyListIsWrittenCorrectly() {
		// create a new user account
		UserAccount newUser = new UserAccount("testUser", "test123");
		// add videos from a user to the new account
		newUser.getVideos().addAll(userList.get(0).getVideos());
		// modify the added list
		for (VideoFile video : newUser.getVideos()) {
			String videoID = video.getID();
			if (videoID.equals("20120213a2")) {
				video.setIsFavourite(true);
				video.setPercentageWatched(0.33f);
				video.setUserRating(3);
			}
			if (videoID.equals("20120102b7")) {
				video.setIsFavourite(false);
				video.setPercentageWatched(0.0f);
				video.setUserRating(0);
			}
			if (videoID.equals("20120102b4")) {
				video.setIsFavourite(true);
				video.setPercentageWatched(0.8f);
				video.setUserRating(5);
			}
		}
		userList.add(newUser);
		reader.writeUserListToXML(userList);
		// now re read the list and check if the information previously added is
		// present
		userList = null;// make sure the previous list is gone
		userList = reader.parseUserAccountList();
		newUser = userList.get(userList.size() - 1);
		assertTrue(newUser.getPassword().equals("test123"));
		assertTrue(newUser.getUserNameID().equals("testUser"));
		for (VideoFile video : newUser.getVideos()) {
			String videoID = video.getID();
			boolean isFavourite = video.getIsFavourite();
			float percentageWatched = video.getPercentageWatched();
			int rating = video.getUserRating();
			if (videoID.equals("20120213a2")) {
				assertTrue(isFavourite == true);
				assertTrue(percentageWatched == 0.33f);
				assertTrue(rating == 3);
			}
			if (videoID.equals("20120102b7")) {
				assertTrue(isFavourite == false);
				assertTrue(percentageWatched == 0.0f);
				assertTrue(rating == 0);
			}
			if (videoID.equals("20120102b4")) {
				assertTrue(isFavourite == true);
				assertTrue(percentageWatched == 0.8f);
				assertTrue(rating == 5);
			}
		}
		// now to clean up remove the latest addition and reqrite list
		userList.remove(userList.size() - 1);
		reader.writeUserListToXML(userList);
	}
}
