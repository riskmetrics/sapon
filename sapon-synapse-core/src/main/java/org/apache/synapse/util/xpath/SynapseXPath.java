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

package org.apache.synapse.util.xpath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.llom.OMDocumentImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.jaxen.BaseXPath;
import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.JaxenException;
import org.jaxen.util.SingletonList;

/**
 * <p>XPath that has been used inside Synapse xpath processing. This has a extension function named
 * <code>get-property</code> which is use to retrieve message context properties with the given
 * name from the function</p>
 *
 * <p>For example the following function <code>get-property('prop')</code> can be evaluatedd using
 * an XPath to retrieve the message context property value with the name <code>prop</code>.</p>
 *
 * <p>Apart from that this XPath has a certain set of XPath variables associated with it. They are
 * as follows;
 * <dl>
 *   <dt><tt>body</tt></dt>
 *   <dd>The SOAP 1.1 or 1.2 body element.</dd>
 *   <dt><tt>header</tt></dt>
 *   <dd>The SOAP 1.1 or 1.2 header element.</dd>
 * </dl>
 * </p>
 *
 * <p>Also there are some XPath prefixes defined in <code>SynapseXPath</code> to access various
 * properties using XPath variables, where the variable name represents the particular prefix and
 * the property name as the local part of the variable. Those variables are;
 * <dl>
 *   <dt><tt>ctx</tt></dt>
 *   <dd>Prefix for Synapse MessageContext properties</dd>
 *   <dt><tt>axis2</tt></dt>
 *   <dd>Prefix for Axis2 MessageContext properties</dd>
 *   <dt><tt>trp</tt></dt>
 *   <dd>Prefix for the transport headers</dd>
 * </dl>
 * </p>
 *
 * <p>This XPath is Thread Safe, and provides a special set of evaluate functions for the
 * <code>MessageContext</code> and <code>SOAPEnvelope</code> as well as a method to retrieve
 * string values of the evaluated XPaths</p>
 *
 * @see org.apache.axiom.om.xpath.AXIOMXPath
 * @see #getContext(Object)
 * @see org.apache.synapse.util.xpath.SynapseXPathFunctionContext
 * @see org.apache.synapse.util.xpath.SynapseXPathVariableContext
 */
public class SynapseXPath extends AXIOMXPath {
    private static final long serialVersionUID = 7639226137534334222L;

    private static final Log log = LogFactory.getLog(SynapseXPath.class);

    /**
     * <p>Initializes the <code>SynapseXPath</code> with the given <code>xpathString</code> as the
     * XPath</p>
     *
     * @param xpathString xpath in its string format
     * @throws JaxenException in case of an initialization failure
     */
    public SynapseXPath(String xpathString) throws JaxenException {
        super(xpathString);
    }

    /**
     * Construct an XPath expression from a given string and initialize its
     * namespace context based on a given element.
     *
     * @param element The element that determines the namespace context of the
     *                XPath expression. See {@link #addNamespaces(OMElement)}
     *                for more details.
     * @param xpathExpr the string representation of the XPath expression.
     * @throws JaxenException if there is a syntax error while parsing the expression
     *                        or if the namespace context could not be set up
     */
    public SynapseXPath(OMElement element, String xpathExpr) throws JaxenException {
        super(element, xpathExpr);
    }

    /**
     * Construct an XPath expression from a given attribute.
     * The string representation of the expression is taken from the attribute
     * value, while the attribute's owner element is used to determine the
     * namespace context of the expression.
     *
     * @param attribute the attribute to construct the expression from
     * @throws JaxenException if there is a syntax error while parsing the expression
     *                        or if the namespace context could not be set up
     */
    public SynapseXPath(OMAttribute attribute) throws JaxenException {
        super(attribute);
    }

    public static SynapseXPath parseXPathString(String xPathStr) throws JaxenException {
        if (xPathStr.indexOf('{') == -1) {
            return new SynapseXPath(xPathStr);
        }

        int count = 0;
        StringBuffer newXPath = new StringBuffer();

        Map<String, String> nameSpaces = new HashMap<String, String>();
        String curSegment = null;
        boolean xPath = false;

        StringTokenizer st = new StringTokenizer(xPathStr, "{}", true);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if ("{".equals(s)) {
                xPath = true;
            } else if ("}".equals(s)) {
                xPath = false;
                String prefix = "rp" + count++;
                nameSpaces.put(prefix, curSegment);
                newXPath.append(prefix).append(":");
            } else {
                if (xPath) {
                    curSegment = s;
                } else {
                    newXPath.append(s);
                }
            }
        }

