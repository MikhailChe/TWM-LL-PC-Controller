package log;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

class GuiLoggerPanel extends JPanel {
	JTextPane logTextPane;
	JScrollPane logScrollPane;

	public GuiLoggerPanel() {
		setBorder(new TitledBorder(null, "Log", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(new BorderLayout(0, 0));

		logScrollPane = new JScrollPane();
		add(logScrollPane, BorderLayout.CENTER);

		logTextPane = new JTextPane();
		logScrollPane.setViewportView(logTextPane);
		Dimension size = new Dimension(256, 256);
		logTextPane.setMinimumSize(size);
		logTextPane.setPreferredSize(size);
		logTextPane.setEditable(false);
	}

	void print(String s) {
		Document doc = logTextPane.getDocument();
		try {
			doc.insertString(doc.getLength(), s, null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		logTextPane.setCaretPosition(doc.getLength());
		logScrollPane.getVerticalScrollBar().setValue(logScrollPane.getVerticalScrollBar().getMaximum());
	}

}
