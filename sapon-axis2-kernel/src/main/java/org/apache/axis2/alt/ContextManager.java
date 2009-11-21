package org.apache.axis2.alt;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.context.SessionContext;

public interface ContextManager {

	MessageContext getMessageContext();
	void setMessageContext(MessageContext messageContext);

	SessionContext<?> getSessionContext();
	void setSessionContext(SessionContext<?> sessionContext);

	OperationContext getOperationContext();
	void setOperationContext(OperationContext context);

	ServiceContext getServiceContext();
	void setServiceContext(ServiceContext context);
	String getServiceContextID();
	void setServiceContextID(String serviceContextID);

	ServiceGroupContext getServiceGroupContext();
	void setServiceGroupContext(ServiceGroupContext serviceGroupContext);
	String getServiceGroupContextId();
	void setServiceGroupContextId(String serviceGroupContextId);

	ConfigurationContext getConfigurationContext();
	void setConfigurationContext(ConfigurationContext context);

	ConfigurationContext getRootContext();
}
