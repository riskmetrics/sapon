/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rampart.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.rampart.handler.WSSHandlerConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class for the Axis2-WSS4J Module
 */
public class Axis2Util {

    private static ThreadLocal doomTacker = new ThreadLocal();

    public static boolean isUseDOOM() {
        Object value = doomTacker.get();
        return (value != null);
    }

    public static void useDOOM(boolean isDOOMRequired) {
//TODO Enable this when we have DOOM fixed to be able to flow in and out of Axis2
//        if(isDOOMRequired) {
//            if(!isUseDOOM()) {
//                System.setProperty(OMAbstractFactory.SOAP11_FACTORY_NAME_PROPERTY, SOAP11Factory.class.getName());
//                System.setProperty(OMAbstractFactory.SOAP12_FACTORY_NAME_PROPERTY, SOAP12Factory.class.getName());
//                System.setProperty(OMAbstractFactory.OM_FACTORY_NAME_PROPERTY, OMDOMFactory.class.getName());
//                doomTacker.set(new Object());
//            }
//        } else {
//            System.getProperties().remove(OMAbstractFactory.SOAP11_FACTORY_NAME_PROPERTY);
//            System.getProperties().remove(OMAbstractFactory.SOAP12_FACTORY_NAME_PROPERTY);
//            System.getProperties().remove(OMAbstractFactory.OM_FACTORY_NAME_PROPERTY);
//            doomTacker.set(null);
//        }
    }


