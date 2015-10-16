package org.jlab.EFADC;

/**
 * Created by john on 9/24/15.
 */
public class RegisterFactory {

    public static EFADC_RegisterSet initRegisters(int header) {
        EFADC_RegisterSet regs;

        if (header == 0) {
            regs = new EFADC_RegistersV2(header);
        } else {
            regs = new EFADC_RegistersV3(header);
        }

        return regs;

    }
}
