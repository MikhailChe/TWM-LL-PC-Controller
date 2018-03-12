package asdaservo;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

public class ServoController {
	static int crc_chk(final byte[] data) {

		int reg_crc = 0xFFFF;
		for (int i = 0; i < data.length; i++) {
			reg_crc ^= data[i];

			for (int j = 0; j < 8; j++) {

				if ((reg_crc & 0x01) == 0x01) { // XXX: weird bug, bogus
					reg_crc >>= 1;
					reg_crc ^= 0xA001;
				} else {
					reg_crc >>= 1;
				}
			}
		}
		return reg_crc;
	}

	public static void main(String[] args) {
		final byte[] data = { 0x01, 0x03, 0x01, 0x01, 0x00, 0x02 };
		int val = crc_chk(data);

		System.out.printf("%x%n%x%n", (val & 0xFF), ((val >> 8) & 0xFF));

		getAvailableSerialPorts().forEach(System.out::println);
	}

	SerialPort connect(String portname) throws Exception {
		CommPortIdentifier portID = CommPortIdentifier.getPortIdentifier(portname);
		if (portID.isCurrentlyOwned()) {
			System.err.println("Error: Port is currently in use");
		} else {
			CommPort commPort = portID.open(this.getClass().getName(), 2000);
			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
				return serialPort;
			}
		}
		return null;
	}

	public static Set<CommPortIdentifier> getAvailableSerialPorts() {
		Set<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
		Enumeration<?> thePorts = CommPortIdentifier.getPortIdentifiers();
		while (thePorts.hasMoreElements()) {
			CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			switch (com.getPortType()) {
			case CommPortIdentifier.PORT_SERIAL:

				if (!com.isCurrentlyOwned()) {
					h.add(com);
				}
				/*
				 * try { CommPort thePort = com.open("CommUtil", 50); thePort.close();
				 * h.add(com); } catch (PortInUseException e) { System.out.println("Port, " +
				 * com.getName() + ", is in use."); } catch (Exception e) {
				 * System.err.println("Failed to open port " + com.getName());
				 * e.printStackTrace(); }
				 */
			}
		}
		return h;
	}
}
