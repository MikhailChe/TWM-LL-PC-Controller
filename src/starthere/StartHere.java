package starthere;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JOptionPane;

import acquisition.Acquisitor;
import unidaq.UniDAQLib;
import unidaq.UniDAQLib.ADC;
import unidaq.UniDAQLib.DAC;

public class StartHere {

	public static ADC ADC;
	public static DAC DAC;

	public static Acquisitor Acquisitor;

	public static void main(String[] args) {
		try (UniDAQLib unidaq = UniDAQLib.instance()) {
			ADC = unidaq.getADC(0);
			DAC = unidaq.getDAC(0);

			MainWindow window = new MainWindow();
			Acquisitor = new Acquisitor(window);

			window.pack();
			window.setMinimumSize(window.getSize());
			window.setLocationRelativeTo(null);
			window.setVisible(true);

			while (window.isVisible()) {
				Thread.yield();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			window.dispose();
			if (DAC != null)
				DAC.writeAOVoltage(0);
		} catch (Throwable e) {
			e.printStackTrace();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			e.printStackTrace(ps);

			JOptionPane.showMessageDialog(null, new String(baos.toByteArray()), "Fatal error",
					JOptionPane.ERROR_MESSAGE);
		}

	}

	@FunctionalInterface
	interface TriFunction<A, B, O> {
		O apply(O o, A a, B b);
	}

}
