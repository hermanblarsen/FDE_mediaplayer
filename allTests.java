import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ src.server.XMLReaderTest.class, src.client.ClientTest.class}) 
public class allTests {
	
}
