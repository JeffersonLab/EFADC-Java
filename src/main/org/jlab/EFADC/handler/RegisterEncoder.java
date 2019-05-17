//
//  CommandEncoder.java
//  EFADC_java
//
//  Created by John McKisson on 1/25/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//

package org.jlab.EFADC.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import org.jlab.EFADC.RegisterSet;

import java.util.List;

/**
 * Encode POJO commands to ChannelBuffer
 */
public class RegisterEncoder extends MessageToMessageEncoder<RegisterSet> {

	@Override
	protected void encode(ChannelHandlerContext ctx, RegisterSet reg, List<Object> out) throws Exception {

		out.add(reg.encode());

	}


}