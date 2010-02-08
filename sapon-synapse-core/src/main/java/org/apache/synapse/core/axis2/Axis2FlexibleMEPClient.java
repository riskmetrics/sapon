/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.core.axis2;

import javax.xml.namespace.QName;

import org.apache.axis2.Axis2Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.NhttpConstants;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.util.MessageHelper;

/**
 * This is a simple client that handles both in only and in out
 */
public class Axis2FlexibleMEPClient {

    private static final Log log = LogFactory.getLog(Axis2FlexibleMEPClient.class);

    /**
     * Based on the Axis2 client code. Sends the Axis2 Message context out and returns
     * the Axis2 message context for the response.
     *
     * Here Synapse works as a Client to the service. It would expect 200 ok, 202 ok and
     * 500 internal server error as possible responses.
     *
     * @param endpoint the endpoint being sent to, maybe null
     * @param synapseOutMessageContext the outgoing synapse message
     * @throws AxisFault on errors
     */
    public static void send(	EndpointDefinition endpoint,
    							SynapseMessageContext synapseOutMessageContext )
    	throws AxisFault
    {
        // save the original message context wihout altering it, so we can tie the response
        MessageContext originalInMsgCtx
            = ((Axis2SynapseMessageContext) synapseOutMessageContext).getAxis2MessageContext();

        // create a new MessageContext to be sent out as this should not corrupt the original
        // we need to create the response to the original message later on
        MessageContext axisOutMsgCtx = cloneForSend(originalInMsgCtx,
            (String) synapseOutMessageContext.getProperty(SynapseConstants.PRESERVE_WS_ADDRESSING));

        if (log.isDebugEnabled()) {
            log.debug("Message [Original Request Message ID : "
                    + synapseOutMessageContext.getMessageID() + "]"
                    + " [New Cloned Request Message ID : " + axisOutMsgCtx.getMessageID() + "]");
        }
        // set all the details of the endpoint only to the cloned message context
        // so that we can use the original message context for resending through different endpoints
        if (endpoint != null) {
        	prepMessageContextForEndpoint(axisOutMsgCtx, endpoint);
        } else {
            processHttpGetMethod(originalInMsgCtx, axisOutMsgCtx);
        }

        if (axisOutMsgCtx.isDoingREST()) {
            if (axisOutMsgCtx.getProperty(WSDL2Constants.ATTR_WHTTP_LOCATION) == null
                    && axisOutMsgCtx.getEnvelope().getBody().getFirstElement() != null) {
                axisOutMsgCtx.setProperty(WSDL2Constants.ATTR_WHTTP_LOCATION,
                        axisOutMsgCtx.getEnvelope().getBody().getFirstElement()
                                .getQName().getLocalPart());
            }
        }

        boolean separateListener      = false;
        boolean wsSecurityEnabled     = false;
        String wsSecPolicyKey         = null;
        String inboundWsSecPolicyKey  = null;
        String outboundWsSecPolicyKey = null;
        boolean wsRMEnabled           = false;
        String wsRMPolicyKey          = null;
        boolean wsAddressingEnabled   = false;
        String wsAddressingVersion    = null;

        if (endpoint != null) {
            separateListener       = endpoint.isUseSeparateListener();
            wsSecurityEnabled      = endpoint.isSecurityOn();
            wsSecPolicyKey         = endpoint.getWsSecPolicyKey();
            inboundWsSecPolicyKey  = endpoint.getInboundWsSecPolicyKey();
            outboundWsSecPolicyKey = endpoint.getOutboundWsSecPolicyKey();
            wsRMEnabled            = endpoint.isReliableMessagingOn();
            wsRMPolicyKey          = endpoint.getWsRMPolicyKey();
            wsAddressingEnabled    = endpoint.isAddressingOn() || wsRMEnabled;
            wsAddressingVersion    = endpoint.getAddressingVersion();
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Sending [add = " + wsAddressingEnabled +
                "] [sec = " + wsSecurityEnabled +
                "] [rm = " + wsRMEnabled +
                (endpoint != null ?
                    "] [mtom = " + endpoint.isUseMTOM() +
                    "] [swa = " + endpoint.isUseSwa() +
                    "] [format = " + endpoint.getFormat() +
                    "] [force soap11=" + endpoint.isForceSOAP11() +
                    "] [force soap12=" + endpoint.isForceSOAP12() +
                    "] [pox=" + endpoint.isForcePOX() +
                    "] [get=" + endpoint.isForceGET() +
                    "] [encoding=" + endpoint.getCharSetEncoding() : "") +
                "] [to " + synapseOutMessageContext.getTo() + "]");
        }

        if (wsAddressingEnabled) {
        	if (wsAddressingVersion == null) {
        		//do nothing
        	} else if ( SynapseConstants.ADDRESSING_VERSION_SUBMISSION.equals(wsAddressingVersion)) {
                axisOutMsgCtx.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,
                                          AddressingConstants.Submission.WSA_NAMESPACE);
            } else if (SynapseConstants.ADDRESSING_VERSION_FINAL.equals(wsAddressingVersion)) {
                axisOutMsgCtx.setProperty(AddressingConstants.WS_ADDRESSING_VERSION,
                                          AddressingConstants.Final.WSA_NAMESPACE);
            }
            axisOutMsgCtx.setProperty
                    (AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.FALSE);
        } else {
            axisOutMsgCtx.setProperty
                    (AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
        }

        ConfigurationContext axisCfgCtx = axisOutMsgCtx.getConfigurationContext();
        AxisConfiguration axisCfg       = axisCfgCtx.getAxisConfiguration();

        AxisService anonymousService =
            AnonymousServiceFactory.getAnonymousService(synapseOutMessageContext.getConfiguration(),
            axisCfg, wsAddressingEnabled, wsRMEnabled, wsSecurityEnabled);
        // mark the anon services created to be used in the client side of synapse as hidden
        // from the server side of synapse point of view
        anonymousService.getServiceGroup().addParameter(new Parameter(SynapseConstants.HIDDEN_SERVICE_PARAM, "true"));
        ServiceGroupContext sgc = new ServiceGroupContext(
            axisCfgCtx, anonymousService.getServiceGroup());
        ServiceContext serviceCtx = sgc.getServiceContext(anonymousService);

        boolean outOnlyMessage = "true".equals(synapseOutMessageContext.getProperty(SynapseConstants.OUT_ONLY))
                               || WSDL2Constants.MEP_URI_IN_ONLY.equals(originalInMsgCtx.getOperationContext().getAxisOperation().getMessageExchangePattern());

        // get a reference to the DYNAMIC operation of the Anonymous Axis2 service
        AxisOperation axisAnonymousOperation = anonymousService.getOperation(
            outOnlyMessage ?
                new QName(AnonymousServiceFactory.OUT_ONLY_OPERATION) :
                new QName(AnonymousServiceFactory.OUT_IN_OPERATION));

        Options clientOptions = MessageHelper.cloneOptions(originalInMsgCtx.getOptions());
        clientOptions.setUseSeparateListener(separateListener);

        if (wsRMEnabled && wsRMPolicyKey != null) {
        	clientOptions.setProperty( SynapseConstants.SANDESHA_POLICY,
                                       MessageHelper.getPolicy(synapseOutMessageContext, wsRMPolicyKey));
        }

        if (wsSecurityEnabled) {
            if (wsSecPolicyKey != null) {
                clientOptions.setProperty( SynapseConstants.RAMPART_POLICY,
                                           MessageHelper.getPolicy(synapseOutMessageContext, wsSecPolicyKey));
            } else {
                if (inboundWsSecPolicyKey != null) {
                    clientOptions.setProperty(SynapseConstants.RAMPART_IN_POLICY,
                                              MessageHelper.getPolicy(synapseOutMessageContext, inboundWsSecPolicyKey));
                }
                if (outboundWsSecPolicyKey != null) {
                    clientOptions.setProperty(SynapseConstants.RAMPART_OUT_POLICY,
                                              MessageHelper.getPolicy(synapseOutMessageContext, outboundWsSecPolicyKey));
                }
            }
        }

        OperationClient mepClient = axisAnonymousOperation.createClient(serviceCtx, clientOptions);
        mepClient.addMessageContext(axisOutMsgCtx);
        axisOutMsgCtx.setAxisMessage(
            axisAnonymousOperation.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE));

