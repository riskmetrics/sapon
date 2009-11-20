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

package org.apache.axiom.om;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Defines the base interface used by most of the XML object model within Axis.
 * <p/>
 * <p/>
 * This tree model for XML captures the idea of deferring the construction of child nodes until they
 * are needed. The <code>isComplete</code> function identifies whether or not a particular node has
 * been fully parsed. A node may not be fully parsed, for example, if all of the children of an
 * element have not yet been parsed. </p>
 * <p/>
 * <p/>
 * In comparison to DOM, in this model, you will not find document fragments, or entities. In
 * addition, while {@link OMDocument} and {@link OMAttribute} exist, neither is an extension of
 * <code>OMNode</code>. </p>
 */
public interface OMNode {
    /**
     * The node is an <code>Element</code>.
     *
     * @see #getType()
     */
    static final short ELEMENT_NODE = 1;

    /**
     * The node is a <code>Text</code> node.
     *
     * @see #getType()
     */
    static final short TEXT_NODE = XMLStreamConstants.CHARACTERS;

    /**
     * The node is a <code>CDATASection</code>.
     *
     * @see #getType()
     */
    static final short CDATA_SECTION_NODE = XMLStreamConstants.CDATA;

    /**
     * The node is a <code>Comment</code>.
     *
     * @see #getType()
     */
    static final short COMMENT_NODE = XMLStreamConstants.COMMENT;

    /**
     * This node is a <code>DTD</code>.
     *
     * @see #getType()
     */
    static final short DTD_NODE = XMLStreamConstants.DTD;

    /**
     * This node is a <code>ProcessingInstruction</code>.
     *
     * @see #getType()
     */
    static final short PI_NODE = XMLStreamConstants.PROCESSING_INSTRUCTION;

    /**
     * This node is an <code>Entity Reference</code>.
     *
     * @see #getType()
     */
    static final short ENTITY_REFERENCE_NODE = XMLStreamConstants.ENTITY_REFERENCE;

    /**
     * This node is an <code>Entity Reference</code>.
     *
     * @see #getType()
     */
    static final short SPACE_NODE = XMLStreamConstants.SPACE;

    /**
     * Returns the parent containing node.
     * <p/>
     * <p/>
     * Returns the parent container, which may be either an {@link OMDocument} or {@link OMElement}.
     *
     * @return The {@link OMContainer} of the node.
     */
    OMContainer getParent();

    /**
     * Returns the next sibling in document order.
     *
     * @return Returns the next sibling in document order.
     */
    OMNode getNextOMSibling() throws OMException;

    /**
     * Indicates whether parser has parsed this information item completely or not. If some info are
     * not available in the item, one has to check this attribute to make sure that, this item has been
     * parsed completely or not.
     *
     * @return Returns boolean.
     */
    boolean isComplete();

    /**
     * Removes a node (and all of its children) from its containing parent.
     * <p/>
     * <p/>
     * Removes a node from its parent. Partially complete nodes will be completed before they are
     * detached from the model. A node cannot be detached until its next sibling has been identified,
     * so that the next sibling and parent can be updated appropriately. Please note that this will not
     * handle the namespaces. For example, if there you have used a namespace within the detaching node
     * and which is defined outside the detaching node, user has to handle it manually. </p>
     *
     * @throws OMException If a node is not complete, the detach can trigger further parsing, which may
     *                     cause an exception.
     */
    OMNode detach() throws OMException;

    /**
     * Discards a node.
     * <p/>
     * <p/>
     * Discard goes to the parser level and if the element is not completely built, then it will be
     * completely skipped at the parser level. </p>
     *
     * @throws OMException
     */
    void discard() throws OMException;

    /**
     * Inserts a new sibling after the current node.
     *
     * @param sibling The node that will be added after the current node.
     * @throws OMException
     */
    void insertSiblingAfter(OMNode sibling) throws OMException;

    /**
     * Inserts a sibling just before the current node.
     *
     * @param sibling The node that will be added before the current node.
     * @throws OMException
     */
    void insertSiblingBefore(OMNode sibling) throws OMException;

    /**
     * Returns the type of node.
     *
     * @return Returns one of {@link #ELEMENT_NODE}, {@link #TEXT_NODE}, {@link #CDATA_SECTION_NODE},
     *         {@link #COMMENT_NODE}, {@link #DTD_NODE}, {@link #PI_NODE}, {@link
     *         #ENTITY_REFERENCE_NODE}, {@link #SPACE_NODE}, or {@link #TEXT_NODE}.
     */
    int getType();

    /**
     * Gets the previous sibling.
     *
     * @return Returns node.
     */
    OMNode getPreviousOMSibling();

    /**
     * Serializes the node with caching.
     *
     * @param xmlWriter
     * @throws XMLStreamException
     */
    void serialize(XMLStreamWriter xmlWriter) throws XMLStreamException;

    /**
     * Serializes the node with caching.
     *
     * @param output
     * @throws XMLStreamException
     */
    void serialize(OutputStream output) throws XMLStreamException;

    /**
     * Serializes the node with caching.
     *
     * @param writer
     * @throws XMLStreamException
     */
    void serialize(Writer writer) throws XMLStreamException;

    /**
     * Serializes the node with caching.
     *
     * @param output
     * @param format
     * @throws XMLStreamException
     */
    void serialize(OutputStream output, OMOutputFormat format)
            throws XMLStreamException;

    /**
     * Serializes the node with caching.
     *
     * @param writer
     * @param format
     * @throws XMLStreamException
     */
    void serialize(Writer writer, OMOutputFormat format)
            throws XMLStreamException;

    /**
     * Serializes the node without caching.
     *
     * @param xmlWriter
     * @throws XMLStreamException
     */
    void serializeAndConsume(XMLStreamWriter xmlWriter)
            throws XMLStreamException;

    /**
     * Serializes the node without caching.
     *
     * @param output
     * @throws XMLStreamException
     */
    void serializeAndConsume(OutputStream output)
            throws XMLStreamException;

    /**
     * Serializes the node without caching.
     *
     * @param writer
     * @throws XMLStreamException
     */
    void serializeAndConsume(Writer writer) throws XMLStreamException;

    /**
     * Serializes the node without caching.
     *
     * @param output
     * @param format
     * @throws XMLStreamException
     */
    void serializeAndConsume(OutputStream output, OMOutputFormat format)
            throws XMLStreamException;

    /**
     * Serializes the node without caching.
     *
     * @param writer
     * @param format
     * @throws XMLStreamException
     */
    void serializeAndConsume(Writer writer, OMOutputFormat format)
            throws XMLStreamException;

    /** Builds itself. */
    void build();

    /**
     * Builds itself with the OMText binary content. AXIOM supports two levels of deffered building.
     * First is deffered building of AXIOM using StAX. Second level is the deffered building of
     * attachments. AXIOM reads in the attachements from the stream only when user asks by calling
     * getDataHandler(). build() method builds the OM without the attachments. buildAll() builds the OM
     * together with attachement data. This becomes handy when user wants to free the input stream.
     */
    void buildWithAttachments();

    /**
     * If a builder and parser is associated with the node, it is closed.
     * @param build if true, the object is built first before closing the builder/parser
     */
    void close(boolean build);
    
    /**
     * Returns the OMFactory that created this object
     */
    OMFactory getOMFactory();

}
