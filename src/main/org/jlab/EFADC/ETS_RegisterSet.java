package org.jlab.EFADC;

import io.netty.buffer.ByteBuf;
import org.jlab.EFADC.matrix.MatrixFactory;
import org.jlab.EFADC.matrix.MatrixRegisterEncoder;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.netty.buffer.Unpooled.buffer;


/**
 * Created by john on 7/13/17.
 */
public class ETS_RegisterSet extends CMP_RegisterSet {

	public static final int MIN_VERSION = 0x3601;

	public static final int NUM_READ_REGS = 131;
	public static final int NUM_STATUS = 9;
	public static final int DATA_SIZE_BYTES = (NUM_READ_REGS + NUM_STATUS) * 2;

	// Need +1 for register 0
	public static final int DATA_SIZE_WRITE_BYTES = (ETS_EFADC_RegisterSet.NUM_REGS + NUM_READ_REGS + 1) * 2;

	private static final int EFADC_REG_INDEX	= 21;
	private static final int HITOUT_WIDTH_INDEX = 129;
	private static final int HITOUT_DELAY_INDEX = 136;

	static final byte FUNC_ETS_REG = 0x03;

	private long lastUpdated;

	private ArrayList<ETS_EFADC_RegisterSet> adc;

	private ETS_Client m_Client;

	// Mask to select which EFADC's are to be sent their config registers
	private int m_EFADCSelect_Mask = 0;


	ETS_RegisterSet(ETS_Client client) {

		// This leaves adc arraylist uninitialized
		super();

		m_Client = client;

		adc = new ArrayList<>(8);
		for (int i = 0; i < 8; i++)
			adc.add(null);

		Logger.getGlobal().info(" ::ETS_RegisterSet()");
	}

	void initTables() {
		m_ORTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		m_ANDTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		// as of ETS 3600 it can still only support 27 EFADC's
		m_DelayTable = MatrixFactory.newRegisterMatrix(27,7);
		m_WidthTable = MatrixFactory.newRegisterMatrix(27, 7);
	}

	public ETS_Client client() {
		return m_Client;
	}


	public void update(RegisterSet reg) {
		Logger.getGlobal().warning("Tried to update with non ETS_Register set");
	}


	public void update(ETS_RegisterSet reg) {
		Logger.getGlobal().info("Updating ETS registers");

		int[] newRegs = reg.getRegisters();

		for (int i = 0; i < newRegs.length; i++) {
			if (register[i] != newRegs[i]) {
				Logger.getGlobal().log(Level.FINE, String.format("Updating %d, %04x -> %04x", i+1, register[i], newRegs[i]));
				register[i] = newRegs[i];
			}
		}

		for (int i = 0; i < status.length; i++) {
			if (status[i] != reg.status[i]) {
				status[i] = reg.status[i];
			}
		}

		lastUpdated = System.currentTimeMillis();
	}


	/**
	 * Update/set the EFADC registers belonging to this ETS
	 * @param regs EFADC registers
	 */
	public void setEFADCRegisterSet(ETS_EFADC_RegisterSet regs) {

		Logger.getGlobal().info(String.format("setEFADCRegisterSet module %d", regs.module()));

		// module has index origin 1
		adc.set(regs.module() - 1, regs);
	}

	/**
	 * @param n Index of ADC from 1 to num ADC's
	 * @param r Registers to set
	 */
	public void setADCRegisters(int n, ETS_EFADC_RegisterSet r) throws EFADC_InvalidADCException {
		if (n < 1 || n > adc.size() + 1) {
			throw new EFADC_InvalidADCException();
		}

		Logger.getGlobal().info(String.format("setADCRegisters: %d", n));

		adc.set(n - 1, r);
	}


	/**
	 * @return Number of connected EFADCs
	 */
	@Override
	public int getADCCount() {
		return Integer.bitCount(getEFADCConnectedMask());
	}


	/**
	 * This is reported in status 2 of an ETS register read
	 * @return Bit mask of connected EFADC devices
	 */
	public int getEFADCConnectedMask() {
		return status[1] & 0x00ff;
	}


	/**
	 * @param mask Mask of EFADC devices to be addressed during register write
	 */
	public void setEFADC_Mask(int mask) {
		//Logger.getGlobal().info(String.format("setEFADC_Mask %04x", mask));

		m_EFADCSelect_Mask = mask;
	}

	public float getTemp() {
		if (status == null) {
			Logger.getGlobal().severe("Status registers not initialized, default constructor used?");
			return 0;
		}

		return (status[6] * (503.975f/1024.0f)) - 273.15f;
	}

	public int getVersion() {
		if (status == null) {
			Logger.getGlobal().severe("Status registers not initialized, default constructor used?");
			return 0;
		}

		return status[0];
	}


	public ArrayList<ETS_EFADC_RegisterSet> getADCRegisters() {
		return adc;
	}


