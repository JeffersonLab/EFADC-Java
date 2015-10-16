package org.jlab.EFADC;

/**
 * Created by john on 9/25/15.
 */
public abstract class AbstractEvent implements EFADC_Event {

    public int modId;
    public int trigId;
    public long tStamp;

    public int getModuleId() {
        return modId;
    }

    public int getTriggerId() {
        return trigId;
    }

    public long getTimestamp() {
        return tStamp;
    }

}
