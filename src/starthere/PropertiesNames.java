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
	TEMPERATURE_STABILITY_K(.5), //
	TEMPERATURE_STABILITY_TIMEUNIT(TimeUnit.SECONDS), //
	TEMPERATURE_STABILITY_TIME(4), //
	SERVODRIVE_COMPORT("COM1"), //
	SERVODRIVE_FREQUENCY(5.0);

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

	<T extends Object> void putProperty(Properties prop, T value) {
		if (value.getClass().getSuperclass().isEnum()) {
			Enum<?> e = (Enum<?>) value;
			prop.put(this.name(), e.name());
			System.out.println("New enum property: " + e.name());

		} else {
			System.out.println("New object property: " + value.toString());
			prop.put(this.name(), value.toString());
		}
	}

	String getProperty(Properties prop) {
		return prop.getProperty(this.name());
	}

	Integer getIntegerProperty(Properties prop) {
		String value = this.getProperty(prop);
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

	Double getDoubleProperty(Properties prop) {
		String value = this.getProperty(prop);
		if (value == null)
			return null;
		try {
			Double out = Double.parseDouble(value);
			return out;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			try {
				Integer out = Integer.parseInt(value);
				return out.doubleValue();
			} catch (NumberFormatException e2) {
				e2.printStackTrace();
				return null;
			}
		}
	}

	Boolean getBooleanProperty(Properties prop) {
		String value = this.getProperty(prop);
		if (value == null)
			return null;
		try {
			Boolean out = Boolean.parseBoolean(value);
			return out;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	<T extends Enum<T>> T getEnum(Properties prop, Class<T> c) {
		String value = this.getProperty(prop);
		System.out.println(value);
		try {
			return Enum.valueOf(c, value);
		} catch (ClassCastException e) {
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
