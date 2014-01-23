package org.jlab.EFADC;

import org.jlab.EFADC.handler.ClientHandler;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/14/12
 */
public class EFADC_EventAggregator /* extends OneToOneDecoder */ {
	// When multiple EFADC's are connected via a CMP, triggers may arrive out of order
	// This will happen when a coincident event occurs across two EFADC's and a new trigger
	// is generated in the time it takes the first event to propegate to the PC

	// The CMP will guarantee that event E is complete upon receipt of event E+N, where N is
	// the number of connected EFADC's

	// Set of module data containing identical trigger data
	private LinkedHashMap<Integer, EventSet> m_EventMap;

	private ClientHandler m_Handler = null;

	private int m_TreeSize;

	/**
	 * @param nSize Number of EFADC's connected
	 */
	public EFADC_EventAggregator(int nSize) {
		m_EventMap = new LinkedHashMap<>(nSize);

		m_TreeSize = nSize;

		Logger.getLogger("global").info("new EFADC_EventAggregator()");
	}


	public EFADC_EventAggregator(int nSize, ClientHandler handler) {
		m_EventMap = new LinkedHashMap<>(nSize);

		m_TreeSize = nSize;

		setHandler(handler);
	}


	public void setHandler(ClientHandler handler) {
		m_Handler = handler;
	}


	/**
	 * Aggregate EFADC_DataEvents into EventSets and forward completed events
	 * @param ctx
	 * @param channel
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	/*
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object obj) throws Exception {

		if (!(obj instanceof EFADC_DataEvent))
			return obj;

		EFADC_DataEvent evt = (EFADC_DataEvent)obj;

		// Look for existing event by trigger id
		EventSet es = m_EventMap.get(evt.trigId);

		if (es == null) {
			// No mapping for this key exists, create new set
			es = new EventSet(evt);

			m_EventMap.put(evt.trigId, es);

			Iterator<Map.Entry<Integer, EventSet>> entryIterator = m_EventMap.entrySet().iterator();

			// First element in list
			Map.Entry<Integer, EventSet> first = entryIterator.next();

			// Remove if new event is more than TreeSize triggers greater
			if (m_EventMap.size() >= m_TreeSize) {

				entryIterator.remove();

				return first.getValue();
			}

		} else {

			// Event exists, add to set
			es.add(evt);
		}

		return m_EventMap.size();
	}
	*/


	public Object process(EFADC_DataEvent evt) throws Exception {

		// Look for existing event by trigger id
		EventSet es = m_EventMap.get(evt.trigId);

		if (es == null) {
			// This is the first time we've seen this trigger ID, create a new set
			es = new EventSet(evt);

			m_EventMap.put(evt.trigId, es);

			Iterator<Map.Entry<Integer, EventSet>> entryIterator = m_EventMap.entrySet().iterator();

			// First element in list
			Map.Entry<Integer, EventSet> first = entryIterator.next();

			// Remove if new event is more than TreeSize triggers greater
			if (m_EventMap.size() >= m_TreeSize) {

				entryIterator.remove();

				return first.getValue();
			}

		} else {
			// Event exists, add to set

			//System.out.printf("+");

			es.add(evt);
		}


		return m_EventMap.size();
	}


	public void flush() {
		for (Iterator<Map.Entry<Integer, EventSet>> it = m_EventMap.entrySet().iterator(); it.hasNext(); ) {

			Map.Entry<Integer, EventSet> evt = it.next();

			if (m_Handler != null)
				m_Handler.eventSetReceived(evt.getValue());

			it.remove();
		}

	}
}
