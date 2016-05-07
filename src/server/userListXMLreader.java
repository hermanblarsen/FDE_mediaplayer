package src.server;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class userListXMLreader {
	
	public userListXMLreader(){
		
	}
	
	public ArrayList<UserAccount> parseUserAccountList(){
		
		ArrayList<UserAccount> userList = new ArrayList<UserAccount>();
		
		try {
			File accountFile = new File("src/server/video_repository/userList.XML");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document document = dBuilder.parse(accountFile);
			document.getDocumentElement().normalize();
			
			NodeList userNodeList = document.getElementsByTagName("User");
			for (int counter = 0; counter < userNodeList.getLength(); counter++){
				Node currentNode = userNodeList.item(counter);
				Element element = (Element) currentNode;
				String Username = element.getAttribute("id");
				String password = element.getElementsByTagName("password").item(0).getTextContent();
				userList.add(new UserAccount(Username, password));
			}
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		return userList;
	}
	
  
}
