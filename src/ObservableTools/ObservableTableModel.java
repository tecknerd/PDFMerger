package ObservableTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

public class ObservableTableModel extends DefaultTableModel implements iObservable {

	private List<iObserver> observers;

	public ObservableTableModel() {
		super();
		observers = new ArrayList<iObserver>();
	}

	public ObservableTableModel (int numRows, int numCols) {
		super(numRows, numCols);
		observers = new ArrayList<iObserver>();
	}

	public ObservableTableModel (int numRows, int numCols, iObserver[] observers) {
		this(numRows, numCols);
		register(observers);
	}

	@Override
	public void addRow(Object[] rowData) {
		super.addRow(rowData);
		notifyObservers();
	}

	@Override
	public void addRow(Vector rowData) {
		super.addRow(rowData);
		notifyObservers();
	}

	@Override
	public void insertRow(int row, Object[] rowData) {
		super.insertRow(row, rowData);
		notifyObservers();
	}

	@Override
	public void removeRow(int row) {
		super.removeRow(row);
		notifyObservers();
	}

	@Override
	public void setRowCount(int rowCount) {
		super.setRowCount(rowCount);
		notifyObservers();
	}

	@Override
	public void register(iObserver o) {
		if(!observers.contains(o)) {
			observers.add(o);
		}
	}
	
	public void register(iObserver[] o) {
		for (iObserver observer : o) {
			register(observer);
		}
	}

	@Override
	public void unregister(iObserver o) {
		if(observers.contains(o)) {
			observers.remove(o);
		}
	}
	
	public void unregister(iObserver[] o) {
		for (iObserver observer : o) {
			unregister(observer);
		}
	}

	@Override
	public void notifyObservers() {
		if (!observers.isEmpty()) {
			observers.forEach(observer -> observer.update(this.getRowCount()));
		}
	}
}
