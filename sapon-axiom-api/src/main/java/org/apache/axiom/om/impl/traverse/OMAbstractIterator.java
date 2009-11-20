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
import java.util.NoSuchElementException;

import org.apache.axiom.om.OMNode;

/**
 * Abstract base class for iterators over sets of OM nodes.
 */
// TODO: We should implement remove and use this as base class for OMChildrenIterator
public abstract class OMAbstractIterator<T extends OMNode>
	implements Iterator<T>, Iterable<T>
{
    private T currentNode;
    private T nextNode;
    private boolean noMoreNodes;

    public OMAbstractIterator(T firstNode) {
        if (firstNode == null) {
            noMoreNodes = true;
        } else {
            nextNode = firstNode;
        }
    }

    /**
     * Get the next node.
     *
     * @param currentNode the predecessor of the node to retrieve
     * @return the next node
     */
    protected abstract T getNextNode(T currentNode);

    public boolean hasNext() {
        if (noMoreNodes) {
            return false;
        } else if (nextNode != null) {
            return true;
        } else {
            nextNode = getNextNode(currentNode);
            noMoreNodes = nextNode == null;
            return !noMoreNodes;
        }
    }

    public T next() {
        if (hasNext()) {
            currentNode = nextNode;
            nextNode = null;
            return currentNode;
        } else {
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Iterator<T> iterator() {
    	return this;
    }
}
