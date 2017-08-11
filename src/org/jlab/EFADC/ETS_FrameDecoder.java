package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.logging.Logger;

/**
 * Created by john on 7/12/17.
 */
public class ETS_FrameDecoder extends EFADC_FrameDecoder {

	ETS_Client m_Client;

	public ETS_FrameDecoder(ETS_Client client) {
		m_Client = client;
	}

	protected Object processRegisterPacket(int type, ChannelBuffer buf, int mark) {

		int regHeader = buf.getUnsignedShort(mark + 4);

		Logger.getGlobal().info(String.format("ETS processRegisterPacket, type %04x, %d bytes", type, buf.readableBytes()));

		if (type == 0x0304) {

			//Decode ETS registers

			int frameSize = ETS_RegisterSet.DATA_SIZE_BYTES;

			Logger.getGlobal().info(String.format("    ETSRegDecode 0304, %d frameSize, %d readable", frameSize, buf.readableBytes()));

			buf.skipBytes(4);	// Dont skip over register header

			if (buf.readableBytes() < frameSize) {
				Logger.getGlobal().severe(
					String.format("Not enough bytes in buffer to read ETS regs, need %d have %d",
					frameSize + 4, buf.readableBytes()));

				return null;
			}

			ChannelBuffer frame = buf.readBytes(frameSize);

			ETS_RegisterSet regs = new ETS_RegisterSet(m_Client);
			regs.decode(frame);
			return regs;

		} else if (type == 0x0303) {
			// ETS individual efadc register

			buf.skipBytes(4);	// Skip over register header as well

			// Pick off efadc module identifier
			int moduleId = buf.readUnsignedShort();

			int frameSize = ETS_EFADC_RegisterSet.DATA_SIZE_BYTES;

			// Register readback payload should be 60 total bytes
			if (buf.readableBytes() < frameSize) {
				Logger.getGlobal().severe(
					String.format("Not enough bytes in buffer to read ETS_EFADC regs, need %d have %d",
					frameSize, buf.readableBytes()));

				return null;
			}

			ChannelBuffer frame = buf.readBytes(frameSize);

			ETS_EFADC_RegisterSet regs = new ETS_EFADC_RegisterSet(m_Client, moduleId);

			regs.decode(frame);
			return regs;


		} else if (type == 0x0309) {
			// Ethernet parameter configuration

		} else
			Logger.getGlobal().warning("Unknown register packet type received: " + type);

		return null;
	}
}
