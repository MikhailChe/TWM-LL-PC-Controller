package log;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
		guiloggers.forEach(gl -> SwingUtilities.invokeLater(()->gl.print(out)));
	}
}
