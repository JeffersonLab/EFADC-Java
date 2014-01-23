package org.jlab.EFADC.command;

import org.jboss.netty.buffer.ChannelBuffer;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/12/12
 * Time: 12:35 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Command {
	static final byte OPCODE_ACT_CMD = 0x02;

	static final byte CMD_ACT_OFF = 0x00;
	static final byte CMD_ACT_CON_TOFF = 0x01;	// Collect trigger data.
	static final byte CMD_ACT_COFF_TON = 0x02;  // Send Test Data that can be used to test Ethernet Bandwidth.
	static final byte CMD_ACT_READBACK = 0x03;	// Request register readback

	static final int ADC_TEST_OFF_CMD = 0x0;
	static final int ADC_TX_MASTER_TO_SLAVE_CMD = 0x01;
	static final int ADC_CML_ENABLE_CMD = 0x2;

	static final int ADC_TEST_REG = 0x0D00;  // this is the register inside the ADC to turn test on
	static final int ADC_AIN_CONFIG_REG = 0x0F00;  // Analog input Disable, CML
	static final int ADC_MASTER_TO_SLAVE_REG = 0xFF00;  // write 1 to this register to transfer data from Master Shift Reg to Salve
	static final int ADC_CLK_OUT_DELAY_REG = 0x1700;


	public static ChannelBuffer StartCollection() {
		ChannelBuffer buf = buffer(4);

		buf.writeByte((byte)0x5a);
		buf.writeByte((byte)0x5a);
		buf.writeByte(OPCODE_ACT_CMD);
		buf.writeByte(CMD_ACT_CON_TOFF);

		return buf;
	}

	public static ChannelBuffer StopCollection() {
		ChannelBuffer buf = buffer(18);

		buf.writeByte((byte)0x5a);
		buf.writeByte((byte)0x5a);
		buf.writeByte(OPCODE_ACT_CMD);
		buf.writeByte(CMD_ACT_OFF);
		/*
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		*/

		return buf;
	}

	public static ChannelBuffer ReadRegisters() {
		ChannelBuffer buf = buffer(18);

		buf.writeByte((byte)0x5a);
		buf.writeByte((byte)0x5a);
		buf.writeByte(OPCODE_ACT_CMD);
		buf.writeByte(CMD_ACT_READBACK);
		/*
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		*/

		return buf;
	}
	
	public static ChannelBuffer TestData() {
		ChannelBuffer buf = buffer(18);

		buf.writeByte((byte)0x5a);
		buf.writeByte((byte)0x5a);
		buf.writeByte(OPCODE_ACT_CMD);
		buf.writeByte(CMD_ACT_COFF_TON);
		/*
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		buf.writeByte((byte)0);
		*/

		return buf;
	}
}
