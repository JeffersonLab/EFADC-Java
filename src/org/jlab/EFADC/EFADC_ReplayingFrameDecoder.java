//
//  EFADC_ReplayingFrameDecoder.java
//  EFADC_java
//
//  Created by John McKisson on 2/7/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import java.util.logging.Logger;


@Deprecated
public class EFADC_ReplayingFrameDecoder extends ReplayingDecoder<EFADC_ReplayingFrameDecoder.DecoderState> {
	
	private static final short VERIFY_BIT = 1 << 0;
	
	//private static final Logger logger = Logger.getLogger(EFADC_ReplayingFrameDecoder.class.getName());
	private static final Logger logger = Logger.getLogger("global");
	
	public enum DecoderState {
		READ_HEADER,
		READ_LENGTH,
		READ_CONTENT,
		READ_FOOTER
	}
	
	private int type;
	private int temp;
	private int sampleCount;
	private int activeChans;
	private int chanCount;
	private int eventSize;
	private int trigId;
	
	private EFADC_DataEvent event;
	
	public EFADC_ReplayingFrameDecoder() {
		super(DecoderState.READ_HEADER);
	}
	
	@Override
	protected Object decode(ChannelHandlerContext ctx,
							Channel channel,
							ChannelBuffer buf, DecoderState state) throws Exception {
		
		switch (state) {
			case READ_HEADER:
				
				int header = buf.readUnsignedShort();
				if (header != 0x5a5a)
					logger.info(String.format("read bad header: %04x  prev event: 0x%04x  size %d", header, trigId, eventSize));
					
				type = buf.readUnsignedShort();
				checkpoint(DecoderState.READ_LENGTH);
				
				if (type == 0x0303) {
					//logger.info("decoding registers");
					
					ChannelBuffer frame = buf.readBytes(EFADC_RegisterSet.DATA_SIZE_BYTES);
					
					EFADC_RegisterSet regs = decodeRegisters(frame);
					checkpoint(DecoderState.READ_HEADER);
					return regs;
				}
				
			case READ_LENGTH: {

				int rbytes = actualReadableBytes();
				if (rbytes < 6) {
					logger.info("READ_LENGTH rbytes = " + rbytes);
					return null;
				}
						
				temp = buf.readUnsignedShort();
				sampleCount = (buf.readUnsignedShort() >> 8) & 0x00ff;	//low byte is reserved and as such, ignored
				activeChans = buf.readUnsignedShort();
				
				chanCount = 0;
				
				//Find active channels
				for (int i = 0; i < 16; i++) {
					if (((activeChans >> i) & 0x1) == 0x1) {
						chanCount++;
					}
				}
				
				//if (type == 0x0301) {
					//Normal data
					eventSize = chanCount * 4;					//data size of sums
					eventSize += (sampleCount * 2) * chanCount;	//total size of sample data
					eventSize += 8;						//trigId + timestamp + padding
				//} else {
				//	eventSize = 1440 - 6;
				//}
				
				checkpoint(DecoderState.READ_CONTENT);
			}
			
			case READ_CONTENT: {
				ChannelBuffer frame = buf.readBytes(eventSize);
				
				event = decodeDataEvent(frame);
				
				checkpoint(DecoderState.READ_FOOTER);
			}
				
			case READ_FOOTER: {
				int footer = buf.readUnsignedShort();
				
				if (footer != 0x000) {
					logger.info(String.format("bad event footer: 0x%04x", footer));
				}
				
				checkpoint(DecoderState.READ_HEADER);
				
				return event;
			}
				
			default: {
				logger.info(String.format("read length, type 0x%04x", type));

				int temp = 0;
				int count = 0;
				do {
					temp = buf.readUnsignedShort();
					count++;
				} while (temp != 0x5a5a);
				buf.readerIndex(buf.readerIndex() - 2);
				
				logger.info(String.format("unknown type: 0x%04x, realigned after %d words", type, count));
				
				//this must be throwing an error...
				//ChannelBuffer echo = buf.readBytes(buf.readableBytes());
				checkpoint(DecoderState.READ_HEADER);
				//return echo;
			}
			break;
				
		}
		
		return null;
	}
	
