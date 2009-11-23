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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;
import org.apache.synapse.endpoints.Endpoint;

/**
 *
 */
public abstract class AbstractDispatcher implements Dispatcher {

    protected Log log;
    private final static String TRANSPORT_HEADERS = "TRANSPORT_HEADERS";

    protected AbstractDispatcher() {
        log = LogFactory.getLog(this.getClass());
    }

    public List<Endpoint> getEndpoints(SessionInformation sessionInformation) {
        return SALSessions.getInstance().getChildEndpoints(sessionInformation);
    }

    protected String extractSessionID(OMElement header, QName keyQName) {

        OMElement sgcIDElm = getHeaderBlock(header, keyQName);

        if (sgcIDElm != null) {
            String sgcID = sgcIDElm.getText();

            if (sgcID != null && !"".equals(sgcID)) {
                return sgcID.trim();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(keyQName + " is null or empty");
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Couldn't find the " + keyQName + " SOAP header to find the session");
            }
        }
        return null;
    }

    protected String extractSessionID(SynapseMessageContext synCtx, String key) {

        if (key != null) {
            Map headerMap = getTransportHeaderMap(synCtx);
            if (headerMap != null) {

                Object cookie = headerMap.get(key);

                if (cookie instanceof String) {
                    return (String) cookie;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Couldn't find the " + key + " header to find the session");
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Couldn't find the TRANSPORT_HEADERS to find the session");
                }

            }
        }
        return null;
    }

    protected void removeSessionID(SynapseMessageContext synCtx, String key) {

        if (key != null) {
            Map headerMap = getTransportHeaderMap(synCtx);
            if (headerMap != null) {
                headerMap.remove(key);
            }
        }
    }

    protected void removeSessionID(OMElement header, QName keyQName) {

        OMElement sgcIDElm = getHeaderBlock(header, keyQName);
        if (sgcIDElm != null) {
            sgcIDElm.detach();
        }
    }


    private Map getTransportHeaderMap(SynapseMessageContext synCtx) {

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2SynapseMessageContext) synCtx).getAxis2MessageContext();

        Object o = axis2MessageContext.getProperty(TRANSPORT_HEADERS);
        if (o != null && o instanceof Map) {
            return (Map) o;
        }
        return null;
    }

    private OMElement getHeaderBlock(OMElement soapHeader, QName keyQName) {

        if (soapHeader != null) {
            return soapHeader.getFirstChildWithName(keyQName);
        }
        return null;
    }
}
