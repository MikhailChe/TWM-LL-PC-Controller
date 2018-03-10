package unidaq;

import static unidaq.UniDaqLibrary.IXUD_BI_10V;
import static unidaq.UniDaqLibrary.IXUD_BI_1V25;
import static unidaq.UniDaqLibrary.IXUD_BI_2V5;
import static unidaq.UniDaqLibrary.IXUD_BI_5V;

public enum ChannelConfig {
	BI_10V(IXUD_BI_10V), BI_5V(IXUD_BI_5V), BI_2V5(IXUD_BI_2V5), BI_1V25(IXUD_BI_1V25);

	ChannelConfig(short config) {
		this.configValue = config;
	}

	short configValue;

	public short getConfigValue() {
		return configValue;
	}
}
