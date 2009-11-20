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


import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMConstants;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.MTOMXMLStreamWriter;
import org.apache.axiom.om.impl.OMNamespaceImpl;
import org.apache.axiom.om.impl.builder.XOPBuilder;
import org.apache.axiom.om.impl.util.OMSerializerUtil;
import org.apache.axiom.om.util.TextHelper;
import org.apache.axiom.om.util.UUIDGenerator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;

public class OMTextImpl extends OMNodeImpl implements OMText, OMConstants {
    /** Field nameSpace used when serializing Binary stuff as MTOM optimized. */
    public static final OMNamespace XOP_NS = new OMNamespaceImpl(
            "http://www.w3.org/2004/08/xop/include", "xop");

    protected String value = null;
    protected char[] charArray;

    private boolean calcNS;  // Set to true after textNS is calculated
    protected OMNamespace textNS;

    protected String mimeType;

    protected boolean optimize = false;

    protected boolean isBinary = false;

    /** Field contentID for the mime part used when serializing Binary stuff as MTOM optimized. */
    private String contentID = null;

    /**
     * Field dataHandler contains the DataHandler Declaring as Object to remove the dependency on
     * Javax.activation.DataHandler
     */
    private Object dataHandlerObject = null;

    /** Field localName used when serializing Binary stuff as MTOM optimized. */
    protected String localName = "Include";

    /** Field attributes used when serializing Binary stuff as MTOM optimized. */
    protected OMAttribute attribute;
    private static final String EMTPY_STRING = "";

    /**
     * Constructor OMTextImpl.
     *
     * @param s
     */
    public OMTextImpl(String s, OMFactory factory) {
        this(s, TEXT_NODE, factory);
    }

    /**
     * @param s
     * @param nodeType - OMText can handle CHARACTERS, SPACES, CDATA and ENTITY REFERENCES.
     *                 Constants for this can be found in OMNode.
     */
    public OMTextImpl(String s, int nodeType, OMFactory factory) {
        this(null, s, nodeType, factory);
    }

    /**
     * Constructor OMTextImpl.
     *
     * @param parent
     * @param text
     */
    public OMTextImpl(OMContainer parent, String text, OMFactory factory) {
        this(parent, text, TEXT_NODE, factory);
    }
    
    /**
     * Construct OMTextImpl that is a copy of the source OMTextImpl
     * @param parent
     * @param source OMTextImpl
     * @param factory
     */
    public OMTextImpl(OMContainer parent, OMTextImpl source, OMFactory factory) {
        super(parent, factory, true);
        // Copy the value of the text
        this.value = source.value;
        this.nodeType = source.nodeType;
        
        // Clone the charArray (if it exists)
        if (source.charArray != null) {
            this.charArray = new char[source.charArray.length];
            System.arraycopy(source.charArray, 0, this.charArray, 0, source.charArray.length);
        }
        
        // Turn off calcNS...the namespace will need to be recalculated
        // in the new tree's context.
        this.calcNS = false;
        this.textNS = null;
        
        // Copy the optimized related settings.
        this.optimize = source.optimize;
        this.mimeType = source.mimeType;
        this.isBinary = source.isBinary;
        
        // TODO
        // Do we need a deep copy of the data-handler 
        this.contentID = source.contentID;
        this.dataHandlerObject = source.dataHandlerObject;
        
        this.localName = source.localName;
        if (source.attribute != null) {
            this.attribute = factory.createOMAttribute(source.attribute.getLocalName(),
                                                       source.attribute.getNamespace(),
                                                       source.attribute.getAttributeValue());
        }
    }

    public OMTextImpl(OMContainer parent, String text, int nodeType,
                      OMFactory factory) {
        super(parent, factory, true);
        this.value = text == null ? EMTPY_STRING : text;
        this.nodeType = nodeType;
    }

    public OMTextImpl(OMContainer parent, char[] charArray, int nodeType,
                      OMFactory factory) {
        super(parent, factory, true);
        this.charArray = charArray;
        this.nodeType = nodeType;
    }


    public OMTextImpl(OMContainer parent, QName text, OMFactory factory) {
        this(parent, text, TEXT_NODE, factory);
    }

    public OMTextImpl(OMContainer parent, QName text, int nodeType,
                      OMFactory factory) {
        super(parent, factory, true);
        if (text == null) throw new IllegalArgumentException("QName text arg cannot be null!");
        this.calcNS = true;
        this.textNS =
                ((OMElementImpl) parent).handleNamespace(text.getNamespaceURI(), text.getPrefix());
        this.value = textNS.getPrefix() + ":" + text.getLocalPart();
        this.nodeType = nodeType;
    }

    /**
     * @param s        - base64 encoded String representation of Binary
     * @param mimeType of the Binary
     */
    public OMTextImpl(String s, String mimeType, boolean optimize,
                      OMFactory factory) {
        this(null, s, mimeType, optimize, factory);
    }

