//
//  EFADC_DataEvent.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

public class EFADC_DataEvent extends AbstractEvent {

	//public int chanCount;	// Initialized by EventFactory before invoking decode
	public int mode;		// This lets us know if the EFADC is in sampling mode, we should expect samples packets
	public int activeChannels;

	public boolean[] chanActive;
	public int[] sums;

	
	EFADC_DataEvent(int mode, int modId) {
		//chanCount = 0;
		chanActive = new boolean[16];

		this.mode= mode;
		this.modId = modId;
	}


	int getChannelCount() {
		return (sums == null ? 0 : sums.length);
	}


	public boolean decode(int chanCount, ChannelBuffer frame) {

		this.trigId = frame.readUnsignedShort();

		//Timestamp
		this.tStamp = (frame.readUnsignedInt() << 16) + frame.readUnsignedShort();

		sums = new int[chanCount];

		int chanIdx = 0;

		//Extract channel/sample data from active channels
		for (int i = 0; i < 16; i++) {

			if (((activeChannels >> i) & 0x1) == 0x1) {
				chanActive[i] = true;

				sums[chanIdx++] = (int)(frame.readUnsignedInt() & 0x1fffff);
			}
		}

		return true;
	}
	
}