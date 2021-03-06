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

package org.apache.synapse.endpoints.dispatch;

import org.apache.synapse.SynapseMessageContext;


/**
 * Dispatches sessions based on HTTP cookies. Session is initiated by the server in the first
 * response when it sends "Set-Cookie" HTTP header with the session ID. For all successive messages
 * client should send "Cookie" HTTP header with session ID send by the server.
 */
public class HttpSessionDispatcher extends AbstractDispatcher {


    /*HTTP Headers  */
    private final static String COOKIE = "Cookie";
    private final static String SET_COOKIE = "Set-Cookie";

    /**
     * Check if "Cookie" HTTP header is available. If so, check if that cookie is in the session
     * map. If cookie is available, there is a session for this cookie. return the (server)
     * endpoint for that session.
     *
     * @param synCtx MessageContext possibly containing a "Cookie" HTTP header.
     * @return Endpoint Server endpoint for the given HTTP session.
     */
    public SessionInformation getSession(SynapseMessageContext synCtx) {
        return SALSessions.getInstance().getSession(extractSessionID(synCtx, COOKIE));
    }

    /**
     * Searches for "Set-Cookie" HTTP header in the message context. If found and that given
     * session ID is not already in the session map update the session map by mapping the cookie
     * to the endpoint.
     *
     * @param synCtx MessageContext possibly containing the "Set-Cookie" HTTP header.
     */
    public void updateSession(SynapseMessageContext synCtx) {

        String cookie = extractSessionID(synCtx, SET_COOKIE);

        if (cookie != null) {

            // extract the first name value pair of the Set-Cookie header, which is considered
            // as the session id which will be sent back from the client with the Cookie header
            // for example;
            //      Set-Cookie: JSESSIONID=760764CB72E96A7221506823748CF2AE; Path=/
            // will result in the session id "JSESSIONID=760764CB72E96A7221506823748CF2AE"
            // and the client is expected to send the Cookie header as;
            //      Cookie: JSESSIONID=760764CB72E96A7221506823748CF2AE
            if (log.isDebugEnabled()) {
                log.debug("Found the HTTP header 'Set-Cookie: "
                        + cookie + "' for updating the session");
            }

            String[] sessionIds = cookie.split(";");
            if (sessionIds == null || sessionIds.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot find a session id for the cookie : " + cookie);
                }
                return;
            }

            String sessionId = sessionIds[0];

            if (log.isDebugEnabled()) {
                log.debug("Using the session id '" + sessionId +
                        "' extracted from the Set-Cookie header ");
            }

            SALSessions.getInstance().updateSession(synCtx, sessionId);
        }

    }

    public void unbind(SynapseMessageContext synCtx) {
        SALSessions.getInstance().removeSession(extractSessionID(synCtx, COOKIE));
    }

    /**
     * HTTP sessions are initiated by the server.
     *
     * @return true
     */
    public boolean isServerInitiatedSession() {
        return true;
    }

    public void removeSessionID(SynapseMessageContext syCtx) {
        removeSessionID(syCtx, COOKIE);
    }
}
