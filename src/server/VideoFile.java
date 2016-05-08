package src.server;

import java.io.Serializable;
import java.util.List;

public class VideoFile implements Serializable {

	private String ID;
	private String title;
	private String filename;
	private int durationInSeconds=180;
	private int publicRating;
	private List<String> publicCommentsList; 
	
	
	/**
	 * @return the iD
	 */
	public String getID() {
		return this.ID;
	}
	/**
	 * @param iD the iD to set
	 */
	public void setID(String iD) {
		this.ID = iD;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return this.title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return this.filename;
	}
	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
