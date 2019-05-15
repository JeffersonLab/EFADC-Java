//
//  EFADC_BufferWriter.java
//  EFADC_java
//
//  Created by John McKisson on 2/10/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

import java.nio.channels.FileChannel;

/**
 * Network pipeline handler to write buffer bytes directly to disk.
 */
public class EFADC_BufferWriter extends SimpleChannelUpstreamHandler {

	FileChannel outChan = null;

	public EFADC_BufferWriter(FileChannel chan) {
		outChan = chan;
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (outChan != null)
			outChan.close();
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
	
		if (outChan != null && e instanceof MessageEvent) {	
			MessageEvent me = (MessageEvent)e;
			Object mo = me.getMessage();
			
			if (mo instanceof ChannelBuffer) {
				ChannelBuffer buf = (ChannelBuffer)mo;

				outChan.write(buf.toByteBuffer());
			}
		}
	
		super.handleUpstream(ctx, e);
	}
}
