package org.jlab.EFADC;


import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/15/13
 */
public class Connector {

	public static boolean DEBUG = false;

	//private EFADC_Client m_Client;
	//private int port;

	/*
	public Connector(String address, int port) {

		Logger.getLogger("global").info("new Connector");

		try {
			m_Client = new EFADC_Client(address, port, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/


	/**
	 * Performs asynchronous connection to an EFADC/CMP.
	 * @return Future representing the connection status, or null if already connected
	 */
	public static Future<Client> connect(String address, int port, boolean debug) throws Exception {

		Logger.getLogger("global").info("Connecting...");

		EFADC_Client theClient;

		//try {
			theClient = new EFADC_Client(address, port, false);
		//} catch (Exception e) {
		//	e.printStackTrace();
		//	throw new EFADC_Exception("");
		//}

		theClient.setDebug(debug);

		// Set up temporary ClientHandler to detect connection progress
		ConnectFuture future = new ConnectFuture(theClient);

		//try {
			if (DEBUG) Logger.getLogger("global").info(" => connect() future: " + future);

			// This just opens the datagram channel, no communication is initiated until after we install the handler and read registers
			theClient.connect();

			if (DEBUG) Logger.getLogger("global").info(" => setHandler()"  + future);
			theClient.setHandler(future);
		//} //catch (EFADC_AlreadyConnectedException e) {
			//Logger.getLogger("global").severe("Already connected?");
			//return null;
		//}

		future.setConnectState(ConnectFuture.ConnectState.CONNECTING);

		if (DEBUG) Logger.getLogger("global").info(" => ReadRegisters() :: state CONNECTING");

		// Try to read registers to see if we're really connected
		if (!theClient.ReadRegisters()) {
			if (DEBUG) Logger.getLogger("global").severe("ReadRegisters failed in connect()");

			throw new EFADC_Exception("ReadRegsters faiure");
		}

		return future;
	}

}
