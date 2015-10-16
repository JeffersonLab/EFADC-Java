package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.Arrays;

/**
 * Created by john on 8/18/15.
 */
public class EFADC_RegistersV2 extends EFADC_RegisterSet implements EFADC_Registers {

    public static final int NUM_REGISTERS = 20;	//Not including status regs, or register 0
    public static final int NUM_STATUS = 7;
    public static final int DATA_SIZE_BYTES = (NUM_REGISTERS + NUM_STATUS) * 2;

    static final int Mode_Mask		= 0x200;

    static final int Reset_Mask = 0x1000;

    static final int[] s_DefaultRegs = new int[] {0x200F, 0x0016, 0x0339, 0x0BD8, 0x03e0, 0x03e0, 0x03e0, 0x03e0, 0xF0F0, 0xF0F0, 0x00F0, 0x0202, 0x0202, 0x0001, 0x0001, 0x0001, 0x0001, 0x1FFA, 0x0000, 0x0CE4};

    static final String[] s_Descriptions = new String[] {
		/* 0 */		"13: Sync; 12: Reset ADC; 10: 1 - Free Running Trigger, rate set by conf18 bits 11:0; 9: Mode (0 SUM, 1 Samples); 8..0: Integration Window",
		/* 1 */		"8..0: Number of Samples Before (NSB).  Delay ADC samples to compensate for triggerIn latency; 14..11: ADC select (when 15=>1); 15: 1 = Write to all ADCs",
		/* 2 */		"9..0: Trigger Data Fifo Almost Full Threshold.",
		/* 3 */		"11..0: Channel Threshold, to detect number of pulses in window (pile up)",
		/* 4 */		"13..0: Detector 1 Threshold",
		/* 5 */		"13..0: Detector 2 Threshold",
		/* 6 */		"13..0: Detector 3 Threshold",
		/* 7 */		"13..0: Detector 4 Threshold",
		/* 8 */		"15..2: Det 2 OR Entry; 11..8: Det 2 AND Entry; 7..4: Det 1 OR Entry; 3..0: Det 1 AND Entry",
		/* 9 */		"15..2: Det 4 OR Entry; 11..8: Det 4 AND Entry; 7..4: Det 3 OR Entry; 3..0: Det 3 AND Entry",
		/* 10 */	"11..4: Coincident Window Width; 3..0: Det 4,3,2,1 COM Entry",
		/* 11 */	"15..8: Det 2 HitOut pulse width, 7..0: Det 1 HitOut pulse width",
		/* 12 */	"15..8: Det 4 HitOut pulse width, 7..0: Det 3 HitOut pulse width",
		/* 13 */	"13..0: Det 1 HitOut Delay",
		/* 14 */	"13..0: Det 2 HitOut Delay",
		/* 15 */	"13..0: Det 3 HitOut Delay",
		/* 16 */	"13..0: Det 4 HitOut Delay",
		/* 17 */	"15: 0 - Select Auto Pulse for Playback or free trigger, 1 - Playback on rising edge of bit 14; 14: rising edge play back or free trigger one time; 12..0: Play back pulse or trigger rate (512ns per count)",
		/* 18 */	"15..12: Select DAC; 11..0: DAC Value",
		/* 19 */	"15..8: AD9230 Register # to write bit 7..0 to; 7..0: ADC Register Value"
    };

    EFADC_RegistersV2() {
        m_Registers = Arrays.copyOf(s_DefaultRegs, s_DefaultRegs.length);
    }

    EFADC_RegistersV2(int val) {
        super(val);

        m_Registers = Arrays.copyOf(s_DefaultRegs, s_DefaultRegs.length);
    }

    public String getRegisterDescription(int i) throws InvalidRegisterException {
        if (i < 0 || i > NUM_REGISTERS)
            throw new InvalidRegisterException();

        return s_Descriptions[i];
    }

