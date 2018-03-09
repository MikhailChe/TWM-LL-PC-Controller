package unidaq;

public class UniDaqException extends Exception {

	private static final long serialVersionUID = -7468665944978099617L;

	public static UniDaqException get(int code) {
		return new UniDaqException(code);
	}

	private int code;

	public UniDaqException(int code) {
		super(UniDaqReturnValue.codeToString(code).toString());
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}

enum UniDaqReturnValue {
	// Return code
	Ixud_NoErr(codeToString(0)), // Correct
	Ixud_OpenDriverErr(codeToString(1)), // Open driver error
	Ixud_PnPDriverErr(codeToString(2)), // Plug & Play error
	Ixud_DriverNoOpen(codeToString(3)), // The driver was not open
	Ixud_GetDriverVersionErr(codeToString(4)), // Receive driver version error
	Ixud_ExceedBoardNumber(codeToString(5)), // Board number error
	Ixud_FindBoardErr(codeToString(6)), // No board found
	Ixud_BoardMappingErr(codeToString(7)), // Board Mapping error
	Ixud_DIOModesErr(codeToString(8)), // Digital input/output mode setting error
	Ixud_InvalidAddress(codeToString(9)), // Invalid address
	Ixud_InvalidSize(codeToString(10)), // Invalid size
	Ixud_InvalidPortNumber(codeToString(11)), // Invalid port number
	Ixud_UnSupportedModel(codeToString(12)), // This board model is not supported
	Ixud_UnSupportedFun(codeToString(13)), // This function is not supported
	Ixud_InvalidChannelNumber(codeToString(14)), // Invalid channel number
	Ixud_InvalidValue(codeToString(15)), // Invalid value
	Ixud_InvalidMode(codeToString(16)), // Invalid mode
	Ixud_GetAIStatusTimeOut(codeToString(17)), // A timeout occurred while receiving the status of the analog input
	Ixud_TimeOutErr(codeToString(18)), // Timeout error
	Ixud_CfgCodeIndexErr(codeToString(19)), // A compatible configuration code table index could not be found
	Ixud_ADCCTLTimeoutErr(codeToString(20)), // ADC controller a timeout error
	Ixud_FindPCIIndexErr(codeToString(21)), // A compatible PCI table index value could not be found
	Ixud_InvalidSetting(codeToString(22)), // Invalid setting value
	Ixud_AllocateMemErr(codeToString(23)), // Error while allocating the memory space
	Ixud_InstallEventErr(codeToString(24)), // Error while installing the interrupt event
	Ixud_InstallIrqErr(codeToString(25)), // Error while installing the interrupt IRQ
	Ixud_RemoveIrqErr(codeToString(26)), // Error while removing the interrupt IRQ
	Ixud_ClearIntCountErr(codeToString(27)), // Error while the clear interrupt count
	Ixud_GetSysBufferErr(codeToString(28)), // Error while retrieving the system buffer
	Ixud_CreateEventErr(codeToString(29)), // Error while create the event
	Ixud_UnSupportedResolution(codeToString(30)), // Resolution not supported
	Ixud_CreateThreadErr(codeToString(31)), // Error while create the thread
	Ixud_ThreadTimeOutErr(codeToString(32)), // Thread timeout error
	Ixud_FIFOOverFlowErr(codeToString(33)), // FIFO overflow error
	Ixud_FIFOTimeOutErr(codeToString(34)), // FIFO timeout error
	Ixud_GetIntInstStatus(codeToString(35)), // Retrieves the status of the interrupt installation
	Ixud_GetBufStatus(codeToString(36)), // Retrieves the status of the system buffer
	Ixud_SetBufCountErr(codeToString(37)), // Error while setting the buffer count
	Ixud_SetBufInfoErr(codeToString(38)), // Error while setting the buffer data
	Ixud_FindCardIDErr(codeToString(39)), // Card ID code could not be found
	Ixud_EventThreadErr(codeToString(40)), // Event Thread error
	Ixud_AutoCreateEventErr(codeToString(41)), // Error while automatically creating an event
	Ixud_RegThreadErr(codeToString(42)), // Register Thread error
	Ixud_SearchEventErr(codeToString(43)), // Search Event error
	Ixud_FifoResetErr(codeToString(44)), // Error while resetting the FIFO
	Ixud_InvalidBlock(codeToString(45)), // Invalid EEPROM block
	Ixud_InvalidAddr(codeToString(46)), // Invalid EEPROM address
	Ixud_AcqireSpinLock(codeToString(47)), // Error while acquiring spin lock
	Ixud_ReleaseSpinLock(codeToString(48)), // Error while releasing spin lock
	Ixud_SetControlErr(codeToString(49)), // Analog input setting error
	Ixud_InvalidChannels(codeToString(50)), // Invalid channel number
	Ixud_SearchCardErr(codeToString(51)), // Invalid model number
	Ixud_SetMapAddressErr(codeToString(52)), // Error while setting the mapping address
	Ixud_ReleaseMapAddressErr(codeToString(53)), // Error while releasing the mapping address
	Ixud_InvalidOffset(codeToString(54)), // Invalid memory offset
	Ixud_ShareHandleErr(codeToString(55)), // Open the share memory fail
	Ixud_InvalidDataCount(codeToString(56)), // Invalid data count
	Ixud_WriteEEPErr(codeToString(57)), // Error while writing the EEPROM
	Ixud_CardIOErr(codeToString(58)), // CardIO error
	Ixud_IOErr(codeToString(59)), // MemoryIO error
	Ixud_SetScanChannelErr(codeToString(60)), // Set channel scan number error
	Ixud_SetScanConfigErr(codeToString(61)), // Set channel scan config error
	Ixud_GetMMIOMapStatus(codeToString(62));//

