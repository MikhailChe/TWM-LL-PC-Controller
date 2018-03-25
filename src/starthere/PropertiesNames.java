package starthere;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Properties;

enum PropertiesNames {
	INITIAL_TEMPERATURE("initialTemperature", 490), //
	ABSOLUTE_MINIMUM_TEMPERATURE("absoluteMinimumTemperature", 300), //
	ABSOLUTE_MAXIMUM_TEMPERATURE("absoluteMaximumTemperature", 1800), //
	CURRENT_MINIMUM_TEMPERATURE("currentMinimumTemperature", 480), //
	CURRENT_MAXIMUM_TEMPERATURE("currentMaximumTemperature", 1650), //
	TEMPERATURE_STEP("currentTemperatureStep", 10), //
	INITIALLY_UP("temperatureRegulationInitiallyUp", true);

	private String name;
	private Object defaultValue;

	PropertiesNames(String name, Object defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	@Override
	public String toString() {
		return name;
	}

	static void fillDefaults(Properties prop) {
		for (PropertiesNames key : EnumSet.allOf(PropertiesNames.class)) {
			key.computeIfAbsent(prop, key.defaultValue);
		}
	}

	<T extends Object> void computeIfAbsent(Properties prop, T value) {
		prop.computeIfAbsent(this.toString(), (s) -> value.toString());
	}

	<T extends Object> T putProperty(Properties prop, T value) {
		prop.put(this.toString(), value.toString());
		return value;
	}

	Integer getIntegerProperty(Properties prop) {
		String value = prop.getProperty(this.toString());
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
		String value = prop.getProperty(this.toString());
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
