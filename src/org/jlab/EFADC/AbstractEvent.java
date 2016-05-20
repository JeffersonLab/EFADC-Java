package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Created by john on 9/25/15.
 *
 */
public abstract class AbstractEvent implements EFADC_Event {

    public short modId;
    //public int trigId;
    //public long tStamp;

	protected ChannelBuffer buf;


    public int getModuleId() {
        return modId;
    }

	/*
    public int getTriggerId() {
        return trigId;
    }

    public long getTimestamp() {
        return tStamp;
    }
    */

	public ChannelBuffer getBuffer() { return buf; }

}
