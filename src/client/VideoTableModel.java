package src.client;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import src.server.VideoFile;

public class VideoTableModel extends AbstractTableModel {
	private List<VideoFile> videoList;
	private String[] columnNames = {"Title",
									"Duration",
									"Favourite"};/*,
									"Tags",
									"Last Watched",
									"Genre",
									"Rating"};*/
	private Object[][] tableData = {
		{"A Title", "3.00", "isFavourite"},
		{"A Second Title", "3.00", "isntFavourite"}};

	public VideoTableModel(List<VideoFile> videoList) {
		this.videoList = videoList;
		//Generate the tableData here?
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return tableData.length;
	}

	public String getColumnName(int columnNumber) {
		return columnNames[columnNumber];
	}
	
	@Override
	public Object getValueAt(int row, int column) {
		return tableData[row][column];
	}

	public Class getColumnClass(int c) {
		return getValueAt(0,c).getClass();
	}
	
	public String getVideoID(int row) {
		String videoID = "";
		for(VideoFile eachVideo : this.videoList){
			if (tableData[row][0].equals(eachVideo.getTitle())){
				videoID = eachVideo.getID();
			}
		}
		return videoID;
	}
}
