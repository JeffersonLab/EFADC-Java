package org.jlab.EFADC.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jlab.EFADC.*;
import org.jlab.EFADC.handler.BasicClientHandler;
import org.jlab.EFADC.handler.ClientHandler;
import org.jlab.EFADC.process.ProcessingStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Created by john on 12/17/14.
 */
public class ServerHandler extends BasicClientHandler {

    static final int maxQueueSize = 500000;

    private LinkedBlockingQueue<EventSet> eventQueue;
    private ClientHandler m_DelegateHandler;
    private ProcessingStrategy m_Strategy;

    private ExecutorService m_CachedThreadPool;

    private Collection<Worker> m_Workers;
    private List<Future<Integer>> m_Futures;

    class Worker implements Callable<Integer> {

        private static final int DRAIN_MAX = 1000;

        boolean shouldContinue = true;
        int count;

        List<EventSet> events = new ArrayList<>(DRAIN_MAX);

        @Override
        public Integer call() throws Exception {

            while (shouldContinue) {

                // Try to take events off the queue, or wait for one if there are none
                if (eventQueue.drainTo(events, DRAIN_MAX) == 0) {

                    try {
                        events.add(eventQueue.poll(10, TimeUnit.MILLISECONDS));

                        // We don't care if we're interrupted
                    } catch (InterruptedException e) {}
                }

                int size = events.size();

                if (size == 0)
                    continue;

                count += size;

                for (EventSet set : events) {
                    m_Strategy.processEventSet(set);
                }
            }

            return count;
        }
    }

    ServerHandler(ClientHandler clientHandler) {
        eventQueue = new LinkedBlockingQueue<>(maxQueueSize);

        m_DelegateHandler = clientHandler;

        m_CachedThreadPool = Executors.newCachedThreadPool();

        m_Workers = new ArrayList<>();

        m_Workers.add(new Worker());
    }


    public void stopAllProcessing() {

        for (Worker w : m_Workers) {
            w.shouldContinue = false;
        }

        try {

            for (Future<Integer> f : m_Futures) {
                Integer result = f.get();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connected(Client client) {
        m_DelegateHandler.connected(client);

        // Start processing threads
        try {
            m_Futures = m_CachedThreadPool.invokeAll(m_Workers);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void eventSetReceived(EventSet set) {

        if (!eventQueue.offer(set)) {
            handleQueueFull(set);
        }

    }


    void handleQueueFull(EventSet set) {

    }

    @Override
    public void eventReceived(EFADC_DataEvent event) {
        Logger.getLogger("global").info("Unaggregated event received");
    }


    @Override
    public void registersReceived(RegisterSet regs) {
        m_DelegateHandler.registersReceived(regs);
    }


    @Override
    public void bufferReceived(ChannelBuffer buffer) {
    }

}
