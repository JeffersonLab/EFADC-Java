//
//  RegisterSetHandler.java
//  EFADC_java
//
//  Created by John McKisson on 5/16/19.
//  Copyright (c) 2019 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jlab.EFADC.*;

import java.util.logging.Logger;


public class RegisterSetHandler extends SimpleChannelInboundHandler<RegisterSet> {

	private boolean isCMP = false;

	public boolean IsCMP() {
		return isCMP;
	}

	private EFADC_ChannelContext context;

	public RegisterSetHandler(EFADC_ChannelContext ctx) {
		context = ctx;
	}


	@Override
	public void channelRead0(ChannelHandlerContext ctx, RegisterSet message) throws Exception {

		context.getAggregator().flush();

		registersReceived(message);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

	public void registersReceived(RegisterSet regs) {

		Logger.getGlobal().info("registersReceived() " + regs.toString());

		if (regs instanceof ETS_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::ETS_RegisterSet");

			ETS_RegisterSet eRegs = (ETS_RegisterSet)regs;
			eRegs.client().setRegisterSet(eRegs);

			if (!context.isCMP()) {
				Logger.getGlobal().info("Set IsCMP true");
				context.setCMP(true);
			}

			context.getListener().registersReceived(eRegs);

		} else if (regs instanceof ETS_EFADC_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::ETS_EFADC_RegisterSet");

			ETS_EFADC_RegisterSet eRegs = (ETS_EFADC_RegisterSet)regs;
			eRegs.client().setRegisterSet(eRegs);

			if (!context.isCMP()) {
				Logger.getGlobal().info("Set IsCMP true");
				context.setCMP(true);
			}

			context.getListener().registersReceived(eRegs);

		} else if (regs instanceof EFADC_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::EFADC_RegisterSet");

			EFADC_RegisterSet eRegs = (EFADC_RegisterSet)regs;

			for (int i = 0; i < EFADC_RegisterSet.NUM_REGS; i++) {
				System.out.printf("%02X\n", regs.getRegister(i));
			}

			Logger.getGlobal().info("  Accepted Triggers: " + eRegs.getAcceptedTrigs());
			Logger.getGlobal().info("  Missed Triggers: " + eRegs.getMissedTrigs());

			Logger.getGlobal().info("Set IsCMP false");
			isCMP = false;

			context.getListener().registersReceived(eRegs);

		} else if (regs instanceof CMP_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::CMP_RegisterSet");

			StringBuilder strB = new StringBuilder();

			CMP_RegisterSet cRegs = (CMP_RegisterSet)regs;

			for (int i = 1; i < cRegs.getADCCount() + 1; i++) {

				EFADC_RegisterSet eRegs = null;

				// Why 0 here?
				try {
					eRegs = cRegs.getADCRegisters(i);

					context.getListener().registersReceived(eRegs);
				} catch (EFADC_InvalidADCException e) {
					Logger.getGlobal().warning("Invalid ADC Selection: " + i);
					continue;
				}


				//Logger.getGlobal().info(eRegs.toString());

				/*
				strB.append("[" + i + "] ");

				for (int j = 0; j < EFADC_RegisterSet.NUM_REGS; j++) {
					strB.append(String.format("%02X ", regs.getRegister(j)));
				}

				Logger.getGlobal().info(strB.toString());
				Logger.getGlobal().info("Accepted Triggers: " + eRegs.acceptedTrigs);
				Logger.getGlobal().info("Missed Triggers: " + eRegs.missedTrigs);

				strB.setLength(0);
				*/
			}

			if (!context.isCMP()) {
				//Logger.getGlobal().info("Set IsCMP true");
				context.setCMP(true);
			}
		}
	}



}