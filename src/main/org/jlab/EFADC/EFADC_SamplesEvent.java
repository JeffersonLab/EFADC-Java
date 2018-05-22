package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.nio.ByteBuffer;
import java.util.logging.Logger;


/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/13/12
 */
public class EFADC_SamplesEvent extends AbstractEvent implements EFADC_Event {

	public static final int IDX_COUNT = 0;
	public static final int IDX_CHANNEL = 2;
	public static final int IDX_TRIGID = 4;
	public static final int IDX_TSTAMP = 6;
	public static final int IDX_DATA = IDX_TSTAMP + 6;

	public EFADC_SamplesEvent(ByteBuffer bytebuf) {
		buf = ChannelBuffers.copiedBuffer(bytebuf);
	}

	public EFADC_SamplesEvent(int modId, ChannelBuffer frame) {
		this.modId = (short)modId;
		buf = frame;
	}

	@Deprecated
	public boolean decode(ChannelBuffer frame) {

		// Copy contents directly to a ByteBuffer so the downstream handler can easily redirect samples to a file
		// or whatever, later
		buf = frame;

		return true;
	}

	public int getSampleCount() {
		return buf.getShort(IDX_COUNT) & 0x0000ffff;
	}

	public int getChannel() {
		return buf.getShort(IDX_CHANNEL) & 0x0000ffff;
	}

	public int getTriggerId() {
		return buf.getShort(IDX_TRIGID) & 0x0000ffff;
	}

	@Override
	public long getTimestamp() {
		long high = ((long)buf.getInt(IDX_TSTAMP) << 16) & 0x0000_ffff_ffff_0000L;
		int low = (int)buf.getShort(IDX_TSTAMP + 4) & 0x0000_ffff;

		long tStamp = (high | low) & 0x0000_ffff_ffff_ffffL;

		//Logger.getLogger("global").info(String.format("high 0x%08x  low 0x%04x  tStamp 0x%08x", high, low, tStamp));

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

			samples[i] = buf.getShort(IDX_DATA + (i * 2)) & 0x0000ffff;

		}

		return samples;
	}

	/*
	public boolean isChannelActive(int chan) {
		return chan == getChannel();
	}
	*/

}
