package me.lpk.gui;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

/**
 * TODO: Determine if this is a good idea:<br>
 * Have a dialog box for each Skidfuscator process (obfuscation job)
 */
public class ProcessDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private final JLabel lbl = new JLabel();
	private final JProgressBar bar = new JProgressBar(0, 100);

	public void setLbl(String txt) {
		lbl.setText(txt);
	}

	public void finish() {
		lbl.setText("Done!");
		bar.setValue(100);
	}

	public void setBar(double percent) {
		int i = (int) (percent * 100);
		bar.setValue(i);
	}
}
