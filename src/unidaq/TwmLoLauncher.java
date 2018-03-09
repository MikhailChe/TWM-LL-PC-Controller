package unidaq;

public class TwmLoLauncher {

	public static void main(String[] args) {
		try (UniDAQLib ADC = new UniDAQLib()) {
			System.out.println("SUCCESS + " + ADC.getTotalBoards());

		} catch (UniDaqException e) {
			e.printStackTrace();
		}
	}

}
