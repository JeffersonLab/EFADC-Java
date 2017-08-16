//
//  AbstractClientHandler.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jlab.EFADC.*;

import java.util.logging.Logger;


public abstract class AbstractClientHandler extends SimpleChannelUpstreamHandler {
	
	int lastEventID;
	int eventCount;
	int totalMissed;
	boolean verbose = false;
	boolean isCMP = false;

	private EFADC_EventAggregator m_Aggregator;

	public AbstractClientHandler() {
		this(false);
	}
	
	public AbstractClientHandler(boolean verbose) {
		resetCounters();
		this.verbose = verbose;
	}

	public boolean IsCMP() {
		return isCMP;
	}

	public void SetCMP(boolean val) {
		isCMP = val;
		//Logger.getLogger("global").info("SetCMP " + val);
		if (isCMP && m_Aggregator == null) {
			m_Aggregator = new EFADC_EventAggregator(10);
			m_Aggregator.setHandler((ClientHandler)this);
		}
	}
	
	public void resetCounters() {
		lastEventID = -1;
		eventCount = 0;
		totalMissed = 0;
	}
	
	public int getEventCount() {
		return eventCount;
	}
	
	public int getMissedEventCount() {
		return totalMissed;
	}

	@Override
	public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			Logger.getGlobal().info(e.toString());
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			Logger.getGlobal().info(e.toString());
		}
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			Logger.getGlobal().info(e.toString());
		}
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			Logger.getGlobal().info(e.toString());
		}
	}

	@Override
	public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			Logger.getGlobal().info(e.toString());
		}
	}


	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object message = e.getMessage();

		if (message instanceof EFADC_DataEvent) {
			EFADC_DataEvent event = (EFADC_DataEvent)message;

			if (event.trigId < lastEventID)
				lastEventID = -1;
			else if (event.trigId != lastEventID + 1) {
				int missedEvents = event.trigId - lastEventID;
				totalMissed += missedEvents;
				//logger.info(String.format("%d missed, %04x - %04x", missedEvents, lastEventID + 1, event.trigId - 1));
			}

			lastEventID = event.trigId;


			if (isCMP) {
				// Aggregate events from a CMP

				if (m_Aggregator == null) {
					Logger.getGlobal().info("Aggregator null, iscmp = " + isCMP);
				}

				Object ret = m_Aggregator.process(event);

				if (ret instanceof EventSet) {

					eventCount++;

					// EventSet was popped off the end of the list, ready for processing
					eventSetReceived((EventSet)ret);
				}

			} else {
				eventCount++;
				eventReceived(event);
			}

		} else if (message instanceof RegisterSet) {

			if (m_Aggregator != null)
				m_Aggregator.flush();
			
			registersReceived((RegisterSet)message);
			
		} else if (message instanceof ChannelBuffer) {
			//logger.info("Stray ChannelBuffer received");
			/*
			ChannelBuffer buf = (ChannelBuffer)message;
			System.out.println("Uncharacterized message received");
			for (int j = 0; j < buf.capacity(); j++) {
				byte b = buf.getByte(j);
				System.out.printf("%02x ", b);
			}
			System.out.println();
			*/
			
			bufferReceived((ChannelBuffer)message);
		} else if (message instanceof DeviceInfo) {
			deviceInfoReceived((DeviceInfo)message);
		}
			
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().close();
	}

	public void registersReceived(RegisterSet regs) {

		Logger.getGlobal().info("registersReceived() " + regs.toString());

		if (regs instanceof ETS_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::ETS_RegisterSet");

			ETS_RegisterSet eRegs = (ETS_RegisterSet)regs;
			eRegs.client().setRegisterSet(eRegs);

		} else if (regs instanceof ETS_EFADC_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::ETS_EFADC_RegisterSet");

			ETS_EFADC_RegisterSet eRegs = (ETS_EFADC_RegisterSet)regs;
			eRegs.client().setRegisterSet(eRegs);

		} else if (regs instanceof EFADC_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::EFADC_RegisterSet");

			EFADC_RegisterSet eRegs = (EFADC_RegisterSet)regs;

			for (int i = 0; i < EFADC_RegisterSet.NUM_REGS; i++) {
				System.out.printf("%02X\n", regs.getRegister(i));
			}

			Logger.getGlobal().info("  Accepted Triggers: " + eRegs.getAcceptedTrigs());
			Logger.getGlobal().info("  Missed Triggers: " + eRegs.getMissedTrigs());

			isCMP = false;

		} else if (regs instanceof CMP_RegisterSet) {
			//Logger.getGlobal().info("  >>registersReceived::CMP_RegisterSet");

			StringBuilder strB = new StringBuilder();

			CMP_RegisterSet cRegs = (CMP_RegisterSet)regs;

			for (int i = 1; i < cRegs.getADCCount() + 1; i++) {

				EFADC_RegisterSet eRegs = null;

				// Why 0 here?
				try {
					eRegs = cRegs.getADCRegisters(i);
				} catch (EFADC_InvalidADCException e) {
					Logger.getGlobal().warning("Invalid ADC Selection: " + i);
					continue;
				}


				//Logger.getLogger("global").info(eRegs.toString());

				/*
				strB.append("[" + i + "] ");

				for (int j = 0; j < EFADC_RegisterSet.NUM_REGS; j++) {
					strB.append(String.format("%02X ", regs.getRegister(j)));
				}

				Logger.getLogger("global").info(strB.toString());
				Logger.getLogger("global").info("Accepted Triggers: " + eRegs.acceptedTrigs);
				Logger.getLogger("global").info("Missed Triggers: " + eRegs.missedTrigs);

				strB.setLength(0);
				*/
			}

			if (isCMP == false) {
				//Logger.getLogger("global").info("Set IsCMP true");
				SetCMP(true);
			}
		}
	}

	public abstract void deviceInfoReceived(DeviceInfo info);
	
	public abstract void bufferReceived(ChannelBuffer buffer);
	public abstract void eventReceived(EFADC_DataEvent event);
	public abstract void eventSetReceived(EventSet set);

}