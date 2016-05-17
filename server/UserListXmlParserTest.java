package server;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

public class UserListXmlParserTest {

	@Test
	public void testParseUserAccountList() {
		UserListXmlParser reader = new UserListXmlParser();
		ArrayList<UserAccount> userList = (ArrayList<UserAccount>) reader.parseUserAccountList();
		UserAccount user = userList.get(0);
		assertTrue(user.getUserNameID().equals("UserName1"));
		assertTrue(user.getPassword().equals("Password1"));
		user = userList.get(1);
		assertTrue(user.getUserNameID().equals("UserName2"));
		assertTrue(user.getPassword().equals("Password2"));
	}

}
