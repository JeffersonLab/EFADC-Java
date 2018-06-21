package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.matrix.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 8/14/12
 */
public class CMP_RegisterSet extends RegisterSet {

	public static final int NUM_READ_REGS = 1;
	public static final int NUM_WRITE_REGS = 152;
	public static final int NUM_STATUS = 14;
	public static final int DATA_SIZE_READ_BYTES = (NUM_READ_REGS + NUM_STATUS) * 2;
	public static final int DATA_SIZE_WRITE_BYTES = (NUM_WRITE_REGS) * 2;

	private static final int EFADC_REG_INDEX	= 21;
	private static final int HITOUT_WIDTH_INDEX = 129;
	private static final int HITOUT_DELAY_INDEX = 136;

	static final byte FUNC_CMP_REG = 0x03;

	private int m_NumADC;
	private int m_SelectedADC;
	private short m_CMPRxBufLimit = 50;	// this value works for summing mode
	private short m_EthernetReadbackBufFullThreshold = 1352;

	public int[] status;
	private ArrayList<EFADC_RegisterSet> adc;

	protected CoincidenceMatrix	m_ORTable;
	protected CoincidenceMatrix	m_ANDTable;
	protected RegisterMatrix		m_DelayTable;
	protected RegisterMatrix		m_WidthTable;

	protected CMP_RegisterSet() {
		this(2);
	}


	CMP_RegisterSet(int nADC) {

		Logger.getGlobal().log(Level.FINER, " ::CMP_RegisterSet()");

		m_NumADC = nADC;

		adc = new ArrayList<>(nADC);

		register = new int[NUM_WRITE_REGS];
		status = new int[NUM_STATUS];

		m_SelectedADC = 0;	// Preselect all ADC's

		/*
		m_WidthTable.set(0, 0x3333);	//129 (master)
		m_WidthTable.set(1, 0x3333);	//130 (slave)
		m_WidthTable.set(2, 0x0000);	//131
		m_WidthTable.set(3, 0x0000);	//132
		m_WidthTable.set(4, 0x0000);	//133
		m_WidthTable.set(5, 0x0000);	//134
		m_WidthTable.set(6, 0x0000);	//135

		m_DelayTable.set(0, 0x1111);	//136 (master)
		m_DelayTable.set(1, 0x1111);	//137 (slave)
		m_DelayTable.set(2, 0x0000);	//138
		m_DelayTable.set(3, 0x0000);	//139
		m_DelayTable.set(4, 0x0000);	//140
		m_DelayTable.set(5, 0x0000);	//141
		m_DelayTable.set(6, 0x0000);	//142
		*/
	}

	void initTables() {

		// 27 is hard coded into the CMP register map as the number of possible detectors
		m_ORTable = MatrixFactory.newCoincidenceMatrix(27, 54);
		m_ANDTable = MatrixFactory.newCoincidenceMatrix(27, 54);

		m_DelayTable = MatrixFactory.newRegisterMatrix(27,7);
		m_WidthTable = MatrixFactory.newRegisterMatrix(27, 7);
	}

	public void update(RegisterSet reg) {
		if (reg instanceof CMP_RegisterSet) {

			CMP_RegisterSet cRegs = (CMP_RegisterSet)reg;

			Logger.getGlobal().info("Updating CMP registers");

			for (int i = 1; i < cRegs.getADCCount() + 1; i++) {

				try {
					EFADC_RegisterSet eRegs = cRegs.getADCRegisters(i);

					setADCRegisters(i, eRegs);

				} catch (EFADC_InvalidADCException e) {
					e.printStackTrace();
				}
			}
		}
	}


	/**
	 * 0 to select all ADC's
	 * @param n
	 */
	public void selectADC(int n) throws EFADC_InvalidADCException {
		if (n < 0 || n > adc.size()) {
			throw new EFADC_InvalidADCException();
		}

		m_SelectedADC = n;
	}

	public int selectedADC() {
		return m_SelectedADC;
	}


	public int[] getRegisters() {
		return register;
	}

	public int getADCCount() {
		return m_NumADC;
	}


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


	/**
	 *
	 * @param n Index of ADC from 1 to num ADC's
	 * @return Register set of the ADC
	 */
	public EFADC_RegisterSet getADCRegisters(int n) throws EFADC_InvalidADCException {
		if (n < 1 || n > adc.size() + 1) {
			throw new EFADC_InvalidADCException();
		}

		return adc.get(n - 1);
	}


