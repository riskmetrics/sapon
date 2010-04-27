package org.apache.synapse.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPFaultNode;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPFaultRole;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPFaultValue;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.Axis2Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OldMessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;
import org.apache.synapse.mediators.eip.EIPConstants;

/**
 *
 */
public class MessageHelper {


    private static Log log = LogFactory.getLog(MessageHelper.class);

    /**
     * This method will simulate cloning the message context and creating an exact copy of the
     * passed message. One should use this method with care; that is because, inside the new MC,
     * most of the attributes of the MC like opCtx and so on are still kept as references inside
     * the axis2 MessageContext for performance improvements. (Note: U dont have to worrie
     * about the SOAPEnvelope, it is a cloned copy and not a reference from any other MC)
     *
     * @param synCtx - this will be cloned
     * @return cloned Synapse MessageContext
     * @throws AxisFault if there is a failure in creating the new Synapse MC or in a failure in
     *          clonning the underlying axis2 MessageContext
     *
     * @see MessageHelper#cloneAxis2MessageContext
     */
    public static SynapseMessageContext cloneMessageContext(SynapseMessageContext synCtx) throws AxisFault {

        // creates the new MessageContext and clone the internal axis2 MessageContext
        // inside the synapse message context and place that in the new one
        SynapseMessageContext newCtx = synCtx.getEnvironment().createMessageContext();

        if(	synCtx instanceof Axis2SynapseMessageContext &&
        	newCtx instanceof Axis2SynapseMessageContext )
        {
        	Axis2SynapseMessageContext a2SMC = (Axis2SynapseMessageContext) synCtx;
        	Axis2SynapseMessageContext newA2SMC = (Axis2SynapseMessageContext) newCtx;
        	newA2SMC.setAxis2MessageContext(
        			cloneAxis2MessageContext(a2SMC.getAxis2MessageContext()));
        }

        newCtx.setContextEntries(synCtx.getContextEntries());

        // set the parent correlation details to the cloned MC -
        //                              for the use of aggregation like tasks
        newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION, synCtx.getMessageID());

        // copying the core parameters of the synapse MC
        newCtx.setTo(synCtx.getTo());
        newCtx.setReplyTo(synCtx.getReplyTo());
        newCtx.setSoapAction(synCtx.getSoapAction());
        newCtx.setWSAAction(synCtx.getWSAAction());
        newCtx.setResponse(synCtx.isResponse());

        // copy all the synapse level properties to the newCtx
        for (String s: synCtx.getPropertyKeySet()) {
        	newCtx.setProperty(s, synCtx.getProperty(s));
        }