	/**
	 *
	 * @param n Index of ADC from 1 to num ADC's
	 * @return Register set of the ADC, null if the registers have not yet been read from the device
	 */
	public ETS_EFADC_RegisterSet getADCRegisters(int n) throws EFADC_InvalidADCException {
		if (n < 1 || n > adc.size() - 1) {
			throw new EFADC_InvalidADCException();
		}

		ETS_EFADC_RegisterSet adcReg = adc.get(n - 1);
		if (adcReg == null) {
			Logger.getGlobal().log(Level.FINE, String.format("ADC %d registers null, generating default...", n));

			adcReg = new ETS_EFADC_RegisterSet(m_Client, n);
			adc.set(n - 1, adcReg);
		}

		return adcReg;
	}


	@Override
	public ByteBuf encode() {
		ByteBuf buffer = buffer(5 + DATA_SIZE_WRITE_BYTES);

		buffer.writeByte((byte)0x5a);
		buffer.writeByte((byte)0x5a);
		buffer.writeByte(OPCODE_SET_REG);	//01
		buffer.writeByte((byte)0x00);		//extra byte?
		buffer.writeByte(FUNC_ETS_REG);		//03

		// EFADC enable, we don't ever need to disable a connected efadc, so just use the same bits
		// that were reported in the status indicating which are connected
		int connected = getEFADCConnectedMask() << 8;
		int addressed = m_EFADCSelect_Mask;

		// bits 15..8 should be an exact copy of the connected mask we've previously read, with bits 7..0
		// representing which units to apply the following efadc register values to
		int reg0 = connected | addressed;

		Logger.getGlobal().log(Level.FINE, String.format("ETSRegEncode %04x connected, %04x addressed, reg0: %04x",
				getEFADCConnectedMask(), addressed, reg0));

		buffer.writeShort(reg0);

		// Encode EFADC registers

		// Find the first addressed EFADC, this is the set that will be sent to all addressed adcs
		assert addressed > 0;
		int idx = 0;
		while ((addressed & 0x1) == 0) {
			addressed >>= 1;
			++idx;
		}

		try {
			ETS_EFADC_RegisterSet adcReg = getADCRegisters(idx+1);

			ByteBuf adcBuf = adcReg.encode();

			buffer.writeBytes(adcBuf);

		} catch (EFADC_InvalidADCException e) {
			e.printStackTrace();
			Logger.getGlobal().severe("Invalid adc index in encode(), idx must have origin 1");
			return null;
		}

		// Encode coincidence table entries
		// 64 bit field widths
		ByteBuf matrixBuf = MatrixRegisterEncoder.encode(m_ORTable, m_ANDTable, 64);

		buffer.writeBytes(matrixBuf);

		//
		// Hardcode these until MatrixRegisterEncoder is fixed
		//

		// HitOut pulse width, registers 129-135
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);
		buffer.writeShort(0x3333);

		// HitOut pulse delay, registers 136-142
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);
		buffer.writeShort(0x1111);

		// Encode NYI, 143-147
		buffer.writeShort(0x0000);
		buffer.writeShort(0x0000);
		buffer.writeShort(0x0000);
		buffer.writeShort(0x0000);
		buffer.writeShort(0x0000);

		// Encode reserved stuff, 148-151
		buffer.writeShort(0x00ff);	// Are these values ignored? Why are they specified in the manual?
		buffer.writeShort(0x000f);
		buffer.writeShort(0x0000);
		buffer.writeShort(0x0000);

		return buffer;
	}

	@Override
	public boolean decode(ByteBuf frame) {

		// ETS_FrameDecoder has already verified that we have the required byte count

		Logger.getGlobal().info("ETS_RegisterSet decode");
		if (!decode(frame, NUM_READ_REGS)) {
			Logger.getGlobal().warning("Error parsing config registers");
			return false;
		}

		status[0] = frame.readUnsignedShort();	// Firmware version
		status[1] = frame.readUnsignedShort();	// efadc active mask
		status[2] = frame.readUnsignedShort();
		status[3] = frame.readUnsignedShort();
		status[4] = frame.readUnsignedShort();
		status[5] = frame.readUnsignedShort();
		status[6] = frame.readUnsignedShort();	// temp
		status[7] = frame.readUnsignedShort();	// MAC 31-16
		status[8] = frame.readUnsignedShort();	// MAC 15-0

		for (int i = 0; i < NUM_STATUS; i++)
			Logger.getGlobal().log(Level.FINE, String.format("[S%d] %04x", i+1, status[i]));

		lastUpdated = System.currentTimeMillis();

		return true;
	}


	public String toString() {
		StringBuffer strB = new StringBuffer("ETS Register Set: ");

		strB.append(String.format("Version: %04x ", status[0]));
		strB.append(String.format("EFADC_Mask: %04x", getEFADCConnectedMask()));
		//strB.append(String.format("Accepted Triggers: %d\n", acceptedTrigs));
		//strB.append(String.format("Missed Triggers: %d\n", missedTrigs));
		//strB.append(String.format("Something Else: %04X\n", unknown));
		//strB.append(String.format("FPGA Die Temp (C): %.2f\n", fpgaTemp));

		return strB.toString();
	}
}