    public String toString() {
        StringBuilder strB = new StringBuilder("EFADC v2 Register Set\n");

        for (int reg : register) {
            strB.append(String.format("%04x ", reg));
        }
        strB.append("\n");

        strB.append(String.format("[01] %04x - Integration Window: %d\n", register[0], getIntegrationWindow()));
        strB.append(String.format("[02] %04x\n", register[1]));
        strB.append(String.format("[03] %04x\n", register[2]));
        strB.append(String.format("[04] %04x\n", register[3]));
        strB.append(String.format("[05] %04x - Det 1 Thresh: %d\n", register[4], register[4] & 0x3fff));
        strB.append(String.format("[06] %04x - Det 2 Thresh: %d\n", register[5], register[5] & 0x3fff));
        strB.append(String.format("[07] %04x - Det 3 Thresh: %d\n", register[6], register[6] & 0x3fff));
        strB.append(String.format("[08] %04x - Det 4 Thresh: %d\n", register[7], register[7] & 0x3fff));
        strB.append(String.format("[11] %04x - Coinc Window Width: %d\n", register[REG_11], getCoincidenceWindowWidth()));
        strB.append(String.format("Version: %d\n", version));
        strB.append(String.format("Accepted Triggers: %d\n", acceptedTrigs));
        strB.append(String.format("Missed Triggers: %d\n", missedTrigs));
        strB.append(String.format("Something Else: %04X\n", unknown));
        strB.append(String.format("FPGA Die Temp (C): %.2f\n", fpgaTemp));

        return strB.toString();
    }

    public void setBiasDAC(int channel, int value) {
        register[REG_19] = (value & 0xFFF) | (channel << 12);
    }


    public int getCoincidenceWindowWidth() {
        return register[REG_11] >> 4;
    }


    public void setCoincidenceWindowWidth(int width) {
        register[REG_11] &= (register[REG_11] & 0x000f);
        register[REG_11] |= (width << 4);
    }

    public void setCoincidenceTable(int reg1, int reg2) {
        register[REG_9] = reg1;
        register[REG_10] = reg2;
    }

    public int getIntegrationWindow() {
        return register[REG_1] & 0x01ff;
    }

    public void setIntegrationWindow(int width) {
        register[REG_1] &= 0xff00;
        register[REG_1] |= (width & 0x01ff);

        //Logger.getLogger("global").info(String.format("Integration Window: %d\n", getIntegrationWindow()));
    }

    public void setMode(int mode) {
        //0 - Normal Mode
        //1 - Sampling Mode

        if (mode == 0)
            register[REG_1] &= ~Mode_Mask;
        else if (mode == 1)
            register[REG_1] |= Mode_Mask;
    }


    public void setNSB(int value) {
        register[REG_2] &= 0xff00;
        register[REG_2] |= (value & 0x01ff);
    }


    /**
     * Configure self trigger mode
     * Bit 10 of Config Register 1 enables Self Triggering
     * Bits 12..0 of Config Register 18 control self trigger rate
     * Bit 15 of Config Register 18 must be 0
     * @param active
     * @param rate
     */
    public void setSelfTrigger(boolean active, int rate) {
        int regVal = register[REG_1];

        if (active) {

            regVal |= 0x400;

            register[REG_1] = regVal;

            regVal = register[REG_18];

            regVal = (regVal & 0x7000) | (rate & 0x1fff);

            register[REG_18] = regVal;

        } else {

            regVal &= ~0x400;

            register[REG_1] = regVal;
        }
    }


    public boolean setThreshold(int group, int value) {
        int addr;
        if (group == 0) addr = REG_5;
        else if (group == 1) addr = REG_6;
        else if (group == 2) addr = REG_7;
        else if (group == 3) addr = REG_8;
        else return false;

        return setRegister(addr, value);
    }

    public boolean decode(ChannelBuffer frame) {

        super.decode(frame, NUM_REGISTERS);

        version = frame.readUnsignedShort();

        acceptedTrigs = frame.readUnsignedInt();
        missedTrigs = frame.readUnsignedInt();

        unknown = frame.readUnsignedShort();

        fpgaTemp = (frame.readUnsignedShort() * (503.975f/1024.0f)) - 273.15f;


        lastUpdated = System.currentTimeMillis();


        //System.out.printf("%04x %04x %04x %04x %04x %04x %04x\n", status1, status3, status3, status4, status5, status6, status7);

        //System.out.println("Accepted: " + acceptedTrigs + "  Missed: " + missedTrigs);
        return true;
    }
}
