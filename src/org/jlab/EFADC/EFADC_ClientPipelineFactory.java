//
//  EFADC_ClientPipelineFactory.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jlab.EFADC.command.CommandEncoder;

import java.util.logging.Logger;

import static org.jboss.netty.channel.Channels.pipeline;

public class EFADC_ClientPipelineFactory implements ChannelPipelineFactory {

	private final EFADC_ChannelContext	m_Context;


	EFADC_ClientPipelineFactory(EFADC_ChannelContext ctx) {
		m_Context = ctx;
	}


	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline p = pipeline();

		//p.addLast("idlehandler", new EFADC_IdleHandler());
		p.addLast("encoder", new CommandEncoder());
		p.addLast("decoder", new EFADC_FrameDecoder());

		// Set global context as attachment for above handlers
		for (ChannelHandler handler : p.toMap().values()) {
			ChannelHandlerContext ctx = p.getContext(handler);
			
			if (ctx.getAttachment() == m_Context)
				Logger.getLogger("global").info("Global context already attached to " + handler);

			ctx.setAttachment(m_Context);
		}

		return p;
	}
}
