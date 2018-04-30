package org.jlab.EFADC;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/13/12
 * Time: 1:28 PM
 */
public class EFADC_ChannelContext {
	private NetworkClient	m_NetworkClient;
	private Object			m_Object;
	private long			m_Updated;
	private Client			m_DeviceClient;
	
	public NetworkClient getClient() {
		return m_NetworkClient;
	}
	
	public void setClient(NetworkClient client) {
		m_NetworkClient = client;
	}
	
	public Object getObject() {
		return m_Object;
	}
	
	public void setObject(Object obj) {
		m_Object = obj;
		m_Updated = System.currentTimeMillis();
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
	
	public String toString() {
		return "[" + m_NetworkClient + "  " + m_Object + "  (" + m_Updated + ")]";
	}
}
