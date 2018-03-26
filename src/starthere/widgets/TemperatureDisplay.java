package starthere.widgets;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class TemperatureDisplay {
	private static TemperatureDisplay instance;

	public static TemperatureDisplay instance() {
		if (instance == null) {
			synchronized (TemperatureDisplay.class) {
				if (instance == null) {
					instance = new TemperatureDisplay();
				}
			}
		}
		return instance;
	}

	List<TemperatureDisplayPanel> guidisplays = new ArrayList<>();

	public JPanel createDisplay() {
		TemperatureDisplayPanel newPanel = new TemperatureDisplayPanel();
		guidisplays.add(newPanel);
		return newPanel;
	}

	public void updateTempearture(final double val) {
		guidisplays.forEach(gd -> gd.updateTemperature(val));
	}
}

class TemperatureDisplayPanel extends JPanel {
	JLabel temperatureLabel;

	public TemperatureDisplayPanel() {
		this.setLayout(new BorderLayout(8, 8));

		temperatureLabel = new JLabel("NAN");
		this.add(temperatureLabel);
	}

	public void updateTemperature(double value) {
		this.temperatureLabel.setText(String.format("%.1fK", value));
	}

}
