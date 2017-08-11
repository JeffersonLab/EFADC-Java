package org.jlab.EFADC;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelConfig;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.jlab.EFADC.command.Command;
import org.jlab.EFADC.handler.ClientHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.jlab.EFADC.Tuple2.o;

/**
 * Created by john on 6/13/17.
 */
public class NetworkClient {

	public static boolean flag_Verbose = false;

	ChannelHandlerContext		m_EchoHandlerContext = null;
	int							m_InterCommandDelay;
	ChannelPipelineFactory		m_TCPClientPipelineFactory, m_UDPClientPipelineFactory;
	int 						m_TCPPort, m_UDPPort;
	InetSocketAddress			m_TCPSocketAddress, m_UDPSocketAddress;
	ClientSocketChannelFactory	m_TCPClientChannelFactory;
	DatagramChannelFactory		m_UDPClientChannelFactory;
	Channel 					m_UDPChannel = null, m_TCPChannel = null;
	EFADC_ChannelContext		m_GlobalContext;
	Timer						m_WheelTimer = null;
	ChannelHandler 				m_IdleStateHandler = null;
	private ReadTimeoutHandler 	m_AcquisitionReadTimeoutHandler;

	public NetworkClient() {
		m_UDPClientChannelFactory = new OioDatagramChannelFactory(Executors.newCachedThreadPool());

		m_WheelTimer = new HashedWheelTimer();

		m_GlobalContext = new EFADC_ChannelContext();
		m_GlobalContext.setClient(this);

		m_UDPClientPipelineFactory = new EFADC_ClientPipelineFactory(m_GlobalContext);
	}

	private Tuple2<Bootstrap, Channel> InitConnectionlessChannel(ChannelFactory f, ChannelPipelineFactory pf, int port) {
		ConnectionlessBootstrap b = new ConnectionlessBootstrap(f);

		b.setPipelineFactory(pf);

		SetSocketOptions(b);

		Channel c = b.bind(new InetSocketAddress(port));
		//Channel c = b.bind(m_UDPSocketAddress);

		return o((Bootstrap)b, c);
	}


	private Tuple2<Bootstrap, Channel> InitConnectedChannel(ChannelFactory f, ChannelPipelineFactory pf, InetSocketAddress sa) {
		ClientBootstrap b = new ClientBootstrap(f);

		SetSocketOptions(b);

		b.setPipelineFactory(pf);

		ChannelFuture future = b.connect(sa);
		future.awaitUninterruptibly();

		assert future.isDone();

		Channel c = null;

		if (future.isCancelled()) {
			// Connection attempt cancelled by user
		} else if (!future.isSuccess()) {
			future.getCause().printStackTrace();
		} else {
			// Connection established successfully
			c = future.getChannel();
		}

		return o((Bootstrap)b, c);
	}

