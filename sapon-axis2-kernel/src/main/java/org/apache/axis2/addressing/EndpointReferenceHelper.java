/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.addressing;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AttributeHelper;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.metadata.InterfaceName;
import org.apache.axis2.addressing.metadata.ServiceName;
import org.apache.axis2.addressing.metadata.WSDLLocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The methods in this class are used to process {@link EndpointReference}
 * objects according to the rules of the 2005/08 (Final) and
 * 2004/08 (submission) WS-Addressing specifications.
 */
public class EndpointReferenceHelper {

    private static final Log log = LogFactory.getLog(EndpointReferenceHelper.class);

    private static final Map<String, QName> finalQNames
    	= new IdentityHashMap<String, QName>();
    private static final Map<String, QName> submissionQNames
    	= new IdentityHashMap<String, QName>();

    /**
     * Populates an endpoint reference based on the <code>OMElement</code> and
     * WS-Addressing namespace that is passed in.
     *
     * @param epr                 an endpoint reference instance to hold the info.
     * @param eprOMElement        an element of endpoint reference type
     * @param addressingNamespace the namespace of the WS-Addressing spec to comply with.
     * @throws AxisFault if unable to locate an address element, or if the specified namespace
     * is different to the actual namespace.
     * @see #fromOM(OMElement)
     */
    public static void fromOM(EndpointReference epr, OMElement eprOMElement,
                              String addressingNamespace) throws AxisFault {
        String namespace = fromOM(epr, eprOMElement);

        if (!namespace.equals(addressingNamespace)) {
			throw new AxisFault("The endpoint reference does not match the specified namespace.");
		}
    }

    /**
     * Populates an endpoint reference based on the <code>OMElement</code>. Returns the
     * WS-Addressing namespace of the endpoint reference.
     *
     * @param epr          an endpoint reference instance to hold the info. If the endpoint
     * reference is null then just the WS-Addressing namespace is returned.
     * @param eprOMElement an element of endpoint reference type
     * @return a string representing the WS-Addressing namespace of the endpoint reference.
     * @throws AxisFault if unable to locate an address element.
     */
    public static String fromOM(EndpointReference epr, OMElement eprOMElement)
    throws AxisFault {
        boolean isFinalAddressingNamespace = false;
        Map<String, QName> map = null;

        //First pass, identify the addressing namespace.
        OMElement address = eprOMElement
                .getFirstChildWithName(finalQNames.get(AddressingConstants.EPR_ADDRESS));

        if (address != null) {
            map = finalQNames;
            isFinalAddressingNamespace = true;

            if (log.isDebugEnabled()) {
                log.debug("fromOM: Found address element for namespace, " +
                        AddressingConstants.Final.WSA_NAMESPACE);
            }
        } else {
            address = eprOMElement.getFirstChildWithName(
                    submissionQNames.get(AddressingConstants.EPR_ADDRESS));

            if (address != null) {
                map = submissionQNames;
                isFinalAddressingNamespace = false;

                if (log.isDebugEnabled()) {
                    log.debug("fromOM: Found address element for namespace, " +
                            AddressingConstants.Submission.WSA_NAMESPACE);
                }
            } else {
                throw new AxisFault(
                        "Unable to locate an address element for the endpoint reference type.");
            }
        }

        //Second pass, identify the properties.
        if (epr != null) {
			fromOM(epr, eprOMElement, map, isFinalAddressingNamespace);
		}

        return (map.get(AddressingConstants.EPR_ADDRESS)).getNamespaceURI();
    }

    /**
     * Populates an endpoint reference based on the <code>String</code> that is
     * passed in. If the http://schemas.xmlsoap.org/ws/2004/08/addressing namespace
     * is in effect then any reference properties will be saved as reference parameters.
     * Regardless of the addressing namespace in effect, any elements present in the
     * <code>String</code> that are not recognised are saved as extensibility elements.
     *
     * @param eprString string from the element of endpoint reference type
     * @throws AxisFault if unable to locate an address element
     */
    public static EndpointReference fromString(String eprString) throws AxisFault {
        try {
            return fromOM(new StAXOMBuilder(
                    new ByteArrayInputStream(eprString.getBytes())).getDocumentElement());
        } catch (XMLStreamException e) {
            throw AxisFault.makeFault(e);
        }
    }

    /**
     * Populates an endpoint reference based on the <code>OMElement</code> that is
     * passed in. If the http://schemas.xmlsoap.org/ws/2004/08/addressing namespace
     * is in effect then any reference properties will be saved as reference parameters.
     * Regardless of the addressing namespace in effect, any elements present in the
     * <code>OMElement</code> that are not recognised are saved as extensibility elements.
     *
     * @param eprOMElement an element of endpoint reference type
     * @throws AxisFault if unable to locate an address element
     */
    public static EndpointReference fromOM(OMElement eprOMElement) throws AxisFault {
        EndpointReference epr = new EndpointReference("");
        fromOM(epr, eprOMElement);

        return epr;
    }

