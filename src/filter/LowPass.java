package filter;

public class LowPass {
	long lastInputTime = Long.MIN_VALUE;

	public LowPass(double RC) {
		this.RC = RC;
	}

	private double RC = Double.NaN;
	private double value = Double.NaN;

	public double getValue() {
		return value;
	}

	public double filter(double newValue) {

		final long CURRENTTIME = System.currentTimeMillis();
		if (Double.isNaN(value) || lastInputTime == Long.MIN_VALUE) {
			value = newValue;
			lastInputTime = CURRENTTIME;
			return value;
		}
		lastInputTime = CURRENTTIME;

		long dtms = CURRENTTIME - lastInputTime;
		if (dtms <= 0) {
			dtms = 1;
		}
		double dt = dtms / 1000.0;

		double a = dt / (RC + dt);

		value = newValue * a + (1 - a) * value;
		return value;
	}

	public static double TauToRC(double T) {
		return T / (2.0 * Math.PI);
	}

	public static double FrequencyToRC(double freqHZ) {
		return 1.0 / (2.0 * Math.PI * freqHZ);
	}
}