        // Make deep copy of fault stack so that parent will not be lost it's fault stack
        Stack<FaultHandler> faultStack = synCtx.getFaultStack();
        if (!faultStack.isEmpty()) {

            List<FaultHandler> newFaultStack = new ArrayList<FaultHandler>();
            newFaultStack.addAll(faultStack);

            for (FaultHandler faultHandler : newFaultStack) {
                if (faultHandler != null) {
                    newCtx.pushFaultHandler(faultHandler);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.info("Parent's Fault Stack : " + faultStack + " : Child's Fault Stack :" + newCtx.getFaultStack());
        }

        return newCtx;
    }

    /**
     * This method will simulate cloning the message context and creating an exact copy of the
     * passed message. One should use this method with care; that is because, inside the new MC,
     * most of the attributes of the MC like opCtx and so on are still kept as references. Otherwise
     * there will be perf issues. But ..... this may reveal in some conflicts in the cloned message
     * if you try to do advanced mediations with the cloned message, in which case you should
     * mannually get a clone of the changing part of the MC and set that cloned part to your MC.
     * Changing the MC after doing that will solve most of the issues. (Note: U dont have to worrie
     * about the SOAPEnvelope, it is a cloned copy and not a reference from any other MC)
     *
     * @param mc - this will be cloned for getting an exact copy
     * @return cloned MessageContext from the given mc
     * @throws AxisFault if there is a failure in copying the certain attributes of the
     *          provided message context
     */
    public static MessageContext cloneAxis2MessageContext(MessageContext mc)
    	throws AxisFault
    {
        MessageContext newMC = clonePartially(mc);
        newMC.setEnvelope(cloneSOAPEnvelope(mc.getEnvelope()));
        newMC.setOptions(cloneOptions(mc.getOptions()));

        newMC.setServiceContext(mc.getServiceContext());
        newMC.setOperationContext(mc.getOperationContext());
        newMC.setAxisMessage(mc.getAxisMessage());
        if (newMC.getAxisMessage() != null) {
            newMC.getAxisMessage().setParent(mc.getAxisOperation());
        }
        newMC.setAxisService(mc.getAxisService());

        // copying transport related parts from the original
        newMC.setTransportIn(mc.getTransportIn());
        newMC.setTransportOut(mc.getTransportOut());
        newMC.setProperty(Axis2Constants.OUT_TRANSPORT_INFO,
            mc.getProperty(Axis2Constants.OUT_TRANSPORT_INFO));

        newMC.setProperty(MessageContext.TRANSPORT_HEADERS, getClonedTransportHeaders(mc));

        return newMC;
    }

    public static Map<String, Object> getClonedTransportHeaders(MessageContext msgCtx) {

        Map<String, Object> headers = (Map<String, Object>) msgCtx.
            getProperty(MessageContext.TRANSPORT_HEADERS);
        Map<String, Object> clonedHeaders = new HashMap<String, Object>();

        if (headers != null) {
        	clonedHeaders.putAll(headers);
        }

        return clonedHeaders;
    }

    public static MessageContext clonePartially(MessageContext ori)
    	throws AxisFault
    {

        MessageContext newMC = new OldMessageContext();

        // do not copy options from the original
        newMC.setConfigurationContext(ori.getConfigurationContext());
        newMC.setMessageID(UUIDGenerator.getUUID());
        newMC.setTo(ori.getTo());
        newMC.setSoapAction(ori.getSoapAction());

        newMC.setProperty(Axis2Constants.Configuration.CHARACTER_SET_ENCODING,
                ori.getProperty(Axis2Constants.Configuration.CHARACTER_SET_ENCODING));
        newMC.setProperty(Axis2Constants.Configuration.ENABLE_MTOM,
                ori.getProperty(Axis2Constants.Configuration.ENABLE_MTOM));
        newMC.setProperty(Axis2Constants.Configuration.ENABLE_SWA,
                ori.getProperty(Axis2Constants.Configuration.ENABLE_SWA));
        newMC.setProperty(Axis2Constants.Configuration.HTTP_METHOD,
            ori.getProperty(Axis2Constants.Configuration.HTTP_METHOD));
        //coping the Message type from req to res to get the message formatters working correctly.
        newMC.setProperty(Axis2Constants.Configuration.MESSAGE_TYPE,
                ori.getProperty(Axis2Constants.Configuration.MESSAGE_TYPE));

        newMC.setDoingREST(ori.isDoingREST());
        newMC.setDoingMTOM(ori.isDoingMTOM());
        newMC.setDoingSwA(ori.isDoingSwA());

        // if the original request carries any attachments, copy them to the 
        // clone as well.  TODO: does it actually matter if the soap part is 
        // included?  for now, let's pretend it doesn't.
        newMC.setAttachments(ori.getAttachments());

        for(final String key: ori.getPropertyNames()) {
            if (key != null) {
                // In a clustered environment, all the properties that need to be relpicated,
                // are replicated explicitly  by the corresponding Mediators (Ex: throttle,
                // cache), and therefore we should avoid any implicit replication
                newMC.setNonReplicableProperty(key, ori.getPropertyNonReplicable(key));
            }
        }

        newMC.setServerSide(false);

        return newMC;
    }

    /**
     * This method will clone the provided SOAPEnvelope and returns the cloned envelope
     * as an exact copy of the provided envelope
     *
     * @param envelope - this will be cloned to get the new envelope
     * @return cloned SOAPEnvelope from the provided one
     */
    public static SOAPEnvelope cloneSOAPEnvelope(SOAPEnvelope envelope) {
        SOAPEnvelope newEnvelope;
        if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI
            .equals(envelope.getBody().getNamespace().getNamespaceURI())) {
            newEnvelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        } else {
            newEnvelope = OMAbstractFactory.getSOAP12Factory().getDefaultEnvelope();
        }

        if (envelope.getHeader() != null) {
            for(SOAPHeaderBlock headerBlock: envelope.getHeader().examineAllHeaderBlocks()) {
                newEnvelope.getHeader().addChild(headerBlock.cloneOMElement());
            }
        }

        if (envelope.getBody() != null) {
            // treat the SOAPFault cloning as a special case otherwise a cloning OMElement as the
            // fault would lead to class cast exceptions if accessed through the getFault method
            if (envelope.getBody().hasFault()) {
                SOAPFault fault = envelope.getBody().getFault();
                newEnvelope.getBody().addFault(cloneSOAPFault(fault));
            } else {
                for(OMNode node: envelope.getBody().cloneOMElement().getChildren()) {
                    newEnvelope.getBody().addChild(node);
                }
            }
        }

        return newEnvelope;
    }

