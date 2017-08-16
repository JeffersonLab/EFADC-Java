package org.jlab.EFADC.test;

import org.jlab.EFADC.*;
import org.jlab.EFADC.handler.AbstractClientHandler;
import org.jlab.EFADC.handler.BasicClientHandler;
import org.jlab.EFADC.handler.ClientHandler;
import org.jlab.EFADC.logging.ErrorFormatter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * org.jlab.EFADC.test
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 4/18/13
 */
public class Test {

	static boolean flag_Run = false;
	static boolean flag_SelfTrig = false;
	static boolean flag_Verbose = false;

	//Command line options
	static String outputFile = "";
	static int time = 1000;
	static int rate = 100;
	static int mode = 0;	//normal or sampling mode
	static int width = 8;
	static int cMode = 0;	//coincidence mode
	static int nsb = 30;
	static int[] thresh;
	static int[] pedestal;

	NetworkClient m_NetworkClient;
	Client m_DeviceClient;
	ClientHandler m_Handler;
	Connector m_Con;


	class TestClientHandler extends BasicClientHandler {
		LinkedBlockingQueue<EventSet> eventQueue;

		public int nEventSets = 0;
		public int nEvents = 0;
		public int nSingleEvents = 0;

		TestClientHandler() {
			eventQueue = new LinkedBlockingQueue<>();
		}

		public void clearEventQueue() {
			eventQueue.clear();
		}

		public LinkedBlockingQueue<EventSet> getEventQueue() {
			return eventQueue;
		}

		@Override
		public void connected(Client client) {
			//m_Client = client;

			Logger.getGlobal().info("in main ClientHandler, connected()");
		}

		@Override
		public void registersReceived(RegisterSet regs) {
			super.registersReceived(regs);

			//Logger.getLogger("global").info("registersReceived");

			// This cast should be avoided, put setRegisterSet in the Client interface?
			m_DeviceClient.setRegisterSet(regs);
		}

		@Override
		public void eventReceived(EFADC_DataEvent event) {
			//Logger.getLogger("global").info("Unaggregated event received");
			nEvents++;
		}

		@Override
		public void eventSetReceived(EventSet set) {
			//Logger.getLogger("global").info("Got EventSet of size: " + set.size());

			if (set.size() == 1) {
				nSingleEvents++;
			}

			nEventSets++;

			eventQueue.add(set);
		}

		@Override
		public void deviceInfoReceived(DeviceInfo info) {
			Logger.getGlobal().info(String.format("Device version: %02x", info.m_Version));
		}
	}


	Test() {

		m_Handler = new TestClientHandler();

		m_Con = new Connector("129.57.53.60", 14999);

		// Open socket and request device info
		Future<EFADC_Client> connectFuture = m_Con.connect(true);	// debugging on

		try {
			// Wait for connection process, this occurs after device info is received and proper
			// device registers are requested and received
			m_DeviceClient = connectFuture.get(5, TimeUnit.SECONDS);

		} catch (TimeoutException e) {
			Logger.getGlobal().severe("Timed out trying to connect");

			return;

		} catch (Exception e) {
			e.printStackTrace();

		}

		if (m_DeviceClient == null) {
			Logger.getGlobal().warning("Did not connect!");
			return;
		}

		m_NetworkClient = m_DeviceClient.networkClient();

		// Aha! We need to detect if the old handler was a CMP or not
		m_NetworkClient.setHandler(m_Handler);

		/** TODO: Implement device selection
		if (m_NetworkClient.IsCMP()) {
			Logger.getLogger("global").info("CMP Detected, telling new handler");
			((AbstractClientHandler)m_Handler).SetCMP(true);
		}
		*/


		Logger.getGlobal().info("Running initialization...");
		init();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	/*
		try {
			m_NetworkClient.SetRawOutputFile("ped_out.bin");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Logger.getLogger("Registers after Init()");
		m_DeviceClient.ReadRegisters();


		Logger.getLogger("global").info("Running pedestal acquisition...");
		pedestals();

		Collection<EventSet> events = new LinkedList<>();

		TestClientHandler testHandler = (TestClientHandler)m_Handler;

		int eventCount = testHandler.getEventQueue().drainTo(events);

		Logger.getLogger("global").info(String.format("Acquisition Complete, handler events: %d, queue events: %d, nEventSets: %d, nEvents: %d, singleEvents: %d",
				m_Handler.getEventCount(), events.size(), testHandler.nEventSets, testHandler.nEventSets, testHandler.nSingleEvents));


		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Logger.getLogger("Registers after acquisition");
		m_DeviceClient.ReadRegisters();
	*/
	}


