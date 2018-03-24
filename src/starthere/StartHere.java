package starthere;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import asdaservo.ServoController;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import unidaq.ChannelConfig;
import unidaq.UniDAQLib.ADC;
import unidaq.UniDaqException;

public class StartHere {

	final static boolean debug = false;

	// // ** MAKE THOSE LINES CONFIGURABLE VIA GUI ***///
	// final static String serialPortName = "COM1";
	// final static int initialTemperature = 420;
	// final static boolean initiallyUp = true;
	// final static int minTemperature = 390;
	// final static int maxTemeprature = 1650;
	//
	// final static int changeDegrees = 10;
	//
	// final static double EXPERIMENT_FREQUENCY = 5;

	// ***********************************************//

	static boolean needhook = true;

	public static void main(String[] args) {
		MainWindow window = new MainWindow();
		window.pack();
		window.setMinimumSize(window.getSize());
		window.setVisible(true);
		while (window.isVisible()) {
			Thread.yield();
		}
		System.exit(0);

	}

	@FunctionalInterface
	interface TriFunction<A, B, O> {
		O apply(O o, A a, B b);
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws PortInUseException
	 * @throws NoSuchPortException
	 * @throws FileNotFoundException
	 */
	public static void setupReadAndPrintExperiment(SettingsHolder settings, final ADC ADC, final short[] channels,
			final double experimentFreq, final int numPeriods)
			throws InterruptedException, NoSuchPortException, PortInUseException, FileNotFoundException {
		if (experimentFreq <= 0) {
			throw new IllegalArgumentException(
					String.format("Shit. experiment frequency is impossibly low (%f)", experimentFreq));
		}

		if (numPeriods <= 0) {
			throw new IllegalArgumentException(
					String.format("Crap! Number of periods is impossibly low (%d)", numPeriods));

		}

		try (ServoController motor = new ServoController(settings.getSerialPortName())) {
			motor.writeSpeed(experimentFreq);
			TimeUnit.SECONDS.sleep(2);
			float[][] values = readOut(ADC, channels, (float) experimentFreq, numPeriods);
			String filename = String.format("%d.txt", System.currentTimeMillis());

			printData(filename, (float) experimentFreq, values);
			System.out.println(filename);
		}

	}

	public static void printData(String filename, final float experimentFrequency, float[][] data)
			throws InterruptedException {

		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

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

	public static float[][] readOut(final ADC ADC, final short channels[], final float experimentFrequency,
			final int numPeriods) {
		final int NUM_CHANNELS = channels.length;
		final float sampleFrequency = (experimentFrequency * 1000) * NUM_CHANNELS;
		final int samplesPerChannel = 1000 * numPeriods;
		float[][] vals = new float[samplesPerChannel][NUM_CHANNELS];
		try {
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
