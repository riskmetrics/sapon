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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is an abstract class that handles an unexpected error during Synapse
 * mediation, but looking at the stack of registered FaultHanders and invoking
 * on them as appropriate. Sequences and Endpoints would be Synapse entities
 * that handles faults. If such an entity is unable to handle an error
 * condition, then a SynapseException should be thrown, which triggers this
 * fault handling logic.
 */
public abstract class FaultHandler {

    private static final Log log = LogFactory.getLog(FaultHandler.class);

    public void handleFault(SynapseMessageContext synCtx)
    {
        if (log.isDebugEnabled()) {
            log.debug("FaultHandler executing impl: " + this.getClass().getName());
        }

        try {
            synCtx.getServiceLog().info("FaultHandler executing impl: " + this.getClass().getName());
            onFault(synCtx);
        } catch (SynapseException e) {
            Stack<FaultHandler> faultStack = synCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                faultStack.pop().handleFault(synCtx);
            }
        }
    }

    /**
     * Extract and set ERROR_MESSAGE and ERROR_DETAIL to the message context from the Exception
     * @param synCtx the message context
     * @param e the exception encountered
     */
    public void handleFault(SynapseMessageContext synCtx, Exception e)
    {
    	if (e != null && synCtx.getProperty(SynapseConstants.ERROR_CODE) == null) {
            synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.DEFAULT_ERROR);
            // use only the first line as the message for multiline exception messages (Axis2 has these)
            synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, e.getMessage().split("\n")[0]);
            synCtx.setProperty(SynapseConstants.ERROR_DETAIL, getStackTrace(e));
            synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, e);
        }

        if (log.isDebugEnabled()) {
            log.debug("ERROR_CODE : " + synCtx.getProperty(SynapseConstants.ERROR_CODE));
            log.debug("ERROR_MESSAGE : " + synCtx.getProperty(SynapseConstants.ERROR_MESSAGE));
            log.debug("ERROR_DETAIL : " + synCtx.getProperty(SynapseConstants.ERROR_DETAIL));
            log.debug("ERROR_EXCEPTION : " + synCtx.getProperty(SynapseConstants.ERROR_EXCEPTION));
        }

        synCtx.getServiceLog().warn("ERROR_CODE : " +
            synCtx.getProperty(SynapseConstants.ERROR_CODE) + " ERROR_MESSAGE : " +
            synCtx.getProperty(SynapseConstants.ERROR_MESSAGE));

        try {
            if (log.isDebugEnabled()) {
                log.debug("FaultHandler : " + this);
            }
            onFault(synCtx);
        } catch (SynapseException se) {
            Stack<FaultHandler> faultStack = synCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                faultStack.pop().handleFault(synCtx, se);
            }
        }
    }

    /**
     * This will be executed to handle any Exceptions occurred within the Synapse environment.
     * @param synCtx SynapseMessageContext of which the fault occured message comprises
     * @throws SynapseException in case there is a failure in the fault execution
     */
    public abstract void onFault(SynapseMessageContext synCtx);

    /**
     * Get the stack trace into a String
     * @param aThrowable
     * @return the stack trace as a string
     */
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
