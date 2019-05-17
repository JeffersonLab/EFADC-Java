package org.jlab.EFADC;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import static org.junit.Assert.*;

public class ETS_EFADC_RegisterSetTest {

	@Test
	public void testEncode() {

		ETS_Client client = new ETS_Client();

		ETS_EFADC_RegisterSet regs = new ETS_EFADC_RegisterSet(client, 1);

		ByteBuf buf = regs.encode();

		int[] expected = new int[] {
				0x000a,
				0x0016,
				0x0180,
				0x0bd8,
				0x03e0,
				0x03e0,
				0x03e0,
				0x03e0,
				0xF0F0,
				0xF0F0,
				0x00F0,
				0x0202,
				0x0202,
				0x0001,
				0x0001,
				0x0001,
				0x0001,
				0x0fff,
				0x0c80,
				0x1ce4
		};

		assertEquals(40, buf.readableBytes());

		int[] actual = new int[20];

		for (int i = 0; i < 20; i++) {
			actual[i] = buf.readShort() & 0x0000ffff;
		}

		assertArrayEquals(expected, actual);

	}
}