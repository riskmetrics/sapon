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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;


/**
 * This class provides support for processing a WSDL4J definition
 * with a lower memory footprint.  This is useful for certain
 * environments.
 *
 * The Type and Documentation objects consume the most space
 * in many scenarios.  This implementation reloads these objects
 * when then they are requested.
 */
public class WSDLWrapperReloadImpl implements WSDLWrapperImpl {

    private static final Log log = LogFactory.getLog(WSDLWrapperReloadImpl.class);
    private static final boolean isDebugEnabled = log.isDebugEnabled();

    // javax.wsdl.Definition implements java.io.Serializable
    static final long serialVersionUID = -2788807375814097409L;

    // the wsdl4j wsdl definition object that is being wrapped
    private Definition wsdlDefinition = null;

    // the location of the base document used in the wsdl4j definition
    private URL wsdlURL = null;
    private String wsdlExplicitURI = null;
    private String wsdlDocumentBaseURI = null;


    // The wsdlDefinition always has the Types and DocumentElement
    // purged from it.
    // If USE_SOFT_REFERENCES is true, then we keep a SOFT reference
    // to these objects.
    // If USE_SOFT_REFERENCES is false, then a loadDefinition is always
    // performed to get the Type or DocumentationElement

    private static boolean USE_SOFT_REFERENCES = true;
    private transient SoftReference<Types> softTypes = null;
    private transient SoftReference<Element> softDocElement = null;

    /**
     * Constructor
     * The WSDL Defintion object is owned by the WSDLWrapperReloadImpl object.
     *
     * @param def    The WSDL Definition
     */
    public WSDLWrapperReloadImpl(Definition def) {
        if (log.isDebugEnabled()) {
            log.debug("WSDLWrapperReloadImpl(Definition def) called");
            log.trace(JavaUtils.callStackToString());
        }
        prepare(def, null);
    }


