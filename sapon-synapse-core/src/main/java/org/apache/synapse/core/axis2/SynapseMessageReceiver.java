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

package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.aspects.AspectConfigurationDetectionStrategy;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.statistics.StatisticsReporter;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.MediatorFaultHandler;

/**
 * This message receiver should be configured in the Axis2 configuration as the
 * default message receiver, which will handle all incoming messages through the
 * synapse mediation
 */
public class SynapseMessageReceiver implements MessageReceiver {

    private static final Log log = LogFactory.getLog(SynapseMessageReceiver.class);

    private final SynapseEnvironment synEnv;

    public SynapseMessageReceiver(SynapseEnvironment env) {
    	this.synEnv = env;
    }

    protected SynapseMessageContext getSynapseMessageContext(
    		MessageContext axisMsgCtx) throws AxisFault
    {
    	if (synEnv == null) {
            String msg = "Synapse environment has not initialized properly..";
            log.fatal(msg);
            throw new SynapseException(msg);
        }
    	SynapseConfiguration synCfg = synEnv.getSynapseConfiguration();

        return Axis2SynapseMessageContextImpl.newInstance(axisMsgCtx, synCfg, synEnv);
    }

    public void receive(MessageContext mc)
    	throws AxisFault
    {
        SynapseMessageContext synCtx = getSynapseMessageContext(mc);

        StatisticsReporter.reportForComponent(synCtx,
                AspectConfigurationDetectionStrategy.getAspectConfiguration(synCtx),
                ComponentType.SEQUENCE);

        if (log.isDebugEnabled()) {
            log.debug("Synapse received a new message for message mediation...");
            log.debug("Received To: " +
                (mc.getTo() != null ? mc.getTo().getAddress() : "null"));
            log.debug("SOAPAction: " +
                (mc.getSoapAction() != null ? mc.getSoapAction() : "null"));
            log.debug("WSA-Action: " +
                (mc.getWSAAction() != null ? mc.getWSAAction() : "null"));

            if (log.isTraceEnabled()) {
                String[] cids = mc.getAttachments().getAllContentIDs();
                if (cids != null && cids.length > 0) {
                    for (String cid : cids) {
                        log.trace("Attachment : " + cid);
                    }
                }
                log.trace("Envelope : " + mc.getEnvelope());
            }
        }

        // get service log for this message and attach to the message context
        Log serviceLog = LogFactory.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX +
            SynapseConstants.SYNAPSE_SERVICE_NAME);
        ((Axis2SynapseMessageContext) synCtx).setServiceLog(serviceLog);

        try {
            // set default fault handler
            synCtx.pushFaultHandler(new MediatorFaultHandler(synCtx.getFaultSequence()));

            // invoke synapse message mediation through the main sequence
            synCtx.send();

        } catch (SynapseException syne) {
            if (!synCtx.getFaultStack().isEmpty()) {
                warn("Executing fault handler due to exception encountered", synCtx);
                (synCtx.getFaultStack().pop()).handleFault(synCtx, syne);

            } else {
                warn("Exception encountered but no fault handler found - " +
                    "message dropped", synCtx);
            }
        }
    }

    private void warn(String msg, SynapseMessageContext msgContext) {
        if (log.isDebugEnabled()) {
            log.warn(msg);
        }
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().warn(msg);
        }
    }
}
