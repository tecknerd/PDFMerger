import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDFViewDialog extends JDialog{

	private JScrollPane imagePane;

	private PDDocument document;
	private PDPageContentStream pageStream;
	private JPanel panel;

	//	private JLabel imageLabel;

	public PDFViewDialog (PDDocument doc) {
		this.document = doc;
		this.setTitle(document.getDocumentInformation().getTitle());
		
		panel = new JPanel();
		imagePane = new JScrollPane(panel);
		
		this.add(imagePane);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.pack();
	}

	public void renderPDF(boolean visibleOnComplete) {
		//		PDDocument document = PDDocument.load(new File(pdfFilename));

		new Thread(new Runnable() {

			@Override
			public void run() {
				PDFProgressBar bar = new PDFProgressBar();
				bar.start();

				PDFRenderer pdfRenderer = new PDFRenderer(document);				

				long startTime = System.currentTimeMillis();

				int totalPages = document.getNumberOfPages();
				for (int pageNum = 0; pageNum < totalPages; pageNum++) {
					if(!bar.isCancelled()) {
						bar.updateText("Rendering page " + (pageNum + 1) + " out of " + totalPages);

						//					pageStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
						//					BufferedImage bim = pdfRenderer.renderImageWithDPI(pageNum, 80, ImageType.RGB);

						try {
							panel.add(new JLabel(new ImageIcon(pdfRenderer.renderImage(pageNum))));
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						break;
					}
				}

				try {
					document.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				long endTime = System.currentTimeMillis();
				System.out.println("Rendering took: " + (endTime - startTime) + " ms");

				bar.stop();

				if (bar.isCancelled()) {
					dispose();
				} else if (visibleOnComplete) {
					setVisible(true);
				}
			}

		}).start();

		//		(new SwingWorker<Void, Void> () {
		//
		//			@Override
		//			protected Void doInBackground() throws Exception {
		//				// TODO Auto-generated method stub
		//				PDFRenderer pdfRenderer = new PDFRenderer(document);				
		//				System.out.println(SwingUtilities.isEventDispatchThread());
		//				long startTime = System.currentTimeMillis();
		//
		//				for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
		////					pageStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
		////					BufferedImage bim = pdfRenderer.renderImageWithDPI(pageNum, 80, ImageType.RGB);
		//					BufferedImage bim = pdfRenderer.renderImage(pageNum);
		//					panel.add(new JLabel(new ImageIcon(bim)));
		//				}
		//				
		//				try {
		//					document.close();
		//				} catch (IOException e) {
		//					// TODO Auto-generated catch block
		//					e.printStackTrace();
		//				}
		//				
		//				long endTime = System.currentTimeMillis();
		//
		//				System.out.println("Rendering took: " + (endTime - startTime) + " ms");
		//				
		//				return null;
		//			}
		//		}).run();
	}
}
