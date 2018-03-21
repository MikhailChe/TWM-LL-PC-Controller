package regulator;

public class PolynomialController {

	public final double sq_term, lin_term, const_term;

	public PolynomialController(double sq, double linear, double constant) {
		if (Double.isFinite(sq) && Double.isFinite(linear) && Double.isFinite(constant)) {
			this.sq_term = sq;
			this.lin_term = linear;
			this.const_term = constant;
		} else {
			throw new IllegalArgumentException(
					"Some parameters are not finite: sq=" + sq + ", linear=" + linear + ", constant=" + constant);
		}
	}

	private double lastValue = Double.NaN;

	public double regulate(double input) {
		return lastValue = input * (input * sq_term + lin_term) + const_term;
	}

	public double getLastValue() {
		return lastValue;
	}

}
