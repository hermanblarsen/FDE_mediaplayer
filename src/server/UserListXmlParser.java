package server;

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

public class UserListXmlParser {
	
	private String listOfUserDetailsDatapath = "serverRepository/userList.XML";
	public UserListXmlParser(){
		
	}
	
	public ArrayList<UserAccount> parseUserAccountList(){
		
		ArrayList<UserAccount> userList = new ArrayList<UserAccount>();
		
		try {
			File accountFile = new File(listOfUserDetailsDatapath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document document = dBuilder.parse(accountFile);
			document.getDocumentElement().normalize();
			
			NodeList userNodeList = document.getElementsByTagName("User");
			for (int counter = 0; counter < userNodeList.getLength(); counter++){
				if(!(userNodeList.item(counter).getNodeType() == Node.ELEMENT_NODE)){
					continue;
				}
				Node currentNode = userNodeList.item(counter);
				Element element = (Element) currentNode;
				String Username = element.getAttribute("id");
				String password = element.getElementsByTagName("password").item(0).getTextContent();
				
				//create the new user account
				UserAccount account = new UserAccount(Username, password);
				
				//obtain the <videoList> element, which contains all <video> elements
				Element videoListElement = (Element) element.getElementsByTagName("videoList").item(0);
				
				//create an ArrayList to store videoFiles
				ArrayList<VideoFile> videoList = new ArrayList<VideoFile>();
				//obtain all the <video> elements inside <videoList>
				NodeList videoElementList = videoListElement.getElementsByTagName("video");
				for (int i = 0; i < videoElementList.getLength(); i++) {
					if (!(videoElementList.item(i).getNodeType() == Node.ELEMENT_NODE)) {
						continue;
					}
					VideoFile tempVideo = new VideoFile();
					Element videoTag = (Element) videoElementList.item(i);
					tempVideo.setID(videoTag.getAttribute("id"));
					//now get the child elements of <video>
					NodeList videoChildren = videoTag.getChildNodes();
					int loops = videoChildren.getLength();
					for (int ii = 0; ii < loops; ii++) {
						if (videoChildren.item(ii) == null){
							continue;
						}
						if (!(videoChildren.item(ii).getNodeType() == Node.ELEMENT_NODE)) {
							continue;
						}
						Element Tag = (Element) videoChildren.item(ii);
						String tagName = Tag.getTagName();
						switch (tagName) {
						case "favourite":
							boolean favourite = Boolean.parseBoolean(Tag.getTextContent());
							tempVideo.setIsFavourite(favourite);
							break;
						case "percentageWatched":
							float percentageWatched = Float.parseFloat(Tag.getTextContent());
							tempVideo.setPercentageWatched(percentageWatched);
							break;
						case "rating":
							int rating = Integer.parseInt(Tag.getTextContent());
							tempVideo.setUserRating(rating);
							break;
						default:
							break;
						}
					}
					videoList.add(tempVideo);
				}
				account.setVideos(videoList);
				//add acount to list
				userList.add(account);
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
