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

package org.apache.synapse.mediators.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.config.xml.SwitchCase;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

/**
 * The switch mediator implements the functionality of the "switch" construct. It first
 * evaluates the given XPath expression into a String value, and performs a match against
 * the given list of cases. This is actually a list of sequences, and depending on the
 * selected case, the selected sequence gets executed.
 */
public class SwitchMediator extends AbstractMediator implements ManagedLifecycle {

    /** The XPath expression specifying the source element to apply the switch case expressions against   */
    private SynapseXPath source = null;
    /** The list of switch cases    */
    private final List<SwitchCase> cases = new ArrayList<SwitchCase>();
    /** The default switch case, if any */
    private SwitchCase defaultCase = null;

    private boolean initialized = false;

    public void init(SynapseEnvironment se) {
        for (ManagedLifecycle swCase : cases) {
            swCase.init(se);
        }
        if (defaultCase != null) {
            defaultCase.init(se);
        }
    }

    public boolean isInitialized() {
    	return initialized;
    }

    public void destroy() {
        for (ManagedLifecycle swCase : cases) {
            swCase.destroy();
        }
        if (defaultCase != null) {
            defaultCase.destroy();
        }
    }

    /**
     * Iterate over switch cases and find match and execute selected sequence
     *
     * @param synCtx current context
     * @return as per standard semantics
     */
    public boolean mediate(SynapseMessageContext synCtx) {



        if (log.isDebugEnabled()) {
            log.debug("Start : Switch mediator");

            if (log.isTraceEnabled()) {
                log.trace("Message : " + synCtx.getEnvelope());
            }
        }

        String sourceText = source.stringValueOf(synCtx);
        if (log.isDebugEnabled()) {
            log.debug("XPath : " + source + " evaluates to : " + sourceText);
        }

        if ((sourceText == null || cases.isEmpty()) && defaultCase != null) {
            log.debug("Source XPath evaluated to : null or no switch " +
                    "cases found. Executing the default case");

            return defaultCase.mediate(synCtx);

        } else {
            for (SwitchCase swCase : cases) {
                if (swCase != null) {
                    if (swCase.matches(sourceText)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Matching case found : " + swCase.getRegex());
                        }
                        return swCase.mediate(synCtx);
                    }
                }
            }

            if (defaultCase != null) {
                // if any of the switch cases did not match
                log.debug("None of the switch cases matched - executing default");
                return defaultCase.mediate(synCtx);
            } else {
                log.debug("None of the switch cases matched - no default case");
            }
        }

        log.debug("End : Switch mediator");
        return true;
    }

    /**
     * Adds the given mediator (Should be a SwitchCaseMediator) to the list of cases
     * of this Switch mediator
     *
     * @param m the SwitchCaseMediator instance to be added
     */
    public void addCase(SwitchCase m) {
        cases.add(m);
    }

    /**
     * Get the list of cases
     *
     * @return the cases list
     */
    public List<SwitchCase> getCases() {
        return cases;
    }

    /**
     * Return the source XPath expression set
     *
     * @return thje source XPath expression
     */
    public SynapseXPath getSource() {
        return source;
    }

    /**
     * Sets the source XPath expression
     *
     * @param source the XPath expression to be used as the source
     */
    public void setSource(SynapseXPath source) {
        this.source = source;
    }

    /**
     * Get default case
     *
     * @return the default csae
     */
    public SwitchCase getDefaultCase() {
        return defaultCase;
    }

    /**
     * setting the default case ...which contains mediators to invoke when no case condition satisfy
     * @param defaultCase A SwitchCase instance representing default case
     */
    public void setDefaultCase(SwitchCase defaultCase) {
        this.defaultCase = defaultCase;
    }
}
