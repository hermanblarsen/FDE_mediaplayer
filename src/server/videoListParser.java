package src.server;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class videoListParser {

	private DOMParser parser;
	private Document doc;

	public videoListParser(String videoListPath){
		parser = new DOMParser();
	    try {
			parser.parse(videoListPath);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    doc = parser.getDocument();
	}
	
	public List<VideoFile> parseVideoList(){
		List<VideoFile> videoList = new ArrayList<VideoFile>();
		//take structure appart
		NodeList root = doc.getElementsByTagName("video");
		for (int i = 0; i < root.getLength(); i++) {
			VideoFile video = new VideoFile();
			Element videoElement = (Element) root.item(i);
			video.setID(videoElement.getAttribute("id"));
			NodeList subElements = videoElement.getChildNodes();
			for (int j = 0; j < subElements.getLength(); j++) {
				Element subElement = (Element) subElements.item(j);
				String elementTag = subElement.getTagName();
				switch (elementTag) {
				case "title":
					video.setTitle(subElement.getTextContent());
					break;
				case "filename":
					video.setFilename(subElement.getTextContent());
					break;
				case "comments":
					//getting all the comment nodes
					ArrayList<String> tempcommentList = (ArrayList<String>) video.getPublicCommentsList();
					NodeList commentList = subElement.getChildNodes();
					for (int k = 0; k < commentList.getLength(); k++) {
						String comment = ((Element)commentList.item(k)).getTextContent();
						tempcommentList.add(comment);
					}
					video.setPublicCommentsList(tempcommentList);
					break;
				default:
					break;
				}
			}
			videoList.add(video);
		}
		return videoList;
	}
}
