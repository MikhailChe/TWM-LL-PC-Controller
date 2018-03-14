package unidaq;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import unidaq.UniDaqLibrary.UCHAR;

/**
 * This file was autogenerated by
 * <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that
 * <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a
 * few opensource projects.</a>.<br>
 * For help, please visit
 * <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> ,
 * <a href="http://rococoa.dev.java.net/">Rococoa</a>, or
 * <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class IXUD_CARD_INFO extends Structure {
	/** Structure size */
	public int dwSize;
	/** Model Number */
	public int dwModelNo;
	/**
	 * CardID is update when calling the function each time.<br>
	 * for new cards, 0xFF=N/A<br>
	 * C type : UCHAR
	 */
	public UCHAR CardID;
	/**
	 * for new cards,1:S.E 2:D.I.F,0xFF=N/A<br>
	 * C type : UCHAR
	 */
	public UCHAR wSingleEnded;
	/** AI/AO Resolution High byte is AI, Low byte is AO */
	public short wAIOResolution;
	/** Number of AI channels(AD) */
	public short wAIChannels;
	/** Number of AO channels(DA) */
	public short wAOChannels;
	/** Number of DI ports */
	public short wDIPorts;
	/** Number of DO ports */
	public short wDOPorts;
	/** Number of DIO ports */
	public short wDIOPorts;
	/** The width is 8/16/32 bit. */
	public short wDIOPortWidth;
	/** Number of Timers/Counters */
	public short wCounterChannels;
	/** PCI-M512==>512, Units in KB. */
	public short wMemorySize;
	/**
	 * Reserver<br>
	 * C type : DWORD[6]
	 */
	public int[] dwReserved1 = new int[6];

	public IXUD_CARD_INFO() {
		super();
	}

	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("dwSize", "dwModelNo", "CardID", "wSingleEnded", "wAIOResolution", "wAIChannels",
				"wAOChannels", "wDIPorts", "wDOPorts", "wDIOPorts", "wDIOPortWidth", "wCounterChannels", "wMemorySize",
				"dwReserved1");
	}

	public IXUD_CARD_INFO(Pointer peer) {
		super(peer);
	}

	public static class ByReference extends IXUD_CARD_INFO implements Structure.ByReference {

	}

	public static class ByValue extends IXUD_CARD_INFO implements Structure.ByValue {

	}
}
