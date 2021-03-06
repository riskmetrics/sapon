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

package org.apache.synapse;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OldMessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContextImpl;

/**
 * Utility class to build test message contexts in a flexible way.
 */
public class DummyMessageContextBuilder {
    private final Map<String,Entry> entries = new HashMap<String,Entry>();
    private boolean requireAxis2MessageContext;
    private String contentString;
    private String contentFile;
    private boolean contentIsEnvelope;
    private boolean addTextAroundBody;

    public DummyMessageContextBuilder setRequireAxis2MessageContext(boolean requireAxis2MessageContext) {
        this.requireAxis2MessageContext = requireAxis2MessageContext;
        return this;
    }

    public DummyMessageContextBuilder setBodyFromString(String string) {
        this.contentString = string;
        contentIsEnvelope = false;
        return this;
    }

    public DummyMessageContextBuilder setBodyFromFile(String path) {
        this.contentFile = path;
        contentIsEnvelope = false;
        return this;
    }

    public DummyMessageContextBuilder setEnvelopeFromFile(String path) {
        this.contentFile = path;
        contentIsEnvelope = true;
        return this;
    }

    public DummyMessageContextBuilder addTextAroundBody() {
        addTextAroundBody = true;
        return this;
    }

    public DummyMessageContextBuilder addEntry(String key, Entry entry) {
        entries.put(key, entry);
        return this;
    }

    public DummyMessageContextBuilder addEntry(String key, URL url) {
        Entry entry = new Entry();
        entry.setType(Entry.URL_SRC);
        entry.setSrc(url);
        entries.put(key, entry);
        return this;
    }

    public DummyMessageContextBuilder addFileEntry(String key, String path) throws MalformedURLException {
        return addEntry(key, new File(path).toURI().toURL());
    }

    /**
     * Build the test message context.
     * This method returns a new (and independent) instance on every invocation.
     *
     * @return
     * @throws Exception
     */
    public SynapseMessageContext build() throws Exception {
        SynapseConfiguration testConfig = new SynapseConfiguration();
        // TODO: check whether we need a SynapseEnvironment in all cases
        SynapseEnvironment synEnv
            = new Axis2SynapseEnvironment(new ConfigurationContext(new AxisConfiguration()),
                                          testConfig);
        SynapseMessageContext synCtx;
        if (requireAxis2MessageContext) {
            synCtx = Axis2SynapseMessageContextImpl.newInstance(new OldMessageContext(),
                                             testConfig, synEnv);
        } else {
            synCtx = new DummyMessageContext(testConfig, synEnv);
        }

        for (Map.Entry<String,Entry> mapEntry : entries.entrySet()) {
            testConfig.addEntry(mapEntry.getKey(), mapEntry.getValue());
        }

        XMLStreamReader parser = null;
        if (contentString != null) {
            parser = StAXUtils.createXMLStreamReader(new StringReader(contentString));
        } else if (contentFile != null) {
            parser = StAXUtils.createXMLStreamReader(new FileInputStream(contentFile));
        }

        SOAPEnvelope envelope;
        if (parser != null) {
            if (contentIsEnvelope) {
                envelope = new StAXSOAPModelBuilder(parser).getSOAPEnvelope();
            } else {
                envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();

                // TODO: don't know why this is here, but without it some unit tests fail...
                OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
                omDoc.addChild(envelope);

                SOAPBody body = envelope.getBody();
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                OMElement bodyElement = builder.getDocumentElement();
                if (addTextAroundBody) {
                    OMFactory fac = OMAbstractFactory.getOMFactory();
                    body.addChild(fac.createOMText("\n"));
                    body.addChild(bodyElement);
                    body.addChild(fac.createOMText("\n"));
                } else {
                    body.addChild(bodyElement);
                }
            }
        } else {
            envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        }

        synCtx.setEnvelope(envelope);
        return synCtx;
    }
}
