package org.jlab.EFADC;

import org.jlab.EFADC.handler.BasicClientHandler;
import org.jlab.EFADC.handler.ClientHandler;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/15/13
 */
public class Connector {

	private EFADC_Client m_Client;
	private ClientHandler m_Handler, m_TempHandler;
	//private Timer m_ConnectTimeout;
	//private ActionListener m_TimerAL;

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
			m_Client = new EFADC_Client(address, port, true);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		/*
		m_TimerAL = new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				// Check if we're already connected, this timer should already have been stopped but just in case...
				if (m_ConnectState == ConnectState.CONNECTED) {
					Logger.getLogger("global").info("Connect timeout but state is connected, ignore");
					return;
				}

				m_ConnectState = ConnectState.DISCONNECTED;
				Logger.getLogger("global").warning("Failed to connect to EFADC/CMP");
			}
		};


		//Start connect timeout timer for 3 seconds
		m_ConnectTimeout = new javax.swing.Timer(3000, m_TimerAL);

		m_ConnectTimeout.setRepeats(false);
		*/
	}



	class ConnectFuture extends BasicClientHandler implements Future<Client> {

		private volatile State state = State.WAITING;
		private final BlockingQueue<Client> reply = new ArrayBlockingQueue<>(1);
		RegisterSet lastSet = null;

		public ConnectFuture() {

		}

		RegisterSet getLastRegisterSet() {
			return lastSet;
		}

		@Override
		public void registersReceived(RegisterSet registers) {

			Logger.getLogger("global").info("in ConnectFuture registersReceived()");

			super.registersReceived(registers);

			lastSet = registers;

			if (m_ConnectState == ConnectState.CONNECTING) {
				m_ConnectState = ConnectState.CONNECTED;

				Logger.getLogger("global").info("ConnectHandler.registersReceived(), state CONNECTED");

				// This is required to detect if we connected to a CMP or standalone EFADC
				m_Client.setRegisterSet(registers);

				// Remove the temporary ClientHandler
				//m_Client.getPipeline().remove("handler");

				// Install user supplied handler
				//m_Client.setHandler(m_Handler);

				try {
					reply.put(m_Client);
					state = State.DONE;
				} catch (InterruptedException e) {
					state = State.CANCELLED;
				}

				// This is redundant, remove eventually
				//m_Handler.connected(m_Client);

				//Logger.getLogger("global").info("Connected to EFADC/CMP");
			}
		}

		@Override
		public Client get() throws InterruptedException, ExecutionException {

			Logger.getLogger("global").info("ConnectHandler.get()");

			return this.reply.take();
		}

		@Override
		public Client get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

			Logger.getLogger("global").info(String.format("ConnectHandler.get(%d)", timeout));

			final Client replyOrNull = reply.poll(timeout, unit);
			if (replyOrNull == null) {
				throw new TimeoutException();
			}
			return replyOrNull;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {

			Logger.getLogger("global").info("ConnectHandler.cancel()");

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
	public Future<Client> connect(boolean debug) {

		Logger.getLogger("global").info("Connecting...");

		/*
		if (m_Client != null) {
			m_Client.cleanup();
			m_Client = null;
		}
		*/

		m_Client.setDebug(debug);

		// Set up temporary ClientHandler to detect connection progress
		m_ConnectFuture = new ConnectFuture();

		try {
			Logger.getLogger("global").info(" => connect()");

			// This just opens the datagram channel, no communication is initiated until after we install the handler and read registers
			m_Client.connect();

			Logger.getLogger("global").info(" => setHandler()");
			m_Client.setHandler(m_ConnectFuture);
		} catch (EFADC_AlreadyConnectedException e) {
			Logger.getLogger("global").severe("Already connected?");
			return null;
		}

		m_ConnectState = ConnectState.CONNECTING;
		Logger.getLogger("global").info(" => ReadRegisters() :: state CONNECTING");

		// Try to read registers to see if we're really connected
		if (!m_Client.ReadRegisters()) {
			Logger.getLogger("global").severe("ReadRegisters failed in connect()");

			return null;
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
