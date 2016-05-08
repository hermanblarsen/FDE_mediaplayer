package src.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VideoFile implements Serializable {

	private String ID;
	private String title;
	private String filename;
	private Boolean isFavourite;
	private int durationInSeconds;
	protected int publicRating;
	protected List<String> publicCommentsList; 

	
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
	
	public Boolean getIsFavourite() {
		//return isFavourite;
		return false;
	}
	public void setIsFavourite(Boolean isFavourite) {
		this.isFavourite = isFavourite;
	}
	public int getDurationInSeconds() {
		//return durationInSeconds;
		return 180;
	}
	public void setDurationInSeconds(int durationInSeconds) {
		this.durationInSeconds = durationInSeconds;
	}
	public int getPublicRating() {
		return publicRating;
	}
	public void setPublicRating(int publicRating) {
		this.publicRating = publicRating;
	}
	public List<String> getPublicCommentsList() {
		return publicCommentsList;
	}
	public void setPublicCommentsList(List<String> publicCommentsList) {
		this.publicCommentsList = publicCommentsList;
	}
	
	
}
