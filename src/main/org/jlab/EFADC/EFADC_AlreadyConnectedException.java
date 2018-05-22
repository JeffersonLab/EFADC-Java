package org.jlab.EFADC;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/12/12
 * Time: 11:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class EFADC_AlreadyConnectedException extends EFADC_Exception {

	public EFADC_AlreadyConnectedException() {
		super("Already connected");
	}

	public EFADC_AlreadyConnectedException(String message) {
		super(message);
	}
}
