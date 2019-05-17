package org.jlab.EFADC;

import io.netty.buffer.ByteBuf;

import java.util.logging.Logger;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/27/12
 */
public abstract class RegisterSet {

	static final byte OPCODE_SET_REG =				(byte)0x01;
	static final int DAT_FOR_REG =					0x0000;	// what is this?

	public static final int REG_1 = 0;
	public static final int REG_2 = 1;
	public static final int REG_3 = 2;
	public static final int REG_4 = 3;
	public static final int REG_5 = 4;
	public static final int REG_6 = 5;
	public static final int REG_7 = 6;
	public static final int REG_8 = 7;
	public static final int REG_9 = 8;
	public static final int REG_10 = 9;
	public static final int REG_11 = 10;
	public static final int REG_12 = 11;
	public static final int REG_13 = 12;
	public static final int REG_14 = 13;
	public static final int REG_15 = 14;
	public static final int REG_16 = 15;
	public static final int REG_17 = 16;
	public static final int REG_18 = 17;
	public static final int REG_19 = 18;
	public static final int REG_20 = 19;

	static final int SyncOn_Mask	= 0x2000;

	protected int[] register;
	protected int[] status;
	protected String[] description;

	public static final boolean DEBUG = false;

	public abstract void update(RegisterSet reg);

	public abstract ByteBuf encode();

	public enum MatrixType {
		OR,
		AND,
		WIDTH,
		DELAY
	}


	public int getRegister(int reg) {
		if (reg < 0 || reg > register.length)
			return 0;

		return register[reg];
	}

	public boolean setRegister(int reg, int value) {

		register[reg] = value;

		return true;
	}

	/**
	 *
	 * @param frame Buffer to extract register values from
	 * @param nRegs Number of registers to decode, each register is a 16 bit word
	 * @return
	 */
	public final boolean decode(ByteBuf frame, int nRegs) {

		StringBuilder strB = new StringBuilder();

		for (int j = 0; j < nRegs; j++) {
			int val = frame.readUnsignedShort();	//prevent sign extension

			if (DEBUG)
				strB.append(String.format("[%d] %04x\n", j+1, val));

			if (!setRegister(j, val)) {
				Logger.getGlobal().warning("Invalid register readback buffer");
				return false;
			}

		}

		if (DEBUG)
			Logger.getGlobal().info(strB.toString());

		return true;
	}

	public void setSync(boolean sync) {
		if (sync)
			register[REG_1] |= SyncOn_Mask;
		else
			register[REG_1] &= ~SyncOn_Mask;
	}
}
