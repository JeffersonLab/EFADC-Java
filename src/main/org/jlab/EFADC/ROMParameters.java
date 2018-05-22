package org.jlab.EFADC;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Created by john on 7/11/16.
 */
public class ROMParameters {
	int ip, subnet, port, serial;
	long mac;

	List<Integer> romConfig = new Vector<>(11);

	ROMParameters(int ip, int subnet, long mac, int port, int serial) {
		this.ip = ip; this.subnet = subnet; this.mac = mac; this.port = port; this.serial = serial;
	}

	List<Integer> getParamsAsRegisters() {
		romConfig.clear();

		// [0]
		romConfig.add(0x0800);

		// [1] [2]
		if (ip != -1) {
			romConfig.add((ip >> 16));
			romConfig.add((ip & 0x0000ffff));
		} else {
			romConfig.add(0x0102);
			romConfig.add(0x0309);
		}

		// [3] [4]
		if (subnet != -1) {
			romConfig.add((subnet >> 16));
			romConfig.add((subnet & 0x0000ffff));
		} else {
			romConfig.add(0xffff);
			romConfig.add(0xff00);
		}

		// [5] - Board serial number (0 is no change)
		romConfig.add((serial & 0x0000ffff));

		// [6]
		romConfig.add(port);

		// [7] [8] [9]
		if (mac != -1) {
			romConfig.add((int)(mac >> 32));
			romConfig.add((int)(mac >> 16));
			romConfig.add(((int)mac & 0x0000ffff));
		} else {
			romConfig.add(0xceba);
			romConfig.add(0xf100);
			romConfig.add(0x0000);
		}

		// [10]
		if (serial != -1)
			romConfig.add(0xabcd);
		else
			romConfig.add(0x0000);

		return romConfig;
	}


	/**
	 * Prep for rom readback by configuring config register 0
	 */
	void armReadback() {
		romConfig.set(0, 0x0a00);
	}


	/**
	 * Verifies that the previously written rom configuration was done correctly
	 * Status registers 1-9 should match config registers 1-9 (index origin 1?)
	 * @return True if the ROM configuration was successfully written
	 */
	boolean verifyReadback(int[] reg) {

		int max = EFADC_RegistersV3.NUM_REGISTERS + 8;

		if (reg.length < max) {
			Logger.getLogger("global").warning("Invalid # of registers for verification");
			return false;
		}


		for (int i = 0; i < 8; i++) {
			if (reg[i] != reg[EFADC_RegistersV3.NUM_REGISTERS + i + 1]) {
				return false;
			}
		}

		return true;
	}
}
