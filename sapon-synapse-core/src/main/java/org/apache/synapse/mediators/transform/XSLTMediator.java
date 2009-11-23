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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.util.jaxp.DOOMResultBuilderFactory;
import org.apache.synapse.util.jaxp.DOOMSourceBuilderFactory;
import org.apache.synapse.util.jaxp.ResultBuilder;
import org.apache.synapse.util.jaxp.ResultBuilderFactory;
import org.apache.synapse.util.jaxp.SourceBuilder;
import org.apache.synapse.util.jaxp.SourceBuilderFactory;
import org.apache.synapse.util.jaxp.StreamResultBuilderFactory;
import org.apache.synapse.util.jaxp.StreamSourceBuilderFactory;
import org.apache.synapse.util.resolver.CustomJAXPURIResolver;
import org.apache.synapse.util.resolver.ResourceMap;
import org.apache.synapse.util.xpath.SourceXPathSupport;
import org.apache.synapse.util.xpath.SynapseXPath;

/**
 * The XSLT mediator performs an XSLT transformation requested, using
 * the current message. The source attribute (if available) specifies the
 * source element on which the transformation would be applied. It will default
 * to the first child of the messages' SOAP body, if it is omitted.
 *
 * Additional properties passed into this mediator would become parameters for
 * XSLT.  Additional features passed into this mediator would become features
 * except for "http://ws.apache.org/ns/synapse/transform/feature/dom" for the
 * Transformer Factory, which is used to decide between using DOM and Streams
 * during the transformation process. By default this is turned on as an
 * optimization, but should be set to false if issues are detected
 *
 *  Note: Set the TransformerFactory system property to generate and use
 *  translets
 *  -Djavax.xml.transform.TransformerFactory=org.apache.xalan.xsltc.trax.TransformerFactoryImpl
 */
public class XSLTMediator extends AbstractMediator {

    private static class ErrorListenerImpl implements ErrorListener {
        private final String activity;

        public ErrorListenerImpl(String activity) {
            this.activity = activity;
        }

        public void warning(TransformerException e) throws TransformerException {
            if (log.isDebugEnabled()) {
                log.debug("Warning encountered during " + activity + " : " + e);
            }
        }

        public void error(TransformerException e) throws TransformerException {
            log.error("Error occured in " + activity + " : " + e);
            throw e;
        }

        public void fatalError(TransformerException e) throws TransformerException {
            log.error("Fatal error occured in " + activity + " : " + e);
            throw e;
        }
    }

    /**
     * The feature for which deciding switching between DOM and Stream during
     * the transformation process
     */
    public static final String USE_DOM_SOURCE_AND_RESULTS =
        "http://ws.apache.org/ns/synapse/transform/feature/dom";

    /**
     * The attribute that allows specifying the {@link SourceBuilderFactory}.
     */
    public static final String SOURCE_BUILDER_FACTORY =
        "http://ws.apache.org/ns/synapse/transform/attribute/sbf";

    /**
     * The attribute that allows specifying the {@link ResultBuilderFactory}.
     */
    public static final String RESULT_BUILDER_FACTORY =
        "http://ws.apache.org/ns/synapse/transform/attribute/rbf";

    /**
     * The resource key/name which refers to the XSLT to be used for the transformation
     */
    private String xsltKey = null;

    /**
     * The (optional) XPath expression which yields the source element for a transformation
     */
    private final SourceXPathSupport source = new SourceXPathSupport();

    /**
     * The name of the message context property to store the transformation result
     */
    private String targetPropertyName = null;

    /**
     * Any parameters which should be passed into the XSLT transformation
     */
    private final List<MediatorProperty> properties = new ArrayList<MediatorProperty>();

    /**
     * Any features which should be set to the TransformerFactory explicitly
     */
    private final List<MediatorProperty> transformerFactoryFeatures = new ArrayList<MediatorProperty>();

    /**
     * Any attributes which should be set to the TransformerFactory explicitly
     */
    private final List<MediatorProperty> transformerFactoryAttributes
                = new ArrayList<MediatorProperty>();

