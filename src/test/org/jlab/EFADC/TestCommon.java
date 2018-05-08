package org.jlab.EFADC;

import org.jlab.EFADC.logging.ErrorFormatter;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class TestCommon {

	public static void setupLogging() {
		Logger.getGlobal().setUseParentHandlers(false);
		Handler h = new ConsoleHandler();
		h.setFormatter(new ErrorFormatter());
		Logger.getGlobal().addHandler(h);
	}
}
