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

package org.apache.axis2.wsdl.util;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class provides support for processing a WSDL4J definition
 * with a lower memory footprint.  This is useful for certain
 * environments.
 */
public class WSDLWrapperBasicImpl implements WSDLWrapperImpl {

    private static final Log log = LogFactory.getLog(WSDLWrapperBasicImpl.class);
    private static final boolean isDebugEnabled = log.isDebugEnabled();

    // javax.wsdl.Definition implements java.io.Serializable
    static final long serialVersionUID = -2788807375814097409L;

    // the wsdl4j wsdl definition object that is being wrapped
    private Definition wsdlDefinition = null;

    // the location of the base document used in the wsdl4j definition
    private URL wsdlURL = null;
    private String wsdlExplicitURI = null;
    private String wsdlDocumentBaseURI = null;

    //-------------------------------------------------------------------------
    // constructors
    //-------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param def    The WSDL Definition
     */
    public WSDLWrapperBasicImpl(Definition def) {
        if (log.isDebugEnabled()) {
            log.debug("WSDLWrapperBasicImpl(Definition def) called");
            log.trace(JavaUtils.callStackToString());
        }
        prepare(def, null);
    }


    /**
     * Constructor
     *
     * @param def    The WSDL Definition
     * @param wURL   The URL for the wsdl
     */
    public WSDLWrapperBasicImpl(Definition def, URL wURL) {
        if (log.isDebugEnabled()) {
            log.debug("WSDLWrapperBasicImpl(Definition def, URL wURL) called");
            log.trace(JavaUtils.callStackToString());
        }
        prepare(def, wURL);
    }


