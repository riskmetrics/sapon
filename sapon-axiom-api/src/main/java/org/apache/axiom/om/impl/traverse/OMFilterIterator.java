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
 * Abstract iterator that returns matching nodes from another iterator.
 */
public abstract class OMFilterIterator<T>
	implements Iterator<T>, Iterable<T>
{
	private final Iterator<? extends OMNode> parent;
    private T nextItem = null;
    private T lastItem = null;
    private boolean nullWasMatch = false;

    public OMFilterIterator(Iterator<? extends OMNode> parent) {
        this.parent = parent;
    }

    /**
     * Determine whether the given node matches the filter criteria.  If it
     * does, return the node as an instance of <T>.  Otherwise, return null.
     * If null is considered a match, be sure to call markNullAsMatch().
     *
     * @param node the node to test
     * @return true if the node matches, i.e. if it should be returned
     *              by a call to {@link #next()}
     */
    protected abstract T matches(OMNode node);

    protected void markNullAsMatch() {
    	nullWasMatch = true;
    }

    protected void detachLast() {
        if (lastItem == null) {
            throw new IllegalStateException();
        }
        if(lastItem instanceof OMNode) {
        	((OMNode)lastItem).detach();
        }
        lastItem = null;
    }

    public boolean hasNext() {
    	if (nextItem != null || nullWasMatch) {
			return true;
		} else {
			while (parent.hasNext()) {
				OMNode node = parent.next();
				nextItem = matches(node);
				if(nextItem != null || nullWasMatch) {
					return true;
			    }
			}
			return false;
		}
    }

    public T next() {
        if (!hasNext()) {
        	throw new NoSuchElementException();
        }

        lastItem = nextItem;
        nextItem = null;
        nullWasMatch = false;
        return lastItem;
    }

    public void remove() {
    	throw new UnsupportedOperationException();
    }

    public Iterator<T> iterator() {
    	return this;
    }
}
