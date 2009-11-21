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

package org.apache.axis2.util;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;

public class ExternalPolicySerializer {

    private List<QName> assertions2Filter = new ArrayList<QName>();

    public void addAssertionToFilter(QName name) {
        assertions2Filter.add(name);
    }

    public void setAssertionsToFilter(List<QName> assertions2Filter) {
        this.assertions2Filter = assertions2Filter;
    }

    public List<QName> getAssertionsToFilter() {
        return assertions2Filter;
    }

    public void serialize(Policy policy, OutputStream os) {
        try {
        	doSerialize((Policy)policy.normalize(false), os);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String initPrefix(XMLStreamWriter writer, String namespace, String fallback)
    	throws XMLStreamException
    {
    	String out = writer.getPrefix(namespace);
    	if (out == null) {
    		out = fallback;
    		writer.setPrefix(out, namespace);
    	}
    	return out;
    }

    private void doSerialize(Policy policy, OutputStream os)
    	throws XMLStreamException, FactoryConfigurationError
    {
    	final XMLStreamWriter writer
    		= XMLOutputFactory.newInstance().createXMLStreamWriter(os);

    	final String wspPrefix = initPrefix(writer, Constants.URI_POLICY_NS, Constants.ATTR_WSP);
    	initPrefix(writer, Constants.URI_WSU_NS, Constants.ATTR_WSU);

    	writer.writeStartElement(Constants.URI_POLICY_NS, Constants.ELEM_POLICY);

    	final Map<QName, String> policyAttrs = policy.getAttributes();
    	final Map<String, String> prefix2ns = new HashMap<String, String>();

    	for (final QName key: policyAttrs.keySet()) {
    		final String namespaceURI = key.getNamespaceURI();
    		final String localName = key.getLocalPart();
    		if (isEmpty(namespaceURI)) {
    			writer.writeAttribute(localName, policy.getAttribute(key));
    		} else {
    			final String writerPrefix = writer.getPrefix(namespaceURI);
    			final String prefix = isEmpty(writerPrefix) ? key.getPrefix()
    														: writerPrefix;

    			final String attrVal = policy.getAttribute(key);
    			if(isEmpty(prefix)) {
    				writer.writeAttribute(namespaceURI, localName, attrVal);
    			} else {
    				writer.writeAttribute(prefix, namespaceURI, localName, attrVal);
    				prefix2ns.put(prefix, key.getNamespaceURI());
    			}
    		}
    	}

    	writer.writeNamespace(wspPrefix, Constants.URI_POLICY_NS);
    	for (Map.Entry<String, String> e: prefix2ns.entrySet()) {
    		writer.writeNamespace(e.getKey(), e.getValue());
    	}

    	writer.writeStartElement(Constants.URI_POLICY_NS, Constants.ELEM_EXACTLYONE);

    	for (Iterator<List<PolicyComponent>> iterator = policy.getAlternatives(); iterator.hasNext();) {
    		final List<PolicyComponent> assertionList = iterator.next();

    		// write <wsp:All>
    		writer.writeStartElement(Constants.URI_POLICY_NS, Constants.ELEM_ALL);

    		for (final PolicyComponent assertion: assertionList) {
    			if (assertions2Filter.contains(((Assertion)assertion).getName())) {
    				// since this is an assertion to filter, we will not serialize this
    				continue;
    			}
    			assertion.serialize(writer);
    		}

    		// write </wsp:All>
    		writer.writeEndElement();
    	}

    	writer.writeEndElement();  //close
    	writer.writeEndElement();
    	writer.flush();
    }

    private boolean isEmpty(String str) {
    	return (str == null) || (str.length() == 0);
    }
}
