package starthere;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import unidaq.ChannelConfig;
import unidaq.UniDAQLib;
import unidaq.UniDaqException;
import unidaq.UniDaqLibrary;

public class StartHere {

	public static void main(String[] args) {
		try (UniDAQLib DAC = new UniDAQLib()) {
			DAC.configAO((short) 0, (short) 0, UniDaqLibrary.IXUD_AO_BI_5V);

			for (int i = 0; i < 100; i++) {
				DAC.writeAOVoltage((short) 0, (short) 0, (float) Math.sin(2.0 * 3.141592 * i / 100.0));
				try {
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (UniDaqException e) {
			e.printStackTrace();
		}
	}

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

	public static float[][] readOut(final short channels[], final float experimentFrequency, final int numPeriods) {
		final int NUM_CHANNELS = channels.length;
		final float sampleFrequency = (experimentFrequency * 1000) * NUM_CHANNELS;
		final int samplesPerChannel = 1000 * numPeriods;
		float[][] vals = new float[samplesPerChannel][NUM_CHANNELS];
		try (UniDAQLib ADC = new UniDAQLib()) {
			System.out.println("SUCCESS + " + ADC.getTotalBoards());
			ChannelConfig[] configuration = new ChannelConfig[NUM_CHANNELS];
			for (int i = 0; i < configuration.length; i++) {
				configuration[i] = ChannelConfig.BI_10V;
			}

			ADC.startAIScan(0, channels, configuration, sampleFrequency, samplesPerChannel);
			float[] buffer = ADC.getAIBuffer((short) 0, samplesPerChannel * NUM_CHANNELS);
			ADC.stopAI((short) 0);
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
