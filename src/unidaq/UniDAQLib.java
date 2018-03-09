package unidaq;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class UniDAQLib implements AutoCloseable {

	private static UniDAQFace INSTANCE = (UniDAQFace) Native.loadLibrary("UniDAQ", UniDAQFace.class);

	short totalBoards = 0;

	public short getTotalBoards() {
		return totalBoards;
	}

	public UniDAQLib() throws Exception {
		short[] totalBoards = new short[1];
		System.out.println("Initializing driver");
		short error = INSTANCE.Ixud_DriverInit(totalBoards);
		if (error > 0) {
			throw new Exception(error + "");
		}
		this.totalBoards = totalBoards[0];
	}

	@Override
	public void close() throws Exception {
		System.out.println("Closing driver");
		INSTANCE.Ixud_DriverClose();
	}

}

interface UniDAQFace extends Library {

	short Ixud_DriverInit(short[] wTotalBoards);

	// short Ixud_GetCardInfo(short wBoardNo, PIXUD_DEVICE_INFO
	// sDevInfo,PIXUD_CARD_INFO sCardInfo, char *szModelName);
	short Ixud_DriverClose();
}
