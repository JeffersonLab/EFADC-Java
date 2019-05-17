//
//  EFADC_BufferWriter.java
//  EFADC_java
//
//  Created by John McKisson on 2/10/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Network pipeline handler to write buffer bytes directly to disk.
 */
public class EFADC_BufferWriter extends SimpleChannelInboundHandler<ByteBuf> {

	FileChannel outChan = null;

	public EFADC_BufferWriter(FileChannel chan) {
		outChan = chan;
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		if (outChan != null) {
			try {
				outChan.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
	
		if (outChan != null && e instanceof MessageEvent) {	
			MessageEvent me = (MessageEvent)e;
			Object mo = me.getMessage();
			
			if (mo instanceof ByteBuf) {
				ByteBuf buf = (ByteBuf) mo;

				outChan.write(buf);
			}
		}
	
		super.handleUpstream(ctx, e);
	}
	 */

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
		if (outChan != null) {
			outChan.write(byteBuf.nioBuffer());
		}
	}
}
