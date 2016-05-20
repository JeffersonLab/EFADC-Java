//
//  EFADC_Client.java
//  EFADC_java
//
//  Created by John McKisson on 10/21/11.
//  Copyright (c) 2011 Jefferson Lab. All rights reserved.
//
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
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.jlab.EFADC.command.Command;
import org.jlab.EFADC.handler.ClientHandler;
import org.jlab.EFADC.matrix.CoincidenceMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.jlab.EFADC.Tuple2.o;
import static org.jlab.EFADC.RegisterSet.*;

public class EFADC_Client implements Client {

	public static boolean flag_Verbose = false;

	ChannelHandlerContext		m_EchoHandlerContext = null;
	EFADC_ChannelContext		m_GlobalContext;
	int							m_InterCommandDelay;
	RegisterSet					m_Registers = null;
	ChannelPipelineFactory		m_TCPClientPipelineFactory, m_UDPClientPipelineFactory;
	int 						m_TCPPort, m_UDPPort;
	InetSocketAddress			m_TCPSocketAddress, m_UDPSocketAddress;
	ClientSocketChannelFactory	m_TCPClientChannelFactory;
	DatagramChannelFactory		m_UDPClientChannelFactory;
	Channel 					m_UDPChannel = null, m_TCPChannel = null;
	
	Timer						m_WheelTimer = null;

	boolean						m_AggregatorEnable = false;
	boolean						m_AcquisitionActive = false;

