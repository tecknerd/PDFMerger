package pdfMerger;
import java.awt.Dimension;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class PDFMerger {

	public static void main(String[] args) throws IOException {
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}

		JFrame frame = new JFrame("TeckNerd PDF Merger - BETA");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(800, 600));
		frame.setLocationRelativeTo(null);
		frame.add(new MergerTablePanel());
		frame.setVisible(true);

		int opt = JOptionPane.showConfirmDialog(frame, "You are about to use a BETA version of PDF Merger.\n" +
				"This product is still under production and is missing features.\n" +
				"Select OK to continue or CANCEL to exit this program", "ALERT!! BETA VERSION", 
				JOptionPane.OK_CANCEL_OPTION);

		if(opt == JOptionPane.CANCEL_OPTION) {
			frame.setVisible(false);
			frame.dispose();
		}
	}
}
