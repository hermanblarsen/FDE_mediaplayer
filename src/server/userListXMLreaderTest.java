package src.server;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

public class userListXMLreaderTest {

	@Test
	public void testParseUserAccountList() {
		userListXMLreader reader = new userListXMLreader();
		ArrayList<UserAccount> userList = (ArrayList<UserAccount>) reader.parseUserAccountList();
		UserAccount user = userList.get(0);
		assertTrue(user.getUserNameID().equals("UserName1"));
		assertTrue(user.getPassword().equals("Password1"));
		user = userList.get(1);
		assertTrue(user.getUserNameID().equals("UserName2"));
		assertTrue(user.getPassword().equals("Password2"));
	}

}
