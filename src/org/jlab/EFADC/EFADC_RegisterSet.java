//
//  EFADC_RegisterSet.java
//  EFADC_java
//
//  Created by John McKisson on 1/25/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//

package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.matrix.MatrixFactory;
import org.jlab.EFADC.matrix.MatrixRegisterEncoder;

import java.util.logging.Logger;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

public abstract class EFADC_RegisterSet extends RegisterSet implements EFADC_Registers {

	static final byte FUNC_EFADC_REG = (byte)0x00;

	static final int EFADC_MATRIX_SIZE = 4;

	// Register index constants are defined because the register header (reg #0 in EFADC list) is not a real EFADC register
	// However it appears in the register readout and is required in the writing of the register block
	// This is because in a CMP register block, the header is only listed once at the start of the buffer, and not in front of
	// each additional EFADC register readout in the CMP readout process
	// TODO: Documentation should be fixed to reflect this


	long lastUpdated;

	//Status registers
	public int m_Version;
	public long acceptedTrigs;
	public long missedTrigs;
	public int unknown;
	public float fpgaTemp;

	int regHeader;	/* 0 */	//"Bits 15..8 indicate standalone EFADC (all zeros) or CMP (bit 15 is 1 and 14..8 indicate # of active EFADCs), 7..0: Packet throttle delay"
	
	public EFADC_RegisterSet() {

		regHeader = 0x0001;

		m_ORTable = MatrixFactory.newCoincidenceMatrix(EFADC_MATRIX_SIZE, 2);
		m_ANDTable = MatrixFactory.newCoincidenceMatrix(EFADC_MATRIX_SIZE, 2);
	}

	public EFADC_RegisterSet(int header) {
		this();
		regHeader = header;
	}

	public void update(RegisterSet reg) {
		if (reg instanceof EFADC_RegisterSet) {
			Logger.getLogger("global").info("Updating EFADC register set");
		} else return;

		if (m_Registers.length != reg.getRegisters().length) {
			Logger.getLogger("global").warning("Register Set not same length");
		}

		for (int i = 0; i < m_Registers.length; i++) {
			m_Registers[i] = reg.getRegister(i);
		}
	}

	public long getAccceptedTriggers() {return acceptedTrigs;}

	public long getLastUpdate() {return lastUpdated;}

	public long getMissedTriggers() {
		return missedTrigs;
	}

	public float getFpgaTemp() {
		return fpgaTemp;
	}


	
	public ChannelBuffer encode(boolean header) {
		int[] regs = getRegistersCombined();

		ChannelBuffer buffer = buffer((header ? 7 : 2) + regs.length * 2);

		if (header) {
			buffer.writeByte((byte)0x5a);
			buffer.writeByte((byte)0x5a);
			buffer.writeByte(OPCODE_SET_REG);
			buffer.writeByte(FUNC_EFADC_REG);
			buffer.writeByte((byte)0x00);	// extra byte? Why is this needed? Only Hai will ever know...
		}

		buffer.writeShort(regHeader);

		for (int reg : regs) {
			buffer.writeShort(reg);
		}
		
		return buffer;
	}

	/**
	 * decode() is abstract instead of defined in the interface because we don't necessarily want
	 * it exposed via the interface but require it to be defined by all register subclasses
 	 */
	public abstract boolean decode(ChannelBuffer frame);

	/**
	 * Utility method to return the register set as it should be written to the efadc
	 * Takes care of parsing the coincident matrix and other crap into the int array
	 * @return
	 */
	protected abstract int[] getRegistersCombined();
}
