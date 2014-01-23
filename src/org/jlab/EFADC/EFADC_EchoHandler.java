//
//  EFADC_EchoHandler.java
//  EFADC_java
//
//  Created by John McKisson on 1/26/12.
//  Copyright (c) 2012 Jefferson Lab. All rights reserved.
//
package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

import java.util.logging.Logger;

public class EFADC_EchoHandler extends SimpleChannelHandler {

	private static final Logger logger = Logger.getLogger("global");

	
	ChannelBuffer echoBuffer;
	
	public EFADC_EchoHandler() {
		echoBuffer = null;
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		
		if (e instanceof MessageEvent) {	//this is a read
			//logger.info(":: " + e);
			MessageEvent me = (MessageEvent)e;
			Object mo = me.getMessage();
			//logger.info(">> " + mo);
			
			if (mo instanceof ChannelBuffer) {
				ChannelBuffer buf = (ChannelBuffer)mo;
				
				String str = "";
				/*
				str = "Read << ";
				for (int j = 0; j < buf.capacity(); j++) {
					byte b = buf.getByte(j);
					str += String.format("%02x ", b);
				}
				logger.info(str);
				*/
				
				if (echoBuffer != null) {
				
					short type = buf.getUnsignedByte(2);
					
					if (type != 0x03) {
				
						if (echoBuffer.compareTo(buf) == 0) {
							
							echoBuffer = null;
							
							//logger.info("Verified echo response");
							
							//Set the EchoFuture as succeeded
							EFADC_ChannelContext chanContext = (EFADC_ChannelContext)ctx.getAttachment();
							Object obj = chanContext.getObject();
							if (obj != null && obj instanceof ChannelFuture) {
								ChannelFuture echoFuture = (ChannelFuture)obj;
								chanContext.setObject(null);
								//logger.info("Set EchoFuture success, attachment clear " + ctx);
								echoFuture.setSuccess();
								
								//don't forward echo'd packet
								return;
								
							} else {
								if (obj == null)
									logger.info("Null ChannelFuture, context: " + chanContext);
								else if (!(obj instanceof ChannelFuture))
									logger.info("Attachment not ChannelFuture instance");
							}
							
						} else {
							logger.info("Echo response mismatch!");
							logger.info("Received Buffer:");
							str = "";
							for (int j = 0; j < buf.capacity(); j++) {
								byte b = buf.getByte(j);
								str += String.format("%02x ", b);
							}
							logger.info(str);
								
							logger.info("VS");
							str = "";
							for (int j = 0; j < echoBuffer.capacity(); j++) {
								byte b = echoBuffer.getByte(j);
								str += String.format("%02x ", b);
							}
							logger.info(str);
						}
					}
				} //else
				//	logger.info(String.format("EchoBuffer null on read, type %x", type));

			}
			
		}
		
		super.handleUpstream(ctx, e);
	}
	
	
	/**
	 * Look at outgoing buffers and store a reference to that buffer for later echo comparison
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		
		if (e instanceof MessageEvent) {	//this is a write
			//logger.info(":: " + e);
			MessageEvent me = (MessageEvent)e;
			Object mo = me.getMessage();
			
			if (mo instanceof ChannelBuffer) {
				ChannelBuffer buf = (ChannelBuffer)mo;
				
				if (echoBuffer == null) {
					echoBuffer = buf;
					//logger.info("Set echo response");
				} else
					logger.info("Wrote packet without previous echo response!");
			}
		}
		
		super.handleDownstream(ctx, e);
	}

}
