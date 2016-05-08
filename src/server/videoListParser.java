package src.server;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
				if (subElements.item(j).getNodeType() == Node.ELEMENT_NODE) {
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
							if(commentList.item(k).getNodeType() == Node.ELEMENT_NODE){
								String comment = ((Element) commentList.item(k)).getTextContent();
								try {
									tempcommentList.add(comment);
								} catch (Exception e) {
								}
							}
						}
						video.setPublicCommentsList(tempcommentList);
						break;
					default:
						break;
					}
				}
			}
			videoList.add(video);
		}
		return videoList;
	}
	
	public void writeVideoList(ArrayList<VideoFile> videoList){
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			//root element
			Element rootElement = doc.createElement("videoList");
			doc.appendChild(rootElement);
			for (VideoFile video : videoList) {
				//video element
				Element videoNode = doc.createElement("video");
				videoNode.setAttribute("id", video.getID());
				
				Element title = doc.createElement("title");
				title.setTextContent(video.getTitle());
				videoNode.appendChild(title);
				
				Element filename = doc.createElement("filename");
				filename.setTextContent(video.getFilename());
				videoNode.appendChild(filename);
				
				Element comments = doc.createElement("comments");
				for(String comment : video.getPublicCommentsList()){
					Element temp_comment = doc.createElement("comment");
					temp_comment.setTextContent(comment);
					comments.appendChild(temp_comment);
				}
				videoNode.appendChild(comments);
				
				rootElement.appendChild(videoNode);
			}
			
			try {
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("C:\\file.xml"));

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			
				transformer.transform(source, result);
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
	}
	
}
