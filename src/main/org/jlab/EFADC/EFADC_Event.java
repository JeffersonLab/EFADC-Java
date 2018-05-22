package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;


/**
 * Created by john on 9/28/15.
 *
 */
public interface EFADC_Event {

    int getModuleId();
    int getTriggerId();
    long getTimestamp();

	//boolean isChannelActive(int chan);

    ChannelBuffer getBuffer();
}