        SynapseXPath synXPath = new SynapseXPath(newXPath.toString());
        for (Map.Entry<String, String> e: nameSpaces.entrySet()) {
            synXPath.addNamespace(e.getKey(), e.getValue());
        }
        return synXPath;
    }

    /**
     * <P>Evaluates the XPath expression against the MessageContext of the current message and
     * returns a String representation of the result</p>
     *
     * @param synCtx the source message which holds the MessageContext against full context
     * @return a String representation of the result of evaluation
     */
    public String stringValueOf(SynapseMessageContext synCtx) {

        try {

            Object result = evaluate(synCtx);

            if (result == null) {
                return null;
            }

            StringBuffer textValue = new StringBuffer();
            if (result instanceof List) {

                List list = (List) result;
                for (Object o : list) {

                    if (o == null && list.size() == 1) {
                        return null;
                    }

                    if (o instanceof OMTextImpl) {
                        textValue.append(((OMTextImpl) o).getText());
                    } else if (o instanceof OMElementImpl) {

                        String s = ((OMElementImpl) o).getText();

                        if (s.trim().length() == 0) {
                            s = o.toString();
                        }
                        textValue.append(s);

                    } else if (o instanceof OMDocumentImpl) {

                        textValue.append(
                            ((OMDocumentImpl) o).getOMDocumentElement().toString());
                    } else if (o instanceof OMAttribute) {
                        textValue.append(
                            ((OMAttribute) o).getAttributeValue());
                    }
                }

            } else {
                textValue.append(result.toString());
            }

            return textValue.toString();

        } catch (JaxenException je) {
            handleException("Evaluation of the XPath expression " + this.toString() +
                " resulted in an error", je);
        }

        return null;
    }

    public void addNamespace(OMNamespace ns) throws JaxenException {
        addNamespace(ns.getPrefix(), ns.getNamespaceURI());
    }

    /**
     * Create a {@link Context} wrapper for the provided object.
     * This methods implements the following class specific behavior:
     * <dl>
     *   <dt>{@link SynapseMessageContext}</dt>
     *   <dd>The XPath expression is evaluated against the SOAP envelope
     *       and the functions and variables defined by
     *       {@link SynapseXPathFunctionContext} and
     *       {@link SynapseXPathVariableContext} are
     *       available.</dd>
     *   <dt>{@link SOAPEnvelope}</dt>
     *   <dd>The variables defined by {@link SynapseXPathVariableContext}
     *       are available.</dd>
     * </dl>
     * For all other object types, the behavior is identical to
     * {@link BaseXPath#getContext(Object)}.
     * <p>
     * Note that the behavior described here also applies to all evaluation
     * methods such as {@link #evaluate(Object)} or {@link #selectSingleNode(Object)},
     * given that these methods all use {@link #getContext(Object)}.
     *
     * @see SynapseXPathFunctionContext#getFunction(String, String, String)
     * @see SynapseXPathVariableContext#getVariableValue(String, String, String)
     */
    @Override
    protected Context getContext(Object obj) {
        if (obj instanceof SynapseMessageContext) {
            SynapseMessageContext synCtx = (SynapseMessageContext)obj;
            ContextSupport baseContextSupport = getContextSupport();
            ContextSupport contextSupport =
                new ContextSupport(baseContextSupport.getNamespaceContext(),
                                   new SynapseXPathFunctionContext(baseContextSupport.getFunctionContext(), synCtx),
                                   new SynapseXPathVariableContext(baseContextSupport.getVariableContext(), synCtx),
                                   baseContextSupport.getNavigator());
            Context context = new Context(contextSupport);
            context.setNodeSet(new SingletonList(synCtx.getEnvelope()));
            return context;
        } else if (obj instanceof SOAPEnvelope) {
            SOAPEnvelope env = (SOAPEnvelope)obj;
            ContextSupport baseContextSupport = getContextSupport();
            ContextSupport contextSupport =
                new ContextSupport(baseContextSupport.getNamespaceContext(),
                                   baseContextSupport.getFunctionContext(),
                                   new SynapseXPathVariableContext(baseContextSupport.getVariableContext(), env),
                                   baseContextSupport.getNavigator());
            Context context = new Context(contextSupport);
            context.setNodeSet(new SingletonList(env));
            return context;
        } else {
            return super.getContext(obj);
        }
    }

    private void handleException(String msg, Throwable e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}