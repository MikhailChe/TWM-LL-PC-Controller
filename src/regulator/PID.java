package regulator;

final public class PID {
	static boolean DEBUG = true;

	final private double P;
	final private double I;
	final private double D;

	/**
	 * 
	 * @param P
	 *            Proportional coefficient of regulator. </br>
	 *            Basically, it's a gain of input error.
	 * @param I
	 *            Integrating coefficient of regulator </br>
	 *            It summs all the errors through time. So if you have an error of
	 *            1, than in a second you'd have an integral of (1 times I)
	 * @param D
	 *            Differentiating coefficient of regulator </br>
	 *            It prevents an error from changing too fast. If input error of
	 *            regulator is changed, it tries to quickly minimize that change.
	 *            For example, say you've got a temprature set and steady. Suddenly
	 *            it's getting colder, so differential part will try to quickly
	 *            compensate for that change.
	 */
	public PID(final double P, final double I, final double D) {
		if (!Double.isFinite(P) || !Double.isFinite(I) || !Double.isFinite(D)) {
			throw new IllegalArgumentException(String.format("Not a number: P=%.2f,I=%.2f,D=%.2f", P, I, D));
		}
		this.P = P;
		this.I = I * P;
		this.D = D * P;
	}

	final public PID setProportionalBounds(final double min, final double max) {
		if (Double.isFinite(max) && Double.isFinite(min)) {
			if (max < min)
				throw new IllegalArgumentException("max < min");
			this.proportionalMax = max;
			this.proportionalMin = min;
		}
		return this;
	}

	final public PID setIntegralBounds(final double min, final double max) {
		if (Double.isFinite(max) && Double.isFinite(min)) {
			if (max < min)
				throw new IllegalArgumentException("max < min");
			this.integralMax = max;
			this.integralMin = min;
		}
		return this;
	}

	final public PID setDifferentialBounds(final double min, final double max) {
		if (Double.isFinite(max) && Double.isFinite(min)) {
			if (max < min)
				throw new IllegalArgumentException("max < min");
			this.differentialMax = max;
			this.differentialMin = min;
		}
		return this;
	}

	volatile private double proportionalMax = 1;
	volatile private double proportionalMin = -1;

	volatile private double integral = 0;
	volatile private double integralMax = 1;
	volatile private double integralMin = -1;

	volatile private double oldError = Double.NaN;

	volatile private double differentialMax = 1;
	volatile private double differentialMin = -1;

	volatile private double lastRegulation = 0;
	volatile private long lastTime = Long.MIN_VALUE;

	final public double regulate(final double error, final long CURRENTTIME) {

		if (lastTime == Long.MIN_VALUE) {
			lastTime = CURRENTTIME;
		}
		double delta = (CURRENTTIME - lastTime) / 1000.0;
		lastTime = CURRENTTIME;
		if (delta <= 0.0005) {
			delta = 0.0005;
		}
		return regulate(error, delta);
	}

	final private double regulate(final double error, final double dt) {

		double proportional = error * P;

		integral += error * I * dt;
		double differential = 0;
		if (!Double.isFinite(oldError)) {
			oldError = error;
		} else {
			differential = (error - oldError) * D / dt;
		}
		oldError = error;

		proportional = bound(proportional, proportionalMin, proportionalMax);
		integral = bound(integral, integralMin, integralMax);
		differential = bound(differential, differentialMin, differentialMax);

		if (DEBUG)
			System.out.printf("dt %-5.3f err %-5.1f pr %-5.1f int %-5.1f dif %-5.1f%n", dt, error, proportional,
					integral, differential);
		return lastRegulation = proportional + integral + differential;
	}

	final public void resetTimings() {
		lastTime = Long.MIN_VALUE;
		oldError = Double.NaN;
	}

	final public double getLast() {
		return lastRegulation;
	}

	final private static double bound(double value, double min, double max) {
		return Math.max(Math.min(value, max), min);
	}
}
