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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.ValidateMediator;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

/**
 * Factory for {@link ValidateMediator} instances.
 * <p>
 * Configuration syntax:
 * <pre>
 * &lt;validate [source="xpath"]>
 *   &lt;schema key="string">+
 *   &lt;property name="<validation-feature-name>" value="true|false"/>
 *   &lt;on-fail>
 *     mediator+
 *   &lt;/on-fail>
 * &lt;/validate>
 * </pre>
 */
public class ValidateMediatorFactory extends AbstractListMediatorFactory {

    private static final QName VALIDATE_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "validate");
    private static final QName ON_FAIL_Q  = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "on-fail");
    private static final QName SCHEMA_Q   = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "schema");

    public Mediator createMediator(OMElement elem) {

        ValidateMediator validateMediator = new ValidateMediator();

        // process schema element definitions and create DynamicProperties
        List<String> schemaKeys = new ArrayList<String>();
        for(OMElement omElem: elem.getChildrenWithName(SCHEMA_Q)) {
        	OMAttribute keyAtt = omElem.getAttribute(ATT_KEY);
        	if (keyAtt != null) {
        		schemaKeys.add(keyAtt.getAttributeValue());
            } else {
            	handleException("A 'schema' definition must contain a local property 'key'");
            }
        }

        if (schemaKeys.size() == 0) {
            handleException("No schemas specified for the validate mediator");
        } else {
            validateMediator.setSchemaKeys(schemaKeys);
        }

        // process source XPath attribute if present
        OMAttribute attSource = elem.getAttribute(ATT_SOURCE);

        if (attSource != null) {
            try {
                validateMediator.setSource(SynapseXPathFactory.getSynapseXPath(elem, ATT_SOURCE));
            } catch (JaxenException e) {
                handleException("Invalid XPath expression specified for attribute 'source'", e);
            }
        }

        // process on-fail
        OMElement onFail = null;
        Iterator<OMElement> iterator = elem.getChildrenWithName(ON_FAIL_Q).iterator();
        if (iterator.hasNext()) {
            onFail = iterator.next();
        }

        if (onFail != null) {
            addChildren(onFail, validateMediator);
        } else {
            handleException("A non-empty <on-fail> child element is required for " +
                "the <validate> mediator");
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processAuditStatus(validateMediator,elem);
        // set the features
        for (Map.Entry<String,String> entry : collectNameValuePairs(elem, FEATURE_Q).entrySet()) {
            String value = entry.getValue();
            boolean isFeatureEnabled;
            if ("true".equals(value)) {
                isFeatureEnabled = true;
            } else if ("false".equals(value)) {
                isFeatureEnabled = false;
            } else {
                handleException("The feature must have value true or false");
                break;
            }
            try {
                validateMediator.addFeature(entry.getKey(), isFeatureEnabled);
            } catch (SAXException e) {
                handleException("Error setting validation feature : " + entry.getKey()
                        + " to : " + value, e);
            }
        }
        return validateMediator;
    }

    public QName getTagQName() {
        return VALIDATE_Q;
    }
}