	/*
	private EFADC_DataEvent readContent(ChannelBuffer buf) throws Exception {
		//if (type == 0x0301 || type == 0x0302) {
			
			
			//logger.info(String.format("event 0x%04x  size? %d", type, eventSize));
			
			
			
			//Process a single event if the whole event is available in the buffer
			
			
			
			//logger.info("event complete");
			return event;
		//} else {
		//	logger.info("bad type in read content");
		//	return null;
		//}

	}
	*/

				
	private EFADC_RegisterSet decodeRegisters(ChannelBuffer buf) {
				
		EFADC_RegisterSet regs = new EFADC_RegisterSet();
		
		String str = "\n";
		
		int j = 0;
		for (; j < 21; j++) {
			int val = buf.readUnsignedShort();	//prevent sign extension
			str += String.format("%04x ", val);
			if (!regs.setRegister(j, val)) {
				logger.warning("Invalid register readback buffer");
				return regs;
			}
			
		}
		
		int status1 = buf.readUnsignedShort();
		int status2 = buf.readUnsignedShort();
		int status3 = buf.readUnsignedShort();
		int status4 = buf.readUnsignedShort();
		int status5 = buf.readUnsignedShort();
		int status6 = buf.readUnsignedShort();
		int status7 = buf.readUnsignedShort();
		
		str += String.format("\n%04x %04x %04x %04x %04x %04x %04x\n", status1, status3, status3, status4, status5, status6, status7);
		
		int acceptedTrigs = status2 + ((status3 & 0x00ff) << 16);
		int missedTrigs = status4 + ((status3 & 0xff00) << 16);
		
		str += "Accepted: " + acceptedTrigs + "  Missed: " + missedTrigs;
		
		logger.info(str);
		
		return regs;
	}
	
	private EFADC_DataEvent decodeDataEvent(ChannelBuffer buf) {
	
		EFADC_DataEvent theEvent = new EFADC_DataEvent();

		theEvent.mode = (temp >> 8) & 0x00ff;
		theEvent.modId = temp & 0x00ff;
		theEvent.verifyMode = (theEvent.mode & VERIFY_BIT) == 1;

		theEvent.chanCount = chanCount;
		theEvent.sampleCount = sampleCount;
		theEvent.activeChannels = activeChans;
		
		theEvent.sums = new int[theEvent.chanCount];
		if (theEvent.verifyMode)
			theEvent.samples = new int[theEvent.chanCount][theEvent.sampleCount];

		//Find active channels
		for (int i = 0; i < 16; i++) {
			if (((activeChans >> i) & 0x1) == 0x1) {
				theEvent.chanActive[i] = true;
				//chanCount++;
			}
		}

		theEvent.trigId = trigId = buf.readUnsignedShort();

		//Timestamp
		int t1 = buf.readUnsignedShort();
		int t2 = buf.readUnsignedShort();
		int t3 = buf.readUnsignedShort();
		//System.out.printf("%04x %04x %04x\n", t1, t2, t3);

		theEvent.tStamp = ((long)t1 << 32) + (t2 << 16) + t3;

		int chanIdx = 0;

		//Extract channel/sample data
		for (int i = 0; i < 16; i++) {
			if (theEvent.chanActive[i]) {
				if (theEvent.verifyMode) {

					for (int j = 0; j < theEvent.sampleCount; j++) {

						theEvent.samples[chanIdx][j] = buf.readUnsignedShort();
					}
				}

				theEvent.sums[chanIdx++] = ((buf.readUnsignedShort() >> 8) & 0x00ff) + buf.readUnsignedShort();
			}
		}

		//buf.readUnsignedShort();	//read 0x0000 at end of data set

		return theEvent;	//return event as POJO
	}
	
}