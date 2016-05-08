package src.client;

import javax.swing.table.AbstractTableModel;

public class VideoTableModel extends AbstractTableModel {
	private String[] columnNames = 
			{"Title",
			"Duration",
			"Favourite"};/*,
			"Tags",
			"Last Watched",
			"Genre",
			"Rating"};*/
	private Object[][] tableData = new Object[100][columnNames.length];
	public VideoTableModel() {
		
	}
	
	public VideoTableModel(int rowNumberNeeded) {
		this.tableData = new Object[rowNumberNeeded][columnNames.length];
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return tableData.length;
		//return 3;
	}

	public String getColumnName(int columnIndex) {
		return this.columnNames[columnIndex];
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return this.tableData[rowIndex][columnIndex];
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		this.tableData[rowIndex][columnIndex] = aValue;
		fireTableCellUpdated(rowIndex, columnIndex);
		fireTableDataChanged();
		//super.setValueAt(aValue, rowIndex, columnIndex);
	}
	
	public Class getColumnClass(int columnIndex) {
		return getValueAt(0,columnIndex).getClass();
	}
	
	public void setTableDataSize(int numberOfVideos) {
		tableData = new Object[numberOfVideos][columnNames.length];
		fireTableDataChanged();
	}
}