	/**
	 * Creates a DOM Document using the SOAP Envelope.
	 * @param env An org.apache.axiom.soap.SOAPEnvelope instance
	 * @return Returns the DOM Document of the given SOAP Envelope.
	 * @throws Exception
	 */
	public static Document getDocumentFromSOAPEnvelope(SOAPEnvelope env, boolean useDoom)
			throws WSSecurityException {
		try {
            if(env instanceof Element) {
                return ((Element)env).getOwnerDocument();
            }

            if (useDoom) {
                env.build();

                // Workaround to prevent a bug in AXIOM where
                // there can be an incomplete OMElement as the first child body
                OMElement firstElement = env.getBody().getFirstElement();
                if (firstElement != null) {
                    firstElement.build();
                }

                //Get processed headers
                SOAPHeader soapHeader = env.getHeader();
                List<QName> processedHeaderQNames = new ArrayList<QName>();
                if(soapHeader != null) {
                    for(OMElement e: soapHeader.getChildElements()) {
                    	SOAPHeaderBlock hb;
                    	if(e instanceof SOAPHeaderBlock) {
                    		hb = (SOAPHeaderBlock)e;
                    	} else {
                    		hb = convertToSOAPHeaderBlock(soapHeader, e);
                    	}
                        if(hb.isProcessed()) {
                            processedHeaderQNames.add(hb.getQName());
                        }
                    }
                }

                // Check the namespace and find SOAP version and factory
                String nsURI = null;
                SOAPFactory factory;
                if (env.getNamespace().getNamespaceURI().equals(
                        SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                    nsURI = SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI;
                    factory = DOOMAbstractFactory.getSOAP11Factory();
                } else {
                    nsURI = SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI;
                    factory = DOOMAbstractFactory.getSOAP12Factory();
                }

                StAXSOAPModelBuilder stAXSOAPModelBuilder = new StAXSOAPModelBuilder(
                        env.getXMLStreamReader(), factory, nsURI);
                SOAPEnvelope envelope = (stAXSOAPModelBuilder)
                        .getSOAPEnvelope();
                ((OMNode) envelope.getParent()).build();

                //Set the processed flag of the processed headers
                SOAPHeader header = envelope.getHeader();
                for (Object element : processedHeaderQNames) {
               QName name = (QName) element;
               Iterator<OMElement> omKids = header.getChildrenWithName(name).iterator();
               if(omKids.hasNext()) {
            	   ((SOAPHeaderBlock)omKids.next()).setProcessed();
               }
            }

                Element envElem = (Element) envelope;
                return envElem.getOwnerDocument();
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                env.build();
                env.serialize(baos);
                ByteArrayInputStream bais = new ByteArrayInputStream(baos
                        .toByteArray());
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                factory.setNamespaceAware(true);
                return factory.newDocumentBuilder().parse(bais);
            }
		} catch (Exception e) {
			throw new WSSecurityException(
					"Error in converting SOAP Envelope to Document", e);
		}
	}

	private static SOAPHeaderBlock convertToSOAPHeaderBlock(SOAPHeader h, OMElement e) {
		final SOAPHeaderBlock hb = h.addHeaderBlock(e.getLocalName(), e.getNamespace());
		for(OMAttribute attr: e.getAllAttributes()) {
			hb.addAttribute(attr.getLocalName(), attr.getAttributeValue(), attr.getNamespace());
		}
		for(OMNamespace ns: e.getAllDeclaredNamespaces()) {
			hb.declareNamespace(ns);
		}
		for(OMNode child: e.getChildElements()) {
			child.detach();
			hb.addChild(child);
		}

		e.detach();
		h.build();
		hb.setProcessed();
		return hb;
	}


	public static SOAPEnvelope getSOAPEnvelopeFromDOMDocument(Document doc, boolean useDoom)
            throws WSSecurityException {

        if(useDoom) {
            try {
                //Get processed headers
                SOAPEnvelope env = (SOAPEnvelope)doc.getDocumentElement();
                List<QName> processedHeaderQNames = new ArrayList<QName>();
                SOAPHeader soapHeader = env.getHeader();

                if(soapHeader != null) {
                    for(OMElement element: soapHeader.getChildElements()) {
                    	SOAPHeaderBlock header = null;

                    	if (element instanceof SOAPHeaderBlock) {
                            header = (SOAPHeaderBlock) element;

                        // If a header block is not an instance of SOAPHeaderBlock, it means that
                        // it is a header we have added in rampart eg. EncryptedHeader and should
                        // be converted to SOAPHeaderBlock for processing
                    	} else {
                    		convertToSOAPHeaderBlock(soapHeader, element);
                    	}

                        if(header.isProcessed()) {
                            processedHeaderQNames.add(element.getQName());
                        }
                    }

                }
                XMLStreamReader reader = ((OMElement) doc.getDocumentElement())
                        .getXMLStreamReader();
                StAXSOAPModelBuilder stAXSOAPModelBuilder = new StAXSOAPModelBuilder(
                        reader, null);
                SOAPEnvelope envelope = stAXSOAPModelBuilder.getSOAPEnvelope();

                //Set the processed flag of the processed headers
                SOAPHeader header = envelope.getHeader();
                for(QName name: processedHeaderQNames) {
                    Iterator<OMElement> omKids = header.getChildrenWithName(name).iterator();
                    if(omKids.hasNext()) {
                        ((SOAPHeaderBlock)omKids.next()).setProcessed();
                    }
                }

                envelope.build();

                return envelope;

            } catch (FactoryConfigurationError e) {
                throw new WSSecurityException(e.getMessage());
            }
        } else {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                XMLUtils.outputDOM(doc.getDocumentElement(), os, true);
                ByteArrayInputStream bais =  new ByteArrayInputStream(os.toByteArray());

                StAXSOAPModelBuilder stAXSOAPModelBuilder = new StAXSOAPModelBuilder(XMLInputFactory.newInstance().createXMLStreamReader(bais), null);
                return stAXSOAPModelBuilder.getSOAPEnvelope();
            } catch (Exception e) {
                throw new WSSecurityException(e.getMessage());
            }
        }
    }


	/**
	 * Provides the appropriate key to pickup config params from the message context.
	 * This is acutally used when the outflow handler (WSDoAllSender)
	 * is repeated n number of times.
	 * @param originalKey The default key
	 * @param inHandler Whether the handler is the inflow handler or not
	 * @param repetition The current repetition number
	 * @return Returns the key to be used internally in the security module to pick
	 * up the config params.
	 */
	public static String getKey(String originalKey, boolean inHandler, int repetition) {

		if(repetition > 0 && !inHandler &&
				!originalKey.equals(WSSHandlerConstants.OUTFLOW_SECURITY)&&
				!originalKey.equals(WSSHandlerConstants.SENDER_REPEAT_COUNT)) {

				return originalKey + repetition;
		}
		return originalKey;
	}

    /**
     * This will build a DOOM Element that is of the same <code>Document</code>
     * @param factory
     * @param element
     * @return
     */
    public static OMElement toDOOM(OMFactory factory, OMElement element){
        StAXOMBuilder builder = new StAXOMBuilder(factory, element.getXMLStreamReader());
        OMElement elem = builder.getDocumentElement();
        elem.build();
        return elem;
    }

}