    /**
     * Creates an <code>OMElement</code> based on the properties of the endpoint
     * reference. The output may differ based on the addressing namespace that is
     * in effect when this method is called. If the http://www.w3.org/2005/08/addressing
     * namespace is in effect, and a metadata property has been defined for the
     * endpoint reference, then there will be a metadata element to contain the
     * property in the output. If the http://schemas.xmlsoap.org/ws/2004/08/addressing
     * namespace is in effect, however, then no metadata element will be included
     * in the output, even if a metadata property element has been defined.
     *
     * @param factory
     * @param epr
     * @param qname
     * @param addressingNamespace
     * @return
     * @throws AxisFault
     */
    public static OMElement toOM(OMFactory factory, EndpointReference epr, QName qname,
                                 String addressingNamespace) throws AxisFault {
        OMElement eprElement = null;

        if (log.isDebugEnabled()) {
            log.debug("toOM: Factory, " + factory);
            log.debug("toOM: Endpoint reference, " + epr);
            log.debug("toOM: Element qname, " + qname);
            log.debug("toOM: Addressing namespace, " + addressingNamespace);
        }

        if (addressingNamespace == null) {
            throw new AxisFault("Addressing namespace cannot be null.");
        }

        if (qname.getPrefix() != null) {
            OMNamespace wrapNs =
                    factory.createOMNamespace(qname.getNamespaceURI(), qname.getPrefix());
            if (factory instanceof SOAPFactory) {
                eprElement =
                        ((SOAPFactory) factory).createSOAPHeaderBlock(qname.getLocalPart(), wrapNs);
            } else {
                eprElement = factory.createOMElement(qname.getLocalPart(), wrapNs);
            }

            OMNamespace wsaNS = factory.createOMNamespace(addressingNamespace,
                                                          AddressingConstants.WSA_DEFAULT_PREFIX);
            OMElement addressE =
                    factory.createOMElement(AddressingConstants.EPR_ADDRESS, wsaNS, eprElement);
            String address = epr.getAddress();
            addressE.setText(address);

            List<OMAttribute> addressAttributes = epr.getAddressAttributes();
            if (addressAttributes != null) {
                for(final OMAttribute omAttribute: addressAttributes) {
                    AttributeHelper.importOMAttribute(omAttribute, addressE);
                }
            }

            List<OMElement> metaData = epr.getMetaData();
            if (metaData != null &&
                    AddressingConstants.Final.WSA_NAMESPACE.equals(addressingNamespace)) {
                OMElement metadataE = factory.createOMElement(
                        AddressingConstants.Final.WSA_METADATA, wsaNS, eprElement);
                for(final OMElement omElement: metaData) {
                    metadataE.addChild(ElementHelper.importOMElement(omElement, factory));
                }

                List<OMAttribute> metadataAttributes = epr.getMetadataAttributes();
                if (metadataAttributes != null) {
                    for(final OMAttribute omAttribute: metadataAttributes) {
                        AttributeHelper.importOMAttribute(omAttribute, metadataE);
                    }
                }
            }


            Map<QName, OMElement> referenceParameters = epr.getAllReferenceParameters();
            if (referenceParameters != null) {
                OMElement refParameterElement = factory.createOMElement(
                        AddressingConstants.EPR_REFERENCE_PARAMETERS, wsaNS, eprElement);
                for(final OMElement omElement: referenceParameters.values()) {
                    refParameterElement.addChild(ElementHelper.importOMElement(omElement, factory));
                }
            }

            List<OMAttribute> attributes = epr.getAttributes();
            if (attributes != null) {
                for(final OMAttribute omAttribute: attributes) {
                    AttributeHelper.importOMAttribute(omAttribute, eprElement);
                }
            }

            // add xs:any
            List<OMElement> extensibleElements = epr.getExtensibleElements();
            if (extensibleElements != null) {
            	for(final OMElement omElement: extensibleElements) {
                    eprElement.addChild(ElementHelper.importOMElement(omElement, factory));
                }
            }
        } else {
            throw new AxisFault("prefix must be specified");
        }

        return eprElement;
    }

