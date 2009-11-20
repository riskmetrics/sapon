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

package org.apache.axiom.om.impl.serialize;

import java.util.ArrayList;

import javax.activation.DataHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttachmentAccessor;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMSerializer;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.MTOMXMLStreamWriter;
import org.apache.axiom.om.impl.util.OMSerializerUtil;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Class StreamingOMSerializer */
public class StreamingOMSerializer implements XMLStreamConstants, OMSerializer {

    Log log = LogFactory.getLog(StreamingOMSerializer.class);

    private static int namespaceSuffix = 0;
    public static final String NAMESPACE_PREFIX = "ns";

    /*
    * The behavior of the serializer is such that it returns when it encounters the
    * starting element for the second time. The depth variable tracks the depth of the
    * serilizer and tells it when to return.
    * Note that it is assumed that this serialization starts on an Element.
    */

    /** Field depth */
    private int depth = 0;

    public static final QName XOP_INCLUDE =
        new QName("http://www.w3.org/2004/08/xop/include", "Include");

    private boolean inputHasAttachments = false;
    private boolean skipEndElement = false;

    /**
     * Method serialize.
     *
     * @param node
     * @param writer
     * @throws XMLStreamException
     */
    public void serialize(XMLStreamReader node, XMLStreamWriter writer)
            throws XMLStreamException {
        serialize(node, writer, true);
    }

    /**
     * @param node
     * @param writer
     * @param startAtNext indicate if reading should start at next event or current event
     * @throws XMLStreamException
     */
    public void serialize(XMLStreamReader node, XMLStreamWriter writer, boolean startAtNext)
            throws XMLStreamException {

        // Set attachment status
        if (node instanceof OMAttachmentAccessor) {
            inputHasAttachments = true;
        }

        serializeNode(node, writer, startAtNext);
    }