	public void initIdleHandler() {
		m_IdleStateHandler = new IdleStateHandler(m_WheelTimer, 60, 30, 0);

		// Don't set attachment for IdleStateHandler or it will throw ClassCastException on Windows
		// So we initialize it down here

		try {
			m_UDPClientPipelineFactory.getPipeline().addFirst("idlestate", m_IdleStateHandler);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initReadTimeoutHandler() {
		// Insert read timeout handler into the pipeline
		m_AcquisitionReadTimeoutHandler = new ReadTimeoutHandler(m_WheelTimer, 1);

		try {
			m_UDPClientPipelineFactory.getPipeline().addFirst("acqTimeout", m_AcquisitionReadTimeoutHandler);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeTimeoutHandler() {
		try {
			// Remove read timeout handler
			m_UDPClientPipelineFactory.getPipeline().remove("acqTimeout");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void SetSocketOptions(Bootstrap b) {
		//Enable broadcast
		b.setOption("broadcast", "false");
		b.setOption("tcpNoDelay", true);


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
		b.setOption("receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor(2000));

		//This needs to be set higher to avoid the "catching up" behavior where packets are missed near the beginning of an acquisition
		b.setOption("receiveBufferSize", 1048576);
		//b.setOption("receiveBufferSizePredictorFactory", new AdaptiveReceiveBufferSizePredictorFactory());
	}


	private void InitTCPControlSocket() {
		m_TCPClientChannelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

		m_TCPClientPipelineFactory = new EFADC_ClientPipelineFactory(m_GlobalContext);

		// TODO add timeout handler for tcp socket

		Tuple2<Bootstrap, Channel> t = InitConnectedChannel(m_TCPClientChannelFactory, m_TCPClientPipelineFactory, m_TCPSocketAddress);

		Bootstrap b = t._1;
		m_TCPChannel = t._2;
	}


	public boolean isConnected() {
		return (m_UDPChannel != null && m_UDPChannel.isOpen());
	}



	public void connect() throws EFADC_AlreadyConnectedException {
		if (m_UDPChannel != null /* && m_UDPChannel.isConnected()*/)
			throw new EFADC_AlreadyConnectedException();

		Tuple2<Bootstrap, Channel> t = InitConnectionlessChannel(m_UDPClientChannelFactory, m_UDPClientPipelineFactory, m_UDPPort);

		Bootstrap b = t._1;
		m_UDPChannel = t._2;

		Logger.getLogger("global").info("m_UDPChannel connected: " + m_UDPChannel.isConnected() + "  open: " + m_UDPChannel.isOpen());

		//b.setOption("connectTimeoutMillis", 10000);

		DatagramChannelConfig config = ((DatagramChannel)m_UDPChannel).getConfig();
	}


	/**
	 * Close sockets and release factory resources
	 */
	public void cleanup() {
		if (m_WheelTimer != null)
			m_WheelTimer.stop();

		if (m_UDPChannel != null) {

			ChannelFuture cf = m_UDPChannel.close();
			cf.awaitUninterruptibly();

			if (m_UDPClientChannelFactory != null)
				m_UDPClientChannelFactory.releaseExternalResources();
		}

		if (m_TCPChannel != null) {

			if (m_TCPChannel.isConnected()) {
				ChannelFuture cf = m_TCPChannel.close();
				cf.awaitUninterruptibly();
			}

			if (m_TCPClientChannelFactory != null)
				m_TCPClientChannelFactory.releaseExternalResources();
		}

	}


	/**
	 * Enable or disable the echo response handler.  Disable is only required if it had been previously enabled.
	 * @param enable
	 */
	@Deprecated
	public void enableEchoResponseHandler(boolean enable) {
		if (enable && m_EchoHandlerContext == null) {
			ChannelPipeline p = m_UDPChannel.getPipeline();

			p.addBefore("encoder", "echo", new EFADC_EchoHandler());

			// Technically we don't need to keep a reference to this object anymore since we're using the global context
			m_EchoHandlerContext = p.getContext("echo");
			m_EchoHandlerContext.setAttachment(m_GlobalContext);

			Logger.getLogger("global").info("Echo handler initialized");

		} else {
			try {
				m_UDPChannel.getPipeline().remove("echo");
			} catch (NoSuchElementException e) {
			}

			Logger.getLogger("global").info("Echo handler disabled");
		}
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

		Logger.getLogger("global").info("Debug mode " + (flag_Verbose ? "enabled" : "disabled"));
	}


	public void setInterCommandDelay(int delay) {
		m_InterCommandDelay = delay;
	}


	/**
	 * Set ClientHandler delegate
	 * @param handler Delegate object
	 */
	public void setHandler(ClientHandler handler) {
		ChannelPipeline p = getPipeline();

		// Remove any existing handler
		try {
			p.remove("handler");
		} catch (NoSuchElementException e) {
			Logger.getLogger("global").info("setHandler :: No existing handler to remove");
		}

		p.addLast("handler", handler);
		p.getContext("handler").setAttachment(m_GlobalContext);
	}


	/**
	 * Replace existing frame decoder
	 * @param decoder New frame decoder
	 */
	public void setDecoder(FrameDecoder decoder) {
		ChannelPipeline p = getPipeline();

		// Remove any existing decoder
		try {
			p.remove("decoder");
		} catch (NoSuchElementException e) {
			Logger.getLogger("global").info("setDecoder :: No existing decoder to remove");
		}

		p.addBefore("handler", "decoder", decoder);
		//p.getContext("handler").setAttachment(m_GlobalContext);
	}



	public ClientHandler getHandler() {
		return (ClientHandler) getPipeline().get("handler");
	}


	public ChannelPipeline getPipeline() {
		return m_UDPChannel.getPipeline();
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
			pipeline.addBefore("encoder", "writer", writer);
		}
	}


	/**
	 * TODO: Return echo future or write future depending on the mode the channel is operating in
	 * @param cmd Command to send to the device
	 * @return Result of command acknowledgement
	 */
	public boolean SendCommand(Object cmd) {
		if (m_UDPChannel == null || !m_UDPChannel.isOpen())
			return false;

		ChannelFuture echoFuture = null;

		if (m_EchoHandlerContext != null) {
			echoFuture = Channels.future(m_UDPChannel);

			m_GlobalContext.setObject(echoFuture);

			if (((EFADC_ChannelContext)m_EchoHandlerContext.getAttachment()).getObject() != echoFuture) {
				Logger.getLogger("global").warning("Echo future not attached to global context!");
			} //else
			//logger.info("Echo future attached successfully at " + m_GlobalContext.getLastUpdated());

			/*
			echoFuture.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) {
					logger.info("Command Echo");
				}
			});
			*/
		}

		ChannelFuture writeFuture;

		//Write command object to the pipeline

		if (m_TCPChannel != null && m_TCPChannel.isConnected()) {
			writeFuture = m_TCPChannel.write(cmd);
		} else {
			// Handle case for connectionless datagram channel
			writeFuture = m_UDPChannel.isConnected() ? m_UDPChannel.write(cmd) : m_UDPChannel.write(cmd, m_UDPSocketAddress);
		}

		if (echoFuture != null) {
			if (!echoFuture.awaitUninterruptibly(2000)) {
				Logger.getLogger("global").info("Error waiting on command echo");
				return false;
			}

		} else {

			writeFuture.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) {
					// Wait briefly for command to echo
					try {
						Thread.sleep(m_InterCommandDelay);
					} catch (InterruptedException e) {
					}

				}
			});
		}

		return true;
	}

	public boolean GetDeviceInfo() {
		return SendCommand(Command.GetDeviceInfo());
	}
}
