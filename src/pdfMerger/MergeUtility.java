package pdfMerger;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

public class MergeUtility {

	public static File merge(Vector<Vector> files, File destinationFile) {
		PDFProgressBar bar = new PDFProgressBar(false);
		bar.start();
		
		PDFMergerUtility mergeUtility = new PDFMergerUtility();

		mergeUtility.setDestinationFileName(destinationFile.getAbsolutePath());
		String keyWords = " ";
		String subjects = " ";

		for (int i = 0; i < files.size(); i++) {
			try {
				File sourceFile = (File) files.elementAt(i).elementAt(0);
				bar.updateText("Adding " + sourceFile.getName() + " to merge queue");
				mergeUtility.addSource(sourceFile);

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
			bar.updateText("Merging " + files.size() + " files");			

			PDDocumentInformation docInfo = new PDDocumentInformation();
			docInfo.setTitle(destinationFile.getName());
			docInfo.setAuthor(System.getProperty("user.name"));
			docInfo.setProducer("TeckNerd PDF Merger");
			docInfo.setSubject(subjects.trim());
			docInfo.setKeywords(keyWords.trim());
			docInfo.setCreationDate(Calendar.getInstance());

			mergeUtility.setDestinationDocumentInformation(docInfo);
			mergeUtility.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
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

		return destinationFile;
	}
}
