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

package org.apache.synapse.util;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.DummyMessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;
import org.apache.synapse.mediators.MediatorUtils;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.SimpleVariableContext;

/**
 *
 */
public class SynapseXPathTest extends TestCase {

    String message = "This is XPath test";

    public void testStringXPath() throws Exception {
        SynapseXPath xpath = SynapseXPath.parseXPathString("$body//{http://somens}test");
        SynapseMessageContext ctx =  MediatorUtils.getTestContext("<m0:test xmlns:m0=\"http://somens\">" + message + "</m0:test>");
        assertEquals(message, xpath.stringValueOf(ctx));
    }

    public void testStringXPath2() throws Exception {
        SynapseXPath xpath = SynapseXPath.parseXPathString("$body//{http://somens}test/{http://someother}another");
        SynapseMessageContext ctx =  MediatorUtils.getTestContext("<m0:test xmlns:m0=\"http://somens\"><m1:another xmlns:m1=\"http://someother\">" + message + "</m1:another></m0:test>");
        assertEquals(message, xpath.stringValueOf(ctx));
    }

    public void testAbsoluteXPath() throws Exception {
        SynapseXPath xpath = new SynapseXPath("//test");
        SynapseMessageContext ctx =  MediatorUtils.getTestContext("<test>" + message + "</test>");
        assertEquals(message, xpath.stringValueOf(ctx));
    }

    public void testBodyRelativeXPath() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$body/test");
        SynapseMessageContext ctx =  MediatorUtils.getTestContext("<test>" + message + "</test>");
        assertEquals(message, xpath.stringValueOf(ctx));
        Object node = xpath.selectSingleNode(ctx);
        assertTrue(node instanceof OMElement);
        assertEquals(message, ((OMElement)node).getText());
    }

    public void testHeaderRelativeXPath() throws Exception {
        SynapseMessageContext ctx =  MediatorUtils.getTestContext("<test>" + message + "</test>");
        OMFactory fac = ctx.getEnvelope().getOMFactory();
        OMNamespace ns = fac.createOMNamespace("http://test", "t");
        ctx.getEnvelope().getHeader().addHeaderBlock("test", ns).setText(message);
        ctx.getEnvelope().getHeader().addHeaderBlock("test2", ns);

        SynapseXPath xpath = new SynapseXPath("$header/t:test");
        xpath.addNamespace(ns);
        assertEquals(message, xpath.stringValueOf(ctx));

        xpath = new SynapseXPath("$header/*");
        assertEquals(2, xpath.selectNodes(ctx).size());
    }

    public void testContextProperties() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$ctx:test");
        SynapseMessageContext synCtx = new DummyMessageContext();
        synCtx.setProperty("test", message);
        assertEquals(xpath.evaluate(synCtx), message);
    }

    public void testAxis2ContextProperties() throws Exception {
        Axis2SynapseMessageContext synCtx = MediatorUtils.getAxis2MessageContext("<test/>", null);
        synCtx.getAxis2MessageContext().setProperty("test", message);
        synCtx.getAxis2MessageContext().setProperty("test2", "1234");
        assertEquals(message, new SynapseXPath("$axis2:test").evaluate(synCtx));
        assertEquals(1234, new SynapseXPath("$axis2:test2").numberValueOf(synCtx).intValue());
        assertTrue(new SynapseXPath("$axis2:test2 = 1234").booleanValueOf(synCtx));
    }

    public void testStandardXPathFunctions() throws Exception {
        SynapseMessageContext ctx = MediatorUtils.getTestContext("<test>123456</test>");
        assertEquals(6, new SynapseXPath("string-length(//test)").numberValueOf(ctx).intValue());
    }

    public void testCustomVariables() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$myvar");
        SimpleVariableContext variableContext = new SimpleVariableContext();
        variableContext.setVariableValue("myvar", "myvalue");
        xpath.setVariableContext(variableContext);
        assertEquals("myvalue", xpath.evaluate(MediatorUtils.getTestContext("<test/>")));
    }
}
