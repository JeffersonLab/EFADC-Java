package org.jlab.EFADC.matrix;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.RegisterSet;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class MatrixRegisterEncoderTest {

	@Test
	public void testRegisterEncode() {

		int numDetectors = 12;

		//ETS
		// must specify size 27 even tho the firmware only supports 12? now
		CoincidenceMatrix orTable = MatrixFactory.newCoincidenceMatrix(27, 108);
		CoincidenceMatrix andTable = MatrixFactory.newCoincidenceMatrix(27, 108);

		CoincidenceMatrixTest.SetIdentityMatrix(numDetectors, andTable);

		ChannelBuffer encodedBuf = MatrixRegisterEncoder.encode(orTable, andTable, 64);

		// encoded buffer should start with register 21 (for ETS)
		for (int i = 0; i < numDetectors; i++) {
			long val = encodedBuf.readLong();

			assertEquals(1 << i, val);
		}
	}
}