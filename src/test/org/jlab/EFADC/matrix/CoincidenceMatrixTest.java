package org.jlab.EFADC.matrix;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class CoincidenceMatrixTest {

	static void SetIdentityMatrix(int numDet, CoincidenceMatrix matrix) {

		for (int i = 0; i < numDet; i++) {
			for (int j = 0; j < numDet; j++) {
				matrix.setCoincident(i, j, i == j, false);
			}
		}
	}

	@Test
	public void setCoincident() {
		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		SetIdentityMatrix(12, andTable);

		// Expected mapping

		byte reg21 = 0b00000000;
		byte reg22 = 0b00000000;
		byte reg23 = 0b00000000;
		byte reg24 = 0b00000001;

		for (int i = 0; i < 12; i++) {
			BigInteger module = andTable.getEntry(i);

			for (int j = 0; j < 12; j++) {
				if (i == j) {
					// a module should be in coincidence with itself
					assertEquals(true, module.testBit(i));
				} else {
					// and none others
					assertEquals(false, module.testBit(j));
				}
			}
		}
	}
}