    private static void fromOM(	EndpointReference epr, OMElement eprOMElement,
    							Map<String, QName> map,
    							boolean isFinalAddressingNamespace)
    {
        for(OMElement eprChildElement: eprOMElement.getChildElements()) {
            QName qname = eprChildElement.getQName();

            if (map.get(AddressingConstants.EPR_ADDRESS).equals(qname)) {
                //We need to identify the address element again in order to ensure
                //that it is not included with the extensibility elements.
                epr.setAddress(eprChildElement.getText());
                List<OMAttribute> addressAttributes = new ArrayList<OMAttribute>();
                for(OMAttribute attribute: eprChildElement.getAllAttributes()) {
                    addressAttributes.add(attribute);
                }
                epr.setAddressAttributes(addressAttributes);
            } else if (map.get(AddressingConstants.EPR_REFERENCE_PARAMETERS).equals(qname)) {
                for(OMElement element: eprChildElement.getChildElements()) {
                    epr.addReferenceParameter(element);
                }
            } else if (isFinalAddressingNamespace &&
                    map.get(AddressingConstants.Final.WSA_METADATA).equals(qname)) {
                for(OMElement elem: eprChildElement.getChildElements()) {
                    epr.addMetaData(elem);
                }
                List<OMAttribute> metadataAttributes = new ArrayList<OMAttribute>();
                for(OMAttribute attribute: eprChildElement.getAllAttributes()) {
                    metadataAttributes.add(attribute);
                }
                epr.setMetadataAttributes(metadataAttributes);
            } else if (!isFinalAddressingNamespace &&
                    map.get(AddressingConstants.Submission.EPR_REFERENCE_PROPERTIES).equals(qname))
            {
                // since we have the model for WS-Final, we don't have a place to keep this reference properties.
                // The only compatible place is reference properties
                for(OMElement element: eprChildElement.getChildElements()) {
                    epr.addReferenceParameter(element);
                }
            } else {
                epr.addExtensibleElement(eprChildElement);
            }
        }

        for(OMAttribute attribute: eprOMElement.getAllAttributes()) {
            epr.addAttribute(attribute);
        }

        if (log.isDebugEnabled()) {
            log.debug("fromOM: Endpoint reference, " + epr);
        }
    }

