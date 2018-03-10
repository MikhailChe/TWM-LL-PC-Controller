package starthere;

import unidaq.UniDAQLib;
import unidaq.UniDaqException;
import unidaq.UniDaqLibrary;

public class StartHere {

	public static void main(String[] args) {
		try (UniDAQLib ADC = new UniDAQLib()) {
			System.out.println("SUCCESS + " + ADC.getTotalBoards());

			ADC.startAIScan(0, new short[] { 0, 1, 2 },
					new short[] { UniDaqLibrary.IXUD_BI_10V, UniDaqLibrary.IXUD_BI_10V, UniDaqLibrary.IXUD_BI_10V },
					1000, 1000);

		} catch (UniDaqException e) {
			e.printStackTrace();
		}
	}

}
