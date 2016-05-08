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
	
	public VideoTableModel(Object[][] tableData) {
		
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return 0;
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
		this.tableData[rowIndex][columnIndex] = aValue;
		fireTableCellUpdated(rowIndex, columnIndex);
		//super.setValueAt(aValue, rowIndex, columnIndex);
	}
	
	public Class getColumnClass(int columnIndex) {
		return getValueAt(0,columnIndex).getClass();
	}
}
