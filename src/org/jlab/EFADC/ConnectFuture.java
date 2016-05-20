package org.jlab.EFADC;

import org.jlab.EFADC.handler.BasicClientHandler;

import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.jlab.EFADC.ConnectFuture.ConnectState.*;

/*
 * Created by john on 5/20/16
 */
public class ConnectFuture extends BasicClientHandler implements Future<Client> {

	public static boolean DEBUG = false;

	enum ConnectState {
		DISCONNECTED("Disconnected"),
		CONNECTING("Connecting"),
		CONNECTED("Connected");

		private String desc;

		ConnectState(String desc) {
			this.desc = desc;
		}

		public String toString() {
			return desc;
		}
	}

	private enum State {WAITING, DONE, CANCELLED}

	private volatile ConnectState m_ConnectState;
	private volatile State state = State.WAITING;
	private final BlockingQueue<Client> reply = new ArrayBlockingQueue<>(1);

	//RegisterSet lastSet = null;

	private EFADC_Client m_Client;

	public ConnectFuture(EFADC_Client client) {
		m_Client = client;
		m_ConnectState = DISCONNECTED;
	}


	void setConnectState(ConnectState state) {
		m_ConnectState = state;
	}


	@Override
	public void registersReceived(RegisterSet registers) {

		Logger.getLogger("global").info(String.format("in ConnectFuture:registersReceived(), state: %s", m_ConnectState));

		super.registersReceived(registers);

		if (m_ConnectState == CONNECTING) {
			m_ConnectState = CONNECTED;

			if (DEBUG) Logger.getLogger("global").info("ConnectHandler.registersReceived(), state CONNECTED");

			// This is required to detect if we connected to a CMP or standalone EFADC
			m_Client.setRegisterSet(registers);

			// Remove the temporary ClientHandler
			//m_Client.getPipeline().remove("handler");

			// Install user supplied handler
			//m_Client.setHandler(m_Handler);

			if (DEBUG && reply.size() == 1) {
				Logger.getLogger("global").severe("Blocking reply already full");
			}

			try {
				if (DEBUG) Logger.getLogger("global").info("About to put, queue size = " + reply.size());
				reply.put(m_Client);
				state = State.DONE;
				if (DEBUG) Logger.getLogger("global").info("PUT " + System.identityHashCode(reply));
			} catch (InterruptedException e) {
				state = State.CANCELLED;
				Logger.getLogger("global").severe("Unable to fill blocking reply");
				e.printStackTrace();
			}

			// This is redundant, remove eventually
			//m_Handler.connected(m_Client);

			//Logger.getLogger("global").info("Connected to EFADC/CMP");
		} else
			Logger.getLogger("global").info("Registers received but not in Connecting state...");
	}

	@Override
	public Client get() throws InterruptedException, ExecutionException {

		if (DEBUG) Logger.getLogger("global").info("ConnectFuture.get()");

		return this.reply.take();
	}

	@Override
	public Client get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

		if (DEBUG) {
			Logger.getLogger("global").info(String.format("ConnectFuture.get(%d)  state: %s  reply: %d", timeout, m_ConnectState, reply.size()));
			Logger.getLogger("global").info("GET " + System.identityHashCode(reply));
		}

		final Client replyOrNull = reply.poll(timeout, unit);
		if (replyOrNull == null) {
			throw new TimeoutException();
		}
		return replyOrNull;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {

		if (DEBUG) Logger.getLogger("global").info("ConnectFuture.cancel()");

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
