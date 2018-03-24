package starthere;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import asdaservo.ServoController;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import regulator.PID;
import regulator.SlopeLimiter;
import starthere.ControlLoop.ReturnStatus;
import unidaq.UniDAQLib;
import unidaq.UniDAQLib.ADC;
import unidaq.UniDAQLib.DAC;
import unidaq.UniDAQManager;
import unidaq.UniDaqException;

public class Measurement implements Runnable {
	UniDAQLib boards = null;

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
				running.interrupt();
				try {
					TimeUnit.SECONDS.timedJoin(running, 10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Measurement m = new Measurement(settings);
			running = new Thread(m);
			running.start();
		} else {
			if (running != null) {
				running.interrupt();
				try {
					TimeUnit.SECONDS.timedJoin(running, 10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public Measurement(SettingsHolder settings) {
		this.settings = settings;

		ADC = UniDAQManager.instance().getADC();
		DAC = UniDAQManager.instance().getDAC();

		outputSlopeLimit = new SlopeLimiter(1, 0);
		regulator = new PID(.04, 1 / 100.0, .1).setProportionalBounds(-5, 5).setIntegralBounds(-5, 5)
				.setDifferentialBounds(-.5, .5);

		ctrlLoop = new ControlLoop(settings.getInitialTemperature(), 3, TimeUnit.SECONDS, 5, regulator,
				outputSlopeLimit, ADC, DAC);
	}

	@Override
	public void run() {
		try {

			boards = UniDAQLib.instance();

			int kelvins = settings.getInitialTemperature();
			boolean goingUp = settings.isInitiallyUp();

			try (ServoController motor = new ServoController(settings.getSerialPortName())) {
				motor.start();
				motor.writeSpeed(5);
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
					controlLoopReturnStatus = ctrlLoop.call();
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
				if (controlLoopReturnStatus == null || ReturnStatus.INTERRUPT.equals(controlLoopReturnStatus)
						|| ReturnStatus.CRITICAL.equals(controlLoopReturnStatus)) {

					break;
				}
				// *****MEASURE ****///
				try {
					StartHere.setupReadAndPrintExperiment(settings, ADC, new short[] { 0, 2, 4, 6 },
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
				}
				if (kelvins < settings.getMinTemperature()) {
					goingUp = true;
				}
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
			}
		}
	}
}
