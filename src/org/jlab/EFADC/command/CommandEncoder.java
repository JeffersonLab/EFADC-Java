//
//  CommandEncoder.java
//  EFADC_java
//
//  Created by John McKisson on 1/25/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//

package org.jlab.EFADC.command;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;

import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jlab.EFADC.CMP_RegisterSet;
import org.jlab.EFADC.EFADC_RegisterSet;
import org.jlab.EFADC.RegisterSet;

/**
 * Encode POJO commands to ChannelBuffer
 */
@Sharable
public class CommandEncoder extends OneToOneEncoder {
	
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object obj) throws Exception {

		/*if (obj instanceof EFADC_RegisterSet) {
			EFADC_RegisterSet regSet = (EFADC_RegisterSet)obj;
			
			return regSet.encode(true);

		} else if (obj instanceof CMP_RegisterSet) {
			CMP_RegisterSet cmpRegSet = (CMP_RegisterSet)obj;

			return cmpRegSet.encode();
			
		} */

		if (obj instanceof RegisterSet) {
			RegisterSet reg = (RegisterSet)obj;

			return reg.encode();

		} if (obj instanceof ChannelBuffer) {
			ChannelBuffer buf = (ChannelBuffer)obj;
			return buf;
			
		} else
			return null;

	}
}