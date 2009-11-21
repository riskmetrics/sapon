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

package org.apache.axis2.dispatchers;

import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisEndpoint;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.LoggingControl;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequestURIBasedServiceDispatcher extends AbstractServiceDispatcher {

    private static final Log log = LogFactory.getLog(RequestURIBasedServiceDispatcher.class);

    /*
     *  (non-Javadoc)
     * @see org.apache.axis2.engine.AbstractDispatcher#findService(org.apache.axis2.context.MessageContext)
     */
    @Override
	public AxisService findService(MessageContext messageContext) throws AxisFault {
        final EndpointReference toEPR = messageContext.getTo();
        if (toEPR == null) {
        	if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
        		log.debug(messageContext.getLogCorrelationID() +
        			" Attempted to check for Service using null target endpoint URI");
            }
            return null;
        }

        if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
        	log.debug(messageContext.getLogCorrelationID() +
        			" Checking for Service using target endpoint address : " +
        			toEPR.getAddress());
        }

        final String filePart = toEPR.getAddress();
        //REVIEW: (nagy) Parsing the RequestURI will also give us the operationName if present, so we could conceivably store it in the MessageContext, but doing so and retrieving it is probably no faster than simply reparsing the URI
        final ConfigurationContext configurationContext = messageContext.getConfigurationContext();

        final String[] values = Utils.parseRequestURLForServiceAndOperation(
        		filePart,
        		configurationContext.getServiceContextPath());
        if ((values.length < 1) || (values[0] == null)) {
        	if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
        		log.debug(messageContext.getLogCorrelationID() +
        				" Attempted to check for Service using target endpoint URI, but the service fragment was missing");
        	}
        	return null;
        }

        final AxisConfiguration registry
        	= configurationContext.getAxisConfiguration();

        final AxisService axisService = registry.getService(values[0]);

        if (axisService != null) {
        	final Map<String, AxisEndpoint> endpoints = axisService.getEndpoints();
        	if (endpoints != null) {
        		final String endpointName;
        		if (endpoints.size() == 1) {
        			endpointName = axisService.getEndpointName();
        		} else {
        			endpointName = values[0].substring(values[0].indexOf(".") + 1);
        		}
        		messageContext.setProperty(WSDL2Constants.ENDPOINT_LOCAL_NAME,
    					endpoints.get(endpointName));
        	}
        }

        return axisService;
    }

    @Override
	public void initDispatcher() {
        init(new HandlerDescription(getClass().getSimpleName()));
    }
}
