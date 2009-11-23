/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.axis2.format;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMText;

/**
 * Utility class with methods to work on {@link OMElement} objects.
 * <p>
 * NOTICE: The code in this class will be moved to Axiom (or somewhere else). Use with care!
 */
public class ElementHelper {
    private ElementHelper() {}
    
    /**
     * Returns a stream representing the concatenation of the text nodes that are children of a
     * given element.
     * The stream returned by this method produces exactly the same character sequence as the
     * the stream created by the following expression:
     * <pre>new StringReader(element.getText())</pre>
     * The difference is that the stream implementation returned by this method is guaranteed
     * to have constant memory usage and is optimized for performance.
     * 
     * @param element the element to read the text nodes from
     * @param cache whether to enable caching when accessing the element
     * @return a stream representing the concatenation of the text nodes
     * 
     * @see OMElement#getText()
     */
    public static Reader getTextAsStream(OMElement element, boolean cache) {
        // If the element is not an OMSourcedElement and has not more than one child, then the most
        // efficient way to get the Reader is to build a StringReader
        if (!(element instanceof OMSourcedElement) && (!cache || element.isComplete())) {
            OMNode child = element.getFirstOMChild();
            if (child == null) {
                return new StringReader("");
            } else if (child.getNextOMSibling() == null) {
                return new StringReader(child instanceof OMText ? ((OMText)child).getText() : "");
            }
        }
        // In all other cases, extract the data from the XMLStreamReader
        return new TextFromElementReader(cache ? element.getXMLStreamReader()
                : element.getXMLStreamReaderWithoutCaching());
    }
    
    /**
     * Write the content of the text nodes that are children of a given element to a
     * {@link Writer}.
     * If <code>cache</code> is true, this method has the same effect as the following instruction:
     * <pre>out.write(element.getText())</pre>
     * The difference is that this method is guaranteed to have constant memory usage and is
     * optimized for performance.
     * 
     * @param element the element to read the text nodes from
     * @param out the stream to write the content to
     * @param cache whether to enable caching when accessing the element
     * @throws XMLStreamException if an error occurs when reading from the element
     * @throws IOException if an error occurs when writing to the stream
     * 
     * @see OMElement#getText()
     */
    public static void writeTextTo(OMElement element, Writer out, boolean cache)
            throws XMLStreamException, IOException {
        
        XMLStreamReader reader = cache ? element.getXMLStreamReader()
                : element.getXMLStreamReaderWithoutCaching();
        int depth = 0;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.CHARACTERS:
                case XMLStreamReader.CDATA:
                    if (depth == 1) {
                        out.write(reader.getText());
                    }
                    break;
                case XMLStreamReader.START_ELEMENT:
                    depth++;
                    break;
                case XMLStreamReader.END_ELEMENT:
                    depth--;
            }
        }
    }
}
