package starthere;

import static starthere.StartHere.Acquisitor;
import static starthere.widgets.log.GuiLogger.log;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import asdaservo.ServoController;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import regulator.PID;
import regulator.SlopeLimiter;
import starthere.ControlLoop.ReturnStatus;
import starthere.widgets.log.GuiLogger;
import unidaq.UniDAQLib.ADC;
import unidaq.UniDAQLib.DAC;
import unidaq.UniDaqException;

public class CtrlAndMeasurementCycle implements Runnable {
	final SettingsHolder settings;
	final ControlLoop ctrlLoop;

	final ADC ADC;
	final DAC DAC;
	final PID regulator;
	final SlopeLimiter outputSlopeLimit;

	static Thread running = null;

	public synchronized static void measurementControl(SettingsHolder settings, boolean start) {
		if (start) {
			tryToStopMeasurementsIfRunning(TimeUnit.SECONDS, 10);
			CtrlAndMeasurementCycle m = new CtrlAndMeasurementCycle(settings);
			running = new Thread(m);
			log().println("Starting new measurements");
			running.start();
		} else {
			tryToStopMeasurementsIfRunning(TimeUnit.SECONDS, 10);
		}
	}

	private synchronized static boolean tryToStopMeasurementsIfRunning(TimeUnit timeunit, int timeout) {
		if (running == null)
			return true;
		if (running.isAlive()) {
			running.interrupt();
			log().println("Waiting for measurements to finish");
			try {
				timeunit.timedJoin(running, timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (running.isAlive()) {
				log().println("No success. Thread is still running");
				return false;
			}
			log().println("Measurements finished");
			return true;
		}
		log().println("Measurements finished");
		return true;
	}

	public synchronized static void manualMeasurements(SettingsHolder settings) {
		// TODO: to be implemented
		throw new UnsupportedOperationException("Not yet implemented" + settings.toString());
	}

	public CtrlAndMeasurementCycle(SettingsHolder settings) {
		this.settings = settings;

		ADC = StartHere.ADC;
		DAC = StartHere.DAC;

		outputSlopeLimit = new SlopeLimiter(.5, 0);
		regulator = new PID(.02, 1 / 200.0, .1).setProportionalBounds(-5, 5).setIntegralBounds(-.1, 5)
				.setDifferentialBounds(-.5, .5);

		ctrlLoop = new ControlLoop(settings, regulator, outputSlopeLimit, ADC, DAC);
	}

	@Override
	public void run() {
		try {
			int kelvins = settings.getInitialTemperature();
			boolean goingUp = settings.isInitiallyUp();

			try (ServoController motor = new ServoController(settings.getSerialPortName())) {
				motor.start();
				motor.writeSpeed(settings.getExperimentFrequency());
			} catch (NoSuchPortException e) {
				e.printStackTrace();
			} catch (PortInUseException e) {
				e.printStackTrace();
			}

			while (!Thread.interrupted()) {
				regulator.resetTimings();
				outputSlopeLimit.resetTiming();
				ctrlLoop.setTemperature(kelvins);
				ReturnStatus controlLoopReturnStatus = null;
				try {
					log().println("Waiting for " + kelvins + "K (temperature)");
					controlLoopReturnStatus = ctrlLoop.call();
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
				if (controlLoopReturnStatus == null || ReturnStatus.INTERRUPT.equals(controlLoopReturnStatus)
						|| ReturnStatus.CRITICAL.equals(controlLoopReturnStatus)) {
					GuiLogger.log().println("Control loop returned state " + controlLoopReturnStatus);
					break;
				}
				for (int i = 0; i < 2; i++) {
					// *****MEASURE ****///
					// TODO: number of periods should come from SettingsHolder
					final int numberOfPeriods = settings.getNumberOfPeriodsPerMeasure();
					log().println("Measuring channels for " + numberOfPeriods + " periods");
					try {
						Acquisitor.setupReadAndPrintExperiment(new short[] { 0, 2, 4, 6 },
								settings.getExperimentFrequency(), numberOfPeriods);
					} catch (FileNotFoundException | NoSuchPortException | PortInUseException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}

				if (goingUp) {
					kelvins += settings.getTemperatureStep();
				} else {
					kelvins -= settings.getTemperatureStep();
				}

				if (kelvins >= settings.getMaxTemeprature()) {
					goingUp = false;
					log().println("Lowering temperature");
				}
				if (kelvins <= settings.getMinTemperature()) {
					goingUp = true;
					log().println("Rising temperature");
				}
			}
			log().println("Measurements interrupted");

		} catch (UniDaqException e) {
			e.printStackTrace();
		}
	}
}
