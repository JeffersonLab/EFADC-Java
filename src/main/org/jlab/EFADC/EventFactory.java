package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.logging.Logger;

/**
 * Created by john on 9/25/15.
 *
 */
public class EventFactory {

    private static final short SAMPLES_BIT = (1 << 1);


	/**
	 * At this point, the readerIndex of the ChannelBuffer should be pointing to the header (5a5a) of the current frame
	 * @param temp Mode/modId word
	 * @param mark Current buffer mark
	 */
    public static EFADC_Event decodeEvent(int temp, int mark, ChannelBuffer buf) {
        EFADC_Event theEvent;

        int eventSize;

        int mode = (temp >> 8) & 0x00ff;
        int modId = temp & 0x00ff;
        boolean samplingMode = (mode & SAMPLES_BIT) == SAMPLES_BIT;

        if (samplingMode) {

			int sampleCount = buf.getUnsignedShort(mark + 6);	// sample count

			eventSize = (sampleCount * 2);	//total size of sample data
			eventSize += 12;				//trigId + timestamp + channel + sampleCount
			eventSize += 4;					//sum at the end

			int avail = buf.readableBytes();

			// Return null here to wait for more bytes to arrive and avoid overhead of constructing new DataEvent prematurely
			if (avail < eventSize + 16) {
				return null;
			}

			buf.skipBytes(6);	// skip up to sampleCount because we want to store it in the event object

			ChannelBuffer frame = buf.readBytes(eventSize);

            EFADC_SamplesEvent evt = new EFADC_SamplesEvent(modId, frame);

            if (EFADC_Client.flag_Verbose) {
                int pad = buf.readUnsignedShort();

                if (pad != 0) {
                    Logger.getLogger("global").info(String.format("SampleData alignment error: 0x%04x  samples %d  trigId %d", pad, evt.getSampleCount(), evt.getTriggerId()));
                }
            }

            theEvent = evt;

        } else {

			//low byte is reserved and as such, ignored, also we dont care about this value in summing mode
			//int sampleCount = (buf.getUnsignedShort(mark + 6) >> 8) & 0x00ff;	// sample or sums count

			short activeChannels = (short)buf.getUnsignedShort(mark + 8);

			int chanCount = 0;

			//Count active channels
			for (int i = 0; i < 16; i++) {
				if (((activeChannels >> i) & 0x1) == 0x1) {
					chanCount++;
				}
			}

			//Ignore sampleCount in integration mode
			eventSize = chanCount * 4;				//data size of sums
			eventSize += 8;						//trigId + timestamp

			int avail = buf.readableBytes();

			// Return null here to wait for more bytes to arrive and avoid overhead of constructing new DataEvent prematurely
			if (avail < eventSize + 10) {
				return null;
			}

			buf.skipBytes(10);	// skip up to triggerId
			ChannelBuffer frame = buf.readBytes(eventSize);

            EFADC_DataEvent evt = new EFADC_DataEvent(modId, chanCount, activeChannels, frame);

            if (EFADC_Client.flag_Verbose) {
                int pad = buf.readUnsignedShort();

                if (pad != 0) {
                    Logger.getLogger("global").info(String.format("SumData alignment error: 0x%04x  sums %d  trigId %d", pad, evt.getChannelCount(), evt.getTriggerId()));
                }
            }

            theEvent = evt;
        }


        return theEvent;
    }
}
