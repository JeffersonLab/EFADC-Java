package org.jlab.EFADC;


import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Created by john on 8/18/15.
 *
 */
public class EFADC_RegistersV3 extends EFADC_RegistersV2 implements EFADC_Registers {

    private static final int NUM_STATUS = 9;
	public static final int DATA_SIZE_BYTES = (NUM_REGISTERS + NUM_STATUS) * 2;

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
     * Prep for ROM config write by copying the rom config array into the register array, saves old register values
     */
    public List<Integer> armROMConfiguration(ROMParameters romParameters) {

		if (romParameters == null) {
			Logger.getLogger("global").warning("Cannot arm ROM config, parameters not set yet");
			return null;
		}

        List<Integer> oldRegs = new Vector<>(11);

		List<Integer> romConfig = romParameters.getParamsAsRegisters();

        for (int i = 0; i < romConfig.size(); i++) {
            oldRegs.add(m_Registers[i]);
            m_Registers[i] = romConfig.get(i);
        }

		Logger.getLogger("global").info("Arming rom configuration...");

		return oldRegs;
    }


	/**
     * Copy saved register values back into the register array and discard saved register list
     */
    public void disarmROMConfiguration(List<Integer> oldRegs) {
        for (int i = 0; i < oldRegs.size(); i++) {
            m_Registers[i] = oldRegs.get(i);
        }
    }

}
