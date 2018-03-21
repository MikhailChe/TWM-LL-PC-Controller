package starthere;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import asdaservo.ServoController;
import filter.LowPass;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import model.thermocouple.graduate.Graduate;
import model.thermocouple.graduate.GraduateFactory;
import regulator.PID;
import regulator.SlopeLimiter;
import unidaq.ChannelConfig;
import unidaq.UniDAQLib;
import unidaq.UniDAQLib.ADC;
import unidaq.UniDAQLib.DAC;
import unidaq.UniDaqException;

public class StartHere {
	static UniDAQLib boards = null;

	final static boolean debug = false;

	// ** MAKE THOSE LINES CONFIGURABLE VIA GUI ***///
	final static String serialPortName = "COM1";
	final static int initialTemperature = 590;
	final static boolean initiallyUp = true;
	final static int minTemperature = 390;
	final static int maxTemeprature = 1650;

	final static int changeDegrees = 10;

	final static double EXPERIMENT_FREQUENCY = 5;

	// ***********************************************//

	static boolean needhook = true;

	public static void main(String[] args)
			throws IOException, InterruptedException, NoSuchPortException, PortInUseException {
		try {
			PID regulator = new PID(.05, 1 / 75.0, .1).setProportionalBounds(-5, 5).setIntegralBounds(-5, 5)
					.setDifferentialBounds(-.5, .5);
			SlopeLimiter outputSlopeLimit = new SlopeLimiter(1, 0);
			boards = UniDAQLib.instance();

			ADC ADC = boards.getADC(0);
			DAC DAC = boards.getDAC(0);

			int kelvins = initialTemperature;
			boolean goingUp = initiallyUp;

			// **** HANDLE CTRL-C or CLI window closing. **** //
			// **Closing port and freeing driver resources ** //
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				if (needhook) {
					System.out.println("Initiating shutdown hook");
					try {
						boards.getDAC(0).writeAOVoltage(0);
					} catch (UniDaqException e) {
						e.printStackTrace();
					}
					if (boards != null) {
						try {
							boards.close();
						} catch (UniDaqException e) {
							e.printStackTrace();
						}
					}
				}
			}));

			try (ServoController motor = new ServoController(serialPortName)) {
				motor.start();
				motor.writeSpeed(5);
			}

			// ** START CONTROL AND MEASUREMENT LOOP ** //
			try (PrintStream powerToTemperature = new PrintStream("fileTemperature.tsv")) {
				while (true) {
					regulator.resetTimings();
					outputSlopeLimit.resetTiming();
					boolean successsControl = controlLoop(kelvins, 2, TimeUnit.SECONDS.toMillis(5), regulator,
							outputSlopeLimit, ADC, DAC);
					if (!successsControl) {
						boolean stopDetected = false;
						while (System.in.available() > 0) {
							int val = System.in.read();
							if ((val == -1) || ((char) val) == 's') {
								stopDetected = true;
							}
						}
						if (stopDetected) {
							break;
						}
					}
					powerToTemperature.printf("%f\t%d%n", regulator.getLast(), kelvins);
					// *****MEASURE ****///
					setupReadAndPrintExperiment(new short[] { 0, 2, 4, 6 }, EXPERIMENT_FREQUENCY, 64);

					if (goingUp) {
						kelvins += changeDegrees;
					} else {
						kelvins -= changeDegrees;
					}

					if (kelvins > maxTemeprature) {
						goingUp = false;
					}
					if (kelvins < minTemperature) {
						goingUp = true;
					}
				}
				powerToTemperature.flush();
			}

		} catch (UniDaqException e) {
			e.printStackTrace();
		} finally {
			if (boards != null) {

				try {
					boards.getDAC(0).writeAOVoltage(0);
				} catch (UniDaqException e) {
					e.printStackTrace();
				}

				try {
					boards.close();
				} catch (UniDaqException e) {
					e.printStackTrace();
				}
				needhook = false;
			}
		}
	}

	@FunctionalInterface
	interface TriFunction<A, B, O> {
		O apply(O o, A a, B b);
	}

	public static boolean controlLoop(final double SETTED_VALUE, final double DEVIATION, final long settlingTimeMillis,
			final PID regulator, final SlopeLimiter outputSlopeLimit, final ADC ADC, final DAC DAC) {
		try {
			while (System.in.available() > 0) {
				System.in.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// *** CONFUGRE ADC AND DAC ***//

		try (PrintStream out = new PrintStream(String.format("output-%d.tsv", System.currentTimeMillis() / 1000L));) {
			short[] channels = { 0, 2, 4, 6 };
			ChannelConfig[] configEnum = { ChannelConfig.BI_10V, ChannelConfig.BI_10V, ChannelConfig.BI_10V,
					ChannelConfig.BI_10V };
			final int temperatureChanel = 1;

			long insideSettedRegionTime = System.currentTimeMillis();

			Graduate grad = GraduateFactory.forBinary(new File("VR-5-20-list2.gradbin"));

			// *** PREPARE REGULATION LINE ***//
			TriFunction<Double, Double, Double> clipper = (o, min, max) -> Math
					.max(Math.min(o.doubleValue(), max.doubleValue()), min.doubleValue());
			// PolynomialController directTemperatureToPowerControl = new
			// PolynomialController(2E-6, -0.0016, 1.0477);

			// *** LOW PASS FILTERS ***//
			LowPass temperatureFilter = new LowPass(LowPass.FrequencyToRC(10));
			LowPass temperatureStabFilter = new LowPass(LowPass.FrequencyToRC(5));
			LowPass errorFilter = new LowPass(LowPass.FrequencyToRC(20));

			long lastMeasureTime = System.currentTimeMillis();
			// *** PREPARE FOR PRINTING STUFF ***//

			final long STARTTIME = System.currentTimeMillis();
			double oldTemperature = Double.NaN;

			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			while (true) {
				// ADC read//
				ADC.clearAIBuffer();
				ADC.startAIScan(channels, configEnum, 400, 1);
				float[] inputValues = ADC.getAIBuffer(channels.length);
				ADC.stopAI();
				// ***********//
				final long CURRENTTIME = System.currentTimeMillis();

				double dt = (CURRENTTIME - lastMeasureTime) / 1000.0;
				lastMeasureTime = CURRENTTIME;
				if (dt <= 0.0005) {
					dt = 0.0005;
				}

				double val = inputValues[temperatureChanel];
				final double amplifierGAIN = 270;
				double voltage = val / amplifierGAIN;
				double temperature = grad.getTemperature(voltage * 1000.0, 18 + 273.25);

				temperature = temperatureFilter.filter(temperature);
				temperatureStabFilter.filter(temperature);

				double error = errorFilter.filter(SETTED_VALUE - temperature);

				double force = regulator.regulate(error, CURRENTTIME);
				// + directTemperatureToPowerControl.regulate(error + temperature); // reusing
				// already filtered
				// value

				double clipped = clipper.apply(force, 0.0, 4.95);

				double slopeLimited = outputSlopeLimit.limit(clipped, CURRENTTIME);

				double normalizeTo2 = 2.0 * slopeLimited / 5.0;

				double clippedNormalization = clipper.apply(normalizeTo2, 0.0, 1.9999999999);

				double linearization = Math.acos(1 - clippedNormalization) / Math.PI * 4.995;

				DAC.writeAOVoltage(clipper.apply(linearization, 0.0, 4.0));

				// ************* PRINT **********//

				out.printf("%f\t%10f\t%10f\t%10f\t%10f\t%10f%n", (CURRENTTIME - STARTTIME) / 1000.0,
						temperatureFilter.getValue(), temperatureStabFilter.getValue(), error, force, linearization);

				if (Math.abs(SETTED_VALUE - temperatureStabFilter.getValue()) > Math.abs(DEVIATION)) {
					insideSettedRegionTime = CURRENTTIME;
				} else {
					if ((CURRENTTIME - insideSettedRegionTime) > settlingTimeMillis) {
						return true;
					}
				}
				if (Double.isNaN(oldTemperature) || Math.abs(oldTemperature - temperatureStabFilter.getValue()) > .1) {
					if (debug) {
						System.out.printf("%20.1f K\t%20.4f%n", temperatureStabFilter.getValue(), slopeLimited);
					}
					oldTemperature = temperatureStabFilter.getValue();
				}
				if (System.in.available() > 0) {
					return false;
				}

			}
		} catch (UniDaqException | IOException e1) {
			e1.printStackTrace();
		}
		return false;
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws PortInUseException
	 * @throws NoSuchPortException
	 * @throws FileNotFoundException
	 */
	public static void setupReadAndPrintExperiment(final short[] channels, final double experimentFreq,
			final int numPeriods)
			throws InterruptedException, NoSuchPortException, PortInUseException, FileNotFoundException {
		if (experimentFreq <= 0) {
			throw new IllegalArgumentException(
					String.format("Shit. experiment frequency is impossibly low (%f)", experimentFreq));
		}

		if (numPeriods <= 0) {
			throw new IllegalArgumentException(
					String.format("Crap! Number of periods is impossibly low (%d)", numPeriods));

		}

		try (ServoController motor = new ServoController(serialPortName)) {
			motor.writeSpeed(experimentFreq);
			TimeUnit.SECONDS.sleep(2);
			float[][] values = readOut(channels, (float) experimentFreq, numPeriods);
			String filename = String.format("%d.txt", System.currentTimeMillis());

			printData(filename, (float) experimentFreq, values);
			System.out.println(filename);
		}

	}

	public static void printData(String filename, final float experimentFrequency, float[][] data) {

		final double ADC_MAX_VOLTAGE = 10;
		final double ADC_MAXTICKS = 32768;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(baos);) {

			ps.printf("%d\t%.0f%n", data.length, experimentFrequency * 10.0f);
			for (int i = 0; i < data.length; i++) {
				for (int channel = 0; channel < data[i].length; channel++) {
					ps.printf("%.0f", data[i][channel] * ADC_MAXTICKS / ADC_MAX_VOLTAGE);
					if (channel < data[i].length - 1)
						ps.print("\t");
				}
				ps.println();
			}
		}
		try {
			Files.write(Paths.get(filename), baos.toByteArray(), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static float[][] readOut(final short channels[], final float experimentFrequency, final int numPeriods) {
		final int NUM_CHANNELS = channels.length;
		final float sampleFrequency = (experimentFrequency * 1000) * NUM_CHANNELS;
		final int samplesPerChannel = 1000 * numPeriods;
		float[][] vals = new float[samplesPerChannel][NUM_CHANNELS];
		try {
			UniDAQLib.ADC ADC = boards.getADC(0);

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
