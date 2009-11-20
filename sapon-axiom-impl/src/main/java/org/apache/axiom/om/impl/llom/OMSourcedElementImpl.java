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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.OMNamespaceImpl;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Element backed by an arbitrary data source. When necessary, this element will be expanded by
 * creating a parser from the data source.</p>
 * <p/>
 * <p>Whenever methods are added to the base {@link OMElementImpl}
 * class the corresponding methods must be added to this class (there's a unit test to verify that
 * this has been done, just to make sure nothing gets accidentally broken). If the method only
 * requires the element name and/or namespace information, the base class method can be called
 * directly. Otherwise, the element must be expanded into a full OM tree (by calling the {@link
 * #forceExpand()} method) before the base class method is called. This will typically involve a
 * heavy overhead penalty, so should be avoided if possible.</p>
 */
public class OMSourcedElementImpl extends OMElementImpl implements OMSourcedElement {

    /** Data source for element data. */
    private OMDataSource dataSource;

    /** Namespace for element, needed in order to bypass base class handling. */
    private OMNamespace definedNamespace = null;

    /** Flag for parser provided to base element class. */
    private boolean isExpanded = false;

    private static Log log = LogFactory.getLog(OMSourcedElementImpl.class);
    private static final boolean isDebugEnabled = log.isDebugEnabled();

    private static Log forceExpandLog = LogFactory.getLog(OMSourcedElementImpl.class.toString()+".forceExpand");

    private XMLStreamReader readerFromDS = null;  // Reader from DataSource

    /**
     * Constructor.
     *
     * @param localName
     * @param ns
     * @param factory
     * @param source
     */
    public OMSourcedElementImpl(String localName, OMNamespace ns, OMFactory factory,
                                OMDataSource source) {
        super(localName, null, factory);
        dataSource = source;
        isExpanded = (dataSource == null);
        if (!isExpanded) {
            if (!isLossyPrefix(dataSource)) {
                // Believe the prefix and create a normal OMNamespace
                definedNamespace = ns;
            } else {
                // Create a deferred namespace that forces an expand to get the prefix
                definedNamespace = new DeferredNamespace(ns.getNamespaceURI());
            }
        } else {
            definedNamespace = ns;
        }
    }

    /**
     * Constructor that takes a QName instead of the local name and the namespace seperately
     *
     * @param qName
     * @param factory
     * @param source
     */
    public OMSourcedElementImpl(QName qName, OMFactory factory, OMDataSource source) {
        //create a namespace
        super(qName.getLocalPart(), null, factory);
        dataSource = source;
        isExpanded = (dataSource == null);
        if (!isExpanded) {
            if (!isLossyPrefix(dataSource)) {
                // Believe the prefix and create a normal OMNamespace
                definedNamespace = new OMNamespaceImpl(qName.getNamespaceURI(), qName.getPrefix());
            } else {
                // Create a deferred namespace that forces an expand to get the prefix
                definedNamespace = new DeferredNamespace(qName.getNamespaceURI());
            }
        } else {
            definedNamespace = new OMNamespaceImpl(qName.getNamespaceURI(), qName.getPrefix());
        }
    }

    public OMSourcedElementImpl(String localName, OMNamespace ns, OMContainer parent, OMFactory factory) {
        super(localName, null, parent, factory);
        dataSource = null;
        definedNamespace = ns;
        isExpanded = true;
        if (ns != null) {
            this.setNamespace(ns);
        }
    }

    public OMSourcedElementImpl(String localName, OMNamespace ns, OMContainer parent, OMXMLParserWrapper builder, OMFactory factory) {
        super(localName, null, parent, builder, factory);
        dataSource = null;
        definedNamespace = ns;
        isExpanded = true;
        if (ns != null) {
            this.setNamespace(ns);
        }
    }

    public OMSourcedElementImpl(String localName, OMNamespace ns, OMFactory factory) {
        super(localName, null, factory);
        dataSource = null;
        definedNamespace = ns;
        isExpanded = true;
        if (ns != null) {
            this.setNamespace(ns);
        }
    }


    /**
     * The namespace uri is immutable, but the OMDataSource may change
     * the value of the prefix.  This method queries the OMDataSource to
     * see if the prefix is known.
     * @param source
     * @return true or false
     */
    private boolean isLossyPrefix(OMDataSource source) {
        Object lossyPrefix = null;
        if (source instanceof OMDataSourceExt) {
            lossyPrefix =
                ((OMDataSourceExt) source).getProperty(OMDataSourceExt.LOSSY_PREFIX);

        }
        return lossyPrefix == Boolean.TRUE;
    }

//    private void setDeferredNamespace(OMDataSource source, String uri, String prefix) {
//        Object lossyPrefix = null;
//        if (source instanceof OMDataSourceExt) {
//            lossyPrefix =
//                ((OMDataSourceExt) source).getProperty(OMDataSourceExt.LOSSY_PREFIX);
//
//        }
//        if (lossyPrefix != Boolean.TRUE) {
//            // Believe the prefix and create a normal OMNamespace
//            definedNamespace = new OMNamespaceImpl(uri, prefix);
//        } else {
//            // Create a deferred namespace that forces an expand to get the prefix
//            definedNamespace = new DeferredNamespace(uri);
//        }
//    }

    /**
     * Generate element name for output.
     *
     * @return name
     */
    private String getPrintableName() {
        String uri = null;
        if (getNamespace() != null) {
            uri = getNamespace().getNamespaceURI();
        }
        if (uri == null || uri.length() == 0) {
            return getLocalName();
        } else {
            return "{" + uri + '}' + getLocalName();
        }
    }

    /**
     * Get parser from data source. Note that getDataReader may consume the underlying data source.
     *
     * @return parser
     */
    private XMLStreamReader getDirectReader() {
        try {
            // If expansion has occurred, then the reader from the datasource is consumed or stale.
            // In such cases use the stream reader from the OMElementImpl
            if (isExpanded()) {
                return super.getXMLStreamReader();
            } else {
                return dataSource.getReader();
            }
        } catch (XMLStreamException e) {
            log.error("Could not get parser from data source for element " +
                    getPrintableName(), e);
            throw new RuntimeException("Error obtaining parser from data source:" +
                    e.getMessage());
        }
    }

    /**
     * Set parser for OM, if not previously set. Since the builder is what actually constructs the
     * tree on demand, this first creates a builder
     */
    private void forceExpand() {
        if (!isExpanded) {

            if (isDebugEnabled) {
                log.debug("forceExpand: expanding element " +
                        getPrintableName());
                if(forceExpandLog.isDebugEnabled()){
                	// When using an OMSourcedElement, it can be particularly difficult to
                	// determine why an expand occurs... a stack trace should help debugging this
                	Exception e = new Exception("Debug Stack Trace");
                	forceExpandLog.debug("forceExpand stack", e);
                }
            }

            // Get the XMLStreamReader
            readerFromDS = getDirectReader();

            // Advance past the START_DOCUMENT to the start tag.
            // Remember the character encoding.
            String characterEncoding = readerFromDS.getCharacterEncodingScheme();
            if (characterEncoding != null) {
                characterEncoding = readerFromDS.getEncoding();
            }
            try {
                if (readerFromDS.getEventType() != XMLStreamConstants.START_ELEMENT) {
                    while (readerFromDS.next() != XMLStreamConstants.START_ELEMENT) {
						;
					}
                }
            } catch (XMLStreamException e) {
                log.error("forceExpand: error parsing data soruce document for element " +
                        getLocalName(), e);
                throw new RuntimeException("Error parsing data source document:" +
                        e.getMessage());
            }

            // Make sure element local name and namespace matches what was expected
            if (!readerFromDS.getLocalName().equals(getLocalName())) {
                log.error("forceExpand: expected element name " +
                        getLocalName() + ", found " + readerFromDS.getLocalName());
                throw new RuntimeException("Element name from data source is " +
                        readerFromDS.getLocalName() + ", not the expected " + getLocalName());
            }
            String readerURI = readerFromDS.getNamespaceURI();
            readerURI = (readerURI == null) ? "" : readerURI;
            String uri = (getNamespace() == null) ? "" :
                ((getNamespace().getNamespaceURI() == null) ? "" : getNamespace().getNamespaceURI());
            if (!readerURI.equals(uri)) {
                log.error("forceExpand: expected element namespace " +
                        getLocalName() + ", found " + uri);
                throw new RuntimeException("Element namespace from data source is " +
                        readerURI + ", not the expected " + uri);
            }

            // Get the current prefix and the reader's prefix
            String readerPrefix = readerFromDS.getPrefix();
            readerPrefix = (readerPrefix == null) ? "" : readerPrefix;
            String prefix = null;

            OMNamespace ns = getNamespace();
            if (ns == null || ns instanceof DeferredNamespace) {
                // prefix is not available until after expansion
            } else {
                prefix = ns.getPrefix();
            }

            // Set the builder for this element
            isExpanded = true;
            super.setBuilder(new StAXOMBuilder(getOMFactory(),
                                               readerFromDS,
                                               this,
                                               characterEncoding));
            setComplete(false);

            // Update the prefix if necessary.  This must be done after
            // isParserSet to avoid a recursive call
            if (!readerPrefix.equals(prefix) ||
                 getNamespace() == null ||
                 ns instanceof DeferredNamespace) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "forceExpand: changing prefix from " + prefix + " to " + readerPrefix);
                }
                setNamespace(new OMNamespaceImpl(readerURI, readerPrefix));
            }

        }
    }

    /**
     * Check if element has been expanded into tree.
     *
     * @return <code>true</code> if expanded, <code>false</code> if not
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getChildElements()
     */
    @Override
	public Iterable<OMElement> getChildElements() {
        forceExpand();
        return super.getChildElements();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#declareNamespace(java.lang.String, java.lang.String)
     */
    @Override
	public OMNamespace declareNamespace(String uri, String prefix) {
        forceExpand();
        return super.declareNamespace(uri, prefix);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#declareDefaultNamespace(java.lang.String)
     */
    @Override
	public OMNamespace declareDefaultNamespace(String uri) {
        forceExpand();
        return super.declareDefaultNamespace(uri);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getDefaultNamespace()
     */
    @Override
	public OMNamespace getDefaultNamespace() {
        forceExpand();
        return super.getDefaultNamespace();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#declareNamespace(org.apache.axiom.om.OMNamespace)
     */
    @Override
	public OMNamespace declareNamespace(OMNamespace namespace) {
        forceExpand();
        return super.declareNamespace(namespace);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#findNamespace(java.lang.String, java.lang.String)
     */
    @Override
	public OMNamespace findNamespace(String uri, String prefix) {
        forceExpand();
        return super.findNamespace(uri, prefix);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#findNamespaceURI(java.lang.String)
     */
    @Override
	public OMNamespace findNamespaceURI(String prefix) {
        forceExpand();
        return super.findNamespaceURI(prefix);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getAllDeclaredNamespaces()
     */
    @Override
	public Iterable<OMNamespace> getAllDeclaredNamespaces() throws OMException {
        forceExpand();
        return super.getAllDeclaredNamespaces();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getAllAttributes()
     */
    @Override
	public Iterable<OMAttribute> getAllAttributes() {
        forceExpand();
        return super.getAllAttributes();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getAttribute(javax.xml.namespace.QName)
     */
    @Override
	public OMAttribute getAttribute(QName qname) {
        forceExpand();
        return super.getAttribute(qname);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getAttributeValue(javax.xml.namespace.QName)
     */
    @Override
	public String getAttributeValue(QName qname) {
        forceExpand();
        return super.getAttributeValue(qname);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#addAttribute(org.apache.axiom.om.OMAttribute)
     */
    @Override
	public OMAttribute addAttribute(OMAttribute attr) {
        forceExpand();
        return super.addAttribute(attr);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#addAttribute(java.lang.String,
     * java.lang.String, org.apache.axiom.om.OMNamespace)
     */
    @Override
	public OMAttribute addAttribute(String attributeName, String value, OMNamespace namespace) {
        forceExpand();
        return super.addAttribute(attributeName, value, namespace);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#removeAttribute(org.apache.axiom.om.OMAttribute)
     */
    @Override
	public void removeAttribute(OMAttribute attr) {
        forceExpand();
        super.removeAttribute(attr);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#setBuilder(org.apache.axiom.om.OMXMLParserWrapper)
     */
    @Override
	public void setBuilder(OMXMLParserWrapper wrapper) {
        throw new UnsupportedOperationException(
                "Builder cannot be set for element backed by data source");
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getBuilder()
     */
    @Override
	public OMXMLParserWrapper getBuilder() {
        forceExpand();
        return super.getBuilder();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#setFirstChild(org.apache.axiom.om.OMNode)
     */
    @Override
	public void setFirstChild(OMNode node) {
        forceExpand();
        super.setFirstChild(node);
    }


    @Override
	public void setLastChild(OMNode omNode) {
        forceExpand();
        super.setLastChild(omNode);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getFirstElement()
     */
    @Override
	public OMElement getFirstElement() {
        forceExpand();
        return super.getFirstElement();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getXMLStreamReader()
     */
    @Override
	public XMLStreamReader getXMLStreamReader() {
        if (isDebugEnabled) {
            log.debug("getting XMLStreamReader for " + getPrintableName());
        }
        if (isExpanded) {
            return super.getXMLStreamReader();
        } else {
            if (isDestructiveRead()) {
                forceExpand();
                return super.getXMLStreamReader();
            }
            return getDirectReader();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getXMLStreamReaderWithoutCaching()
     */
    @Override
	public XMLStreamReader getXMLStreamReaderWithoutCaching() {
        if (isDebugEnabled) {
            log.debug("getting XMLStreamReader without caching for " +
                    getPrintableName());
        }
        if (isExpanded) {
            XMLStreamReader reader = super.getXMLStreamReaderWithoutCaching();
            return reader;
        } else {
            return getDirectReader();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#setText(java.lang.String)
     */
    @Override
	public void setText(String text) {
        forceExpand();
        super.setText(text);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#setText(javax.xml.namespace.QName)
     */
    @Override
	public void setText(QName text) {
        forceExpand();
        super.setText(text);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getText()
     */
    @Override
	public String getText() {
        forceExpand();
        return super.getText();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getTextAsQName()
     */
    @Override
	public QName getTextAsQName() {
        forceExpand();
        return super.getTextAsQName();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getLocalName()
     */
    @Override
	public String getLocalName() {
        // no need to set the parser, just call base method directly
        return super.getLocalName();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#setLocalName(java.lang.String)
     */
    @Override
	public void setLocalName(String localName) {
        // no need to expand the tree, just call base method directly
        super.setLocalName(localName);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getNamespace()
     */
    @Override
	public OMNamespace getNamespace() throws OMException {
        if (isExpanded()) {
            return super.getNamespace();
        }
        return definedNamespace;
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#setNamespace(org.apache.axiom.om.OMNamespace)
     */
    @Override
	public void setNamespace(OMNamespace namespace) {
        forceExpand();
        super.setNamespace(namespace);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#
     * setNamespaceWithNoFindInCurrentScope(org.apache.axiom.om.OMNamespace)
     */
    @Override
	public void setNamespaceWithNoFindInCurrentScope(OMNamespace namespace) {
        forceExpand();
        super.setNamespaceWithNoFindInCurrentScope(namespace);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getQName()
     */
    @Override
	public QName getQName() {
        if (isExpanded()) {
            return super.getQName();
        } else if (getNamespace() != null) {
            // always ignore prefix on name from sourced element
            return new QName(getNamespace().getNamespaceURI(), getLocalName());
        } else {
            return new QName(getLocalName());
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#toStringWithConsume()
     */
    @Override
	public String toStringWithConsume() throws XMLStreamException {
        if (isExpanded()) {
            return super.toStringWithConsume();
        } else {
            StringWriter writer = new StringWriter();
            XMLStreamWriter writer2 = StAXUtils.createXMLStreamWriter(writer);
            dataSource.serialize(writer2);  // dataSource.serialize consumes the data
            writer2.flush();
            return writer.toString();
        }
    }

    private boolean isDestructiveWrite() {
        if (dataSource instanceof OMDataSourceExt) {
            return ((OMDataSourceExt) dataSource).isDestructiveWrite();
        } else {
            return true;
        }
    }

    private boolean isDestructiveRead() {
        if (dataSource instanceof OMDataSourceExt) {
            return ((OMDataSourceExt) dataSource).isDestructiveRead();
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#resolveQName(java.lang.String)
     */
    @Override
	public QName resolveQName(String qname) {
        forceExpand();
        return super.resolveQName(qname);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#cloneOMElement()
     */
    @Override
	public OMElement cloneOMElement() {
        forceExpand();
        return super.cloneOMElement();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#setLineNumber(int)
     */
    @Override
	public void setLineNumber(int lineNumber) {
        // no need to expand the tree, just call base method directly
        super.setLineNumber(lineNumber);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMElement#getLineNumber()
     */
    @Override
	public int getLineNumber() {
        // no need to expand the tree, just call base method directly
        return super.getLineNumber();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#discard()
     */
    @Override
	public void discard() throws OMException {
        // discard without expanding the tree
        setComplete(true);
        super.detach();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#getType()
     */
    @Override
	public int getType() {
        // no need to expand the tree, just call base method directly
        return super.getType();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#internalSerialize(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void internalSerialize(javax.xml.stream.XMLStreamWriter writer)
            throws XMLStreamException {
        // The contract of internalSerialize is to "cache" the om
        if (isExpanded()) {
            super.internalSerialize(writer);
        } else {
            if (isDestructiveWrite()) {
                forceExpand();
                super.internalSerialize(writer);
            } else {
                dataSource.serialize(writer);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.impl.llom.OMElementImpl#
     * internalSerialize(javax.xml.stream.XMLStreamWriter, boolean)
     */
    @Override
	protected void internalSerialize(XMLStreamWriter writer, boolean cache)
            throws XMLStreamException {
        if (isExpanded()) {
            super.internalSerialize(writer, cache);
        } else if (cache) {
            if (isDestructiveWrite()) {
                forceExpand();
                super.internalSerialize(writer, true);
            } else {
                dataSource.serialize(writer);
            }
        } else {
            internalSerializeAndConsume(writer);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#internalSerializeAndConsume(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void internalSerializeAndConsume(XMLStreamWriter writer) throws XMLStreamException {
        if (isDebugEnabled) {
            log.debug("serialize " + getPrintableName() + " to XMLStreamWriter");
        }
        if (isExpanded()) {
            super.internalSerializeAndConsume(writer);
        } else {
            dataSource.serialize(writer);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#serialize(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void serialize(XMLStreamWriter xmlWriter) throws XMLStreamException {
        // The contract is to serialize with caching
        internalSerialize(xmlWriter);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#serialize(java.io.OutputStream)
     */
    @Override
	public void serialize(OutputStream output) throws XMLStreamException {
        OMOutputFormat format = new OMOutputFormat();
        serialize(output, format);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#serialize(java.io.Writer)
     */
    @Override
	public void serialize(Writer writer) throws XMLStreamException {
        OMOutputFormat format = new OMOutputFormat();
        serialize(writer, format);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#
     * serialize(java.io.OutputStream, org.apache.axiom.om.OMOutputFormat)
     */
    @Override
	public void serialize(OutputStream output, OMOutputFormat format) throws XMLStreamException {
        if (isExpanded) {
            super.serialize(output, format);
        } else if (isDestructiveWrite()) {
            forceExpand();
            super.serialize(output, format);
        } else {
            dataSource.serialize(output, format);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#
     * serialize(java.io.Writer, org.apache.axiom.om.OMOutputFormat)
     */
    @Override
	public void serialize(Writer writer, OMOutputFormat format) throws XMLStreamException {
        if (isExpanded) {
            super.serialize(writer, format);
        } else if (isDestructiveWrite()) {
            forceExpand();
            super.serialize(writer, format);
        } else {
            dataSource.serialize(writer, format);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#serializeAndConsume(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void serializeAndConsume(javax.xml.stream.XMLStreamWriter xmlWriter)
            throws XMLStreamException {
        internalSerializeAndConsume(xmlWriter);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#serializeAndConsume(java.io.OutputStream)
     */
    @Override
	public void serializeAndConsume(OutputStream output) throws XMLStreamException {
        if (isDebugEnabled) {
            log.debug("serialize " + getPrintableName() + " to output stream");
        }
        OMOutputFormat format = new OMOutputFormat();
        format.setAutoCloseWriter(true);
        if (isExpanded()) {
            super.serializeAndConsume(output, format);
        } else {
            dataSource.serialize(output, format);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#serializeAndConsume(java.io.Writer)
     */
    @Override
	public void serializeAndConsume(Writer writer) throws XMLStreamException {
        if (isDebugEnabled) {
            log.debug("serialize " + getPrintableName() + " to writer");
        }
        if (isExpanded()) {
            super.serializeAndConsume(writer);
        } else {
            OMOutputFormat format = new OMOutputFormat();
            format.setAutoCloseWriter(true);
            dataSource.serialize(writer, format);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#
     * serializeAndConsume(java.io.OutputStream, org.apache.axiom.om.OMOutputFormat)
     */
    @Override
	public void serializeAndConsume(OutputStream output, OMOutputFormat format)
            throws XMLStreamException {
        if (isDebugEnabled) {
            log.debug("serialize formatted " + getPrintableName() +
                    " to output stream");
        }
        if (isExpanded()) {
            super.serializeAndConsume(output, format);
        } else {
            dataSource.serialize(output, format);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMNode#
     * serializeAndConsume(java.io.Writer, org.apache.axiom.om.OMOutputFormat)
     */
    @Override
	public void serializeAndConsume(Writer writer, OMOutputFormat format)
            throws XMLStreamException {
        if (isDebugEnabled) {
            log.debug("serialize formatted " + getPrintableName() +
                    " to writer");
        }
        if (isExpanded()) {
            super.serializeAndConsume(writer, format);
        } else {
            dataSource.serialize(writer, format);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMContainer#addChild(org.apache.axiom.om.OMNode)
     */
    @Override
	public void addChild(OMNode omNode) {
        forceExpand();
        super.addChild(omNode);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMContainer#getChildrenWithName(javax.xml.namespace.QName)
     */
    @Override
	public Iterable<OMElement> getChildrenWithName(QName elementQName) {
        forceExpand();
        return super.getChildrenWithName(elementQName);
    }

    @Override
	public Iterable<OMElement> getChildrenWithLocalName(String localName) {
        forceExpand();
        return super.getChildrenWithLocalName(localName);
    }

    @Override
	public Iterable<OMElement> getChildrenWithNamespaceURI(String uri) {
        forceExpand();
        return super.getChildrenWithNamespaceURI(uri);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMContainer#getFirstChildWithName(javax.xml.namespace.QName)
     */
    @Override
	public OMElement getFirstChildWithName(QName elementQName) throws OMException {
        forceExpand();
        return super.getFirstChildWithName(elementQName);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMContainer#getChildren()
     */
    @Override
	public Iterable<OMNode> getChildren() {
        forceExpand();
        return super.getChildren();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMContainer#getFirstOMChild()
     */
    @Override
	public OMNode getFirstOMChild() {
        forceExpand();
        return super.getFirstOMChild();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.OMContainer#buildNext()
     */
    @Override
	public void buildNext() {
        forceExpand();
        super.buildNext();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.impl.llom.OMElementImpl#detach()
     */
    @Override
	public OMNode detach() throws OMException {
        // detach without expanding the tree
        boolean complete = isComplete();
        setComplete(true);
        OMNode result = super.detach();
        setComplete(complete);
        return result;
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.impl.llom.OMElementImpl#getNextOMSibling()
     */
    @Override
	public OMNode getNextOMSibling() throws OMException {
        // no need to expand the tree, just call base method directly
        return super.getNextOMSibling();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.impl.llom.OMElementImpl#getTrimmedText()
     */
    @Override
	public String getTrimmedText() {
        forceExpand();
        return super.getTrimmedText();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.impl.llom.OMElementImpl#handleNamespace(javax.xml.namespace.QName)
     */
    @Override
	OMNamespace handleNamespace(QName qname) {
        forceExpand();
        return super.handleNamespace(qname);
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.impl.llom.OMElementImpl#isComplete()
     */
    @Override
	public boolean isComplete() {
        if (isExpanded) {
            return super.isComplete();
        } else {
            return true;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.om.impl.llom.OMElementImpl#toString()
     */
    @Override
	public String toString() {
        if (isExpanded) {
            return super.toString();
        } else if (isDestructiveWrite()) {
            forceExpand();
            return super.toString();
        } else {
            try {
                StringWriter writer = new StringWriter();
                OMOutputFormat format = new OMOutputFormat();
                dataSource.serialize(writer, format);
                String text = writer.toString();
                writer.close();
                return text;
            } catch (XMLStreamException e) {
                throw new RuntimeException("Cannot serialize OM Element " + this.getLocalName(), e);
            } catch (IOException e) {
                throw new RuntimeException("Cannot serialize OM Element " + this.getLocalName(), e);
            }
        }
    }

    /* (non-Javadoc)
      * @see org.apache.axiom.om.OMNode#buildAll()
      */
    @Override
	public void buildWithAttachments() {

        // If not done, force the parser to build the elements
        if (!done) {
            this.build();
        }

        // If the OMSourcedElement is in in expanded form, then
        // walk the descendents to make sure they are built.
        // If the OMSourcedElement is backed by a OMDataSource,
        // we don't want to walk the children (because this will result
        // in an unnecessary translation from OMDataSource to a full OM tree).
        if (isExpanded()) {
            for(OMNode node: getChildren()) {
                node.buildWithAttachments();
            }
        }
    }

    @Override
	public void build() throws OMException {
        super.build();
    }

    @Override
	protected void notifyChildComplete() {
        super.notifyChildComplete();
    }


    @Override
	OMNamespace handleNamespace(String namespaceURI, String prefix) {
        return super.handleNamespace(namespaceURI,
                                     prefix);
    }

    /**
     * Provide access to the data source encapsulated in OMSourcedElement.
     * This is usesful when we want to access the raw data in the data source.
     *
     * @return the internal datasource
     */
    public OMDataSource getDataSource() {
        return dataSource;
    }

    /**
     * setOMDataSource
     */
    public OMDataSource setDataSource(OMDataSource dataSource) {
        if (!isExpanded()) {
            OMDataSource oldDS = this.dataSource;
            this.dataSource = dataSource;
            return oldDS;  // Caller is responsible for closing the data source
        } else {
            // TODO
            // Remove the entire subtree and replace with
            // new datasource.  There maybe a more performant way to do this.
            OMDataSource oldDS = this.dataSource;
            for(OMNode node: getChildren()) {
                node.detach();
            }
            this.dataSource = dataSource;
            setComplete(false);
            isExpanded = false;
            super.setBuilder(null);
            if (isLossyPrefix(dataSource)) {
                // Create a deferred namespace that forces an expand to get the prefix
                definedNamespace = new DeferredNamespace(definedNamespace.getNamespaceURI());
            }
            return oldDS;
        }
    }

    /**
     * setComplete override The OMSourcedElement has its own isolated builder/reader during the
     * expansion process. Thus calls to setCompete should stop here and not propogate up to the
     * parent (which may have a different builder or no builder).
     */
    @Override
	public void setComplete(boolean value) {
        done = value;
        if (done == true) {
            if (readerFromDS != null) {
                try {
                    readerFromDS.close();
                } catch (XMLStreamException e) {
                }
                readerFromDS = null;
            }
            if (dataSource != null) {
                if (dataSource instanceof OMDataSourceExt) {
                    ((OMDataSourceExt)dataSource).close();
                }
                dataSource = null;
            }
        }
        if (done == true && readerFromDS != null) {
            try {
                readerFromDS.close();
            } catch (XMLStreamException e) {
            }
            readerFromDS = null;
        }
    }

    class DeferredNamespace implements OMNamespace {

        String uri;

        DeferredNamespace(String ns) {
            this.uri = ns;
        }

        public boolean equals(String uri, String prefix) {
            String thisPrefix = getPrefix();
            return (this.uri.equals(uri) &&
                    (thisPrefix == null ? prefix == null :
                            thisPrefix.equals(prefix)));
        }

        public String getName() {
            return uri;
        }

        public String getNamespaceURI() {
            return uri;
        }

        public String getPrefix() {
            if (!isExpanded()) {
                forceExpand();
            }
            return getNamespace().getPrefix();
        }

        @Override
		public int hashCode() {
            String thisPrefix = getPrefix();
            return uri.hashCode() ^ (thisPrefix != null ? thisPrefix.hashCode() : 0);
        }

        @Override
		public boolean equals(Object obj) {
            if (!(obj instanceof OMNamespace)) {
                return false;
            }
            OMNamespace other = (OMNamespace)obj;
            String otherPrefix = other.getPrefix();
            String thisPrefix = getPrefix();
            return (uri.equals(other.getNamespaceURI()) &&
                    (thisPrefix == null ? otherPrefix == null :
                            thisPrefix.equals(otherPrefix)));
        }

    }
}
