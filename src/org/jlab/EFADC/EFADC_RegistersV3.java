package org.jlab.EFADC;


import java.util.logging.Logger;

/**
 * Created by john on 8/18/15.
 *
 */
public class EFADC_RegistersV3 extends EFADC_RegistersV2 implements EFADC_Registers {

    static final int Mode_Mask		= 0xc000; // reg 1 bits 15..14

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


	/*
	 * V3 moved the mode bits to 15..14 of register 1
	 * there are now 3 modes, 0 is sum mode, 1 is old sampling mode (but not implemented), and 2 is new sampling mode
	 */
    @Override
    public void setMode(int mode) {

        if (mode == 0)
			m_Registers[REG_1] &= ~Mode_Mask;
       // else if (mode == 1)
			//m_Registers[REG_1] |= Mode_Mask;
        else if (mode == 2) {
			int r1 = m_Registers[REG_1];

            r1 |= 0x8000;

			m_Registers[REG_1] = r1;
		}
    }


	/**
	 * 11..8 : ADC signal  range select . 0: -.4V; 1: -2
	 * 8- Channel 1-4
	 * 9- Channel 5-8
	 * 10- Channel 9-12
	 * 11—Channel 13-16
	 *
	 * For v3 hardware the switch was 'fixed' to have only a 2 or 1 v range
	 * 0 is 1 V range, 1 is 2 V range,
	 *
	 * @param module Detector module, 0 - channels 1-4, 1 - chans 5-8, 2 - chans 9-12, 3 - chans 13-16
	 * @param range Voltage range,
	 */
	public void setInputRange(int module, int range) {
		if (m_Version != 0x3500) {
			Logger.getLogger("global").warning("setInputRange feature only available in firmware 0x3500 and above");
			return;
		}

		if (range < 0 || range > 1) {
			Logger.getLogger("global").warning("Valid ranges are 0 (1V) or 1 (2V)");
			return;
		}

		// module-to-bit map
		int[] modbit = new int[] {8, 9, 10, 11};

		int reg = getRegister(REG_3);

		if (range == 0)
			reg &= ~(1 << modbit[module]);
		else if (range == 1)
			reg |= (1 << modbit[module]);

		Logger.getLogger("global").info(String.format("setInputRange:: Setting reg3 to %04x", reg));

		setRegister(REG_3, reg);
	}

}
