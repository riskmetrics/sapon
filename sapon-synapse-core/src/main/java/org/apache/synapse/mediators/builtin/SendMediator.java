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

package org.apache.synapse.mediators.builtin;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * SendMediator sends a message using specified semantics. If it contains an
 * endpoint it will send the message to that endpoint. Once a message is sent
 * to the endpoint further sending behaviors are completely governed by that
 * endpoint. If there is no endpoint available, SendMediator will send the
 * message to the implicitly stated destination.
 */
public class SendMediator extends AbstractMediator
	implements ManagedLifecycle
{
    private Endpoint endpoint = null;
    private boolean initialized = false;

    /**
     * This will call the send method on the messages with implicit message parameters
     * or else if there is an endpoint, with that endpoint parameters
     *
     * @param synCtx the current message to be sent
     * @return false always as this is a leaf mediator
     */
    public boolean mediate(SynapseMessageContext synCtx) {

        log.debug("Start : Send mediator");
        if (log.isTraceEnabled()) {
            log.trace("Message : " + synCtx.getEnvelope());
        }

        // if no endpoints are defined, send where implicitly stated
        if (endpoint == null) {

            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Sending ").append(synCtx.isResponse() ? "response" : "request")
                        .append(" message using implicit message properties..");
                sb.append("\nSending To: ").append(synCtx.getTo() != null ?
                        synCtx.getTo().getAddress() : "null");
                sb.append("\nSOAPAction: ").append(synCtx.getWSAAction() != null ?
                        synCtx.getWSAAction() : "null");
                log.debug(sb.toString());
            }

            if (log.isTraceEnabled()) {
                log.trace("Envelope : " + synCtx.getEnvelope());
            }
            synCtx.getEnvironment().send(null, synCtx);

        } else {
            endpoint.send(synCtx);
        }

        log.debug("End : Send mediator");

        return true;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void init(SynapseEnvironment synapseEnvironment) {
        if (endpoint != null) {
            endpoint.init(synapseEnvironment);
        }
        initialized = true;
    }

    public boolean isInitialized() {
    	return initialized;
    }

    public void destroy() {
        if (endpoint != null) {
            endpoint.destroy();
        }
    }
}
