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

import java.util.ArrayList;
import java.util.List;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.SynapseEnvironment;

/**
 * This is the base class for all List mediators
 *
 * @see ListMediator
 */
public abstract class AbstractListMediator
	extends AbstractMediator
	implements ListMediator
{
    /** the list of child mediators held. These are executed sequentially */
    protected final List<Mediator> mediators = new ArrayList<Mediator>();

    public boolean mediate(SynapseMessageContext synCtx) {
    	if (log.isDebugEnabled()) {
    		log.debug("Sequence <" + getType() + "> :: mediate()");
    	}

    	for (Mediator mediator : mediators) {
    		if (!mediator.mediate(synCtx)) {
    			return false;
    		}
    	}
        return true;
    }

    public List<Mediator> getList() {
        return mediators;
    }

    public boolean addChild(Mediator m) {
        return mediators.add(m);
    }

    public boolean addAll(List<Mediator> c) {
        return mediators.addAll(c);
    }

    public Mediator getChild(int pos) {
        return mediators.get(pos);
    }

    public boolean removeChild(Mediator m) {
        return mediators.remove(m);
    }

    public Mediator removeChild(int pos) {
        return mediators.remove(pos);
    }

    /**
     * Initialize child mediators recursively
     * @param se synapse environment
     */
    public void init(SynapseEnvironment se) {

        if (log.isDebugEnabled()) {
            log.debug("Initializing child mediators of mediator : " + getType());
        }

        for (Mediator mediator : mediators) {
            if (mediator instanceof ManagedLifecycle) {
                ((ManagedLifecycle) mediator).init(se);
            }
        }
    }

    /**
     * Destroy child mediators recursively
     */
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying child mediators of mediator : " + getType());
        }

        for (Mediator mediator : mediators) {

            if (mediator instanceof ManagedLifecycle) {
                ((ManagedLifecycle) mediator).destroy();
            }
        }
    }
}
