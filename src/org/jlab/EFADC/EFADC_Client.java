//
//  EFADC_Client.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jlab.EFADC.command.Command;
import org.jlab.EFADC.matrix.CoincidenceMatrix;
import java.util.logging.Logger;

import static org.jlab.EFADC.RegisterSet.*;


public class EFADC_Client implements Client {

	RegisterSet					m_Registers = null;

	boolean						m_AggregatorEnable = false;
	boolean						m_AcquisitionActive = false;

	protected NetworkClient		m_NetworkClient;


	public EFADC_Client() {
		m_Registers = new EFADC_RegisterSet();

		m_NetworkClient = new NetworkClient();

		m_NetworkClient.setInterCommandDelay(50);
	}

	public EFADC_Client(NetworkClient nc) {
		m_Registers = new EFADC_RegisterSet();

		m_NetworkClient = nc;

		m_NetworkClient.setInterCommandDelay(50);
	}

	
	
	public EFADC_Client(String address, int port, boolean enableIdleTimer) throws Exception {
		this();

		m_NetworkClient.setAddress(address, port);

		if (enableIdleTimer) {
			m_NetworkClient.initIdleHandler();

		} else
			Logger.getLogger("global").info("Idle handler disabled");


		// TODO add accessors to modify tree size and handler
		//m_EventAggregator = new EFADC_EventAggregator(10);
	}

	public NetworkClient networkClient() {
		return m_NetworkClient;
	}


	void flushAggregateBuffer() {
		//m_EventAggregator.flush();
	}


	public void setAggregatorEnable(boolean val) {
		m_AggregatorEnable = val;
	}

	public boolean getCollectState() {
		return m_AcquisitionActive;
	}




	public void setRegisterSet(RegisterSet regs) {

		if (m_Registers instanceof EFADC_RegisterSet && regs instanceof CMP_RegisterSet) {
			// Replace if we detected CMP
			m_Registers = regs;

		} else {
			// Don't replace entirely because we'll lose information
			m_Registers.update(regs);
		}
	}

	/**
	 * Get last received register set
	 * @return RegisterSet
	 */
	public RegisterSet getRegisterSet() {
		return m_Registers;
	}


	/**
	 * Set ADC to accept incoming positive pulses.  User should make sure DAC values are set around 500.
	 * Note: This currently sends these commands to ALL EFADCs in the DAQ network
	 */
	public void SetADCPositive() {
		//Set bit 15 of register2 to 1 to address all ADC's
		m_Registers.setRegister(2, m_Registers.getRegister(2) | 0x8000);

		//Write 0x1404 to all ADCs
		m_Registers.setRegister(20, 0x1401);

		SendSetRegisters(1);
		SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Write 0xFF01 to all ADCs
		m_Registers.setRegister(20, 0xFF01);

		SendSetRegisters(1);
		SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Set ADC to accept incoming negative pulses.  User should make sure DAC values are set around 3300.
	 * Note: This currently sends these commands to ALL EFADCs in the DAQ network
	 */
	public void SetADCNegative() {
		//Set bit 15 of register2 to 1 to address all ADC's
		m_Registers.setRegister(REG_2, m_Registers.getRegister(REG_2) | 0x8000);

		//Write 0x1400 to all ADCs
		m_Registers.setRegister(REG_20, 0x1400);

		SendSetRegisters(1);
		//SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Write 0xFF01 to all ADCs
		m_Registers.setRegister(REG_20, 0xFF01);

		SendSetRegisters(1);
		//SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public void SetCoincidenceWindowWidth(int width) {
		EFADC_RegisterSet adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = cmpReg.getADCRegisters(i);

					adcReg.setCoincidenceWindowWidth(width);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;

			adcReg.setCoincidenceWindowWidth(width);
		}
	}


	public void SetANDCoincident(int detA, int detB, boolean val, boolean reverse) {

		/*
		CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

		CoincidenceMatrix matrix = (CoincidenceMatrix)cmpReg.getMatrix(CMP_RegisterSet.MatrixType.AND);

		matrix.setCoincident(detA, detB, val, false);
		*/

		SetCoincident(detA, detB, val, reverse, CMP_RegisterSet.MatrixType.AND);
	}

	public void SetORCoincident(int detA, int detB, boolean val, boolean reverse) {
		CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

		CoincidenceMatrix matrix = (CoincidenceMatrix)cmpReg.getMatrix(CMP_RegisterSet.MatrixType.OR);

		matrix.setCoincident(detA, detB, val, false);

	}


