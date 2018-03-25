package starthere;

import static starthere.PropertiesNames.ABSOLUTE_MAXIMUM_TEMPERATURE;
import static starthere.PropertiesNames.ABSOLUTE_MINIMUM_TEMPERATURE;
import static starthere.PropertiesNames.CURRENT_MAXIMUM_TEMPERATURE;
import static starthere.PropertiesNames.CURRENT_MINIMUM_TEMPERATURE;
import static starthere.PropertiesNames.INITIALLY_UP;
import static starthere.PropertiesNames.INITIAL_TEMPERATURE;
import static starthere.PropertiesNames.TEMPERATURE_STEP;
import static starthere.PropertiesNames.fillDefaults;
import static starthere.PropertiesNames.saveProperties;
import static starthere.StartHere.Acquisitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import asdaservo.ServoController;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import unidaq.UniDaqException;

public class MainWindow extends JFrame implements SettingsHolder {
	private JSpinner servoFrequencyHzSpinner;
	private JSpinner initTemperatureSpinner;
	private JSpinner minTemperatureSpinner;
	private JSpinner maxTemperatureSpinner;
	private JSpinner temperatureStepSpinner;
	private JCheckBox initiallyUpCheckbox;
	private JComboBox<String> servoDriveComPortCombobox;

	public MainWindow() {

		Properties properties = new Properties();
		try (FileInputStream propFile = new FileInputStream("MainWindow.properties")) {
			properties.load(propFile);
		} catch (Exception e) {
			System.err.println("Properties file was not found. Loading defaults");
		}
		fillDefaults(properties);
		saveProperties(properties);

		final int initialTemperature = INITIAL_TEMPERATURE.getIntegerProperty(properties);
		final int absoluteMinimumTemperature = ABSOLUTE_MINIMUM_TEMPERATURE.getIntegerProperty(properties);
		final int absoluteMaximumTemperature = ABSOLUTE_MAXIMUM_TEMPERATURE.getIntegerProperty(properties);
		final int currentMinimumTemperature = CURRENT_MINIMUM_TEMPERATURE.getIntegerProperty(properties);
		final int currentMaximumTemperature = CURRENT_MAXIMUM_TEMPERATURE.getIntegerProperty(properties);
		final int temperatureStep = TEMPERATURE_STEP.getIntegerProperty(properties);
		final boolean isInitiallyUp = INITIALLY_UP.getBooleanProperty(properties);

		JPanel leftPanel = new JPanel();
		leftPanel.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)),
				"Experiment control", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		getContentPane().add(leftPanel, BorderLayout.WEST);
		GridBagLayout gbl_leftPanel = new GridBagLayout();
		gbl_leftPanel.columnWidths = new int[] { 0, 0 };
		gbl_leftPanel.rowHeights = new int[] { 144, 0, 0, 0 };
		gbl_leftPanel.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_leftPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		leftPanel.setLayout(gbl_leftPanel);

		JPanel temperatureRegulationPanel = new JPanel();
		GridBagConstraints gbc_temperatureRegulationPanel = new GridBagConstraints();
		gbc_temperatureRegulationPanel.fill = GridBagConstraints.BOTH;
		gbc_temperatureRegulationPanel.insets = new Insets(0, 0, 5, 0);
		gbc_temperatureRegulationPanel.gridx = 0;
		gbc_temperatureRegulationPanel.gridy = 0;
		leftPanel.add(temperatureRegulationPanel, gbc_temperatureRegulationPanel);
		temperatureRegulationPanel.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)),
				"Temperature control", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagLayout gbl_temperatureRegulationPanel = new GridBagLayout();
		gbl_temperatureRegulationPanel.columnWidths = new int[] { 0, 64 };
		gbl_temperatureRegulationPanel.columnWeights = new double[] { 0.0, 1.0 };
		gbl_temperatureRegulationPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
		temperatureRegulationPanel.setLayout(gbl_temperatureRegulationPanel);

		JLabel initialTemperatureLabel = new JLabel("Initial temperature");

		GridBagConstraints gbc_initialTemperatureLabel = new GridBagConstraints();
		gbc_initialTemperatureLabel.anchor = GridBagConstraints.EAST;
		gbc_initialTemperatureLabel.insets = new Insets(0, 0, 5, 5);
		gbc_initialTemperatureLabel.gridx = 0;
		gbc_initialTemperatureLabel.gridy = 0;
		temperatureRegulationPanel.add(initialTemperatureLabel, gbc_initialTemperatureLabel);

		initTemperatureSpinner = new JSpinner();
		initTemperatureSpinner.setModel(
				new SpinnerNumberModel(initialTemperature, absoluteMinimumTemperature, absoluteMaximumTemperature, 1));
		GridBagConstraints gbc_initTemperatureSpinner = new GridBagConstraints();
		gbc_initTemperatureSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_initTemperatureSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_initTemperatureSpinner.gridx = 1;
		gbc_initTemperatureSpinner.gridy = 0;
		temperatureRegulationPanel.add(initTemperatureSpinner, gbc_initTemperatureSpinner);

		initTemperatureSpinner.addChangeListener((e) -> {
			int newInitialTemperature = (int) initTemperatureSpinner.getValue();
			INITIAL_TEMPERATURE.putProperty(properties, newInitialTemperature);
			saveProperties(properties);
		});

		JLabel minTemperatureLabel = new JLabel("Minimal temperature");
		GridBagConstraints gbc_minTemperatureLabel = new GridBagConstraints();
		gbc_minTemperatureLabel.anchor = GridBagConstraints.EAST;
		gbc_minTemperatureLabel.insets = new Insets(0, 0, 5, 5);
		gbc_minTemperatureLabel.gridx = 0;
		gbc_minTemperatureLabel.gridy = 1;

		temperatureRegulationPanel.add(minTemperatureLabel, gbc_minTemperatureLabel);
		minTemperatureSpinner = new JSpinner();
		minTemperatureSpinner.setModel(new SpinnerNumberModel(currentMinimumTemperature, absoluteMinimumTemperature,
				currentMaximumTemperature, 1));
		GridBagConstraints gbc_minTemperatureSpinner = new GridBagConstraints();
		gbc_minTemperatureSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_minTemperatureSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_minTemperatureSpinner.gridx = 1;
		gbc_minTemperatureSpinner.gridy = 1;
		temperatureRegulationPanel.add(minTemperatureSpinner, gbc_minTemperatureSpinner);

		JLabel maxTemperatureLabel = new JLabel("Maximum tempearture");
		GridBagConstraints gbc_maxTemperatureLabel = new GridBagConstraints();
		gbc_maxTemperatureLabel.anchor = GridBagConstraints.EAST;
		gbc_maxTemperatureLabel.insets = new Insets(0, 0, 5, 5);
		gbc_maxTemperatureLabel.gridx = 0;
		gbc_maxTemperatureLabel.gridy = 2;
		temperatureRegulationPanel.add(maxTemperatureLabel, gbc_maxTemperatureLabel);

		maxTemperatureSpinner = new JSpinner();
		maxTemperatureSpinner.setModel(new SpinnerNumberModel(currentMaximumTemperature, currentMinimumTemperature,
				absoluteMaximumTemperature, 1));
		GridBagConstraints gbc_maxTemperatureSpinner = new GridBagConstraints();
		gbc_maxTemperatureSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_maxTemperatureSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_maxTemperatureSpinner.gridx = 1;
		gbc_maxTemperatureSpinner.gridy = 2;
		temperatureRegulationPanel.add(maxTemperatureSpinner, gbc_maxTemperatureSpinner);

		// Redefine minimum and maximum values of minTemperatureSpinner and
		// maxTemperatureSpinner based on it's values

		minTemperatureSpinner.addChangeListener((e) -> {
			SpinnerNumberModel model = (SpinnerNumberModel) maxTemperatureSpinner.getModel();
			int value = (int) minTemperatureSpinner.getValue();
			CURRENT_MINIMUM_TEMPERATURE.putProperty(properties, value);
			saveProperties(properties);
			model.setMinimum(value);
		});

		maxTemperatureSpinner.addChangeListener((e) -> {
			SpinnerNumberModel model = (SpinnerNumberModel) minTemperatureSpinner.getModel();
			int value = (int) maxTemperatureSpinner.getValue();
			CURRENT_MAXIMUM_TEMPERATURE.putProperty(properties, value);
			saveProperties(properties);
			model.setMaximum(value);
		});

		JLabel temperatureStepLabel = new JLabel("Temperature step");
		GridBagConstraints gbc_temperatureStep = new GridBagConstraints();
		gbc_temperatureStep.anchor = GridBagConstraints.EAST;
		gbc_temperatureStep.insets = new Insets(0, 0, 5, 5);
		gbc_temperatureStep.gridx = 0;
		gbc_temperatureStep.gridy = 3;
		temperatureRegulationPanel.add(temperatureStepLabel, gbc_temperatureStep);

		temperatureStepSpinner = new JSpinner();
		temperatureStepSpinner.setModel(new SpinnerNumberModel(temperatureStep, 1, 100, 1));
		GridBagConstraints gbc_temperatureStepSpinner = new GridBagConstraints();
		gbc_temperatureStepSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_temperatureStepSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_temperatureStepSpinner.gridx = 1;
		gbc_temperatureStepSpinner.gridy = 3;
		temperatureRegulationPanel.add(temperatureStepSpinner, gbc_temperatureStepSpinner);

		temperatureStepSpinner.addChangeListener((e) -> {
			int newTemperatureStep = (int) temperatureStepSpinner.getValue();
			TEMPERATURE_STEP.putProperty(properties, newTemperatureStep);
			saveProperties(properties);
		});

		JLabel initiallyUpLabel = new JLabel("Initially up?");
		GridBagConstraints gbc_initiallyUpLabel = new GridBagConstraints();
		gbc_initiallyUpLabel.anchor = GridBagConstraints.EAST;
		gbc_initiallyUpLabel.insets = new Insets(0, 0, 0, 5);
		gbc_initiallyUpLabel.gridx = 0;
		gbc_initiallyUpLabel.gridy = 4;
		temperatureRegulationPanel.add(initiallyUpLabel, gbc_initiallyUpLabel);

		initiallyUpCheckbox = new JCheckBox();
		initiallyUpCheckbox.setSelected(isInitiallyUp);
		GridBagConstraints gbc_initiallyUpCheckbox = new GridBagConstraints();
		gbc_initiallyUpCheckbox.fill = GridBagConstraints.HORIZONTAL;
		gbc_initiallyUpCheckbox.gridx = 1;
		gbc_initiallyUpCheckbox.gridy = 4;
		temperatureRegulationPanel.add(initiallyUpCheckbox, gbc_initiallyUpCheckbox);

		initiallyUpCheckbox.addChangeListener((e) -> {
			boolean newFlag = isInitiallyUp();
			INITIALLY_UP.putProperty(properties, newFlag);
			saveProperties(properties);
		});

		JPanel temperatureStabilizationPanel = new JPanel();
		temperatureStabilizationPanel.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)),
				"Temperature stabilization", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_temperatureStabilizationPanel = new GridBagConstraints();
		gbc_temperatureStabilizationPanel.insets = new Insets(0, 0, 5, 0);
		gbc_temperatureStabilizationPanel.fill = GridBagConstraints.BOTH;
		gbc_temperatureStabilizationPanel.gridx = 0;
		gbc_temperatureStabilizationPanel.gridy = 1;
		leftPanel.add(temperatureStabilizationPanel, gbc_temperatureStabilizationPanel);
		GridBagLayout gbl_temperatureStabilizationPanel = new GridBagLayout();
		gbl_temperatureStabilizationPanel.columnWidths = new int[] { 0, 64 };
		gbl_temperatureStabilizationPanel.columnWeights = new double[] { 0.0, 1.0 };
		gbl_temperatureStabilizationPanel.rowWeights = new double[] { 0.0, 0.0, 0.0 };
		temperatureStabilizationPanel.setLayout(gbl_temperatureStabilizationPanel);

		JLabel stabilizationDegreesLabel = new JLabel("Stabilization degrees");
		GridBagConstraints gbc_stabilizationDegreesLabel = new GridBagConstraints();
		gbc_stabilizationDegreesLabel.insets = new Insets(0, 0, 5, 5);
		gbc_stabilizationDegreesLabel.anchor = GridBagConstraints.EAST;
		gbc_stabilizationDegreesLabel.gridx = 0;
		gbc_stabilizationDegreesLabel.gridy = 0;
		temperatureStabilizationPanel.add(stabilizationDegreesLabel, gbc_stabilizationDegreesLabel);

		JSpinner stabilizationDegreesSpinner = new JSpinner();
		stabilizationDegreesSpinner.setModel(new SpinnerNumberModel(.5, .5, 10, .1));
		GridBagConstraints gbc_stabilizationDegreesSpinner = new GridBagConstraints();
		gbc_stabilizationDegreesSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_stabilizationDegreesSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_stabilizationDegreesSpinner.gridx = 1;
		gbc_stabilizationDegreesSpinner.gridy = 0;
		temperatureStabilizationPanel.add(stabilizationDegreesSpinner, gbc_stabilizationDegreesSpinner);

		JLabel stabilizationTimeLabel = new JLabel("Stabilization time");
		GridBagConstraints gbc_stabilizationTimeLabel = new GridBagConstraints();
		gbc_stabilizationTimeLabel.anchor = GridBagConstraints.EAST;
		gbc_stabilizationTimeLabel.insets = new Insets(0, 0, 5, 5);
		gbc_stabilizationTimeLabel.gridx = 0;
		gbc_stabilizationTimeLabel.gridy = 1;
		temperatureStabilizationPanel.add(stabilizationTimeLabel, gbc_stabilizationTimeLabel);

		JSpinner stabilizationTimeSpinner = new JSpinner();
		stabilizationTimeSpinner.setModel(new SpinnerNumberModel(1, 1, 60, 1));
		GridBagConstraints gbc_stabilizationTimeSpinner = new GridBagConstraints();
		gbc_stabilizationTimeSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_stabilizationTimeSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_stabilizationTimeSpinner.gridx = 1;
		gbc_stabilizationTimeSpinner.gridy = 1;
		temperatureStabilizationPanel.add(stabilizationTimeSpinner, gbc_stabilizationTimeSpinner);

		JLabel stabilizationTimeUnitLabel = new JLabel("Stabilization time unit");
		GridBagConstraints gbc_stabilizationTimeUnitLabel = new GridBagConstraints();
		gbc_stabilizationTimeUnitLabel.anchor = GridBagConstraints.EAST;
		gbc_stabilizationTimeUnitLabel.insets = new Insets(0, 0, 0, 5);
		gbc_stabilizationTimeUnitLabel.gridx = 0;
		gbc_stabilizationTimeUnitLabel.gridy = 2;
		temperatureStabilizationPanel.add(stabilizationTimeUnitLabel, gbc_stabilizationTimeUnitLabel);

		JComboBox<TimeUnit> stabilizationTimeUnitCombobox = new JComboBox<>();
		stabilizationTimeUnitCombobox.setModel(new DefaultComboBoxModel<>(TimeUnit.values()));
		stabilizationTimeUnitCombobox.setSelectedIndex(3);
		GridBagConstraints gbc_stabilizationTimeUnitCombobox = new GridBagConstraints();
		gbc_stabilizationTimeUnitCombobox.fill = GridBagConstraints.HORIZONTAL;
		gbc_stabilizationTimeUnitCombobox.gridx = 1;
		gbc_stabilizationTimeUnitCombobox.gridy = 2;
		temperatureStabilizationPanel.add(stabilizationTimeUnitCombobox, gbc_stabilizationTimeUnitCombobox);

		JPanel experimentStartStopPanel = new JPanel();
		GridBagConstraints gbc_experimentStartStopPanel = new GridBagConstraints();
		gbc_experimentStartStopPanel.fill = GridBagConstraints.BOTH;
		gbc_experimentStartStopPanel.gridx = 0;
		gbc_experimentStartStopPanel.gridy = 2;
		leftPanel.add(experimentStartStopPanel, gbc_experimentStartStopPanel);
		experimentStartStopPanel.setLayout(new GridLayout(1, 0, 0, 0));

		JButton btnStartExperiment = new JButton("E-Start");
		btnStartExperiment.setActionCommand("start");
		btnStartExperiment.addActionListener((e) -> {
			final JButton source = (JButton) e.getSource();
			source.setEnabled(false);
			if (e.getActionCommand().equals("start")) {
				source.setActionCommand("stop");
				source.setText("E-Stop");
				SwingUtilities.invokeLater(() -> {
					Measurement.measurementControl(MainWindow.this, true);
					source.setEnabled(true);
				});
			} else if (e.getActionCommand().equals("stop")) {
				source.setActionCommand("start");
				source.setText("E-Start");
				SwingUtilities.invokeLater(() -> {
					Measurement.measurementControl(MainWindow.this, false);
					source.setEnabled(true);
				});
			}
		});

		experimentStartStopPanel.add(btnStartExperiment);

		JButton btnManualMeasure = new JButton("Manual measure");
		btnManualMeasure.addActionListener((e) -> {
			AbstractButton source = (AbstractButton) e.getSource();
			source.setEnabled(false);

			final Runnable setupPrint = () -> {
				try {
					Acquisitor.setupReadAndPrintExperiment(new short[] { 0, 2, 4, 6 }, this.getExperimentFrequency(),
							64);
				} catch (FileNotFoundException | InterruptedException | NoSuchPortException | PortInUseException
						| UniDaqException e1) {
					e1.printStackTrace();
				}
			};
			new Thread(() -> {
				setupPrint.run();
				SwingUtilities.invokeLater(() -> source.setEnabled(true));
			}).start();
		});

		experimentStartStopPanel.add(btnManualMeasure);

		JPanel rightPanel = new JPanel();
		rightPanel.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)),
				"Hardware configuration", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		getContentPane().add(rightPanel, BorderLayout.EAST);
		GridBagLayout gbl_rightPanel = new GridBagLayout();
		gbl_rightPanel.columnWidths = new int[] { 139 };
		gbl_rightPanel.rowHeights = new int[] { 0, 0, 0 };
		gbl_rightPanel.columnWeights = new double[] { 0.0 };
		gbl_rightPanel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		rightPanel.setLayout(gbl_rightPanel);

		JPanel servoDriveSettings = new JPanel();
		servoDriveSettings.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)),
				"Servo-drive configuration", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_servoDriveSettings = new GridBagConstraints();
		gbc_servoDriveSettings.insets = new Insets(0, 0, 5, 0);
		gbc_servoDriveSettings.fill = GridBagConstraints.BOTH;
		gbc_servoDriveSettings.gridx = 0;
		gbc_servoDriveSettings.gridy = 0;
		rightPanel.add(servoDriveSettings, gbc_servoDriveSettings);
		GridBagLayout gbl_servoDriveSettings = new GridBagLayout();
		gbl_servoDriveSettings.columnWidths = new int[] { 139, 0 };
		gbl_servoDriveSettings.rowHeights = new int[] { 0, 0, 0 };
		gbl_servoDriveSettings.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_servoDriveSettings.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		servoDriveSettings.setLayout(gbl_servoDriveSettings);

		servoDriveComPortCombobox = new JComboBox<>();
		GridBagConstraints gbc_servoDriveComPortCombobox = new GridBagConstraints();
		gbc_servoDriveComPortCombobox.insets = new Insets(0, 0, 5, 0);
		gbc_servoDriveComPortCombobox.gridx = 0;
		gbc_servoDriveComPortCombobox.gridy = 0;
		servoDriveSettings.add(servoDriveComPortCombobox, gbc_servoDriveComPortCombobox);
		servoDriveComPortCombobox.setModel(new DefaultComboBoxModel<>(new String[] { "COM1", "COM2", "COM3", "COM4",
				"COM5", "COM6", "COM7", "COM8", "COM9", "COM10", "COM11", "COM12" }));

		JPanel servoStartStopPanel = new JPanel();
		GridBagConstraints gbc_servoStartStopPanel = new GridBagConstraints();
		gbc_servoStartStopPanel.insets = new Insets(8, 8, 8, 8);
		gbc_servoStartStopPanel.fill = GridBagConstraints.BOTH;
		gbc_servoStartStopPanel.gridx = 0;
		gbc_servoStartStopPanel.gridy = 1;
		servoDriveSettings.add(servoStartStopPanel, gbc_servoStartStopPanel);
		servoStartStopPanel.setLayout(new GridLayout(1, 2, 8, 8));

		ActionListener servoStartStop = (ActionEvent e) -> {
			Object comboBoxObj = servoDriveComPortCombobox.getSelectedItem();
			if (comboBoxObj instanceof String) {
				try (ServoController servo = new ServoController(comboBoxObj.toString())) {
					if (e.getActionCommand().contains("Start")) {
						servo.start();
						Object speedValue = getServoFrequencyHzSpinner().getValue();
						if (speedValue instanceof Number) {
							servo.writeSpeed(((Number) speedValue).doubleValue());
						}
					} else if (e.getActionCommand().equals("Stop")) {
						servo.stop();
					}
				} catch (NoSuchPortException e1) {
					e1.printStackTrace();
				} catch (PortInUseException e1) {
					e1.printStackTrace();
				}
			}
		};

		JButton servoStartButton = new JButton("Start");
		servoStartButton.setActionCommand("Start");
		servoStartButton.addActionListener(servoStartStop);
		servoStartStopPanel.add(servoStartButton);

		JButton servoStopButton = new JButton("Stop");
		servoStopButton.setActionCommand("Stop");
		servoStopButton.addActionListener(servoStartStop);
		servoStartStopPanel.add(servoStopButton);

		servoFrequencyHzSpinner = new JSpinner();
		servoFrequencyHzSpinner.setModel(new SpinnerNumberModel(1, 1, 30, 1));
		servoFrequencyHzSpinner.setToolTipText("Частота, Гц");
		servoStartStopPanel.add(servoFrequencyHzSpinner);

		JPanel centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
	}

	protected JSpinner getServoFrequencyHzSpinner() {
		return servoFrequencyHzSpinner;
	}

	@Override
	public int getInitialTemperature() {
		return ((Number) getInitTemperatureSpinner().getValue()).intValue();
	}

	@Override
	public int getMinTemperature() {
		return ((Number) getMinTemperatureSpinner().getValue()).intValue();
	}

	@Override
	public int getMaxTemeprature() {
		return ((Number) getMaxTemperatureSpinner().getValue()).intValue();
	}

	@Override
	public boolean isInitiallyUp() {
		return getInitiallyUpCheckbox().isSelected();
	}

	@Override
	public int getTemperatureStep() {
		return ((Number) getTemperatureStepSpinner().getValue()).intValue();
	}

	@Override
	public String getSerialPortName() {
		return (String) getServoDriveComPortCombobox().getSelectedItem();
	}

	@Override
	public double getExperimentFrequency() {
		return ((Number) getServoFrequencyHzSpinner().getValue()).doubleValue();
	}

	public JSpinner getInitTemperatureSpinner() {
		return initTemperatureSpinner;
	}

	protected JSpinner getMinTemperatureSpinner() {
		return minTemperatureSpinner;
	}

	protected JSpinner getMaxTemperatureSpinner() {
		return maxTemperatureSpinner;
	}

	protected JSpinner getTemperatureStepSpinner() {
		return temperatureStepSpinner;
	}

	protected JCheckBox getInitiallyUpCheckbox() {
		return initiallyUpCheckbox;
	}

	protected JComboBox<String> getServoDriveComPortCombobox() {
		return servoDriveComPortCombobox;
	}
}
