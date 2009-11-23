package org.apache.synapse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.TransportSender;

public class DummySender implements TransportSender {

	public void cleanup(MessageContext msgContext) throws AxisFault {
		//do nothing
	}

	public void init(	ConfigurationContext confContext,
						TransportOutDescription transportOut) throws AxisFault
	{
		//do nothing
	}

	public void stop()
	{
		//do nothing
	}

	public void flowComplete(MessageContext msgContext)
	{
		//do nothing
	}

	public HandlerDescription getHandlerDesc()
	{
		return null;
	}

	public String getName()
	{
		return null;
	}

	public Parameter getParameter(String name)
	{
		return null;
	}

	public void init(HandlerDescription handlerDesc)
	{
		//do nothing
	}

	public InvocationResponse invoke(MessageContext msgContext)
		throws AxisFault
	{
		return null;
	}
}
