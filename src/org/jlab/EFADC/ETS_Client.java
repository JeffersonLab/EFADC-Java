package org.jlab.EFADC;

import org.jlab.EFADC.command.Command;
import org.jlab.EFADC.matrix.CoincidenceMatrix;

import java.util.List;
import java.util.logging.Logger;

import static org.jlab.EFADC.RegisterSet.REG_2;
import static org.jlab.EFADC.RegisterSet.REG_20;

/**
 * Created by john on 6/13/17.
 */
public class ETS_Client extends EFADC_Client implements Client {

	ETS_RegisterSet m_Registers;

	private static final int POLARITY_NEGATIVE = 0;
	private static final int POLARITY_POSITIVE = 1;
	private static final int POLARITY_NEGATIVE_MASK = 0x1400;
	private static final int POLARITY_POSITIVE_MASK = 0x1401;


	public ETS_Client() {
		m_Registers = new ETS_RegisterSet(this);

		m_NetworkClient = new NetworkClient();

		m_NetworkClient.setInterCommandDelay(50);
	}

	public ETS_Client(NetworkClient nc) {
		m_Registers = new ETS_RegisterSet(this);

		m_NetworkClient = nc;

		m_NetworkClient.setInterCommandDelay(50);
	}

	public boolean ReadRegisters() {
		return m_NetworkClient.SendCommand(Command.ReadETSRegisters());
	}

	public boolean ReadEFADCRegisters() {
		return m_NetworkClient.SendCommand(Command.ReadRegisters());
	}


	@Override
	public void setRegisterSet(RegisterSet regs) {
		Logger.getGlobal().warning("Got Unknown RegisterSet in ETS_Client setRegisterSet");
	}


	public void setRegisterSet(ETS_EFADC_RegisterSet regs) {
		Logger.getGlobal().info("setRegisterSet ETS_EFADC");

		// Assuming m_Registers is already ETS_RegisterSet
		m_Registers.setEFADCRegisterSet(regs);
	}

	public void setRegisterSet(ETS_RegisterSet regs) {
		Logger.getGlobal().info("setRegisterSet ETS: " + regs);

		// Don't replace entirely because we'll lose information
		// ??
		//m_Registers.update(regs);
		m_Registers = regs;
	}


