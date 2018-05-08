package org.jlab.EFADC;

import org.jlab.EFADC.handler.ClientHandler;
import org.jlab.EFADC.test.Test;
import org.junit.Before;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class ConnectorTest {

	private Client m_DeviceClient;
	private ClientHandler m_Handler;
	private Connector m_Con;
	private NetworkClient m_NetworkClient;

	@Before
	public void setUp() throws Exception {

		TestCommon.setupLogging();

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
			fail("Timed out trying to connect");
			throw new Exception("Timed out while trying to connect");
		}

		if (m_DeviceClient == null) {
			Logger.getGlobal().warning("Did not connect!");
			throw new Exception("Did not connect");
		}

		m_NetworkClient = m_DeviceClient.networkClient();

		// Aha! We need to detect if the old handler was a CMP or not
		m_NetworkClient.setHandler(m_Handler);
	}

	private void init() {
		Logger.getGlobal().info("Running initialization...");

		m_DeviceClient.SendSetRegisters(0);

		/*
		Logger.getGlobal().info("\n *** Press ENTER to send READ EFADC registers ***\n");
		try {
			scanner.nextLine();
		} catch (Exception e) {}
		*/

		Logger.getGlobal().info("Reading EFADC registers");

		((ETS_Client)m_DeviceClient).ReadEFADCRegisters();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


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
		m_DeviceClient.SetIntegrationWindow(3);

		m_DeviceClient.SetNSB(50);
		m_DeviceClient.SetMode(0);

		m_DeviceClient.SetIdentityMatrix();

		m_DeviceClient.SendSetRegisters(-1);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			m_NetworkClient.SetRawOutputFile("ped_out.bin");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Logger.getLogger("Registers after Init()");
		m_DeviceClient.ReadRegisters();
	}

	private void startAcquisition() {
		//m_Client.SetSync(false);

		//m_Client.SendSetRegisters(true);

		Logger.getGlobal().info(">> Start Acquisition");

		try {
			//	Thread.sleep(50);

			m_DeviceClient.StartCollection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void stopAcquisition() {

		Logger.getGlobal().info(">> Stop Acquisition");

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
		m_DeviceClient.SendSetRegisters(-1);	// Need to send to all efadcs

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
		m_DeviceClient.SendSetRegisters(-1);
	}

	@org.junit.Test
	public void testInit() {

		init();

	}

	@org.junit.Test
	public void testPedestals() {
		init();

		for (int i = 0; i < 5; i++) {

			Logger.getGlobal().info("Running pedestal acquisition...");
			pedestals();

			Collection<EventSet> events = new LinkedList<>();

			TestClientHandler testHandler = (TestClientHandler)m_Handler;

			int eventCount = testHandler.getEventQueue().drainTo(events);

			Logger.getGlobal().info(String.format("Acquisition Complete, handler events: %d, queue events: %d, nEventSets: %d, nEvents: %d, singleEvents: %d",
					m_Handler.getEventCount(), events.size(), testHandler.nEventSets, testHandler.nEventSets, testHandler.nSingleEvents));


			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		Logger.getLogger("Registers after acquisition");
		m_DeviceClient.ReadRegisters();

	}
}