package server;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * The VideoListXmlParser reads and writes from/to the video list, storing
 * comments, public rating, and more. 
 */
public class VideoListXmlParser {

	private DOMParser xmlParser;
	private Document document;
	private String videoListXmlDatapath = "serverRepository/videoList.xml";

	public VideoListXmlParser(String videoListPath) {
		xmlParser = new DOMParser();
		try {
			xmlParser.parse(videoListPath);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		document = xmlParser.getDocument();
	}

	public List<VideoFile> parseVideoList() {
		List<VideoFile> videoList = new ArrayList<VideoFile>();
		// for each video element in the file get all child nodes
		NodeList root = document.getElementsByTagName("video");
		for (int i = 0; i < root.getLength(); i++) {
			VideoFile video = new VideoFile();
			Element videoElement = (Element) root.item(i);
			video.setID(videoElement.getAttribute("id"));
			NodeList subElements = videoElement.getChildNodes();
			// depending on tag of child node assign the textContend to
			// different field of the video
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
						// getting all the comment nodes
						ArrayList<String> tempcommentList = new ArrayList<String>();
						NodeList commentList = subElement.getChildNodes();
						for (int k = 0; k < commentList.getLength(); k++) {
							if (commentList.item(k).getNodeType() == Node.ELEMENT_NODE) {
								String comment = ((Element) commentList.item(k)).getTextContent();
								tempcommentList.add(comment);
							}
						}
						video.setPublicCommentsList(tempcommentList);
						break;
					case "publicRating":
						video.setPublicRating(Float.parseFloat(subElement.getTextContent()));
						break;
					case "userRating":
						video.setUserRating(Integer.parseInt(subElement.getTextContent()));
						break;
					default:
						break;
					}
				}
			}
			// add parsed video file to list. repeat for all videos
			videoList.add(video);
		}
		return videoList;
	}

	public void writeVideoList(List<VideoFile> videoList) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			// root element
			Element rootElement = doc.createElement("videoList");
			doc.appendChild(rootElement);
			for (VideoFile video : videoList) {
				// video element
				Element videoNode = doc.createElement("video");
				videoNode.setAttribute("id", video.getID());

				Element title = doc.createElement("title");
				title.setTextContent(video.getTitle());
				videoNode.appendChild(title);

				Element filename = doc.createElement("filename");
				filename.setTextContent(video.getFilename());
				videoNode.appendChild(filename);

				// if there are comments available, write them into the xml
				ArrayList<String> temporary_commentList = (ArrayList<String>) video.getPublicCommentsList();
				// check if there is a comment list, and if it exists write it
				// to file,
				if (temporary_commentList != null) {
					Element comments = doc.createElement("comments");
					for (String comment : temporary_commentList) {
						Element temp_comment = doc.createElement("comment");
						temp_comment.setTextContent(comment);
						comments.appendChild(temp_comment);
					}
					videoNode.appendChild(comments);
				}

				Element publicRating = doc.createElement("publicRating");
				publicRating.setTextContent(Float.toString(video.getPublicRating()));
				videoNode.appendChild(publicRating);

				Element userRating = doc.createElement("userRating");
				userRating.setTextContent(Integer.toString(video.getUserRating()));
				videoNode.appendChild(userRating);

				rootElement.appendChild(videoNode);
			}

			try {
				// write the content into xml file
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(videoListXmlDatapath));

				transformer.transform(source, result);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
}