	/**
	 * @param n Index of ADC from 1 to num ADC's
	 * @param r Registers to set
	 */
	private void setADCRegisters(int n, EFADC_RegisterSet r) throws EFADC_InvalidADCException {
		if (n < 1 || n > adc.size() + 1) {
			throw new EFADC_InvalidADCException();
		}

		adc.set(n - 1, r);
	}


	/**
	 * First encode the EFADC registers (21 registers, no status)
	 * Reg 21 to 128 are Coincidence table entries
	 * Reg 129 to 135 are HitOut pulse width entries
	 * Reg 136 to 142 are HitOut pulse delay entries
	 * Reg 143 to 147 are NYI
	 * Reg 148 and 149 are numbers that I have to compute for some stupid reason
	 * Reg 150 and 151 are reserved
	 * @return
	 */
	public ChannelBuffer encode() {
		//int[] regs = getRegisters();

		ChannelBuffer buffer = buffer(5 + DATA_SIZE_WRITE_BYTES);

		buffer.writeByte((byte)0x5a);
		buffer.writeByte((byte)0x5a);
		buffer.writeByte(OPCODE_SET_REG);	//01
		buffer.writeByte((byte)0x00);		//extra byte?
		buffer.writeByte(FUNC_CMP_REG);		//03

		try {
			EFADC_RegisterSet efadcRegs = getADCRegisters(m_SelectedADC == 0 ? 1 : m_SelectedADC);	// selected adc index is 1 greater because 0 selects all

			//Logger.getGlobal().info(String.format("CMP_RegisterSet:encode()\n\tADC %d Int Window: %d\n\tCoincidenceWidth: %d",
			//		m_SelectedADC, efadcRegs.getIntegrationWindow(), efadcRegs.getCoincidenceWindowWidth()));

			// Encode selected EFADC register set
			buffer.writeBytes(efadcRegs.encode(false));	// dont encode 5a5a header
		} catch (EFADC_InvalidADCException e) {
			Logger.getGlobal().severe("Out of Bounds while selecting ADC: " + m_SelectedADC);
			return null;
		}

		// Encode coincidence table entries
		ChannelBuffer matrixBuf = MatrixRegisterEncoder.encode(m_ORTable, m_ANDTable, 32);	// ~216 bytes

		//Logger.getGlobal().info(String.format("CoincidenceTableBuf size = %d", matrixBuf.readableBytes()));

		buffer.writeBytes(matrixBuf);

		/*
		// Encode HitOut pulse widths
		ChannelBuffer regBuf = MatrixRegisterEncoder.encode(m_WidthTable, 16, 4);

		Logger.getGlobal().info(String.format("WidthTableBuf size = %d", regBuf.readableBytes()));

		buffer.writeBytes(regBuf);

		// Encode HitOut Delay
		regBuf = MatrixRegisterEncoder.encode(m_DelayTable, 16, 4);

		Logger.getGlobal().info(String.format("DelayTableBuf size = %d", regBuf.readableBytes()));

		buffer.writeBytes(regBuf);
		*/

		// Fake encode these until the register encoding matrix is fixed
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);

		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);

		// Encode efadc register select, 143
		// Bits 2..0
		// 000 - Select all ADC's
		// 001 - Master
		// 010 - Slave
		// 011 - ADC #3, etc
		buffer.writeShort(m_SelectedADC & 0x7);

		//Logger.getGlobal().info(String.format("Reg 143 (Selected ADC): %d", m_SelectedADC & 0x7));

		//Reset back to 0
		m_SelectedADC = 0;

		// Encode NYI, 144-147
		buffer.writeShort(0x0000);	//0x1111
		buffer.writeShort(0x0000);
		buffer.writeShort(0x0000);
		buffer.writeShort(0x0000);	//0x02ff

		// Encode unneccessarily computed numbers (hard coded), 148-149
		buffer.writeShort(getEthernetReadbackBufFullThreshold());
		buffer.writeShort(getCMPRxBufLimit());

		// Encode reserved stuff, 150-151
		buffer.writeShort(0x00ff);	//0x00ff
		buffer.writeShort(0x00ff);	//0x00ff

		//Logger.getGlobal().info(String.format("RegisterWriteBuf size = %d", buffer.readableBytes()));

		/*
		StringBuilder outStr = new StringBuilder();

		for (byte b : buffer.array()) {
			outStr.append(String.format("%02X ", b));
		}

		Logger.getGlobal().info(outStr.toString());
		*/

		return buffer;
	}


	public boolean decode(ChannelBuffer frame) {
		//super.decode(frame, NUM_REGS);

		// Read register 0 which only appears once (instead of before each EFADC register block)
		int reg0 = frame.readUnsignedShort();

		status[0] = frame.readUnsignedShort();
		status[1] = frame.readUnsignedShort();
		status[2] = frame.readUnsignedShort();

		Logger.getGlobal().info(String.format("CMP Reg0: %04x Status 1-3:  %04x  %04x  %04x", reg0, status[0], status[1], status[2]));

		// Should be clear anyway
		adc.clear();

		// Decode EFADCs one by one
		for (int i = 0; i < m_NumADC; i++) {

			// Read EFADC configuration and status
			ChannelBuffer adcBuf = frame.readBytes(EFADC_RegisterSet.DATA_SIZE_BYTES);

			EFADC_RegisterSet adcReg = new EFADC_RegisterSet();
			adcReg.decode(adcBuf);

			adc.add(adcReg);
		}

		for (int i = 3; i < NUM_STATUS; i++) {
			status[i] = frame.readUnsignedShort();
		}

		// Copy Master?? EFADC registers to register map

		EFADC_RegisterSet masterEFADC = adc.get(0);

		System.arraycopy(masterEFADC.getRegisters(), 0, register, 0, EFADC_RegisterSet.NUM_REGS);

		/*
		int writeIdx = 0;
		for (int i = 0; i < EFADC_RegisterSet.NUM_REGS; i++) {
			register[writeIdx++] = masterEFADC.getRegister(i);
		}
		*/



		return true;
	}


	/*
	Mode 1 (verifying mode)
	NumberOfTriggerThat_EfadcTriggerFifoCanHold = int((1000/(WordsPerTrigger+2))-3);
	NumberOfTriggerThat_EfadcTnTsFifoCanHold = 500;
	NumberOfTriggerThat_EfadcReadOutCanHold = int((4000/((NumberOfAdcChannelHasData*WordsPerTrigger)+10))-1);
	NumberOftriggerThat_CmpRxFifoCanHandle = int((2000/((NumberOfAdcChannelHasData*WordsPerTrigger)+10))-1);

	CmpRxBufFull = The smallest of the above calculations.

	Mode 0 (Sum mode)
	NumberOfTriggerThat_EfadcTriggerFifoCanHold = int((1000/(WordsPerTrigger+2))-3);
	NumberOfTriggerThat_EfadcTnTsFifoCanHold = 500;
	NumberOfTriggerThat_EfadcReadOutCanHold = int((4000/((NumberOfAdcChannelHasData*2)+10))-1);
	NumberOftriggerThat_CmpRxFifoCanHandle = int((2000/((NumberOfAdcChannelHasData*2)+10))-1);

	CmpRxBufFull = The smallest of the above calculations

	 */
	public short getCMPRxBufLimit() {

		int nWordsPerTrigger = (getRegister(REG_2)  & 0x01FF) + 1;

		// Count
		int nORbits = 0;
		int nANDbits = 32;
		int nMode = 0;

		int opt1 = (int)(1000 / (nWordsPerTrigger + 2)) - 3;
		int opt2 = 500;

		if (nMode == 0) {
			int opt3 = (int)(4000 / ((nANDbits * 2) + 10)) - 1;
			int opt4 = (int)(2000 / ((nANDbits * 2) + 10)) - 1;

		} else {

			int opt3 = (int)(4000 / (nANDbits * nWordsPerTrigger + 10)) - 1;
			int opt4 = (int)(2000 / (nANDbits * nWordsPerTrigger + 10)) - 1;
		}



		return m_CMPRxBufLimit;
	}


	public void setCMPRxBufLimit(short val) {
		m_CMPRxBufLimit = val;
	}


	/*
	RdBckBufFull_TH stores assembled data (in bytes) to be sent to host via Ethernet link.  The entire Fifo is sent when the number of bytes stored is greater than this.

	Mode 1 (verifying mode)
	RdBckBufFull_TH = int((1400/((NumberOfAdcChannelHasData*WordsPerTrigger)+10))-1);

	RdBckBufFull_TH = RdBckBufFull_TH * ((NumberOfAdcChannelHasData*WordsPerTrigger)+10);

	WordsPerTrigger = bit(8..0) of EFADC Config1 : Int Window


	Mode 0 (Sum mode)
	RdBckBufFull_TH = int((1400/((NumberOfAdcChannelHasData*2)+10))-1);
	RdBckBufFull_TH = RdBckBufFull_TH * ((NumberOfAdcChannelHasData*2)+10);

	 */
	public short getEthernetReadbackBufFullThreshold() {
		return m_EthernetReadbackBufFullThreshold;
	}


	public void setEthernetReadbackBufFullThreshold(short val) {
		m_EthernetReadbackBufFullThreshold = val;
	}

}