    /**
     * Clones the given {@link org.apache.axis2.client.Options} object. This is not a deep copy
     * because this will be called for each and every message going out from synapse. The parent
     * of the cloning options object is kept as a reference.
     *
     * @param options clonning object
     * @return clonned Options object
     */
    public static Options cloneOptions(Options options) {

        // create new options object and set the parent
        Options clonedOptions = new Options(options.getParent());

        // copy general options
        clonedOptions.setCallTransportCleanup(options.isCallTransportCleanup());
        clonedOptions.setExceptionToBeThrownOnSOAPFault(options.isExceptionToBeThrownOnSOAPFault());
        clonedOptions.setManageSession(options.isManageSession());
        clonedOptions.setSoapVersionURI(options.getSoapVersionURI());
        clonedOptions.setTimeOutInMilliSeconds(options.getTimeOutInMilliSeconds());
        clonedOptions.setUseSeparateListener(options.isUseSeparateListener());

        // copy transport related options
        clonedOptions.setListener(options.getListener());
        clonedOptions.setTransportIn(options.getTransportIn());
        clonedOptions.setTransportInProtocol(options.getTransportInProtocol());
        clonedOptions.setTransportOut(clonedOptions.getTransportOut());

        // copy username and password options
        clonedOptions.setUserName(options.getUserName());
        clonedOptions.setPassword(options.getPassword());

        // cloen the property set of the current options object
        for (Object o : options.getProperties().keySet()) {
            String key = (String) o;
            clonedOptions.setProperty(key, options.getProperty(key));
        }

        return clonedOptions;
    }

    /**
     * Removes Submission and Final WS-Addressing headers and return the SOAPEnvelope from the given
     * message context
     *
     * @param axisMsgCtx the Axis2 Message context
     * @return the resulting SOAPEnvelope
     */
    public static SOAPEnvelope removeAddressingHeaders(MessageContext axisMsgCtx) {

        SOAPEnvelope env = axisMsgCtx.getEnvelope();
        SOAPHeader soapHeader = env.getHeader();
        List<SOAPHeaderBlock> addressingHeaders;

        if (soapHeader != null) {
            addressingHeaders =
                soapHeader.getHeaderBlocksWithNSURI(AddressingConstants.Submission.WSA_NAMESPACE);

            if (addressingHeaders != null && addressingHeaders.size() != 0) {
                detachAddressingInformation(addressingHeaders);

            } else {
                addressingHeaders =
                    soapHeader.getHeaderBlocksWithNSURI(AddressingConstants.Final.WSA_NAMESPACE);
                if (addressingHeaders != null && addressingHeaders.size() != 0) {
                    detachAddressingInformation(addressingHeaders);
                }
            }
        }
        return env;
    }