	private String codename;

	UniDaqReturnValue(String codename) {
		this.codename = codename;
	}

	static String codeToString(int code) {
		switch (code) {
		// Return code
		case 0:
			return "Correct";
		case 1:
			return "Open driver error";
		case 2:
			return "Plug & Play error";
		case 3:
			return "The driver was not open";
		case 4:
			return "Receive driver version error";
		case 5:
			return "Board number error";
		case 6:
			return "No board found";
		case 7:
			return "Board Mapping error";
		case 8:
			return "Digital input/output mode setting error";
		case 9:
			return "Invalid address";
		case 10:
			return "Invalid size";
		case 11:
			return "Invalid port number";
		case 12:
			return "This board model is not supported";
		case 13:
			return "This function is not supported";
		case 14:
			return "Invalid channel number";
		case 15:
			return "Invalid value";
		case 16:
			return "Invalid mode";
		case 17:
			return "A timeout occurred while receiving the status of the analog input";
		case 18:
			return "Timeout error";
		case 19:
			return "A compatible configuration code table index could not be found";
		case 20:
			return "ADC controller a timeout error";
		case 21:
			return "A compatible PCI table index value could not be found";
		case 22:
			return "Invalid setting value";
		case 23:
			return "Error while allocating the memory space";
		case 24:
			return "Error while installing the interrupt event";
		case 25:
			return "Error while installing the interrupt IRQ";
		case 26:
			return "Error while removing the interrupt IRQ";
		case 27:
			return "Error while the clear interrupt count";
		case 28:
			return "Error while retrieving the system buffer";
		case 29:
			return "Error while create the event";
		case 30:
			return "Resolution not supported";
		case 31:
			return "Error while create the thread";
		case 32:
			return "Thread timeout error";
		case 33:
			return "FIFO overflow error";
		case 34:
			return "FIFO timeout error";
		case 35:
			return "Retrieves the status of the interrupt installation";
		case 36:
			return "Retrieves the status of the system buffer";
		case 37:
			return "Error while setting the buffer count";
		case 38:
			return "Error while setting the buffer data";
		case 39:
			return "Card ID code could not be found";
		case 40:
			return "Event Thread error";
		case 41:
			return "Error while automatically creating an event";
		case 42:
			return "Register Thread error";
		case 43:
			return "Search Event error";
		case 44:
			return "Error while resetting the FIFO";
		case 45:
			return "Invalid EEPROM block";
		case 46:
			return "Invalid EEPROM address";
		case 47:
			return "Error while acquiring spin lock";
		case 48:
			return "Error while releasing spin lock";
		case 49:
			return "Analog input setting error";
		case 50:
			return "Invalid channel number";
		case 51:
			return "Invalid model number";
		case 52:
			return "Error while setting the mapping address";
		case 53:
			return "Error while releasing the mapping address";
		case 54:
			return "Invalid memory offset";
		case 55:
			return "Open the share memory fail";
		case 56:
			return "Invalid data count";
		case 57:
			return "Error while writing the EEPROM";
		case 58:
			return "CardIO error";
		case 59:
			return "MemoryIO error";
		case 60:
			return "Set channel scan number error";
		case 61:
			return "Set channel scan config error";
		case 62:
			return "Get MIMO map status error";
		default:
			return "UNKNOWN ERROR!!!";
		}
	}

	public String toString() {
		return this.codename;
	}
}