package org.jlab.EFADC;

import org.jlab.EFADC.handler.ClientHandler;

import java.io.File;
import java.io.IOException;

/**
 * org.jlab.EFADC
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 5/15/13
 */
public interface Client {

	//public void Init();

	void SetADCPositive();
	void SetADCNegative();

	void SetCoincidenceWindowWidth(int width);

	void SetANDCoincident(int detA, int detB, boolean val, boolean reverse);
	void SetORCoincident(int detA, int detB, boolean val, boolean reverse);

	void SetIdentityMatrix();
	void SetZeroMatrix();

	void SetIntegrationWindow(int window);
	void SetIntegrationWindow(int adc, int window);
	void SetMode(int mode);

	void SetNSB(int window);

	void SetSelfTrigger(boolean enable, int value);
	void SetSync(boolean val);
	void SetThreshold(int det, int thresh);

	boolean SendSetRegisters(int adc);

	boolean SendLCDData(int line, String text);

	boolean IsCMP();
	boolean isConnected();

	boolean SetDACValues(int[] values);

	boolean StartCollection() throws Exception;
	boolean StopCollection() throws Exception;

	boolean ReadRegisters();

	//public void SetRawOutputFile(File file) throws IOException;
	//public void SetRawOutputFile(String filename) throws IOException;

	//public void cleanup();

	RegisterSet getRegisterSet();
	void setRegisterSet(RegisterSet regs);

	NetworkClient networkClient();

	//public void setHandler(ClientHandler handler);

}
