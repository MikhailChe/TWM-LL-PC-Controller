package starthere.widgets.log;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class GuiLogger {
	private static GuiLogger inst;

	public static GuiLogger log() {
		return inst();
	}

	public static GuiLogger inst() {
		if (inst == null) {
			synchronized (GuiLogger.class) {
				if (inst == null) {
					inst = new GuiLogger();
				}
			}
		}
		return inst;
	}

	List<GuiLoggerPanel> guiloggers = new ArrayList<>();

	public JPanel createLogger() {
		GuiLoggerPanel newPanel = new GuiLoggerPanel();
		guiloggers.add(newPanel);
		return newPanel;
	}

	public void println(String s) {
		print(String.format("%s%n", s));
	}

	public void print(final String s) {
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		final String out = "[" + time + "]  " + s;
		guiloggers.forEach(gl -> SwingUtilities.invokeLater(() -> gl.print(out)));
	}
}

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
