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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMConstants;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.OMXMLStreamReader;
import org.apache.axiom.om.impl.OMContainerEx;
import org.apache.axiom.om.impl.OMNamespaceImpl;
import org.apache.axiom.om.impl.OMNodeEx;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.builder.XOPAwareStAXOMBuilder;
import org.apache.axiom.om.impl.builder.XOPBuilder;
import org.apache.axiom.om.impl.llom.factory.OMLinkedListImplFactory;
import org.apache.axiom.om.impl.traverse.OMChildElementIterator;
import org.apache.axiom.om.impl.traverse.OMChildrenIterator;
import org.apache.axiom.om.impl.traverse.OMChildrenLocalNameIterator;
import org.apache.axiom.om.impl.traverse.OMChildrenNamespaceIterator;
import org.apache.axiom.om.impl.traverse.OMChildrenQNameIterator;
import org.apache.axiom.om.impl.util.EmptyIterator;
import org.apache.axiom.om.impl.util.OMSerializerUtil;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.om.util.OMXMLStreamReaderValidator;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OMElementImpl extends OMNodeImpl
	implements OMElement, OMConstants, OMContainerEx
{
    private static final Log log = LogFactory.getLog(OMElementImpl.class);
    private static boolean DEBUG_ENABLED = log.isDebugEnabled();

    public static final OMNamespace DEFAULT_DEFAULT_NS_OBJECT = new OMNamespaceImpl("", "");

    protected OMNamespace ns;
    protected String localName;
    protected Map<QName, OMAttribute> attributes = null;

    protected OMNode firstChild;
    protected OMNode lastChild;

    protected QName qName;

    protected Map<String, OMNamespace> namespaces = null;

    private int lineNumber;

    /**
     * Constructor OMElementImpl. A null namespace indicates that the default
     * namespace in scope is used
     */
    public OMElementImpl(String localName, OMNamespace ns, OMContainer parent,
                         OMXMLParserWrapper builder, OMFactory factory) {
        super(parent, factory, false);
        this.localName = localName;
        if (ns != null) {
            setNamespace(ns);
        }
        this.builder = builder;
        firstChild = null;
    }


    /** Constructor OMElementImpl. */
    public OMElementImpl(String localName, OMNamespace ns, OMFactory factory) {
        this(localName, ns, null, factory);
    }

    /**
     * This is the basic constructor for OMElement. All the other constructors depends on this.
     *
     * @param localName - this MUST always be not null
     * @param ns        - can be null
     * @param parent    - this should be an OMContainer
     * @param factory   - factory that created this OMElement
     *                  <p/>
     *                  A null namespace indicates that the default namespace in scope is used
     */
    public OMElementImpl(String localName, OMNamespace ns, OMContainer parent,
                         OMFactory factory) {
        super(parent, factory, true);
        if (localName == null || localName.trim().length() == 0) {
            throw new OMException("localname can not be null or empty");
        }
        this.localName = localName;
        if (ns != null) {
            setNamespace(ns);
        }
    }

    /**
     * It is assumed that the QName passed contains, at least, the localName for this element.
     *
     * @param qname - this should be valid qname according to javax.xml.namespace.QName
     * @throws OMException
     */
    public OMElementImpl(QName qname, OMContainer parent, OMFactory factory)
            throws OMException {
        this(qname.getLocalPart(), null, parent, factory);
        this.ns = handleNamespace(qname);
    }

    /** Method handleNamespace. */
    OMNamespace handleNamespace(QName qname) {
        OMNamespace ns = null;

        // first try to find a namespace from the scope
        String namespaceURI = qname.getNamespaceURI();
        if (namespaceURI != null && namespaceURI.length() > 0) {
            String prefix = qname.getPrefix();
            ns = findNamespace(qname.getNamespaceURI(),
                               prefix);

            /**
             * What is left now is
             *  1. nsURI = null & parent != null, but ns = null
             *  2. nsURI != null, (parent doesn't have an ns with given URI), but ns = null
             */
            if (ns == null) {
                if ("".equals(prefix)) {
                    prefix = OMSerializerUtil.getNextNSPrefix();
                }
                ns = declareNamespace(namespaceURI, prefix);
            }
            if (ns != null) {
                this.ns = ns;
            }
        }
        return ns;
    }

    /**
     * Method handleNamespace.
     *
     * @return Returns namespace.
     */
    private OMNamespace handleNamespace(OMNamespace ns) {
        OMNamespace namespace = findNamespace(ns.getNamespaceURI(),
                                              ns.getPrefix());
        if (namespace == null) {
            namespace = declareNamespace(ns);
        }
        return namespace;
    }

    OMNamespace handleNamespace(String namespaceURI, String prefix) {
        OMNamespace namespace = findNamespace(namespaceURI,
                                              prefix);
        if (namespace == null) {
            namespace = declareNamespace(namespaceURI, prefix);
        }
        return namespace;
    }

    /**
     * Adds child to the element. One can decide whether to append the child or to add to the front
     * of the children list.
     */
    public void addChild(OMNode child) {
        if (child.getOMFactory() instanceof OMLinkedListImplFactory) {
            addChild((OMNodeImpl) child);
        } else {
            addChild(importNode(child));
        }
    }


    /**
     * Searches for children with a given QName and returns an iterator to traverse through the
     * OMNodes. This QName can contain any combination of prefix, localname and URI.
     *
     * @throws OMException
     */
    public Iterable<OMElement> getChildrenWithName(QName elementQName) {
        OMNode firstChild = getFirstOMChild();
        return new OMChildrenQNameIterator(firstChild, elementQName);
    }


    public Iterable<OMElement> getChildrenWithLocalName(String localName) {
        return new OMChildrenLocalNameIterator(getFirstOMChild(),
                                               localName);
    }


    public Iterable<OMElement> getChildrenWithNamespaceURI(String uri) {
        return new OMChildrenNamespaceIterator(getFirstOMChild(),
                                               uri);
    }


    /**
     * Method getFirstChildWithName.
     *
     * @throws OMException
     */
    public OMElement getFirstChildWithName(QName elementQName) throws OMException {
        OMChildrenQNameIterator omChildrenQNameIterator =
                new OMChildrenQNameIterator(getFirstOMChild(),
                                            elementQName);
        OMNode omNode = null;
        if (omChildrenQNameIterator.hasNext()) {
            omNode = omChildrenQNameIterator.next();
        }

        return ((omNode != null) && (OMNode.ELEMENT_NODE == omNode.getType())) ?
                (OMElement) omNode : null;

    }

    /** Method addChild. */
    private void addChild(OMNodeImpl child) {
        if (child.parent == this &&
            child == lastChild) {
            // The child is already the last node.
            // We don't need to detach and re-add it.
        } else {
            // Normal Case

            // The order of these statements is VERY important
            // Since setting the parent has a detach method inside
            // it strips down all the rerefences to siblings.
            // setting the siblings should take place AFTER setting the parent

            child.setParent(this);

            if (firstChild == null) {
                firstChild = child;
                child.previousSibling = null;
            } else {
                child.previousSibling = (OMNodeImpl) lastChild;
                ((OMNodeImpl) lastChild).nextSibling = child;
            }

            child.nextSibling = null;
            lastChild = child;
        }

        // For a normal OMNode, the incomplete status is
        // propogated up the tree.
        // However, a OMSourcedElement is self-contained
        // (it has an independent parser source).
        // So only propogate the incomplete setting if this
        // is a normal OMNode
        if (!child.isComplete() &&
            !(child instanceof OMSourcedElement)) {
            this.setComplete(false);
        }

    }

    /**
     * Gets the next sibling. This can be an OMAttribute or OMText or OMELement for others.
     *
     * @throws OMException
     */
    @Override
	public OMNode getNextOMSibling() throws OMException {
        while (!done && builder != null ) {
            if (builder.isCompleted()) {
                if (DEBUG_ENABLED) {
                    log.debug("Builder is complete.  Setting OMElement to complete.");
                }
                setComplete(true);
            } else {
                int token = builder.next();
                if (token == XMLStreamConstants.END_DOCUMENT) {
                    throw new OMException(
                    "Parser has already reached end of the document. No siblings found");
                }
            }
        }
        return super.getNextOMSibling();
    }

    /**
     * Returns a collection of this element. Children can be of types OMElement, OMText.
     *
     * @return Returns children.
     */
    public Iterable<OMNode> getChildren() {
        return new OMChildrenIterator(getFirstOMChild());
    }

    /**
     * Returns a filtered list of children - just the elements.
     *
     * @return Returns an iterator of the child elements.
     */
    public Iterable<OMElement> getChildElements() {
        return new OMChildElementIterator(getFirstElement());
    }

    /**
     * Creates a namespace in the current element scope.
     *
     * @return Returns namespace.
     */
    public OMNamespace declareNamespace(String uri, String prefix) {
        if ("".equals(prefix)) {
			prefix = OMSerializerUtil.getNextNSPrefix();
		}
        OMNamespaceImpl ns = new OMNamespaceImpl(uri, prefix);
        return declareNamespace(ns);
    }

    /**
     * We use "" to store the default namespace of this element. As one can see user can not give ""
     * as the prefix, when he declare a usual namespace.
     *
     * @param uri
     */
    public OMNamespace declareDefaultNamespace(String uri) {

        OMNamespaceImpl namespace = new OMNamespaceImpl(uri == null ? "" : uri, "");

        if (namespaces == null) {
            this.namespaces = new HashMap<String, OMNamespace>(5);
        }
        namespaces.put("", namespace);
        if (ns == null || "".equals(ns.getPrefix())) {
            ns = namespace;
            this.qName = null;
        }
        return namespace;
    }

    public OMNamespace getDefaultNamespace() {
        OMNamespace defaultNS;
        if (namespaces != null && (defaultNS = namespaces.get("")) != null) {
            return defaultNS;
        }
        if (parent instanceof OMElementImpl) {
            return ((OMElementImpl) parent).getDefaultNamespace();

        }
        return null;
    }

    /** @return Returns namespace. */
    public OMNamespace declareNamespace(OMNamespace namespace) {
        if (namespaces == null) {
            this.namespaces = new HashMap<String, OMNamespace>(5);
        }
        String prefix = namespace.getPrefix();
        if (prefix == null) {
            prefix = OMSerializerUtil.getNextNSPrefix();
            namespace = new OMNamespaceImpl(namespace.getNamespaceURI(), prefix);
        }
        namespaces.put(prefix, namespace);
        return namespace;
    }

    /**
     * Finds a namespace with the given uri and prefix, in the scope of the document. Starts to find
     * from the current element and goes up in the hiararchy until one is found. If none is found,
     * returns null.
     */
    public OMNamespace findNamespace(String uri, String prefix) {

        // check in the current element
        OMNamespace namespace = findDeclaredNamespace(uri, prefix);
        if (namespace != null) {
            return namespace;
        }

        // go up to check with ancestors
        if (parent != null) {
            //For the OMDocumentImpl there won't be any explicit namespace
            //declarations, so going up the parent chain till the document
            //element should be enough.
            if (parent instanceof OMElement) {
                namespace = ((OMElementImpl) parent).findNamespace(uri, prefix);
            }
        }

        return namespace;
    }

    public OMNamespace findNamespaceURI(String prefix) {
        OMNamespace ns = this.namespaces == null ?
                null :
                (OMNamespace) this.namespaces.get(prefix);

        if (ns == null && this.parent instanceof OMElement) {
            // try with the parent
            ns = ((OMElement) this.parent).findNamespaceURI(prefix);
        }
        return ns;
    }

    public static final OMNamespaceImpl xmlns =
            new OMNamespaceImpl(OMConstants.XMLNS_URI,
                                OMConstants.XMLNS_PREFIX);

    /**
     * Checks for the namespace <B>only</B> in the current Element. This is also used to retrieve
     * the prefix of a known namespace URI.
     */
    private OMNamespace findDeclaredNamespace(String uri, String prefix) {
        if (uri == null) {
            return namespaces == null ? null : (OMNamespace)namespaces.get(prefix);
        }

        //If the prefix is available and uri is available and its the xml namespace
        if (prefix != null && prefix.equals(OMConstants.XMLNS_PREFIX) &&
                uri.equals(OMConstants.XMLNS_URI)) {
            return xmlns;
        }

        if (namespaces == null) {
            return null;
        }

        if (prefix == null || "".equals(prefix)) {

            OMNamespace defaultNamespace = this.getDefaultNamespace();
            if (defaultNamespace != null && uri.equals(defaultNamespace.getNamespaceURI())) {
                return defaultNamespace;
            }

            String nsUri;
            for(OMNamespace omNamespace: namespaces.values()) {
                nsUri = omNamespace.getNamespaceURI();
                if (nsUri != null && nsUri.equals(uri)) {
                    return omNamespace;
                }
            }
        } else {
            OMNamespace namespace = namespaces.get(prefix);
            if (namespace != null && uri.equals(namespace.getNamespaceURI())) {
                return namespace;
            }
        }
        return null;
    }


    /**
     * Method getAllDeclaredNamespaces.
     *
     * @return Returns Iterator.
     */
    public Iterable<OMNamespace> getAllDeclaredNamespaces() {
        if (namespaces == null) {
            return new EmptyIterator<OMNamespace>();
        }
        return namespaces.values();
    }

    /**
     * Returns a List of OMAttributes.
     *
     * @return Returns iterator.
     */
    public Iterable<OMAttribute> getAllAttributes() {
        if (attributes == null) {
            return new EmptyIterator<OMAttribute>();
        }
        return attributes.values();
    }

    /**
     * Returns a named attribute if present.
     *
     * @param qname the qualified name to search for
     * @return Returns an OMAttribute with the given name if found, or null
     */
    public OMAttribute getAttribute(QName qname) {
        return attributes == null ? null : (OMAttribute) attributes.get(qname);
    }

    /**
     * Returns a named attribute's value, if present.
     *
     * @param qname the qualified name to search for
     * @return Returns a String containing the attribute value, or null.
     */
    public String getAttributeValue(QName qname) {
        OMAttribute attr = getAttribute(qname);
        return (attr == null) ? null : attr.getAttributeValue();
    }

    /**
     * Inserts an attribute to this element. Implementor can decide as to insert this in the front
     * or at the end of set of attributes.
     *
     * <p>The owner of the attribute is set to be the particular <code>OMElement</code>.
     * If the attribute already has an owner then the attribute is cloned (i.e. its name,
     * value and namespace are copied to a new attribute) and the new attribute is added
     * to the element. It's owner is then set to be the particular <code>OMElement</code>.
     *
     * @return The attribute that was added to the element. Note: The added attribute
     * may not be the same instance that was given to add. This can happen if the given
     * attribute already has an owner. In such case the returned attribute and the given
     * attribute are <i>equal</i> but not the same instance.
     *
     * @see OMAttributeImpl#equals(Object)
     */
    public OMAttribute addAttribute(OMAttribute attr){
        // If the attribute already has an owner element then clone the attribute
        if (attr.getOwner() !=null){
            attr = new OMAttributeImpl(
                    attr.getLocalName(), attr.getNamespace(), attr.getAttributeValue(), attr.getOMFactory());
        }

        if (attributes == null) {
            this.attributes = new LinkedHashMap<QName, OMAttribute>(5);
        }
        OMNamespace namespace = attr.getNamespace();
        String nsURI;
        String nsPrefix;
        if (namespace != null && (nsURI = namespace.getNamespaceURI()) != null &&
                !"".equals(nsURI) &&
                this.findNamespace(nsURI, (nsPrefix = namespace.getPrefix())) == null) {
            this.declareNamespace(nsURI, nsPrefix);
        }

        // Set the owner element of the attribute
        ((OMAttributeImpl)attr).owner = this;
        attributes.put(attr.getQName(), attr);
        return attr;
    }

    /** Method removeAttribute. */
    public void removeAttribute(OMAttribute attr) {
        if (attributes != null) {
            // Remove the owner from this attribute
            ((OMAttributeImpl)attr).owner = null;
            attributes.remove(attr.getQName());
        }
    }

    /**
     *
     * Creates an <code>OMAttributeImpl</code> instance out of the given arguments and
     * inserts that attribute to this element. Implementor can decide as to insert this
     * in the front or at the end of set of attributes.
     *
     * <p>The owner of the attribute is set to be the particular <code>OMElement</code>.
     * If the attribute already has an owner then the attribute is cloned (i.e. its name,
     * value and namespace are copied to a new attribute) and the new attribute is added
     * to the element. It's owner is then set to be the particular <code>OMElement</code>.
     *
     * @param attributeName The name of the attribute
     * @param value The value of the attribute
     * @param ns The namespace of the attribute
     *
     * @return The attribute that was added to the element. Note: The added attribute
     * may not be the same instance that was given to add. This can happen if the given
     * attribute already has an owner. In such case the returned attribute and the given
     * attribute are <i>equal</i> but not the same instance.
     *
     * @see OMAttributeImpl#equals(Object)
     */
    public OMAttribute addAttribute(String attributeName, String value,
                                    OMNamespace ns) {
        OMNamespace namespace = null;
        if (ns != null) {
            String namespaceURI = ns.getNamespaceURI();
            String prefix = ns.getPrefix();
            namespace = findNamespace(namespaceURI, prefix);
            if (namespace == null) {
                namespace = new OMNamespaceImpl(namespaceURI, prefix);
            }
        }
        return addAttribute(new OMAttributeImpl(attributeName, namespace, value, this.factory));
    }

    /** Method setBuilder. */
    public void setBuilder(OMXMLParserWrapper wrapper) {
        this.builder = wrapper;
    }

    /**
     * Method getBuilder.
     *
     * @return Returns OMXMLParserWrapper.
     */
    public OMXMLParserWrapper getBuilder() {
        return builder;
    }

    /** Forces the parser to proceed, if parser has not yet finished with the XML input. */
    public void buildNext() {
        if (builder != null) {
            if (!builder.isCompleted()) {
                builder.next();
            } else {
                this.setComplete(true);
                if (DEBUG_ENABLED) {
                    log.debug("Builder is complete.  Setting OMElement to complete.");
                }
            }
        }
    }

    /**
     * Method getFirstOMChild.
     *
     * @return Returns child.
     */
    public OMNode getFirstOMChild() {
        while ((firstChild == null) && !done) {
            buildNext();
        }
        return firstChild;
    }

    /** Method setFirstChild. */
    public void setFirstChild(OMNode firstChild) {
        if (firstChild != null) {
            ((OMNodeEx) firstChild).setParent(this);
        }
        this.firstChild = firstChild;
    }


    public void setLastChild(OMNode omNode) {
         this.lastChild = omNode;
    }

    /**
     * Removes this information item and its children, from the model completely.
     *
     * @throws OMException
     */
    @Override
	public OMNode detach() throws OMException {
        if (!done) {
            build();
        }
        super.detach();
        return this;
    }

    /**
     * Method isComplete.
     *
     * @return Returns boolean.
     */
    @Override
	public boolean isComplete() {
        return done;
    }

    /** Gets the type of node, as this is the super class of all the nodes. */
    @Override
	public int getType() {
        return OMNode.ELEMENT_NODE;
    }

    @Override
	public void build() throws OMException {
        /**
         * builder is null. Meaning this is a programatical created element but it has children which are not completed
         * Build them all.
         */
        if (builder == null && !done) {
            for(OMNode omNode: getChildren()) {
                omNode.build();
            }
        } else {
            super.build();
        }

    }

    /**
     * Method getXMLStreamReader.
     *
     * @see OMElement#getXMLStreamReader()
     */
    public XMLStreamReader getXMLStreamReader() {
        return getXMLStreamReader(true);
    }

    /**
     * Method getXMLStreamReaderWithoutCaching.
     *
     * @see OMElement#getXMLStreamReaderWithoutCaching()
     */
    public XMLStreamReader getXMLStreamReaderWithoutCaching() {
        return getXMLStreamReader(false);
    }

    /**
     * Method getXMLStreamReader.
     *
     * @return Returns reader.
     */
    private XMLStreamReader getXMLStreamReader(boolean cache) {
        if (builder != null && this.builder instanceof StAXOMBuilder) {
            if (!isComplete()) {
                if (((StAXOMBuilder) builder).isLookahead()) {
                    this.buildNext();
                }
            }
        }

        // The om tree was built by hand and is already complete
        OMXMLStreamReader reader = null;
        if ((builder == null) && done) {
            reader =  new OMStAXWrapper(null, this, false);
        } else {
            if ((builder == null) && !cache) {
                throw new UnsupportedOperationException(
                "This element was not created in a manner to be switched");
            }
            if (builder != null && builder.isCompleted() && !cache && !done) {
                throw new UnsupportedOperationException(
                "The parser is already consumed!");
            }
            reader = new OMStAXWrapper(builder, this, cache);
        }

        // If debug is enabled, wrap the OMXMLStreamReader in a validator.
        // The validator will check for mismatched events to help determine if the OMStAXWrapper
        // is functioning correctly.  All problems are reported as debug.log messages

        if (DEBUG_ENABLED) {
            reader =
                new OMXMLStreamReaderValidator(reader, // delegate to actual reader
                     false); // log problems (true will cause exceptions to be thrown)
        }

        return reader;
    }

    /**
     * Sets the text of the given element. caution - This method will wipe out all the text elements
     * (and hence any mixed content) before setting the text.
     */
    public void setText(String text) {

        OMNode child = this.getFirstOMChild();
        while (child != null) {
            if (child.getType() == OMNode.TEXT_NODE) {
                child.detach();
            }
            child = child.getNextOMSibling();
        }

        OMAbstractFactory.getOMFactory().createOMText(this, text);
    }

    /**
     * Sets the text, as a QName, of the given element. caution - This method will wipe out all the
     * text elements (and hence any mixed content) before setting the text.
     */
    public void setText(QName text) {

        OMNode child = this.getFirstOMChild();
        while (child != null) {
            if (child.getType() == OMNode.TEXT_NODE) {
                child.detach();
            }
            child = child.getNextOMSibling();
        }

        OMAbstractFactory.getOMFactory().createOMText(this, text);
    }

    /**
     * Selects all the text children and concatenates them to a single string.
     *
     * @return Returns String.
     */
    public String getText() {
        String childText = null;
        StringBuffer buffer = null;
        OMNode child = this.getFirstOMChild();

        while (child != null) {
            final int type = child.getType();
            if (type == OMNode.TEXT_NODE || type == OMNode.CDATA_SECTION_NODE) {
                OMText textNode = (OMText) child;
                String textValue = textNode.getText();
                if (textValue != null && textValue.length() != 0) {
                    if (childText == null) {
                        // This is the first non empty text node. Just save the string.
                        childText = textValue;
                    } else {
                        // We've already seen a non empty text node before. Concatenate using
                        // a StringBuffer.
                        if (buffer == null) {
                            // This is the first text node we need to append. Initialize the
                            // StringBuffer.
                            buffer = new StringBuffer(childText);
                        }
                        buffer.append(textValue);
                    }
                }
            }
            child = child.getNextOMSibling();
        }

        if (childText == null) {
            // We didn't see any text nodes. Return an empty string.
            return "";
        } else if (buffer != null) {
            return buffer.toString();
        } else {
            return childText;
        }
    }

    public QName getTextAsQName() {
        String childText = getTrimmedText();
        if (childText != null) {
            return resolveQName(childText);
        }
        return null;
    }

    /**
     * Convenience method that returns the concatenation of all trimmed OMText
     * child nodes of this element.
     */
    public String getTrimmedText() {
        String out = null;
        StringBuffer outBuffer = null;

        OMNode child = this.getFirstOMChild();
        while (child != null) {
            if (child.getType() == OMNode.TEXT_NODE) {
                OMText textNode = (OMText) child;
                String textValue = textNode.getText();
                if (textValue != null && textValue.length() != 0) {
                    if (out == null) {
                        out = textValue.trim();
                    } else {
                    	if(outBuffer == null) {
                    		outBuffer = new StringBuffer(out);
                    	}
                        outBuffer.append(textValue.trim());
                    }
                }
            }
            child = child.getNextOMSibling();
        }

        if(outBuffer != null) {
        	return outBuffer.toString();
        } else if(out != null) {
        	return out;
        } else {
        	return "";
        }
    }

    /**
     * Method internalSerialize.
     *
     * @throws XMLStreamException
     */
    @Override
	public void internalSerialize(XMLStreamWriter writer) throws XMLStreamException {
        internalSerialize(writer, true);
    }

///////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////

    protected void internalSerialize(XMLStreamWriter writer, boolean cache)
            throws XMLStreamException {

        if (cache) {
            //in this case we don't care whether the elements are built or not
            //we just call the serializeAndConsume methods
            OMSerializerUtil.serializeStartpart(this, writer);
            for(OMNode child: getChildren()) {
                ((OMNodeEx) child).internalSerialize(writer);
            }
            OMSerializerUtil.serializeEndpart(writer);

        } else {
            //Now the caching is supposed to be off. However caching been switched off
            //has nothing to do if the element is already built!
            if (this.done || (this.builder == null)) {
                OMSerializerUtil.serializeStartpart(this, writer);
                OMNodeImpl child = (OMNodeImpl) firstChild;
                while (child != null) {
                    if ((!(child instanceof OMElement)) || child.isComplete() ||
                            child.builder == null) {
                        child.internalSerializeAndConsume(writer);
                    } else {
                        OMElement element = (OMElement) child;
                        element.getBuilder().setCache(false);
                        OMSerializerUtil.serializeByPullStream(element, writer, cache);
                    }
                    child = child.nextSibling;
                }
                OMSerializerUtil.serializeEndpart(writer);
            } else {
                OMSerializerUtil.serializeByPullStream(this, writer, cache);
            }


        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This method serializes and consumes without building the object structure in memory. Misuse
     * of this method will cause loss of data. So it is advised to use populateYourSelf() method,
     * before calling this method, if one wants to preserve data in the stream. This was requested
     * during the second Axis2 summit.
     *
     * @throws XMLStreamException
     */
    @Override
	public void internalSerializeAndConsume(XMLStreamWriter writer) throws XMLStreamException {
        this.internalSerialize(writer, false);
    }

    /**
     * Gets first element.
     *
     * @return Returns element.
     */
    public OMElement getFirstElement() {
        OMNode node = getFirstOMChild();
        while (node != null) {
            if (node.getType() == OMNode.ELEMENT_NODE) {
                return (OMElement) node;
            } else {
                node = node.getNextOMSibling();
            }
        }
        return null;
    }

    /**
     * Method getLocalName.
     *
     * @return Returns local name.
     */
    public String getLocalName() {
        return localName;
    }

    /** Method setLocalName. */
    public void setLocalName(String localName) {
        this.localName = localName;
        this.qName = null;
    }

    /**
     * Method getNamespace.
     *
     * @throws OMException
     */
    public OMNamespace getNamespace() throws OMException {
//        return ns != null ? ns : DEFAULT_DEFAULT_NS_OBJECT;
        if (ns == null) {
            // User wants to keep this element in the default default namespace. Let's try to see the default namespace
            // is overriden by some one up in the tree
            OMNamespace parentDefaultNS = this.findNamespaceURI("");

            if (parentDefaultNS != null && !"".equals(parentDefaultNS.getNamespaceURI())) {
                // if it was overriden, then we must explicitly declare default default namespace as the namespace
                // of this element
                ns = DEFAULT_DEFAULT_NS_OBJECT;
                this.qName = null;
            }
        }
        return ns;
    }

    /** Method setNamespace. */
    public void setNamespace(OMNamespace namespace) {
        OMNamespace nsObject = null;
        if (namespace != null) {
            nsObject = handleNamespace(namespace);
        }
        this.ns = nsObject;
        this.qName = null;
    }

    public void setNamespaceWithNoFindInCurrentScope(OMNamespace namespace) {
        this.ns = namespace;
        this.qName = null;
    }

    /**
     * Method getQName.
     *
     * @return Returns QName.
     */
    public QName getQName() {
        if (qName != null) {
            return qName;
        }

        if (ns != null) {
            if (ns.getPrefix() != null) {
                qName = new QName(ns.getNamespaceURI(), localName, ns.getPrefix());
            } else {
                qName = new QName(ns.getNamespaceURI(), localName);
            }
        } else {
            qName = new QName(localName);
        }
        return qName;
    }

    public String toStringWithConsume() throws XMLStreamException {
        StringWriter writer = new StringWriter();
        XMLStreamWriter writer2 = StAXUtils.createXMLStreamWriter(writer);
        try {
            this.serializeAndConsume(writer2);
            writer2.flush();
        } finally {
            writer2.close();
        }
        return writer.toString();
    }

    @Override
	public String toString() {
        StringWriter writer = new StringWriter();
        try {
            XMLStreamWriter writer2 = StAXUtils.createXMLStreamWriter(writer);
            try {
                this.serialize(writer2);
                writer2.flush();
            } finally {
                writer2.close();
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("Can not serialize OM Element " + this.getLocalName(), e);
        }
        return writer.toString();
    }

    /**
     * Method discard.
     *
     * @throws OMException
     */
    public void discard() throws OMException {
        if (done || builder == null) {
            this.detach();
        } else {
            builder.discard(this);
        }
    }

    /**
     * Converts a prefix:local qname string into a proper QName, evaluating it in the OMElement
     * context. Unprefixed qnames resolve to the local namespace.
     *
     * @param qname prefixed qname string to resolve
     * @return Returns null for any failure to extract a qname.
     */
    public QName resolveQName(String qname) {
        ElementHelper helper = new ElementHelper(this);
        return helper.resolveQName(qname);
    }

    public OMElement cloneOMElement() {

        if (log.isDebugEnabled()) {
            log.debug("cloneOMElement start");
            log.debug("  element string =" + getLocalName());
            log.debug(" isComplete = " + isComplete());
            log.debug("  builder = " + builder);
        }
        // Make sure the source (this node) is completed
        if (!isComplete()) {
            this.build();
        }

        // Now get a parser for the full tree
        XMLStreamReader xmlStreamReader = this.getXMLStreamReader(true);
        if (log.isDebugEnabled()) {
            log.debug("  reader = " + xmlStreamReader);
        }

        // Get a new builder.  Use an xop aware builder if the original
        // builder is xop aware
        StAXOMBuilder newBuilder = null;
        if (builder instanceof XOPBuilder) {
            Attachments attachments = ((XOPBuilder)builder).getAttachments();
            attachments.getAllContentIDs();
            if (xmlStreamReader instanceof OMXMLStreamReader) {
                if (log.isDebugEnabled()) {
                    log.debug("  read optimized xop:include");
                }
                ((OMXMLStreamReader)xmlStreamReader).setInlineMTOM(false);
            }
            newBuilder = new XOPAwareStAXOMBuilder(xmlStreamReader, attachments);
        } else {
            newBuilder = new StAXOMBuilder(xmlStreamReader);
        }
        if (log.isDebugEnabled()) {
            log.debug("  newBuilder = " + newBuilder);
        }

        // Build the (target) clonedElement from the parser
        OMElement clonedElement = newBuilder.getDocumentElement();
        clonedElement.build();
        return clonedElement;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    /* (non-Javadoc)
      * @see org.apache.axiom.om.OMNode#buildAll()
      */
    @Override
	public void buildWithAttachments() {
        if (!done) {
            this.build();
        }
        for(OMNode node: getChildren()) {
            node.buildWithAttachments();
        }
    }

    /** This method will be called when one of the children becomes complete. */
    protected void notifyChildComplete() {
        if (!this.done && builder == null) {
            for(OMNode node: getChildren()) {
                if (!node.isComplete()) {
                    return;
                }
            }
            this.setComplete(true);
        }
    }
}