    /**
     * @param parent
     * @param s        - base64 encoded String representation of Binary
     * @param mimeType of the Binary
     */
    public OMTextImpl(OMContainer parent, String s, String mimeType,
                      boolean optimize, OMFactory factory) {
        this(parent, s, factory);
        this.mimeType = mimeType;
        this.optimize = optimize;
        this.isBinary = true;
        done = true;
        this.nodeType = TEXT_NODE;
    }

    /** @param dataHandler To send binary optimised content Created programatically. */
    public OMTextImpl(Object dataHandler, OMFactory factory) {
        this(dataHandler, true, factory);
    }

    /**
     * @param dataHandler
     * @param optimize    To send binary content. Created progrmatically.
     */
    public OMTextImpl(Object dataHandler, boolean optimize, OMFactory factory) {
        super(factory);
        this.dataHandlerObject = dataHandler;
        this.isBinary = true;
        this.optimize = optimize;
        done = true;
        this.nodeType = TEXT_NODE;
    }

    /**
     * @param contentID
     * @param parent
     * @param builder   Used when the builder is encountered with a XOP:Include tag Stores a
     *                  reference to the builder and the content-id. Supports deferred parsing of
     *                  MIME messages.
     */
    public OMTextImpl(String contentID, OMContainer parent,
                      OMXMLParserWrapper builder, OMFactory factory) {
        super(parent, factory, false);
        this.contentID = contentID;
        this.optimize = true;
        this.isBinary = true;
        this.builder = builder;
        this.nodeType = TEXT_NODE;
    }

    /**
     * @param writer
     * @throws XMLStreamException
     */
    public void internalSerialize(XMLStreamWriter writer) throws XMLStreamException {
        internalSerializeLocal(writer);
    }

    /**
     * Writes the relevant output.
     *
     * @param writer
     * @throws XMLStreamException
     */
    private void writeOutput(XMLStreamWriter writer) throws XMLStreamException {
        int type = getType();
        if (type == TEXT_NODE || type == SPACE_NODE) {
            writer.writeCharacters(this.getText());
        } else if (type == CDATA_SECTION_NODE) {
            writer.writeCData(this.getText());
        } else if (type == ENTITY_REFERENCE_NODE) {
            writer.writeEntityRef(this.getText());
        }
    }

    /** Returns the value. */
    public String getText() throws OMException {
        if (charArray != null || this.value != null) {
            return getTextFromProperPlace();
        } else {
            try {
                return TextHelper.toString(getInputStream());
            } catch (Exception e) {
                throw new OMException(e);
            }
        }
    }

    public char[] getTextCharacters() {
        return charArray != null ? charArray : value.toCharArray();
    }

    public boolean isCharacters() {
        return charArray != null;
    }

    /**
     * This OMText contains two data source:value and charArray. This method will return text from
     * correct place.
     */
    private String getTextFromProperPlace() {
        return charArray != null ? new String(charArray) : value;
    }


    /** Returns the value. */
    public QName getTextAsQName() throws OMException {
        return ((OMElement)parent).resolveQName(getTextFromProperPlace());
    }

    /* (non-Javadoc)
      * @see org.apache.axiom.om.OMText#getNamespace()
      */
    public OMNamespace getNamespace() {
        // If the namespace has already been determined, return it
        // Otherwise calculate the namespace if the text contains a colon and is not detached.
        if (calcNS) {
            return textNS;
        } else {
            calcNS = true;
            if (getParent() != null) {
                String text = getTextFromProperPlace();
                if (text != null) {
                    int colon = text.indexOf(':');
                    if (colon > 0) {
                        textNS = ((OMElementImpl) getParent()).
                                findNamespaceURI(text.substring(0, colon));
                        if (textNS != null) {
                            charArray = null;
                            value = text.substring(colon + 1);
                        }
                    }
                }
            }
        }
        return textNS;
    }

    public boolean isOptimized() {
        return optimize;
    }

    public void setOptimize(boolean value) {
        this.optimize = value;
        if (value) {
            isBinary = true;
        }
    }

    /**
     * Receiving binary can happen as either MTOM attachments or as Base64 Text In the case of
     * Base64 user has to explicitly specify that the content is binary, before calling
     * getDataHandler(), getInputStream()....
     */
    public void setBinary(boolean value) {
        isBinary = value;
    }

    public boolean isBinary() {
        return isBinary;
    }


    /**
     * Gets the datahandler.
     *
     * @return Returns javax.activation.DataHandler
     */
    public Object getDataHandler() {
        if ((value != null || charArray != null) && isBinary) {
            String text = getTextFromProperPlace();
            return org.apache.axiom.attachments.utils.DataHandlerUtils
                    .getDataHandlerFromText(text, mimeType);
        } else {

            if (dataHandlerObject == null) {
                if (contentID == null) {
                    throw new RuntimeException("ContentID is null");
                }
                dataHandlerObject = ((XOPBuilder) builder)
                        .getDataHandler(contentID);
            }
            return dataHandlerObject;
        }
    }

    public String getLocalName() {
        return localName;
    }

