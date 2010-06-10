package org.apache.synapse.core.axis2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.Axis2Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OldMessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.MediatorWorker;
import org.apache.synapse.mediators.base.SequenceMediator;

public class Axis2SynapseMessageContextImpl implements Axis2SynapseMessageContext
{
	public static Axis2SynapseMessageContext newInstance( 	MessageContext axisMsgCtx,
															SynapseConfiguration synCfg,
															SynapseEnvironment synEnv)
	{
		return new Axis2SynapseMessageContextImpl(axisMsgCtx, synCfg, synEnv);
	}

	private static final Log log = LogFactory
			.getLog(Axis2SynapseMessageContextImpl.class);

	private final SynapseConfiguration synCfg;
	private final SynapseEnvironment synEnv;

	/** Synapse Message Context properties */
	private final Map<String, Object> properties = new HashMap<String, Object>();

	/**
	 * Local entries fetched from the configuration or from the registry for the
	 * transactional resource access
	 */
	private final Map<String, Object> localEntries = new HashMap<String, Object>();

	/**
	 * Fault Handler stack which will be popped and called the handleFault in
	 * error states
	 */
	private final Stack<FaultHandler> faultStack = new Stack<FaultHandler>();

	/** The Axis2 MessageContext reference */
	private MessageContext axis2MessageContext;

	/** Attribute of the MC specifying whether this is a response or not */
	private boolean response = false;

	/**
	 * Attribute specifying whether this MC corresponds to fault response or not
	 */
	private boolean faultResponse = false;

	/** The service log for this message */
	private Log serviceLog;

	/**
	 * Constructor for the Axis2MessageContext inside Synapse
	 *
	 * @param axisMsgCtx
	 *            MessageContext representing the relevant Axis MC
	 * @param synCfg
	 *            SynapseConfiguraion describing Synapse
	 * @param synEnv
	 *            SynapseEnvironment describing the environment of Synapse
	 */
	private Axis2SynapseMessageContextImpl(	MessageContext axisMsgCtx,
											SynapseConfiguration synCfg,
											SynapseEnvironment synEnv)
	{
		setAxis2MessageContext(axisMsgCtx);
		this.synCfg = synCfg;
		this.synEnv = synEnv;
	}

	public SynapseConfiguration getConfiguration() {
		return synCfg;
	}

	public SynapseEnvironment getEnvironment() {
		return synEnv;
	}

	public Map<String, Object> getContextEntries() {
		return Collections.unmodifiableMap(localEntries);
	}

	public void setContextEntries(Map<String, Object> entries) {
		localEntries.putAll(entries);
	}

	public Mediator getMainSequence() {
		Object o = localEntries.get(SynapseConstants.MAIN_SEQUENCE_KEY);
		if (o != null && o instanceof Mediator) {
			return (Mediator) o;
		} else {
			Mediator main = getConfiguration().getMainSequence();
			localEntries.put(SynapseConstants.MAIN_SEQUENCE_KEY, main);
			return main;
		}
	}

	public Mediator getFaultSequence() {
		Object o = localEntries.get(SynapseConstants.FAULT_SEQUENCE_KEY);
		if (o != null && o instanceof Mediator) {
			return (Mediator) o;
		} else {
			Mediator fault = getConfiguration().getFaultSequence();
			localEntries.put(SynapseConstants.FAULT_SEQUENCE_KEY, fault);
			return fault;
		}
	}

	public Mediator getSequence(String key) {
		Object o = localEntries.get(key);
		if (o != null && o instanceof Mediator) {
			return (Mediator) o;
		} else {
			Mediator m = getConfiguration().getSequence(key);
			localEntries.put(key, m);
			return m;
		}
	}

	public Endpoint getEndpoint(String key) {
		Object o = localEntries.get(key);
		if (o != null && o instanceof Endpoint) {
			return (Endpoint) o;
		} else {
			Endpoint e = getConfiguration().getEndpoint(key);
			localEntries.put(key, e);
			return e;
		}
	}