    /**
     * Method serializeNode.
     *
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeNode(XMLStreamReader reader, XMLStreamWriter writer)
        throws XMLStreamException {
        serializeNode(reader, writer, true);
    }
    protected void serializeNode(XMLStreamReader reader,
                                 XMLStreamWriter writer,
                                 boolean startAtNext)
            throws XMLStreamException {
        // TODO We get the StAXWriter at this point and uses it hereafter
        // assuming that this is the only entry point to this class.
        // If there can be other classes calling methodes of this we might
        // need to change methode signatures to OMOutputer

        // If not startAtNext, then seed the processing with the current element.
        boolean useCurrentEvent = !startAtNext;

        while (reader.hasNext() || useCurrentEvent) {
            int event = 0;
            if (useCurrentEvent) {
                event = reader.getEventType();
                useCurrentEvent = false;
            } else {
                event = reader.next();
            }

            if (event == START_ELEMENT) {
                serializeElement(reader, writer);
                depth++;
            } else if (event == ATTRIBUTE) {
                serializeAttributes(reader, writer);
            } else if (event == CHARACTERS) {
                serializeText(reader, writer);
            } else if (event == COMMENT) {
                serializeComment(reader, writer);
            } else if (event == CDATA) {
                serializeCData(reader, writer);
            } else if (event == END_ELEMENT) {
                serializeEndElement(writer);
                depth--;
            } else if (event == START_DOCUMENT) {
                depth++; //if a start document is found then increment the depth
            } else if (event == END_DOCUMENT) {
                if (depth != 0) {
					depth--;  //for the end document - reduce the depth
				}
                try {
                    serializeEndElement(writer);
                } catch (Exception e) {
                    //TODO: log exceptions
                }
            }
            if (depth == 0) {
                break;
            }
        }
    }

    /**
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeElement(XMLStreamReader reader,
                                    XMLStreamWriter writer)
            throws XMLStreamException {


        // Note: To serialize the start tag, we must follow the order dictated by the JSR-173 (StAX) specification.
        // Please keep this code in sync with the code in OMSerializerUtil.serializeStartpart

        // The algorithm is:
        // ... generate setPrefix/setDefaultNamespace for each namespace declaration if the prefix is unassociated.
        // ... generate setPrefix/setDefaultNamespace if the prefix of the element is unassociated
        // ... generate setPrefix/setDefaultNamespace for each unassociated prefix of the attributes.
        //
        // ... generate writeStartElement (See NOTE_A)
        //
        // ... generate writeNamespace/writerDefaultNamespace for the new namespace declarations determine during the "set" processing
        // ... generate writeAttribute for each attribute

        // NOTE_A: To confuse matters, some StAX vendors (including woodstox), believe that the setPrefix bindings
        // should occur after the writeStartElement.  If this is the case, the writeStartElement is generated first.

        ArrayList<String> writePrefixList = null;
        ArrayList<String> writeNSList = null;

        // Get the prefix and namespace of the element.  "" and null are identical.
        String ePrefix = reader.getPrefix();
        ePrefix = (ePrefix != null && ePrefix.length() == 0) ? null : ePrefix;
        String eNamespace = reader.getNamespaceURI();
        eNamespace = (eNamespace != null && eNamespace.length() == 0) ? null : eNamespace;

        if (this.inputHasAttachments &&
            XOP_INCLUDE.getNamespaceURI().equals(eNamespace)) {
            String eLocalPart = reader.getLocalName();
            if (XOP_INCLUDE.getLocalPart().equals(eLocalPart)) {
                if (serializeXOPInclude(reader, writer)) {
                    // Since the xop:include is replaced with inlined text,
                    // skip the rest of serialize element and skip the end event for
                    // of the xop:include
                    skipEndElement = true;
                    return;
                }
            }
        }

        // Write the startElement if required
        boolean setPrefixFirst = OMSerializerUtil.isSetPrefixBeforeStartElement(writer);
        if (!setPrefixFirst) {
            if (eNamespace != null) {
                if (ePrefix == null) {
                    if (!OMSerializerUtil.isAssociated("", eNamespace, writer)) {

                        if (writePrefixList == null) {
                            writePrefixList = new ArrayList<String>();
                            writeNSList = new ArrayList<String>();
                        }
                        writePrefixList.add("");
                        writeNSList.add(eNamespace);
                    }

                    writer.writeStartElement("", reader.getLocalName(), eNamespace);
                } else {

                    if (!OMSerializerUtil.isAssociated(ePrefix, eNamespace, writer)) {
                        if (writePrefixList == null) {
                            writePrefixList = new ArrayList<String>();
                            writeNSList = new ArrayList<String>();
                        }
                        writePrefixList.add(ePrefix);
                        writeNSList.add(eNamespace);
                    }

                    writer.writeStartElement(ePrefix, reader.getLocalName(), eNamespace);
                }
            } else {
                writer.writeStartElement(reader.getLocalName());
            }
        }

        // Generate setPrefix for the namespace declarations
        int count = reader.getNamespaceCount();
        for (int i = 0; i < count; i++) {
            String prefix = reader.getNamespacePrefix(i);
            prefix = (prefix != null && prefix.length() == 0) ? null : prefix;
            String namespace = reader.getNamespaceURI(i);
            namespace = (namespace != null && namespace.length() == 0) ? null : namespace;

            String newPrefix = OMSerializerUtil.generateSetPrefix(prefix, namespace, writer, false, setPrefixFirst);
            // If this is a new association, remember it so that it can written out later
            if (newPrefix != null) {
                if (writePrefixList == null) {
                    writePrefixList = new ArrayList<String>();
                    writeNSList = new ArrayList<String>();
                }
                if (!writePrefixList.contains(newPrefix)) {
                    writePrefixList.add(newPrefix);
                    writeNSList.add(namespace);
                }
            }
        }

        // Generate setPrefix for the element
        // If the prefix is not associated with a namespace yet, remember it so that we can
        // write out a namespace declaration
        String newPrefix = OMSerializerUtil.generateSetPrefix(ePrefix, eNamespace, writer, false, setPrefixFirst);
        // If this is a new association, remember it so that it can written out later
        if (newPrefix != null) {
            if (writePrefixList == null) {
                writePrefixList = new ArrayList<String>();
                writeNSList = new ArrayList<String>();
            }
            if (!writePrefixList.contains(newPrefix)) {
                writePrefixList.add(newPrefix);
                writeNSList.add(eNamespace);
            }
        }

        // Now Generate setPrefix for each attribute
        count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String prefix = reader.getAttributePrefix(i);
            prefix = (prefix != null && prefix.length() == 0) ? null : prefix;
            String namespace = reader.getAttributeNamespace(i);
            namespace = (namespace != null && namespace.length() == 0) ? null : namespace;

            // Default prefix referencing is not allowed on an attribute
            if (prefix == null && namespace != null) {
                String writerPrefix = writer.getPrefix(namespace);
                writerPrefix =
                        (writerPrefix != null && writerPrefix.length() == 0) ? null : writerPrefix;
                prefix = (writerPrefix != null) ?
                        writerPrefix :
                        generateUniquePrefix(writer.getNamespaceContext());
            }
            newPrefix = OMSerializerUtil.generateSetPrefix(prefix, namespace, writer, true, setPrefixFirst);
            // If the prefix is not associated with a namespace yet, remember it so that we can
            // write out a namespace declaration
            if (newPrefix != null) {
                if (writePrefixList == null) {
                    writePrefixList = new ArrayList<String>();
                    writeNSList = new ArrayList<String>();
                }
                if (!writePrefixList.contains(newPrefix)) {
                    writePrefixList.add(newPrefix);
                    writeNSList.add(namespace);
                }
            }
        }

        // Now write the startElement
        if (setPrefixFirst) {
            if (eNamespace != null) {
                if (ePrefix == null) {
                    writer.writeStartElement("", reader.getLocalName(), eNamespace);
                } else {
                    writer.writeStartElement(ePrefix, reader.getLocalName(), eNamespace);
                }
            } else {
                writer.writeStartElement(reader.getLocalName());
            }
        }

        // Now write out the list of namespace declarations in this list that we constructed
        // while doing the "set" processing.
        if (writePrefixList != null) {
            for (int i = 0; i < writePrefixList.size(); i++) {
                String prefix = writePrefixList.get(i);
                String namespace = writeNSList.get(i);
                if (prefix != null) {
                    if (namespace == null) {
                        writer.writeNamespace(prefix, "");
                    } else {
                        writer.writeNamespace(prefix, namespace);
                    }
                } else {
                    writer.writeDefaultNamespace(namespace);
                }
            }
        }

        // Now write the attributes
        count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String prefix = reader.getAttributePrefix(i);
            prefix = (prefix != null && prefix.length() == 0) ? null : prefix;
            String namespace = reader.getAttributeNamespace(i);
            namespace = (namespace != null && namespace.length() == 0) ? null : namespace;


            if (prefix == null && namespace != null) {
                // Default namespaces are not allowed on an attribute reference.
                // Earlier in this code, a unique prefix was added for this case...now obtain and use it
                prefix = writer.getPrefix(namespace);
                //XMLStreamWriter doesn't allow for getPrefix to know whether you're asking for the prefix
                //for an attribute or an element. So if the namespace matches the default namespace getPrefix will return
                //the empty string, as if it were an element, in all cases (even for attributes, and even if
                //there was a prefix specifically set up for this), which is not the desired behavior.
                //Since the interface is base java, we can't fix it where we need to (by adding an attr boolean to
                //XMLStreamWriter.getPrefix), so we hack it in here...
                if (prefix == null || "".equals(prefix)) {
                    for (int j = 0; j < writePrefixList.size(); j++) {
                        if (namespace.equals(writeNSList.get(j))) {
                            prefix = writePrefixList.get(j);
                        }
                    }
                }
            } else if (namespace != null) {
                // Use the writer's prefix if it is different, but if the writers
                // prefix is empty then do not replace because attributes do not
                // default to the default namespace like elements do.
                String writerPrefix = writer.getPrefix(namespace);
                if (writerPrefix != null && !prefix.equals(writerPrefix) && !"".equals(writerPrefix)) {
                    prefix = writerPrefix;
                }
            }
            if (namespace != null) {
                // Qualified attribute
                writer.writeAttribute(prefix, namespace,
                                      reader.getAttributeLocalName(i),
                                      reader.getAttributeValue(i));
            } else {
                // Unqualified attribute
                writer.writeAttribute(reader.getAttributeLocalName(i),
                                      reader.getAttributeValue(i));
            }
        }
    }

    /**
     * Method serializeEndElement.
     *
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeEndElement(XMLStreamWriter writer)
            throws XMLStreamException {
        if (this.skipEndElement) {
            skipEndElement = false;
            return;
        }
        writer.writeEndElement();
    }

    /**
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeText(XMLStreamReader reader,
                                 XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeCharacters(reader.getText());
    }

    /**
     * Method serializeCData.
     *
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeCData(XMLStreamReader reader,
                                  XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeCData(reader.getText());
    }

    /**
     * Method serializeComment.
     *
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeComment(XMLStreamReader reader,
                                    XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeComment(reader.getText());
    }

    /**
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void serializeAttributes(XMLStreamReader reader,
                                       XMLStreamWriter writer)
            throws XMLStreamException {
        int count = reader.getAttributeCount();
        String prefix = null;
        String namespaceName = null;
        String writerPrefix = null;
        for (int i = 0; i < count; i++) {
            prefix = reader.getAttributePrefix(i);
            namespaceName = reader.getAttributeNamespace(i);
            /*
               Some parser implementations return null for the unqualified namespace.
               But getPrefix(null) will throw an exception (according to the XMLStreamWriter
               javadoc. We guard against this by using "" for the unqualified namespace.
            */
            namespaceName =(namespaceName == null) ? "" : namespaceName;

