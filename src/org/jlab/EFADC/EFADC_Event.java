package org.jlab.EFADC;

/**
 * Created by john on 9/28/15.
 *
 */
public interface EFADC_Event {

    int getModuleId();
    int getTriggerId();
    long getTimestamp();
}
