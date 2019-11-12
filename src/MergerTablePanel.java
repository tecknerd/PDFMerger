import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Objects;
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
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.PDFMergerUtility.DocumentMergeMode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;

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

	private PDFMergerUtility PDFmerger = new PDFMergerUtility();
	private FileNameExtensionFilter fileFilter = new FileNameExtensionFilter(".pdf", "pdf", ".PDF", "PDF");
	
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
			fc.addChoosableFileFilter(fileFilter);
			fc.setAcceptAllFileFilterUsed(false);
			fc.setMultiSelectionEnabled(true); 
			int fcResponse = fc.showOpenDialog(null);
			
			lastSelectedDirectory = fc.getSelectedFiles()[fc.getSelectedFiles().length - 1].getPath();
			
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
			JFileChooser fc = lastSelectedDirectory.equals(" ") ? new JFileChooser() : new JFileChooser(lastSelectedDirectory);
			fc.addChoosableFileFilter(fileFilter);
			fc.setSelectedFile(new File("merged.pdf"));

			int fcResponse = fc.showSaveDialog(null);
			
			lastSelectedDirectory = fc.getSelectedFile().getPath();
			
			if (fcResponse == JFileChooser.APPROVE_OPTION) {
				
				new Thread (new Runnable() {

					@Override
					public void run() {
						PDFProgressBar bar = new PDFProgressBar(false);
						bar.start();
						
						String destinationFilePath = fc.getSelectedFile().getAbsolutePath();
						String keyWords = " ";
						String subjects = " ";
						
						ObservableTableModel model = (ObservableTableModel) inputTable.getModel();

						if (!destinationFilePath.endsWith(".pdf")) {
							destinationFilePath += ".pdf";
						}

						File mergedFile = new File(destinationFilePath);

						PDFmerger.setDestinationFileName(destinationFilePath);

						for (int i = 0; i < model.getRowCount(); i++) {
							try {
								File sourceFile = (File) model.getValueAt(i, 0);
								bar.updateText("Adding " + sourceFile.getName() + " to merge queue");
								PDFmerger.addSource(sourceFile);
								
								PDDocument doc = PDDocument.load(sourceFile);
								if (Objects.nonNull(doc.getDocumentInformation().getKeywords())) {
									keyWords += doc.getDocumentInformation().getKeywords(); 
								}
								
								if (Objects.nonNull(doc.getDocumentInformation().getSubject())) {
									subjects += doc.getDocumentInformation().getSubject(); 
								}
								
								doc.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						try {
							bar.updateText("Merging " + model.getRowCount() + " files");			
							
							PDDocumentInformation docInfo = new PDDocumentInformation();
							docInfo.setTitle(PDFmerger.getDestinationFileName());
							docInfo.setAuthor(System.getProperty("user.name"));
							docInfo.setProducer("TeckNerd PDF Merger");
							docInfo.setSubject(subjects.trim());
							docInfo.setKeywords(keyWords.trim());
							docInfo.setCreationDate(Calendar.getInstance());
							
							PDFmerger.setDestinationDocumentInformation(docInfo);
							PDFmerger.mergeDocuments(MemoryUsageSetting.setupMixed(10000000));
							((ObservableTableModel) outputTable.getModel()).addRow(new File[] {mergedFile});
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						bar.stop();
						
						if (!EventQueue.isDispatchThread()) {
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									JOptionPane.showMessageDialog(null, "Documents merged");
								}
							});
						} else {
							JOptionPane.showMessageDialog(null, "Documents merged");
						}

					}
				}).start();
//				
//				(new SwingWorker<Object, Object>() {
//					PDFProgressBar bar = new PDFProgressBar();
//					@Override
//					protected Boolean doInBackground() throws Exception {
//						bar.start();
//
//						String filePath = fc.getSelectedFile().getAbsolutePath();
//
//						ObservableTableModel model = (ObservableTableModel) inputTable.getModel();
//
//						if (!filePath.endsWith(".pdf")) {
//							filePath += ".pdf";
//						}
//
//						File mergedFile = new File(filePath);
//
//						PDFmerger.setDestinationFileName(filePath);
//
//						for (int i = 0; i < model.getRowCount(); i++) {
//							PDFmerger.addSource((File) model.getValueAt(i, 0));
//						}
//
//						try {
//							PDFmerger.mergeDocuments(MemoryUsageSetting.setupMixed(10000000));
//							((ObservableTableModel) outputTable.getModel()).addRow(new File[] {mergedFile});
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//
//						//						pdfDocs.forEach(doc -> {
//						//							try {
//						//								doc.close();
//						//							} catch (IOException e) {
//						//								// TODO Auto-generated catch block
//						//								e.printStackTrace();
//						//							}
//						//						});
//
//						return null;
//					}
//
//					@Override
//					protected void done() {
//						super.done();
//						bar.stop();
//						if (!EventQueue.isDispatchThread()) {
//							SwingUtilities.invokeLater(new Runnable() {
//
//								@Override
//								public void run() {
//									// TODO Auto-generated method stub
//									JOptionPane.showMessageDialog(null, "Documents merged");
//								}
//							});
//						} else {
//							JOptionPane.showMessageDialog(null, "Documents merged");
//						}
//					}
//				}).run();
			}
		}
	};

	private ActionListener clearFilesAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {

			ObservableTableModel model = (ObservableTableModel) ((JTable) ((JButton)(evt.getSource()))
					.getClientProperty("table")).getModel();
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
		inputTable.addMouseListener(new RowMenu(inputTable));

		outputTable = makeTable();
		outputTable.setName("Output Table");
		outputTable.addMouseListener(new RowMenu(outputTable));

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