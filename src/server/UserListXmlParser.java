package server;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * 
 * 
 * 
 * 
 */
public class UserListXmlParser {

	private String listOfUserDetailsDatapath = "serverRepository/userList.XML";

	public UserListXmlParser() {

	}

	public ArrayList<UserAccount> parseUserAccountList() {

		ArrayList<UserAccount> userList = new ArrayList<UserAccount>();

		try {
			// Setting up document from XML file
			File accountFile = new File(listOfUserDetailsDatapath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document document = dBuilder.parse(accountFile);
			document.getDocumentElement().normalize();

			// Parsing of document begins
			NodeList userNodeList = document.getElementsByTagName("User");
			for (int counter = 0; counter < userNodeList.getLength(); counter++) {
				if (!(userNodeList.item(counter).getNodeType() == Node.ELEMENT_NODE)) {
					continue;
				}
				Node currentNode = userNodeList.item(counter);
				Element element = (Element) currentNode;
				String username = element.getAttribute("id");
				String password = element.getElementsByTagName("password").item(0).getTextContent();

				// create the new user account
				UserAccount account = new UserAccount(username, password);

				// obtain the <videoList> element, which contains all <video>
				// elements
				Element videoListElement = (Element) element.getElementsByTagName("videoList").item(0);

				// create an ArrayList to store videoFiles
				ArrayList<VideoFile> videoList = new ArrayList<VideoFile>();
				// obtain all the <video> elements inside <videoList>
				NodeList videoElementList = videoListElement.getElementsByTagName("video");
				for (int i = 0; i < videoElementList.getLength(); i++) {
					if (!(videoElementList.item(i).getNodeType() == Node.ELEMENT_NODE)) {
						continue;
					}
					VideoFile tempVideo = new VideoFile();
					Element videoTag = (Element) videoElementList.item(i);
					tempVideo.setID(videoTag.getAttribute("id"));
					// now get the child elements of <video>
					NodeList videoChildren = videoTag.getChildNodes();
					int loops = videoChildren.getLength();
					for (int ii = 0; ii < loops; ii++) {
						if (videoChildren.item(ii) == null) {
							continue;
						}
						if (!(videoChildren.item(ii).getNodeType() == Node.ELEMENT_NODE)) {
							continue;
						}
						Element tag = (Element) videoChildren.item(ii);
						String tagName = tag.getTagName();
						switch (tagName) {
						case "favourite":
							boolean favourite = Boolean.parseBoolean(tag.getTextContent());
							tempVideo.setIsFavourite(favourite);
							break;
						case "percentageWatched":
							float percentageWatched = Float.parseFloat(tag.getTextContent());
							tempVideo.setPercentageWatched(percentageWatched);
							break;
						case "rating":
							int rating = Integer.parseInt(tag.getTextContent());
							tempVideo.setUserRating(rating);
							break;
						default:
							break;
						}
					}
					videoList.add(tempVideo);
				}
				account.setVideos(videoList);
				// add acount to list
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

	public void writeUserListToXML(ArrayList<UserAccount> userList) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			Element rootElement = doc.createElement("userList");
			doc.appendChild(rootElement);

			for (UserAccount user : userList) {

				Element userElement = doc.createElement("User");
				userElement.setAttribute("id", user.getUserNameID());

				Element password = doc.createElement("password");
				password.setTextContent(user.getPassword());
				userElement.appendChild(password);

				Element videoList = doc.createElement("videoList");
				for (VideoFile video : user.getVideos()) {
					Element videoElement = doc.createElement("video");
					videoElement.setAttribute("id", video.getID());

					Element favourite = doc.createElement("favourite");
					favourite.setTextContent(Boolean.toString(video.getIsFavourite()));
					videoElement.appendChild(favourite);

					Element percentageWatched = doc.createElement("percentageWatched");
					percentageWatched.setTextContent(Float.toString(video.getPercentageWatched()));
					videoElement.appendChild(percentageWatched);

					Element rating = doc.createElement("rating");
					rating.setTextContent(Integer.toString(video.getUserRating()));
					videoElement.appendChild(rating);

					videoList.appendChild(videoElement);
				}
				userElement.appendChild(videoList);
				rootElement.appendChild(userElement);
			}

			try {
				// write the content into xml file
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(listOfUserDetailsDatapath));
				transformer.transform(source, result);
			} catch (TransformerException e) {
				e.printStackTrace();
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

}
