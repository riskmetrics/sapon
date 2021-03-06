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

import java.util.concurrent.ExecutorService;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OldMessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.aspects.statistics.StatisticsCollector;
import org.apache.synapse.aspects.statistics.StatisticsReporter;
import org.apache.synapse.commons.util.TemporaryData;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.dispatch.Dispatcher;
import org.apache.synapse.util.UUIDGenerator;
import org.apache.synapse.util.concurrent.SynapseThreadPool;

/**
 * This is the Axis2 implementation of the SynapseEnvironment
 */
public class Axis2SynapseEnvironment
	implements SynapseEnvironment
{

    private static final Log log = LogFactory.getLog(Axis2SynapseEnvironment.class);

    private SynapseConfiguration synapseConfig;
    private ConfigurationContext configContext;
    private ExecutorService executorService;
    private boolean initialized = false;

    private StatisticsCollector statisticsCollector;

    public Axis2SynapseEnvironment(SynapseConfiguration synCfg) {
    	this.synapseConfig = synCfg;

        int coreThreads = SynapseThreadPool.SYNAPSE_CORE_THREADS;
        int maxThreads  = SynapseThreadPool.SYNAPSE_MAX_THREADS;
        long keepAlive  = SynapseThreadPool.SYNAPSE_KEEP_ALIVE;
        int qlength     = SynapseThreadPool.SYNAPSE_THREAD_QLEN;

        try {
            qlength = Integer.parseInt(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_QLEN));
        } catch (Exception ignore) {}

        try {
            coreThreads = Integer.parseInt(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_CORE));
        } catch (Exception ignore) {}

        try {
            maxThreads = Integer.parseInt(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_MAX));
        } catch (Exception ignore) {}

        try {
            keepAlive = Long.parseLong(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_ALIVE));
        } catch (Exception ignore) {}

        this.executorService = new SynapseThreadPool(coreThreads, maxThreads, keepAlive, qlength,
            synCfg.getProperty(SynapseThreadPool.SYN_THREAD_GROUP,
                SynapseThreadPool.SYNAPSE_THREAD_GROUP),
            synCfg.getProperty(SynapseThreadPool.SYN_THREAD_IDPREFIX,
                SynapseThreadPool.SYNAPSE_THREAD_ID_PREFIX));
    }

    public Axis2SynapseEnvironment(	final ConfigurationContext cfgCtx,
    								final SynapseConfiguration synapseConfig)
    {
        this(synapseConfig);
        this.configContext = cfgCtx;
    }

    /**
     * This will be used for sending the message provided, to the endpoint
     * specified by the EndpointDefinition using the axis2 environment.
     *
     * @param endpoint - EndpointDefinition to be used to find the endpoint
     *                   information and the properties of the sending process
     * @param synCtx   - Synapse MessageContext to be sent
     */
    public void send(EndpointDefinition endpoint, SynapseMessageContext synCtx) {
        if (synCtx.isResponse()) {
        	StatisticsReporter.reportForAll(synCtx);
            if (endpoint != null) {
                Axis2Sender.sendOn(endpoint, synCtx);
            } else {
                Axis2Sender.sendBack(synCtx);
            }
        } else {
            // If this request is related to session affinity endpoints - For client initiated session
            Dispatcher dispatcher =
                    (Dispatcher) synCtx.getProperty(
                            SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_DISPATCHER);
            if (dispatcher != null) {
                if (!dispatcher.isServerInitiatedSession()) {
                    dispatcher.updateSession(synCtx);
                }
            }
            Axis2Sender.sendOn(endpoint, synCtx);
        }
    }

    /**
     * This method will be used to create a new MessageContext in the Axis2
     * environment for Synapse. This will set all the relevant parts to the
     * MessageContext, but for this MessageContext to be useful creator has to
     * fill in the data like envelope and operation context and so on. This
     * will set a default envelope of type soap12 and a new messageID for the
     * created message along with the ConfigurationContext is being set in to
     * the message correctly.
     *
     * @return Synapse MessageContext with the underlying axis2 message context set
     */
    public SynapseMessageContext createMessageContext() {

    	log.debug("Creating Message Context");

        MessageContext axis2MC = new OldMessageContext();
        axis2MC.setConfigurationContext(this.configContext);

        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        SynapseMessageContext smc
        	= Axis2SynapseMessageContextImpl.newInstance(axis2MC, synapseConfig, this);
        smc.setMessageID(UUIDGenerator.getUUID());
        try {
			smc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
			smc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
		} catch (Exception e) {
            handleException("Unable to attach the SOAP envelope to " +
                    "the created new message context", e);
        }

        return smc;
    }

    /**
     * Factory method to create the TemporaryData object as per on the parameters specified in the
     * synapse.properties file, so that the TemporaryData parameters like threashold chunk size
     * can be customized by using the properties file. This can be extended to enforce further
     * policies if required in the future.
     *
     * @return created TemporaryData object as per in the synapse.properties file
     */
    public TemporaryData createTemporaryData() {

        String chkSize = synapseConfig.getProperty(SynapseConstants.CHUNK_SIZE);
        String chukNumber = synapseConfig.getProperty(SynapseConstants.THRESHOLD_CHUNKS);
        int numberOfChunks = SynapseConstants.DEFAULT_THRESHOLD_CHUNKS;
        int chunkSize = SynapseConstants.DEFAULT_CHUNK_SIZE;

        if (chkSize != null) {
            chunkSize = Integer.parseInt(chkSize);
        }

        if (chukNumber != null) {
            numberOfChunks = Integer.parseInt(chukNumber);
        }

        String tempPrefix = synapseConfig.getProperty(SynapseConstants.TEMP_FILE_PREFIX,
                SynapseConstants.DEFAULT_TEMPFILE_PREFIX);
        String tempSuffix = synapseConfig.getProperty(SynapseConstants.TEMP_FILE_SUFIX,
                SynapseConstants.DEFAULT_TEMPFILE_SUFIX);

        return new TemporaryData(numberOfChunks, chunkSize, tempPrefix, tempSuffix);
    }

    /**
     * This method returns the StatisticsCollector
     *
     * @return Returns the StatisticsCollector
     */
    public StatisticsCollector getStatisticsCollector() {
        return statisticsCollector;
    }

    /**
     * To set the StatisticsCollector
     *
     * @param collector - Statistics collector to be set
     */
    public void setStatisticsCollector(StatisticsCollector collector) {
        this.statisticsCollector = collector;
    }

    /**
     * This will give the access to the synapse thread pool for the
     * advanced mediation tasks.
     *
     * @return an ExecutorService to execute the tasks in a new thread from the pool
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Has this environment properly initialized?
     *
     * @return true if ready for processing
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Mark this environment as ready for processing
     *
     * @param state true means ready for processing
     */
    public void setInitialized(boolean state) {
        this.initialized = state;
    }

    /**
     * Retrieves the {@link SynapseConfiguration} from the <code>environment</code>
     *
     * @return synapseConfig associated with the enviorenment
     */
    public SynapseConfiguration getSynapseConfiguration() {
        return this.synapseConfig;
    }

    /**
     * Retrieves the {@link ConfigurationContext} associated with this <code>axis2SynapseEnv</code>
     *
     * @return configContext of the axis2 synapse environment
     */
    public ConfigurationContext getAxis2ConfigurationContext() {
        return this.configContext;
    }

    private void handleException(String message, Throwable e) {
        log.error(message, e);
        throw new SynapseException(message, e);
    }


}
