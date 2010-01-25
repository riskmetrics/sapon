package org.apache.axis2.context;

import java.io.Externalizable;
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.activation.DataHandler;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.alt.ContextManager;
import org.apache.axis2.alt.ExecutionTracker;
import org.apache.axis2.alt.MessageContextFlags;
import org.apache.axis2.alt.Pausable;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.externalize.SafeSerializable;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.neethi.Policy;

public interface MessageContext
	extends Externalizable, SafeSerializable, MessageContextFlags, Pausable,
			ContextManager, ExecutionTracker, Context<OperationContext>
{
    static final String REMOTE_ADDR = "REMOTE_ADDR";
    static final String TRANSPORT_ADDR = "TRANSPORT_ADDR";
    static final String TRANSPORT_HEADERS = "TRANSPORT_HEADERS";
    static final String TRANSPORT_OUT = "TRANSPORT_OUT";
    static final String TRANSPORT_IN = "TRANSPORT_IN";

    static final String CHARACTER_SET_ENCODING = "CHARACTER_SET_ENCODING";
    static final String UTF_8 = "UTF-8";
    static final String UTF_16 = "utf-16";
    static final String DEFAULT_CHAR_SET_ENCODING = UTF_8;

    static final String TRANSPORT_SUCCEED = "TRANSPORT_SUCCEED";

    /**
     * To invoke fireAndforget method we have to hand over transport sending logic to a thread
     * other wise user has to wait till it get transport response (in the case of HTTP its HTTP
     * 202)
     */
    static final String TRANSPORT_NON_BLOCKING = "transportNonBlocking";

    /**
     * This property allows someone (e.g. RM) to disable an async callback from
     * being invoked if a fault occurs during message transmission.  If this is
     * not set, it can be assumed that the fault will be delivered via
     * Callback.onError(...).
     */
    static final String DISABLE_ASYNC_CALLBACK_ON_TRANSPORT_ERROR =
            "disableTransmissionErrorCallback";

    static ThreadLocal<MessageContext> currentMessageContext
    	= new ThreadLocal<MessageContext>();

	String getLogCorrelationID();

	void setAxisMessage(AxisMessage axisMessage);
	AxisMessage getAxisMessage();
	AxisOperation getAxisOperation();
	AxisService getAxisService();
	AxisServiceGroup getAxisServiceGroup();

	SOAPEnvelope getEnvelope();
	void setEnvelope(SOAPEnvelope envelope) throws AxisFault;

	String getSoapAction();
	void setSoapAction(String soapAction);
	String getWSAAction();
	void setWSAAction(String actionURI);
	String getMessageID();
	void setMessageID(String messageId);
	void setWSAMessageId(String messageId);

	Parameter getModuleParameter(String key, String moduleName, HandlerDescription handler);
	Parameter getParameter(String key);

	Object getLocalProperty(String name, boolean searchOptions);
	boolean isPropertyTrue(String name);
	boolean isPropertyTrue(String name, boolean defaultVal);

	EndpointReference getFaultTo();
	void setFaultTo(EndpointReference reference);
	EndpointReference getFrom();
	void setFrom(EndpointReference reference);
	EndpointReference getReplyTo();
	void setReplyTo(EndpointReference reference);
	EndpointReference getTo();
	void setTo(EndpointReference reference);

	RelatesTo[] getRelationships();
	void setRelationships(RelatesTo[] list);
	RelatesTo getRelatesTo(String type);
	RelatesTo getRelatesTo();
	void addRelatesTo(RelatesTo reference);

	TransportInDescription getTransportIn();
	void setTransportIn(TransportInDescription in);
	TransportOutDescription getTransportOut();
	void setTransportOut(TransportOutDescription out);

	String getIncomingTransportName();
	void setIncomingTransportName(String incomingTransportName);

	long getInboundContentLength() throws IOException;

	Options getOptions();
	void setOptions(Options options);
	void setOptionsExplicit(Options op);

	Policy getEffectivePolicy();

	boolean isEngaged(String moduleName);

	Attachments getAttachments();
	void setAttachments(Attachments attachments);
	DataHandler getAttachment(String contentID);
	void addAttachment(String contentID, DataHandler dataHandler);
	String addAttachment(DataHandler dataHandler);
	void removeAttachment(String contentID);

	boolean containsSelfManagedDataKey(Class<?> clazz, Object key);
	Object getSelfManagedData(Class<?> clazz, Object key);
	void setSelfManagedData(Class<?> clazz, Object key, Object value);
	void setSelfManagedDataMapExplicit(LinkedHashMap<String, Object> map);
	void removeSelfManagedData(Class<?> clazz, Object key);

	void activate(ConfigurationContext cc);
	void activateWithOperationContext(OperationContext operationCtx);

	MessageContext extractCopyMessageContext();

	Exception getFailureReason();
	void setFailureReason(Exception failureReason);

	void checkMustUnderstand() throws AxisFault;
}
