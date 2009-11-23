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

package org.apache.synapse.mediators.transform;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
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
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

/**
 * This transforms the current message instance into a SOAP Fault message. The
 * SOAP version for the fault message could be explicitly specified. Else if the
 * original message was SOAP 1.1 the fault will also be SOAP 1.1 else, SOAP 1.2
 *
 * This class exposes methods to set SOAP 1.1 and 1.2 fault elements and uses
 * these as required.
 *
 * Directs the fault messages' "To" EPR to the "FaultTo" or the "ReplyTo" or to
 * null of the original SOAP message
 */
public class FaultMediator extends AbstractMediator {

    public static final String WSA_ACTION = "Action";
    /** Make a SOAP 1.1 fault */
    public static final int SOAP11 = 1;
    /** Make a SOAP 1.2 fault */
    public static final int SOAP12 = 2;
    /** Make a POX fault */
    public static final int POX = 3;
    /** Holds the SOAP version to be used to make the fault, if specified */
    private int soapVersion;
    /** Whether to mark the created fault as a response or not */
    private boolean markAsResponse = true;
    /** Whether it is required to serialize the response attribute or not */
    private boolean serializeResponse = false;

    // -- fault elements --
    /** The fault code QName to be used */
    private QName faultCodeValue = null;
    /** An XPath expression that will give the fault code QName at runtime */
    private SynapseXPath faultCodeExpr = null;
    /** The fault reason to be used */
    private String faultReasonValue = null;
    /** An XPath expression that will give the fault reason string at runtime */
    private SynapseXPath faultReasonExpr = null;
    /** The fault node URI to be used */
    private URI faultNode = null;
    /** The fault role URI to be used - if applicable */
    private URI faultRole = null;
    /** The fault detail to be used */
    private String faultDetail = null;
    /** An XPath expression that will give the fault code QName at runtime */
    private SynapseXPath faultDetailExpr = null;
    /** array of fault detail elements */
    private final List<OMElement> faultDetailElements = new ArrayList<OMElement>();

    public boolean mediate(SynapseMessageContext synCtx) {

        if (log.isDebugEnabled()) {
            log.debug("Start : Fault mediator");

            if (log.isTraceEnabled()) {
                log.trace("Message : " + synCtx.getEnvelope());
            }
        }

        switch (soapVersion) {
            case SOAP11:
                makeSOAPFault(synCtx, SOAP11);
                break;
            case SOAP12:
                makeSOAPFault(synCtx, SOAP12);
                break;
            case POX:
                makePOXFault(synCtx);
                break;

            default : {
                // if this is a POX or REST message then make a POX fault
                if (synCtx.isDoingPOX() || synCtx.isDoingGET()) {
                    makePOXFault(synCtx);
                } else {

                    // determine from current message's SOAP envelope namespace
                    SOAPEnvelope envelop = synCtx.getEnvelope();
                    if (envelop != null) {

                        if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(
                            envelop.getNamespace().getNamespaceURI())) {

                            soapVersion = SOAP12;
                            makeSOAPFault(synCtx, SOAP12);

                        } else {
                            soapVersion = SOAP11;
                            makeSOAPFault(synCtx, SOAP11);
                        }

                    } else {
                        // default to SOAP 11
                        makeSOAPFault(synCtx, SOAP11);
                    }
                }
            }
        }

        // if the message has to be marked as a response mark it as response
        if (markAsResponse) {
            synCtx.setResponse(true);
            synCtx.setTo(synCtx.getReplyTo());
        }

