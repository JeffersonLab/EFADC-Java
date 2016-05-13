package org.jlab.EFADC;


/**
 * Created by john on 8/18/15.
 *
 */
public class EFADC_RegistersV3 extends EFADC_RegistersV2 implements EFADC_Registers {

    static final int Mode_Mask		= (1 << 9);

    //static final int Reset_Mask = 0x1000; // deprecated in v3

    public EFADC_RegistersV3(int header) {
        super(header);
    }

    @Override
    public String getRegisterDescription(int i) throws InvalidRegisterException {
        if (i < 0 || i > NUM_REGISTERS)
            throw new InvalidRegisterException();

        String returnStr;

        switch (i) {

            case 1:
                returnStr = "15: Mode (0 SUM, 1 Sampling); 13: Sync; 10: 1 - Free Running Trigger, rate set by conf18 bits 11:0; 8..0: Integration Window";
                break;

            case 18:
                returnStr = "12..0: Self trigger rate (512ns per count)";
                break;

            default:
                returnStr = super.getRegisterDescription(i);

        }

        return returnStr;
    }


    @Override
    public void setMode(int mode) {

        if (mode == 0)
			m_Registers[REG_1] &= ~Mode_Mask;
        else if (mode == 1)
			m_Registers[REG_1] |= Mode_Mask;
    }

}
