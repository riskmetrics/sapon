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

package org.apache.axiom.om.impl.llom;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMConstants;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;

public class OMAttributeImpl implements OMAttribute {

	private String localName;
    private String value;
    private String type;
    private OMNamespace namespace;
    private QName qName;

    /** <code>OMFactory</code> that created this <code>OMAttribute</code> */
    private OMFactory factory;

    // Keep track of the owner of the attribute
    protected OMElement owner;

    /**
     * Constructor OMAttributeImpl.
     *
     * @param localName
     * @param ns
     * @param value
     */
    public OMAttributeImpl(String localName, OMNamespace ns, String value, OMFactory factory)
    {
        if (localName == null || localName.trim().length() == 0) {
			throw new IllegalArgumentException("Local name may not be null or empty");
		}

        this.localName = localName;
        this.value = value;
        this.namespace = ns;
        this.type = OMConstants.XMLATTRTYPE_CDATA;
        this.factory = factory;
    }

    public QName getQName() {
        if (qName != null) {
            return qName;
        }

        if (namespace != null) {
            // QName throws IllegalArgumentException for null prefixes.
            if (namespace.getPrefix() == null) {
                this.qName = new QName(namespace.getNamespaceURI(), localName);
            } else {
                this.qName = new QName(namespace.getNamespaceURI(), localName, namespace.getPrefix());
            }
        } else {
            this.qName = new QName(localName);
        }
        return this.qName;
    }

    // -------- Getters and Setters

    /**
     * Method getLocalName.
     *
     * @return Returns local name.
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Method setLocalName.
     *
     * @param localName
     */
    public void setLocalName(String localName) {
        if (localName == null || localName.trim().length() == 0) {
			throw new IllegalArgumentException("Local name may not be null or empty");
		}
        this.localName = localName;
        this.qName = null;
    }

    /**
     * Method getAttributeValue.
     *
     * @return Returns value.
     */
    public String getAttributeValue() {
        return value;
    }

    /**
     * Method setAttributeValue.
     *
     * @param value
     */
    public void setAttributeValue(String value) {
        this.value = value;
    }

    /**
     * Method getAttributeType.
     *
     * @return Returns type.
     */
    public String getAttributeType() {
    	return type;
    }

    /**
     * Method setAttributeType.
     *
     * @param type
     */
    public void setAttributeType(String type) {
        this.type = type;
    }

    /**
     * Method setOMNamespace.
     *
     * @param omNamespace
     */
    public void setNamespace(OMNamespace omNamespace) {
        this.namespace = omNamespace;
        this.qName = null;
    }

    /**
     * Method getNamespace.
     *
     * @return Returns namespace.
     */
    public OMNamespace getNamespace() {
        return namespace;
    }

    public OMFactory getOMFactory() {
        return this.factory;
    }

    /**
     * Returns the owner element of this attribute
     * @return OMElement - the owner element
     */
    public OMElement getOwner() {
        return owner;
    }

    /**
     * Checks the equality of two <code>OMAttribute</code> instances.  Returns
     * false for any object not implementing <code>OMAttribute</code>.
     *
     * <p>Equal OMAttributes have equal namespaces, localNames, and values.</p>
     *
     * <p>We ignore the owner when checking for equality because the owner
     * is introduced solely to keep things simple for the programmer and is
     * not logically a part of an attribute itself.</p>
     *
     * @param obj The object to compare with this instance.
     * @return True if obj is equal to this or else false.
     */
    @Override
	public boolean equals(Object obj) {
        if (! (obj instanceof OMAttribute)) {
			return false;
		}
        OMAttribute other = (OMAttribute)obj;

        return (namespace == null ? other.getNamespace() == null
        		                  : namespace.equals(other.getNamespace()))
        	&& localName.equals(other.getLocalName())
        	&& (value == null ? other.getAttributeValue() == null
        			          : value.equals(other.getAttributeValue()));
    }

    @Override
	public int hashCode() {
        return localName.hashCode()
        	 ^ (value != null ? value.hashCode() : 0)
        	 ^ (namespace != null ? namespace.hashCode() : 0);
    }

}
