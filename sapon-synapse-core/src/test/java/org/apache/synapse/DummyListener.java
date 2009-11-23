package org.apache.synapse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;

public class DummyListener implements TransportListener {

	public void destroy()
	{
		//do nothing
	}

	public EndpointReference getEPRForService(String serviceName, String ip)
		throws AxisFault
	{
		return null;
	}

	public EndpointReference[] getEPRsForService(String serviceName, String ip)
			throws AxisFault {
		return null;
	}

	public SessionContext<?> getSessionContext(MessageContext messageContext)
	{
		return null;
	}

	public void init(	ConfigurationContext axisConf,
						TransportInDescription transprtIn)
		throws AxisFault
	{
		//do nothing
	}

	public void start() throws AxisFault
	{
		//do nothing
	}

	public void stop() throws AxisFault
	{
		//do nothing
	}

}
