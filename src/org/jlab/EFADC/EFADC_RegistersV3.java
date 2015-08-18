package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Created by john on 8/18/15.
 */
public class EFADC_RegistersV3 extends EFADC_RegisterSet implements EFADC_Registers {
    @Override
    public boolean decode(ChannelBuffer frame) {
        return false;
    }

    @Override
    public void setBiasDAC(int channel, int value) {

    }

    @Override
    public int getCoincidenceWindowWidth() {
        return 0;
    }

    @Override
    public void setCoincidenceWindowWidth(int width) {

    }

    @Override
    public void setCoincidenceTable(int reg1, int reg2) {

    }

    @Override
    public int getIntegrationWindow() {
        return 0;
    }

    @Override
    public void setIntegrationWindow(int width) {

    }

    @Override
    public void setMode(int mode) {

    }

    @Override
    public void setNSB(int value) {

    }

    @Override
    public void setSelfTrigger(boolean active, int rate) {

    }

    @Override
    public boolean setThreshold(int group, int value) {
        return false;
    }

    @Override
    public String getRegisterDescription(int i) {
        return null;
    }
}
