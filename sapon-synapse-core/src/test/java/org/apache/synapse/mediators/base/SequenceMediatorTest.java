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

package org.apache.synapse.mediators.base;

import junit.framework.TestCase;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OldMessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.SynapseMessageReceiver;
import org.apache.synapse.mediators.DummyMediator;
import org.apache.synapse.mediators.MediatorUtils;
import org.apache.synapse.mediators.TestMediateHandler;

public class SequenceMediatorTest extends TestCase {

    private StringBuffer result = new StringBuffer();

    public void testSequenceMediator() throws Exception {

        DummyMediator t1 = new DummyMediator();
        t1.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T1.");
                }
            });
        DummyMediator t2 = new DummyMediator();
        t2.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T2.");
                }
            });
        DummyMediator t3 = new DummyMediator();
        t3.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T3");
                }
            });

        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);

        // invoke transformation, with static enveope
        SynapseMessageContext synCtx = MediatorUtils.getTestContext("<empty/>");
        seq.mediate(synCtx);

        assertTrue("T1.T2.T3".equals(result.toString()));
    }

    public void testErrorHandling() throws Exception {

        DummyMediator t1 = new DummyMediator();
        t1.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T1.");
                }
            });
        DummyMediator t2 = new DummyMediator();
        t2.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T2.");
                    throw new SynapseException("test");
                }
            });
        DummyMediator t3 = new DummyMediator();
        t3.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T3.");
                }
            });
        DummyMediator t4 = new DummyMediator();
        t4.setHandler(
            new TestMediateHandler() {
                public void handle(SynapseMessageContext synCtx) {
                    result.append("T4");
                    assertEquals("test", synCtx.getProperty(SynapseConstants.ERROR_MESSAGE));
                }
            });

        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        seq.setErrorHandler("myErrorHandler");

        SequenceMediator seqErr = new SequenceMediator();
        seqErr.setName("myErrorHandler");
        seqErr.addChild(t4);

        // invoke transformation, with static enveope
        SynapseConfiguration synConfig = new SynapseConfiguration();
        synConfig.addSequence("myErrorHandler", seqErr);
        synConfig.addSequence(SynapseConstants.MAIN_SEQUENCE_KEY, seq);

        SynapseEnvironment synEnv = new Axis2SynapseEnvironment(synConfig);
        MessageContext mc = new OldMessageContext();
        mc.setEnvelope(MediatorUtils.getTestContext("<empty/>").getEnvelope());

        new SynapseMessageReceiver(synEnv).receive(mc);

        assertTrue("T1.T2.T4".equals(result.toString()));
    }
}