    /**
     * Remove WS-A headers
     *
     * @param headerInformation headers to be removed
     */
    private static void detachAddressingInformation(List<SOAPHeaderBlock> headerInformation) {
        for (Object o : headerInformation) {
            if (o instanceof SOAPHeaderBlock) {
                SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) o;
                headerBlock.detach();
            } else if (o instanceof OMElement) {
                // work around for a known addressing bug which sends non SOAPHeaderBlock objects
                OMElement om = (OMElement) o;
                OMNamespace ns = om.getNamespace();
                if (ns != null && (
                    AddressingConstants.Submission.WSA_NAMESPACE.equals(ns.getNamespaceURI()) ||
                        AddressingConstants.Final.WSA_NAMESPACE.equals(ns.getNamespaceURI()))) {
                    om.detach();
                }
            }
        }
    }

    /**
     * Get the Policy object for the given name from the Synapse configuration at runtime
     * @param synCtx the current synapse configuration to get to the synapse configuration
     * @param propertyKey the name of the property which holds the Policy required
     * @return the Policy object with the given name, from the configuration
     */
    public static Policy getPolicy(org.apache.synapse.SynapseMessageContext synCtx, String propertyKey) {
        Object property = synCtx.getEntry(propertyKey);
        if (property != null && property instanceof OMElement) {
            return PolicyEngine.getPolicy((OMElement) property);
        } else {
            handleException("Cannot locate policy from the property : " + propertyKey);
        }
        return null;
    }

    /**
     * Clones the SOAPFault, fault cloning is not the same as cloning the OMElement because if the
     * Fault is accessed through the SOAPEnvelope.getBody().getFault() method it will lead to a
     * class cast because the cloned element is just an OMElement but not a Fault.
     *
     * @param fault that needs to be cloned
     * @return the cloned fault
     */
    public static SOAPFault cloneSOAPFault(SOAPFault fault) {

        SOAPFactory fac;
        int soapVersion;
        final int SOAP_11 = 1;
        final int SOAP_12 = 2;
        if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI
                .equals(fault.getNamespace().getNamespaceURI())) {
            fac = OMAbstractFactory.getSOAP11Factory();
            soapVersion = SOAP_11;
        } else {
            fac = OMAbstractFactory.getSOAP12Factory();
            soapVersion = SOAP_12;
        }
        SOAPFault newFault = fac.createSOAPFault();

        SOAPFaultCode code = fac.createSOAPFaultCode();
        SOAPFaultReason reason = fac.createSOAPFaultReason();

        switch (soapVersion) {
            case SOAP_11:
                code.setText(fault.getCode().getTextAsQName());
                reason.setText(fault.getReason().getText());
                break;
            case SOAP_12:
                SOAPFaultValue value = fac.createSOAPFaultValue(code);
                value.setText(fault.getCode().getTextAsQName());
                for (Object obj : fault.getReason().getAllSoapTexts()) {
                    SOAPFaultText text = fac.createSOAPFaultText();
                    text.setText(((SOAPFaultText) obj).getText());
                    reason.addSOAPText(text);
                }
                break;
        }

        newFault.setCode(code);
        newFault.setReason(reason);

        if (fault.getNode() != null) {
            SOAPFaultNode soapfaultNode = fac.createSOAPFaultNode();
            soapfaultNode.setNodeValue(fault.getNode().getNodeValue());
            newFault.setNode(soapfaultNode);
        }

        if (fault.getRole() != null) {
            SOAPFaultRole soapFaultRole = fac.createSOAPFaultRole();
            soapFaultRole.setRoleValue(fault.getRole().getRoleValue());
            newFault.setRole(soapFaultRole);
        }

        if (fault.getDetail() != null) {
            SOAPFaultDetail soapFaultDetail = fac.createSOAPFaultDetail();
            for (OMElement elem: fault.getDetail().getAllDetailEntries()) {
                soapFaultDetail.addDetailEntry(elem.cloneOMElement());
            }
            newFault.setDetail(soapFaultDetail);
        }

        return newFault;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
