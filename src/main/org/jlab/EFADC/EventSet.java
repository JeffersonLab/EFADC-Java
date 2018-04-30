package org.jlab.EFADC;

import java.util.ArrayList;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 11/14/12
 */
public class EventSet extends ArrayList<EFADC_DataEvent> {
	public int triggerId;

	EventSet(EFADC_DataEvent e1) {
		triggerId = e1.trigId;

		this.add(e1);
	}

	@Override
	public int hashCode() {
		return triggerId;
	}
}
