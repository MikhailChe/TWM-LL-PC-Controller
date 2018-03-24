package unidaq;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class UniDAQLib implements AutoCloseable {

	private static BoardState state = BoardState.UNDEFINED;
	private static UniDAQLib INSTANCE;

	public synchronized static UniDAQLib instance() throws UniDaqException {
		if (INSTANCE == null) {
			synchronized (UniDAQLib.class) {
				if (INSTANCE == null) {
					INSTANCE = new UniDAQLib();
				}
			}
		}
		if (state.equals(BoardState.CLOSED) || state.equals(BoardState.UNDEFINED)) {
			INSTANCE.open();
		}
		return INSTANCE;
	}

	private UniDAQLib() throws UniDaqException {
		open();
	}

	short totalBoards = 0;

	private synchronized void open() throws UniDaqException {
		IntBuffer dllVersion = IntBuffer.allocate(1);
		short error = UniDaqLibrary.Ixud_GetDllVersion(dllVersion);
		System.out.println("DLL VERSION:" + dllVersion.get(0));
		if (error > 0) {
			throw new UniDaqException(error);
		}

		System.out.println("Initializing driver");
		ShortBuffer totalBoards1 = ShortBuffer.allocate(1);
		error = UniDaqLibrary.Ixud_DriverInit(totalBoards1);
		System.out.println("Total number of boards: " + totalBoards1.get(0));
		if (error > 0) {
			throw new UniDaqException(error);
		}
		this.totalBoards = totalBoards1.get(0);
		state = BoardState.OPENED;
	}

	@Override
	public synchronized void close() throws UniDaqException {
		// System.out.println("Closing driver");
		// UniDaqLibrary.Ixud_DriverClose();
		// state = BoardState.CLOSED;
	}

	public short getTotalBoards() {
		return totalBoards;
	}

	public ADC getADC(int boardNumber) {
		if (boardNumber >= totalBoards) {
			throw new IllegalArgumentException(
					"Parameter boardNumber (" + boardNumber + ") is out of range (" + (totalBoards - 1) + ")");
		}
		if (boardNumber < 0) {
			throw new IllegalArgumentException("Parameter boardNumber < 0");
		}
		return new ADC(boardNumber);
	}

	public DAC getDAC(int boardNumber) {
		if (boardNumber >= totalBoards) {
			throw new IllegalArgumentException(
					"Parameter boardNumber (" + boardNumber + ") is out of range (" + (totalBoards - 1) + ")");
		}
		if (boardNumber < 0) {
			throw new IllegalArgumentException("Parameter boardNumber < 0");
		}
		return new DAC(boardNumber);
	}

	public class ADC {
		private short boardNumber;

		ADC(int boardNumber) {
			this.boardNumber = (short) boardNumber;
			configAI(0, (short) 1);
		}

		public void configAI(int bufferSize, short cardType) {
			UniDaqLibrary.Ixud_ConfigAI(boardNumber, (short) 0, bufferSize, cardType, (short) 0);
		}

		public void clearAIBuffer() {
			UniDaqLibrary.Ixud_ClearAIBuffer(boardNumber);
		}

		public float[] pollingAIScan(short[] channels, short[] channelConfig, int dataCountPerChannel)
				throws UniDaqException {
			if (channels.length != channelConfig.length) {
				throw new IllegalArgumentException("Channel configuration is not specified for all channels");
			}
			if (channels.length >= Short.MAX_VALUE) {
				throw new IllegalArgumentException("Cannot handle that much channels");
			}
			float[] values = new float[channels.length * dataCountPerChannel];
			short error = UniDaqLibrary.Ixud_PollingAIScan(boardNumber, (short) channels.length,
					ShortBuffer.wrap(channels), ShortBuffer.wrap(channelConfig), dataCountPerChannel,
					FloatBuffer.wrap(values));
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
		private void startAIScan_LL(short[] channels, short[] channelConfig, float samplingRate,
				int dataCountPerChannel) throws UniDaqException {
			short error = UniDaqLibrary.Ixud_StartAIScan(boardNumber, (short) channels.length,
					ShortBuffer.wrap(channels), ShortBuffer.wrap(channelConfig), samplingRate, dataCountPerChannel);
			if (error > 0) {
				throw new UniDaqException(error);
			}
		}

		public void startAIScan(short[] channels, ChannelConfig[] channelConfig, float samplingRate)
				throws UniDaqException {
			startAIScan(channels, channelConfig, samplingRate, 0);
		}

		public void startAIScan(short[] channels, ChannelConfig[] channelConfig, float samplingRate,
				int dataCountPerChannel) throws UniDaqException {
			if (boardNumber < 0) {
				throw new IllegalArgumentException(
						"I don't believe this board event exist (boradNumber = " + boardNumber + ")");
			}
			if (channels.length != channelConfig.length) {
				throw new IllegalArgumentException("Channel configuration is not specified for all channels");
			}
			if (channels.length >= Short.MAX_VALUE) {
				throw new IllegalArgumentException("Cannot handle that much channels");
			}
			if (boardNumber >= Short.MAX_VALUE) {
				throw new IllegalArgumentException(
						"I don't believe this board event exist (boradNumber = " + boardNumber + ")");
			}
			short[] config = new short[channelConfig.length];
			for (int i = 0; i < channelConfig.length; i++) {
				config[i] = channelConfig[i].getConfigValue();
			}
			startAIScan_LL(channels, config, samplingRate, dataCountPerChannel);
		}

		public float[] getAIBuffer(int dataCount) throws UniDaqException {
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

		public void stopAI() throws UniDaqException {
			short error = UniDaqLibrary.Ixud_StopAI(boardNumber);
			if (error > 0) {
				throw new UniDaqException(error);
			}
		}

		public class BufferStatus {
			public short bufferStatus;
			public int dataCount;
		}

		public BufferStatus getBufferStatus() throws UniDaqException {
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

	}

	public class DAC {
		private short boardNumber;

		DAC(int boardNumber) {
			this.boardNumber = (short) boardNumber;
			try {
				configAO((short) 0, UniDaqLibrary.IXUD_AO_BI_5V);
			} catch (UniDaqException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 
		 * @param boardNumber
		 * @param channel
		 * @param cfgCode
		 *            IXUD_AO_BI_5V or IXUD_AO_BI_10V
		 * @throws UniDaqException
		 */
		private void configAO(short channel, short cfgCode) throws UniDaqException {
			short error = UniDaqLibrary.Ixud_ConfigAO(boardNumber, channel, cfgCode);
			if (error > 0) {
				throw new UniDaqException(error);
			}
		}

		private void writeAOVoltage_LL(float value) throws UniDaqException {
			short error = UniDaqLibrary.Ixud_WriteAOVoltage(boardNumber, (short) 0, value);
			if (error > 0) {
				throw new UniDaqException(error);
			}
		}

		public void writeAOVoltage(double value) throws UniDaqException {
			writeAOVoltage_LL((float) value);

		}
	}

	private enum BoardState {
		OPENED, CLOSED, UNDEFINED
	}

}
