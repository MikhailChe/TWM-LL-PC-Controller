package acquisition;

import static starthere.StartHere.ADC;
import static starthere.widgets.log.GuiLogger.log;

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
import starthere.SettingsHolder;
import unidaq.ChannelConfig;
import unidaq.UniDaqException;

public class Acquisitor {

	SettingsHolder settings;

	public Acquisitor(SettingsHolder settings) {
		this.settings = settings;
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws PortInUseException
	 * @throws NoSuchPortException
	 * @throws FileNotFoundException
	 */
	public void setupReadAndPrintExperiment(final short[] channels, final double experimentFreq, final int numPeriods)
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
			TimeUnit.SECONDS.sleep(1);
			double[][] values = readOut(channels, (float) experimentFreq, numPeriods);
			String filename = String.format("%d.txt", System.currentTimeMillis());

			printData(filename, (float) experimentFreq, values);
			System.out.println(filename);
		}

	}

	public static void printData(String filename, final float experimentFrequency, double[][] data)
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

	public static double[][] readOut(final short channels[], final float experimentFrequency, final int numPeriods)
			throws InterruptedException {
		final int NUM_CHANNELS = channels.length;
		final float sampleFrequency = (experimentFrequency * 1000) * NUM_CHANNELS;
		final int samplesPerChannel = 1000 * numPeriods;

		log().println("Reading " + NUM_CHANNELS + " channels");
		log().println("Sample frequency = " + sampleFrequency + "Hz");

		double[][] vals = new double[samplesPerChannel][NUM_CHANNELS];
		try {
			ChannelConfig[] configuration = new ChannelConfig[NUM_CHANNELS];
			for (int i = 0; i < configuration.length; i++) {
				configuration[i] = ChannelConfig.BI_10V;
			}
			ADC.startAIScan(channels, configuration, sampleFrequency, samplesPerChannel);
			// read in part no longer than .5s
			int totalSamples = samplesPerChannel * NUM_CHANNELS;
			int sample = 0;
			log().println("Must read " + totalSamples + " samples total at " + experimentFrequency + "Hz");
			try {
				double[] flatBuffer = toDoubleArray(ADC.getAIBuffer(totalSamples));

				for (int flatBufferIndex = 0; flatBufferIndex < totalSamples;) {
					for (int channel = 0; channel < NUM_CHANNELS; channel++) {
						vals[sample][channel] = flatBuffer[flatBufferIndex + channel];
					}
					flatBufferIndex += NUM_CHANNELS;
					sample++;
				}

				if (Thread.interrupted())
					throw new InterruptedException();
			} finally {
				ADC.stopAI();
			}
			log().println("Read all");
		} catch (UniDaqException e) {
			e.printStackTrace();
		}
		return vals;
	}

	public static double[] toDoubleArray(final float[] arr) {
		double[] out = new double[arr.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = arr[i];
		}
		return out;
	}
}
