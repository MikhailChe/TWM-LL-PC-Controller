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

public class Measurement implements Runnable {
	final SettingsHolder settings;
	final ControlLoop ctrlLoop;

	final ADC ADC;
	final DAC DAC;
	final PID regulator;
	final SlopeLimiter outputSlopeLimit;

	static Thread running = null;

	public synchronized static void measurementControl(SettingsHolder settings, boolean start) {
		if (start) {
			if (running != null) {
				if (running.isAlive()) {
					running.interrupt();
					log().println("Waiting for measurements to finish");
					try {
						TimeUnit.SECONDS.timedJoin(running, 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (running.isAlive()) {
						log().println("No success. Thread is still running");
					} else {
						log().println("Measurements finished");
					}
				} else {
					log().println("Measurements finished");
				}
			}
			Measurement m = new Measurement(settings);
			running = new Thread(m);
			log().println("Starting new measurements");
			running.start();
		} else {
			if (running != null) {
				if (running.isAlive()) {
					running.interrupt();
					log().println("Waiting for measurements to finish");
					try {
						TimeUnit.SECONDS.timedJoin(running, 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (running.isAlive()) {
						log().println("No success. Thread is still running");
					} else {
						log().println("Measurements finished");
					}
				} else {
					log().println("Measurements finished");
				}
			}

		}
	}

	public Measurement(SettingsHolder settings) {
		this.settings = settings;

		ADC = StartHere.ADC;
		DAC = StartHere.DAC;

		outputSlopeLimit = new SlopeLimiter(1, 0);
		regulator = new PID(.04, 1 / 100.0, .1).setProportionalBounds(-5, 5).setIntegralBounds(-5, 5)
				.setDifferentialBounds(-.5, .5);

		ctrlLoop = new ControlLoop(settings.getInitialTemperature(), 3, TimeUnit.SECONDS, 5, regulator,
				outputSlopeLimit, ADC, DAC);
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
				// *****MEASURE ****///
				// TODO: number of periods should come from SettingsHolder
				log().println("Measuring channels for 64 periods");
				try {
					Acquisitor.setupReadAndPrintExperiment(new short[] { 0, 2, 4, 6 },
							settings.getExperimentFrequency(), 64);
				} catch (FileNotFoundException | NoSuchPortException | PortInUseException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}

				if (goingUp) {
					kelvins += settings.getTemperatureStep();
				} else {
					kelvins -= settings.getTemperatureStep();
				}

				if (kelvins > settings.getMaxTemeprature()) {
					goingUp = false;
					log().println("Lowering temperature");
				}
				if (kelvins < settings.getMinTemperature()) {
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
