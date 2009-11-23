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

package org.apache.synapse.registry.url;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.synapse.config.Entry;
import org.apache.synapse.registry.Registry;

public class SimpleURLRegistryTest extends TestCase {

	private static final String ROOT = SimpleURLRegistryTest.class.getResource("/conf").getFile();
    private static final String FILE = "text.xml";
    private static final String FILE_FULL = ROOT + "/" + FILE;
    private static final String FILE2 = "large_file.xml";
    private static final String FILE2_FULL = ROOT + "/" + FILE2;
    private static final String TEXT_1 = "text1";
    private static final String TEXT_1_XML = "<text1 />";
    private static final String TEXT_2 = "text2";
    private static final String TEXT_2_XML = "<text2 />";

    @Override
	public void setUp() throws Exception {
        writeToFile(TEXT_1_XML);

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement root = factory.createOMElement("root", null);
        for (int i=0; i<1000; i++) {
            OMElement child = factory.createOMElement("child", null);
            child.setText("some text");
            root.addChild(child);
        }
        FileOutputStream out = new FileOutputStream(FILE2_FULL);
        root.serialize(out);
        out.close();
    }

    public void testRegistry() throws Exception {
        Registry reg = new SimpleURLRegistry();
        Properties props = new Properties();
        props.put("root", getClass().getResource("/conf").toString());
        props.put("cachableDuration", "1500");
        reg.init(props);
        Entry prop = new Entry();
        prop.setType(Entry.REMOTE_ENTRY);
        prop.setKey(FILE);

        // initial load of file from registry
        assertTrue(isEmptyTag(TEXT_1, reg.getResource(prop).toString()));

        // sleep 1 sec
        Thread.sleep(1000);
        assertTrue(isEmptyTag(TEXT_1, reg.getResource(prop).toString()));

        // sleep another 1 sec, has expired in cache, but content hasnt changed
        Thread.sleep(1000);
        assertTrue(isEmptyTag(TEXT_1, reg.getResource(prop).toString()));

        // the renewed cache should be valid for another 1.5 secs
        // change the file now and change next cache duration
        writeToFile(TEXT_2_XML);
        props.put("cachableDuration", "100");
        reg.init(props);
        // still cached content should be available and valid
        assertTrue(isEmptyTag(TEXT_1, reg.getResource(prop).toString()));

        // now sleep ~1 sec, still cache should be valid
        Thread.sleep(800);
        assertTrue(isEmptyTag(TEXT_1, reg.getResource(prop).toString()));

        // sleep another 1 sec.. cache should expire and new content should be loaded
        Thread.sleep(1000);
        assertTrue(isEmptyTag(TEXT_2, reg.getResource(prop).toString()));

        // change content back to original
        writeToFile(TEXT_1_XML);

        // sleep for .5 sec, now the new content should be loaded as new expiry time
        // is .1 sec
        Thread.sleep(500);
        assertTrue(isEmptyTag(TEXT_1, reg.getResource(prop).toString()));
    }

    public void testLargeFile() throws Exception {
        Registry reg = new SimpleURLRegistry();
        Properties props = new Properties();
        props.put("root", getClass().getResource("/conf").toString());
        props.put("cachableDuration", "1500");
        reg.init(props);

        OMNode node = reg.lookup(FILE2);
        node.serialize(new NullOutputStream());
    }

    public void testXPathEvaluationOnRegistryResource() throws Exception {
        SimpleURLRegistry registry = new SimpleURLRegistry();
        OMNode omNode =
                registry.lookup(
                        getClass().getResource("/org/apache/synapse/core/registry/resource.xml").toString());

        assertNotNull(omNode);

        AXIOMXPath xpath = new AXIOMXPath("//table/entry[@id='one']/value/child::text()");
        OMNode node = (OMNode) xpath.selectSingleNode(omNode);

        assertNotNull(node);
        assertTrue(node instanceof OMText);
        assertEquals("ValueOne", ((OMText) node).getText());
    }

    @Override
	public void tearDown() throws Exception {
        new File(FILE_FULL).delete();
        new File(FILE2_FULL).delete();
    }

    private void writeToFile(String content) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(FILE_FULL)));
        out.write(content);
        out.close();
    }

    private boolean isEmptyTag(String target, String totest) {
    	return ("<"+target+"/>").equals(totest) ||
    		   ("<"+target+" />").equals(totest) ||
    		   ("<"+target+"></"+target+">").equals(totest);
    }
}