    /**
     * Retrieves the WS-Addressing EPR ServiceName element from an EPR.
     *
     * @param epr the EPR to retrieve the element from
     * @param addressingNamespace the WS-Addressing namespace associated with
     * the EPR.
     * @return an instance of <code>ServiceName</code>. The return value is
     * never <code>null</code>.
     * @throws AxisFault
     */
    public static ServiceName getServiceNameMetadata(EndpointReference epr, String addressingNamespace) throws AxisFault {
    	ServiceName serviceName = new ServiceName();

        final List<OMElement> elements;
        if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNamespace)) {
			elements = epr.getExtensibleElements();
		} else {
			elements = epr.getMetaData();
		}

        if (elements != null) {
            //Retrieve the service name and endpoint name.
            for(final OMElement omElement: elements) {
                if (ServiceName.isServiceNameElement(omElement)) {
                    serviceName.fromOM(omElement);
                    break;
                }
            }
        }

        return serviceName;
    }

    /**
     * Retrieves the WS-Addressing EPR PortType, or InterfaceName, element from an EPR,
     * as appropriate.
     *
     * @param epr the EPR to retrieve the element from
     * @param addressingNamespace the WS-Addressing namespace associated with
     * the EPR.
     * @return an instance of <code>InterfaceName</code>. The return value is
     * never <code>null</code>.
     * @throws AxisFault
     */
    public static InterfaceName getInterfaceNameMetadata(EndpointReference epr, String addressingNamespace) throws AxisFault {
        InterfaceName interfaceName = new InterfaceName();
        List<OMElement> elements = null;

        if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNamespace)) {
			elements = epr.getExtensibleElements();
		} else {
			elements = epr.getMetaData();
		}

        if (elements != null) {
            //Retrieve the service name and endpoint name.
            for(final OMElement omElement: elements) {
                if (InterfaceName.isInterfaceNameElement(omElement)) {
                    interfaceName.fromOM(omElement);
                    break;
                }
            }
        }

        return interfaceName;
    }

    /**
     * Retrieves the wsdli:wsdlLocation attribute from an EPR.
     *
     * @param epr the EPR to retrieve the attribute from
     * @param addressingNamespace the WS-Addressing namespace associated with
     * the EPR.
     * @return an instance of <code>WSDLLocation</code>. The return value is
     * never <code>null</code>.
     * @throws AxisFault
     */
    public static WSDLLocation getWSDLLocationMetadata(EndpointReference epr, String addressingNamespace) throws AxisFault {
        WSDLLocation wsdlLocation = new WSDLLocation();
        List<OMAttribute> attributes = null;

        if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNamespace)) {
			attributes = epr.getAttributes();
		} else {
			attributes = epr.getMetadataAttributes();
		}

        if (attributes != null) {
            //Retrieve the wsdl location.
            for (final OMAttribute omAttribute: attributes) {
                if (WSDLLocation.isWSDLLocationAttribute(omAttribute)) {
                    wsdlLocation.fromOM(omAttribute);
                    break;
                }
            }
        }

        return wsdlLocation;
    }

    /**
     * Adds an instance of <code>ServiceName</code> as metadata to the specified EPR.
     * The metadata is mapped to a WS-Addressing EPR ServiceName element.
     *
     * @param factory an <code>OMFactory</code>
     * @param epr the EPR to retrieve the attribute from
     * @param addressingNamespace the WS-Addressing namespace associated with
     * the EPR.
     * @param serviceName an instance of <code>ServiceName</code> that contains the
     * metadata
     * @throws AxisFault
     */
    public static void setServiceNameMetadata(OMFactory factory, EndpointReference epr, String addressingNamespace, ServiceName serviceName) throws AxisFault {
        if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNamespace)) {
            OMElement omElement = serviceName.toOM(factory, ServiceName.subQName);
            epr.addExtensibleElement(omElement);
        }
        else {
            OMElement omElement = serviceName.toOM(factory, ServiceName.wsamQName);
            epr.addMetaData(omElement);
        }
    }

    /**
     * Adds an instance of <code>InterfaceName</code> as metadata to the specified EPR.
     * The metadata is mapped to a WS-Addressing EPR PortType or InterfaceName element.
     *
     * @param factory an <code>OMFactory</code>
     * @param epr the EPR to retrieve the attribute from
     * @param addressingNamespace the WS-Addressing namespace associated with
     * the EPR.
     * @param interfaceName an instance of <code>InterfaceName</code> that contains the
     * metadata
     * @throws AxisFault
     */
     public static void setInterfaceNameMetadata(OMFactory factory, EndpointReference epr, String addressingNamespace, InterfaceName interfaceName) throws AxisFault {
        if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNamespace)) {
            OMElement omElement = interfaceName.toOM(factory, InterfaceName.subQName);
            epr.addExtensibleElement(omElement);
        }
        else {
            OMElement omElement = interfaceName.toOM(factory, InterfaceName.wsamQName);
            epr.addMetaData(omElement);
        }
    }

     /**
      * Adds an instance of <code>WSDLLocation</code> as metadata to the specified EPR.
      * The metadata is mapped to a wsdli:wsdlLocation attribute.
      *
      * @param factory an <code>OMFactory</code>
      * @param epr the EPR to retrieve the attribute from
      * @param addressingNamespace the WS-Addressing namespace associated with
      * the EPR.
      * @param wsdlLocation an instance of <code>WSDLLocation</code> that contains the
      * metadata
      * @throws AxisFault
      */
    public static void setWSDLLocationMetadata(OMFactory factory, EndpointReference epr, String addressingNamespace, WSDLLocation wsdlLocation) throws AxisFault {
        OMAttribute attribute = wsdlLocation.toOM(factory);

        if (AddressingConstants.Submission.WSA_NAMESPACE.equals(addressingNamespace)) {
            epr.addAttribute(attribute);
        }
        else {
            List<OMAttribute> list = new ArrayList<OMAttribute>();
            list.add(attribute);
            epr.setMetadataAttributes(list);
        }
    }

    static {
        finalQNames.put(AddressingConstants.EPR_ADDRESS, new QName(
                AddressingConstants.Final.WSA_NAMESPACE, AddressingConstants.EPR_ADDRESS));
        finalQNames.put(AddressingConstants.EPR_REFERENCE_PARAMETERS, new QName(
                AddressingConstants.Final.WSA_NAMESPACE,
                AddressingConstants.EPR_REFERENCE_PARAMETERS));
        finalQNames.put(AddressingConstants.Final.WSA_METADATA, new QName(
                AddressingConstants.Final.WSA_NAMESPACE, AddressingConstants.Final.WSA_METADATA));

        submissionQNames.put(AddressingConstants.EPR_ADDRESS, new QName(
                AddressingConstants.Submission.WSA_NAMESPACE, AddressingConstants.EPR_ADDRESS));
        submissionQNames.put(AddressingConstants.EPR_REFERENCE_PARAMETERS, new QName(
                AddressingConstants.Submission.WSA_NAMESPACE,
                AddressingConstants.EPR_REFERENCE_PARAMETERS));
        submissionQNames.put(AddressingConstants.Submission.EPR_REFERENCE_PROPERTIES, new QName(
                AddressingConstants.Submission.WSA_NAMESPACE,
                AddressingConstants.Submission.EPR_REFERENCE_PROPERTIES));
    }
}