        return true;
    }

    private void makePOXFault(SynapseMessageContext synCtx) {

        OMFactory fac = synCtx.getEnvelope().getOMFactory();
        OMElement faultPayload = fac.createOMElement(new QName("Exception"));

        if (faultDetail != null) {

            if (log.isDebugEnabled()) {
                log.debug("Setting the fault detail : "
                    + faultDetail + " as the POX Fault");
            }

            faultPayload.setText(faultDetail);

        } else if (faultDetailExpr != null) {

            String faultDetail = faultDetailExpr.stringValueOf(synCtx);

            if (log.isDebugEnabled()) {
                log.debug("Setting the fault detail : "
                        + faultDetail + " as the POX Fault");
            }

            faultPayload.setText(faultDetail);

        } else if (faultReasonValue != null) {

            if (log.isDebugEnabled()) {
                log.debug("Setting the fault reason : "
                    + faultReasonValue + " as the POX Fault");
            }

            faultPayload.setText(faultReasonValue);

        } else if (faultReasonExpr != null) {

            String faultReason = faultReasonExpr.stringValueOf(synCtx);
            faultPayload.setText(faultReason);

            if (log.isDebugEnabled()) {
                log.debug("Setting the fault reason : "
                    + faultReason + " as the POX Fault");
            }
        }

        SOAPBody body = synCtx.getEnvelope().getBody();
        if (body != null) {

            if (body.getFirstElement() != null) {
                body.getFirstElement().detach();
            }

            synCtx.setFaultResponse(true);
            ((Axis2SynapseMessageContext) synCtx).getAxis2MessageContext().setProcessingFault(true);

            if (log.isDebugEnabled()) {
                String msg = "Original SOAP Message : " + synCtx.getEnvelope().toString() +
                    "POXFault Message created : " + faultPayload.toString();
                log.trace(msg);
                if (log.isTraceEnabled()) {
                    log.trace(msg);
                }
            }

            body.addChild(faultPayload);
        }
    }

    /**
     * Actual transformation of the current message into a fault message
     * @param synCtx the current message context
     * @param soapVersion SOAP version of the resulting fault desired
     * @param synLog the Synapse log to use
     */
    private void makeSOAPFault(SynapseMessageContext synCtx, int soapVersion)
    {
        if (log.isDebugEnabled()) {
            log.debug("Creating a SOAP "
                    + (soapVersion == SOAP11 ? "1.1" : "1.2") + " fault");
        }

        // get the correct SOAP factory to be used
        SOAPFactory factory = (soapVersion == SOAP11 ?
                OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory());

        // create the SOAP fault document and envelope
        OMDocument soapFaultDocument = factory.createOMDocument();
        SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
        soapFaultDocument.addChild(faultEnvelope);

        // create the fault element  if it is need
        SOAPFault fault = faultEnvelope.getBody().getFault();
        if(fault == null){
            fault = factory.createSOAPFault();
        }

        // populate it
        setFaultCode(synCtx, factory, fault);
        setFaultResaon(synCtx, factory, fault);
        setFaultNode(factory, fault);
        setFaultRole(factory, fault);
        setFaultDetail(synCtx, factory, fault);

        // set the all headers of original SOAP Envelope to the Fault Envelope
        if (synCtx.getEnvelope() != null) {
            SOAPHeader soapHeader = synCtx.getEnvelope().getHeader();
            if (soapHeader != null) {
                for(SOAPHeaderBlock header: soapHeader.examineAllHeaderBlocks()) {
                	faultEnvelope.getHeader().addChild(header);
                }
            }
        }

        if (log.isDebugEnabled()) {
            String msg =
                "Original SOAP Message : " + synCtx.getEnvelope().toString() +
                "Fault Message created : " + faultEnvelope.toString();
            if (log.isTraceEnabled()) {
                log.trace(msg);
            }
            if (log.isTraceEnabled()) {
                log.trace(msg);
            }
        }

        // overwrite current message envelope with new fault envelope
        try {
            synCtx.setEnvelope(faultEnvelope);
        } catch (AxisFault af) {
            handleException("Error replacing current SOAP envelope " +
                    "with the fault envelope", af, synCtx);
        }

        if (synCtx.getFaultTo() != null) {
            synCtx.setTo(synCtx.getFaultTo());
        } else if (synCtx.getReplyTo() != null) {
            synCtx.setTo(synCtx.getReplyTo());
        } else {
            synCtx.setTo(null);
        }

        // set original messageID as relatesTo
        if(synCtx.getMessageID() != null) {
            RelatesTo relatesTo = new RelatesTo(synCtx.getMessageID());
            synCtx.setRelatesTo(new RelatesTo[] { relatesTo });
        }

        log.debug("End : Fault mediator");
    }

    private void setFaultCode(SynapseMessageContext synCtx, SOAPFactory factory, SOAPFault fault) {

        QName fault_code = null;

        if (faultCodeValue == null && faultCodeExpr == null) {
            handleException("A valid fault code QName value or expression is required", synCtx);
        } else if (faultCodeValue != null) {
            fault_code = faultCodeValue;
        } else {
            fault_code = QName.valueOf(faultCodeExpr.stringValueOf(synCtx));
        }

        SOAPFaultCode code = factory.createSOAPFaultCode();
        switch(soapVersion){
            case SOAP11:
                code.setText(fault_code);
                break;
            case SOAP12:
                SOAPFaultValue value = factory.createSOAPFaultValue(code);
                value.setText(fault_code);
                break;
        }
        fault.setCode(code);
    }

    private void setFaultResaon(SynapseMessageContext synCtx, SOAPFactory factory, SOAPFault fault) {
        String reasonString = null;

        if (faultReasonValue == null && faultReasonExpr == null) {
            handleException("A valid fault reason value or expression is required", synCtx);
        } else if (faultReasonValue != null) {
            reasonString = faultReasonValue;
        } else {
            reasonString = faultReasonExpr.stringValueOf(synCtx);
        }

        SOAPFaultReason reason = factory.createSOAPFaultReason();
        switch(soapVersion) {
            case SOAP11:
                reason.setText(reasonString);
                break;
            case SOAP12:
                SOAPFaultText text = factory.createSOAPFaultText();
                text.setText(reasonString);
                reason.addSOAPText(text);
                break;
        }
        fault.setReason(reason);
    }

    private void setFaultNode(SOAPFactory factory, SOAPFault fault) {
        if (faultNode != null) {
            SOAPFaultNode soapfaultNode = factory.createSOAPFaultNode();
            soapfaultNode.setNodeValue(faultNode.toString());
            fault.setNode(soapfaultNode);
        }
    }

    private void setFaultRole(SOAPFactory factory, SOAPFault fault) {
        if (faultRole != null) {
            SOAPFaultRole soapFaultRole = factory.createSOAPFaultRole();
            soapFaultRole.setRoleValue(faultRole.toString());
            fault.setRole(soapFaultRole);
        }
    }

    private void setFaultDetail(SynapseMessageContext synCtx, SOAPFactory factory, SOAPFault fault) {
        if (faultDetail != null) {
            SOAPFaultDetail soapFaultDetail = factory.createSOAPFaultDetail();
            soapFaultDetail.setText(faultDetail);
            fault.setDetail(soapFaultDetail);
        } else if (faultDetailExpr != null) {
            SOAPFaultDetail soapFaultDetail = factory.createSOAPFaultDetail();
            soapFaultDetail.setText(faultDetailExpr.stringValueOf(synCtx));
            fault.setDetail(soapFaultDetail);
        } else if (!faultDetailElements.isEmpty()) {
            SOAPFaultDetail soapFaultDetail = factory.createSOAPFaultDetail();
            for (OMElement faultDetailElement : faultDetailElements) {
                soapFaultDetail.addChild(faultDetailElement.cloneOMElement());
            }
            fault.setDetail(soapFaultDetail);
        } else if (fault.getDetail() != null) {
            // work around for a rampart issue in the following thread
            // http://www.nabble.com/Access-to-validation-error-message-tf4498668.html#a13284520
            fault.getDetail().detach();
        }
    }

    public int getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion(int soapVersion) {
        this.soapVersion = soapVersion;
    }

    public boolean isMarkAsResponse() {
        return markAsResponse;
    }

    public void setMarkAsResponse(boolean markAsResponse) {
        this.markAsResponse = markAsResponse;
    }

    public boolean isSerializeResponse() {
        return serializeResponse;
    }

    public void setSerializeResponse(boolean serializeResponse) {
        this.serializeResponse = serializeResponse;
    }

    public QName getFaultCodeValue() {
        return faultCodeValue;
    }

    public void setFaultCodeValue(QName faultCodeValue) {

        if (soapVersion == SOAP11) {
            this.faultCodeValue = faultCodeValue;

        } else if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(
                faultCodeValue.getNamespaceURI()) &&
                (SOAP12Constants.FAULT_CODE_DATA_ENCODING_UNKNOWN.equals(
                        faultCodeValue.getLocalPart()) ||
                        SOAP12Constants.FAULT_CODE_MUST_UNDERSTAND.equals(
                                faultCodeValue.getLocalPart()) ||
                        SOAP12Constants.FAULT_CODE_RECEIVER.equals(
                                faultCodeValue.getLocalPart()) ||
                        SOAP12Constants.FAULT_CODE_SENDER.equals(
                                faultCodeValue.getLocalPart()) ||
                        SOAP12Constants.FAULT_CODE_VERSION_MISMATCH.equals(
                                faultCodeValue.getLocalPart())) ) {

            this.faultCodeValue = faultCodeValue;

        } else {
            handleException("Invalid Fault code value for a SOAP 1.2 fault : " + faultCodeValue);
        }
    }

    public SynapseXPath getFaultCodeExpr() {
        return faultCodeExpr;
    }

    public void setFaultCodeExpr(SynapseXPath faultCodeExpr) {
        this.faultCodeExpr = faultCodeExpr;
    }

    public String getFaultReasonValue() {
        return faultReasonValue;
    }

    public void setFaultReasonValue(String faultReasonValue) {
        this.faultReasonValue = faultReasonValue;
    }

    public SynapseXPath getFaultReasonExpr() {
        return faultReasonExpr;
    }

    public void setFaultReasonExpr(SynapseXPath faultReasonExpr) {
        this.faultReasonExpr = faultReasonExpr;
    }

    public URI getFaultNode() {
        return faultNode;
    }

    public void setFaultNode(URI faultNode) {
        if (soapVersion == SOAP11) {
            handleException("A fault node does not apply to a SOAP 1.1 fault");
        }
        this.faultNode = faultNode;
    }

    public URI getFaultRole() {
        return faultRole;
    }

    public void setFaultRole(URI faultRole) {
        this.faultRole = faultRole;
    }

    public String getFaultDetail() {
        return faultDetail;
    }

    public void setFaultDetail(String faultDetail) {
        this.faultDetail = faultDetail;
    }

    public SynapseXPath getFaultDetailExpr() {
        return faultDetailExpr;
    }

    public void setFaultDetailExpr(SynapseXPath faultDetailExpr) {
        this.faultDetailExpr = faultDetailExpr;
    }

    public List<OMElement> getFaultDetailElements() {
        return faultDetailElements;
    }

    public void addFaultDetailElement(OMElement element) {
        faultDetailElements.add(element);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
