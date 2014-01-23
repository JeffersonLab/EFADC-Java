package org.jlab.EFADC;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/13/12
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class EFADC_ChannelContext {
	private EFADC_Client	m_Client;
	private Object			m_Object;
	private long			m_Updated;
	
	public EFADC_Client getClient() {
		return m_Client;
	}
	
	public void setClient(EFADC_Client client) {
		m_Client = client;
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
		return "[" + m_Client + "  " + m_Object + "  (" + m_Updated + ")]";
	}
}
