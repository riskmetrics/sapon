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

package org.apache.axiom.om.impl.traverse;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;

public class OMChildrenWithSpecificAttributeIterator
	extends OMFilterIterator<OMElement>
{
	private final QName attributeName;
    private final String attributeValue;
    private final boolean detach;

    private boolean doCaseSensitiveValueChecks = true;

    /**
     * Constructor OMChildrenWithSpecificAttributeIterator.
     *
     * @param currentChild
     * @param attributeName
     * @param attributeValue
     * @param detach
     */
    public OMChildrenWithSpecificAttributeIterator(OMNode currentChild,
                                                   QName attributeName,
                                                   String attributeValue,
                                                   boolean detach )
    {
        super(new OMChildrenIterator(currentChild));
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
        this.detach = detach;
    }

    public void setCaseInsensitiveValueChecks(boolean val) {
        doCaseSensitiveValueChecks = val;
    }

    @Override
	public OMElement next() {
    	OMElement out = super.next();
    	if(detach) {
    		detachLast();
    	}
    	return out;
    }

    @Override
	public OMElement matches(OMNode node) {
        if (node instanceof OMElement) {
            OMAttribute attr =
                    ((OMElement) node).getAttribute(attributeName);
            if ((attr != null) && (doCaseSensitiveValueChecks ?
                    attr.getAttributeValue().equals(attributeValue) :
                    attr.getAttributeValue().equalsIgnoreCase(attributeValue))) {
                return (OMElement)node;
            }
        }
        return null;
    }

    @Override
	public void remove() {
    	detachLast();
    }
}
