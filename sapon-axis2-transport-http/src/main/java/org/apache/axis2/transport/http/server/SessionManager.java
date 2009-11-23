/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.transport.http.server;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.engine.DependencyManager;

public class SessionManager {

    private final Map<String, SessionContext> sessionmap;

    public SessionManager() {
        super();
        this.sessionmap = new HashMap<String, SessionContext>();
    }

    public synchronized SessionContext getSessionContext(String sessionKey) {
        SessionContext sessionContext = null;
        if (sessionKey != null && sessionKey.length() != 0) {
            sessionContext = this.sessionmap.get(sessionKey);
        }
        if (sessionContext == null) {
            sessionKey = UUIDGenerator.getUUID();
            sessionContext = new SessionContext(null);
            sessionContext.setCookieID(sessionKey);
            this.sessionmap.put(sessionKey, sessionContext);
        }
        sessionContext.touch();
        cleanupServiceGroupContexts();
        return sessionContext;
    }

    private void cleanupServiceGroupContexts() {
    	final List<String> toRemove = new LinkedList<String>();
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, SessionContext> e: this.sessionmap.entrySet()) {
            SessionContext sessionContext = e.getValue();
            long elapsed = currentTime - sessionContext.getLastTouchedTime();
            if (elapsed > sessionContext.sessionContextTimeoutInterval) {
            	toRemove.add(e.getKey());
                Iterable<ServiceGroupContext> groupContexts
                	= sessionContext.getServiceGroupContext();
                if (groupContexts != null) {
                    for(ServiceGroupContext groupContext: groupContexts) {
                        cleanupServiceContexts(groupContext);
                    }
                }
            }
        }
        for(final String key: toRemove) {
        	this.sessionmap.remove(key);
        }
    }

    private void cleanupServiceContexts(final ServiceGroupContext serviceGroupContext) {
    	for(final ServiceContext serviceContext: serviceGroupContext.getServiceContexts()) {
            DependencyManager.destroyServiceObject(serviceContext);
        }
    }

}
