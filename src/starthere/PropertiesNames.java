package starthere;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

enum PropertiesNames {
	INITIAL_TEMPERATURE(490), //
	ABSOLUTE_MINIMUM_TEMPERATURE(300), //
	ABSOLUTE_MAXIMUM_TEMPERATURE(1800), //
	CURRENT_MINIMUM_TEMPERATURE(480), //
	CURRENT_MAXIMUM_TEMPERATURE(1650), //
	TEMPERATURE_STEP(10), //
	INITIALLY_UP(true), //
	TEMPERATURE_STABILITY_K(3), //
	TEMPERATURE_STABILITY_TIMEUNIT(TimeUnit.SECONDS), //
	TEMPERATURE_STABILITY_TIME(4), //
	SERVODRIVE_COMPORT("COM1"), //
	SERVIDRIVE_FREQUENCY(5);

	private Object defaultValue;

	PropertiesNames(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	static void fillDefaults(Properties prop) {
		for (PropertiesNames key : EnumSet.allOf(PropertiesNames.class)) {
			key.computeIfAbsent(prop, key.defaultValue);
		}
	}

	<T extends Object> void computeIfAbsent(Properties prop, T value) {
		prop.computeIfAbsent(this.name(), (s) -> value.toString());
	}

	<T extends Object> T putProperty(Properties prop, T value) {
		prop.put(this.name(), value.toString());
		return value;
	}

	Object getProperty(Properties prop) {
		return prop.getProperty(this.name());
	}

	Integer getIntegerProperty(Properties prop) {
		String value = prop.getProperty(this.name());
		if (value == null)
			return null;
		try {
			Integer out = Integer.parseInt(value);
			return out;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			try {
				Double out = Double.parseDouble(value);
				return out.intValue();
			} catch (NumberFormatException e2) {
				e2.printStackTrace();
				return null;
			}
		}
	}

	Boolean getBooleanProperty(Properties prop) {
		String value = prop.getProperty(this.name());
		if (value == null)
			return null;
		try {
			Boolean out = Boolean.parseBoolean(value);
			return out;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static void saveProperties(Properties prop) {
		try (FileOutputStream propFile = new FileOutputStream("MainWindow.properties")) {
			prop.store(propFile, "Properties for temperature regulation and hardware");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
