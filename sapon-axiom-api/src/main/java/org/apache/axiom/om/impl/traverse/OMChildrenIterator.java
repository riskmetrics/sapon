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

import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNode;

public class OMChildrenIterator implements Iterator<OMNode>, Iterable<OMNode> {

    protected OMNode currentChild;
    protected OMNode lastChild;

    protected boolean nextCalled = false;
    protected boolean removeCalled = false;

    public OMChildrenIterator(OMNode currentChild) {
        this.currentChild = currentChild;
    }

    /**
     * Removes the last element returned by the iterator (optional operation) from the underlying
     * collection.   This method must be called only once per call to <tt>next</tt>.  The behavior
     * of an iterator is unspecified if the underlying collection is modified while the iteration is
     * in progress in any way other than by calling this method.
     *
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is not supported by
     *                                       this Iterator.
     * @throws IllegalStateException         if the <tt>next</tt> method has not yet been called, or
     *                                       the <tt>remove</tt> method has already been called
     *                                       after the last call to the <tt>next</tt> method.
     */
    public void remove() {
        if (!nextCalled) {
            throw new IllegalStateException(
                    "next method has not yet being called");
        }
        if (removeCalled) {
            throw new IllegalStateException("remove has already being called");
        }
        removeCalled = true;

        // since this acts on the last child there is no need to mess with the current child
        if (lastChild == null) {
            throw new OMException("cannot remove a child at this stage!");
        }
        lastChild.detach();
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other words, returns
     * <tt>true</tt> if <tt>next</tt> would return an element rather than throwing an exception.)
     *
     * @return Returns <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return (currentChild != null);
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return Returns the next element in the iteration.
     * @throws java.util.NoSuchElementException
     *          iteration has no more elements.
     */
    public OMNode next() {
        nextCalled = true;
        removeCalled = false;
        if (hasNext()) {
            lastChild = currentChild;
            currentChild = currentChild.getNextOMSibling();
            return lastChild;
        } else {
            throw new NoSuchElementException();
        }
    }

    public Iterator<OMNode> iterator() {
    	return this;
    }
}
