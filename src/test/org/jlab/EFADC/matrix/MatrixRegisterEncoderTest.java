package org.jlab.EFADC.matrix;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.RegisterSet;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class MatrixRegisterEncoderTest {

	@Test
	public void testRegisterEncode_Identity() {

		int numDetectors = 12;

		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		CoincidenceMatrix.SetIdentityMatrix(numDetectors, andTable);

		ChannelBuffer encodedBuf = MatrixRegisterEncoder.encode(orTable, andTable, 64);

		// encoded buffer should start with register 21 (for ETS)
		for (int i = 0; i < numDetectors; i++) {
			int ORval = encodedBuf.readInt();

			assertEquals(0, ORval);

			int ANDval = encodedBuf.readInt();

			assertEquals(1 << i, ANDval);
		}
	}

	@Test
	public void testRegisterEncode_Single_AND() {

		int numDetectors = 12;

		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		andTable.setCoincident(0, 1, true, false);

		ChannelBuffer encodedBuf = MatrixRegisterEncoder.encode(orTable, andTable, 64);

		// encoded buffer should start with register 21 (for ETS)
		for (int i = 0; i < numDetectors; i++) {
			int ORval = encodedBuf.readInt();

			assertEquals(0, ORval);

			int ANDval = encodedBuf.readInt();

			// if this is det 0 it should be coincident with det 1
			if (i == 0 ) {
				assertEquals(1 << 1, ANDval);
			} else if (i == 1) {
				// this will pass if symmetry is false
				assertNotEquals(1 << 0, ANDval);
			}
		}
	}

	@Test
	public void testRegisterEncode_Single_AND_Symmetric() {

		int numDetectors = 12;

		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		andTable.setCoincident(0, 1, true, true);

		ChannelBuffer encodedBuf = MatrixRegisterEncoder.encode(orTable, andTable, 64);

		// encoded buffer should start with register 21 (for ETS)
		for (int i = 0; i < numDetectors; i++) {
			int ORval = encodedBuf.readInt();

			assertEquals(0, ORval);

			int ANDval = encodedBuf.readInt();

			// if this is det 0 it should be coincident with det 1
			if (i == 0 ) {
				assertEquals(1 << 1, ANDval);
			} else if (i == 1) {
				// this will pass if symmetry is true
				assertEquals(1 << 0, ANDval);
			}
		}
	}

	@Test
	public void testRegisterEncode_Double_AND() {

		int numDetectors = 12;

		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		andTable.setCoincident(0, 2, true, false);
		andTable.setCoincident(1, 3, true, false);

		ChannelBuffer encodedBuf = MatrixRegisterEncoder.encode(orTable, andTable, 64);

		// encoded buffer should start with register 21 (for ETS)
		for (int i = 0; i < numDetectors; i++) {
			int ORval = encodedBuf.readInt();

			assertEquals(0, ORval);

			int ANDval = encodedBuf.readInt();

			// if this is det 0 it should be coincident with det 2
			if (i == 0 ) {
				assertEquals(1 << 2, ANDval);
			} else if (i == 1) {

				assertEquals(1 << 3, ANDval);
			} else if (i == 2) {
				// this will pass if symmetry is true
				assertNotEquals(1 << 0, ANDval);
			} else if (i == 3) {
				// this will pass if symmetry is true
				assertNotEquals(1 << 1, ANDval);
			}
		}
	}

	@Test
	public void testRegisterEncode_Double_AND_Symmetric() {

		int numDetectors = 12;

		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		andTable.setCoincident(0, 2, true, true);
		andTable.setCoincident(1, 3, true, true);

		ChannelBuffer encodedBuf = MatrixRegisterEncoder.encode(orTable, andTable, 64);

		// encoded buffer should start with register 21 (for ETS)
		for (int i = 0; i < numDetectors; i++) {
			int ORval = encodedBuf.readInt();

			assertEquals(0, ORval);

			int ANDval = encodedBuf.readInt();

			// if this is det 0 it should be coincident with det 2
			if (i == 0 ) {
				assertEquals(1 << 2, ANDval);
			} else if (i == 1) {

				assertEquals(1 << 3, ANDval);
			} else if (i == 2) {
				// this will pass if symmetry is true
				assertEquals(1 << 0, ANDval);
			} else if (i == 3) {
				// this will pass if symmetry is true
				assertEquals(1 << 1, ANDval);
			}
		}
	}

	@Test
	public void testRegisterEncode_2x2_AND_Symmetric() {

		int numDetectors = 12;

		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		andTable.setCoincident(0, 2, true, true);
		andTable.setCoincident(0, 3, true, true);
		andTable.setCoincident(1, 2, true, true);
		andTable.setCoincident(1, 3, true, true);

		ChannelBuffer encodedBuf = MatrixRegisterEncoder.encode(orTable, andTable, 64);

		// encoded buffer should start with register 21 (for ETS)
		for (int i = 0; i < numDetectors; i++) {
			int ORval = encodedBuf.readInt();

			assertEquals(0, ORval);

			int ANDval = encodedBuf.readInt();

			if (i == 0 || i == 1) {
				// det 0 should be coincident with 2 and 3
				assertEquals((1 << 2 | 1 << 3), ANDval);
			} else if (i == 2 || i == 3) {
				// this will pass if symmetry is true
				assertEquals((1 << 0 | 1 << 1), ANDval);
			}
		}
	}
}