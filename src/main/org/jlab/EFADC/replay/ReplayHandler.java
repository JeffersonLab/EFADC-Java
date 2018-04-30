package org.jlab.EFADC.replay;

import org.jboss.netty.channel.*;

import java.util.logging.*;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 2/28/12
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplayHandler extends SimpleChannelUpstreamHandler {
	
	private static final Logger logger = Logger.getLogger(ReplayHandler.class.getName());

	private boolean verbose = false;

	@Override
	public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			logger.info(e.toString());
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			logger.info(e.toString());
		}
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			logger.info(e.toString());
		}
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			logger.info(e.toString());
		}
	}

	@Override
	public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) {
		if (verbose) {
			logger.info(e.toString());
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		logger.info(e.toString());
		e.getChannel().write(e.getMessage());
	}
	

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
		e.getChannel().close();
	}
}
