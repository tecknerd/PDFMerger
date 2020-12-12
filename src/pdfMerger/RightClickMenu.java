package pdfMerger;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

public class RightClickMenu implements MouseListener {

	private JPopupMenu rightClickMenu;
	private JMenuItem deleteItem;
	private JMenuItem viewDocButton;
	private JMenuItem metaDataButton;

	private JTable table;

	private ActionListener viewDocAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			File file = (File) table.getValueAt(table.getSelectedRow(), 0);
			if (Objects.nonNull(file)) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						PDDocument pdfDoc;
						try {
							pdfDoc = PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly());
							PDFViewDialog view = new PDFViewDialog(pdfDoc);
							view.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							view.setSize(800, 950);
							view.setPreferredSize(new Dimension(800, 950));
							view.renderPDF(true);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			} else {
				System.err.println("Error rendering image");
				JOptionPane.showMessageDialog(table, "Error rendering image", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	};

	private ActionListener removeAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {			
			int selectedRow = table.getSelectedRow();
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			model.removeRow(selectedRow);
		}
	};

	private ActionListener metaDataAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					PDFProgressBar bar = new PDFProgressBar();
					bar.updateText("Fetching metadata");
					bar.start();

					File file = (File) table.getValueAt(table.getSelectedRow(), 0);
					try {
						PDDocument doc = PDDocument.load(file);
						PDDocumentInformation docInfo = doc.getDocumentInformation();

						JTextArea textDisplay = new JTextArea();
						textDisplay.setEditable(false);
						textDisplay.append("File: " + file.getName() + "\n");
						textDisplay.append("Title: " + nullCheck(docInfo.getTitle()) + "\n");
						textDisplay.append("Author: " + nullCheck(docInfo.getAuthor()) + "\n");
						textDisplay.append("Producer: " + nullCheck(docInfo.getProducer() + "\n"));
						textDisplay.append("Subject: " + nullCheck(docInfo.getSubject()) + "\n");
						textDisplay.append("Keywords: " + nullCheck(docInfo.getKeywords()) + "\n");
						textDisplay.append("Number of pages: " + doc.getNumberOfPages() + "\n");
						textDisplay.append("Creation date: " + formatDate(docInfo.getCreationDate()) + "\n");
						textDisplay.append("Modification date: " + formatDate(docInfo.getModificationDate()) + "\n");
						textDisplay.append("Trapped: " + nullCheck(docInfo.getTrapped()) + "\n");

						JScrollPane metaDataScroll = new JScrollPane(textDisplay);

						JDialog metaDataDialog = new JDialog();
						metaDataDialog.setTitle("MetaData");
						metaDataDialog.setModalityType(ModalityType.APPLICATION_MODAL);
						metaDataDialog.add(metaDataScroll);
						metaDataDialog.setSize(400, 400);
						metaDataDialog.setResizable(true);
						metaDataDialog.setLocationRelativeTo(null);

						bar.stop();
						metaDataDialog.setVisible(true);

						doc.close();

					} catch (IOException e1) {
						e1.printStackTrace();
					}					
				}

				private String nullCheck(String s) {
					return s == null ? " " : s;
				}

				private String formatDate(Calendar cal) {
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					return Objects.isNull(cal) ? " " : dateFormat.format(cal.getTime());
				}
			}).start();
		}
	};

	public RightClickMenu(JTable t) {
		table = t;

		deleteItem = new JMenuItem("Remove");
		deleteItem.addActionListener(removeAction);

		viewDocButton = new JMenuItem("View Document");	
		viewDocButton.addActionListener(viewDocAction);

		metaDataButton = new JMenuItem("Metadata");
		metaDataButton.addActionListener(metaDataAction);

		rightClickMenu = new JPopupMenu();
		rightClickMenu.add(viewDocButton);
		rightClickMenu.add(new JSeparator());
		rightClickMenu.add(deleteItem);
		rightClickMenu.add(new JSeparator());
		rightClickMenu.add(metaDataButton);
		rightClickMenu.pack();

		table.add(rightClickMenu);
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		int row = table.rowAtPoint(e.getPoint());

		if (SwingUtilities.isRightMouseButton(e) && row > -1) {
			table.setRowSelectionInterval(row, row);
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());;
		}
	}
}
