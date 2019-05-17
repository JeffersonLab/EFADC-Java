package org.jlab.EFADC;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ETS_RegisterSetTest {

	@Test
	public void initTables() {
	}

	@Test
	public void client() {
	}

	@Test
	public void update() {
	}

	@Test
	public void update1() {
	}

	@Test
	public void testSetEFADCRegisterSet() {
		ETS_Client client = new ETS_Client();

		ETS_RegisterSet regs = new ETS_RegisterSet(client);

		ETS_EFADC_RegisterSet adcRegs = new ETS_EFADC_RegisterSet(client, 1);

		regs.setEFADCRegisterSet(adcRegs);

		ETS_EFADC_RegisterSet actual = null;

		try {
			actual = regs.getADCRegisters(1);
		} catch (EFADC_InvalidADCException e) {
			fail("Unexpected Exception");
		}

		assertNotNull(actual);

		assertEquals(adcRegs, actual);
	}

	@Test
	public void testSetADCRegisters() {
		ETS_Client client = new ETS_Client();

		ETS_RegisterSet regs = new ETS_RegisterSet(client);

		ETS_EFADC_RegisterSet adcRegs = new ETS_EFADC_RegisterSet(client, 1);

		//
		try {
			regs.setADCRegisters( 1, adcRegs);
		} catch (EFADC_InvalidADCException e) {
			fail("Unhandled Exception in setADCRegisters");
		}

		ETS_EFADC_RegisterSet actual = null;

		try {
			actual = regs.getADCRegisters(1);
		} catch (EFADC_InvalidADCException e) {
			fail("Unexpected Exception in getADCRegisters");
		}

		assertNotNull(actual);

		assertEquals(adcRegs, actual);

		// Testing with out of range module ids
		boolean exception = false;
		try {
			regs.setADCRegisters( 0, adcRegs);
		} catch (EFADC_InvalidADCException e) {
			exception = true;
		}

		assertTrue(exception);

	}

	@Test
	public void getADCCount() {
	}

	@Test
	public void getEFADCMask() {
	}

	@Test
	public void setEFADC_Mask() {
	}

	@Test
	public void getTemp() {
	}

	@Test
	public void getVersion() {
	}

	@Test
	public void testGetADCRegisters_Multi() {
		ETS_Client client = new ETS_Client();

		ETS_RegisterSet regs = new ETS_RegisterSet(client);

		List<ETS_EFADC_RegisterSet> regList = regs.getADCRegisters();

		assertEquals(8, regList.size());

		List<ETS_EFADC_RegisterSet> regListExpected = new ArrayList<>(8);

		regListExpected.add(new ETS_EFADC_RegisterSet(client, 1));
		regListExpected.add(new ETS_EFADC_RegisterSet(client, 2));
		regListExpected.add(new ETS_EFADC_RegisterSet(client, 3));
		regListExpected.add(new ETS_EFADC_RegisterSet(client, 4));
		regListExpected.add(new ETS_EFADC_RegisterSet(client, 5));
		regListExpected.add(new ETS_EFADC_RegisterSet(client, 6));
		regListExpected.add(new ETS_EFADC_RegisterSet(client, 7));
		regListExpected.add(new ETS_EFADC_RegisterSet(client, 8));

		try {
			regs.setADCRegisters(1, regListExpected.get(0));
			regs.setADCRegisters(2, regListExpected.get(1));
			regs.setADCRegisters(3, regListExpected.get(2));
			regs.setADCRegisters(4, regListExpected.get(3));
			regs.setADCRegisters(5, regListExpected.get(4));
			regs.setADCRegisters(6, regListExpected.get(5));
			regs.setADCRegisters(7, regListExpected.get(6));
			regs.setADCRegisters(8, regListExpected.get(7));

		} catch (EFADC_InvalidADCException e) {
			fail("Unxpectd exception in setADCRegisters in wrong test");
		}

		assertThat(regList, is(regListExpected));

	}

	@Test
	public void testGetADCRegisters_Single() {
		ETS_Client client = new ETS_Client();

		ETS_RegisterSet regs = new ETS_RegisterSet(client);

		ETS_EFADC_RegisterSet adcRegs = new ETS_EFADC_RegisterSet(client, 1);

		//
		try {
			regs.setADCRegisters( 1, adcRegs);
		} catch (EFADC_InvalidADCException e) {
			fail("Unhandled Exception in setADCRegisters");
		}

		ETS_EFADC_RegisterSet actual = null;

		try {
			actual = regs.getADCRegisters(1);
		} catch (EFADC_InvalidADCException e) {
			fail("Unexpected Exception in getADCRegisters");
		}

		assertNotNull(actual);

		assertEquals(adcRegs, actual);

		// Testing with out of range module ids
		// should get an exception here
		boolean exception = false;
		try {
			regs.getADCRegisters( 0);
		} catch (EFADC_InvalidADCException e) {
			exception = true;
		}

		assertTrue(exception);

		exception = false;
		try {
			regs.getADCRegisters( 8);
		} catch (EFADC_InvalidADCException e) {
			exception = true;
		}

		assertTrue(exception);

		// and a default register sets here
		for (int n = 2; n < 8; n++) {

			ETS_EFADC_RegisterSet regsN = null;
			try {
				regsN = regs.getADCRegisters(n);
			} catch (EFADC_InvalidADCException e) {
				fail("Unexpected exception in getADCRegisters");
			}

			assertNotNull(regsN);

			// each register set must have assigned module id
			assertEquals(n, regsN.module());
		}


	}

	@Test
	public void testEncode() {

		ETS_Client client = new ETS_Client();

		ETS_RegisterSet regs = new ETS_RegisterSet(client);

		regs.initTables();

		ETS_EFADC_RegisterSet adcRegs = new ETS_EFADC_RegisterSet(client, 1);

		regs.setEFADCRegisterSet(adcRegs);

		regs.setEFADC_Mask(0x01);

		ByteBuf buf = regs.encode();

		if (buf == null) {
			fail("encode retured null");
		}

		assertEquals(ETS_RegisterSet.DATA_SIZE_WRITE_BYTES + 5, buf.readableBytes());

	}

	@Test
	public void testDecode() {
	}

	@Test
	public void testToString() {
	}
}