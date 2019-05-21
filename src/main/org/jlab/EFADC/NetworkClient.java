package org.jlab.EFADC;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.DatagramPacketDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jlab.EFADC.command.Command;
import org.jlab.EFADC.handler.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by john on 6/13/17.
 */
public class NetworkClient {

	public static boolean flag_Verbose = false;

	int							m_InterCommandDelay;
	int 						m_TCPPort, m_UDPPort;
	InetSocketAddress			m_TCPSocketAddress, m_UDPSocketAddress;
	Channel						m_UDPChannel = null, m_TCPChannel = null;
	EFADC_ChannelContext		m_GlobalContext;
	ChannelHandler				m_IdleStateHandler = null;
	private ReadTimeoutHandler m_AcquisitionReadTimeoutHandler;
	private Bootstrap			m_Bootstrap = null;

	public NetworkClient() {

		m_GlobalContext = new EFADC_ChannelContext();
		m_GlobalContext.setClient(this);

		EventLoopGroup workerGroup = new NioEventLoopGroup();

		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioDatagramChannel.class);
		b.handler(new ChannelInitializer<NioDatagramChannel>() {

			@Override
			protected void initChannel(NioDatagramChannel ch) throws Exception {

				// why do we get the decode ref here?
				EFADC_FrameDecoder decoder = new EFADC_FrameDecoder();

				// none of these are needed apparently
				//ch.pipeline().addLast("udpEncoder", new DatagramPacketEncoder(new CommandEncoder()));
				//ch.pipeline().addLast("regEncoder", new DatagramPacketEncoder(new RegisterEncoder()));
				//ch.pipeline().addLast("encoder", new CommandEncoder());

				ch.pipeline().addLast("decoder", new DatagramPacketDecoder(decoder));
				ch.pipeline().addLast("deviceInfoHandler", new DeviceInfoHandler(m_GlobalContext));
			}

		});

