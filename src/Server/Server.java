package Server;

import java.util.List;

public class Server {
	private List<VideoFile> videoList;
	
	public void Server(){	
	}
	
	public List<VideoFile> getVideoList(){
		XMLReader reader = new XMLReader();
		videoList = reader.getList("videoList.xml");
		return videoList;
	}
}
