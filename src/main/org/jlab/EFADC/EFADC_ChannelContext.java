package org.jlab.EFADC;

import org.jlab.EFADC.handler.ClientHandler;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/13/12
 * Time: 1:28 PM
 */
public class EFADC_ChannelContext {
	private NetworkClient			m_NetworkClient;
	private Object					m_Object;
	private long					m_Updated;
	private Client					m_DeviceClient;
	private EFADC_EventAggregator	m_Aggregator;
	private ClientHandler			m_Listener;
	private boolean					isCMP;

	EFADC_ChannelContext() {
		m_Aggregator = new EFADC_EventAggregator(10);
	}
	
	public NetworkClient getClient() {
		return m_NetworkClient;
	}
	
	public void setClient(NetworkClient client) {
		m_NetworkClient = client;
	}

	public EFADC_EventAggregator getAggregator() {
		return m_Aggregator;
	}
	
	public Object getObject() {
		return m_Object;
	}
	
	public void setObject(Object obj) {
		m_Object = obj;
		m_Updated = System.currentTimeMillis();
	}

	public boolean isCMP() {
		return isCMP;
	}

	public void setCMP(boolean val) {
		isCMP = val;
	}

	public Client getDeviceClient() {
		Logger.getGlobal().info("getDeviceClient: " + m_DeviceClient);
		return m_DeviceClient;
	}

	public void setDeviceClient(Client c) {
		Logger.getGlobal().info("setDeviceClient: " + c);
		m_DeviceClient = c;
	}

	public long getLastUpdated() {
		return m_Updated;
	}

	public void setListener(ClientHandler listener) {
		m_Listener = listener;
		m_Aggregator.setListener(listener);
	}

	public ClientHandler getListener() {
		return m_Listener;
	}
	
	public String toString() {
		return "[" + m_NetworkClient + "  " + m_Object + "  (" + m_Updated + ")]";
	}
}
