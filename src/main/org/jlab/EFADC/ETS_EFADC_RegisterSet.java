package org.jlab.EFADC;


import io.netty.buffer.ByteBuf;

import java.util.logging.Logger;

import static io.netty.buffer.Unpooled.buffer;


/**
 * Created by john on 7/27/17.
 * RegisterSet used by individual EFADC modules in an ETS system.
 */
public class ETS_EFADC_RegisterSet extends EFADC_RegisterSet {

	public static final int NUM_REGS = 20;
	public static final int NUM_STATUS = 9;
	public static final int DATA_SIZE_BYTES = (NUM_REGS + NUM_STATUS) * 2;

	private int m_ModuleId;		// Index origin 1
	private ETS_Client m_Client;


	public ETS_EFADC_RegisterSet(ETS_Client client, int mId) {
		Logger.getGlobal().info(String.format(" ::ETS_EFADC_RegisterSet(%d)", mId));

		m_Client = client;
		m_ModuleId = mId;
		status = new int[NUM_STATUS];

		register = new int[] {
				0x000a,
				0x0016,
				0x0180,
				0x0bd8,
				0x03e0,
				0x03e0,
				0x03e0,
				0x03e0,
				0xF0F0,
				0xF0F0,
				0x00F0,
				0x0202,
				0x0202,
				0x0001,
				0x0001,
				0x0001,
				0x0001,
				0x0fff,
				0x0c80,
				0x1ce4};
	}

	public int module() {
		return m_ModuleId;
	}

	public ETS_Client client() {
		return m_Client;
	}


	/**
	 *
	 * @param frame
	 * @return
	 */
	@Override
	public boolean decode(ByteBuf frame) {

		// Read register 0 which is the efadc select information
		// This is already read in FrameDecoder and used as the constructor parameter
		//int reg0 = frame.readUnsignedShort();

		// Decode configuration registers
		super.decode(frame, ETS_EFADC_RegisterSet.NUM_REGS);

		for (int i = 0; i < ETS_EFADC_RegisterSet.NUM_STATUS; i++) {
			status[i] = frame.readUnsignedShort();
		}

		//System.out.printf("%04x %04x %04x %04x %04x %04x %04x\n", status1, status3, status3, status4, status5, status6, status7);

		return true;
	}


	/**
	 *
	 * @return
	 */
	public ByteBuf encode() {
		int[] regs = getRegisters();

		ByteBuf buffer = buffer(regs.length * 2);

		for (int reg : regs) {
			buffer.writeShort(reg);
		}

		return buffer;
	}


	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer("ETS_EFADC Register Set");

		strB.append(String.format(", Module %d\n", m_ModuleId));

		for (int reg : register) {
			strB.append(String.format("%04x ", reg));
		}
		strB.append("\n");

		strB.append(String.format("[01] %04x - Mode: %d (%s)  Int Window: %d\n",
				register[REG_1], getMode(), (getMode() == 1 ? "Sum" : "Sampling"), getIntegrationWindow()));
		strB.append(String.format("[02] %04x - NSB: %d\n", register[REG_2], getNSB()));
		strB.append(String.format("[03] %04x\n", register[REG_3]));
		strB.append(String.format("[04] %04x\n", register[REG_4]));
		strB.append(String.format("[05] %04x - Det 1 Thresh: %d\n", register[REG_5], register[REG_5] & 0x3fff));
		strB.append(String.format("[06] %04x - Det 2 Thresh: %d\n", register[REG_6], register[REG_6] & 0x3fff));
		strB.append(String.format("[07] %04x - Det 3 Thresh: %d\n", register[REG_7], register[REG_7] & 0x3fff));
		strB.append(String.format("[08] %04x - Det 4 Thresh: %d\n", register[REG_8], register[REG_8] & 0x3fff));
		strB.append(String.format("[11] %04x - Coinc Window Width: %d\n", register[REG_11], getCoincidenceWindowWidth()));

		return strB.toString();
	}
}
