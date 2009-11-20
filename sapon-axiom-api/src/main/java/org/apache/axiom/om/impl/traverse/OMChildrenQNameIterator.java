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

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;

/**
 * Class OMChildrenQNameIterator
 *
 * This iterator returns the elements that have a matching QName.
 * This class can be extended to customize the QName equality.
 *
 */
public class OMChildrenQNameIterator extends OMFilterIterator<OMElement>  {

	private final QName givenQName;

	/**
     * Constructor OMChildrenQNameIterator.
     *
     * @param currentChild
     * @param givenQName
     */
    public OMChildrenQNameIterator(OMNode currentChild, QName givenQName) {
    	this(new OMChildrenIterator(currentChild), givenQName);
    }

    public OMChildrenQNameIterator(Iterator<? extends OMNode> baseIter, QName givenQName) {
    	super(baseIter);
        this.givenQName = givenQName;
    }

    /**
     * Returns true if the qnames are equal.
     * The default algorithm is to use the QName equality (which examines the
     * namespace and localPart).  You can extend this class to provide your own
     * equality algorithm.
     *
     * @param searchQName
     * @param currentQName
     * @return true if qnames are equal.
     */
    public boolean isEqual(QName searchQName, QName currentQName) {
        return searchQName.equals(currentQName);
    }

	@Override
	protected OMElement matches(OMNode node) {
		if(node != null && node instanceof OMElement) {
			QName nodeQName = ((OMElement)node).getQName();
			if(givenQName == null || isEqual(givenQName, nodeQName)) {
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
