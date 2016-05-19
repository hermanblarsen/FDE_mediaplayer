package server;

import java.io.Serializable;
import java.util.List;

/**
 * The VideoFile contains information about the video such as ID, title,
 * filename and length(duration). Additionally it can hold user specific
 * details, such as isFavourite, percentage watched and user rating, or public
 * ratings and comments,
 * 
 * 
 * 
 * 
 */
public class VideoFile implements Serializable {

	private String ID;
	private String title;
	private String filename;
	private int durationInSeconds = 0;

	private Boolean isFavourite = false;
	private float percentageWatched = 0;
	private int userRating = 0; // a users individual rating of the movie

	private float publicRating = 0; // the movies average ratings
	private List<String> publicCommentsList;

	public String getID() {
		return this.ID;
	}

	public void setID(String iD) {
		this.ID = iD;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getFilename() {
		return this.filename;
	}

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
