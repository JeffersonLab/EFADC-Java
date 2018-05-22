package org.jlab.EFADC;

/**
 * Created by john on 8/18/15.
 */
public class InvalidRegisterException extends EFADC_Exception {
    public InvalidRegisterException() {
        super("Invalid Register");
    }

    public InvalidRegisterException(String message) {
        super(message);
    }
}
