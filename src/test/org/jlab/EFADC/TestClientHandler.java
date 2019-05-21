package org.jlab.EFADC;

import io.netty.buffer.ByteBuf;
import org.jlab.EFADC.handler.ClientHandler;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class TestClientHandler implements ClientHandler {
	LinkedBlockingQueue<EventSet> eventQueue;

	public int nEventSets = 0;
	public int nEvents = 0;
	public int nSingleEvents = 0;

	TestClientHandler() {
		eventQueue = new LinkedBlockingQueue<>();
	}

	public void clearEventQueue() {
		eventQueue.clear();
	}

	public LinkedBlockingQueue<EventSet> getEventQueue() {
		return eventQueue;
	}

/*
	@Override
	public void connected(Client client) {
		//m_Client = client;

		Logger.getGlobal().info("in main ClientHandler, connected()");
	}

 */

	@Override
	public void bufferReceived(ByteBuf buffer) {

	}

		/*
		@Override
		public void registersReceived(RegisterSet regs) {
			super.registersReceived(regs);

			Logger.getGlobal().info("registersReceived");

			// This cast should be avoided, put setRegisterSet in the Client interface?
			// This is called in the superclass implementation
			//m_DeviceClient.setRegisterSet(regs);
		}
		*/

	@Override
	public void eventReceived(EFADC_DataEvent event) {
		//Logger.getGlobal().info("Unaggregated event received");
		nEvents++;
	}

	@Override
	public void eventSetReceived(EventSet set) {
		//Logger.getGlobal().info("Got EventSet of size: " + set.size());

		if (set.size() == 1) {
			nSingleEvents++;
		}

		nEventSets++;

		eventQueue.add(set);
	}

	@Override
	public void registersReceived(RegisterSet regs) {

	}

	@Override
	public void deviceInfoReceived(DeviceInfo info) {
		Logger.getGlobal().info(String.format("Device version: %02x", info.m_Version));
	}
}