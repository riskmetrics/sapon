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

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPFaultValue;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

public class SOAPUtils {

    private static final Log log = LogFactory.getLog(SOAPUtils.class);

    /**
     * Converts the SOAP version of the message context.  Creates a new
     * envelope of the given SOAP version, copy headers and bodies from the old
     * envelope and sets the new envelope to the same message context.
     *
     * @param axisOutMsgCtx  messageContext where version conversion is done
     * @param soapVersionURI either org.apache.axis2.namespace.Constants.URI_SOAP12_ENV
     * 						 or org.apache.axis2.namespace.Constants.URI_SOAP11_ENV
     * @throws AxisFault in case of an error in conversion
     */
    public static void convertSoapVersion(	MessageContext axisOutMsgCtx,
    										String soapVersionURI )
    	throws AxisFault
    {
        if (org.apache.axis2.namespace.Constants.URI_SOAP12_ENV.equals(soapVersionURI)) {
            convertSOAP11toSOAP12(axisOutMsgCtx);
        } else if (org.apache.axis2.namespace.Constants.URI_SOAP11_ENV.equals(soapVersionURI)) {
            convertSOAP12toSOAP11(axisOutMsgCtx);
        } else {
            throw new SynapseException("Invalid soapVersionURI:" + soapVersionURI);
        }
    }

    private static String SOAP_ATR_ACTOR = "actor";
    private static String SOAP_ATR_ROLE = "role";
    private static String SOAP_ATR_MUST_UNDERSTAND = "mustUnderstand";

    /**
     * Converts the version of the the message context to 1.2.
     * <br />
     * <b>Message Changes:</b>
     * <ol>
     *     <li>Convert envelope, header elements</li>
     *     <li>For each header block convert attribute actor to role</li>
     *     <li>For each header block convert mustUnderstand value type</li>
     *     <li>For each header block remove 1.1 namespaced other attributes</li>
     * </ol>
     *
     * <b>Fault Changes:</b>
     * <ol>
     *     <li>Convert fault element</li>
     *     <li>faultcode to Fault/Code</li>
     *     <li>faultstring to First Fault/Reason/Text with lang=en</li>
     * </ol>
     *
     * @param axisOutMsgCtx message context to be converted
     * @throws AxisFault incase conversion process fails
     */
    public static void convertSOAP11toSOAP12( MessageContext axisOutMsgCtx )
    	throws AxisFault
    {
        SOAPEnvelope oldEnv = axisOutMsgCtx.getEnvelope();
        SOAPFactory soap12Factory = OMAbstractFactory.getSOAP12Factory();
        SOAPEnvelope newEnvelope  = soap12Factory.getDefaultEnvelope();

        if (oldEnv.getHeader() != null) {
        	copyHeader(	oldEnv, soap12Factory, newEnvelope,
        				SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI,
        				SOAP_ATR_ACTOR, SOAP_ATR_ROLE );
        }

        if (oldEnv.getBody() != null) {
        	copyBody(oldEnv, soap12Factory, newEnvelope);
        }

        axisOutMsgCtx.setEnvelope(newEnvelope);
    }

    /**
     * Converts the version of the the message context to 1.1.
     * <br />
     * <b>Message Changes:</b>
     * <ol>
     *     <li>Convert envelope, header elements</li>
     *     <li>For each header block convert attribute role to actor</li>
     *     <li>For each header block convert mustUnderstand value type</li>
     *     <li>For each header block remove 1.2 namespaced other attributes</li>
     * </ol>
     *
     * <b>Fault Changes:</b>
     * <ol>
     *     <li>Convert fault element</li>
     *     <li>Fault/Code to faultcode</li>
     *     <li>First Fault/Reason/Text to faultstring</li>
     * </ol>
     * @param axisOutMsgCtx message context to be converted
     * @throws AxisFault in case of an error in conversion
     */
    public static void convertSOAP12toSOAP11(MessageContext axisOutMsgCtx)
    	throws AxisFault
    {
        SOAPEnvelope oldEnv = axisOutMsgCtx.getEnvelope();
        SOAPFactory soap11Factory = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope newEnvelope  = soap11Factory.getDefaultEnvelope();

        if (oldEnv.getHeader() != null) {
        	copyHeader(	oldEnv, soap11Factory, newEnvelope,
    					SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI,
    					SOAP_ATR_ROLE, SOAP_ATR_ACTOR );
        }

        if (oldEnv.getBody() != null) {
        	copyBody(oldEnv, soap11Factory, newEnvelope);
        }

        axisOutMsgCtx.setEnvelope(newEnvelope);
    }

