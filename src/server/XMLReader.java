package src.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class XMLReader extends DefaultHandler{
	
	VideoFile currentVideofile;
	String currentSubElement;
	List<VideoFile> videoList;
	
	public XMLReader () {
	}
	
	@Override
	public void startDocument() throws SAXException {
        videoList = new ArrayList<VideoFile>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, 
					Attributes attributes) throws SAXException {

		// Sort out element name if (no) namespace in use
        String elementName = localName;
        if ("".equals(elementName)) {
            elementName = qName;
        }

        // Work out what to do with this element
        switch (elementName) {
        case "video":
        	currentVideofile = new VideoFile();
            break;
        case "title":
            currentSubElement = "title";
            break;
        case "filename":
            currentSubElement = "filename";
            break;
        default:
            currentSubElement = "none";
            break;
        }

        // This assumes only one attribute - it will not work for more than one.
        if (attributes != null) {
            // Sort out attribute name
            String attributeName = attributes.getLocalName(0);
            if ("".equals(attributeName)) {
                attributeName = attributes.getQName(0);
            }
            
            // Store value
            String attributeValue = attributes.getValue(0);
            switch (elementName) {
            case "video":
            	currentVideofile.setID(attributeValue);
                break;
            default:
                break;
            }
        }
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String newContent = new String(ch, start, length);
        
        switch (currentSubElement) {
        case "title":
        	currentVideofile.setTitle(newContent);
            break;
        case "filename":
        	currentVideofile.setFilename(newContent);
            break;
        default:
            break;
        }
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		// Finishing an element means we're definitely not in a sub-element anymore
        currentSubElement = "none";

        // Sort out element name if (no) namespace in use
        String elementName = localName;
        if ("".equals(elementName)) {
            elementName = qName;
        }

        if (elementName.equals("video")) {
            videoList.add(currentVideofile);
            // We've finished and stored this video, so remove the reference
            currentVideofile = null;
        }
	}
	
	@Override
	public void endDocument() throws SAXException {
		//No action appears necessary
	}

	
	/**
	 * @param filenameList Filename of videolist, "filename + .xml"
	 * @return videoList containing the VideoFiles from the xml
	 */
	public List<VideoFile> getList(String filenameList) {
		try {
            // use the default parser
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // parse the input
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(filenameList, this);
		}
			catch (ParserConfigurationException pce) {
				pce.printStackTrace();
		}
			catch (SAXException saxe) {
				saxe.printStackTrace();
		}
			catch (IOException ioe) {
				ioe.printStackTrace();
		}
		return this.videoList;
	}

}
