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
		{"An Example Title", "3.00", "isFavourite"},
		{"A Second Example Title", "3.00", "isntFavourite"}};
	public VideoTableModel() {
		
	}
	
	public VideoTableModel(Object[][] tableData) {
		
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return tableData.length;
	}

	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return tableData[rowIndex][columnIndex];
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		super.setValueAt(aValue, rowIndex, columnIndex);
	}

	public Class getColumnClass(int columnIndex) {
		return getValueAt(0,columnIndex).getClass();
	}
	
	public void addVideoListData(List<VideoFile> videoList) {
		this.videoList = videoList;
		int counter = 0;
		for(VideoFile eachVideo : this.videoList){
				
		}
	}
	public String getVideoID(int rowIndex) {
		String videoID = "";
		for(VideoFile eachVideo : this.videoList){
			if (tableData[rowIndex][0].equals(eachVideo.getTitle())){
				videoID = eachVideo.getID();
			}
		}
		return videoID;
	}
}
