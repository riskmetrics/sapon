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

package org.apache.synapse.core;

import java.util.concurrent.ExecutorService;

import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.aspects.statistics.StatisticsCollector;
import org.apache.synapse.commons.util.TemporaryData;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.endpoints.EndpointDefinition;

/**
 * The SynapseEnvironment allows access into the the host SOAP engine.
 * It allows the sending of messages, classloader access etc.
 */
public interface SynapseEnvironment {

	/**
	 * This method allows a message to be sent through the underlying SOAP
	 * engine. This will send request messages on (forward), and send the
	 * response messages back to the client
	 *
	 * @param endpoint
	 *            - Endpoint to be used for sending
	 * @param smc
	 *            - Synapse MessageContext to be sent
	 */
    void send(EndpointDefinition endpoint, SynapseMessageContext smc);

    /**
     * Creates a new Synapse <code>MessageContext</code> instance.
     *
     * @return a MessageContext
     */
    SynapseMessageContext createMessageContext();

	/**
	 * Creates a new <code>TemporaryData</code> instance for the temp storage
	 * requirements
	 *
	 * @return a TemporaryData created from the parameters provided in the
	 *         synapse.properties
	 */
    TemporaryData createTemporaryData();

    /**
     * This method returns the StatisticsCollector.
     *
     * @return Returns the StatisticsCollector
     */
    StatisticsCollector getStatisticsCollector();

    /**
     * To set the StatisticsCollector to the environment
     *
     * @param statisticsCollector - StatisticsCollector to be set
     */
    void setStatisticsCollector(StatisticsCollector statisticsCollector);

    /**
     * This is used by anyone who needs access to a SynapseThreadPool.
     * It offers the ability to start work.
     *
     * @return Returns the ExecutorService
     */
    ExecutorService getExecutorService();

    /**
     * Has the Synapse Environment properly initialized?
     *
     * @return true if the environment is ready for processing
     */
    boolean isInitialized();

    /**
     * Set the environment as ready for message processing
     *
     * @param state true means ready for processing
     */
    void setInitialized(boolean state);

	/**
	 * Retrieves the {@link SynapseConfiguration} from the
	 * <code>environment</code>
	 *
	 * @return configuration of the synapse
	 */
    SynapseConfiguration getSynapseConfiguration();
}
