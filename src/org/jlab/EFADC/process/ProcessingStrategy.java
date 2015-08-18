package org.jlab.EFADC.process;

import org.jlab.EFADC.EventSet;

/**
 * Created by john on 12/17/14.
 */
public interface ProcessingStrategy {
    public void processEventSet(EventSet set);
}
