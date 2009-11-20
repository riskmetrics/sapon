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

package org.apache.axiom.om.impl.builder;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.OMContainerEx;
import org.apache.axiom.om.impl.OMNodeEx;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public class SAXOMBuilder extends DefaultHandler implements LexicalHandler {
    private OMElement root = null;

    private OMNode lastNode = null;

    private OMElement nextElem = null;

    private OMFactory factory = OMAbstractFactory.getOMFactory();

    //private List prefixMappings = new ArrayList();

    private int textNodeType = OMNode.TEXT_NODE;

    @Override
	public void setDocumentLocator(Locator arg0) {
    }

    @Override
	public void startDocument() throws SAXException {

    }

    @Override
	public void endDocument() throws SAXException {
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    protected OMElement createNextElement(String localName) throws OMException {
        OMElement e;
        if (lastNode == null) {
            root = e = factory.createOMElement(localName, null, null, null);
        } else if (lastNode.isComplete()) {
            e = factory.createOMElement(localName, null, lastNode.getParent(),
                                        null);
            ((OMNodeEx) lastNode).setNextOMSibling(e);
            ((OMNodeEx) e).setPreviousOMSibling(lastNode);
        } else {
            OMContainerEx parent = (OMContainerEx) lastNode;
            e = factory.createOMElement(localName, null, (OMElement) lastNode,
                                        null);
            parent.setFirstChild(e);
        }
        return e;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String,
     *      java.lang.String)
     */
    @Override
	public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        if (nextElem == null) {
			nextElem = createNextElement(null);
		}
        if (prefix.length() == 0) {
            nextElem.declareDefaultNamespace(uri);
        } else {
            nextElem.declareNamespace(uri, prefix);
        }
    }

    @Override
	public void endPrefixMapping(String arg0) throws SAXException {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
	public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts) throws SAXException {
        if (localName == null || localName.trim().equals("")) {
			localName = qName.substring(qName.indexOf(':') + 1);
		}
        if (nextElem == null) {
			nextElem = createNextElement(localName);
		} else {
			nextElem.setLocalName(localName);
		}

        nextElem.setNamespace(nextElem.findNamespace(namespaceURI, null));

        int j = atts.getLength();
        for (int i = 0; i < j; i++) {
            // Note that some SAX parsers report namespace declarations as attributes in addition
            // to calling start/endPrefixMapping.
            // NOTE: This filter was introduced to make SAXOMBuilder work with some versions of
            //       XMLBeans (2.3.0). It is not clear whether this is a bug in XMLBeans or not.
            //       See http://forum.springframework.org/showthread.php?t=43958 for a discussion.
            //       If this test causes problems with other parsers, don't hesitate to remove it.
            if (!atts.getQName(i).startsWith("xmlns")) {
                OMAttribute attr = nextElem.addAttribute(atts.getLocalName(i), atts.getValue(i),
                        nextElem.findNamespace(atts.getURI(i), null));

                attr.setAttributeType(atts.getType(i));
            }
        }

        lastNode = nextElem;
        nextElem = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
	public void endElement(String arg0, String arg1, String arg2)
            throws SAXException {
        if (lastNode.isComplete()) {
            OMContainer parent = lastNode.getParent();
            ((OMNodeEx) parent).setComplete(true);
            lastNode = (OMNode) parent;
        } else {
            OMElement e = (OMElement) lastNode;
            ((OMNodeEx) e).setComplete(true);
        }
    }

    public void startCDATA() throws SAXException {
        textNodeType = OMNode.CDATA_SECTION_NODE;
    }

    public void endCDATA() throws SAXException {
        textNodeType = OMNode.TEXT_NODE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
	public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (lastNode == null) {
            throw new SAXException("");
        }
        OMNode node;
        if (lastNode.isComplete()) {
            node = factory.createOMText(lastNode.getParent(),
                    new String(ch, start, length), textNodeType);
            ((OMNodeEx) lastNode).setNextOMSibling(node);
            ((OMNodeEx) node).setPreviousOMSibling(lastNode);
        } else {
            OMContainerEx e = (OMContainerEx) lastNode;
            node = factory.createOMText(e, new String(ch, start, length), textNodeType);
            e.setFirstChild(node);
        }
        lastNode = node;
    }

    @Override
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
            throws SAXException {
    }

    @Override
	public void processingInstruction(String arg0, String arg1)
            throws SAXException {
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        if (lastNode == null) {
            // Do nothing: the comment appears before the root element.
            return;
        }
        OMNode node;
        if (lastNode.isComplete()) {
            node = factory.createOMComment(lastNode.getParent(),
                    new String(ch, start, length));
            ((OMNodeEx) lastNode).setNextOMSibling(node);
            ((OMNodeEx) node).setPreviousOMSibling(lastNode);
        } else {
            OMContainerEx e = (OMContainerEx) lastNode;
            node = factory.createOMComment(e, new String(ch, start, length));
            e.setFirstChild(node);
        }
        lastNode = node;
    }

    @Override
	public void skippedEntity(String arg0) throws SAXException {
    }

    public void startEntity(String name) throws SAXException {
    }

    public void endEntity(String name) throws SAXException {
    }

    /**
     * Get the root element of the Axiom tree built by this content handler.
     *
     * @return the root element of the tree
     * @throws OMException if the tree is not complete
     */
    public OMElement getRootElement() {
        if (root != null && root.isComplete()) {
            return root;
        } else {
            throw new OMException("Tree not complete");
        }
    }
}
