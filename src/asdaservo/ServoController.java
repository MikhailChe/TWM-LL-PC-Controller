package asdaservo;

import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class ServoController implements AutoCloseable {
	static int calculateCRC(final byte[] data) {

		int reg_crc = 0xFFFF;
		for (int i = 0; i < data.length; i++) {
			reg_crc ^= data[i] & 0xFF;

			for (int j = 0; j < 8; j++) {

				if ((reg_crc & 0x01) == 0x01) { // XXX: weird bug, bogus
					reg_crc = (reg_crc >> 1) ^ 0xA001;
				} else {
					reg_crc >>= 1;
				}
			}
		}
		return reg_crc;
	}

	public static boolean crcMatch(final byte[] arr) {
		int crc = calculateCRC(Arrays.copyOf(arr, arr.length - 2));
		int crc_l = crc & 0xFF;
		int crc_h = (crc >> 8) & 0xFF;

		if (crc_l == Byte.toUnsignedInt(arr[arr.length - 2]) && crc_h == Byte.toUnsignedInt(arr[arr.length - 1])) {
			System.out.println("CRC match");
			return true;
		}
		System.out.println("CRC not match: [" + crc_l + "," + crc_h + "] <---> ["
				+ Byte.toUnsignedInt(arr[arr.length - 2]) + "," + Byte.toUnsignedInt(arr[arr.length - 1]) + "]");

		return false;
	}

	public static byte[] addCRC(final byte[] input) {
		int crc = calculateCRC(input);

		byte[] output = Arrays.copyOf(input, input.length + 2);
		output[output.length - 2] = (byte) (crc & 0xFF);
		output[output.length - 1] = (byte) ((crc >> 8) & 0xFF);
		return output;
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

	final String ME;

	String portName;
	CommPortIdentifier portID = null;
	SerialPort port = null;
	boolean portOpened = false;

	public ServoController(String portName) throws NoSuchPortException, PortInUseException {
		this.portName = portName;
		this.ME = this.getClass().getName();
		open();
		close();
	}

	public synchronized void open() throws NoSuchPortException, PortInUseException {
		if (portID == null) {
			portID = CommPortIdentifier.getPortIdentifier(portName);
		}

		if (portID.isCurrentlyOwned()) {
			if (!ME.equals(portID.getCurrentOwner())) {
				throw new PortInUseException();
			}
		} else {
			CommPort comport = portID.open(ME, 2000);
			if (comport instanceof SerialPort) {
				port = (SerialPort) comport;
				try {
					port.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_2,
							SerialPort.PARITY_NONE);
					port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
				} catch (UnsupportedCommOperationException ignore) {
					ignore.printStackTrace();
				}
			}
		}
	}

	@Override
	public void close() {
		if (portID != null) {
			if (this.getClass().getName().equals(portID.getCurrentOwner())) {
				if (port != null) {
					port.close();
					port = null;
				}
			}
		}
	}

	private final byte ADR = 0x01;// адрес сервопривода
	// -----------------------------------//
	// private final byte CMD_READ = 0x03;// команда чтения
	private final byte CMD_WRITE = 0x06; // команда записи
	// -----------------------------//
	private final byte ADR_SPEED_1 = 0x01; // P1
	private final byte ADR_SPEED_2 = 0x09; // -09
	// -----------------------------//
	// private final byte ADR_SDI_HIGH = 0x03;// P3
	// private final byte ADR_SDI_LOW = 0x06;// 06
	// -----------------------------//
	private final byte ADR_SRON_HIGH = 0x02;// P2
	private final byte ADR_SRON_LOW = 0x33;// 51
	// -----------------------------//
	private final byte SRON_ON_HIGH = 0x00;// старт
	private final byte SRON_ON_LOW = 0x01;// SERVO ON
	// -----------------------------//
	private final byte SRON_OFF_HIGH = 0x00;// стоп
	private final byte SRON_OFF_LOW = 0x00;// SERVO OFF
	// -----------------------------//
	// -----------------------------//
	// private final byte START_1 = 0x00;// старт
	// private final byte START_2 = 0x05;// Первой скорости
	// -----------------------------//
	// private final byte STOPED_1 = 0x00;// стоп
	// private final byte STOPED_2 = 0x04;// Первой скорости

	public void start() {
		try {
			open();
			startStop(port.getOutputStream(), port.getInputStream(), true);
		} catch (NoSuchPortException | PortInUseException e) {
			// IGNORE. Handled at constructor
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void stop() {
		try {
			open();
			startStop(port.getOutputStream(), port.getInputStream(), false);
		} catch (NoSuchPortException | PortInUseException e) {
			// IGNORE. Handled at constructor
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}

	private synchronized boolean startStop(final OutputStream out, final InputStream in, final boolean startNOTstop) {
		byte[] pack;
		try {
			if (startNOTstop) {
				pack = addCRC(new byte[] { ADR, CMD_WRITE, ADR_SRON_HIGH, ADR_SRON_LOW, SRON_ON_HIGH, SRON_ON_LOW });
				out.write(pack);

				TimeUnit.MILLISECONDS.sleep(50);

				// pack = addCRC(new byte[] { ADR, CMD_WRITE, ADR_SDI_HIGH, ADR_SDI_LOW,
				// START_1, START_2 });
				// out.write(pack);
				//
				// TimeUnit.MILLISECONDS.sleep(50);
				//
				// pack = addCRC(new byte[] { ADR, CMD_WRITE, 0x04, 0x07, 0x00, 0x05 });
				// out.write(pack);

			} else {
				pack = addCRC(new byte[] { ADR, CMD_WRITE, ADR_SRON_HIGH, ADR_SRON_LOW, SRON_OFF_HIGH, SRON_OFF_LOW });
				out.write(pack);

				TimeUnit.MILLISECONDS.sleep(50);

			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void writeSpeed(final double frequencyHZ) {
		try {
			open();
			writeSpeed(port.getOutputStream(), port.getInputStream(), frequencyHZ);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchPortException | PortInUseException e) {
			// THIS SHOULD NEVER HAPPEN. HANDLED AT CONSTRUCTOR
			e.printStackTrace();
		} finally {
			close();
		}
	}

	private boolean writeSpeed(final OutputStream out, final InputStream in, final double frequency) {
		int rpm = (int) Math.round(frequency * 60.0 / 2.0); // divide by 2 becasuse of modulator shape

		byte speed_l = (byte) (rpm & 0xFF);
		byte speed_h = (byte) ((rpm >> 8) & 0xFF);

		byte[] pack = addCRC(new byte[] { ADR, CMD_WRITE, ADR_SPEED_1, ADR_SPEED_2, speed_h, speed_l });

		try {
			while (in.available() > 0) {
				in.read();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}

		try {
			out.write(pack);
			byte[] input = new byte[pack.length];
			final long TIMEOUT = 300;
			long timeoutStart = currentTimeMillis();
			while (in.available() < input.length) {
				try {
					TimeUnit.MILLISECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}
				if ((currentTimeMillis() - timeoutStart) > TIMEOUT) {
					System.err.println("TIMEOUT writing speed");
					return false;
				}
			}

			in.read(input);
			// int settedSpeed = (Byte.toUnsignedInt(input[4]) << 8) |
			// (Byte.toUnsignedInt(input[5]));
			// if (settedSpeed != rpm) {
			// crcMatch(pack);
			// crcMatch(input);
			// return false;
			// }
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
