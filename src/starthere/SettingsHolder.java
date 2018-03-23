package starthere;

public interface SettingsHolder {

	int getInitialTemperature();

	int getMinTemperature();

	int getMaxTemeprature();

	int getTemperatureStep();

	boolean isInitiallyUp();

	String getSerialPortName();

	double getExperimentFrequency();

}
