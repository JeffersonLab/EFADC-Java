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
import org.jlab.EFADC.Client;
import org.jlab.EFADC.EFADC_DataEvent;
import org.jlab.EFADC.EventSet;
import org.jlab.EFADC.RegisterSet;

public interface ClientHandler extends ChannelUpstreamHandler {

	public int getEventCount();
	public int getMissedEventCount();

	public void connected(Client client);
	public void bufferReceived(ChannelBuffer buffer);
	public void eventReceived(EFADC_DataEvent event);
	public void eventSetReceived(EventSet set);
	public void error(String strError);
	public void registersReceived(RegisterSet regs);

	public boolean IsCMP();
}