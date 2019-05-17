//
//  DeviceInfoHandler.java
//  EFADC_java
//
//  Created by John McKisson on 5/16/19.
//  Copyright (c) 2019 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jlab.EFADC.*;


public class DeviceInfoHandler extends SimpleChannelInboundHandler<DeviceInfo> {

	EFADC_ChannelContext context;

	public DeviceInfoHandler(EFADC_ChannelContext c) {
		context = c;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, DeviceInfo message) throws Exception {

		ClientHandler listener = context.getListener();
		if (listener != null)
			listener.deviceInfoReceived(message);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}