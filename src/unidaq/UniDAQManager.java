package unidaq;

import unidaq.UniDAQLib.ADC;
import unidaq.UniDAQLib.DAC;

public class UniDAQManager {

	final UniDAQLib lib;
	final ADC ADC;
	final DAC DAC;

	private volatile static UniDAQManager INSTANCE;

	private UniDAQManager() throws UniDaqException {
		lib = UniDAQLib.instance();
		ADC = lib.getADC(0);
		DAC = lib.getDAC(0);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				lib.close();
			} catch (UniDaqException e) {
				e.printStackTrace();
			}
		}));

	}

	public static UniDAQManager instance() {
		if (INSTANCE == null) {
			synchronized (UniDAQManager.class) {
				if (INSTANCE == null) {
					try {
						INSTANCE = new UniDAQManager();
					} catch (UniDaqException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return INSTANCE;
	}

	public ADC getADC() {
		return ADC;
	}

	public DAC getDAC() {
		return DAC;
	}

}
