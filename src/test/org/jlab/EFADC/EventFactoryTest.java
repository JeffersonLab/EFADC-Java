package org.jlab.EFADC;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class EventFactoryTest {

	@Test
	public void testDecodeDataEvent() {


		String[] rawStr = new String[] {"5a:5a:03:01:00:01:00:00:0f:0f:29:16:00:37:5a:bc:4a:7f:00:00:58:8b:00:00:44:c2:00:00:3d:d2:00:00:54:2a:00:00:44:0b:00:00:4e:77:00:00:47:65:00:00:45:e3:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:18:00:37:5a:c0:5f:f2:00:00:5d:69:00:00:3f:45:00:00:45:e0:00:00:4c:22:00:00:73:62:00:00:5e:67:00:00:75:d4:00:00:54:7d:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:19:00:37:5a:c3:48:06:00:00:5d:81:00:00:60:e5:00:00:40:10:00:00:73:41:00:00:58:59:00:00:72:31:00:00:54:61:00:00:70:6d:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:1a:00:37:5a:c5:9a:7a:00:00:72:a4:00:00:5d:5f:00:00:4b:40:00:00:79:dd:00:00:5a:e1:00:00:6a:64:00:00:5b:37:00:00:63:8a:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:1b:00:37:5a:d0:4a:ae:00:00:7c:ac:00:00:4a:41:00:00:4c:38:00:00:70:3e:00:00:48:0a:00:00:4c:bc:00:00:4a:1a:00:00:45:b0:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:1c:00:37:5a:d0:4f:55:00:00:6a:f3:00:00:62:c9:00:00:43:e6:00:00:7f:3f:00:00:59:83:00:00:6f:c5:00:00:54:b1:00:00:6e:7c:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:1d:00:37:5a:d1:dc:f4:00:00:63:71:00:00:6a:d8:00:00:60:82:00:00:62:20:00:00:7c:b8:00:00:58:71:00:00:59:91:00:00:74:d6:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:1e:00:37:5a:d9:28:e7:00:00:48:73:00:00:65:50:00:00:51:ec:00:00:4f:ec:00:00:47:ca:00:00:43:bd:00:00:3d:f5:00:00:48:c3:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:1f:00:37:5a:da:62:d5:00:00:6d:fc:00:00:61:6d:00:00:55:3a:00:00:6f:72:00:00:50:17:00:00:4d:45:00:00:45:ae:00:00:52:3d:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:20:00:37:5a:da:b4:2f:00:00:69:04:00:00:57:0d:00:00:3e:98:00:00:76:cb:00:00:58:20:00:00:87:09:00:00:6e:43:00:00:6a:ca:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:21:00:37:5a:db:c5:23:00:00:58:42:00:00:4e:a1:00:00:46:ea:00:00:54:fd:00:00:50:80:00:00:49:82:00:00:47:b8:00:00:4c:82:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:22:00:37:5a:df:68:df:00:20:d2:9f:00:00:54:5c:00:00:a6:bd:00:00:6d:29:00:00:4d:aa:00:00:43:28:00:00:45:03:00:00:46:0f:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:23:00:37:5a:df:fd:ee:00:00:75:1b:00:00:40:6b:00:00:60:b3:00:00:49:9f:00:00:6c:bb:00:00:4a:a4:00:00:66:d5:00:00:49:8f:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:24:00:37:5a:e0:4e:a8:00:00:67:65:00:00:5a:d4:00:00:56:03:00:00:61:47:00:00:73:4a:00:00:5e:3c:00:00:57:10:00:00:73:d1:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:25:00:37:5a:e3:e7:b3:00:00:60:8b:00:00:6e:73:00:00:6c:f8:00:00:55:b9:00:21:41:34:00:00:96:33:00:00:83:96:00:21:47:92:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:26:00:37:5a:e6:7f:26:00:00:71:96:00:00:54:80:00:00:66:6e:00:00:53:f5:00:00:6a:df:00:00:4c:ee:00:00:57:ca:00:00:5a:44:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:27:00:37:5a:e9:81:c8:00:00:68:cb:00:00:3f:18:00:00:4d:46:00:00:4f:90:00:00:69:07:00:00:54:ab:00:00:6a:34:00:00:4c:2b:00:00",
										"5a:5a:03:01:00:01:00:00:0f:0f:29:28:00:37:5a:fe:33:87:00:00:5b:0c:00:00:64:4b:00:00:3e:fe:00:00:75:37:00:00:5b:3e:00:00:76:13:00:00:52:59:00:00:78:b7:00:00"};

		for (int j = 0; j < rawStr.length; j++) {

			String[] strBytes = rawStr[j].split(":");
			byte[] rawBytes = new byte[strBytes.length];

			for (int i = 0; i < rawBytes.length; i++) {
				rawBytes[i] = (byte)Integer.parseInt(strBytes[i], 16);
			}

			ChannelBuffer buf = HeapChannelBufferFactory.getInstance().getBuffer(rawBytes, 0, rawBytes.length);

			int mark = buf.readerIndex();

			int header = buf.getUnsignedShort(mark);	// skip 2

			if (header != 0x5a5a) {
				fail("invalid header");
			}

			int type = buf.getUnsignedShort(mark + 2);	// skip 4

			if (type != 0x0301) {
				fail("invali type");
			}

			int temp = buf.getUnsignedShort(mark + 4);

			EFADC_Event event = EventFactory.decodeEvent(temp, mark, buf);

			if (!(event instanceof EFADC_DataEvent)) {
				fail("Event is not a data event");
			}

			EFADC_DataEvent dEvent = (EFADC_DataEvent)event;

			int tId = dEvent.getTriggerId();
			long tStamp = dEvent.getTimestamp();

			System.out.printf("%d\t%08x\n", tId, tStamp);

			if (tStamp < 0) {
				fail("sign extended timestamp");
			}
		}
	}
}