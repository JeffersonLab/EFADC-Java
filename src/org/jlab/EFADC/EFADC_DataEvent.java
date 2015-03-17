//
//  EFADC_DataEvent.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

public class EFADC_DataEvent {

	public int chanCount;
	public int mode;
	public int modId;
	public int trigId;
	public int sampleCount;
	public int activeChannels;
	public long tStamp;
	//public int tStampHigh;	
	//public int tStampLow;
	
	public boolean verifyMode;
	public boolean[] chanActive;
	
	public int[] sums;
	public int[][] samples;
	
	EFADC_DataEvent() {
		chanCount = 0;
		chanActive = new boolean[16];
	}
	
	public boolean decode(ChannelBuffer frame) {

        trigId = frame.readUnsignedShort();

		//Timestamp
		/*
		int t1 = frame.readUnsignedShort();
		int t2 = frame.readUnsignedShort();
		int t3 = frame.readUnsignedShort();

		//System.out.printf("%04x %04x %04x\n", t1, t2, t3);

		tStamp = ((long)t1 << 32) + (t2 << 16) + t3;
		*/

		tStamp = (frame.readUnsignedInt() << 16) + frame.readUnsignedShort();

		//Find active channels
		/*
		for (int i = 0; i < 16; i++) {
			if (((theEvent.activeChannels >> i) & 0x1) == 0x1) {
				theEvent.chanActive[i] = true;
			}
		}
		*/

		sums = new int[chanCount];
		if (verifyMode)
			samples = new int[chanCount][sampleCount];

		int chanIdx = 0;

		//String str = "";

		//Extract channel/sample data
		for (int i = 0; i < 16; i++) {
			if (((activeChannels >> i) & 0x1) == 0x1) {
				chanActive[i] = true;

				if (verifyMode) {

					for (int j = 0; j < sampleCount; j++) {

						samples[chanIdx][j] = frame.readUnsignedShort();

						//str += String.format("%04x ", theEvent.samples[chanIdx][j]);
					}
				}

				sums[chanIdx++] = (int)(frame.readUnsignedInt() & 0x1fffff);
			}
		}

		return true;
	}
	
}