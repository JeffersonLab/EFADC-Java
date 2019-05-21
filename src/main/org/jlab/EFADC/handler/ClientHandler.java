//
//  ClientHandler.java
//  EFADC_java
//
//  Created by John McKisson on 2/16/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC.handler;

import io.netty.buffer.ByteBuf;
import org.jlab.EFADC.*;

public interface ClientHandler {

	//void connected(Client client);
	void bufferReceived(ByteBuf buffer);
	void eventReceived(EFADC_DataEvent event);
	void eventSetReceived(EventSet set);
	void registersReceived(RegisterSet regs);
	void deviceInfoReceived(DeviceInfo info);

}