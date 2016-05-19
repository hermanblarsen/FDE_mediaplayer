import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({server.ClientConnectionTest.class,client.ClientTest.class,server.UserListXmlParserTest.class,server.VideoListXmlParserTest.class}) 
public class allTests {
	
}
