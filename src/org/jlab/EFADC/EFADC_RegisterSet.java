//
//  EFADC_RegisterSet.java
//  EFADC_java
//
//  Created by John McKisson on 1/25/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//

package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.logging.Logger;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

public abstract class EFADC_RegisterSet extends RegisterSet {

	static final byte FUNC_EFADC_REG = (byte)0x00;



	// Register index constants are defined because the register header (reg #0 in EFADC list) is not a real EFADC register
	// However it appears in the register readout and is required in the writing of the register block
	// This is because in a CMP register block, the header is only listed once at the start of the buffer, and not in front of
	// each additional EFADC register readout in the CMP readout process
	// TODO: Documentation should be fixed to reflect this

	protected int[] m_Registers;


	long lastUpdated;

	//Status registers
	public int version;
	public long acceptedTrigs;
	public long missedTrigs;
	public int unknown;
	public float fpgaTemp;

	int regHeader;	/* 0 */	//"Bits 15..8 indicate standalone EFADC (all zeros) or CMP (bit 15 is 1 and 14..8 indicate # of active EFADCs), 7..0: Packet throttle delay"
	
	public EFADC_RegisterSet() {

		regHeader = 0x0001;
	}

	public EFADC_RegisterSet(int header) {
		this();
		regHeader = header;
	}

	public void update(RegisterSet reg) {
		if (reg instanceof EFADC_RegisterSet) {
			Logger.getLogger("global").info("Updating EFADC register set");
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
	
	public int[] getRegisters() {return m_Registers;}

	
	public ChannelBuffer encode(boolean header) {
		int[] regs = getRegisters();

		ChannelBuffer buffer = buffer((header ? 7 : 2) + regs.length * 2);

		if (header) {
			buffer.writeByte((byte)0x5a);
			buffer.writeByte((byte)0x5a);
			buffer.writeByte(OPCODE_SET_REG);
			buffer.writeByte(FUNC_EFADC_REG);
			buffer.writeByte((byte)0x00);	// extra byte?
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
}
