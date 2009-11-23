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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;

/**
 * This is the superclass of all mediators, and defines common logging,
 * tracing other aspects
 * for all mediators who extend from this.
 * elements of a mediator class.
 */
public abstract class AbstractMediator implements Mediator, AspectConfigurable
{
    /**
     * The standard log for mediators, subclass must assign the logger.
     */
    protected static Log log;

    private AspectConfiguration aspectConfiguration;

    /**
     * A constructor that makes subclasses pick up the correct logger
     */
    protected AbstractMediator() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * Returns the class name of the mediator
     * @return the class name of the mediator
     */
    public String getType() {
        String cls = getClass().getName();
        int p = cls.lastIndexOf(".");
        if (p == -1) {
			return cls;
		} else {
			return cls.substring(p + 1);
		}
    }

    /**
     * Perform an error log message to all logs @ ERROR. Writes to the general log, the service log
     * and the trace log (of trace is on) and throws a SynapseException
     * @param msg the log message
     * @param msgContext the message context
     */
    protected void handleException(String msg, SynapseMessageContext msgContext) {
        log.error(msg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg);
        }
        throw new SynapseException(msg);
    }

    /**
     * Perform an error log message to all logs @ ERROR. Writes to the general log, the service log
     * and the trace log (of trace is on) and throws a SynapseException
     * @param msg the log message
     * @param e an Exception encountered
     * @param msgContext the message context
     */
    protected void handleException(String msg, Exception e, SynapseMessageContext msgContext) {
        log.error(msg, e);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg, e);
        }
        throw new SynapseException(msg, e);
    }

    public boolean isStatisticsEnable() {
        return this.aspectConfiguration != null
                && this.aspectConfiguration.isStatisticsEnable();
    }

    public void disableStatistics() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.disableStatistics();
        }
    }

    public void enableStatistics() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.enableStatistics();
        }
    }

    /**
     * Configure aspects according to the given configuration
     *
     * @param aspectConfiguration AspectConfiguration instance
     */
    public void configure(AspectConfiguration aspectConfiguration) {
       this.aspectConfiguration = aspectConfiguration;
    }

    /**
     * Get the aspects  configuration
     *
     * @return AspectConfiguration instance
     */
    public AspectConfiguration getAspectConfiguration() {
        return aspectConfiguration;
    }
}
