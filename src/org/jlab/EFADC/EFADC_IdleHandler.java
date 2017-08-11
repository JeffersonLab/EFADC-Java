package org.jlab.EFADC;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jlab.EFADC.command.Command;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: 3/13/12
 * Time: 10:09 AM
 * To change this template use File | Settings | File Templates.
 *
 * Sends a ReadRegisters request when there is no outbound traffic
 * for 30 seconds.  The connection is closed when there is no inbound traffic
 * for 60 seconds.
 */
public class EFADC_IdleHandler extends IdleStateAwareChannelHandler{

	@Override
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws EFADC_Exception {


		/** TODO: Implement Idle Handler
		Client client = ((EFADC_ChannelContext)ctx.getAttachment()).getClient();

		if (evt.getState() == IdleState.READER_IDLE) {

			// Check acquisition state, throw error if active and receiving no data?
			if (client.getCollectState()) {

				// Flush EFADC_EventAggregator buffer
				// TODO: Come back to this
				//client.flushAggregateBuffer();

				throw new EFADC_AcquisitionIdleException("Reader idle while acquisition active");

			} else {

				// This should only happen if a disconnect occurs due to the writer idle handler which sends a
				// register read command, to which we should expect a response
				//Logger.getLogger("global").info("Reader idle, closing channel");
				//evt.getChannel().close();

				// Just throw some efadc exception
				throw new EFADC_ConnectionException("EFADC not communicating, check connection");
			}

		} else if (evt.getState() == IdleState.WRITER_IDLE) {
			Logger.getLogger("global").info("Writer idle, sending ReadRegisters");

			//This will trigger an echo handler error because it is not initiated thru SendCommand...
			client.SendCommand(Command.ReadRegisters());
		}
		 */
	}
}
