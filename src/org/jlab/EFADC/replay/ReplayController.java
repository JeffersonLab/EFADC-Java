package org.jlab.EFADC.replay;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jlab.EFADC.EFADC_FrameDecoder;
import org.jlab.EFADC.handler.ClientHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 2/28/12
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplayController {

	Channel m_ServerChannel;
	ChannelFuture m_ReplayClientChannelFuture;
	FileChannel m_ReplayFileChannel;
	ClientBootstrap m_ReplayClientBootstrap;
	ServerBootstrap m_ReplayServerBootstrap;
	ReplayHandler m_ReplayHandler;
	LocalAddress m_LocalAddress;
	long	m_ThrottleTime;

	public ReplayController(ClientHandler handler) {
		m_ReplayClientChannelFuture = null;
		m_ReplayFileChannel = null;

		m_LocalAddress = new LocalAddress("replay");

		m_ReplayHandler = new ReplayHandler();

		// Configure the server
		m_ReplayServerBootstrap = new ServerBootstrap(new DefaultLocalServerChannelFactory());

		ChannelPipeline p = m_ReplayServerBootstrap.getPipeline();
		p.addLast("decoder", new EFADC_FrameDecoder());
		p.addLast("handler", handler);

		m_ReplayServerBootstrap.setOption("reuseAddress", true);

		// Start up the server
		m_ServerChannel = m_ReplayServerBootstrap.bind(m_LocalAddress);

		m_ThrottleTime = 25;

		Logger.getLogger("global").info("ReplayController initialized");
	}

	public void setThrottle(long ms) {
		m_ThrottleTime = ms;
	}

	public void setReplayFileChannel(FileChannel chan) {

		m_ReplayFileChannel = chan;

		// Configure the client
		m_ReplayClientBootstrap = new ClientBootstrap(new DefaultLocalClientChannelFactory());

		// Set up the client-side pipeline factory
		m_ReplayClientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(m_ReplayHandler);
			}
		});

		// Make the connection attempt to the server
		m_ReplayClientChannelFuture = m_ReplayClientBootstrap.connect(m_LocalAddress);
		m_ReplayClientChannelFuture.awaitUninterruptibly();
	}

	public long StartReplay() throws Exception {
		if (m_ReplayFileChannel == null)
			return 0;
		
		Logger.getLogger("global").info("Allocating replay buffer");

		//ByteBuffer replayBuffer = ByteBuffer.allocate(1048576);	// 1MB
		ByteBuffer replayBuffer = ByteBuffer.allocate(524288);	// 512K
		//ByteBuffer replayBuffer = ByteBuffer.allocate(262144);	// 256K
		//ByteBuffer replayBuffer = ByteBuffer.allocate(131072);		// 128K

		Channel replayChannel = m_ReplayClientChannelFuture.getChannel();

		// Read binary data from the file channel
		ChannelFuture lastWriteFuture = null;
		long bytesRead = 0;

		try {
			long maxSize = m_ReplayFileChannel.size();

			Logger.getLogger("global").info(maxSize + " bytes to replay");

			while (bytesRead < maxSize) {
				
				replayBuffer.clear();
				
				long read = m_ReplayFileChannel.read(replayBuffer);
				bytesRead += read;
			
				replayBuffer.flip();

				// The documentation says this wraps the NIO buffer's current slice, which should be the entire buffer
				ChannelBuffer wrappedBuffer = ChannelBuffers.wrappedBuffer(replayBuffer);

				//Logger.getLogger("global").info(bytesRead + " bytes replayed...");

				// Sends the received line to the server
				lastWriteFuture = replayChannel.write(wrappedBuffer);
				
				Thread.sleep(m_ThrottleTime);
			}

		} catch (Throwable e) {
			throw new RuntimeException(e);

		} finally {
			
			Logger.getLogger("global").info("Cleaning up replay stuff");

			// Wait until all messages are flushed before closing the channel
			if (lastWriteFuture != null) {
				lastWriteFuture.awaitUninterruptibly();
			}
			replayChannel.close();

			if (!replayChannel.getCloseFuture().awaitUninterruptibly(2000))
				Logger.getLogger("global").info("Timeout while closing bound replay client channel");

			cleanup();
		}

		return bytesRead;
	}

	public void cleanup() {
		if (!m_ServerChannel.close().awaitUninterruptibly(2000))
			Logger.getLogger("global").info("Timeout while closing bound replay server channel");

		try {
			m_ReplayFileChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Release all resources used by the replay local transport
		m_ReplayClientBootstrap.releaseExternalResources();
		m_ReplayServerBootstrap.releaseExternalResources();
	}
}
