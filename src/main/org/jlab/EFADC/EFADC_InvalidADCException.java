package org.jlab.EFADC;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 8/29/13
 */
public class EFADC_InvalidADCException extends EFADC_Exception {

	public EFADC_InvalidADCException() {
		super("Invalid ADC Selection");
	}

	public EFADC_InvalidADCException(String message) {
		super(message);
	}
}
