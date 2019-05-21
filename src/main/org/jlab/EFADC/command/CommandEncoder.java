//
//  CommandEncoder.java
//  EFADC_java
//
//  Created by John McKisson on 1/25/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//

package org.jlab.EFADC.command;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import org.jlab.EFADC.RegisterSet;

import java.util.List;
import java.util.logging.Logger;

/**
 * Encode POJO commands to ChannelBuffer
 */
public class CommandEncoder extends MessageToMessageEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object obj, List<Object> out) throws Exception {

		/*if (obj instanceof EFADC_RegisterSet) {
			EFADC_RegisterSet regSet = (EFADC_RegisterSet)obj;

			return regSet.encode(true);

		} else if (obj instanceof CMP_RegisterSet) {
			CMP_RegisterSet cmpRegSet = (CMP_RegisterSet)obj;

			return cmpRegSet.encode();

		} */

/*
		if (obj instanceof RegisterSet) {
			RegisterSet reg = (RegisterSet)obj;

			out.add(reg.encode());

			//out.add(wrappedBuffer(reg.encode()));

		}
*/

		if (obj instanceof ByteBuf) {
			ByteBuf buf = (ByteBuf)obj;

			out.add(buf);

		} else
			Logger.getGlobal().warning("Invalid object type for encoder");

	}


}