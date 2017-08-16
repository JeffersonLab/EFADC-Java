package org.jlab.EFADC;

import org.jboss.netty.channel.ChannelPipeline;
import org.jlab.EFADC.handler.BasicClientHandler;
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
	private ClientHandler m_Handler, m_TempHandler;

	private ConnectState m_ConnectState;
	private ConnectFuture m_ConnectFuture;

	private int port;

	enum ConnectState {
		DISCONNECTED,
		CONNECTING,
		CONNECTED
	}

	private enum State {WAITING, DONE, CANCELLED}


	public Connector(String address, int port/*, ClientHandler handler*/) {
		//m_Handler = handler;
		m_ConnectState = ConnectState.DISCONNECTED;

		Logger.getLogger("global").info("new Connector, state DISCONNECTED");

		try {
			m_Client = new NetworkClient(/*address, port, true*/);

			m_Client.setAddress(address, port);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

	}


	class ConnectFuture extends BasicClientHandler implements Future<EFADC_Client> {

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
			Logger.getLogger("global").info("in ConnectFuture deviceInfoReceived()");

			if (m_ConnectState == ConnectState.CONNECTING) {

				// Create an m_Device object and request device specific registers
				// This will complete the Connect process

				if (info.m_Version == ETS_RegisterSet.MIN_VERSION) {
					// m_Device was already instantiated as an EFADC_Client, just init the subclass here
					m_Device = new ETS_Client(m_Client);

					// Install ETS frame decoder in place of default EFADC decoder
					m_Client.setDecoder(new ETS_FrameDecoder((ETS_Client)m_Device));

					Logger.getGlobal().log(Level.FINE, "    > Installed ETS Client/FrameDecoder");
				} else
					Logger.getGlobal().log(Level.FINE, "    > Retained EFADC Client/FrameDecoder");

				m_Device.ReadRegisters();
			}
		}

		/**
		 * Step 2 in the connection process, device specific registers are received,
		 * @param registers
		 */
		@Override
		public void registersReceived(RegisterSet registers) {

			Logger.getGlobal().log(Level.FINE, "in ConnectFuture registersReceived()");

			super.registersReceived(registers);

			lastSet = registers;

			// At this point, an m_Device should already exist

			if (m_ConnectState == ConnectState.CONNECTING) {
				m_ConnectState = ConnectState.CONNECTED;

				Logger.getGlobal().info("ConnectHandler.registersReceived(), state -> CONNECTED");
				Logger.getGlobal().info(registers.toString());

				// This is required to detect if we connected to a CMP/ETS or standalone EFADC
				// TODO: I think we already know at this point... But this shouldn't hurt any
				//m_Device.setRegisterSet(registers);

				try {
					reply.put(m_Device);
					state = State.DONE;
				} catch (InterruptedException e) {
					state = State.CANCELLED;
				}

				//Logger.getLogger("global").info("Connected to EFADC/CMP");
			}
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
			m_Client.setHandler(m_ConnectFuture);
		} catch (EFADC_AlreadyConnectedException e) {
			Logger.getGlobal().severe("Already connected?");
			return null;
		}

		m_ConnectState = ConnectState.CONNECTING;
		Logger.getGlobal().info(" => GetDeviceInfo() :: state -> CONNECTING");

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

	/*
	private void finishConnect() {

		m_ConnectState = ConnectState.CONNECTED;

		Logger.getLogger("global").info("in finishConnect(), state CONNECTED");

		//m_ConnectTimeout.stop();
		//m_ConnectTimeout.removeActionListener(m_TimerAL);
		//m_ConnectTimeout = null;


		// Remove the temporary ClientHandler
		m_Client.getPipeline().remove("handler");

		m_Client.setHandler(m_Handler);

		m_ConnectFuture.setDone(m_Client);

		// This is redundant, remove eventually
		m_Handler.connected(m_Client);

		Logger.getLogger("global").info("Connected to EFADC/CMP");
	}
	*/
}