	public Object getEntry(String key) {
		Object o = localEntries.get(key);
		if (o != null && o instanceof Entry) {
			return ((Entry) o).getValue();
		} else {
			Object e = getConfiguration().getEntry(key);
			if (e != null) {
				localEntries.put(key, e);
				return e;
			} else {
				getConfiguration().getEntryDefinition(key);
				return getConfiguration().getEntry(key);
			}
		}
	}

	public Object getProperty(String key) {
		if(properties.containsKey(key)) {
			return properties.get(key);
		} else {
			return axis2MessageContext.getProperty(key);
		}
	}

	public void setProperty(String key, Object value) {
		if (value == null) {
			return;
		}

		properties.put(key, value);

		// do not commit response by default in the server process
		// TODO: wtf is this doing here?
		if (SynapseConstants.RESPONSE.equals(key)
				&& getAxis2MessageContext().getOperationContext() != null) {
			getAxis2MessageContext().getOperationContext().setProperty(
					Axis2Constants.RESPONSE_WRITTEN, "SKIP");
		}
	}

	public Set<String> getPropertyKeySet() {
		return properties.keySet();
	}

	// TODO: MessageContext setters need to be converted to use a PropStack
	public EndpointReference getFaultTo() {
		return axis2MessageContext.getFaultTo();
	}

	public void setFaultTo(EndpointReference reference) {
		axis2MessageContext.setFaultTo(reference);
	}

	public EndpointReference getFrom() {
		return axis2MessageContext.getFrom();
	}

	public void setFrom(EndpointReference reference) {
		axis2MessageContext.setFrom(reference);
	}

	public SOAPEnvelope getEnvelope() {
		return axis2MessageContext.getEnvelope();
	}

