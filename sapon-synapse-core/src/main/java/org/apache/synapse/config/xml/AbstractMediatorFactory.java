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

package org.apache.synapse.config.xml;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.Nameable;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;

/**
 * Parent class for all the {@link MediatorFactory} implementations
 */
public abstract class AbstractMediatorFactory implements MediatorFactory {

    /** the standard log for mediators, will assign the logger for the actual subclass */
    protected static Log log;
    protected static final QName ATT_NAME    = new QName("name");
    protected static final QName ATT_VALUE   = new QName("value");
    protected static final QName ATT_XPATH   = new QName("xpath");
    protected static final QName ATT_REGEX   = new QName("regex");
    protected static final QName ATT_SEQUENCE = new QName("sequence");
    protected static final QName ATT_EXPRN   = new QName("expression");
    protected static final QName ATT_KEY     = new QName("key");
    protected static final QName ATT_SOURCE  = new QName("source");
    protected static final QName ATT_TARGET  = new QName("target");
    protected static final QName ATT_ONERROR = new QName("onError");
    protected static final QName ATT_STATS
        = new QName(XMLConfigConstants.STATISTICS_ATTRIB_NAME);
    protected static final QName PROP_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");
    protected static final QName FEATURE_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "feature");
    protected static final QName TARGET_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "target");

    /**
     * A constructor that makes subclasses pick up the correct logger
     */
    protected AbstractMediatorFactory() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * This is to Initialize the mediator regarding tracing and statistics.
     *
     * @param mediator of which trace state has to be set
     * @param mediatorOmElement from which the trace state is extracted
     *
     * @since 1.3
     */
    protected void processAuditStatus(Mediator mediator, OMElement mediatorOmElement) {

        String name = null;
        if (mediator instanceof Nameable) {
            name = ((Nameable) mediator).getName();
        }
        if (name == null || "".equals(name)) {
            name = SynapseConstants.ANONYMOUS_SEQUENCE;
        }

        if (mediator instanceof AspectConfigurable) {
            AspectConfiguration configuration = new AspectConfiguration(name);
            ((AspectConfigurable) mediator).configure(configuration);

            OMAttribute statistics = mediatorOmElement.getAttribute(ATT_STATS);
            if (statistics != null) {
                String statisticsValue = statistics.getAttributeValue();
                if (statisticsValue != null) {
                    if (XMLConfigConstants.STATISTICS_ENABLE.equals(statisticsValue)) {
                        configuration.enableStatistics();
                    }
                }
            }
        }
    }

    /**
     * Collect the <tt>name</tt> and <tt>value</tt> attributes from the children
     * with a given QName.
     *
     * @return
     */
    protected Map<String,String> collectNameValuePairs(OMElement elem, QName childElementName) {
        Map<String,String> result = new LinkedHashMap<String,String>();
        for(OMElement child: elem.getChildrenWithName(childElementName)) {
            OMAttribute attName = child.getAttribute(ATT_NAME);
            OMAttribute attValue = child.getAttribute(ATT_VALUE);
            if (attName != null && attValue != null) {
                String name = attName.getAttributeValue().trim();
                String value = attValue.getAttributeValue().trim();
                if (result.containsKey(name)) {
                    handleException("Duplicate " + childElementName.getLocalPart()
                            + " with name " + name);
                } else {
                    result.put(name, value);
                }
            } else {
                handleException("Both of the name and value attributes are required for a "
                        + childElementName.getLocalPart());
            }
        }
        return result;
    }

    protected void handleException(String message, Exception e) {
        LogFactory.getLog(this.getClass()).error(message, e);
        throw new SynapseException(message, e);
    }

    protected void handleException(String message) {
        LogFactory.getLog(this.getClass()).error(message);
        throw new SynapseException(message);
    }
}
