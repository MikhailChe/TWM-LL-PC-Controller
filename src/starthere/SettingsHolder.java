package starthere;

import java.util.concurrent.TimeUnit;

public interface SettingsHolder {

	int getInitialTemperature();

	int getMinTemperature();

	int getMaxTemeprature();

	int getTemperatureStep();

	boolean isInitiallyUp();

	String getSerialPortName();

	double getExperimentFrequency();

	double getStabilizationDegrees();

	int getStabilizationTime();

	TimeUnit getStabilizationTimeUnit();

	int getNumberOfPeriodsPerMeasure();

}
