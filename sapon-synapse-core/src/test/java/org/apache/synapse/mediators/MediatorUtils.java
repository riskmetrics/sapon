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

package org.apache.synapse.mediators;

import java.util.Map;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OldMessageContext;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.DummyMessageContextBuilder;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContextImpl;

public class MediatorUtils {

    public static SynapseMessageContext getTestContext(String bodyText, Map<String,Entry> props) throws Exception {
        DummyMessageContextBuilder builder = new DummyMessageContextBuilder();
        builder.setBodyFromString(bodyText);
        if (props != null) {
            for (Map.Entry<String,Entry> mapEntry : props.entrySet()) {
                builder.addEntry(mapEntry.getKey(), mapEntry.getValue());
            }
        }
        return builder.build();
    }

    public static Axis2SynapseMessageContext getAxis2MessageContext(String bodyText,
                                                             Map<String,Entry> props) throws Exception {
        DummyMessageContextBuilder builder = new DummyMessageContextBuilder();
        builder.setRequireAxis2MessageContext(true);
        builder.setBodyFromString(bodyText);
        if (props != null) {
            for (Map.Entry<String,Entry> mapEntry : props.entrySet()) {
                builder.addEntry(mapEntry.getKey(), mapEntry.getValue());
            }
        }
        return (Axis2SynapseMessageContext)builder.build();
    }

    public static SynapseMessageContext getTestContext(String bodyText) throws Exception {
        return getTestContext(bodyText, null);
    }

    public static SynapseMessageContext createLightweightSynapseMessageContext(
            String payload) throws Exception {
        MessageContext mc = new OldMessageContext();
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(config);
        SynapseMessageContext synMc = Axis2SynapseMessageContextImpl.newInstance(mc, config, env);
        SOAPEnvelope envelope =
                OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc =
                OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        envelope.getBody().addChild(createOMElement(payload));

        synMc.setEnvelope(envelope);
        return synMc;
    }

    public static OMElement createOMElement(String xml) {
        return SynapseConfigUtils.stringToOM(xml);
    }

}
