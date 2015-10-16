//
//  EFADC_FrameDecoder.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.util.logging.Logger;

public class EFADC_FrameDecoder extends FrameDecoder {

	//int lastTrig = -1;
	int eventSize = 0;

	EFADC_Event lastEvent;
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {
		
		if (buf.readableBytes() < 4) {
			// The header field was not received yet - return null.
			// This method will be invoked again when more packets are
			// received and appended to the buffer.
			//logger.info("buffer has " + buf.readableBytes() + " readable bytes...");
			return null;
		}
		
		int mark = buf.readerIndex();
		
		int header = buf.getUnsignedShort(mark);	// skip 2
			
		int type = buf.getUnsignedShort(mark + 2);	// skip 4
		
		if (header != 0x5a5a) {
			Logger.getLogger("global").info(String.format("bad header: %04x  type: 0x%04x  lastTrig: %d  lastEventSize: %d", header, type, lastEvent.getTriggerId(), eventSize));

			if (EFADC_Client.flag_Verbose) {
				int readable = buf.readableBytes();

				String out = readable + " bytes in buffer: ";

				for (int j = 0; j < readable; j++) {
					byte b = buf.getByte(j);
					out += String.format("%02x ", b);
				}
				Logger.getLogger("global").info(out);
			}

			return null;
		} //else
			//Logger.getLogger("global").info("decodeFrame()");
		
		switch (type) {
			case 0x0301:
			case 0x0302: {
			
				if (buf.readableBytes() < 10)	//4 from header + 6 more to calculate event length
					return null;
			
				int temp = buf.getUnsignedShort(mark + 4);

				// We may immediately see another 5a5a XXXX here because of buffer preloading in the fpga
				// this happens when a stop command is received and the fifo not cleared
				// >> 00 00 5a 5a 03 01 5a 5a 03 03 etc...
				// If this is the case, mark the buffer position at the next byte (read 4 bytes)

				if (temp == 0x5a5a) {
					buf.readUnsignedInt();
					Logger.getLogger("global").info("Skipping buffer preload...");
					return null;
				}

				EFADC_Event theEvent = EventFactory.decodeEvent(temp, mark, buf);

				/*
				if (EFADC_Client.flag_Verbose) {
					int pad = frame.readUnsignedShort();

					if (pad != 0) {
						logger.info(String.format("Data alignment error: 0x%04x  sums %d  samples %d  trigId %d", pad, theEvent.chanCount, theEvent.sampleCount, theEvent.trigId));
						//logger.info(str);
					}
				}
				*/

				lastEvent = theEvent;

				return theEvent;	//return event as POJO
			}
			
			case 0x0303: {
				int regHeader = buf.getUnsignedShort(mark + 4);	// skip 6

				if (regHeader >> 15 == 0) {
					//Decode standard EFADC registers

					// Register readback payload should be 60 total bytes
					// TODO Assuming version 2 and 3 have same number of registers
					if (buf.readableBytes() < EFADC_RegistersV2.DATA_SIZE_BYTES + 4)
						return null;

					buf.skipBytes(6);	// Skip over register header as well
					ChannelBuffer frame = buf.readBytes(54);

					EFADC_RegisterSet regs = RegisterFactory.initRegisters(regHeader);

					regs.decode(frame);

					return regs;

				} else
					Logger.getLogger("global").warning("0303 Command returning CMP registers?");

				break;

			}

			case 0x0304: {
				int regHeader = buf.getUnsignedShort(mark + 4);

				//Decode CMP registers
				int nADC = regHeader >> 8 & 0x7f;

				//Calculate total frame size, when reading from the CMP, first register that normally belongs to all EFADCs is only read once
				// TODO Update for new efadc  assuming version 2 for now...
				int frameSize = CMP_RegisterSet.DATA_SIZE_READ_BYTES + (nADC * (EFADC_RegistersV2.DATA_SIZE_BYTES));

				Logger.getLogger("global").info(String.format("CMP Reg Decode, %04x regHeader, %d ADC's, %d frame size, %d readable", regHeader, nADC, frameSize, buf.readableBytes()));

				if (buf.readableBytes() < frameSize + 4) {
					Logger.getLogger("global").warning(String.format("Not enough bytes in buffer to read CMP regs, need %d have %d", frameSize + 4, buf.readableBytes()));
					return null;
				}

				buf.skipBytes(4);	// Dont skip over register header
				ChannelBuffer frame = buf.readBytes(frameSize);

				CMP_RegisterSet regs = new CMP_RegisterSet(nADC);
				regs.decode(frame);
				return regs;
			}
			
			default:
				
				//System.out.printf("Packet 0x%04x echo\n", type);

				// mark + 2 was the event type, so read on from 4 bytes from the mark
				int count = 4;
				
				while (buf.readableBytes() > 1) {

					if (buf.getUnsignedShort(mark + count) == 0x5a5a) {
						break;
					} else
						++count;	// Advance 1 byte at a time because we don't know if the buffer has an even number
									// of bytes and will be short-aligned with the header
				}
				
				/*
				for (int j = 0; j < echo.capacity(); j++) {
					byte b = echo.getByte(j);
					System.out.printf("%02x ", b);
				}
				System.out.println();
				*/
				
				//Logger.getLogger("global").info("Stray buffer " + count + " bytes long");
			
				return buf.readBytes(count);
		}

		// If we got here, I think there is some other problem...

		return null;
	}
}