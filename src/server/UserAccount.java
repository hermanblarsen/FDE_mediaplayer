package src.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserAccount implements Serializable{
	private String userNameID;
	public String getUserNameID() {
		return userNameID;
	}

	public void setUserNameID(String userNameID) {
		this.userNameID = userNameID;
	}

	private String password;
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	private List<VideoFile> favouriteVideos = new ArrayList<VideoFile>();
	
	public UserAccount(String userName, String password){
		this.userNameID = userName;
		this.password = password;
	}
}
