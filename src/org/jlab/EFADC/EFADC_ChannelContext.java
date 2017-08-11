package org.jlab.EFADC;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/13/12
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class EFADC_ChannelContext {
	private NetworkClient	m_NetworkClient;
	private Object			m_Object;
	private long			m_Updated;
	
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

	public long getLastUpdated() {
		return m_Updated;
	}
	
	public String toString() {
		return "[" + m_NetworkClient + "  " + m_Object + "  (" + m_Updated + ")]";
	}
}
