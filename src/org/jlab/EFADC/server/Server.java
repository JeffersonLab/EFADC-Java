package org.jlab.EFADC.server;

import org.jlab.EFADC.Client;
import org.jlab.EFADC.Connector;
import org.jlab.EFADC.handler.AbstractClientHandler;
import org.jlab.EFADC.handler.ClientHandler;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Created by john on 12/17/14.
 *
 * Server is a standalone application that runs an acquisition using an EFADC
 *
 * A client provides a ProcessingStrategy which implements any processing of the raw events
 *
 * An OutputStrategy can be provided as well which can write raw or corrected data to disk
 */
public class Server {

    private Client daqClient = null;
    //private ClientHandler m_EventHandler;
    private ServerHandler m_ServerHandler;
    private boolean m_IsDebug = false;
    private int m_TimeOutSec = 5;

    Server(ClientHandler handler) {
        //m_EventHandler = handler;

        m_ServerHandler = new ServerHandler(handler);
    }

    public void setDebug(boolean val) {
        m_IsDebug = val;
    }

    public void setTimeOutSec(int val) {
        m_TimeOutSec = val;
    }

    public void connect(String strAddress) {

        try {

            Connector daqConnector = new Connector(strAddress, 4999);

            Future<Client> connectFuture = daqConnector.connect(m_IsDebug);

            Logger.getLogger("global").info("Connecting...");

            daqClient = connectFuture.get(m_TimeOutSec, TimeUnit.SECONDS);

            if (daqClient == null) {

                Logger.getLogger("global").severe("Failed to connect!");
                if (m_ServerHandler != null) {
                    m_ServerHandler.error("Failed to connect!");
                }

            } else {

                Logger.getLogger("global").info("Connected!  Replacing event handler");

                daqClient.setHandler(m_ServerHandler);

                if (daqClient.IsCMP()) {
                    Logger.getLogger("global").info("CMP Detected, telling new handler");
                    m_ServerHandler.SetCMP(true);
                }

                m_ServerHandler.connected(daqClient);

                //This should happen in the connected() handler in the toolsheet
                //btnSetDACBias(null);
                //tlsh.setProperty("STATUSSTR", "Connected");
                //daqClient.SetADCNegative();
            }

        } catch (TimeoutException e) {
            Logger.getLogger("global").severe("Timeout, did not connect");

            if (m_ServerHandler != null) {
                m_ServerHandler.error("Error initializing EFADC.  Timeout, did not connect.");
            }

        } catch (Exception e2) {
            Logger.getLogger("global").throwing(this.getClass().getName(), "connect", e2);
            e2.printStackTrace();
        }
    }

}
