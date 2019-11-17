import java.awt.Dialog.ModalityType;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class PDFProgressBar extends JProgressBar {
	private JDialog container;
	private JPanel panel;
	private JPanel buttonPanel;
	private JLabel textLabel;
	private JButton cancelButton;
	
	private boolean cancel;
	
	private ActionListener cancelAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			cancel = true;
		}
	};
	
	public PDFProgressBar() {
		this(true);
	}

	public PDFProgressBar(boolean cancellable) {
		super();
		this.setIndeterminate(true);

		textLabel = new JLabel(" ");

		panel = new JPanel(new GridLayout(0, 1));
		panel.add(textLabel);
		panel.add(this);
		
		buttonPanel = new JPanel();
		if (cancellable) {
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(cancelAction);
			buttonPanel.add(cancelButton);
		}
		panel.add(buttonPanel);

		container = new JDialog();
		container.add(panel);
		container.setSize(300, 150);
		container.setLocationRelativeTo(null);
		container.setTitle("This may take several minutes");
		container.setModalityType(ModalityType.APPLICATION_MODAL);
		container.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		cancel = false;
	}

	public void start() {
		onEDT(new Runnable() {

			@Override
			public void run() {
				container.setVisible(true);
			}
		});
	}

	public void stop() {
		onEDT(new Runnable() {

			@Override
			public void run() {
				container.setVisible(false);
				container.dispose();
			}
		});	
	}

	public void updateText(String text) {
		onEDT(new Runnable() {

			@Override
			public void run() {
				textLabel.setText(text);
				container.revalidate();	
			}
		});	
	}

	private void onEDT(Runnable r) {
		if(SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}
	
	public boolean isCancelled() {
		return cancel;
	}
}