	private ReadTimeoutHandler 	m_AcquisitionReadTimeoutHandler;
	private	ChannelHandler 		m_IdleStateHandler = null;
	
	
	public EFADC_Client(String address, int port, boolean enableIdleTimer) throws Exception {

		m_UDPClientChannelFactory = new OioDatagramChannelFactory(Executors.newCachedThreadPool());

		setAddress(address, port);

		m_WheelTimer = new HashedWheelTimer();
		
		m_GlobalContext = new EFADC_ChannelContext();
		m_GlobalContext.setClient(this);
		
		m_UDPClientPipelineFactory = new EFADC_ClientPipelineFactory(m_GlobalContext);

		if (enableIdleTimer) {
			m_IdleStateHandler = new IdleStateHandler(m_WheelTimer, 60, 30, 0);

			// Don't set attachment for IdleStateHandler or it will throw ClassCastException on Windows
			// So we initialize it down here

			m_UDPClientPipelineFactory.getPipeline().addFirst("idlestate", m_IdleStateHandler);

		} else
			Logger.getLogger("global").info("Idle handler disabled");

		// TODO: Use factory or whatever to get proper register set version
		//m_Registers = new EFADC_RegisterSet();

		setInterCommandDelay(50);

		// TODO add accessors to modify tree size and handler
		//m_EventAggregator = new EFADC_EventAggregator(10);
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


	void flushAggregateBuffer() {
		//m_EventAggregator.flush();
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


	public void setAggregatorEnable(boolean val) {
		m_AggregatorEnable = val;
	}

	public boolean getCollectState() {
		return m_AcquisitionActive;
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

		/*
		Map<String,ChannelHandler> map = p.toMap();

		Logger.getLogger("global").info("::Handler Map Before Add::");
		for (Object o : map.values()) {
			Logger.getLogger("global").info(""+o);
		}
		*/

		// Remove any existing handler
		try {
			p.remove("handler");
		} catch (NoSuchElementException e) {
			Logger.getLogger("global").info("setHandler :: No existing handler to remove");
		}

		p.addLast("handler", handler);
		p.getContext("handler").setAttachment(m_GlobalContext);

		/*
		map = p.toMap();

		Logger.getLogger("global").info("::Handler Map After Add::");
		for (String name : map.keySet()) {
			Logger.getLogger("global").info(name + " :: " + map.get(name));
		}
		*/

	}



	public ClientHandler getHandler() {
		return (ClientHandler) getPipeline().get("handler");
	}


	public ChannelPipeline getPipeline() {
		return m_UDPChannel.getPipeline();
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



	public void setRegisterSet(RegisterSet regs) {

		if (m_Registers == null || (m_Registers instanceof EFADC_RegisterSet && regs instanceof CMP_RegisterSet)) {
			// Replace if we detected CMP
			m_Registers = regs;

		} else {
			// Don't replace entirely because we'll lose information
			m_Registers.update(regs);
		}
	}

	/**
	 * Get last received register set
	 * @return RegisterSet
	 */
	public RegisterSet getRegisterSet() {
		return m_Registers;
	}


	/**
	 * Set ADC to accept incoming positive pulses.  User should make sure DAC values are set around 500.
	 * Note: This currently sends these commands to ALL EFADCs in the DAQ network
	 */
	public void SetADCPositive() {
		//Set bit 15 of register2 to 1 to address all ADC's
		m_Registers.setRegister(REG_2, m_Registers.getRegister(REG_2) | (1 << 15));

		//Write 0x1404 to all ADCs	(this sets 2's compliment)
		m_Registers.setRegister(REG_20, 0x1401);

		SendSetRegisters(1);
		//SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Write 0xFF01 to all ADCs
		m_Registers.setRegister(REG_20, 0xFF01);

		SendSetRegisters(1);
		//SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Set ADC to accept incoming negative pulses.  User should make sure DAC values are set around 3300.
	 * Note: This currently sends these commands to ALL EFADCs in the DAQ network
	 */
	public void SetADCNegative() {
		//Set bit 15 of register2 to 1 to address all ADC's
		m_Registers.setRegister(REG_2, m_Registers.getRegister(REG_2) | (1 << 15));

		//Write 0x1400 to all ADCs
		m_Registers.setRegister(REG_20, 0x1400);

		SendSetRegisters(1);
		//SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Write 0xFF01 to all ADCs (this does a SW transfer)
		m_Registers.setRegister(REG_20, 0xFF01);

		SendSetRegisters(1);
		//SendSetRegisters(2);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public void SetCoincidenceWindowWidth(int width) {
		EFADC_Registers adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = (EFADC_Registers)cmpReg.getADCRegisters(i);

					adcReg.setCoincidenceWindowWidth(width);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_Registers)m_Registers;

			adcReg.setCoincidenceWindowWidth(width);
		}
	}


	@Deprecated
	public void SetANDCoincident(int detA, int detB, boolean val, boolean reverse) {
		m_Registers.SetCoincident(detA, detB, val, reverse, RegisterSet.MatrixType.AND);
	}

	@Deprecated
	public void SetORCoincident(int detA, int detB, boolean val, boolean reverse) {
		CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

		CoincidenceMatrix matrix = (CoincidenceMatrix)cmpReg.getMatrix(RegisterSet.MatrixType.OR);

		matrix.setCoincident(detA, detB, val, false);

	}


	public void SetCoincident(int detA, int detB, boolean val, boolean reverse, RegisterSet.MatrixType type) {

		//CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

		//CoincidenceMatrix matrix = (CoincidenceMatrix)cmpReg.getMatrix(type);

		m_Registers.SetCoincident(detA, detB, val, reverse, type);

		/*
		Logger.getLogger("global").info(String.format("SetCoincident(%d, %d, %s, %s, Opp: %s)",
				detA, detB,
				val ? "ON" : "OFF",
				type == CMP_RegisterSet.MatrixType.AND ? "AND" : "OR",
				reverse ? "YES" : "NO"));
		*/
	}


	// Set each module in coincidence with itself
	public void SetIdentityMatrix() {
		EFADC_RegisterSet adcReg;
		int numDet = 4;

		if (IsCMP()) {
			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			numDet *= cmpReg.getADCCount();
		}

		for (int i = 0; i < numDet; i++) {
			for (int j = 0; j < numDet; j++) {
				SetCoincident(i, j, i == j, false, RegisterSet.MatrixType.AND);
			}
		}

	}

	// Set coincidence matrix to all zero
	public void SetZeroMatrix() {
		EFADC_RegisterSet adcReg;
		int numDet = 4;

		if (IsCMP()) {
			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			numDet *= cmpReg.getADCCount();
		}

		for (int i = 0; i < numDet; i++) {
			for (int j = 0; j < numDet; j++) {
				SetCoincident(i, j, false, false, RegisterSet.MatrixType.AND);
				SetCoincident(i, j, false, false, RegisterSet.MatrixType.OR);
			}
		}
	}


	/**
	 * Set Integration Window for all EFADCs
	 * @param window
	 */
	public void SetIntegrationWindow(int window) {
		EFADC_Registers adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = (EFADC_Registers)cmpReg.getADCRegisters(i);

					adcReg.setIntegrationWindow(window);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_Registers)m_Registers;

			adcReg.setIntegrationWindow(window);
		}
	}