	public void SetCoincident(int detA, int detB, boolean val, boolean reverse, CMP_RegisterSet.MatrixType type) {

		CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

		CoincidenceMatrix matrix = (CoincidenceMatrix)cmpReg.getMatrix(type);

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
	public void SetIdentityMatrix() {
		EFADC_RegisterSet adcReg;
		int numDet = 4;

		if (IsCMP()) {
			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			numDet *= cmpReg.getADCCount();
		}

		for (int i = 0; i < numDet; i++) {
			for (int j = 0; j < numDet; j++) {
				SetCoincident(i, j, i == j, false, CMP_RegisterSet.MatrixType.AND);
			}
		}

	}

	// Set coincidence matrix to all zero
	public void SetZeroMatrix() {
		EFADC_RegisterSet adcReg;
		int numDet = 4;

		if (IsCMP()) {
			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			numDet *= cmpReg.getADCCount();
		}

		for (int i = 0; i < numDet; i++) {
			for (int j = 0; j < numDet; j++) {
				SetCoincident(i, j, false, false, CMP_RegisterSet.MatrixType.AND);
				SetCoincident(i, j, false, false, CMP_RegisterSet.MatrixType.OR);
			}
		}
	}


	/**
	 * Set Integration Window for all EFADCs
	 * @param window
	 */
	public void SetIntegrationWindow(int window) {
		EFADC_RegisterSet adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = cmpReg.getADCRegisters(i);

					adcReg.setIntegrationWindow(window);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;

			adcReg.setIntegrationWindow(window);
		}
	}


