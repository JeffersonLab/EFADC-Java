package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;


/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/13/12
 */
public class EFADC_SamplesEvent extends AbstractEvent implements EFADC_Event {

	public EFADC_SamplesEvent(int modId, ChannelBuffer frame) {
		this.modId = (short)modId;
		buf = frame;
	}

	@Deprecated
	public boolean decode(ChannelBuffer frame) {

		// Copy contents directly to a ByteBuffer so the downstream handler can easily redirect samples to a file
		// or whatever, later
		buf = frame;

		// We need this stuff to be able to extract samples
		//trigId = frame.readUnsignedShort();
		//channel = frame.readUnsignedShort();
		//sampleCount = (short)frame.readUnsignedShort();

		return true;
	}

	public int getSampleCount() {
		return buf.getShort(0);
	}

	public int getChannel() {
		return buf.getShort(2);
	}

	public int getTriggerId() {
		// Trigger ID is the first shortint in the buffer
		return buf.getShort(4);
	}

	@Override
	public long getTimestamp() {
		long tStamp = (buf.getInt(6) << 16) + buf.getShort(8);

		return tStamp;
	}

	public int getSum() {
		int sumIdx = getSampleCount() * 2 + 12;

		return buf.getInt(sumIdx) & 0x1fffff;
	}

	public int getSample(int num) {

		if (num > getSampleCount())
			return -1;

		int sampleIdx = 12 + 2 * num;

		return buf.getShort(sampleIdx);
	}


	public int[] extractSamples() {

		int count = getSampleCount();

		int[] samples = new int[count];

		//Extract sample data
		for (int i = 0; i < count; i++) {

			samples[i] = buf.getShort(6 + (i * 2));

		}

		return samples;
	}

	/*
	public boolean isChannelActive(int chan) {
		return chan == getChannel();
	}
	*/

}