	/**
	 * Set Mode for all EFADCs
	 * @param mode
	 */
	public void SetMode(int mode) {
		EFADC_Registers adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = (EFADC_Registers)cmpReg.getADCRegisters(i);

					adcReg.setMode(mode);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_Registers)m_Registers;

			adcReg.setMode(mode);
		}
	}


	/**
	 * Set NSB for all EFADCs
	 * @param window
	 */
	public void SetNSB(int window) {
		EFADC_Registers adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = (EFADC_Registers)cmpReg.getADCRegisters(i);

					adcReg.setNSB(window);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_Registers)m_Registers;

			adcReg.setNSB(window);
		}
	}


	/**
	 * Set Integration Window for specific EFADC
	 * @param adc Range 1 to # ADC's
	 * @param window
	 */
	public void SetIntegrationWindow(int adc, int window) {
		EFADC_Registers adcReg = null;

		if (IsCMP()) {
			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			if (adc == 0) {
				// Special case to select all ADC's

			} else {

				try {
					adcReg = (EFADC_Registers)cmpReg.getADCRegisters(adc);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + adc);
				}
			}

		} else {
			adcReg = (EFADC_Registers)m_Registers;
		}

		if (adcReg == null) {
			Logger.getLogger("global").warning("NULL Register Set in SetIntegrationWindow");
			return;
		}

		adcReg.setIntegrationWindow(window);
	}


	/**
	 * Enables self triggering for all EFADC register sets
	 * @param enable
	 * @param value
	 */
	public void SetSelfTrigger(boolean enable, int value) {
		EFADC_Registers adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = (EFADC_Registers)cmpReg.getADCRegisters(i);

					adcReg.setSelfTrigger(enable, value);

					Logger.getLogger("global").info(String.format("Setting self trigger ADC %d: %s", i, enable ? "true" : "false"));

				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_Registers)m_Registers;

			adcReg.setSelfTrigger(enable, value);
		}
	}


	/**
	 * Set Sync for all EFADCs
	 * @param val
	 */
	public void SetSync(boolean val) {
		EFADC_RegisterSet adcReg;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {
				try {
					adcReg = cmpReg.getADCRegisters(i);

					adcReg.setSync(val);
				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + i);
				}
			}

		} else {
			adcReg = (EFADC_RegisterSet)m_Registers;

			adcReg.setSync(val);
		}
	}


	public void SetThreshold(int det, int thresh) {

		EFADC_Registers adcReg = null;
		int adcDet = det;

		if (IsCMP()) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			int adc = (int)(det / 4.0) + 1;

			// Address proper module in each efadc
			adcDet = det - (adc - 1) * 4;

			try {
				adcReg = (EFADC_Registers)cmpReg.getADCRegisters(adc);

			} catch (EFADC_InvalidADCException e) {
				Logger.getLogger("global").warning("Invalid ADC Selection: " + adc);
			}

			Logger.getLogger("global").info(String.format("Setting CMP Threshold, chan %d, adc %d, det %d, value %d", det, adc, adcDet, thresh));

		} else {
			Logger.getLogger("global").info("Setting EFADC Threshold");
			adcReg = (EFADC_Registers)m_Registers;
		}

		if (adcReg == null) {
			Logger.getLogger("global").warning("NULL Register Set in SetThreshold");
			return;
		}

		adcReg.setThreshold(adcDet, thresh);
	}


	@Deprecated
	public void SetCoincidenceTableEntry(int type) {
	
		if (type == 0) {
		
			//This will set each PMT in coincidence with itself
			m_Registers.setRegister(9, 0x2010);
			m_Registers.setRegister(10, 0x8040);
			
		} else if (type == 1) {
		
			//This configures a 1 to 3 coincidence mode
			m_Registers.setRegister(9, 0x010E);
			m_Registers.setRegister(10, 0x0408);
			
		} else if (type == 2) {
		
			//This configures a 2 exclusive pair coincidence mode
			m_Registers.setRegister(9, 0x0102);
			m_Registers.setRegister(10, 0x0408);
			
		} else if (type == 3) {
		
			//This configures a 4 detector ring mode
			m_Registers.setRegister(9, 0x0D0E);
			m_Registers.setRegister(10, 0x070B);

		}
	}


	/**
	 * Send register values to the EFADC/CMP
	 * We have to manage the CMP registers in a tricky way since a completely new encoded packet is required to set each EFADC register set
	 * @return
	 */
	public boolean SendSetRegisters(int adc) {

		if (m_Registers instanceof CMP_RegisterSet) {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			//for (int i = 1; i < cmpReg.getADCCount() + 1; i++) {

				try {
					cmpReg.selectADC(adc);

					SendCommand(m_Registers);

					Thread.sleep(50);

				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + adc);
					return false;

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			//}

			return true;

		} else
			//We're either sending same register set to all EFADC's on a CMP, or just EFADC registers to a standalone unit
			return SendCommand(m_Registers);
	}



	public boolean SendLCDData(int line, String text) {
		return SendCommand(EFADC_LCDMessage.encode(line, text));
	}


	/**
	 * Set all DAC values for a specific efadc register set.  Specific EFADC must be selected beforehand if sending to a CMP.
	 * TODO: Get Hai to implement a serial command that will allow me to set all DAC values at once
	 * @param values DAC Values
	 * @param reg EFADC Registers
	 * @return
	 */
	private boolean sendDACValues(int[] values, EFADC_Registers reg, int adc) {
		for (int i = 0; i < 16; i++) {
			reg.setBiasDAC(i, values[i]);
			if (!SendSetRegisters(adc))
				return false;


			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		return true;
	}

	public boolean IsCMP() {
		if (m_Registers != null && m_Registers instanceof CMP_RegisterSet)
			return true;

		return false;
	}

	
	public boolean SetDACValues(int[] values) {

		if (m_Registers instanceof EFADC_RegisterSet) {

			return sendDACValues(values, (EFADC_Registers)m_Registers, 0);

		} else {

			CMP_RegisterSet cmpReg = (CMP_RegisterSet)m_Registers;

			int adcCount = cmpReg.getADCCount();

			// We need to send the ENTIRE register set a total of 16 * adcCount() times because of the way the registers were implemented in firmware...
			if (values.length < adcCount * 16) {
				Logger.getLogger("global").severe(String.format("DAC Values array needs to be %d, currently only %d", adcCount * 16, values.length));
				return false;
			}

			int[] ival = new int[16];	// Individual dac values per efadc

			// Individually address each efadc in the cmp register block
			for (int i = 0; i < adcCount; i++) {

				int selADC = i + 1;	// +1 here because a subtraction occurs internally, and a selected adc value of 0 sends to all efadcs

				try {
					EFADC_Registers adcReg = (EFADC_Registers)cmpReg.getADCRegisters(selADC);

					//cmpReg.selectADC(selADC);

					System.arraycopy(values, 16*i, ival, 0, 16);

					if (!sendDACValues(ival, adcReg, selADC)) {
						Logger.getLogger("global").warning(String.format("Failed setting regs for adc %d/%d", i, adcCount));
						return false;
					}

				} catch (EFADC_InvalidADCException e) {
					Logger.getLogger("global").warning("Invalid ADC Selection: " + selADC);
				}

			}
		}
		
		return true;
	}



	public boolean StartCollection() throws Exception {

		if (m_AggregatorEnable) {
			// Insert read timeout handler into the pipeline
			m_AcquisitionReadTimeoutHandler = new ReadTimeoutHandler(m_WheelTimer, 1);

			m_UDPClientPipelineFactory.getPipeline().addFirst("acqTimeout", m_AcquisitionReadTimeoutHandler);
		}

		m_AcquisitionActive = true;

		return SendCommand(Command.StartCollection());
	}


	public boolean StopCollection() throws Exception {

		try {
			// Remove read timeout handler
			m_UDPClientPipelineFactory.getPipeline().remove("acqTimeout");
		} catch (NoSuchElementException e) {}

		m_AcquisitionActive = false;

		return SendCommand(Command.StopCollection());
	}


	public boolean ReadRegisters() {
		return SendCommand(Command.ReadRegisters());
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

}