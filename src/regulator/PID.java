package regulator;

public class PID {
	final double P;
	final double I;
	final double D;

	public PID(double P, double I, double D) {
		if (!Double.isFinite(P) || !Double.isFinite(I) || !Double.isFinite(D)) {
			throw new IllegalArgumentException(String.format("Not a number: P=%.2f,I=%.2f,D=%.2f", P, I, D));
		}
		this.P = P;
		this.I = I * P;
		this.D = D * P;
	}

	public PID setProportionalBounds(double min, double max) {
		if (Double.isFinite(max) && Double.isFinite(min)) {
			if (max < min)
				throw new IllegalArgumentException("max < min");
			this.proportionalMax = max;
			this.proportionalMin = min;
		}
		return this;
	}

	public PID setIntegralBounds(double min, double max) {
		if (Double.isFinite(max) && Double.isFinite(min)) {
			if (max < min)
				throw new IllegalArgumentException("max < min");
			this.integralMax = max;
			this.integralMin = min;
		}
		return this;
	}

	public PID setDifferentialBounds(double min, double max) {
		if (Double.isFinite(max) && Double.isFinite(min)) {
			if (max < min)
				throw new IllegalArgumentException("max < min");
			this.differentialMax = max;
			this.differentialMin = min;
		}
		return this;
	}

	volatile double proportionalMax = 1;
	volatile double proportionalMin = -1;

	volatile double integral = 0;
	volatile double integralMax = 1;
	volatile double integralMin = -1;

	volatile double oldError = Double.NaN;

	volatile double differentialMax = 1;
	volatile double differentialMin = -1;

	private double lastRegulation = 0;

	long time = Long.MIN_VALUE;

	public double regulate(final double error, final long TIME) {

		if (time == Long.MIN_VALUE) {
			time = TIME;
		}
		double delta = (TIME - time) / 1000.0;
		time = TIME;
		if (delta <= 0.0005) {
			delta = 0.0005;
		}
		return regulate(error, delta);
	}

	private double regulate(final double error, final double dt) {
		double proportional = error * P;
		integral += error * I * dt;
		double differential = 0;
		if (Double.isNaN(oldError)) {
			oldError = error;
		} else {
			differential = (error - oldError) * D / dt;
		}
		oldError = error;

		proportional = bound(proportional, proportionalMin, proportionalMax);
		integral = bound(integral, integralMin, integralMax);
		differential = bound(differential, differentialMin, differentialMax);

		System.out.printf("dt %-5.3f err %-5.1f pr %-5.1f int %-5.1f dif %-5.1f%n", dt, error, proportional, integral,
				differential);
		return lastRegulation = proportional + integral + differential;
	}

	public void resetTimings() {
		time = Long.MIN_VALUE;
		oldError = Double.NaN;
	}

	public double getLast() {
		return lastRegulation;
	}

	private static double bound(double value, double min, double max) {
		return Math.max(Math.min(value, max), min);
	}
}
