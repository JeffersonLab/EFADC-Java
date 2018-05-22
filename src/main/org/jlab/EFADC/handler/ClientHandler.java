//
//  ClientHandler.java
//  EFADC_java
//
//  Created by John McKisson on 2/16/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jlab.EFADC.*;

public interface ClientHandler extends ChannelUpstreamHandler {

	int getEventCount();
	int getMissedEventCount();

	void connected(Client client);
	void bufferReceived(ChannelBuffer buffer);
	void eventReceived(EFADC_DataEvent event);
	void eventSetReceived(EventSet set);
	void registersReceived(RegisterSet regs);
	void samplesReceived(EFADC_SamplesEvent event);

	boolean IsCMP();
}