        // set the SEND_TIMEOUT for transport sender
        if (endpoint != null && endpoint.getTimeoutDuration() > 0) {
            axisOutMsgCtx.setProperty(SynapseConstants.SEND_TIMEOUT, endpoint.getTimeoutDuration());
        }

        if (!outOnlyMessage) {
            // always set a callback as we decide if the send it blocking or non blocking within
            // the MEP client. This does not cause an overhead, as we simply create a 'holder'
            // object with a reference to the outgoing synapse message context
            // synapseOutMessageContext
            AsyncCallback callback = new AsyncCallback(synapseOutMessageContext);
            if (endpoint != null) {
                // set the timeout time and the timeout action to the callback, so that the
                // TimeoutHandler can detect timed out callbacks and take approprite action.
                callback.setTimeOutOn(System.currentTimeMillis() + endpoint.getTimeoutDuration());
                callback.setTimeOutAction(endpoint.getTimeoutAction());
            } else {
                callback.setTimeOutOn(System.currentTimeMillis());
            }
            mepClient.setCallback(callback);
        }

        // with the nio transport, this causes the listener not to write a 202
        // Accepted response, as this implies that Synapse does not yet know if
        // a 202 or 200 response would be written back.
        originalInMsgCtx.getOperationContext().setProperty(
            Axis2Constants.RESPONSE_WRITTEN, "SKIP");