	// Basic daq initialization
	private void init() {

		// Need to enable EFADC's
		m_DeviceClient.SendSetRegisters(0);

		Logger.getGlobal().info("Reading EFADC registers");

		((ETS_Client)m_DeviceClient).ReadEFADCRegisters();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Sync behavior has changed with the master/slave setup
		//m_Client.SetSync(true);

		/*

		m_DeviceClient.SetDACValues(new int[] {3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300,
				3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300});


		// This should be called last because it internally calls SendSetRegisters
		m_DeviceClient.SetADCNegative();

		m_DeviceClient.SetThreshold(0, 1000);
		m_DeviceClient.SetThreshold(1, 1001);
		m_DeviceClient.SetThreshold(2, 1002);
		m_DeviceClient.SetThreshold(3, 1003);
		m_DeviceClient.SetThreshold(4, 1004);
		m_DeviceClient.SetThreshold(5, 1005);
		m_DeviceClient.SetThreshold(6, 1006);
		m_DeviceClient.SetThreshold(7, 1007);
		m_DeviceClient.SetIntegrationWindow(25);

		m_DeviceClient.SetNSB(50);
		m_DeviceClient.SetMode(0);

		m_DeviceClient.SetIdentityMatrix();

		m_DeviceClient.SendSetRegisters(1);

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}

		m_DeviceClient.SendSetRegisters(2);


		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {

		}
		*/

	}