    /**
     * A resource map used to resolve xsl:import and xsl:include.
     */
    private ResourceMap resourceMap;

    /**
     * The Template instance used to create a Transformer object. This is  thread-safe
     *
     * @see javax.xml.transform.Templates
     */
    private Templates cachedTemplates = null;

    /**
     * The TransformerFactory instance which use to create Templates...This is not thread-safe.
     * @see javax.xml.transform.TransformerFactory
     */
    private final TransformerFactory transFact = TransformerFactory.newInstance();

    /**
     * Lock used to ensure thread-safe creation and use of the above Transformer
     */
    private final Object transformerLock = new Object();

    /**
     * The source builder factory to use.
     */
    private SourceBuilderFactory sourceBuilderFactory = new StreamSourceBuilderFactory();

    /**
     * The result builder factory to use.
     */
    private ResultBuilderFactory resultBuilderFactory = new StreamResultBuilderFactory();

    /**
     * Transforms this message (or its element specified as the source) using the
     * given XSLT transformation
     *
     * @param synCtx the current message where the transformation will apply
     * @return true always
     */
    public boolean mediate(SynapseMessageContext synCtx)
    {
        log.debug("Start : XSLT mediator");
        if (log.isTraceEnabled()) {
            log.trace("Message : " + synCtx.getEnvelope());
        }

        try {
            performXSLT(synCtx);
        } catch (Exception e) {
            handleException("Unable to perform XSLT transformation using : " + xsltKey +
                " against source XPath : " + source, e, synCtx);
        }

        log.debug("End : XSLT mediator");
        return true;
    }

    /**
     * Perform actual XSLT transformation
     * @param synCtx current message
     * @param synLog the logger to be used
     */
    private void performXSLT(SynapseMessageContext synCtx) {

        OMNode sourceNode = source.selectOMNode(synCtx);
        if(sourceNode == null) {
        	log.warn("No node was selected for XSLT transformation.");
        	return;
        }
        boolean isSoapEnvelope = (sourceNode == synCtx.getEnvelope());
        boolean isSoapBody = (sourceNode == synCtx.getEnvelope().getBody());

        if (log.isTraceEnabled()) {
            log.trace("Transformation source : " + sourceNode.toString());
        }

        // determine if it is needed to create or create the template
        if (isCreationOrRecreationRequired(synCtx)) {
            // many threads can see this and come here for acquiring the lock
            synchronized (transformerLock) {
                // only first thread should create the template
                if (isCreationOrRecreationRequired(synCtx)) {
                    createTemplate(synCtx);
                }
            }
        }

        try {
            // perform transformation
            Transformer transformer = cachedTemplates.newTransformer();
            if (!properties.isEmpty()) {
                // set the parameters which will pass to the Transformation
                applyProperties(transformer, synCtx);
            }

            transformer.setErrorListener(new ErrorListenerImpl("XSLT transformation"));

            String outputMethod = transformer.getOutputProperty(OutputKeys.METHOD);
            String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);

            if (log.isDebugEnabled()) {
                log.debug("output method: " + outputMethod
                        + "; encoding: " + encoding);
            }

            ResultBuilderFactory.Output output;
            if ("text".equals(outputMethod)) {
                log.debug("Processing non SOAP/XML (text) transformation result");
                output = ResultBuilderFactory.Output.TEXT;
            } else if (isSoapEnvelope) {
                output = ResultBuilderFactory.Output.SOAP_ENVELOPE;
            } else {
                output = ResultBuilderFactory.Output.ELEMENT;
            }

            SynapseEnvironment synEnv = synCtx.getEnvironment();
            ResultBuilder resultBuilder =
                    resultBuilderFactory.createResultBuilder(synEnv, output);
            SourceBuilder sourceBuilder = sourceBuilderFactory.createSourceBuilder(synEnv);

            if (log.isDebugEnabled()) {
                log.debug("Using " + sourceBuilder.getClass().getName());
                log.debug("Using " + resultBuilder.getClass().getName());
            }

            try {
                transformer.transform(sourceBuilder.getSource((OMElement)sourceNode),
                                      resultBuilder.getResult());
            } finally {
                sourceBuilder.release();
            }

            log.debug("Transformation completed - processing result");

            // get the result OMElement
            OMElement result =
                resultBuilder.getNode(encoding == null ? null : Charset.forName(encoding));

            if (log.isTraceEnabled()) {
                log.trace("Transformation result : " + result.toString());
            }

            if (targetPropertyName != null) {
                // add result XML as a message context property to the message
                if (log.isDebugEnabled()) {
                    log.debug("Adding result as message context property : " +
                        targetPropertyName);
                }
                synCtx.setProperty(targetPropertyName, result);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Replace " +
                        (isSoapEnvelope ? "SOAP envelope" : isSoapBody ? "SOAP body" : "node")
                        + " with result");
                }

