package org.jlab.EFADC.test;

import org.jlab.EFADC.*;

/**
 * org.jlab.EFADC.test
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 4/18/13
 */
public class Main {

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


	Main() {

		/*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		startAcquisition();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		stopAcquisition();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		startAcquisition();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		stopAcquisition();
		*/

		/*
		boolean again = true;

		do {
			Logger.getGlobal().info("\n *** Select C (Collect On), O (Collect Off) or E (Exit) ***\n");
			String input = "";
			try {
				input = scanner.nextLine();
			} catch (Exception e) {}

			if (input.startsWith("E"))
				again = false;
			else if (input.startsWith("C")) {
				startAcquisition();
			} else if (input.startsWith("O")) {
				stopAcquisition();
			}

		} while (!again);
		*/

		/*
		Logger.getGlobal().info("\n *** Press ENTER to run pedestal calibration ***\n");
		try {
			scanner.nextLine();
		} catch (Exception e) {}
		*/

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

		//setupLogging();

		new Main();

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
			Logger.getGlobal().info("Error reading registers");
			System.exit(-1);
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}


		if (flag_Run || flag_SelfTrig) {
			Logger.getGlobal().info("Run Starting...");

			theClient.getRegisterSet().setSync(true);
			if (!theClient.SendSetRegisters()) {
				Logger.getGlobal().info("Error setting registers");
				System.exit(-1);
			}

			theClient.getRegisterSet().setSync(false);
			if (!theClient.SendSetRegisters()) {
				Logger.getGlobal().info("Error setting registers");
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

			Logger.getGlobal().info("Stopping...");

			if (!theClient.StopCollection())
				System.out.println("Error stopping data collection");
		}


		if (!theClient.ReadRegisters()) {
			Logger.getGlobal().info("Error reading registers");
			System.exit(-1);
		}


		if (flag_Run || flag_SelfTrig) {

			int events = theClient.getHandler().getEventCount();

			float rate = events / (time * 1.0f);

			Logger.getGlobal().info(String.format("%d events, %f KHz, %d missed", events, rate, theClient.getHandler().getMissedEventCount()));
		}


		theClient.cleanup();
		*/
	}
}