            // Using getNamespaceContext should be avoided when not necessary.
            // Some parser implementations construct a new NamespaceContext each time it is invoked.
            // writerPrefix = writer.getNamespaceContext().getPrefix(namespaceName);
            writerPrefix = writer.getPrefix(namespaceName);

            if (!"".equals(namespaceName)) {
                //prefix has already being declared but this particular attrib has a
                //no prefix attached. So use the prefix provided by the writer
                if (writerPrefix != null && (prefix == null || prefix.equals(""))) {
                    writer.writeAttribute(writerPrefix, namespaceName,
                                          reader.getAttributeLocalName(i),
                                          reader.getAttributeValue(i));

                    //writer prefix is available but different from the current
                    //prefix of the attrib. We should be decalring the new prefix
                    //as a namespace declaration
                } else if (prefix != null && !"".equals(prefix) && !prefix.equals(writerPrefix)) {
                    writer.writeNamespace(prefix, namespaceName);
                    writer.writeAttribute(prefix, namespaceName,
                                          reader.getAttributeLocalName(i),
                                          reader.getAttributeValue(i));

                    //prefix is null (or empty), but the namespace name is valid! it has not
                    //being written previously also. So we need to generate a prefix
                    //here
                } else {
                    prefix = generateUniquePrefix(writer.getNamespaceContext());
                    writer.writeNamespace(prefix, namespaceName);
                    writer.writeAttribute(prefix, namespaceName,
                                          reader.getAttributeLocalName(i),
                                          reader.getAttributeValue(i));
                }
            } else {
                //empty namespace is equal to no namespace!
                writer.writeAttribute(reader.getAttributeLocalName(i),
                                      reader.getAttributeValue(i));
            }


        }
    }

    /**
     * Generates a unique namespace prefix that is not in the scope of the NamespaceContext
     *
     * @param nsCtxt
     * @return string
     */
    private String generateUniquePrefix(NamespaceContext nsCtxt) {
        String prefix = NAMESPACE_PREFIX + namespaceSuffix++;
        //null should be returned if the prefix is not bound!
        while (nsCtxt.getNamespaceURI(prefix) != null) {
            prefix = NAMESPACE_PREFIX + namespaceSuffix++;
        }

        return prefix;
    }

    /**
     * Method serializeNamespace.
     *
     * @param prefix
     * @param URI
     * @param writer
     * @throws XMLStreamException
     */