	/**
	 * Set Mode for all EFADCs
	 * @param mode
	 */
	public void SetMode(int mode) {
		EFADC_RegisterSet adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = cmpReg.getADCRegisters(i);

					adcReg.setMode(mode);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;

			adcReg.setMode(mode);
		}
	}


	/**
	 * Set NSB for all EFADCs
	 * @param window
	 */
	public void SetNSB(int window) {
		EFADC_RegisterSet adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = cmpReg.getADCRegisters(i);

					adcReg.setNSB(window);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;

			adcReg.setNSB(window);
		}
	}


	/**
	 * Set Integration Window for specific EFADC
	 * @param adc Range 1 to # ADC's
	 * @param window
	 */
	public void SetIntegrationWindow(int adc, int window) {
		EFADC_RegisterSet adcReg = null;

		if (IsCMP()) {
			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			if (adc == 0) {
				// Special case to select all ADC's

			} else {

				try {
					adcReg = cmpReg.getADCRegisters(adc);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + adc);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;
		}

		if (adcReg == null) {
			Logger.getLogger("global").warning("NULL Register Set in SetIntegrationWindow");
			return;
		}

		adcReg.setIntegrationWindow(window);
	}


	/**
	 * Enables self triggering for all EFADC register sets
	 * @param enable
	 * @param value
	 */
	public void SetSelfTrigger(boolean enable, int value) {
		EFADC_RegisterSet adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = cmpReg.getADCRegisters(i);

					adcReg.setSelfTrigger(enable, value);

					Logger.getLogger("global").info(String.format("Setting self trigger ADC %d: %s", i, enable ? "true" : "false"));

				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;

			adcReg.setSelfTrigger(enable, value);
		}
	}


	/**
	 * Set Sync for all EFADCs
	 * @param val
	 */
	@Deprecated
	public void SetSync(boolean val) {
		EFADC_RegisterSet adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = cmpReg.getADCRegisters(i);

					adcReg.setSync(val);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;

			adcReg.setSync(val);
		}
	}


	public void SetThreshold(int det, int thresh) {

		EFADC_RegisterSet adcReg = null;
		int adcDet = det;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			int adc = (int)(det / 4.0) + 1;

			// Address proper module in each efadc
			adcDet = det - (adc - 1) * 4;

			try {
				adcReg = cmpReg.getADCRegisters(adc);

			} catch (EFADC_InvalidADCException e) {
				Logger.getLogger("global").warning("Invalid ADC Selection: " + adc);
			}

			Logger.getLogger("global").info(String.format("Setting CMP Threshold, chan %d, adc %d, det %d, value %d", det, adc, adcDet, thresh));

		} else {
			Logger.getLogger("global").info("Setting EFADC Threshold");
			adcReg = (EFADC_RegisterSet)m_Registers;
		}

		if (adcReg == null) {
			Logger.getLogger("global").warning("NULL Register Set in SetThreshold");
			return;
		}

		adcReg.setThreshold(adcDet, thresh);
	}


	@Deprecated
	public void SetCoincidenceTableEntry(int type) {
	
		if (type == 0) {
		
			//This will set each PMT in coincidence with itself
			m_Registers.setRegister(9, 0x2010);
			m_Registers.setRegister(10, 0x8040);
			
		} else if (type == 1) {
		
			//This configures a 1 to 3 coincidence mode
			m_Registers.setRegister(9, 0x010E);
			m_Registers.setRegister(10, 0x0408);
			
		} else if (type == 2) {
		
			//This configures a 2 exclusive pair coincidence mode
			m_Registers.setRegister(9, 0x0102);
			m_Registers.setRegister(10, 0x0408);
			
		} else if (type == 3) {
		
			//This configures a 4 detector ring mode
			m_Registers.setRegister(9, 0x0D0E);
			m_Registers.setRegister(10, 0x070B);

		}
	}


	/**
	 * Send register values to the EFADC/CMP
	 * We have to manage the CMP registers in a tricky way since a completely new encoded packet is required to set each EFADC register set
	 * @return
	 */
	public boolean SendSetRegisters(int adc) {

		if (m_Registers instanceof CMP_RegisterSet) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			//for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {

				try {
					cmpReg.selectADC(adc);

					m_NetworkClient.SendCommand(m_Registers);

					Thread.sleep(50);

				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + adc);
					return false;

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			//}

			return true;

		} else
			//We're either sending same register set to all EFADC's on a CMP, or just EFADC registers to a standalone unit
			return m_NetworkClient.SendCommand(m_Registers);
	}



	public boolean SendLCDData(int line, String text) {
		return m_NetworkClient.SendCommand(EFADC_LCDMessage.encode(line, text));
	}


	/**
	 * Set all DAC values for a specific efadc register set.  Specific EFADC must be selected beforehand if sending to a CMP.
	 * @param values DAC Values
	 * @param reg EFADC Registers
	 * @return
	 */
	protected boolean sendDACValues(int[] values, EFADC_RegisterSet reg, int adc) {
		for (int i = 0; i < 16; i++) {
			reg.setBiasDAC(i, values[i]);
			if (!SendSetRegisters(adc))
				return false;

		}
		return true;
	}

	public boolean IsCMP() {
		if (m_Registers != null && m_Registers instanceof CMP_RegisterSet)
			return true;

		return false;
	}

	
	public boolean SetDACValues(int[] values) {

		if (m_Registers instanceof EFADC_RegisterSet) {

			return sendDACValues(values, (EFADC_RegisterSet)m_Registers, 0);

		} else {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			int adcCount = cmpReg.getADCCount();

			// We need to send the ENTIRE register set a total of 16 * adcCount() times because of the way the registers were implemented in firmware...
			if (values.length < adcCount * 16) {
				Logger.getLogger("global").severe(String.format("DAC Values array needs to be %d, currently only %d", adcCount * 16, values.length));
				return false;
			}

			int[] ival = new int[16];	// Individual dac values per efadc

			// Individually address each efadc in the cmp register block
			for (int i = 0; i < adcCount; i++) {

				int selADC = i + 1;	// +1 here because a subtraction occurs internally, and a selected adc value of 0 sends to all efadcs

				try {
					EFADC_RegisterSet adcReg = cmpReg.getADCRegisters(selADC);

					//cmpReg.selectADC(selADC);

					System.arraycopy(values, 16*i, ival, 0, 16);

					if (!sendDACValues(ival, adcReg, selADC)) {
						Logger.getLogger("global").warning(String.format("Failed setting regs for adc %d/%d", i, adcCount));
						return false;
					}

				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + selADC);
				}

			}
		}
		
		return true;
	}



	public boolean StartCollection() throws Exception {

		if (m_AggregatorEnable) {
			m_NetworkClient.initReadTimeoutHandler();
		}

		m_AcquisitionActive = true;

		return m_NetworkClient.SendCommand(Command.StartCollection());
	}


	public boolean StopCollection() throws Exception {

		m_NetworkClient.removeTimeoutHandler();

		m_AcquisitionActive = false;

		return m_NetworkClient.SendCommand(Command.StopCollection());
	}

	public void SendSync() {
		m_NetworkClient.SendCommand(Command.SendSync());
	}


	public boolean ReadRegisters() {
		return m_NetworkClient.SendCommand(Command.ReadRegisters());
	}

}