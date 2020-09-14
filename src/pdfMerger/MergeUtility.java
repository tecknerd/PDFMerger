import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Objects;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import ObservableTools.ObservableTableModel;

public class Merger implements ActionListener {
	private PDFMergerUtility PDFmerger = new PDFMergerUtility();
	private FileNameExtensionFilter fileFilter = new FileNameExtensionFilter(".pdf", "pdf", ".PDF", "PDF");
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		File selectedFile = new File("merged.pdf");
		JFileChooser fc = lastSelectedDirectory.equals(" ") ? new JFileChooser() : new JFileChooser(lastSelectedDirectory);
		fc.addChoosableFileFilter(fileFilter);
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
				PDFProgressBar bar = new PDFProgressBar(false);
				bar.start();

				File destinationFile = file;
				ObservableTableModel model = (ObservableTableModel) inputTable.getModel();
				PDFmerger.setDestinationFileName(destinationFile.getAbsolutePath());
				String keyWords = " ";
				String subjects = " ";

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
					((ObservableTableModel) outputTable.getModel()).addRow(new File[] {destinationFile});
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
	}
}