    /**
     * Constructor
     * The WSDL Defintion object is owned by the WSDLWrapperReloadImpl object.
     *
     * @param def    The WSDL Definition
     * @param wURL   The URL for the wsdl
     */
    public WSDLWrapperReloadImpl(Definition def, URL wURL) {
        if (log.isDebugEnabled()) {
            log.debug("WSDLWrapperReloadImpl(Definition def, URL wURL) called");
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

        if (def != null) {
            try {
                wsdlDocumentBaseURI = def.getDocumentBaseURI();

                // build up the wsdlURL if possible
                if ((wsdlURL == null) && (wsdlDocumentBaseURI != null)) {
                    try {
                        URL locURL = new URL(wsdlDocumentBaseURI);
                        wsdlURL = locURL;
                    } catch (Exception uex) {
                        // keep going
                    }
                }

                // get the explicit location of the wsdl if possible
                if (wsdlURL != null) {
                    wsdlExplicitURI = getExplicitURI(wsdlURL);
                }

                // Release the Types and DocumentationElement Resources
                releaseResources();

            } catch (Exception e) {
                if (isDebugEnabled) {
                    log.debug(getClass().getName() + ".prepare():  Caught exception ["
                            + e.getClass().getName() + "]  error [" + e.getMessage() + "]", e);
                }
            }
        }

        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".prepare():   wsdlDocumentBaseURI [" + wsdlDocumentBaseURI
                    + "]     wsdlExplicitURI [" + wsdlExplicitURI + "]   wsdlURL [" + wsdlURL + "]");
        }

    }


    //-------------------------------------------------------------------------
    // public WSDLWrapperImpl methods
    //-------------------------------------------------------------------------

    /*
     * Releases Type and DocumentElement Resources
     */
    public void releaseResources() {
        if (wsdlDefinition != null) {
            Types types = wsdlDefinition.getTypes();
            if (types != null) {
                wsdlDefinition.setTypes(null);
            }
            this.setCachedTypes(types);

            Element element = wsdlDefinition.getDocumentationElement();
            if (element != null) {
                wsdlDefinition.setDocumentationElement(null);
            }
            this.setCachedDocElement(element);

        }
    }

    /**
     * Store the cached type.  Since this is a SOFT reference,
     * the gc may remove it.
     * @param types
     */
    private void setCachedTypes(Types types) {
        if (USE_SOFT_REFERENCES) {
            if (softTypes == null || softTypes.get() == null) {
                if (types != null) {
                    softTypes = new SoftReference<Types>(types);
                }
            }
        }
    }

    /**
     * Get the cached type.  Since this is a SOFT reference,
     * the gc may remove it.
     * @return types
     */
    private Types getCachedTypes() {
        if (USE_SOFT_REFERENCES) {
            if (softTypes == null || softTypes.get() == null) {
                return null;
            } else if (softTypes.get().equals(Boolean.FALSE)) {
                // The wsdl has no types
                return null;
            } else {
                return softTypes.get();
            }
        } else {
            return null;
        }
    }

    private boolean hasCachedTypes() {
        if (USE_SOFT_REFERENCES) {
            return (softTypes != null && softTypes.get() != null);
        } else {
            return false;
        }
    }

    /**
     * Store the cached document element.  Since this is a SOFT reference,
     * the gc may remove it.
     * @param e Element
     */
    private void setCachedDocElement(Element e) {
        if (USE_SOFT_REFERENCES) {
            if (softDocElement == null || softDocElement.get() == null) {
                if (e != null) {
                    softDocElement = new SoftReference<Element>(e);
                }
            }
        }
    }

    /**
     * Get the cached type.  Since this is a SOFT reference,
     * the gc may remove it.
     * @return types
     */
    private Element getCachedDocElement() {
        if (USE_SOFT_REFERENCES) {
            if (softDocElement == null || softDocElement.get() == null) {
                return null;
            } else {
                return softDocElement.get();
            }
        } else {
            return null;
        }
    }

    private boolean hasCachedDocElement() {
        if (USE_SOFT_REFERENCES) {
            return (softDocElement != null && softDocElement.get() != null);
        } else {
            return false;
        }
    }


    /*
     * Returns a full, reloaded,WSDL4J Definition object.
     * This avoids the memory saving capabilities of this wrapper.
     * The caller must not save the returned defintion.
     * @return Defintion
     */
    public Definition getUnwrappedDefinition() {
        Definition def;
        if (wsdlDefinition == null) {
            // If no definiotn, load one
            try {
                def = loadDefinition();
            } catch (Exception e) {
                // unable to load the definition
                if (isDebugEnabled) {
                    log.debug(getClass().getName()
                            + ".getUnwrappedDefinition(): error trying to load Definition    ["
                            + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
                }
                def = null;
            }
        } else if (wsdlDefinition instanceof WSDLWrapperBasicImpl) {
            // If wrapping another wrapper, then delegate
            def = ((WSDLWrapperBasicImpl) wsdlDefinition).getUnwrappedDefinition();
        } else {
            // The question is whether a new WSDLDefinition should be loaded and
            // returned or whether the existing definition (w/o the Type
            // and DocumentElement) should be returned.
            //
            // The answer is to reload the WSDLDefinition and provide the new
            // one without affecting the existing WSDLDefintion that is stored.
            // The onus is on the caller to free this new WSDLDefinition and
            // not hold onto it.  If the calller wants memory saving
            // capabilities, then the caller should be using this wrapper directly.
            try {
                def = loadDefinition();
                if(def == null) {
                    def = wsdlDefinition;
                }
            }
            catch (Exception e) {
                // unable to load the definition
                if (isDebugEnabled) {
                    log.debug(getClass().getName()
                              + ".getUnwrappedDefinition(): error trying to load Definition    ["
                              + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
                }
                def = wsdlDefinition;
            }
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
        // This identifies the origin of the Definition and
        // allows the Definition to be reloaded.
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
        // This identifies the origin of the Definition and
        // allows the Definition to be reloaded.
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

    public Map<String, String> getNamespaces() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getNamespaces()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNamespaces();
        }
        return null;
    }

    public List<String> getNativeAttributeNames() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getNativeAttributeNames()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNativeAttributeNames();
        }
        return null;
    }

    public void setTypes(Types types) {

        if (wsdlDefinition != null) {
            // note: the wsdl definition implementation can't re-create the types
            //       after the types got set to null so the wsdl definition would
            //       need to be reloaded
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".setTypes() from wsdl Definition");
            }

            wsdlDefinition.setTypes(types);

            // TODO: should we keep the types object if it gets set?
            wsdlDefinition.setTypes(null);
        } else {
            /*
             // reload the wsdl definition object
             // TODO: what about any online changes that have been made to the definition?

             if (isDebugEnabled) {
                 log.debug(getClass().getName()+".setTypes() from reloaded wsdl Definition");
             }

             Definition def = null;
             try {
                 def = loadDefinition();
             }
             catch (Exception e) {
                 if (isDebugEnabled) {
                     log.debug(getClass().getName()+".setTypes(): error trying to load Definition    ["+e.getClass().getName()+"]  error ["+e.getMessage()+"] ", e);
                 }
             }

             if (def != null) {
                 def.setTypes(types);
             }
             else {
                 if (isDebugEnabled) {
                     log.debug(getClass().getName()+".setTypes(): nothing to set");
                 }
             }
             */
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".setTypes(): nothing to set");
            }
        }
    }


    public Types getTypes() {
        if (isDebugEnabled) {
            log.trace(getClass().getName() + ".getTypes() call stack =" + JavaUtils.callStackToString());
        }
        // See if we have a soft reference to the Type

        if (hasCachedTypes()) {
            Types t = getCachedTypes();
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".getTypes() from soft reference [" + t
                            + "]");
            }
            return t;
        }


        // reload the wsdl definition object
        // TODO: what about any changes that have been made to the definition?

        Definition def = null;
        try {
            def = loadDefinition();
        } catch (Exception e) {
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".getTypes(): error trying to load Definition    ["
                        + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
            }
        }

        if (def != null) {
            Types t = def.getTypes();
            setCachedTypes(t);
            setCachedDocElement(def.getDocumentationElement());

            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".getTypes() from reloaded wsdl Definition returning [" + t
                        + "]");
            }

            return t;
        } else {
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".getTypes() returning NULL");
            }
            return null;
        }

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

    public List<Import> getImports(String namespaceURI) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getImports(" + namespaceURI + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getImports(namespaceURI);
        }
        return null;
    }

    public Map<String, List<Import>> getImports() {
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

    public Map<QName, Message> getMessages() {
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

    public Map<QName, Binding> getBindings() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getBindings()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getBindings();
        }
        return null;
    }

    public Map<QName, Binding> getAllBindings() {
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

    public Map<QName, PortType> getPortTypes() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getPortTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getPortTypes();
        }
        return null;
    }

    public Map<QName, PortType> getAllPortTypes() {
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

    public Map<QName, Service> getServices() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getServices()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getServices();
        }
        return null;
    }

    public Map<QName, Service> getAllServices() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getAllServices()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getAllServices();
        }
        return null;
    }

    public void setDocumentationElement(org.w3c.dom.Element docEl) {

        if (wsdlDefinition != null) {
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".setDocumentationElement(docEl) from wsdl Definition");
            }

            // set the element in the definition
            // don't make assumptions about what the implementation does
            wsdlDefinition.setDocumentationElement(docEl);
            // reset the element
            wsdlDefinition.setDocumentationElement(null);
        } else {
            /*
             // reload the wsdl definition object
             // TODO: what about any online changes that have been made to the definition

             if (isDebugEnabled) {
                 log.debug(getClass().getName()+".setDocumentationElement(docEl) from reloaded wsdl Definition");
             }

             Definition def = null;
             try {
                 def = loadDefinition();
             }
             catch (Exception e) {
                 if (isDebugEnabled) {
                     log.debug(getClass().getName()+".setDocumentationElement(docEl): error trying to load Definition    ["+e.getClass().getName()+"]  error ["+e.getMessage()+"] ", e);
                 }
             }

             if (def != null) {
                 def.setDocumentationElement(docEl);
             }
             else {
                 if (isDebugEnabled) {
                     log.debug(getClass().getName()+".setDocumentationElement(docEl): nothing to set");
                 }
             }
             */
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".setDocumentationElement(docEl): nothing to set");
            }
        }
    }

    public org.w3c.dom.Element getDocumentationElement() {

        if (isDebugEnabled) {
            log.trace(getClass().getName() + ".getDocumentationElement() call stack =" +
                      JavaUtils.callStackToString());
        }

        // See if we have a soft reference to the DocumentElement
        if (hasCachedDocElement()) {
            Element e = getCachedDocElement();

            if (log.isDebugEnabled()) {
                log.debug(getClass().getName()
                          + ".getDocumentationElement() from soft reference ");
            }
            return e;
        }

        // reload the wsdl definition object
        // TODO: what about any online changes that have been made to the definition?

        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getDocumentationElement() from reloaded wsdl Definition");
        }

        Definition def = null;
        try {
            def = loadDefinition();
        } catch (Exception e) {
            if (isDebugEnabled) {
                log.debug(getClass().getName()
                        + ".getDocumentationElement(): error trying to load Definition    ["
                        + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
            }
        }

        if (def != null) {
            org.w3c.dom.Element docElement = def.getDocumentationElement();
            setCachedDocElement(docElement);
            setCachedTypes(def.getTypes());

            if (isDebugEnabled) {
                if (docElement != null) {
                    log.debug(getClass().getName()
                            + ".getDocumentationElement() from reloaded wsdl Definition returning  NON-NULL org.w3c.dom.Element");
                } else {
                    log.debug(getClass().getName()
                            + ".getDocumentationElement() from reloaded wsdl Definition returning  NULL org.w3c.dom.Element");
                }
            }
            return docElement;
        } else {
            if (isDebugEnabled) {
                log.debug(getClass().getName() + ".getDocumentationElement() returning NULL");
            }
            return null;
        }
    }

    public void addExtensibilityElement(ExtensibilityElement extElement) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".addExtensibilityElement(" + extElement + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addExtensibilityElement(extElement);
        }
    }

    public List<ExtensibilityElement> getExtensibilityElements() {
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
            return this.getClass().getName() + "\n" + wsdlDefinition.toString();
        }
        return this.getClass().getName();
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

    public Map<QName, List<QName>> getExtensionAttributes() {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getExtensionAttributes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensionAttributes();
        }
        return null;
    }

    public void setExtensionAttribute(QName name, List<QName> value) {
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".setExtensionAttribute(" + name + ",  " + value + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setExtensionAttribute(name, value);
        }
    }


    //-------------------------------------------------------------------------
    // private methods
    //-------------------------------------------------------------------------


    private String getExplicitURI(URL wsdlURL) throws WSDLException {

        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".getExplicitURI(" + wsdlURL + ") ");
        }

        String explicitURI = null;

        ClassLoader classLoader = AccessController.doPrivileged(
        		new PrivilegedAction<ClassLoader>() {
        			public ClassLoader run() {
        				return Thread.currentThread().getContextClassLoader();
        			}
        		});

        try {
            URL url = wsdlURL;
            String filePath = null;
            boolean isFileProtocol =
                    (url != null && "file".equals(url.getProtocol())) ? true : false;

            if (isFileProtocol) {
                filePath = (url != null) ? url.getPath() : null;

                URI uri = null;
                if (url != null) {
                    uri = new URI(url.toString());
                }

                // Check if the uri has relative path
                // ie path is not absolute and is not starting with a "/"
                boolean isRelativePath =
                        (filePath != null && !new File(filePath).isAbsolute()) ? true : false;

                if (isRelativePath) {
                    if (isDebugEnabled) {
                        log.debug(getClass().getName() + ".getExplicitURI(" + wsdlURL
                                + "): WSDL URL has a relative path");
                    }

                    // Lets read the complete WSDL URL for relative path from class loader
                    // Use relative path of url to fetch complete URL.
                    url = getAbsoluteURL(classLoader, filePath);

                    if (url == null) {
                        if (isDebugEnabled) {
                            log.debug(getClass().getName() + ".getExplicitURI(" + wsdlURL + "): "
                                    + "WSDL URL for relative path not found in ClassLoader");
                            log.debug(getClass().getName()
                                    + ".getExplicitURI("
                                    + wsdlURL
                                    + "): "
                                    + "Unable to read WSDL from relative path, check the relative path");
                            log.debug(getClass().getName() + ".getExplicitURI(" + wsdlURL + "): "
                                    + "Relative path example: file:/WEB-INF/wsdl/<wsdlfilename>");
                            log.debug(getClass().getName()
                                    + ".getExplicitURI("
                                    + wsdlURL
                                    + "): "
                                    + "Using relative path as default wsdl URL to load wsdl Definition.");
                        }
                        url = wsdlURL;
                    }
                    else {
                        if (isDebugEnabled) {
                            log.debug(getClass().getName() + ".getExplicitURI(" + wsdlURL + "): "
                                    + "WSDL URL found for relative path: " + filePath + " scheme: "
                                    + uri.getScheme());
                        }
                    }
                }
            }

            URLConnection urlCon = url.openConnection();
            InputStream is = null;
            try {
            	is = getInputStream(urlCon);
            } catch (IOException e) {
                if (isDebugEnabled) {
                    log.debug(getClass().getName() + ".getExplicitURI(" + wsdlURL + "): "
                            + "Could not open url connection. Trying to use "
                            + "classloader to get another URL.");
                }

                if (filePath != null) {

                    url = getAbsoluteURL(classLoader, filePath);
                    if (url == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Could not locate URL for wsdl. Reporting error");
                        }
                        throw new WSDLException("WSDL4JWrapper : ", e.getMessage(), e);
                    } else {
                        urlCon = url.openConnection();
                        if (log.isDebugEnabled()) {
                            log.debug("Found URL for WSDL from jar");
                        }
                    }
                } else {
                    if (isDebugEnabled) {
                        log.debug(getClass().getName() + ".getExplicitURI(" + wsdlURL + "): "
                                + "Could not get URL from classloader. Reporting "
                                + "error due to no file path.");
                    }
                    throw new WSDLException("WSDLWrapperReloadImpl : ", e.getMessage(), e);
                }
            }
            if (is != null) {
                is.close();
            }

            explicitURI = urlCon.getURL().toString();

        } catch (Exception ex) {
            throw new WSDLException("WSDLWrapperReloadImpl : ", ex.getMessage(), ex);
        }

        return explicitURI;
    }


    private URL getAbsoluteURL(final ClassLoader classLoader, final String filePath) throws WSDLException {
        URL url = AccessController.doPrivileged(
                new PrivilegedAction<URL>() {
                    public URL run() {
                        return classLoader.getResource(filePath);
                    }
                });
        if (url == null) {
            if (log.isDebugEnabled()) {
                log.debug("Could not get URL from classloader. Looking in a jar.");
            }
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlLoader = (URLClassLoader) classLoader;
                url = getURLFromJAR(urlLoader, wsdlURL);
            }
        }
        return url;
    }

    /**
     * Load and Return a Definition object.
     * (The caller will determine if the Definition object should have
     * its resources freed or not)
     * @return Definition
     * @throws WSDLException
     */
    private Definition loadDefinition() throws WSDLException {

        Definition def = null;

        if (wsdlExplicitURI != null) {
            try {
                def = AccessController.doPrivileged(
                		new PrivilegedExceptionAction<Definition>() {
                			public Definition run() throws WSDLException {
                				WSDLReader reader = getWSDLReader();
                				return reader.readWSDL(wsdlExplicitURI);
                			}
                		});
            } catch (PrivilegedActionException e) {
                if (isDebugEnabled) {
                    log.debug(getClass().getName() + ".loadDefinition(): "
                            + "Exception thrown from AccessController: " + e);
                    log.trace("Call Stack = " + JavaUtils.callStackToString());
                }
                WSDLException we = new WSDLException("WSDLWrapperReloadImpl : ", e.getMessage(), e);
                throw we;
            }
        }

        // Loading the wsdl is expensive.  Dump the callstack.. so that we
        // support can look at the trace and determine if this class is being used incorrectly.
        if (isDebugEnabled) {
            log.debug(getClass().getName() + ".loadDefinition():  returning Definition [" + def + "]");
            log.trace("Call Stack = " + JavaUtils.callStackToString());
        }
        return def;
    }

    private URL getURLFromJAR(URLClassLoader urlLoader, URL relativeURL) throws WSDLException {

        URL[] urlList = urlLoader.getURLs();

        if (urlList == null) {
            return null;
        }

        for (URL url : urlList) {
            if (url == null) {
                return null;
            }

            if ("file".equals(url.getProtocol())) {

                File f = new File(url.getPath());

                //If file is not of type directory then its a jar file
                if (f.exists() && !f.isDirectory()) {
                    try {
                        JarFile jf = new JarFile(f);
                        Enumeration<JarEntry> entries = jf.entries();
                        // read all entries in jar file and return the first
                        // wsdl file that matches the relative path
                        while (entries.hasMoreElements()) {
                            JarEntry je = entries.nextElement();
                            String name = je.getName();
                            if (name.endsWith(".wsdl")) {
                                String relativePath = relativeURL.getPath();
                                if (relativePath.endsWith(name)) {
                                    String path = f.getAbsolutePath();

                                    // This check is necessary because Unix/Linux file paths begin
                                    // with a '/'. When adding the prefix 'jar:file:/' we may end
                                    // up with '//' after the 'file:' part. This causes the URL
                                    // object to treat this like a remote resource
                                    if (path != null && path.indexOf("/") == 0) {
                                        path = path.substring(1, path.length());
                                    }

                                    URL absoluteUrl =
                                            new URL("jar:file:/" + path + "!/" + je.getName());
                                    return absoluteUrl;
                                }
                            }
                        }
                    } catch (Exception e) {
                        WSDLException we =
                                new WSDLException("WSDLWrapperReloadImpl : ", e.getMessage(), e);
                        throw we;
                    }
                }
            }
        }

        return null;
    }


    /**
     * Returns a wsdl reader for the wsdl
     *
     * @return WSDLReader
     * @exception WSDLException
     */
    private WSDLReader getWSDLReader() throws WSDLException {
        WSDLReader reader;
        try {
            reader = AccessController.doPrivileged(
            		new PrivilegedExceptionAction<WSDLReader>() {
            			public WSDLReader run() throws WSDLException {
            				WSDLFactory factory = WSDLFactory.newInstance();
            				return factory.newWSDLReader();
            			}
            		});
        } catch (PrivilegedActionException e) {
            throw (WSDLException) e.getException();
        }
        // prevent system out from occurring
        reader.setFeature(com.ibm.wsdl.Constants.FEATURE_VERBOSE, false);
        return reader;
    }


    /**
     * This method provides a Java2 Security compliant way to obtain the InputStream
     * for a given URLConnection object. This is needed as a given URLConnection object
     * may be an instance of a FileURLConnection object which would require access
     * permissions if Java2 Security was enabled.
     */
    private InputStream getInputStream(URLConnection urlCon) throws Exception {
    	final URLConnection finalURLCon = urlCon;
    	InputStream is = null;
    	try {
    		is = AccessController.doPrivileged(
        			new PrivilegedExceptionAction<InputStream>() {
    					public InputStream run() throws IOException {
    						return finalURLCon.getInputStream();
    					}
        			});
    	}
    	catch(PrivilegedActionException e) {
    		throw e.getException();
    	}
    	return is;
    }

}
