//
//  BasicClientHandler.java
//  EFADC_java
//
//  Created by John McKisson on 2/16/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.Client;
import org.jlab.EFADC.EFADC_DataEvent;
import org.jlab.EFADC.EventSet;

public class BasicClientHandler extends AbstractClientHandler implements ClientHandler {

	@Override
	public void connected(Client client) {
	}

	@Override
	public void bufferReceived(ChannelBuffer buffer) {
	}

	@Override
	public void eventReceived(EFADC_DataEvent event) {
	}

	@Override
	public void eventSetReceived(EventSet set) {
	}

	@Override
	public void error(String strError) {
	}

	//@Override
	//public void registersReceived(RegisterSet regs) {}
}
