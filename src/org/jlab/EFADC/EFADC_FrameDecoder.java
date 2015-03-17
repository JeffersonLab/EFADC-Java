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

	private static final short VERIFY_BIT = 1;
	
	private static final Logger logger = Logger.getLogger("global");
	
	int lastTrig = -1;
	int eventSize = 0;
    long lastTime = 0;
	
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
			logger.warning(String.format("bad header: %04x  type: 0x%04x  lastTime:%04x%08x lastTrig: %04x  lastEventSize: %d",
                    header, type, (lastTime >> 32) & 0x0000ffff, lastTime & 0xffffffff, lastTrig, eventSize));

            /*
			if (EFADC_Client.flag_Verbose) {
				int readable = buf.readableBytes();

				String out = readable + " bytes in buffer: ";

				for (int j = 0; j < readable; j++) {
					byte b = buf.getByte(j);
					out += String.format("%02x ", b);
				}
				logger.info(out);
			}
			*/

            // Allow the handler to continue on into the default switch statement

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
					logger.info("Skipping buffer preload...");
					return null;
				}

				//low byte is reserved and as such, ignored
				int sampleCount = (buf.getUnsignedShort(mark + 6) >> 8) & 0x00ff;
				int activeChannels = buf.getUnsignedShort(mark + 8);
				
				int mode = (temp >> 8) & 0x00ff;
				int modId = temp & 0x00ff;
				boolean verifyMode = (mode & VERIFY_BIT) == VERIFY_BIT;
				
				int chanCount = 0;
				
				//Count active channels
				for (int i = 0; i < 16; i++) {
					if (((activeChannels >> i) & 0x1) == 0x1) {
						chanCount++;
					}
				}
				
				eventSize = chanCount * 4;					//data size of sums
				eventSize += (sampleCount * 2) * chanCount;	//total size of sample data
				eventSize += 2 + 6 + 2;						//trigId + timestamp + padding
				
				//logger.info(String.format("verify %d  eventSize %d  chanCount %d  sampleCount %d", verifyMode ? 1 : 0, eventSize, chanCount, sampleCount));
				
				int avail = buf.readableBytes();

				// Return null here to wait for more bytes to arrive and avoid overhead of constructing new DataEvent prematurely
				if (avail < eventSize + 10) {
                    logger.info(String.format("Not enough bytes in read buffer, need %d have %d.", eventSize + 10, avail));
					return null;
				}
					
				buf.skipBytes(10);
				ChannelBuffer frame = buf.readBytes(eventSize);
				
				EFADC_DataEvent theEvent = new EFADC_DataEvent();

				theEvent.mode = mode;
				theEvent.modId = modId;
				theEvent.verifyMode = verifyMode;
				theEvent.sampleCount = sampleCount;
				theEvent.activeChannels = activeChannels;
				theEvent.chanCount = chanCount;

				lastTrig = theEvent.trigId;
                lastTime = theEvent.tStamp;

				theEvent.decode(frame);


				if (EFADC_Client.flag_Verbose) {
					int pad = frame.readUnsignedShort();

					if (pad != 0) {
						logger.warning(String.format("Data alignment error: 0x%04x  sums %d  samples %d  trigId %d", pad, theEvent.chanCount, theEvent.sampleCount, theEvent.trigId));
						//logger.info(str);
					}
				}


				return theEvent;	//return event as POJO
			}
			
			case 0x0303: {
				int regHeader = buf.getUnsignedShort(mark + 4);	// skip 6

				if (regHeader >> 15 == 0) {
					//Decode standard EFADC registers

					// Register readback payload should be 60 total bytes
					if (buf.readableBytes() < EFADC_RegisterSet.DATA_SIZE_BYTES + 4)
						return null;

					buf.skipBytes(6);	// Skip over register header as well
					ChannelBuffer frame = buf.readBytes(54);

					EFADC_RegisterSet regs = new EFADC_RegisterSet(regHeader);

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
				int frameSize = CMP_RegisterSet.DATA_SIZE_READ_BYTES + (nADC * (EFADC_RegisterSet.DATA_SIZE_BYTES));

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
				
				logger.warning("Attempting buffer re-alignment");

				// mark + 2 was the event type, so read on from 4 bytes from the mark
				int count = 4;
				boolean found = false;

				while (buf.readableBytes() > mark + count) {

					if (buf.getUnsignedShort(mark + count) == 0x5a5a) {
                        found = true;
                        logger.info("  Header found at index " + (mark + count));
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

                if (found) {
                    logger.warning("Re-aligned buffer after " + count + " bytes at index " + (mark + count));

                    //
                    //return buf.readBytes(mark + count);

                    //if (mark + count < buf.readableBytes(

                    buf.skipBytes(mark + count);
                } else {
                    // Ok, something has gone seriously wrong
                    // We should probably stop acquisition here...
                    return null;
                }
			

		}

		// If we got here, I think there is some other problem...

		return null;
	}
}