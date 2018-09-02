package starthere;

import static starthere.StartHere.Acquisitor;
import static starthere.widgets.log.GuiLogger.log;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import asdaservo.ServoController;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import regulator.PID;
import regulator.SlopeLimiter;
import starthere.ControlLoop.ReturnStatus;
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

	public synchronized static boolean measurementControl(SettingsHolder settings, boolean start) {
		if (start) {
			if (tryToStopMeasurementsIfRunning(TimeUnit.SECONDS, 60)) {
				CtrlAndMeasurementCycle m = new CtrlAndMeasurementCycle(settings);
				running = new Thread(m);
				log().println("Starting new measurements");
				running.start();
				return true;
			}
			log().println("Thread is still running, so I'm not starting a new one");
			return false;
		}
		if (!tryToStopMeasurementsIfRunning(TimeUnit.SECONDS, 1)) {
			for (int i = 0; i < 3; i++) {
				try {
					StartHere.DAC.writeAOVoltage(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (tryToStopMeasurementsIfRunning(TimeUnit.SECONDS, 60)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public static boolean isRunning() {
		if (running == null)
			return false;
		if (running.isAlive())
			return true;
		return false;
	}

	private synchronized static boolean tryToStopMeasurementsIfRunning(TimeUnit timeunit, int timeout) {
		if (!isRunning()) {
			log().println("No need to stop, none of " + CtrlAndMeasurementCycle.class.getName() + " is running");
			return true;
		}
		running.interrupt();
		log().println("Sent and interrupt signal to measurement loop, waiting...");
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

	public static void blockAndStopAll() {
		do {
			log().println("PANIC! STOPPING REGULATOR");
			try {
				StartHere.DAC.writeAOVoltage(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (!tryToStopMeasurementsIfRunning(TimeUnit.MILLISECONDS, 100));
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

			measureLoop: while (!Thread.interrupted()) {
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
					log().println("Control loop returned state " + controlLoopReturnStatus);
					break;
				}
				for (int i = 0; i < 2; i++) {
					if (Thread.interrupted()) {
						break measureLoop;
					}
					// *****MEASURE ****///
					// TODO: number of periods should come from SettingsHolder

					final int numberOfPeriods = settings.getNumberOfPeriodsPerMeasure();
					log().println("Measuring channels for " + numberOfPeriods + " periods");

					final AtomicInteger acqusitionStatus = new AtomicInteger(0);
					new Thread(() -> {
						log().println("Running acquisition service");
						try {
							Acquisitor.setupReadAndPrintExperiment(new short[] { 0, 2, 4, 6 },
									settings.getExperimentFrequency(), numberOfPeriods);
						} catch (FileNotFoundException | NoSuchPortException | PortInUseException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
							acqusitionStatus.set(-1);
						}
						acqusitionStatus.set(1);
					}).start();

					while (acqusitionStatus.get() == 0) {
						if (Thread.interrupted()) {
							break measureLoop;
						}
						try {
							TimeUnit.MILLISECONDS.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break measureLoop;
						}
					}
					log().println("Done capturing data. Regulating...");

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
			try {
				DAC.writeAOVoltage(0);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (UniDaqException e) {
			e.printStackTrace();
		}
	}
}
