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
package org.apache.synapse.endpoints;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.clustering.Member;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.LoadBalanceMembershipHandler;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;
import org.apache.synapse.endpoints.algorithms.AlgorithmContext;

/**
 * Represents a dynamic load balance endpoint. The application membership is not static, but discovered
 * through some mechanism such as using a GCF
 */
public class DynamicLoadbalanceEndpoint extends LoadbalanceEndpoint {

    private static final Log log = LogFactory.getLog(DynamicLoadbalanceEndpoint.class);

    /**
     * The algorithm context , place holder for keep any runtime states related to the load balance
     * algorithm
     */
    private AlgorithmContext algorithmContext;

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        ConfigurationContext cc =
                ((Axis2SynapseEnvironment) synapseEnvironment).getAxis2ConfigurationContext();
        if (!initialized) {
            super.init(synapseEnvironment);
            if (algorithmContext == null) {
                algorithmContext = new AlgorithmContext(isClusteringEnabled, cc, getName());
            }
        }
        log.info("Dynamic load balance endpoint initialized");
    }

    private LoadBalanceMembershipHandler lbMembershipHandler;

    public DynamicLoadbalanceEndpoint() {
    }

    public void setLoadBalanceMembershipHandler(LoadBalanceMembershipHandler lbMembershipHandler) {
        this.lbMembershipHandler = lbMembershipHandler;
    }

    public LoadBalanceMembershipHandler getLbMembershipHandler() {
        return lbMembershipHandler;
    }

    public void send(SynapseMessageContext synCtx) {
        EndpointReference to = synCtx.getTo();
        DynamicLoadbalanceFaultHandler faultHandler = new DynamicLoadbalanceFaultHandler(to);
        if (isFailover()) {
            synCtx.pushFaultHandler(faultHandler);
        }
        ConfigurationContext configCtx =
                ((Axis2SynapseMessageContext) synCtx).getAxis2MessageContext().getConfigurationContext();
        if (lbMembershipHandler.getConfigurationContext() == null) {
            lbMembershipHandler.setConfigurationContext(configCtx);
        }
//        algorithmContext.setConfigurationContext(configCtx);
        sendToApplicationMember(synCtx, to, faultHandler);
    }

    public void setName(String name) {
        super.setName(name);
//        algorithmContext.setContextID(name);
    }

    private void sendToApplicationMember(SynapseMessageContext synCtx,
                                         EndpointReference to,
                                         DynamicLoadbalanceFaultHandler faultHandler) {
        org.apache.axis2.context.MessageContext axis2MsgCtx =
                ((Axis2SynapseMessageContext) synCtx).getAxis2MessageContext();

        String transport = axis2MsgCtx.getTransportIn().getName();
        Member currentMember =
                lbMembershipHandler.getNextApplicationMember(algorithmContext);
        faultHandler.setCurrentMember(currentMember);
        if (currentMember != null) {

            // URL rewrite
            if (transport.equals("http") || transport.equals("https")) {
                String address = to.getAddress();
                if (address.indexOf(":") != -1) {
                    try {
                        address = new URL(address).getPath();
                    } catch (MalformedURLException e) {
                        String msg = "URL " + address + " is malformed";
                        log.error(msg, e);
                        throw new SynapseException(msg, e);
                    }
                }
                EndpointReference epr =
                        new EndpointReference(transport + "://" + currentMember.getHostName() +
                                              ":" + currentMember.getHttpPort() + address);
                synCtx.setTo(epr);
                if (isFailover()) {
                    synCtx.getEnvelope().build();
                }

                AddressEndpoint endpoint = new AddressEndpoint();
                endpoint.setName("DynamicLoadBalanceAddressEndpoint-" + Math.random());
                EndpointDefinition definition = new EndpointDefinition();
                definition.setAddress(epr.getAddress());
                endpoint.setDefinition(definition);
                endpoint.init((SynapseEnvironment)
                        ((Axis2SynapseMessageContext) synCtx).getAxis2MessageContext().
                        getConfigurationContext().getAxisConfiguration().
                        getParameterValue(SynapseConstants.SYNAPSE_ENV));
                endpoint.send(synCtx);
            } else {
                log.error("Cannot load balance for non-HTTP/S transport " + transport);
            }
        } else {
            synCtx.getFaultStack().pop(); // Remove the DynamicLoadbalanceFaultHandler
            String msg = "No application members available";
            log.error(msg);
            throw new SynapseException(msg);
        }
    }

    /**
     * This FaultHandler will try to resend the message to another member if an error occurs
     * while sending to some member. This is a failover mechanism
     */
    private class DynamicLoadbalanceFaultHandler extends FaultHandler {

        private EndpointReference to;
        private Member currentMember;

        public void setCurrentMember(Member currentMember) {
            this.currentMember = currentMember;
        }

        private DynamicLoadbalanceFaultHandler(EndpointReference to) {
            this.to = to;
        }

        public void onFault(SynapseMessageContext synCtx) {
            if (currentMember == null) {
                return;
            }
            synCtx.pushFaultHandler(this);
            sendToApplicationMember(synCtx, to, this);
        }
    }
}

