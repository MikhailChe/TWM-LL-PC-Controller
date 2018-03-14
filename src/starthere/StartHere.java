package starthere;

import static java.lang.System.out;

import java.io.IOException;
import java.io.PrintStream;

import unidaq.ChannelConfig;
import unidaq.UniDAQLib;
import unidaq.UniDaqException;
import unidaq.UniDaqLibrary;

public class StartHere {

	public static void main(String[] args) throws IOException {
		controlLoop();
	}

	/**
	 * @param args
	 */
	public static void main2(String[] args) {
		final float experimentFreq = 5;
		final int numPeriods = 2;
		float[][] values = readOut(new short[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 }, experimentFreq, numPeriods);

		printData(System.out, experimentFreq, values);
	}

	public static void printData(PrintStream ps, final float experimentFrequency, float[][] data) {
		System.out.printf("%d\t%.0f%n", data.length, experimentFrequency * 10.0f);
		for (int i = 0; i < data.length; i++) {
			for (int channel = 0; channel < data[i].length; channel++) {
				ps.printf("%.0f", data[i][channel] * 32768);
				if (channel < data[i].length - 1)
					ps.print("\t");
			}
			ps.println();
		}
	}

	@FunctionalInterface
	interface TriFunction<A, B, O> {
		O apply(O o, A a, B b);
	}

	public static void controlLoop() {
		try {
			while (System.in.available() > 0) {
				System.in.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (UniDAQLib board = UniDAQLib.instance()) {
			UniDAQLib.ADC ADC = board.getADC(0);
			UniDAQLib.DAC DAC = board.getDAC(0);
			short[] channels = { 0, 2, 4, 6 };
			short[] config = { UniDaqLibrary.IXUD_BI_10V, UniDaqLibrary.IXUD_BI_10V, UniDaqLibrary.IXUD_BI_10V,
					UniDaqLibrary.IXUD_BI_10V };

			final double P_K = 5;
			final double I_K = .1 * P_K;
			final double SETTED_VALUE = 1.5;
			final double FILTER_VALUE = 0.95;
			double filtered = 0;

			TriFunction<Double, Double, Double> clipper = (o, min, max) -> Math.max(Math.min(o, max), min);

			final int temperatureChanel = 1;
			try {
				// DAC.writeAOVoltage(4.9);
				long measurements = System.currentTimeMillis();
				double integral = 0;
				while (true) {
					double dt = (System.currentTimeMillis() - measurements) / 1000.0;
					measurements = System.currentTimeMillis();
					if (dt <= 0.001) {
						dt = 0.001;
					}

					float[] inputValues = ADC.pollingAIScan(channels, config, 1);
					double val = inputValues[temperatureChanel];

					double error = SETTED_VALUE - val;

					integral += error * I_K * dt;
					if (integral > 4) {
						integral = 4;
					} else if (integral < -4) {
						integral = -4;
					}

					double force = error * P_K + integral;

					filtered = filtered * FILTER_VALUE + force * (1 - FILTER_VALUE);

					filtered = clipper.apply(filtered, 0.0, 4.5);
					out.printf("val = %10f\terror = %10f\tforce = %10f\tfilt = %10f%n", val, error, force, filtered);
					// for (int i = 0; i < inputValues.length; i++) {
					// out.printf("%12f", inputValues[i]);
					// }
					// out.printf("%n");

					DAC.writeAOVoltage(filtered);
					// DAC.writeAOVoltage(0);
					try {
						if (System.in.available() > 0) {
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (UniDaqException e) {
				e.printStackTrace();
			} finally {
				try {
					DAC.writeAOVoltage(0);
				} catch (UniDaqException e) {
					e.printStackTrace();
				}
				// ADC.stopAI();
			}
		} catch (UniDaqException e2) {
			e2.printStackTrace();
		}
	}

	public static float[][] readOut(final short channels[], final float experimentFrequency, final int numPeriods) {
		final int NUM_CHANNELS = channels.length;
		final float sampleFrequency = (experimentFrequency * 1000) * NUM_CHANNELS;
		final int samplesPerChannel = 1000 * numPeriods;
		float[][] vals = new float[samplesPerChannel][NUM_CHANNELS];
		try (UniDAQLib board = UniDAQLib.instance()) {
			UniDAQLib.ADC ADC = board.getADC(0);
			System.out.println("SUCCESS + " + board.getTotalBoards());
			ChannelConfig[] configuration = new ChannelConfig[NUM_CHANNELS];
			for (int i = 0; i < configuration.length; i++) {
				configuration[i] = ChannelConfig.BI_10V;
			}

			ADC.startAIScan(channels, configuration, sampleFrequency, samplesPerChannel);
			float[] buffer = ADC.getAIBuffer(samplesPerChannel * NUM_CHANNELS);
			ADC.stopAI();
			for (int j = 0; j < samplesPerChannel; j++) {
				for (int i = 0; i < NUM_CHANNELS; i++) {
					int index = j * NUM_CHANNELS + i;
					vals[j][i] = buffer[index];
				}
			}
		} catch (UniDaqException e) {
			e.printStackTrace();
		}
		return vals;
	}

}