    public java.io.InputStream getInputStream() throws OMException {
        if (isBinary) {
            if (dataHandlerObject == null) {
                getDataHandler();
            }
            InputStream inStream;
            javax.activation.DataHandler dataHandler =
                    (javax.activation.DataHandler) dataHandlerObject;
            try {
                inStream = dataHandler.getDataSource().getInputStream();
            } catch (IOException e) {
                throw new OMException(
                        "Cannot get InputStream from DataHandler." + e);
            }
            return inStream;
        } else {
            throw new OMException("Unsupported Operation");
        }
    }

    public String getContentID() {
        if (contentID == null) {
            contentID = UUIDGenerator.getUUID()
                    + "@apache.org";
        }
        return this.contentID;
    }

    public void internalSerializeAndConsume(XMLStreamWriter writer)
            throws XMLStreamException {
        internalSerializeLocal(writer);
    }

    private void internalSerializeLocal(XMLStreamWriter writer2) throws XMLStreamException {

        if ((!this.isBinary) || (!this.isOptimized())) {
            writeOutput(writer2);
        } else {
            //check whether we have a MTOMXMLStreamWriter. if so
            //we can optimize the writing!
            if (writer2 instanceof MTOMXMLStreamWriter) {
                MTOMXMLStreamWriter writer = (MTOMXMLStreamWriter) writer2;
                if (writer.isOptimized() && writer.isOptimizedThreshold(this)) {
                    if (contentID == null) {
                        contentID = writer.getNextContentId();
                    }
                    // send binary as MTOM optimised
                    this.attribute = new OMAttributeImpl("href",
                                                         new OMNamespaceImpl("", ""),
                                                         "cid:" + getContentID(), this.factory);
                    this.serializeStartpart(writer);
                    writer.writeOptimized(this);
                    writer.writeEndElement();
                } else {
                    //do normal base64
                    writeOutput(writer);
                }
            } else {
                //we do not have a optimized writer. Just do the normal
                //base64 writing
                writeOutput(writer2);
            }

        }
    }

    /*
     * Methods to copy from OMSerialize utils
     */
    private void serializeStartpart(XMLStreamWriter writer)
            throws XMLStreamException {
        String nameSpaceName = XOP_NS.getNamespaceURI();
        String writer_prefix = writer.getPrefix(nameSpaceName);
        String prefix = XOP_NS.getPrefix();
        if (writer_prefix != null) {
            writer.writeStartElement(nameSpaceName, this
                    .getLocalName());
        } else {
            // According to StAX, setPrefix must occur before
            // writeStartElement
            if (OMSerializerUtil.isSetPrefixBeforeStartElement(writer)) {
                writer.setPrefix(prefix, nameSpaceName);
                writer.writeStartElement(prefix, this.getLocalName(),
                                         nameSpaceName);
            } else {
                writer.writeStartElement(prefix, this.getLocalName(),
                                         nameSpaceName);
                writer.setPrefix(prefix, nameSpaceName);
            }
        }
        // add the elements attribute "href"
        serializeAttribute(this.attribute, writer);
        // add the namespace
        serializeNamespace(XOP_NS, writer);
    }

    /**
     * Method serializeAttribute.
     *
     * @param attr
     * @throws XMLStreamException
     */
    static void serializeAttribute(OMAttribute attr, XMLStreamWriter writer)
            throws XMLStreamException {
        // first check whether the attribute is associated with a namespace
        OMNamespace ns = attr.getNamespace();
        String prefix;
        String namespaceName;
        if (ns != null) {
            // add the prefix if it's availble
            prefix = ns.getPrefix();
            namespaceName = ns.getNamespaceURI();
            if (prefix != null) {
                writer.writeAttribute(prefix, namespaceName, attr
                        .getLocalName(), attr.getAttributeValue());
            } else {
                writer.writeAttribute(namespaceName, attr.getLocalName(), attr
                        .getAttributeValue());
            }
        } else {
            writer.writeAttribute(attr.getLocalName(), attr.getAttributeValue());
        }
    }

    /**
     * Method serializeNamespace.
     *
     * @param namespace
     * @param writer
     * @throws XMLStreamException
     */
    static void serializeNamespace(OMNamespace namespace, XMLStreamWriter writer)
            throws XMLStreamException {
        if (namespace != null) {
            String uri = namespace.getNamespaceURI();
            String ns_prefix = namespace.getPrefix();
            writer.writeNamespace(ns_prefix, namespace.getNamespaceURI());
            writer.setPrefix(ns_prefix, uri);
        }
    }

    /**
     * A slightly different implementation of the discard method.
     *
     * @throws OMException
     */
    public void discard() throws OMException {
        if (done) {
            this.detach();
        } 
    }

    /* (non-Javadoc)
      * @see org.apache.axiom.om.OMNode#buildAll()
      */
    public void buildWithAttachments() {
        if (!this.done) {
            this.build();
        }
        if (isOptimized()) {
            this.getDataHandler();
        }
    }
    
    public void setContentID(String cid) {
        this.contentID = cid;
    }

}
