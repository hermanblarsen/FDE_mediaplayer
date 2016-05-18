//package server;
//
//import static org.junit.Assert.*;
//
//import org.junit.Before;
//import org.junit.Test;
//
//import client.Client;
//
//public class ClientConnectionTest {
//	private Server server;
//	private boolean serverRunning = false;
//	private ClientConnection connection;
//	private Client client;
//	private Thread t;
//	
//	@Before
//	public void startServer() throws InterruptedException{
//		if(!serverRunning){
//			//only need to start server once since it can accept multiple clients
//			server = new Server();
//			t = new Thread(server);
//			t.start();
//			serverRunning = true;
//			client = new Client(false);
//			Thread.sleep(200);
//			connection = server.getClientConnection(0);
//			client.login("123", "WrongPassword");
//			serverRunning = true;
//		}
//	}
//	
//	
//	@Test
//	public void testClientConnectsToServer() {
//		assertNotNull(connection);
//	}
//
//}
