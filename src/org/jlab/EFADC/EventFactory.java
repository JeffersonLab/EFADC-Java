package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.logging.Logger;

/**
 * Created by john on 9/25/15.
 *
 */
public class EventFactory {

    private static final short SAMPLES_BIT = 1;

    public static EFADC_Event decodeEvent(int temp, int mark, ChannelBuffer buf) {
        EFADC_Event theEvent;

        int eventSize = 0;

        int mode = (temp >> 8) & 0x00ff;
        int modId = temp & 0x00ff;
        boolean samplingMode = (mode & SAMPLES_BIT) == SAMPLES_BIT;

        //low byte is reserved and as such, ignored
        int sampleCount = (buf.getUnsignedShort(mark + 6) >> 8) & 0x00ff;
        int activeChannels = buf.getUnsignedShort(mark + 8);

        int chanCount = 0;

        //Count active channels
        for (int i = 0; i < 16; i++) {
            if (((activeChannels >> i) & 0x1) == 0x1) {
                chanCount++;
            }
        }

        if (samplingMode) {
            eventSize = (sampleCount * 2) * chanCount;	//total size of sample data
            eventSize += 10;						//trigId + timestamp + padding

        } else {
            //Ignore sampleCount in integration mode
            eventSize = chanCount * 4;					//data size of sums
            eventSize += 10;						//trigId + timestamp + padding

            //logger.info(String.format("verify %d  eventSize %d  chanCount %d  sampleCount %d", verifyMode ? 1 : 0, eventSize, chanCount, sampleCount));
        }

        int avail = buf.readableBytes();

        // Return null here to wait for more bytes to arrive and avoid overhead of constructing new DataEvent prematurely
        if (avail < eventSize + 10) {
            return null;
        }

        buf.skipBytes(10);
        ChannelBuffer frame = buf.readBytes(eventSize);

        if (samplingMode) {
            EFADC_SamplesEvent evt = new EFADC_SamplesEvent(modId);

            evt.decode(frame);

            if (EFADC_Client.flag_Verbose) {
                int pad = frame.readUnsignedShort();

                if (pad != 0) {
                    Logger.getLogger("global").info(String.format("SampleData alignment error: 0x%04x  samples %d  trigId %d", pad, evt.getSampleCount(), evt.getTriggerId()));
                    //logger.info(str);
                }
            }

            theEvent = evt;

        } else {
            EFADC_DataEvent evt = new EFADC_DataEvent(mode, modId);

            evt.activeChannels = activeChannels;

            evt.decode(chanCount, frame);

            if (EFADC_Client.flag_Verbose) {
                int pad = frame.readUnsignedShort();

                if (pad != 0) {
                    Logger.getLogger("global").info(String.format("SumData alignment error: 0x%04x  sums %d  trigId %d", pad, evt.getChannelCount(), evt.getTriggerId()));
                    //logger.info(str);
                }
            }

            theEvent = evt;
        }


        return theEvent;
    }
}
