package regulator;

final public class SlopeLimiter {

	final double limitPerUnitTime;

	public SlopeLimiter(double limitPerUnitTime, double initialValue) {
		if (Double.isFinite(limitPerUnitTime)) {
			this.limitPerUnitTime = limitPerUnitTime;
		} else {
			throw new IllegalArgumentException("Not a number: ticksPerSeconds=" + limitPerUnitTime);
		}
		if (Double.isFinite(initialValue)) {
			this.oldValue = initialValue;
		} else {
			throw new IllegalArgumentException("Not a number: initialValue=" + initialValue);
		}
	}

	private volatile double oldValue = Double.NaN;
	private volatile long lastTime = Long.MIN_VALUE;

	private volatile double limitedValue = 0;

	public final double limit(final double value, final long CURRENTTIME) {

		if (lastTime == Long.MIN_VALUE) {
			lastTime = CURRENTTIME;
		}
		double dt = (CURRENTTIME - lastTime) / 1000.0;
		lastTime = CURRENTTIME;
		if (dt <= 0.0005) {
			dt = 0.0005;
		}
		return limit(value, dt);
	}

	final private double limit(final double value, final double dt) {
		if (dt <= 0.00001) {
			throw new IllegalArgumentException("Delta time is impossibly small");
		}

		if (Double.isNaN(oldValue)) {
			oldValue = value;
		}

		if (Math.abs(value - oldValue) / dt > limitPerUnitTime) {
			double sign = Math.signum(value - oldValue);
			limitedValue = oldValue + sign * dt * limitPerUnitTime;
		} else {
			limitedValue = value;
		}
		oldValue = limitedValue;
		return limitedValue;
	}

	public final double getValue() {
		return this.limitedValue;
	}

	public void resetTiming() {
		oldValue = Double.NaN;

	}

}
