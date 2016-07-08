package org.jlab.EFADC;


import java.util.List;
import java.util.Vector;

/**
 * Created by john on 8/18/15.
 *
 */
public class EFADC_RegistersV3 extends EFADC_RegistersV2 implements EFADC_Registers {

    private static final int NUM_STATUS = 9;
	public static final int DATA_SIZE_BYTES = (NUM_REGISTERS + NUM_STATUS) * 2;

    static final int Mode_Mask		= 0xc000; // reg 1 bits 15..14

    //static final int Reset_Mask = 0x1000; // deprecated in v3

    private List<Integer> romConfig = null;
    private List<Integer> oldRegs = null;

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
     * Configures the rom configuration array, use -1 for values not to be changed
     * @param ip
     * @param subnet
     * @param macAddr
     * @param port
     */
    public void setROMConfiguration(int ip, int subnet, long macAddr, int port) {

        romConfig = new Vector<>(11);

        // [0]
        romConfig.add(0x0800);

        // [1] [2]
        if (ip != -1) {
            romConfig.add((ip >> 16));
            romConfig.add((ip & 0x0000ffff));
        } else {
            romConfig.add(0x0102);
            romConfig.add(0x0309);
        }

        // [3] [4]
        if (subnet != -1) {
            romConfig.add((subnet >> 16));
            romConfig.add((subnet & 0x0000ffff));
        } else {
            romConfig.add(0xffff);
            romConfig.add(0xff00);
        }

        // [5] - Board serial number (0 is no change)
        romConfig.add(0x0000);

        // [6]
        romConfig.add(port);

        // [7] [8] [9]
        if (macAddr != -1) {
            romConfig.add((int)(macAddr >> 32));
            romConfig.add((int)(macAddr >> 16));
            romConfig.add(((int)macAddr & 0x0000ffff));
        } else {
            romConfig.add(0xceba);
            romConfig.add(0xf100);
            romConfig.add(0x0000);
        }

        // [10]
        romConfig.add(0x0000);
    }


	/**
     * Verifies that the previously written rom configuration was done correctly
     * Status registers 2-10 should match config registers 1-9 (index origin 1?)
     * @return True if the ROM configuration was successfully written
     */
    public boolean verifyROMReadback() {

        for (int i = 0; i < 8; i++) {
            if (m_Registers[i] != m_Registers[NUM_REGISTERS + i + 1]) {
                return false;
            }
        }

        return true;
    }


	/**
     * Prep for rom readback by configuring config register 0
     */
    public boolean armROMReadConfiguration() {
        if (romConfig != null) {
            romConfig.set(0, 0x0A00);
            return true;
        }

        return false;
    }


	/**
     * Prep for ROM config write by copying the rom config array into the register array, saves old register values
     */
    public void armROMConfiguration() {
        oldRegs = new Vector<>(11);

        for (int i = 0; i < romConfig.size(); i++) {
            oldRegs.add(m_Registers[i]);
            m_Registers[i] = romConfig.get(i);
        }
    }


	/**
     * Copy saved register values back into the register array and discard saved register list
     */
    public void disarmROMConfiguration() {
        for (int i = 0; i < oldRegs.size(); i++) {
            m_Registers[i] = oldRegs.get(i);
        }

        oldRegs.clear();
        oldRegs = null;
        romConfig.clear();
        romConfig = null;
    }


	/**
     * @return True if the ROM configuration is currently armed
     */
    public boolean isROMConfigArmed() {
        return oldRegs != null;
    }

}