    private static void copyHeader(	SOAPEnvelope oldEnv,
    								SOAPFactory factory,
    								SOAPEnvelope newEnvelope,
    								String baseNamespaceURI,
    								String roleActorSource,
    								String roleActorTarget )
    {
        for(SOAPHeaderBlock soapHeader: oldEnv.getHeader().examineAllHeaderBlocks()) {
            SOAPHeaderBlock newSOAPHeader = factory.createSOAPHeaderBlock(
                    soapHeader.getLocalName(), soapHeader.getNamespace());

            copyAttrs(	soapHeader, newSOAPHeader, newEnvelope,
            			baseNamespaceURI, roleActorSource, roleActorTarget );

            for(OMNode child: soapHeader.getChildren()) {
            	newSOAPHeader.addChild(child);
            }

            newEnvelope.getHeader().addChild(newSOAPHeader);
        }
    }

    private static void copyAttrs(	SOAPHeaderBlock source, SOAPHeaderBlock target,
    								SOAPEnvelope newEnvelope, String baseNamespaceURI,
    								String roleActorSource, String roleActorTarget )
    {
        for(OMAttribute attr: source.getAllAttributes()) {
        	if(attr.getNamespace() != null
        			&& baseNamespaceURI.equals(
        					attr.getNamespace().getNamespaceURI())) {
        		String attrName = attr.getLocalName();

        		if(roleActorSource.equals(attrName)) {
        			OMAttribute newAtr = source.getOMFactory().createOMAttribute(
        					roleActorTarget, newEnvelope.getNamespace(),
        					attr.getAttributeValue());
        			target.addAttribute(newAtr);
        		} else if(SOAP_ATR_MUST_UNDERSTAND.equals(attrName)) {
        			boolean isMustUnderstand = source.getMustUnderstand();
        			target.setMustUnderstand(isMustUnderstand);
        		} else {
        			log.warn("Removed unsupported attribute from SOAP message: "
        					 + attrName);
        		}
        	} else {
        		target.addAttribute(attr);
        	}
        }
	}

    private static void copyBody(	SOAPEnvelope oldEnvelope,
    								SOAPFactory factory,
    								SOAPEnvelope newEnvelope )
    {
        for(OMNode omNode: oldEnvelope.getBody().getChildren()) {
            if (omNode != null && omNode instanceof SOAPFault) {
                SOAPFault soapFault = (SOAPFault) omNode;
                SOAPFault newSOAPFault = factory.createSOAPFault();
                newEnvelope.getBody().addChild(newSOAPFault);

                SOAPFaultCode code = soapFault.getCode();
                if(code != null) {
                    SOAPFaultCode newSOAPFaultCode
                            = factory.createSOAPFaultCode(newSOAPFault);

                    SOAPFaultValue value = code.getValue();
                    if(value != null) {
                        factory.createSOAPFaultValue(newSOAPFaultCode);
                        if(value.getTextAsQName() != null) {
                            newSOAPFaultCode.setText(
                                    getMappingCode(	value.getTextAsQName(),
                                    				factory.getSoapVersionURI()));
                        }
                    }
                }

                SOAPFaultReason reason = soapFault.getReason();
                if(reason != null) {
                    SOAPFaultReason newSOAPFaultReason
                            = factory.createSOAPFaultReason(newSOAPFault);

                    List<SOAPFaultText> allSoapTexts = reason.getAllSoapTexts();
                    if(!allSoapTexts.isEmpty()) {
                        newSOAPFaultReason.setText(allSoapTexts.get(0).getText());
                    }
                }
            } else {
                newEnvelope.getBody().addChild(omNode);
            }
        }
    }

