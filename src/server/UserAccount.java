package src.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserAccount implements Serializable{
	private String userNameID;
	private String password;
	private List<VideoFile> Videos = new ArrayList<VideoFile>();
	
	public String getUserNameID() {
		return userNameID;
	}

	public List<VideoFile> getVideos() {
		return Videos;
	}

	public void setVideos(List<VideoFile> videos) {
		Videos = videos;
	}

	public void setUserNameID(String userNameID) {
		this.userNameID = userNameID;
	}

	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	
	
	public UserAccount(String userName, String password){
		this.userNameID = userName;
		this.password = password;
	}
}