        mepClient.execute(true);
   }

    private static void prepMessageContextForEndpoint(MessageContext msgCtx,
    		                                          EndpointDefinition endpoint)
    	throws AxisFault
    {
    	if (SynapseConstants.FORMAT_POX.equals(endpoint.getFormat())) {
            msgCtx.setDoingREST(true);
            msgCtx.setProperty(Axis2Constants.Configuration.MESSAGE_TYPE,
                    org.apache.axis2.transport.http.HTTPConstants.MEDIA_TYPE_APPLICATION_XML);

        } else if (SynapseConstants.FORMAT_GET.equals(endpoint.getFormat())) {
            msgCtx.setDoingREST(true);
            msgCtx.setProperty(Axis2Constants.Configuration.HTTP_METHOD,
                Axis2Constants.Configuration.HTTP_METHOD_GET);
            msgCtx.setProperty(Axis2Constants.Configuration.MESSAGE_TYPE,
                    org.apache.axis2.transport.http.HTTPConstants.MEDIA_TYPE_X_WWW_FORM);

        } else if (SynapseConstants.FORMAT_SOAP11.equals(endpoint.getFormat())) {
            msgCtx.setDoingREST(false);
            msgCtx.removeProperty(Axis2Constants.Configuration.MESSAGE_TYPE);
            // We need to set this ezplicitly here in case the requset was not a POST
            msgCtx.setProperty(Axis2Constants.Configuration.HTTP_METHOD,
                Axis2Constants.Configuration.HTTP_METHOD_POST);
            if (msgCtx.getSoapAction() == null && msgCtx.getWSAAction() != null) {
                msgCtx.setSoapAction(msgCtx.getWSAAction());
            }
            if(!msgCtx.isSOAP11()) {
                SOAPUtils.convertSOAP12toSOAP11(msgCtx);
            }

        } else if (SynapseConstants.FORMAT_SOAP12.equals(endpoint.getFormat())) {
            msgCtx.setDoingREST(false);
            msgCtx.removeProperty(Axis2Constants.Configuration.MESSAGE_TYPE);
            // We need to set this ezplicitly here in case the requset was not a POST
            msgCtx.setProperty(Axis2Constants.Configuration.HTTP_METHOD,
                Axis2Constants.Configuration.HTTP_METHOD_POST);
            if (msgCtx.getSoapAction() == null && msgCtx.getWSAAction() != null) {
                msgCtx.setSoapAction(msgCtx.getWSAAction());
            }
            if(msgCtx.isSOAP11()) {
                SOAPUtils.convertSOAP11toSOAP12(msgCtx);
            }

        } else if (SynapseConstants.FORMAT_REST.equals(endpoint.getFormat())) {
            msgCtx.removeProperty(Axis2Constants.Configuration.MESSAGE_TYPE);
            msgCtx.setDoingREST(true);
        } else {
            throw new RuntimeException("Unknown endpoint format: "
            							+ endpoint.getFormat());
        }

        if (endpoint.isUseMTOM()) {
            msgCtx.setDoingMTOM(true);
            // fix / workaround for AXIS2-1798
            msgCtx.setProperty(
                    Axis2Constants.Configuration.ENABLE_MTOM,
                    Axis2Constants.VALUE_TRUE);
            msgCtx.setDoingMTOM(true);

        } else if (endpoint.isUseSwa()) {
            msgCtx.setDoingSwA(true);
            // fix / workaround for AXIS2-1798
            msgCtx.setProperty(
                    Axis2Constants.Configuration.ENABLE_SWA,
                    Axis2Constants.VALUE_TRUE);
            msgCtx.setDoingSwA(true);
        }

        if (endpoint.getCharSetEncoding() != null) {
            msgCtx.setProperty(Axis2Constants.Configuration.CHARACTER_SET_ENCODING,
                    endpoint.getCharSetEncoding());
        }

        if (endpoint.getAddress() != null) {
            if (SynapseConstants.FORMAT_REST.equals(endpoint.getFormat()) &&
                msgCtx.getProperty(NhttpConstants.REST_URL_POSTFIX) != null) {
                msgCtx.setTo(
                    new EndpointReference(endpoint.getAddress() +
                    msgCtx.getProperty(NhttpConstants.REST_URL_POSTFIX)
                ));
            } else {
                msgCtx.setTo(new EndpointReference(endpoint.getAddress()));
            }
            msgCtx.setProperty(NhttpConstants.ENDPOINT_PREFIX, endpoint.getAddress());
        }

        if (endpoint.isUseSeparateListener()) {
            msgCtx.getOptions().setUseSeparateListener(true);
        }
    }

    private static MessageContext cloneForSend(MessageContext ori, String preserveAddressing)
            throws AxisFault {

        MessageContext newMC = MessageHelper.clonePartially(ori);

        newMC.setEnvelope(ori.getEnvelope());
        if (preserveAddressing != null && Boolean.parseBoolean(preserveAddressing)) {
            newMC.setMessageID(ori.getMessageID());
        } else {
            MessageHelper.removeAddressingHeaders(newMC);
        }

        newMC.setProperty(MessageContext.TRANSPORT_HEADERS,
        				  ori.getProperty(MessageContext.TRANSPORT_HEADERS));

        return newMC;
    }

    public static void clearSecurityProperties(Options options) {

        Options current = options;
        while (current != null && current.getProperty(SynapseConstants.RAMPART_POLICY) != null) {
             current.setProperty(SynapseConstants.RAMPART_POLICY, null);
             current = current.getParent();
        }
    }

    private static void processHttpGetMethod(MessageContext originalInMsgCtx,
                                             MessageContext axisOutMsgCtx) {

        String httpMethod = (String) originalInMsgCtx.getProperty(
                Axis2Constants.Configuration.HTTP_METHOD);
        if (Axis2Constants.Configuration.HTTP_METHOD_GET.equals(httpMethod)) {
            axisOutMsgCtx.setProperty(
                    Axis2Constants.Configuration.MESSAGE_TYPE,
                    HTTPConstants.MEDIA_TYPE_X_WWW_FORM);
            if (axisOutMsgCtx.getProperty(WSDL2Constants.ATTR_WHTTP_LOCATION) == null
                    && axisOutMsgCtx.getEnvelope().getBody().getFirstElement() != null) {
                axisOutMsgCtx.setProperty(WSDL2Constants.ATTR_WHTTP_LOCATION,
                        axisOutMsgCtx.getEnvelope().getBody().getFirstElement()
                                .getQName().getLocalPart());
            }
        }
    }
}