                if (isSoapEnvelope) {
                    try {
                        synCtx.setEnvelope((SOAPEnvelope) result);
                    } catch (AxisFault ex) {
                        handleException("Unable to replace SOAP envelope with result", ex, synCtx);
                    }

                } else if (isSoapBody) {
                    for(OMElement child: synCtx.getEnvelope().getBody().getChildElements()) {
                        child.detach();
                    }

                    for(OMElement child: result.getChildElements()) {
                        synCtx.getEnvelope().getBody().addChild(child);
                    }

                } else {
                    sourceNode.insertSiblingAfter(result);
                    sourceNode.detach();
                }
            }

        } catch (TransformerException e) {
            handleException("Error performing XSLT transformation using : " + xsltKey, e, synCtx);
        }
    }

    /**
     * Create a XSLT template object and assing it to the cachedTemplates variable
     * @param synCtx current message
     * @param synLog logger to use
     */
    private void createTemplate(SynapseMessageContext synCtx) {
        // Set an error listener (SYNAPSE-307).
        transFact.setErrorListener(new ErrorListenerImpl("stylesheet parsing"));
        // Allow xsl:import and xsl:include resolution
        transFact.setURIResolver(new CustomJAXPURIResolver(resourceMap,
                synCtx.getConfiguration()));
        try {
            cachedTemplates = transFact.newTemplates(
                    SynapseConfigUtils.getStreamSource(synCtx.getEntry(xsltKey)));
            if (cachedTemplates == null) {
                handleException("Error compiling the XSLT with key : " + xsltKey, synCtx);
            }
        } catch (Exception e) {
            handleException("Error creating XSLT transformer using : " + xsltKey, e, synCtx);
        }
    }

    /**
     * Utility method to determine weather it is needed to create a XSLT template
     * @param synCtx current message
     * @return true if it is needed to create a new XSLT template
     */
    private boolean isCreationOrRecreationRequired(SynapseMessageContext synCtx) {

        // if there are no cachedTemplates we need to create a one
        if (cachedTemplates == null) {
            // this is a creation case
            return true;
        } else {
            // build transformer - if necessary
            Entry dp = synCtx.getConfiguration().getEntryDefinition(xsltKey);
            // if the xsltKey refers to a dynamic resource, and if it has been expired
            // it is a recreation case
            return dp != null && dp.isDynamic() && (!dp.isCached() || dp.isExpired());
        }
    }

    public SynapseXPath getSource() {
        return source.getXPath();
    }

    public void setSource(SynapseXPath source) {
        this.source.setXPath(source);
    }

    public String getXsltKey() {
        return xsltKey;
    }

    public void setXsltKey(String xsltKey) {
        this.xsltKey = xsltKey;
    }

    public void addProperty(MediatorProperty p) {
        properties.add(p);
    }

    /**
     * Set the properties defined in the mediator as parameters on the stylesheet.
     *
     * @param transformer Transformer instance
     * @param synCtx MessageContext instance
     * @param synLog SynapseLog instance
     */
    private void applyProperties(Transformer transformer, SynapseMessageContext synCtx)
    {
        for (MediatorProperty prop : properties) {
            if (prop != null) {
                String value;
                if (prop.getValue() != null) {
                    value = prop.getValue();
                } else {
                    value = prop.getExpression().stringValueOf(synCtx);
                }
                if (log.isDebugEnabled()) {
                    if (value == null) {
                        log.debug("Not setting parameter '" + prop.getName() + "'");
                    } else {
                        log.debug("Setting parameter '" + prop.getName() + "' to '"
                                + value + "'");
                    }
                }
                if (value != null) {
                    transformer.setParameter(prop.getName(), value);
                }
            }
        }
    }

    /**
     * Add a feature to be set on the {@link TransformerFactory} used by this mediator instance.
     * This method can also be used to enable some Synapse specific optimizations and
     * enhancements as described in the documentation of this class.
     *
     * @param featureName The name of the feature
     * @param isFeatureEnable the desired state of the feature
     *
     * @see TransformerFactory#setFeature(String, boolean)
     * @see XSLTMediator
     */
    public void addFeature(String featureName, boolean isFeatureEnable) {
        MediatorProperty mp = new MediatorProperty();
        mp.setName(featureName);
        if (isFeatureEnable) {
            mp.setValue("true");
        } else {
            mp.setValue("false");
        }
        transformerFactoryFeatures.add(mp);
        if (USE_DOM_SOURCE_AND_RESULTS.equals(featureName)) {
            if (isFeatureEnable) {
                sourceBuilderFactory = new DOOMSourceBuilderFactory();
                resultBuilderFactory = new DOOMResultBuilderFactory();
            }
        } else {
            try {
                transFact.setFeature(featureName, isFeatureEnable);
            } catch (TransformerConfigurationException e) {
                String msg = "Error occured when setting features to the TransformerFactory";
                log.error(msg, e);
                throw new SynapseException(msg, e);
            }
        }
    }

    /**
     * Add an attribute to be set on the {@link TransformerFactory} used by this mediator instance.
     * This method can also be used to enable some Synapse specific optimizations and
     * enhancements as described in the documentation of this class.
     *
     * @param name The name of the feature
     * @param value should this feature enable?
     *
     * @see TransformerFactory#setAttribute(String, Object)
     * @see XSLTMediator
     */
    public void addAttribute(String name, String value) {
        MediatorProperty mp = new MediatorProperty();
        mp.setName(name);
        mp.setValue(value);
        transformerFactoryAttributes.add(mp);
        if (SOURCE_BUILDER_FACTORY.equals(name) || RESULT_BUILDER_FACTORY.equals(name)) {
            Object instance;
            try {
                instance = Class.forName(value).newInstance();
            } catch (ClassNotFoundException e) {
                String msg = "The class specified by the " + name + " attribute was not found";
                log.error(msg, e);
                throw new SynapseException(msg, e);
            } catch (Exception e) {
                String msg = "The class " + value + " could not be instantiated";
                log.error(msg, e);
                throw new SynapseException(msg, e);
            }
            if (SOURCE_BUILDER_FACTORY.equals(name)) {
                sourceBuilderFactory = (SourceBuilderFactory)instance;
            } else {
                resultBuilderFactory = (ResultBuilderFactory)instance;
            }
        } else {
            try {
                transFact.setAttribute(name, value);
            } catch (IllegalArgumentException e) {
                String msg = "Error occured when setting attribute to the TransformerFactory";
                log.error(msg, e);
                throw new SynapseException(msg, e);
            }
        }
    }

    /**
     * @return Return the features explicitly set to the TransformerFactory through this mediator.
     */
    public List<MediatorProperty> getFeatures(){
        return transformerFactoryFeatures;
    }

    /**
     * @return Return the attributes explicitly set to the TransformerFactory through this mediator.
     */
    public List<MediatorProperty> getAttributes(){
        return transformerFactoryAttributes;
    }

    public void addAllProperties(List<MediatorProperty> list) {
        properties.addAll(list);
    }

    public List<MediatorProperty> getProperties() {
        return properties;
    }

    public String getTargetPropertyName() {
        return targetPropertyName;
    }

    public void setTargetPropertyName(String targetPropertyName) {
        this.targetPropertyName = targetPropertyName;
    }

    public ResourceMap getResourceMap() {
        return resourceMap;
    }

    public void setResourceMap(ResourceMap resourceMap) {
        this.resourceMap = resourceMap;
    }
}


