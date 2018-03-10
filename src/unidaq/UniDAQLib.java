package unidaq;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class UniDAQLib implements AutoCloseable {

	short totalBoards = 0;

	public short getTotalBoards() {
		return totalBoards;
	}

	public UniDAQLib() throws UniDaqException {
		IntBuffer dllVersion = IntBuffer.allocate(1);
		short error = UniDaqLibrary.Ixud_GetDllVersion(dllVersion);
		System.out.println("DLL VERSION:" + dllVersion.get(0));
		if (error > 0) {
			throw new UniDaqException(error);
		}

		System.out.println("Initializing driver");
		ShortBuffer totalBoards = ShortBuffer.allocate(1);
		error = UniDaqLibrary.Ixud_DriverInit(totalBoards);
		System.out.println("Total number of boards: " + totalBoards.get(0));
		if (error > 0) {
			throw new UniDaqException(error);
		}
		this.totalBoards = totalBoards.get(0);
	}

	public void configAI(short boardNumber, int bufferSize, short cardType) {
		UniDaqLibrary.Ixud_ConfigAI(boardNumber, (short) 0, bufferSize, cardType, (short) 0);
	}

	public void clearAIBuffer(short boardNumber) {
		UniDaqLibrary.Ixud_ClearAIBuffer(boardNumber);
	}

	public float[] pollingAIScan(short boardNumber, short[] channels, short[] channelConfig, int dataCountPerChannel)
			throws UniDaqException {
		if (channels.length != channelConfig.length) {
			throw new IllegalArgumentException("Channel configuration is not specified for all channels");
		}
		if (channels.length >= Short.MAX_VALUE) {
			throw new IllegalArgumentException("Cannot handle that much channels");
		}
		float[] values = new float[channels.length * dataCountPerChannel];
		short error = UniDaqLibrary.Ixud_PollingAIScan(boardNumber, (short) channels.length, ShortBuffer.wrap(channels),
				ShortBuffer.wrap(channelConfig), dataCountPerChannel, FloatBuffer.wrap(values));
		if (error > 0) {
			throw new UniDaqException(error);
		}
		return values;
	}

	/**
	 * 
	 * @param boardNumber
	 * @param channels
	 * @param channelConfig
	 * @param samplingRate
	 * @param dataCountPerChannel
	 *            =0 for continuous mode (must call stopAI to stop);
	 * @throws UniDaqException
	 */
	public void startAIScan(short boardNumber, short[] channels, short[] channelConfig, float samplingRate,
			int dataCountPerChannel) throws UniDaqException {
		if (boardNumber <= 0) {
			throw new IllegalArgumentException(
					"I don't believe this board event exist (boradNumber = " + boardNumber + ")");
		}
		if (channels.length != channelConfig.length) {
			throw new IllegalArgumentException("Channel configuration is not specified for all channels");
		}
		if (channels.length >= Short.MAX_VALUE) {
			throw new IllegalArgumentException("Cannot handle that much channels");
		}

		short error = UniDaqLibrary.Ixud_StartAIScan(boardNumber, (short) channels.length, ShortBuffer.wrap(channels),
				ShortBuffer.wrap(channelConfig), samplingRate, dataCountPerChannel);
		if (error > 0) {
			throw new UniDaqException(error);
		}
	}

	public void startAIScan(int boardNumber, short[] channels, short[] channelConfig, float samplingRate,
			int dataCountPerChannel) throws UniDaqException {
		if (boardNumber >= Short.MAX_VALUE) {
			throw new IllegalArgumentException(
					"I don't believe this board event exist (boradNumber = " + boardNumber + ")");
		}
		startAIScan((short) boardNumber, channels, channelConfig, samplingRate, dataCountPerChannel);
	}

	public float[] getAIBuffer(short boardNumber, int dataCount) throws UniDaqException {
		if (dataCount <= 0) {
			throw new IllegalArgumentException(
					"Cannot read negative amount of values. I mean ... HOW? Should I read from past?");
		}

		FloatBuffer buffer = FloatBuffer.allocate(dataCount);
		short error = UniDaqLibrary.Ixud_GetAIBuffer(boardNumber, dataCount, buffer);
		if (error > 0) {
			throw new UniDaqException(error);
		}

		return buffer.array();
	}

	public void stopAI(short boardNumber) throws UniDaqException {
		short error = UniDaqLibrary.Ixud_StopAI(boardNumber);
		if (error > 0) {
			throw new UniDaqException(error);
		}
	}

	class BufferStatus {
		short bufferStatus;
		int dataCount;
	}

	public BufferStatus getBufferStatus(short boardNumber) throws UniDaqException {
		ShortBuffer bufferStatus = ShortBuffer.allocate(1);
		IntBuffer dataCount = IntBuffer.allocate(1);
		short error = UniDaqLibrary.Ixud_GetBufferStatus(boardNumber, bufferStatus, dataCount);
		if (error > 0) {
			throw new UniDaqException(error);
		}
		BufferStatus bstat = new BufferStatus();
		bstat.bufferStatus = bufferStatus.get();
		bstat.dataCount = dataCount.get();
		return bstat;
	}

	@Override
	public void close() throws UniDaqException {
		System.out.println("Closing driver");
		UniDaqLibrary.Ixud_DriverClose();
	}

}
