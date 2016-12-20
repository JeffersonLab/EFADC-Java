package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.matrix.CoincidenceMatrix;
import org.jlab.EFADC.matrix.Matrix;
import org.jlab.EFADC.matrix.RegisterMatrix;

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

	static final int SyncOn_Mask	= 0x2000;

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

	public enum MatrixType {
		OR,
		AND,
		WIDTH,
		DELAY
	}



	protected int[] m_Registers;
	protected String[] description;

	protected CoincidenceMatrix		m_ORTable;
	protected CoincidenceMatrix		m_ANDTable;
	protected RegisterMatrix		m_DelayTable;
	protected RegisterMatrix		m_WidthTable;

	public Matrix getMatrix(MatrixType type) {

		switch (type) {
			case OR:
				return m_ORTable;
			case AND:
				return m_ANDTable;
			case WIDTH:
				return m_WidthTable;
			case DELAY:
				return m_DelayTable;
			default:
				return null;
		}

	}

	@Deprecated
	public void SetANDCoincident(int detA, int detB, boolean val, boolean reverse) {

		/*
		CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

		CoincidenceMatrix matrix = (CoincidenceMatrix)cmpReg.getMatrix(CMP_RegisterSet.MatrixType.AND);

		matrix.setCoincident(detA, detB, val, false);
		*/

		SetCoincident(detA, detB, val, reverse, RegisterSet.MatrixType.AND);
	}

	@Deprecated
	public void SetORCoincident(int detA, int detB, boolean val, boolean reverse) {

		CoincidenceMatrix matrix = (CoincidenceMatrix)getMatrix(RegisterSet.MatrixType.OR);

		matrix.setCoincident(detA, detB, val, false);
	}


	public void SetCoincident(int detA, int detB, boolean val, boolean reverse, RegisterSet.MatrixType type) {

		CoincidenceMatrix matrix = (CoincidenceMatrix)getMatrix(type);

		matrix.setCoincident(detA, detB, val, reverse);

		/*
		Logger.getLogger("global").info(String.format("SetCoincident(%d, %d, %s, %s, Opp: %s)",
				detA, detB,
				val ? "ON" : "OFF",
				type == CMP_RegisterSet.MatrixType.AND ? "AND" : "OR",
				reverse ? "YES" : "NO"));
		*/
	}

	protected abstract void update(RegisterSet reg);


	public int getRegister(int reg) {
		if (reg < 0 || reg > m_Registers.length)
			return 0;

		return m_Registers[reg];
	}

	public int[] getRegisters() {return m_Registers;}

	public boolean setRegister(int reg, int value) {

		if (reg >= m_Registers.length) {
			Logger.getLogger("global").severe(String.format("Attempt to access invalid register %d to value %0x (max registers %d)", reg, value, m_Registers.length));
			return false;
		}

		m_Registers[reg] = value;

		return true;
	}

	public final boolean decode(ChannelBuffer frame, int nRegs) {

		for (int j = 0; j < nRegs; j++) {
			int val = frame.readUnsignedShort();	//prevent sign extension
			//str += String.format("%04x ", val);
			if (!setRegister(j, val)) {
				Logger.getLogger("global").warning("Invalid register readback buffer");
				return false;
			}

		}

		return true;
	}

	public void setSync(boolean sync) {
		if (sync)
			m_Registers[REG_1] |= SyncOn_Mask;
		else
			m_Registers[REG_1] &= ~SyncOn_Mask;
	}

	public int getADCInvertMask() {
		Logger.getLogger("global").warning("Returning default value for getADCInvertMask() in abstract RegisterSet");
		return 0;
	}
}
