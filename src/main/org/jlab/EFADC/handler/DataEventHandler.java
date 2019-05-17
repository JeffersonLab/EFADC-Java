//
//  AbstractClientHandler.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jlab.EFADC.*;

import java.util.logging.Logger;


public class DataEventHandler extends SimpleChannelInboundHandler<EFADC_DataEvent> {

	int lastEventID;
	int eventCount;
	int totalMissed;
	boolean verbose = false;
	boolean isCMP = false;

	private EFADC_ChannelContext context;
	private EFADC_EventAggregator m_Aggregator;

	public DataEventHandler(EFADC_ChannelContext ctx) {
		this(false);

		context = ctx;
	}

	public DataEventHandler(boolean verbose) {
		resetCounters();
		this.verbose = verbose;
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
	public void channelRead0(ChannelHandlerContext ctx, EFADC_DataEvent event) throws Exception {

		if (event.trigId < lastEventID)
			lastEventID = -1;
		else if (event.trigId != lastEventID + 1) {
			int missedEvents = event.trigId - lastEventID;
			totalMissed += missedEvents;
			//logger.info(String.format("%d missed, %04x - %04x", missedEvents, lastEventID + 1, event.trigId - 1));
		}

		lastEventID = event.trigId;

		if (context.isCMP()) {
			// Aggregate events from a CMP or ETS

			if (m_Aggregator == null) {
				Logger.getGlobal().info("Aggregator null, iscmp = " + isCMP);
			}

			Object ret = m_Aggregator.process(event);

			if (ret instanceof EventSet) {

				eventCount++;

				// EventSet was popped off the end of the list, ready for processing
				context.getListener().eventSetReceived((EventSet)ret);
			}

		} else {
			eventCount++;
			context.getListener().eventReceived(event);
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}