package org.jlab.EFADC;

public class RegisterFactory {

	public static ETS_RegisterSet InitETSRegisters(ETS_Client client) {
		ETS_RegisterSet regs = new ETS_RegisterSet(client);

		regs.initTables();

		return regs;
	}

	public static CMP_RegisterSet InitCMPRegisters(int adc) {
		CMP_RegisterSet regs = new CMP_RegisterSet(adc);

		regs.initTables();

		return regs;
	}
}