    /**********************************************************************
     *                     Fault code conversions                         *
     **********************************************************************/

    private static final QName S11_FAULTCODE_VERSIONMISMATCH = new QName(
            SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "VersionMismatch",
            SOAP11Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);
    private static final QName S12_FAULTCODE_VERSIONMISMATCH = new QName(
            SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI, "VersionMismatch",
            SOAP12Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);

    private static final QName S11_FAULTCODE_MUSTUNDERSTAND = new QName(
            SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "MustUnderstand",
            SOAP11Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);
    private static final QName S12_FAULTCODE_MUSTUNDERSTAND = new QName(
            SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI, "MustUnderstand",
            SOAP12Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);

    private static final QName S11_FAULTCODE_CLIENT = new QName(
            SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Client",
            SOAP11Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);
    private static final QName S12_FAULTCODE_SENDER = new QName(
            SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Sender",
            SOAP12Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);

    private static final QName S11_FAULTCODE_SERVER = new QName(
            SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Server",
            SOAP11Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);
    private static final QName S12_FAULTCODE_RECEIVER = new QName(
            SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Receiver",
            SOAP12Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);

    private static final QName S12_FAULTCODE_DATAENCODINGUNKNOWN = new QName(
            SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI, "DataEncodingUnknown",
            SOAP12Constants.SOAP_DEFAULT_NAMESPACE_PREFIX);

    private static QName getMappingSOAP12Code(QName soap11Code) {
        if (S11_FAULTCODE_VERSIONMISMATCH.equals(soap11Code)) {
            return S12_FAULTCODE_VERSIONMISMATCH;
        } else if (S11_FAULTCODE_MUSTUNDERSTAND.equals(soap11Code)) {
            return S12_FAULTCODE_MUSTUNDERSTAND;
        } else if (S11_FAULTCODE_CLIENT.equals(soap11Code)) {
            return S12_FAULTCODE_SENDER;
        } else if (S11_FAULTCODE_SERVER.equals(soap11Code)) {
            return S12_FAULTCODE_RECEIVER;
        } else {
            log.warn("An unidentified SOAP11 FaultCode encountered, returning a blank QName");
            return new QName("", "");
        }
    }

    private static QName getMappingSOAP11Code(QName soap12Code) {
        if (S12_FAULTCODE_VERSIONMISMATCH.equals(soap12Code)) {
            return S11_FAULTCODE_VERSIONMISMATCH;
        } else if (S12_FAULTCODE_MUSTUNDERSTAND.equals(soap12Code)) {
            return S11_FAULTCODE_MUSTUNDERSTAND;
        } else if (S12_FAULTCODE_SENDER.equals(soap12Code)) {
            return S11_FAULTCODE_SERVER;
        } else if (S12_FAULTCODE_RECEIVER.equals(soap12Code)) {
            return S11_FAULTCODE_SERVER;
        } else if (S12_FAULTCODE_DATAENCODINGUNKNOWN.equals(soap12Code)) {
            log.debug("There is no matching SOAP11 code value for SOAP12 fault code " +
                    "DataEncodingUnknown, returning a blank QName");
            return new QName("");
        } else {
            log.warn("An unidentified SOAP11 FaultCode encountered, returning a blank QName");
            return new QName("");
        }
    }

    private static QName getMappingCode(QName in, String targetVersion) {
    	if(targetVersion.equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
    		return getMappingSOAP11Code(in);
    	} else if(targetVersion.equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
    		return getMappingSOAP12Code(in);
    	} else {
    		throw new RuntimeException("Unknown soap version: " + targetVersion);
    	}
    }
}
