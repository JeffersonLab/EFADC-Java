package org.jlab.EFADC.test;

import org.jlab.EFADC.EFADC_DataEvent;

import java.util.*;

/**
 * Created by john on 5/12/16.
 *
 */
public class PedestalFinder {
	ArrayList<Map<Integer, Integer>> chan;

	public PedestalFinder(int chans) {
		chan = new ArrayList<>(chans);

		for (int i = 0; i < chans; i++)
			chan.add(i, new HashMap<Integer, Integer>());
	}

	public void addEvent(EFADC_DataEvent evt) {

		int chanIdx = 0;
		for (int i = 0; i < 16; i++) {
			if (evt.chanActive[i]) {
				//if ((System.currentTimeMillis() / 1000) % 2 == 0)
				//	System.out.printf("[%d] sum %d  ped %d\n", i, event.sums[chanIdx], pedData[i]);

				int nGlobalChannel = i + (16 * (evt.modId - 1));

				Map<Integer, Integer> aChannel = chan.get(nGlobalChannel);

				int nChanValue = 0;

				if (aChannel.containsKey(evt.sums[chanIdx])) {
					nChanValue = aChannel.get(evt.sums[chanIdx]) + 1;
				}

				aChannel.put(evt.sums[chanIdx], nChanValue);

				//System.out.printf("[%d:%d] ", chanIdx, evt.sums[chanIdx]);

				chanIdx++;
			}
			//System.out.println();
		}
	}


	public int[] findMaxima() {
		int[] data = new int[chan.size()];

		for (int i = 0; i < chan.size(); i++) {

			Map<Integer,Integer> channel = chan.get(i);

			int max = 0;

			Set<Map.Entry<Integer,Integer>> entrySet = channel.entrySet();
			Iterator<Map.Entry<Integer,Integer>> it = entrySet.iterator();

			while (it.hasNext()) {
				Map.Entry<Integer,Integer> entry = it.next();

				if (entry.getValue() > max)
					max = entry.getKey();
			}

			data[i] = max;
		}

		return data;
	}
}
