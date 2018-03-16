package regulator;

final public class SlopeLimiter {

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

	final private double limitPerUnitTime;

	private volatile double oldValue = Double.NaN;
	private volatile long lastTime = Long.MIN_VALUE;

	final public double limit(final double value, final long CURRENTTIME) {

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
		if (!Double.isFinite(dt) || dt <= 0.00001) {
			throw new IllegalArgumentException("Delta time is impossibly small or nan = " + dt);
		}

		if (!Double.isInfinite(value)) {
			throw new IllegalArgumentException("Value is not finitie (=" + value);
		}

		if (!Double.isFinite(oldValue)) {
			oldValue = value;
		} else {
			if (Math.abs(value - oldValue) / dt > limitPerUnitTime) {
				double sign = Math.signum(value - oldValue);
				oldValue = oldValue + sign * dt * limitPerUnitTime;
			} else {
				oldValue = value;
			}
		}
		return oldValue;
	}

	final public double getValue() {
		return this.oldValue;
	}

	final public void resetTiming() {
		lastTime = Long.MIN_VALUE;
	}
}
