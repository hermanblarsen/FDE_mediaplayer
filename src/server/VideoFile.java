package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VideoFile implements Serializable {

	private String ID;
	private String title;
	private String filename;
	private Boolean isFavourite = false;
	private int durationInSeconds = 0;
	private float publicRating = 0;// the average of all ratings
	private int userRating = 0;// the individual rating of the user
	private float percentageWatched = 0;
	private List<String> publicCommentsList;

	/**
	 * @return the iD
	 */
	public String getID() {
		return this.ID;
	}

	/**
	 * @param iD
	 *            the iD to set
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
	 * @param title
	 *            the title to set
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
	 * @param filename
	 *            the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public float getPercentageWatched() {
		return percentageWatched;
	}

	public void setPercentageWatched(float percentageWatched) {
		if (percentageWatched <= 1 && percentageWatched >= 0) {
			this.percentageWatched = percentageWatched;
		} else {
			this.percentageWatched = 0;
		}
	}

	public int getUserRating() {
		return userRating;
	}

	public void setUserRating(int userRating) {
		this.userRating = userRating;
	}

	public Boolean getIsFavourite() {
		return isFavourite;
	}

	public void setIsFavourite(Boolean isFavourite) {
		this.isFavourite = isFavourite;
	}

	public int getDurationInSeconds() {
		return this.durationInSeconds;
	}

	public void setDurationInSeconds(int durationInSeconds) {
		this.durationInSeconds = durationInSeconds;
	}

	public float getPublicRating() {
		return publicRating;
	}

	public void setPublicRating(float publicRating) {
		this.publicRating = publicRating;
	}

	public List<String> getPublicCommentsList() {
		return publicCommentsList;
	}

	public void setPublicCommentsList(List<String> publicCommentsList) {
		this.publicCommentsList = publicCommentsList;
	}
}
