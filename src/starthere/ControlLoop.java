package starthere;

import static starthere.ControlLoop.ReturnStatus.INTERRUPT;
import static starthere.ControlLoop.ReturnStatus.OK;

import java.io.File;
import java.util.concurrent.Callable;

import filter.LowPass;
import model.thermocouple.graduate.Graduate;
import model.thermocouple.graduate.GraduateFactory;
import regulator.PID;
import regulator.SlopeLimiter;
import starthere.ControlLoop.ReturnStatus;
import starthere.StartHere.TriFunction;
import starthere.widgets.TemperatureDisplay;
import starthere.widgets.log.GuiLogger;
import unidaq.ChannelConfig;
import unidaq.UniDAQLib.ADC;
import unidaq.UniDAQLib.DAC;
import unidaq.UniDaqException;

public class ControlLoop implements Callable<ReturnStatus> {

	double SETTED_VALUE;
	final SettingsHolder settings;
	final PID regulator;
	final SlopeLimiter outputSlopeLimit;
	final ADC ADC;
	final DAC DAC;

	static Graduate grad = GraduateFactory.forBinary(new File("VR-5-20-list2.gradbin"));

	// *** PREPARE REGULATION LINE ***//
	static TriFunction<Double, Double, Double> clipper = (o, min, max) -> Math
			.max(Math.min(o.doubleValue(), max.doubleValue()), min.doubleValue());

	// *** LOW PASS FILTERS ***//
	static LowPass temperatureFilter = new LowPass(LowPass.FrequencyToRC(10));
	static LowPass temperatureStabFilter = new LowPass(LowPass.FrequencyToRC(5));
	static LowPass errorFilter = new LowPass(LowPass.FrequencyToRC(20));

	public ControlLoop(final SettingsHolder settings, final PID regulator, final SlopeLimiter outputSlopeLimit,
			final ADC ADC, final DAC DAC) {
		this.settings = settings;
		this.SETTED_VALUE = settings.getInitialTemperature();
		this.regulator = regulator;
		this.outputSlopeLimit = outputSlopeLimit;
		this.ADC = ADC;
		this.DAC = DAC;

	}

	static enum ReturnStatus {
		OK, TIMEOUT, INTERRUPT, CRITICAL
	}

	@Override
	public ReturnStatus call() {
		{
			// *** CONFUGRE ADC AND DAC ***//
			short[] channels = { 0, 2, 4, 6 };
			ChannelConfig[] configEnum = { ChannelConfig.BI_10V, ChannelConfig.BI_10V, ChannelConfig.BI_10V,
					ChannelConfig.BI_10V };
			final int temperatureChanel = 1;

			long insideSettedRegionTime = System.currentTimeMillis();

			long lastMeasureTime = System.currentTimeMillis();
			// *** PREPARE FOR PRINTING STUFF ***//
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			while (true) {

				final long CURRENTTIME = System.currentTimeMillis();

				double dt = (CURRENTTIME - lastMeasureTime) * .001;
				lastMeasureTime = CURRENTTIME;
				if (dt <= 0.0005) {
					dt = 0.0005;
				}

				// ADC read//
				double temperature;
				try {
					temperature = getKelvins(channels, configEnum, temperatureChanel);
				} catch (UniDaqException e) {
					e.printStackTrace();
					return ReturnStatus.CRITICAL;
				}
				temperature = temperatureFilter.filter(temperature);

				TemperatureDisplay.instance().updateTempearture(temperature);

				temperatureStabFilter.filter(temperature);

				double linearization = regulationLinearization(errorFilter, CURRENTTIME, temperature);
				try {
					double clippedOutput = clipper.apply(linearization, 0.0, 4.0);
					DAC.writeAOVoltage(clippedOutput);
				} catch (UniDaqException e) {
					if (e.getCode() != 15) {
						e.printStackTrace();
						return ReturnStatus.CRITICAL;
					}
				}

				if (Math.abs(SETTED_VALUE - temperatureStabFilter.getValue()) > Math
						.abs(settings.getStabilizationDegrees())) {
					insideSettedRegionTime = CURRENTTIME;
				} else {
					if ((CURRENTTIME - insideSettedRegionTime) > settings.getStabilizationTimeUnit()
							.toMillis(settings.getStabilizationTime())) {
						return OK;
					}
				}
				// TODO: update temperature on GUI right here
				if (Thread.interrupted()) {
					GuiLogger.log().println("Control loop detected interrupt");
					return INTERRUPT;
				}
			}
		}
	}

	private double regulationLinearization(LowPass errorFilter, final long CURRENTTIME, double temperature) {
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
		return linearization;
	}

	private double getKelvins(short[] channels, ChannelConfig[] configEnum, final int temperatureChanel)
			throws UniDaqException {
		double adcTemperatureVal = readTempeartureChannel(channels, configEnum, temperatureChanel);
		final double amplifierGAIN = 270;
		double voltage = adcTemperatureVal / amplifierGAIN;
		double temperature = grad.getTemperature(voltage * 1000.0, 18 + 273.25);
		return temperature;
	}

	private double readTempeartureChannel(short[] channels, ChannelConfig[] configEnum, final int temperatureChanel)
			throws UniDaqException {
		float[] inputValues = adcRead(channels, configEnum);
		double val = inputValues[temperatureChanel];
		return val;
	}

	private float[] adcRead(short[] channels, ChannelConfig[] configEnum) throws UniDaqException {
		ADC.clearAIBuffer();
		float[] inputValues = ADC.pollingAIScan(channels, configEnum, 1);
		// ADC.startAIScan(channels, configEnum, 400, 1);
		// float[] inputValues = ADC.getAIBuffer(channels.length);
		// ADC.stopAI();
		return inputValues;
	}

	public void setTemperature(int kelvins) {
		this.SETTED_VALUE = kelvins;
	}

}
