package pdfMerger;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import ObservableTools.ObservableTableModel;
import ObservableTools.TableRowObserverButton;

public class MergerTablePanel extends JPanel {

	private enum RowAction {
		toTop, oneUp, oneDown, toBottom;
	}

	private JPanel inputContainer;
	private JPanel outputContainer;

	private JSplitPane splitPane;
	private JScrollPane inputScrollPane;
	private JScrollPane outputScrollPane;

	private JTable inputTable;
	private JTable outputTable;

	private JButton importButton;
	private TableRowObserverButton mergeFilesButton;

	private String lastSelectedDirectory = " ";

	private ActionListener moveRowAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			RowAction action =  (RowAction) ((JButton)(evt.getSource())).getClientProperty("rowAction");
			JTable t = ((JTable) ((JButton)(evt.getSource())).getClientProperty("table"));
			ObservableTableModel model = (ObservableTableModel) t.getModel();

			int selectedRow = t.getSelectedRow();
			int newPosition = 0;
			int rowCount = model.getRowCount();

			if (rowCount > 0) {
				switch (action) {
				case oneUp:
					newPosition = selectedRow > 0 ? selectedRow - 1 : selectedRow;
					break;
				case oneDown:
					newPosition = selectedRow < rowCount - 1? selectedRow + 1 : selectedRow;
					break;
				case toBottom:
					newPosition = selectedRow < rowCount ? rowCount - 1 : selectedRow;
					break;
				case toTop:
				default:
					break;
				}

				File f = (File) model.getValueAt(selectedRow, 0);
				model.removeRow(selectedRow);
				model.insertRow(newPosition, new File[] {f});
				t.setRowSelectionInterval(newPosition, newPosition);
			}
		}
	};

	private ActionListener importAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JFileChooser fc = lastSelectedDirectory.equals(" ") ? new JFileChooser() : new JFileChooser(lastSelectedDirectory);
			fc.addChoosableFileFilter(PDFExtensionFilter.filter);
			fc.setAcceptAllFileFilterUsed(false);
			fc.setMultiSelectionEnabled(true); 

			int fcResponse = fc.showOpenDialog(null);

			lastSelectedDirectory = fc.getCurrentDirectory().getAbsolutePath();

			if (fcResponse == JFileChooser.APPROVE_OPTION) {
				ObservableTableModel model = (ObservableTableModel) inputTable.getModel();

				for (File file :fc.getSelectedFiles()) {
					model.addRow(new File[] {file});
				}

				model.fireTableDataChanged();

				revalidate();
				repaint();
			}
		}
	};

	private ActionListener mergeAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			File selectedFile = new File("merged.pdf");
			JFileChooser fc = lastSelectedDirectory.equals(" ") ? new JFileChooser() : new JFileChooser(lastSelectedDirectory);
			fc.addChoosableFileFilter(PDFExtensionFilter.filter);
			fc.setSelectedFile(selectedFile);

			while (true) {
				int fcResponse = fc.showSaveDialog(null);
				selectedFile = fc.getSelectedFile();
				lastSelectedDirectory = fc.getCurrentDirectory().getAbsolutePath();

				if (fcResponse == JFileChooser.APPROVE_OPTION) {	
					if (!selectedFile.getAbsolutePath().endsWith(".pdf")) {
						selectedFile = new File(selectedFile.getAbsolutePath() + ".pdf");
					}

					if (Files.exists(selectedFile.toPath())) {
						int fileExistsResponse = JOptionPane.showConfirmDialog(null, "A file named " + selectedFile.getName() + " already exists.\n"
								+ "Do you want to overwrite this file?", "WARNING!", JOptionPane.OK_CANCEL_OPTION);

						if(fileExistsResponse == JOptionPane.OK_OPTION) {
							merge(selectedFile);
							break;
						}
					} else {
						merge(selectedFile);
						break;
					}
				} else if (fcResponse == JFileChooser.CANCEL_OPTION) {
					break;
				}
			}
		}

		private void merge(File file) {
			new Thread (new Runnable() {

				@Override
				public void run() {
					File mergedFile = MergeUtility.merge(((ObservableTableModel) inputTable.getModel()).getDataVector(), file);
					((ObservableTableModel) outputTable.getModel()).addRow(new File[] {mergedFile});
				}
			}).start();
		}
	};

	private ActionListener clearFilesAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {

			ObservableTableModel model = (ObservableTableModel) ((JTable) ((JButton)(evt.getSource()))
					.getClientProperty("table")).getModel();
			
			model.getDataVector().clear();
			model.setRowCount(0);
			
			repaint();
			revalidate();
		}
	};

	public MergerTablePanel(){
		super(new BorderLayout());
		init();
	}

	private void init() {
		inputTable = makeTable();
		inputTable.setName("Input Table");
		inputTable.addMouseListener(new RightClickMenu(inputTable));

		outputTable = makeTable();
		outputTable.setName("Output Table");
		outputTable.addMouseListener(new RightClickMenu(outputTable));

		inputScrollPane = makeScrollPane("Input Files", inputTable);
		outputScrollPane = makeScrollPane("Output Files", outputTable);

		importButton = new JButton("Import File");
		importButton.addActionListener(importAction);

		mergeFilesButton = new TableRowObserverButton("Merge Files", i -> i >= 2);
		mergeFilesButton.addActionListener(mergeAction);
		mergeFilesButton.setEnabled(false);

		((ObservableTableModel) inputTable.getModel()).register(mergeFilesButton);

		JPanel mergeControls = new JPanel();
		mergeControls.add(importButton);
		mergeControls.add(mergeFilesButton);

		inputContainer = new JPanel(new BorderLayout());
		inputContainer.add(inputScrollPane, BorderLayout.CENTER);
		inputContainer.add(createButtonPanel(inputTable), BorderLayout.EAST);
		inputContainer.add(mergeControls, BorderLayout.SOUTH);

		outputContainer = new JPanel(new BorderLayout());
		outputContainer.add(outputScrollPane, BorderLayout.CENTER);
		outputContainer.add(createButtonPanel(outputTable), BorderLayout.EAST);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputContainer, outputContainer);
		splitPane.setResizeWeight(0.5);

		this.add(splitPane, BorderLayout.CENTER);		
	}

	private JTable makeTable() {
		JTable t = new JTable(new ObservableTableModel(0, 1)) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		t.setTableHeader(null);
		t.setFillsViewportHeight(true);
		t.setDragEnabled(true);
		t.setDropMode(DropMode.INSERT_ROWS);

		return t;
	}

	private JScrollPane makeScrollPane(String title, JTable t) {
		JScrollPane sp = new JScrollPane(t);
		sp.setBorder (BorderFactory.createTitledBorder 
				(BorderFactory.createEtchedBorder (), title, TitledBorder.CENTER, TitledBorder.TOP));

		return sp;
	}

	private JPanel createButtonPanel(JTable t) {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder 
				(BorderFactory.createEtchedBorder (), t.getName(), TitledBorder.CENTER, TitledBorder.TOP));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

		TableRowObserverButton moveToTop = new TableRowObserverButton("Top", i -> i >= 3);
		moveToTop.putClientProperty("table", t);
		moveToTop.putClientProperty("rowAction", RowAction.toTop);
		moveToTop.addActionListener(moveRowAction);
		moveToTop.setEnabled(false);
		p.add(moveToTop);

		TableRowObserverButton moveUp = new TableRowObserverButton("Up", i -> i >= 2);
		moveUp.putClientProperty("table", t);
		moveUp.putClientProperty("rowAction", RowAction.oneUp);
		moveUp.addActionListener(moveRowAction);
		moveUp.setEnabled(false);
		p.add(moveUp);

		TableRowObserverButton moveDown = new TableRowObserverButton("Down", i -> i >= 2);
		moveDown.putClientProperty("table", t);
		moveDown.putClientProperty("rowAction", RowAction.oneDown);
		moveDown.addActionListener(moveRowAction);
		moveDown.setEnabled(false);
		p.add(moveDown);

		TableRowObserverButton moveToBottom = new TableRowObserverButton("Bottom", i -> i >= 3);
		moveToBottom.putClientProperty("table", t);
		moveToBottom.putClientProperty("rowAction", RowAction.toBottom);
		moveToBottom.addActionListener(moveRowAction);
		moveToBottom.setEnabled(false);
		p.add(moveToBottom);

		p.add(Box.createGlue());

		TableRowObserverButton clearFiles = new TableRowObserverButton("Clear Files", i -> i >= 1);
		clearFiles.putClientProperty("table", t);
		clearFiles.addActionListener(clearFilesAction);
		clearFiles.setEnabled(false);
		p.add(clearFiles);

		((ObservableTableModel) t.getModel()).register(moveToTop);
		((ObservableTableModel) t.getModel()).register(moveUp);
		((ObservableTableModel) t.getModel()).register(moveDown);
		((ObservableTableModel) t.getModel()).register(moveToBottom);
		((ObservableTableModel) t.getModel()).register(clearFiles);

		return p;
	}
}
//and then taDa!!! magic it works because i love you
