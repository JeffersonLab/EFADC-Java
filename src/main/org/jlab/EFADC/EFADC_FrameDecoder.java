//
//  EFADC_FrameDecoder.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.logging.Logger;

public class EFADC_FrameDecoder extends MessageToMessageDecoder<ByteBuf> {

	private static final short VERIFY_BIT = 1;

	private EFADC_ChannelContext channelContext;

	int lastTrig = -1;
	int eventSize = 0;

	private Client client = null;

	public void setContext(EFADC_ChannelContext context) {
		channelContext = context;
	}

	public EFADC_ChannelContext getContext() {
		return channelContext;
	}

	private Object processDataPacket(ByteBuf buf, int mark, boolean connected) {
		if (buf.readableBytes() < 10)	//4 from header + 6 more to calculate event length
			return null;

		int temp = buf.getUnsignedShort(mark + 4);

		// We may immediately see another 5a5a XXXX here because of buffer preloading in the fpga
		// this happens when a stop command is received and the fifo not cleared
		// >> 00 00 5a 5a 03 01 5a 5a 03 03 etc...
		// If this is the case, mark the buffer position at the next byte (read 4 bytes)

		if (temp == 0x5a5a) {
			buf.readUnsignedInt();
			Logger.getGlobal().info("Skipping buffer preload...");
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
			return null;
		}

		// This may be a premature data packet before the device is actually connected, skip it
		if (!connected) {
			Logger.getGlobal().warning(String.format("Device not yet in connected state, ignoring data packet (%d bytes)", eventSize + 10));

			buf.skipBytes(eventSize + 10);
			return null;
		}

		buf.skipBytes(10);

		ByteBuf frame = buf.readBytes(eventSize);

		EFADC_DataEvent theEvent = new EFADC_DataEvent();

		theEvent.mode = mode;
		theEvent.modId = modId;
		theEvent.verifyMode = verifyMode;
		theEvent.sampleCount = sampleCount;
		theEvent.activeChannels = activeChannels;
		theEvent.chanCount = chanCount;

		theEvent.trigId = frame.readUnsignedShort();
		lastTrig = theEvent.trigId;

		theEvent.decode(frame);

		if (NetworkClient.flag_Verbose) {
			int pad = frame.readUnsignedShort();

			if (pad != 0) {
				Logger.getGlobal().warning(String.format("Data alignment error: 0x%04x  sums %d  samples %d  trigId %d", pad, theEvent.chanCount, theEvent.sampleCount, theEvent.trigId));
				//logger.info(str);
			}
		}

		return theEvent;	//return event as POJO
	}

	protected Object processRegisterPacket(int type, ByteBuf buf, int mark) {
		if (type == 0x0303) {
			int regHeader = buf.getUnsignedShort(mark + 4);	// skip 6

			if (regHeader >> 15 == 0) {
				//Decode standard EFADC registers

				// Register readback payload should be 60 total bytes
				if (buf.readableBytes() < EFADC_RegisterSet.DATA_SIZE_BYTES + 4)
					return null;

				buf.skipBytes(6);	// Skip over register header as well
				ByteBuf frame = buf.readBytes(54);

				EFADC_RegisterSet regs = new EFADC_RegisterSet(regHeader);

				regs.decode(frame);
				return regs;

			} else
				Logger.getGlobal().warning("0303 Command returning CMP registers?");

		} else if (type == 0x0304) {

			int regHeader = buf.getUnsignedShort(mark + 4);

			//Decode CMP registers
			int nADC = regHeader >> 8 & 0x7f;

			//Calculate total frame size, when reading from the CMP, first register that normally belongs to all EFADCs is only read once
			int frameSize = CMP_RegisterSet.DATA_SIZE_READ_BYTES + (nADC * (EFADC_RegisterSet.DATA_SIZE_BYTES));

			Logger.getGlobal().info(String.format("CMP Reg Decode, %04x regHeader, %d ADC's, %d frame size, %d readable", regHeader, nADC, frameSize, buf.readableBytes()));

			if (buf.readableBytes() < frameSize + 4) {
				Logger.getGlobal().warning(String.format("Not enough bytes in buffer to read CMP regs, need %d have %d", frameSize + 4, buf.readableBytes()));
				return null;
			}

			buf.skipBytes(4);	// Dont skip over register header
			ByteBuf frame = buf.readBytes(frameSize);

			CMP_RegisterSet regs = RegisterFactory.InitCMPRegisters(nADC);
			regs.decode(frame);
			return regs;

		} else
			Logger.getGlobal().warning("Unknown register packet type received: " + type);

		return null;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
		if (buf.readableBytes() < 4) {
			// The header field was not received yet - return null.
			// This method will be invoked again when more packets are
			// received and appended to the buffer.
			//logger.info("buffer has " + buf.readableBytes() + " readable bytes...");
			return;
		}
		
		int mark = buf.readerIndex();
		
		int header = buf.getUnsignedShort(mark);	// skip 2
			
		int type = buf.getUnsignedShort(mark + 2);	// skip 4
		
		if (header != 0x5a5a) {
			Logger.getGlobal().info(String.format("bad header: %04x  type: 0x%04x  lastTrig: %d  lastEventSize: %d", header, type, lastTrig, eventSize));

			if (NetworkClient.flag_Verbose) {
				int readable = buf.readableBytes();

				String out = readable + " bytes in buffer: ";

				for (int j = 0; j < readable; j++) {
					byte b = buf.getByte(j);
					out += String.format("%02x ", b);
				}
				Logger.getGlobal().info(out);
			}

			return;
		} //else
			//Logger.getGlobal().info("decodeFrame()");
		
		switch (type) {
			case 0x0301:
			case 0x0302: {

				// Check if we're in connected state before trying to parse a data packet
				// There is a bug where the device will send a data packet as the first response
				// after a powerup

				// Idea, assign client via constructor or other mutator method instead of checking
				// context each time...?

				// Get the device via global context
				if (client == null) {
					//EFADC_ChannelContext channelContext = (EFADC_ChannelContext)ctx.getAttachment();
					if (channelContext == null) {
						Logger.getGlobal().severe("NULL channelContext with non null client!");
					} else
						client = channelContext.getDeviceClient();
				}

				// client may still be null after this if it isn't connected
				// This is a terrible way to check if we're connected, but we need a reference to the client somehow..
				list.add(processDataPacket(buf, mark, (client != null && client.isConnected())));
				break;
			}

			case 0x0303:
			case 0x0304:
			case 0x0305: {
				list.add(processRegisterPacket(type, buf, mark));
				break;
			}

			/*
			 * Read device info from 0209 command
			 */
			case 0x030A: {
				int ver;

				Logger.getGlobal().info("DeviceInfo received");

				buf.skipBytes(4);
				ver = buf.readUnsignedShort();

				list.add(new DeviceInfo(ver));
				break;
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
				
				//Logger.getGlobal().info("Stray buffer " + count + " bytes long");
			
				list.add(buf.readBytes(count));
		}

		// If we got here, I think there is some other problem...

		//return null;
	}


}