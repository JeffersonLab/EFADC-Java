package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/13/12
 */
public class EFADC_SamplesEvent extends AbstractEvent implements EFADC_Event {

	public int channel;
	public int[] samples;

	public EFADC_SamplesEvent(int modId) {
		this.modId = modId;
	}

	public int getSampleCount() {
		return (samples == null ? 0 : samples.length);
	}

	public boolean decode(ChannelBuffer frame) {

		trigId = frame.readUnsignedShort();
		channel = frame.readUnsignedShort();
		int sampleCount = frame.readUnsignedShort();

		samples = new int[sampleCount];

		//Extract sample data
		for (int i = 0; i < sampleCount; i++) {

			samples[i] = frame.readUnsignedShort();

		}

		return true;
	}
}