	/**
	 * @param adc
	 * @return
	 */
	@Override
	public boolean SendSetRegisters(int adc) {

		// Get connected efadc's
		int mask = m_Registers.getEFADCMask();
		int adcBit = 1 << (adc - 1);

		// 0 value will address all efadcs
		if (adc == 0)
			adcBit = mask;

		Logger.getGlobal().info(String.format("SendSetRegisters(%d) mask %04x, adcBit %04x",
				adc, mask, adcBit));

		try {
			m_Registers.setEFADC_Mask(mask | adcBit);

			m_NetworkClient.SendCommand(m_Registers);

			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}


	/**
	 * Set ADC to accept incoming positive pulses.  User should make sure DAC values are set around 500.
	 * Note: This currently sends these commands to ALL EFADCs in the DAQ network
	 */
	@Override
	public void SetADCPositive() {
		setADCPolarity(POLARITY_POSITIVE);
	}


	/**
	 * Set ADC to accept incoming negative pulses.  User should make sure DAC values are set around 3300.
	 * Note: This currently sends these commands to ALL EFADCs in the DAQ network
	 */
	@Override
	public void SetADCNegative() {
		setADCPolarity(POLARITY_NEGATIVE);
	}


	/**
	 * Set ADC to accept incoming negative pulses.  User should make sure DAC values are set around 3300.
	 * Note: This currently sends these commands to ALL EFADCs in the DAQ network
	 * @param polarity 0 - Set polarity negative, 1 - set polarity positive
	 */
	private void setADCPolarity(int polarity) {

		int mask = m_Registers.getEFADCMask();

		// Individually address each connected & active EFADC
		// A set to #0 sets ALL EFADC register values
		for (int adc = 1; adc < 9; adc++, mask >>= 1) {
			boolean active = (mask & 0x1) == 1;

			if (active) {
				try {
					ETS_EFADC_RegisterSet adcReg = m_Registers.getADCRegisters(adc);

					if (adcReg == null) {
						Logger.getGlobal().warning("ADC registers null, probably uninitialized...");
						throw new EFADC_InvalidADCException();
					}

					//Set bit 15 of register2 to 1 to address all ADC's
					adcReg.setRegister(REG_2, adcReg.getRegister(REG_2) | 0x8000);

					//Write 0x1400 to all ADCs
					adcReg.setRegister(REG_20, (polarity == POLARITY_NEGATIVE ? POLARITY_NEGATIVE_MASK : POLARITY_POSITIVE_MASK));

					SendSetRegisters(adc);

					//Write 0xFF01 to all ADCs
					adcReg.setRegister(REG_20, 0xFF01);

					SendSetRegisters(adc);

				} catch (EFADC_InvalidADCException e) {
					Logger.getGlobal().severe("Invalid ADC Selection: " + adc);
				}
			}
		}
	}


	@Override
	public void SetCoincidenceWindowWidth(int width) {

		int mask = m_Registers.getEFADCMask();

		// Individually address each connected & active EFADC
		// A set to #0 sets ALL EFADC register values
		for (int adc = 1; adc < 9; adc++, mask >>= 1) {
			boolean active = (mask & 0x1) == 1;

			if (active) {
				try {
					ETS_EFADC_RegisterSet adcReg = m_Registers.getADCRegisters(adc);

					if (adcReg == null) {
						Logger.getGlobal().warning("ADC registers null, probably uninitialized...");
						throw new EFADC_InvalidADCException();
					}

					adcReg.setCoincidenceWindowWidth(width);
				} catch (EFADC_InvalidADCException e) {
					Logger.getGlobal().severe("Invalid ADC Selection: " + adc);
				}
			}
		}

	}


	@Override
	public void SetCoincident(int detA, int detB, boolean val, boolean reverse, RegisterSet.MatrixType type) {

		CoincidenceMatrix matrix = (CoincidenceMatrix)m_Registers.getMatrix(type);

		matrix.setCoincident(detA, detB, val, reverse);

		/*
		Logger.getLogger("global").info(String.format("SetCoincident(%d, %d, %s, %s, Opp: %s)",
				detA, detB,
				val ? "ON" : "OFF",
				type == CMP_RegisterSet.MatrixType.AND ? "AND" : "OR",
				reverse ? "YES" : "NO"));
		*/
	}


	// Set each module in coincidence with itself
	@Override
	public void SetIdentityMatrix() {
		int numDet = 4;

		numDet *= m_Registers.getADCCount();

		for (int i = 0; i < numDet; i++) {
			for (int j = 0; j < numDet; j++) {
				SetCoincident(i, j, i == j, false, RegisterSet.MatrixType.AND);
			}
		}
	}


	/**
	 * Set Integration Window for all EFADCs
	 * @param window
	 */
	@Override
	public void SetIntegrationWindow(int window) {
		ETS_RegisterSet etsReg = (ETS_RegisterSet)m_Registers;

		List<ETS_EFADC_RegisterSet> adcRegs = etsReg.getADCRegisters();

		for (ETS_EFADC_RegisterSet adcReg : adcRegs) {
			if (adcReg != null) {
				adcReg.setIntegrationWindow(window);
			}
		}
	}


	/**
	 * Set NSB for all EFADCs
	 * @param window
	 */
	@Override
	public void SetNSB(int window) {
		ETS_EFADC_RegisterSet adcReg;

		ETS_RegisterSet etsReg = (ETS_RegisterSet)m_Registers;


		for (int i = 1; i < Integer.bitCount(etsReg.getEFADCMask()) + 1; i++) {
			try {
				adcReg = etsReg.getADCRegisters(i);

				if (adcReg != null)
					adcReg.setNSB(window);
			} catch (EFADC_InvalidADCException e) {
				Logger.getGlobal().warning("Invalid ADC Selection: " + i);
			}
		}
	}


	@Override
	public boolean SetDACValues(int[] values) {

		int mask = m_Registers.getEFADCMask();

		int adcCount = Integer.bitCount(mask);

		// We need to send the ENTIRE register set a total of 16 * adcCount() times because of the way the registers were implemented in firmware...
		if (values.length < adcCount * 16) {
			Logger.getGlobal().severe(String.format("DAC Values array needs to be %d, currently only %d", adcCount * 16, values.length));
			return false;
		}

		int[] ival = new int[16];	// Individual dac values per efadc

		// Individually address each connected & active EFADC
		// A set to #0 sets ALL EFADC register values
		for (int adc = 1; adc < 9; adc++, mask >>= 1) {
			boolean active = (mask & 0x1) == 1;

			if (active) {

				try {
					ETS_EFADC_RegisterSet adcReg = m_Registers.getADCRegisters(adc);

					if (adcReg == null) {
						Logger.getGlobal().warning("ADC registers null, probably uninitialized...");
						throw new EFADC_InvalidADCException();
					}

					System.arraycopy(values, 16*adc, ival, 0, 16);

					if (!sendDACValues(ival, adcReg, adc)) {
						Logger.getGlobal().warning(String.format("Failed setting regs for adc %d/%d", adc, adcCount));
						return false;
					}

				} catch (EFADC_InvalidADCException e) {
					Logger.getGlobal().severe("Invalid ADC Selection: " + adc);
					return false;
				}
			}
		}

		return true;
	}


	@Override
	public void SetThreshold(int det, int thresh) {

		// Address efadc by channel
		int adc = (int)(det / 4.0) + 1;

		// Address module in specific efadc
		int adcDet = det - (adc - 1) * 4;

		try {
			ETS_EFADC_RegisterSet adcReg = ((ETS_RegisterSet)m_Registers).getADCRegisters(adc);

			if (adcReg == null) {
				Logger.getGlobal().warning("NULL Register Set in SetThreshold");
				return;
			}

			Logger.getGlobal().info(String.format("Setting ETS Threshold, chan %d, adc %d, det %d, value %d", det, adc, adcDet, thresh));

			adcReg.setThreshold(adcDet, thresh);

		} catch (EFADC_InvalidADCException e) {
			Logger.getGlobal().warning("Invalid ADC Selection: " + adc);
		}

	}
}
