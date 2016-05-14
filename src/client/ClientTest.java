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
		Server theServer = new Server();
		Thread T = new Thread(theServer);
		T.start();
		testClient = new Client();
		testClient.testMode = true;
		testClient.connectToTheServer();
	}


	@Test
	public void ClientLoginWithWrongPassword() {
		assertFalse(testClient.login("wrongUser", "WrongPassword"));
	}
	
	@Test
	public void ClientLoginWithRightPassword() {
		assertTrue(testClient.login("UserName1", "Password1"));
	}	
	
	
	
	
}
