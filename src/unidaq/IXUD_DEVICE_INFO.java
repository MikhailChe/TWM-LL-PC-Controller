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
public class IXUD_DEVICE_INFO extends Structure {
	/** Structure Size */
	public int dwSize;
	/** Vendor ID */
	public short wVendorID;
	/** Device ID */
	public short wDeviceID;
	/** Sub Vendor ID */
	public short wSubVendorID;
	/** Sub Device ID */
	public short wSubDeviceID;
	/**
	 * PCI Bar 0 ~ 5<br>
	 * C type : DWORD[6]
	 */
	public int[] dwBAR = new int[6];
	/**
	 * the PCI Bus No. in mainboard<br>
	 * C type : UCHAR
	 */
	public UCHAR BusNo;
	/**
	 * the Slot No. in PCI bus<br>
	 * C type : UCHAR
	 */
	public UCHAR DevNo;
	/**
	 * IRQ No.<br>
	 * C type : UCHAR
	 */
	public UCHAR IRQ;
	/**
	 * Sub Aux for Tiger series,0xFF=N/A<br>
	 * C type : UCHAR
	 */
	public UCHAR Aux;
	/**
	 * PCI Bar Virtual Address(For MMIO)<br>
	 * C type : DWORD[6]
	 */
	public int[] dwBarVirtualAddress = new int[6];

	public IXUD_DEVICE_INFO() {
		super();
	}

	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("dwSize", "wVendorID", "wDeviceID", "wSubVendorID", "wSubDeviceID", "dwBAR", "BusNo",
				"DevNo", "IRQ", "Aux", "dwBarVirtualAddress");
	}

	public IXUD_DEVICE_INFO(Pointer peer) {
		super(peer);
	}

	public static class ByReference extends IXUD_DEVICE_INFO implements Structure.ByReference {

	}

	public static class ByValue extends IXUD_DEVICE_INFO implements Structure.ByValue {

	}
}
