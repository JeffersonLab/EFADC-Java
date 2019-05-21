package org.jlab.EFADC;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DatagramPacketDecoder;
import org.jlab.EFADC.handler.ClientHandler;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/15/13
 */
public class Connector {

	private NetworkClient m_Client;
	private EFADC_Client m_Device;

	private ConnectState m_ConnectState;
	private ConnectFuture m_ConnectFuture;

	private int port;

	enum ConnectState {
		DISCONNECTED,
		CONNECTING,
		CONNECTED
	}

	private enum State {WAITING, DONE, CANCELLED}


	public Connector(String address, int port) {
		m_ConnectState = ConnectState.DISCONNECTED;

		Logger.getGlobal().log(Level.FINE,"new Connector, state DISCONNECTED");

		try {
			m_Client = new NetworkClient(/*address, port, true*/);

			m_Client.setAddress(address, port);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	class ConnectFuture implements ClientHandler, Future<EFADC_Client> {

		private volatile State state = State.WAITING;
		private final BlockingQueue<EFADC_Client> reply = new ArrayBlockingQueue<>(1);
		RegisterSet lastSet = null;

		public ConnectFuture() {

		}

		RegisterSet getLastRegisterSet() {
			return lastSet;
		}

		/**
		 * Step 1 in the connection process, device info is received and device specific registers are requested
		 * @param info DeviceInfo received from the target device
		 */
		@Override
		public void deviceInfoReceived(DeviceInfo info) {
			Logger.getGlobal().entering("ConnectFuture", "deviceInfoReceived");

			if (m_ConnectState == ConnectState.CONNECTING) {

				// Create an m_Device object and request device specific registers
				// This will complete the Connect process

				if (info.m_Version >= ETS_RegisterSet.MIN_VERSION) {
					// m_Device was already instantiated as an EFADC_Client, just init the subclass here
					m_Device = new ETS_Client(m_Client);

					// Install ETS frame decoder in place of default EFADC decoder
					m_Client.setDecoder(new ETS_FrameDecoder((ETS_Client)m_Device));

					// Link the specific hw device to the socket thru our global context
					m_Client.getGlobalContext().setDeviceClient(m_Device);

					Logger.getGlobal().log(Level.FINER, "    > Installed ETS Client/FrameDecoder");
				} else
					Logger.getGlobal().log(Level.FINER, "    > Retained EFADC Client/FrameDecoder");

				m_Device.ReadRegisters();
			}

			Logger.getGlobal().exiting("ConnectFuture", "deviceInfoReceived");
		}


		@Override
		public void bufferReceived(ByteBuf buffer) {

		}

		@Override
		public void eventReceived(EFADC_DataEvent event) {

		}

		@Override
		public void eventSetReceived(EventSet set) {

		}

		/**
		 * Step 2 in the connection process, device specific registers are received,
		 * sync command is sent
		 * @param registers
		 */
		@Override
		public void registersReceived(RegisterSet registers) {

			Logger.getGlobal().entering("ConnectFuture", "registersReceived");

			//super.registersReceived(registers);

			lastSet = registers;

			// At this point, an m_Device should already exist

			if (m_ConnectState == ConnectState.CONNECTING) {
				m_ConnectState = ConnectState.CONNECTED;

				m_Device.SendSync();

				// Wait required 200 uS
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {}

				m_Device.setConnected(true);

				Logger.getGlobal().log(Level.FINE, "ConnectHandler.registersReceived(), state -> CONNECTED");
				Logger.getGlobal().log(Level.FINER, registers::toString);

				// This is required to detect if we connected to a CMP/ETS or standalone EFADC
				// TODO: I think we already know at this point... But this shouldn't hurt any

				try {
					reply.put(m_Device);
					state = State.DONE;
				} catch (InterruptedException e) {
					state = State.CANCELLED;
				}

				//Logger.getGlobal().info("Connected to EFADC/CMP");
			}

			Logger.getGlobal().exiting("ConnectFuture", "registersReceived");
		}



		@Override
		public EFADC_Client get() throws InterruptedException, ExecutionException {

			Logger.getGlobal().log(Level.FINE, "ConnectHandler.get()");

			return this.reply.take();
		}

		@Override
		public EFADC_Client get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

			Logger.getGlobal().log(Level.FINE, String.format("ConnectHandler.get(%d)", timeout));

			final EFADC_Client replyOrNull = reply.poll(timeout, unit);
			if (replyOrNull == null) {
				throw new TimeoutException();
			}
			return replyOrNull;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {

			Logger.getGlobal().log(Level.FINE, "ConnectHandler.cancel()");

			//try {
				state = State.CANCELLED;
				//cleanUp();
				return true;
			//} catch (JMSException e) {
			//	throw Throwables.propagate(e);
			//}
		}

		@Override
		public boolean isCancelled() {
			return state == State.CANCELLED;
		}

		@Override
		public boolean isDone() {
			return state == State.DONE;
		}

	}

	/**
	 * Performs asynchronous connection to an EFADC/CMP.
	 * @return Future representing the connection status, or null if other failure **Specify**
	 */
	public Future<EFADC_Client> connect(boolean debug) {

		Logger.getGlobal().info("Connecting...");

		m_Client.setDebug(debug);

		// Set up temporary ClientHandler to detect connection progress
		m_ConnectFuture = new ConnectFuture();

		try {
			Logger.getGlobal().info(" => connect()");

			// This just opens the datagram channel, no communication is initiated until after we install the handler and read registers
			m_Client.connect();

			Logger.getGlobal().log(Level.FINE, " => setHandler()");
			m_Client.setClientListener(m_ConnectFuture);
		} catch (EFADC_AlreadyConnectedException e) {
			Logger.getGlobal().severe("Already connected?");
			return null;
		}

		m_ConnectState = ConnectState.CONNECTING;
		Logger.getGlobal().log(Level.FINE," => GetDeviceInfo() :: state -> CONNECTING");

		if (!m_Client.GetDeviceInfo()) {
			Logger.getGlobal().severe("GetDeviceInfo failed in connect()");

			return null;
		}

		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}


		/*
		// We're asynchronous so we could possibly get here after the connect sequence when the timer should be stopped/null
		if (m_ConnectState != ConnectState.CONNECTED && m_ConnectTimeout != null)
			m_ConnectTimeout.start();
		*/

		return m_ConnectFuture;
	}

}
