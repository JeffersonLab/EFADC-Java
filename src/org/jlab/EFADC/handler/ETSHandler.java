package org.jlab.EFADC.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.*;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.Client;
import org.jlab.EFADC.DeviceInfo;
import org.jlab.EFADC.EFADC_DataEvent;
import org.jlab.EFADC.EventSet;

public class ETSHandler extends AbstractClientHandler implements ClientHandler {

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
	public void deviceInfoReceived(DeviceInfo info) {

	}

	@Override
	public void registersReceived(RegisterSet regs) {}

	public void registersReceived(ETS_EFADC_RegisterSet regs) {}

	public void registersReceived(ETS_RegisterSet regs) {}
}