//    private void serializeNamespace(String prefix,
//                                    String URI,
//                                    XMLStreamWriter writer)
//            throws XMLStreamException {
//        String prefix1 = writer.getPrefix(URI);
//        if (prefix1 == null) {
//            writer.writeNamespace(prefix, URI);
//            writer.setPrefix(prefix, URI);
//        }
//    }

    /**
     * Inspect the current element and if it is an
     * XOP Include then write it out as inlined or optimized.
     * @param reader
     * @param writer
     * @return true if inlined
     */
    protected boolean serializeXOPInclude(XMLStreamReader reader,
                                          XMLStreamWriter writer) {
       String cid = ElementHelper.getContentID(reader);
       DataHandler dh = getDataHandler(cid, (OMAttachmentAccessor) reader);
       if (dh == null) {
           return false;
       }

       OMFactory omFactory = OMAbstractFactory.getOMFactory();
       OMText omText = omFactory.createOMText(dh, true);
       omText.setContentID(cid);


       MTOMXMLStreamWriter mtomWriter =
           (writer instanceof MTOMXMLStreamWriter) ?
                   (MTOMXMLStreamWriter) writer :
                       null;

       if (mtomWriter != null &&
               mtomWriter.isOptimized() &&
               mtomWriter.isOptimizedThreshold(omText)) {
           // This will write the attachment after the xml message
           mtomWriter.writeOptimized(omText);
           return false;
       }

       // This will inline the attachment
       omText.setOptimize(false);
       try {
           writer.writeCharacters(omText.getText());
           return true;
       } catch (XMLStreamException e) {
           // Just writer out the xop:include
           return false;
       }

    }

    private DataHandler getDataHandler(String cid, OMAttachmentAccessor oaa) {
        DataHandler dh = null;

        String blobcid = cid;
        if (blobcid.startsWith("cid:")) {
            blobcid = blobcid.substring(4);
        }
        // Get the attachment
        if (oaa != null) {
             dh = oaa.getDataHandler(blobcid);
        }

        if (dh == null) {
            blobcid = getNewCID(cid);
            if (blobcid.startsWith("cid:")) {
                blobcid = blobcid.substring(4);
            }
            if (oaa != null) {
                dh = oaa.getDataHandler(blobcid);
           }
        }
        return dh;
    }

    /**
     * @param cid
     * @return cid with translated characters
     */
    private String getNewCID(String cid) {
        String cid2 = cid;

        try {
            cid2 = java.net.URLDecoder.decode(cid, "UTF-8");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("getNewCID decoding " + cid + " as UTF-8 decoding error: " + e);
            }
        }
        return cid2;
    }
}
