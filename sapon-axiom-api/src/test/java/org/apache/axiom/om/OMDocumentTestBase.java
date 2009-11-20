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

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.apache.axiom.om.impl.builder.StAXOMBuilder;

public class OMDocumentTestBase extends TestCase {
    private String sampleXML = "<?xml version='1.0' encoding='utf-8'?>" +
            "<!--This is some comments at the start of the document-->" +
            "<?PITarget PIData?>" +
            "<Axis2>" +
            "    <ProjectName>The Apache Web Sevices Project</ProjectName>" +
            "</Axis2>";

    private OMImplementation omImplementation;

    public OMDocumentTestBase(OMImplementation omImplementation) {
    	setOMImplementation(omImplementation);
    }

    public void setOMImplementation(OMImplementation omImplementation) {
        this.omImplementation = omImplementation;
    }

    public void testParse() {
        checkSampleXML(getSampleOMDocument(sampleXML));
    }

    public void testSerializeAndConsume() throws XMLStreamException {
        // read the string in to the builder
        OMDocument omDocument = getSampleOMDocument(sampleXML);

        // serialise it to a string
        String outXML = "";
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        omDocument.serializeAndConsume(outStream);
        outXML = new String(outStream.toByteArray());

        // again load that to another builder
        checkSampleXML(getSampleOMDocument(outXML));
    }

    private void checkSampleXML(OMDocument document) {
        // check for the comment and the PI
        boolean commentFound = false;
        boolean piFound = false;
        for(OMNode omNode: document.getChildren()) {
            if (omNode instanceof OMComment) {
                commentFound = true;
            } else if (omNode instanceof OMProcessingInstruction) {
                piFound = true;
            } else if (omNode instanceof OMElement && !commentFound && !piFound) {
                fail("OMElement should come after Comment and PI");

            }
        }
        assertTrue(commentFound && piFound);
    }

    /**
     * Test that a document that is not well formed triggers an appropriate error.
     */
    public void testMalformedDocument() {
        OMDocument document = getSampleOMDocument("<Root><Child attr='a' attr='a'/></Root>");
        try {
            document.serialize(new ByteArrayOutputStream());
            fail("Expected exception");
        } catch (Exception ex) {
            // We expect an exception here
        }
    }

    private OMDocument getSampleOMDocument(String xml) {
        try {
            XMLStreamReader xmlStreamReader =
                    XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            StAXOMBuilder builder =
                    new StAXOMBuilder(omImplementation.getOMFactory(), xmlStreamReader);
            return builder.getDocument();
        } catch (XMLStreamException e) {
            throw new UnsupportedOperationException();
        }
    }

//    private OMDocument getSampleOMDocument() {
//        OMFactory omFactory = OMAbstractFactory.getOMFactory();
//        OMDocument omDocument = omFactory.createOMDocument();
//        omFactory.createOMComment(omDocument, "This is some comments at the start of the document");
//        omDocument.setCharsetEncoding("utf-8");
//        omFactory.createOMProcessingInstruction(omDocument, "PITarget", "PIData");
//
//        OMElement documentElement = omFactory.createOMElement("Axis2", null, omDocument);
//        omDocument.setDocumentElement(documentElement);
//        omFactory.createOMElement("ProjectName", null, documentElement);
//        documentElement.getFirstElement().setText("The Apache Web Sevices Project");
//
//        return omDocument;
//    }

}
