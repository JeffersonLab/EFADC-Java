package org.jlab.EFADC;

import org.omg.CORBA.DynAnyPackage.Invalid;

/**
 * Created by john on 8/18/15.
 */
public interface EFADC_Registers {

    void setBiasDAC(int channel, int value);

    int getCoincidenceWindowWidth();

    void setCoincidenceWindowWidth(int width);

    void setCoincidenceTable(int reg1, int reg2);

    int getIntegrationWindow();

    void setIntegrationWindow(int width);

    void setMode(int mode);

    void setNSB(int value);

    void setSelfTrigger(boolean active, int rate);

    boolean setThreshold(int group, int value);

    String getRegisterDescription(int i) throws InvalidRegisterException;

    String toString();
}