	public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
		axis2MessageContext.setEnvelope(envelope);
	}

	public String getMessageID() {
		return axis2MessageContext.getMessageID();
	}

	public void setMessageID(String string) {
		axis2MessageContext.setMessageID(string);
	}

	public RelatesTo getRelatesTo() {
		return axis2MessageContext.getRelatesTo();
	}

	public void setRelatesTo(RelatesTo[] reference) {
		axis2MessageContext.setRelationships(reference);
	}

	public EndpointReference getReplyTo() {
		return axis2MessageContext.getReplyTo();
	}

	public void setReplyTo(EndpointReference reference) {
		axis2MessageContext.setReplyTo(reference);
	}

	public EndpointReference getTo() {
		return axis2MessageContext.getTo();
	}

	public void setTo(EndpointReference reference) {
		axis2MessageContext.setTo(reference);
	}

	public void setWSAAction(String actionURI) {
		axis2MessageContext.setWSAAction(actionURI);
	}

	public String getWSAAction() {
		return axis2MessageContext.getWSAAction();
	}

	public void setWSAMessageID(String messageID) {
		axis2MessageContext.setWSAMessageId(messageID);
	}

	public String getWSAMessageID() {
		return axis2MessageContext.getMessageID();
	}

	public String getSoapAction() {
		return axis2MessageContext.getSoapAction();
	}

	public void setSoapAction(String string) {
		axis2MessageContext.setSoapAction(string);
	}

	public boolean isDoingMTOM() {
		return axis2MessageContext.isDoingMTOM();
	}

	public boolean isDoingSWA() {
		return axis2MessageContext.isDoingSwA();
	}

	public void setDoingMTOM(boolean b) {
		axis2MessageContext.setDoingMTOM(b);
	}

	public void setDoingSWA(boolean b) {
		axis2MessageContext.setDoingSwA(b);
	}

	public boolean isDoingPOX() {
		return axis2MessageContext.isDoingREST();
	}

	public void setDoingPOX(boolean b) {
		axis2MessageContext.setDoingREST(b);
	}

	public boolean isDoingGET() {
		return Axis2Constants.Configuration.HTTP_METHOD_GET
				.equals(axis2MessageContext
						.getProperty(Axis2Constants.Configuration.HTTP_METHOD))
				&& axis2MessageContext.isDoingREST();
	}

	public void setDoingGET(boolean b) {
		if (b) {
			axis2MessageContext.setDoingREST(b);
			axis2MessageContext.setProperty(
					Axis2Constants.Configuration.HTTP_METHOD,
					Axis2Constants.Configuration.HTTP_METHOD_GET);
		} else {
			axis2MessageContext
					.removeProperty(Axis2Constants.Configuration.HTTP_METHOD);
		}
	}

	public boolean isSOAP11() {
		return axis2MessageContext.isSOAP11();
	}

	public void setResponse(boolean b) {
		response = b;
		axis2MessageContext
				.setProperty(SynapseConstants.ISRESPONSE_PROPERTY, b);
	}

	public boolean isResponse() {
		Object o = properties.get(SynapseConstants.RESPONSE);
		return o != null && o instanceof String
				&& ((String) o).equalsIgnoreCase("true") || response;
	}

	public void setFaultResponse(boolean b) {
		faultResponse = b;
	}

	public boolean isFaultResponse() {
		return faultResponse;
	}

	public Stack<FaultHandler> getFaultStack() {
		return this.faultStack;
	}

	public void pushFaultHandler(FaultHandler fault) {
		this.faultStack.push(fault);
	}

	/**
	 * Return the service level Log for this message context or null
	 *
	 * @return the service level Log for the message
	 */
	public Log getServiceLog() {
		if (serviceLog != null) {
			return serviceLog;
		} else {
			String serviceName = (String) getProperty(SynapseConstants.PROXY_SERVICE);
			if (serviceName != null
					&& synCfg.getProxyService(serviceName) != null) {
				serviceLog = LogFactory
						.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX
								+ serviceName);
				return serviceLog;
			} else {
				serviceLog = LogFactory
						.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX
								.substring(0,
										SynapseConstants.SERVICE_LOGGER_PREFIX
												.length() - 1));
				return serviceLog;
			}
		}
	}

	/**
	 * Set the service log
	 *
	 * @param serviceLog
	 *            log to be used on a per-service basis
	 */
	public void setServiceLog(Log serviceLog) {
		this.serviceLog = serviceLog;
	}

	public MessageContext getAxis2MessageContext() {
		return axis2MessageContext;
	}

	public void setAxis2MessageContext(MessageContext axisMsgCtx) {
		this.axis2MessageContext = axisMsgCtx;
		Boolean resp = (Boolean) axisMsgCtx
				.getProperty(SynapseConstants.ISRESPONSE_PROPERTY);
		if (resp != null) {
			response = resp;
		}
	}

	public void setPaused(boolean value) {
		axis2MessageContext.setPaused(value);
	}

	public boolean isPaused() {
		return axis2MessageContext.isPaused();
	}

	public boolean isServerSide() {
		return axis2MessageContext.isServerSide();
	}

	public void setServerSide(boolean value) {
		axis2MessageContext.setServerSide(value);
	}

	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();
		String separator = "\n";

		if (getTo() != null) {
			sb.append("To : ").append(getTo().getAddress());
		} else {
			sb.append("To : ");
		}

		if (getFrom() != null) {
			sb.append(separator).append("From : ").append(
					getFrom().getAddress());
		}

		if (getWSAAction() != null) {
			sb.append(separator).append("WSAction : ").append(getWSAAction());
		}

		if (getSoapAction() != null) {
			sb.append(separator).append("SOAPAction : ")
					.append(getSoapAction());
		}

		if (getReplyTo() != null) {
			sb.append(separator).append("ReplyTo : ").append(
					getReplyTo().getAddress());
		}

		if (getMessageID() != null) {
			sb.append(separator).append("MessageID : ").append(getMessageID());
		}

		SOAPHeader soapHeader = getEnvelope().getHeader();
		if (soapHeader != null) {
			sb.append(separator).append("Headers : ");
			for (SOAPHeaderBlock headerBlock: soapHeader.examineAllHeaderBlocks()) {
				sb.append(separator).append("\t").append(
						headerBlock.getLocalName()).append(" : ").append(
						headerBlock.getText());
			}
		}

		SOAPBody soapBody = getEnvelope().getBody();
		if (soapBody != null) {
			sb.append(separator).append("Body : ").append(soapBody.toString());
		}

		return sb.toString();
	}

	public boolean send() {
		log.debug("Injecting Message");
		Mediator mandatorySeq = getConfiguration().getMandatorySequence();
		if (mandatorySeq != null) {
			log.debug("Start mediating with mandatory sequence");

			if (!mandatorySeq.mediate(this)) {
				if (log.isDebugEnabled()) {
					log
							.debug((isResponse() ? "Response" : "Request")
									+ " message for the "
									+ (getProperty(SynapseConstants.PROXY_SERVICE) != null ? "proxy service "
											+ getProperty(SynapseConstants.PROXY_SERVICE)
											: "message mediation")
									+ " dropped in the "
									+ "pre-mediation state by the mandatory sequence : \n"
									+ this);
				}
				return false;
			}
		}

		// if this is not a response to a proxy service
		String proxyName = (String) getProperty(SynapseConstants.PROXY_SERVICE);
		if (proxyName == null || "".equals(proxyName)) {
			if (log.isDebugEnabled()) {
				log.debug("Using Main Sequence for injected message");
			}
			return getMainSequence().mediate(this);
		}

		ProxyService proxyService = getConfiguration().getProxyService(
				proxyName);
		if (proxyService != null) {

			Mediator outSequence = getProxyOutSequence(proxyService);
			if (outSequence != null) {
				outSequence.mediate(this);
			} else {
				if (log.isDebugEnabled()) {
					log
							.debug(proxyService
									+ " does not specifies an out-sequence - sending the response back");
				}
				Axis2Sender.sendBack(this);
			}
		}
		return true;
	}

	public void sendAsync(SequenceMediator seq) {
		if (log.isDebugEnabled()) {
			log
					.debug("Injecting MessageContext for asynchronous mediation using the : "
							+ (seq.getName() == null ? "Anonymous" : seq
									.getName()) + " Sequence");
		}
		getEnvironment().getExecutorService().execute(
				new MediatorWorker(seq, this));
	}

	/**
	 * Helper method to determine out sequence of the proxy service
	 *
	 * @param proxyService
	 *            Proxy Service
	 * @return Out Sequence of the given proxy service, if there are any,
	 *         otherwise null
	 */
	private Mediator getProxyOutSequence(ProxyService proxyService) {
		// TODO is it meaningful to move this method into proxy service or
		// TODO a class that Strategically detects out sequence ?
		String sequenceName = proxyService.getTargetOutSequence();
		if (sequenceName != null && !"".equals(sequenceName)) {
			Mediator outSequence = getSequence(sequenceName);
			if (outSequence != null) {
				if (log.isDebugEnabled()) {
					log
							.debug("Using the sequence named "
									+ sequenceName
									+ " for the outgoing message mediation of the proxy service "
									+ proxyService);
				}
				return outSequence;
			} else {
				log.error("Unable to find the out-sequence "
						+ "specified by the name " + sequenceName);
				throw new SynapseException("Unable to find the "
						+ "out-sequence specified by the name " + sequenceName);
			}
		} else {
			Mediator outSequence = proxyService.getTargetInLineOutSequence();
			if (outSequence != null) {
				if (log.isDebugEnabled()) {
					log
							.debug("Using the anonymous out-sequence specified in the proxy service "
									+ proxyService
									+ " for outgoing message mediation");
				}
				return outSequence;
			}
		}
		return null;
	}

}