	private void startAcquisition() {
		//m_Client.SetSync(false);

		//m_Client.SendSetRegisters(true);


		try {
		//	Thread.sleep(50);

			m_DeviceClient.StartCollection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void stopAcquisition() {
		try {
			m_DeviceClient.StopCollection();

			Thread.sleep(50);

			// Flush event aggregator
			m_DeviceClient.ReadRegisters();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void pedestals() {

		//Adjust coincidence window widths so the self triggering correlates
		m_DeviceClient.SetCoincidenceWindowWidth(50);


		m_DeviceClient.SetSelfTrigger(true, 200);	// ~10Khz trigger
		m_DeviceClient.SendSetRegisters(1);		// Need to send to all efadcs
		m_DeviceClient.SendSetRegisters(2);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		startAcquisition();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		stopAcquisition();

		m_DeviceClient.SetSelfTrigger(false, 200);
		m_DeviceClient.SendSetRegisters(1);
		m_DeviceClient.SendSetRegisters(2);
	}


	private static void setupLogging() {
		Logger.getLogger("global").setUseParentHandlers(false);
		Handler h = new ConsoleHandler();
		h.setFormatter(new ErrorFormatter());
		Logger.getLogger("global").addHandler(h);
	}


	private static void parseArguments(String[] args, EFADC_Client client) {
		int i = 0, j;
		String arg;
		char flag;

		while (i < args.length && args[i].startsWith("-")) {
			arg = args[i++];

			if (arg.equals("-run")) {
				flag_Run = true;

			} else if (arg.equals("-verbose")) {
				flag_Verbose = true;
			}

			// use this type of check for arguments that require arguments
			else if (arg.equals("-output")) {
				if (i < args.length)
					outputFile = args[i++];
				else
					System.err.println("-output requires a filename");

				if (flag_Verbose)
					System.out.println("output file = " + outputFile);

			} else if (arg.equals("-mode")) {
				if (i < args.length)
					try {
						mode = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						mode = 0;
					}
				else
					System.err.println("Mode must be 0 or 1");

				if (flag_Verbose)
					System.out.println("Mode " + mode);

			} else if (arg.equals("-ped")) {
				int ped = -1, val = -1;
				if (i < args.length + 1) {
					try {
						ped = Integer.parseInt(args[i++]);
						val = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Arguments must be numbers");
					}

					pedestal[ped] = val;

				} else
					System.err.println("-ped requires two arguments");

				if (flag_Verbose)
					System.out.println("Pedestal " + ped + " value " + val);

			} else if (arg.equals("-register")) {
				int reg = -1, val = -1;
				if (i < args.length + 1) {
					try {
						reg = Integer.parseInt(args[i++]);
						val = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Arguments must be numbers");
					}

					if (!client.getRegisterSet().setRegister(reg, val))
						System.err.println("Invalid register: " + reg);

				} else
					System.err.println("-register requires two arguments");

				if (flag_Verbose)
					System.out.println("Register " + reg + " value " + val);

			} else if (arg.equals("-thresh")) {
				int module = -1, val = -1;
				if (i < args.length + 1) {
					try {
						module = Integer.parseInt(args[i++]);
						val = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Arguments must be numbers");
					}

					if (module < 0 || module > 3)
						System.err.println("Module must be 0 to 3");
					else
						thresh[module] = val;

				} else
					System.err.println("-thresh requires two arguments");

				if (flag_Verbose)
					System.out.printf("Module %d threshold %d\n", module, val);

			} else if (arg.equals("-time")) {

				if (i < args.length)
					try {
						time = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Invalid time argument, defaulting to 1000ms");
					}
				else
					System.err.println("-time requires a time limit");

				if (flag_Verbose)
					System.out.println("Time set to " + time + "ms");

			} else if (arg.equals("-selftrig")) {
				if (i < args.length) {
					try {
						rate = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Invalid rate argument, defaulting to 100ms");
					}
					flag_SelfTrig = true;

				} else
					System.err.println("-selftrig requires a trigger rate");

				if (flag_Verbose)
					System.out.println("Self triggering at rate: " + rate);

			} else if (arg.equals("-window")) {
				if (i < args.length) {
					try {
						width = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Invalid width argument, defaulting to 10 samples");
					}

				} else
					System.err.println("-width requires an argument");

				if (flag_Verbose)
					System.out.println("Integration Width: " + width);

			} else if (arg.equals("-cmode")) {
				if (i < args.length) {
					try {
						cMode = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Invalid mode argument, defaulting to mode 0");
					}

				} else
					System.err.println("-cmode requires an argument");

				if (flag_Verbose)
					System.out.println("Coincidence Mode: " + cMode);

			} else if (arg.equals("-nsb")) {
				if (i < args.length) {
					try {
						nsb = Integer.parseInt(args[i++]);
					} catch (NumberFormatException e) {
						System.err.println("Invalid NSB value, defaulting to " + nsb);
					}

				} else
					System.err.println("-nsb requires an argument");

				if (flag_Verbose)
					System.out.println("NSB: " + nsb);

				// use this type of check for a series of flag arguments
			} else {
				for (j = 1; j < arg.length(); j++) {
					flag = arg.charAt(j);
					switch (flag) {
						case 'x':
							if (flag_Verbose) System.out.println("Option x");
							break;
						case 'n':
							if (flag_Verbose) System.out.println("Option n");
							break;
						default:
							System.err.println("illegal option " + flag);
							break;
					}
				}
			}
		}

	}

	public static void main(String[] args) throws Exception {

		setupLogging();

		new Test();

		/*

		//default thresholds
		thresh = new int[] {3000, 3000, 3000, 3000};

		pedestal = new int[] {3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300, 3300};



		EFADC_Client theClient = new EFADC_Client("1.2.3.9", 5000, false);

		parseArguments(args, theClient);

		if (!outputFile.equals("")) {
			try {
				theClient.setRawOutputFile(outputFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		theClient.setHandler(new BasicClientHandler());

		if (!theClient.SetDACValues(pedestal)) {
			theClient.cleanup();
			System.exit(-1);
		}



		//if (!theClient.SendLCDData(1, "EFADC-16 Java"))
		//	System.exit(-1);

		if (flag_SelfTrig) {
			theClient.getRegisterSet().setSelfTrigger(true, rate);
		}

		theClient.getRegisterSet().setThreshold(0, thresh[0]);
		theClient.getRegisterSet().setThreshold(1, thresh[1]);
		theClient.getRegisterSet().setThreshold(2, thresh[2]);
		theClient.getRegisterSet().setThreshold(3, thresh[3]);

		//theClient.SetCoincidenceTableEntry(cMode);
		theClient.getRegisterSet().setIntegrationWindow(width);
		theClient.getRegisterSet().setMode(mode);
		theClient.getRegisterSet().setNSB(nsb);
		if (!theClient.SendSetRegisters()) {
			System.err.println("Error setting registers");
			theClient.cleanup();
			System.exit(-1);
		}


		if (!theClient.ReadRegisters()) {
			Logger.getLogger("global").info("Error reading registers");
			System.exit(-1);
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}


		if (flag_Run || flag_SelfTrig) {
			Logger.getLogger("global").info("Run Starting...");

			theClient.getRegisterSet().setSync(true);
			if (!theClient.SendSetRegisters()) {
				Logger.getLogger("global").info("Error setting registers");
				System.exit(-1);
			}

			theClient.getRegisterSet().setSync(false);
			if (!theClient.SendSetRegisters()) {
				Logger.getLogger("global").info("Error setting registers");
				System.exit(-1);
			}

			if (!theClient.StartCollection()) {
				System.out.println("Error starting data collection");
			}

		}

		if (flag_Run || flag_SelfTrig) {
			//Wait
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
			}

			Logger.getLogger("global").info("Stopping...");

			if (!theClient.StopCollection())
				System.out.println("Error stopping data collection");
		}


		if (!theClient.ReadRegisters()) {
			Logger.getLogger("global").info("Error reading registers");
			System.exit(-1);
		}


		if (flag_Run || flag_SelfTrig) {

			int events = theClient.getHandler().getEventCount();

			float rate = events / (time * 1.0f);

			Logger.getLogger("global").info(String.format("%d events, %f KHz, %d missed", events, rate, theClient.getHandler().getMissedEventCount()));
		}


		theClient.cleanup();
		*/
	}
}
