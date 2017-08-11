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

	public void SetADCPositive();
	public void SetADCNegative();

	public void SetCoincidenceWindowWidth(int width);

	public void SetANDCoincident(int detA, int detB, boolean val, boolean reverse);
	public void SetORCoincident(int detA, int detB, boolean val, boolean reverse);

	public void SetIdentityMatrix();
	public void SetZeroMatrix();

	public void SetIntegrationWindow(int window);
	public void SetIntegrationWindow(int adc, int window);
	public void SetMode(int mode);

	public void SetNSB(int window);

	public void SetSelfTrigger(boolean enable, int value);
	public void SetSync(boolean val);
	public void SetThreshold(int det, int thresh);

	public boolean SendSetRegisters(int adc);

	public boolean SendLCDData(int line, String text);

	public boolean IsCMP();

	public boolean SetDACValues(int[] values);

	public boolean StartCollection() throws Exception;
	public boolean StopCollection() throws Exception;

	public boolean ReadRegisters();

	//public void SetRawOutputFile(File file) throws IOException;
	//public void SetRawOutputFile(String filename) throws IOException;

	//public void cleanup();

	RegisterSet getRegisterSet();
	void setRegisterSet(RegisterSet regs);

	NetworkClient networkClient();

	//public void setHandler(ClientHandler handler);

}
