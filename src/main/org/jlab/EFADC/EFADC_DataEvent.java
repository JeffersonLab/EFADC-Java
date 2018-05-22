//
//  EFADC_DataEvent.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;


public class EFADC_DataEvent extends AbstractEvent implements EFADC_Event {

	public short modId;
	public short activeChannels;
	public byte chanCount;	// range for chancount is only 0-15


	EFADC_DataEvent(int modId, int chanCount, short activeChans, ChannelBuffer frame) {

		this.modId = (short)modId;

		this.chanCount = (byte)chanCount;
		this.activeChannels = activeChans;

		buf = frame;

		/* Just some debug stuff
		int trigId = getTriggerId();
		long ts = getTimestamp();

		int[] sums = extractSums();

		System.out.printf("tid: %d  ts: %d\n", trigId, ts);
		*/
	}

	@Deprecated
	public boolean decode(int chanCount, short activeChans, ChannelBuffer frame) {

		this.chanCount = (byte)chanCount;
		this.activeChannels = activeChans;

		buf = frame;

		//this.trigId = frame.readUnsignedShort();

		//Timestamp
		//this.tStamp = (frame.readUnsignedInt() << 16) + frame.readUnsignedShort();

		/*
		sums = new int[chanCount];

		int chanIdx = 0;

		//Extract channel/sample data from active channels
		for (int i = 0; i < 16; i++) {

			if (((activeChannels >> i) & 0x1) == 0x1) {
				//chanActive[i] = true;

				sums[chanIdx++] = (int)(frame.readUnsignedInt() & 0x1fffff);
			}
		}
		*/

		return true;
	}


	int getChannelCount() {
		return chanCount;
	}

	public int getModuleId() {
		return modId;
	}

	public long getTimestamp() {

		long high = buf.getInt(2);
		high <<= 16;

		int low = buf.getShort(6);
		//prevent sign extension
		low &= 0x0000ffff;

		return high | low;
	}

	public int getTriggerId() {
		return buf.getShort(0);
	}


	public int[] extractSums() {
		int[] sums = new int[chanCount];

		int chanIdx = 0;

		//Extract channel/sample data from active channels
		for (int i = 0; i < 16; i++) {

			if (((activeChannels >> i) & 0x1) == 0x1) {

				// 8 is the starting offset for samples in the underlying buffer
				sums[chanIdx] = (buf.getInt(8 + (chanIdx * 4)) & 0x1fffff);
				++chanIdx;
			}
		}

		return sums;
	}


	public boolean isChannelActive(int chan) {
		return (((activeChannels >> chan) & 0x1) == 0x1);
	}
	
}