package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The UserAccount currently stores its own credentials (username/password) as
 * well as a user specific video list with details about how the user has
 * interacted with the specific videos.
 */
public class UserAccount implements Serializable {
	private String username;
	private String password;
	private List<VideoFile> userSpecificVideoList = new ArrayList<VideoFile>();

	public String getUserNameID() {
		return this.username;
	}

	public List<VideoFile> getVideos() {
		return this.userSpecificVideoList;
	}

	public void setVideos(List<VideoFile> videoList) {
		this.userSpecificVideoList = videoList;
	}

	public void setUserNameID(String userNameID) {
		this.username = userNameID;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public UserAccount(String userName, String password) {
		this.username = userName;
		this.password = password;
	}
}
