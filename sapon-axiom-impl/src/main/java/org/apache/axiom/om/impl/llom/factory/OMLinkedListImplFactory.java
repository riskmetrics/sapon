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

package org.apache.axiom.om.impl.llom.factory;

import java.util.Hashtable;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMComment;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMDocType;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMProcessingInstruction;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.OMNamespaceImpl;
import org.apache.axiom.om.impl.llom.OMAttributeImpl;
import org.apache.axiom.om.impl.llom.OMCommentImpl;
import org.apache.axiom.om.impl.llom.OMDocTypeImpl;
import org.apache.axiom.om.impl.llom.OMDocumentImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMProcessingInstructionImpl;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;
import org.apache.axiom.om.impl.llom.OMTextImpl;

public class OMLinkedListImplFactory
	implements OMFactory
{
    private static final String uriAndPrefixSeparator = ";";

    // Pooling of OMNamespace objects is disabled.  See the comment in OMNamespace.
    private static boolean POOL_OMNAMESPACES = false;

    /**
     * This is a map of namespaces with the namespace URI as the key and
     * Namespace object itself as the value.
	 *
     * OMFactories are shared across threads.  The Hashtable is necessary to
     * prevent concurrent modification exceptions.
     */
    protected Map<String, OMNamespace> namespaceTable
    	= new Hashtable<String, OMNamespace>(5);

    /**
     * Method createOMElement.
     *
     * @param localName
     * @param ns
     * @return Returns OMElement.
     */
    public OMElement createOMElement(String localName, OMNamespace ns) {
        return new OMElementImpl(localName, ns, this);
    }

    public OMElement createOMElement(String localName, OMNamespace ns, OMContainer parent) {
        return new OMElementImpl(localName, ns, parent, this);
    }

    /**
     * Method createOMElement.
     *
     * @param localName
     * @param ns
     * @param parent
     * @param builder
     * @return Returns OMElement.
     */
    public OMElement createOMElement(String localName, OMNamespace ns,
                                     OMContainer parent,
                                     OMXMLParserWrapper builder) {
        return new OMElementImpl(localName, ns, parent,
                                 builder, this);
    }

    /**
     * Method createOMElement.
     *
     * @param localName
     * @param namespaceURI
     * @param namespacePrefix
     * @return Returns OMElement.
     */
    public OMElement createOMElement(String localName, String namespaceURI,
                                     String namespacePrefix) {
        return this.createOMElement(localName,
                                    this.createOMNamespace(namespaceURI,
                                                           namespacePrefix));
    }

    /**
     * Create an OMElement with the given QName under the given parent.
     *
     * If the QName contains a prefix, we will ensure that an OMNamespace is created
     * mapping the given namespace to the given prefix.  If no prefix is passed, we'll
     * use whatever's already mapped in the parent, or create a generated one.
     *
     * @param qname the QName of the element to create
     * @param parent the OMContainer in which to place the new element
     * @return Returns the new OMElement
     * @throws OMException if there's a namespace mapping problem
     */
    public OMElement createOMElement(QName qname, OMContainer parent)
            throws OMException {
        return new OMElementImpl(qname, parent, this);
    }

    /**
     * Create an OMElement with the given QName
     * <p/>
     * If the QName contains a prefix, we will ensure that an OMNamespace is created mapping the
     * given namespace to the given prefix.  If no prefix is passed, we'll use whatever's already
     * mapped in the parent, or create a generated one.
     *
     * @param qname
     * @return the new OMElement.
     */
    public OMElement createOMElement(QName qname) throws OMException {
        return new OMElementImpl(qname, null, this);
    }

    /**
     * Construct element with arbitrary data source.
     *
     * @param source
     * @param localName
     * @param ns
     */
    public OMSourcedElement createOMElement(OMDataSource source, String localName, OMNamespace ns) {
        return new OMSourcedElementImpl(localName, ns, this, source);
    }

    /**
     * Method createOMNamespace.
     *
     * @param uri
     * @param prefix
     * @return Returns OMNamespace.
     */
    public OMNamespace createOMNamespace(String uri, String prefix) {
        // An OMNamespaceImpl consists of only two String objects;
        // The overhead to create "yet another" key string and pool these
        // small objects is unnecessary.  In addition,
        // the objects are never freed from the pool, which means that the
        // the table will grow very large over time.  For this reason, the
        // pooling of OMNamespaces is disabled.

        if (POOL_OMNAMESPACES) {
            String key = uri;
            if (prefix != null && prefix.length() > 0) {
                key = key + uriAndPrefixSeparator + prefix;
            }
            OMNamespace existingNamespaceObject = namespaceTable.get(key);
            if (existingNamespaceObject == null) {
                existingNamespaceObject = new OMNamespaceImpl(uri, prefix);
                namespaceTable.put(key, existingNamespaceObject);
            }
            return existingNamespaceObject;
        } else {
            return new OMNamespaceImpl(uri, prefix);
        }
    }

    /**
     * Method createOMText.
     *
     * @param parent
     * @param text
     * @return Returns OMText.
     */
    public OMText createOMText(OMContainer parent, String text) {
        return new OMTextImpl(parent, text, this);
    }

    public OMText createOMText(OMContainer parent, QName text) {
        return new OMTextImpl(parent, text, this);
    }

    public OMText createOMText(OMContainer parent, String text, int type) {
        return new OMTextImpl(parent, text, type, this);
    }

    public OMText createOMText(OMContainer parent, char[] charArary, int type) {
        return new OMTextImpl(parent, charArary, type, this);
    }

    public OMText createOMText(OMContainer parent, QName text, int type) {
        return new OMTextImpl(parent, text, type, this);
    }

    /**
     * Method createOMText.
     *
     * @param s
     * @return Returns OMText.
     */
    public OMText createOMText(String s) {
        return new OMTextImpl(s, this);
    }

    public OMText createOMText(String s, int type) {
        return new OMTextImpl(s, type, this);
    }

    /**
     * Creates text.
     *
     * @param s
     * @param mimeType
     * @param optimize
     * @return Returns OMText.
     */
    public OMText createOMText(String s, String mimeType, boolean optimize) {
        return new OMTextImpl(s, mimeType, optimize, this);
    }

    /**
     * Creates text.
     *
     * @param dataHandler
     * @param optimize
     * @return Returns OMText.
     */
    public OMText createOMText(Object dataHandler, boolean optimize) {
        return new OMTextImpl(dataHandler, optimize, this);
    }

    public OMText createOMText(String contentID, OMContainer parent,
                               OMXMLParserWrapper builder) {
        return new OMTextImpl(contentID, parent, builder, this);
    }

    /**
     * Create OMText node that is a copy of the source text node
     * @param parent
     * @param source
     * @return
     */
    public OMText createOMText(OMContainer parent, OMText source) {
        return new OMTextImpl(parent, (OMTextImpl) source, this);
    }

    /**
     * Creates text.
     *
     * @param parent
     * @param s
     * @param mimeType
     * @param optimize
     * @return Returns OMText.
     */
    public OMText createOMText(OMContainer parent,
                               String s,
                               String mimeType,
                               boolean optimize) {
        return new OMTextImpl(parent, s, mimeType, optimize, this);
    }

    /**
     * Creates attribute.
     *
     * @param localName
     * @param ns
     * @param value
     * @return Returns OMAttribute.
     */
    public OMAttribute createOMAttribute(String localName,
                                         OMNamespace ns,
                                         String value) {
        return new OMAttributeImpl(localName, ns, value, this);
    }

    /**
     * Creates DocType/DTD.
     *
     * @param parent
     * @param content
     * @return Returns doctype.
     */
    public OMDocType createOMDocType(OMContainer parent, String content) {
        return new OMDocTypeImpl(parent, content, this);
    }

    /**
     * Creates a PI.
     *
     * @param parent
     * @param piTarget
     * @param piData
     * @return Returns OMProcessingInstruction.
     */
    public OMProcessingInstruction createOMProcessingInstruction(OMContainer parent,
                                                                 String piTarget, String piData) {
        return new OMProcessingInstructionImpl(parent, piTarget, piData, this);
    }

    /**
     * Creates a comment.
     *
     * @param parent
     * @param content
     * @return Returns OMComment.
     */
    public OMComment createOMComment(OMContainer parent, String content) {
        return new OMCommentImpl(parent, content, this);
    }

    /* (non-Javadoc)
    * @see org.apache.axiom.om.OMFactory#createOMDocument()
    */
    public OMDocument createOMDocument() {
        return new OMDocumentImpl();
    }

    /* (non-Javadoc)
      * @see org.apache.axiom.om.OMFactory#createOMDocument(org.apache.axiom.om.OMXMLParserWrapper)
      */
    public OMDocument createOMDocument(OMXMLParserWrapper builder) {
        return new OMDocumentImpl(builder);
    }
}