    /**
     * Initialize the wsdl definition wrapper
     *
     * @param def    The WSDL4J definition
     * @param wURL   The URL where the WSDL is obtained
     */
    private void prepare(Definition def, URL wURL) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".prepare()");
        }

        wsdlDefinition = def;
        wsdlURL = wURL;
    }


    //-------------------------------------------------------------------------
    // public WSDLWrapperImpl methods
    //-------------------------------------------------------------------------

    /*
     * Releases objects to reduce memory footprint.
     */
    public void releaseResources() {
        // placeholder
    }


    /*
     * Returns the WSDL4J Definition object that is being wrapped
     */
    public Definition getUnwrappedDefinition() {
        Definition def;

        if ((wsdlDefinition != null) &&
            (wsdlDefinition instanceof WSDLDefinitionWrapper)) {
            def = ((WSDLDefinitionWrapper) wsdlDefinition).getUnwrappedDefinition();
        } else {
            def = wsdlDefinition;
        }

        return def;
    }


    /**
     * Sets the WSDL4J Definition object that is being wrapped
     *
     * @param d  the WSDL4J Definition object
     */
    public void setDefinitionToWrap(Definition d) {
        wsdlDefinition = d;
    }


    /**
     * Sets the location for the WSDL4J Definition object that is being wrapped
     */
    public void setWSDLLocation(String uriLocation) {
        if (uriLocation != null) {
            try {
                wsdlURL = new URL(uriLocation);
            }
            catch (Exception e) {
                // todo
            }
        }
    }


    /**
     * Gets the location for the WSDL4J Definition object that is being wrapped
     */
    public String getWSDLLocation() {
        if (wsdlURL != null) {
            return wsdlURL.toString();
        }
        else {
            return null;
        }
    }


    /**
     * Closes the use of the wrapper implementation and allows
     * internal resources to be released.
     */
    public void close() {
        // nothing to do for this implementation
    }


    //-------------------------------------------------------------------------
    // javax.wsdl.Defintion interface methods
    //-------------------------------------------------------------------------

    public void setDocumentBaseURI(String d) {

        // Set the URI of the base document for the Definition.
        // This identifies the origin of the Definition.
        // Note that this is the URI of the base document, not the imports.

        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setDocumentBaseURI(" + d + ")");
        }

        if (wsdlDefinition != null) {
            wsdlDefinition.setDocumentBaseURI(d);
        }
    }

    public String getDocumentBaseURI() {

        // Get the URI of the base document for the Definition.
        // This identifies the origin of the Definition.
        // Note that this is the URI of the base document, not the imports.

        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getDocumentBaseURI()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getDocumentBaseURI();
        }
        return null;
    }

    public void setQName(QName n) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setQName(" + n + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setQName(n);
        }
    }

    public QName getQName() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getQName()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getQName();
        }
        return null;
    }

    public void setTargetNamespace(String t) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setTargetNamespace(" + t + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setTargetNamespace(t);
        }
    }

    public String getTargetNamespace() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getTargetNamespace()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getTargetNamespace();
        }
        return null;
    }

    public void addNamespace(String prefix, String namespaceURI) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addNamespace(" + prefix + ", " + namespaceURI + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addNamespace(prefix, namespaceURI);
        }
    }

    public String removeNamespace(String prefix) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".removeNamespace(" + prefix + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeNamespace(prefix);
        }
        return null;
    }

    public String getNamespace(String prefix) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getNamespace(" + prefix + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNamespace(prefix);
        }
        return null;
    }

    public String getPrefix(String namespaceURI) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getPrefix(" + namespaceURI + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getPrefix(namespaceURI);
        }
        return null;
    }

    public Map getNamespaces() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getNamespaces()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNamespaces();
        }
        return null;
    }

    public List getNativeAttributeNames() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getNativeAttributeNames()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNativeAttributeNames();
        }
        return null;
    }

    public void setTypes(Types types) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setTypes()");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setTypes(types);
        }
    }


    public Types getTypes() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getTypes();
        }
        return null;
    }

    public void addImport(Import importDef) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addImport(" + importDef + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addImport(importDef);
        }
    }

    public Import removeImport(Import importDef) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".removeImport(" + importDef + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeImport(importDef);
        }
        return null;
    }

    public List getImports(String namespaceURI) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getImports(" + namespaceURI + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getImports(namespaceURI);
        }
        return null;
    }

    public Map getImports() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getImports()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getImports();
        }
        return null;
    }

    public void addMessage(Message message) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addMessage(" + message + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addMessage(message);
        }
    }

    public Message getMessage(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getMessage(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getMessage(name);
        }
        return null;
    }

    public Message removeMessage(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".removeMessage(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeMessage(name);
        }
        return null;
    }

    public Map getMessages() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getMessages()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getMessages();
        }
        return null;
    }

    public void addBinding(Binding binding) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addBinding(" + binding + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addBinding(binding);
        }
    }

    public Binding getBinding(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getBinding(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getBinding(name);
        }
        return null;
    }

    public Binding removeBinding(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".removeBinding(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeBinding(name);
        }
        return null;
    }

    public Map getBindings() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getBindings()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getBindings();
        }
        return null;
    }

    public Map getAllBindings() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getAllBindings()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getAllBindings();
        }
        return null;
    }

    public void addPortType(PortType portType) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addPortType(" + portType + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addPortType(portType);
        }
    }

    public PortType getPortType(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getPortType(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getPortType(name);
        }
        return null;
    }

    public PortType removePortType(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".removePortType(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removePortType(name);
        }
        return null;
    }

    public Map getPortTypes() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getPortTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getPortTypes();
        }
        return null;
    }

    public Map getAllPortTypes() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getAllPortTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getAllPortTypes();
        }
        return null;
    }

    public void addService(Service service) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addService(" + service + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addService(service);
        }
    }

    public Service getService(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getService(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getService(name);
        }
        return null;
    }

    public Service removeService(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".removeService(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeService(name);
        }
        return null;
    }

    public Map getServices() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getServices()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getServices();
        }
        return null;
    }

    public Map getAllServices() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getAllServices()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getAllServices();
        }
        return null;
    }

    public void setDocumentationElement(org.w3c.dom.Element docEl) {

        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setDocumentationElement()");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setDocumentationElement(docEl);
        }
    }

    public org.w3c.dom.Element getDocumentationElement() {

        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getDocumentationElement()");
        }
        if (wsdlDefinition != null) {
            return  wsdlDefinition.getDocumentationElement();
        }
        return null;
    }

    public void addExtensibilityElement(ExtensibilityElement extElement) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addExtensibilityElement(" + extElement + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addExtensibilityElement(extElement);
        }
    }

    public List getExtensibilityElements() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getExtensibilityElements()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensibilityElements();
        }
        return null;
    }

    public Binding createBinding() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createBinding()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBinding();
        }
        return null;
    }

    public BindingFault createBindingFault() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createBindingFault()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingFault();
        }
        return null;
    }

    public BindingInput createBindingInput() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createBindingInput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingInput();
        }
        return null;
    }

    public BindingOperation createBindingOperation() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createBindingOperation()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingOperation();
        }
        return null;
    }

    public BindingOutput createBindingOutput() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createBindingOutput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingOutput();
        }
        return null;
    }

    public Fault createFault() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createFault()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createFault();
        }
        return null;
    }

    public Import createImport() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createImport()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createImport();
        }
        return null;
    }

    public Input createInput() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createInput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createInput();
        }
        return null;
    }

    public Message createMessage() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createMessage()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createMessage();
        }
        return null;
    }

    public Operation createOperation() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createOperation()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createOperation();
        }
        return null;
    }

    public Output createOutput() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createOutput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createOutput();
        }
        return null;
    }

    public Part createPart() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createPart()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createPart();
        }
        return null;
    }

    public Port createPort() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createPort()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createPort();
        }
        return null;
    }

    public PortType createPortType() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createPortType()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createPortType();
        }
        return null;
    }

    public Service createService() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createService()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createService();
        }
        return null;
    }

    public Types createTypes() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".createTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createTypes();
        }
        return null;
    }

    public void setExtensionRegistry(ExtensionRegistry extReg) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setExtensionRegistry(" + extReg + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setExtensionRegistry(extReg);
        }
    }

    public ExtensionRegistry getExtensionRegistry() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getExtensionRegistry()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensionRegistry();
        }
        return null;
    }

    @Override
	public String toString() {
        if (wsdlDefinition != null) {
            return wsdlDefinition.toString();
        }
        return "";
    }

    //-------------------------------------------------------------------------
    // other AbstractWSDLElement methods
    //-------------------------------------------------------------------------

    public ExtensibilityElement removeExtensibilityElement(ExtensibilityElement extElement) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".removeExtensibilityElement(" + extElement + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeExtensibilityElement(extElement);
        }
        return null;

    }

    public java.lang.Object getExtensionAttribute(QName name) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getExtensionAttribute(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensionAttribute(name);
        }
        return null;
    }

    public Map getExtensionAttributes() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getExtensionAttributes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensionAttributes();
        }
        return null;
    }

    public void setExtensionAttribute(QName name, java.lang.Object value) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setExtensionAttribute(" + name + ",  " + value + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setExtensionAttribute(name, value);
        }
    }



}