		m_Bootstrap = b;
	}



	public EFADC_ChannelContext getGlobalContext() {
		return m_GlobalContext;
	}



	public void initReadTimeoutHandler() {
		// Insert read timeout handler into the pipeline
		m_AcquisitionReadTimeoutHandler = new ReadTimeoutHandler( 1);

		try {
			m_UDPChannel.pipeline().addFirst("acqTimeout", m_AcquisitionReadTimeoutHandler);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeTimeoutHandler() {
		try {
			// Remove read timeout handler
			m_UDPChannel.pipeline().remove("acqTimeout");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void SetSocketOptions(Bootstrap b) {
		//Enable broadcast
		b.option(ChannelOption.SO_BROADCAST, false);
		b.option(ChannelOption.TCP_NODELAY, true);

		// Allow packets as large as up to 1024 bytes (default is 768).
		// You could increase or decrease this value to avoid truncated packets
		// or to improve memory footprint respectively.
		//
		// Please also note that a large UDP packet might be truncated or
		// dropped by your router no matter how you configured this option.
		// In UDP, a packet is truncated or dropped if it is larger than a
		// certain size, depending on router configuration.  IPv4 routers
		// truncate and IPv6 routers drop a large packet.  That's why it is
		// safe to send small packets in UDP.
		//b.setOption("receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(2000));

		//This needs to be set higher to avoid the "catching up" behavior where packets are missed near the beginning of an acquisition
		//b.setOption("receiveBufferSize", 1048576);
		//b.setOption("receiveBufferSizePredictorFactory", new AdaptiveReceiveBufferSizePredictorFactory());
	}

	/*
	private void InitTCPControlSocket() {
		m_TCPClientChannelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

		m_TCPClientPipelineFactory = new EFADC_ClientPipelineFactory(m_GlobalContext);

		// TODO add timeout handler for tcp socket

		Tuple2<Bootstrap, Channel> t = InitConnectedChannel(m_TCPClientChannelFactory, m_TCPClientPipelineFactory, m_TCPSocketAddress);

		Bootstrap b = t._1;
		m_TCPChannel = t._2;
	}
	 */


	public boolean isConnected() {
		return (m_UDPChannel != null && m_UDPChannel.isOpen());
	}



	public void connect() throws EFADC_AlreadyConnectedException {

		Logger.getGlobal().entering("connect", "NetworkClient");

		if (m_UDPChannel != null /* && m_UDPChannel.isConnected()*/)
			throw new EFADC_AlreadyConnectedException();


		try {
			m_UDPChannel = m_Bootstrap.bind(0).sync().channel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Logger.getGlobal().log(Level.FINE, "bound channel");

		ChannelPipeline p = m_UDPChannel.pipeline();

		p.addLast("registerHandler", new RegisterSetHandler(m_GlobalContext));
		p.addLast("dataHandler", new DataEventHandler(m_GlobalContext));

		//m_UDPChannel.connect(m_UDPSocketAddress);

		Logger.getGlobal().exiting("connect", "NetworkClient");
	}


	/**
	 * Close sockets and release factory resources
	 */
	public void cleanup() {

		if (m_UDPChannel != null) {
			ChannelFuture cf = m_UDPChannel.close();
			cf.awaitUninterruptibly();
		}

		/*
		if (m_TCPChannel != null) {

			if (m_TCPChannel.isConnected()) {
				ChannelFuture cf = m_TCPChannel.close();
				cf.awaitUninterruptibly();
			}

		}
		*/
	}


	/**
	 * Set remote device address and port.
	 * @param address Device IP address
	 * @param port TCP port, UDP port is port + 1
	 * @throws EFADC_AlreadyConnectedException
	 */
	public void setAddress(String address, int port) throws EFADC_AlreadyConnectedException {
		if (m_UDPChannel != null)
			throw new EFADC_AlreadyConnectedException();

		m_TCPPort = port;
		m_UDPPort = port + 1;
		m_TCPSocketAddress = new InetSocketAddress(address, m_TCPPort);
		m_UDPSocketAddress = new InetSocketAddress(address, m_UDPPort);
	}


	public void setDebug(boolean val) {
		flag_Verbose = val;

		Logger.getGlobal().info("Debug mode " + (flag_Verbose ? "enabled" : "disabled"));
	}


	public void setInterCommandDelay(int delay) {
		m_InterCommandDelay = delay;
	}


	/**
	 * Set ClientHandler delegate
	 * @param listener Delegate object
	 */
	public void setClientListener(ClientHandler listener) {
		m_GlobalContext.setListener(listener);
	}


	/**
	 * Replace existing frame decoder
	 * @param decoder New frame decoder
	 */
	public void setDecoder(EFADC_FrameDecoder decoder) {
		ChannelPipeline p = getPipeline();

		// Remove any existing decoder
		try {
			p.remove("decoder");
		} catch (NoSuchElementException e) {
			Logger.getGlobal().info("setDecoder :: No existing decoder to remove");
		}

		p.addBefore("deviceInfoHandler", "decoder", new DatagramPacketDecoder(decoder));

		decoder.setContext(m_GlobalContext);

		Logger.getGlobal().info("setDecoder: " + decoder);
	}


	public ClientHandler getListener() {
		return m_GlobalContext.getListener();
	}


	public ChannelPipeline getPipeline() {
		return m_UDPChannel.pipeline();
	}


	/**
	 * Set raw output file
	 * @param file File or null to disable
	 * @throws Exception
	 */
	public void SetRawOutputFile(File file) throws IOException {

		if (file == null) {
			setRawOutputChannel(null);
			return;
		}

		FileChannel outChan = new FileOutputStream(file).getChannel();

		setRawOutputChannel(outChan);
	}

	/**
	 * Set raw output file name
	 * @param filename Filename or empty string (or null) to disable
	 * @throws java.io.IOException
	 */
	public void SetRawOutputFile(String filename) throws IOException {

		if (filename == null || filename.equals("")) {
			setRawOutputChannel(null);
			return;
		}

		FileChannel outChan = new FileOutputStream(filename, false).getChannel();

		setRawOutputChannel(outChan);
	}


	private void setRawOutputChannel(FileChannel outChan) {

		ChannelPipeline pipeline = getPipeline();

		if (outChan == null) {
			try {
				pipeline.remove("writer");
			} catch (NoSuchElementException e) {}
		}

		EFADC_BufferWriter writer = new EFADC_BufferWriter(outChan);

		try {
			pipeline.replace("writer", "writer", writer);
		} catch (NoSuchElementException e) {
			pipeline.addFirst("writer", writer);
		}
	}


	/**
	 * TODO: Return echo future or write future depending on the mode the channel is operating in
	 * @param cmd Command to send to the device
	 * @return Result of command acknowledgement
	 */
	public boolean SendCommand(Object cmd) {
		if (m_UDPChannel == null || !m_UDPChannel.isOpen()) {
			Logger.getGlobal().warning("UDP Channel closed when trying to SendCommand");
			return false;
		}

		ChannelFuture echoFuture = null;

		/*
		if (m_EchoHandlerContext != null) {
			echoFuture = Channels.future(m_UDPChannel);

			m_GlobalContext.setObject(echoFuture);

			if (((EFADC_ChannelContext)m_EchoHandlerContext.getAttachment()).getObject() != echoFuture) {
				Logger.getGlobal().warning("Echo future not attached to global context!");
			} //else
			//logger.info("Echo future attached successfully at " + m_GlobalContext.getLastUpdated());


			//echoFuture.addListener(new ChannelFutureListener() {
			//	public void operationComplete(ChannelFuture future) {
			//		logger.info("Command Echo");
			//	}
			//});

		}
		 */

		ChannelFuture writeFuture;

		//Write command object to the pipeline

		/*
		if (m_TCPChannel != null && m_TCPChannel.isActive()) {
			writeFuture = m_TCPChannel.write(cmd);
		} else {
			// Handle case for connectionless datagram channel
			writeFuture = m_UDPChannel.isActive() ? m_UDPChannel.writeAndFlush(cmd) : m_UDPChannel.write(cmd);
		}
		*/
		writeFuture = m_UDPChannel.writeAndFlush(new DefaultAddressedEnvelope<>(cmd, m_UDPSocketAddress));

		if (echoFuture != null) {
			if (!echoFuture.awaitUninterruptibly(2000)) {
				Logger.getGlobal().info("Error waiting on command echo");
				return false;
			}

		} else {


			writeFuture.addListener((ChannelFutureListener) future -> {
				// Wait briefly for command to echo
				try {
					Thread.sleep(m_InterCommandDelay);
				} catch (InterruptedException e) {
				}
			});

		}

		return true;
	}

	public boolean SendRegisters(RegisterSet regs) {
		if (m_UDPChannel == null || !m_UDPChannel.isOpen()) {
			Logger.getGlobal().warning("UDP Channel closed when trying to SendCommand");
			return false;
		}

		ChannelFuture echoFuture = null;

		/*
		if (m_EchoHandlerContext != null) {
			echoFuture = Channels.future(m_UDPChannel);

			m_GlobalContext.setObject(echoFuture);

			if (((EFADC_ChannelContext)m_EchoHandlerContext.getAttachment()).getObject() != echoFuture) {
				Logger.getGlobal().warning("Echo future not attached to global context!");
			} //else
			//logger.info("Echo future attached successfully at " + m_GlobalContext.getLastUpdated());


			//echoFuture.addListener(new ChannelFutureListener() {
			//	public void operationComplete(ChannelFuture future) {
			//		logger.info("Command Echo");
			//	}
			//});

		}
		 */

		ChannelFuture writeFuture;

		//Write command object to the pipeline

		/*
		if (m_TCPChannel != null && m_TCPChannel.isActive()) {
			writeFuture = m_TCPChannel.write(cmd);
		} else {
			// Handle case for connectionless datagram channel
			writeFuture = m_UDPChannel.isActive() ? m_UDPChannel.writeAndFlush(cmd) : m_UDPChannel.write(cmd);
		}
		*/
		writeFuture = m_UDPChannel.writeAndFlush(new DefaultAddressedEnvelope<>(regs.encode(), m_UDPSocketAddress));

		if (echoFuture != null) {
			if (!echoFuture.awaitUninterruptibly(2000)) {
				Logger.getGlobal().info("Error waiting on register echo");
				return false;
			}

		} else {


			writeFuture.addListener((ChannelFutureListener) future -> {
				// Wait briefly for command to echo
				try {
					Thread.sleep(m_InterCommandDelay);
				} catch (InterruptedException e) {
				}
			});

		}

		return true;
	}

	public boolean GetDeviceInfo() {
		return SendCommand(Command.GetDeviceInfo());
	}
}
