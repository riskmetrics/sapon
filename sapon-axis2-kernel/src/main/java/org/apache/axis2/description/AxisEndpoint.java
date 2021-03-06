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

package org.apache.axis2.description;

import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.hierarchy.ServiceDescendant;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.util.Utils;
import org.apache.axis2.util.WSDLSerializationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;

public class AxisEndpoint extends AxisDescriptionBase
	implements ServiceDescendant
{

    private static final Log logger = LogFactory.getLog(AxisEndpoint.class);

    private AxisService parent;

    // The name of the endpoint
    private String name;

    // The binding reffered to by the endpoint
    private AxisBinding binding;

    // The address of the endpoint
    private String endpointURL;

    // The alias used for the endpoint
    private String alias;

    private Map<String, Object> options;

    private String transportInDescName;

    public String getEndpointURL() {
//        AxisService axisServce = (AxisService) this.getParent();
//        if ((axisServce!= null && axisServce.isModifyUserWSDLPortAddress()) || endpointURL == null) {
        if (endpointURL == null) {
            endpointURL = calculateEndpointURL();
        }
		return endpointURL;
    }

    public void setEndpointURL(String endpointURL) {
        this.endpointURL = endpointURL;
    }

    public AxisEndpoint() {
        options = new HashMap<String, Object>();
    }

    public void setProperty(String name, Object value) {
        options.put(name, value);
    }

    /**
     * @param name name of the property to search for
     * @return the value of the property, or null if the property is not found
     */
    public Object getProperty(String name) {
        Object obj = options.get(name);
        if (obj != null) {
            return obj;
        }

        return null;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AxisBinding getBinding() {
        return binding;
    }

    public void setBinding(AxisBinding binding) {
        this.binding = binding;
    }

    @Override
	public void engageModule(AxisModule axisModule) throws AxisFault {
        // TODO - We totally should support this.  Endpoint Policy Subject, anyone?
        throw new UnsupportedOperationException("Sorry we do not support this");
    }

    @Override
	public boolean isEngaged(String moduleName) {
        throw new UnsupportedOperationException("axisMessage.isEngaged() is not supported");
    }

    public OMElement toWSDL20(OMNamespace wsdl, OMNamespace tns, OMNamespace whttp) {
        String property;
        OMFactory omFactory = OMAbstractFactory.getOMFactory();
        OMElement endpointElement =
                omFactory.createOMElement(WSDL2Constants.ENDPOINT_LOCAL_NAME, wsdl);
        endpointElement.addAttribute(
                omFactory.createOMAttribute(WSDL2Constants.ATTRIBUTE_NAME, null, name));
        endpointElement.addAttribute(omFactory.createOMAttribute(WSDL2Constants.BINDING_LOCAL_NAME,
                                                                 null, tns.getPrefix() + ":" +
                                                                       getBinding().getName()
                                                                               .getLocalPart()));
        endpointElement.addAttribute(omFactory.createOMAttribute(WSDL2Constants.ATTRIBUTE_ADDRESS,
                                                                 null, getEndpointURL()));
        Object authenticationScheme =
                this.options.get(WSDL2Constants.ATTR_WHTTP_AUTHENTICATION_TYPE);
        if (authenticationScheme != null) {
            endpointElement.addAttribute(omFactory.createOMAttribute(
                    WSDL2Constants.ATTRIBUTE_AUTHENTICATION_TYPE, whttp,
                    authenticationScheme.toString()));
        }
        property = (String)options.get(WSDL2Constants.ATTR_WHTTP_AUTHENTICATION_REALM);
        if (property != null) {
            endpointElement.addAttribute(omFactory.createOMAttribute(
                    WSDL2Constants.ATTRIBUTE_AUTHENTICATION_REALM, whttp, property));
        }
        WSDLSerializationUtil.addWSDLDocumentationElement(this, endpointElement, omFactory, wsdl);
        WSDLSerializationUtil.addPoliciesAsExtensibleElement(this, endpointElement);
        return endpointElement;
    }

    public AxisService getAxisService() {
        return parent;
    }

    public void setParent(AxisService service) {
        parent = service;
    }

    public void setTransportInDescription(String transportInDescName) {
        this.transportInDescName = transportInDescName;
    }

    public String calculateEndpointURL() {
        return calculateEndpointURL(null);
    }

    public String calculateEndpointURL(String hostIP) {
        if (transportInDescName != null && parent != null) {
            AxisConfiguration axisConfiguration = getConfiguration();
            if (axisConfiguration != null) {
                try {
                    String serviceName = (parent).getName();
                    TransportInDescription in =
                            axisConfiguration.getTransportIn(transportInDescName);
                    TransportListener listener = in.getReceiver();
                    String ip;

                    if (hostIP != null) {
                        ip = hostIP;
                    } else {
                        ip = Utils.getIpAddress(axisConfiguration);
                    }

                    // we should pass [serviceName].[endpointName] instead of
                    // [endpointName]
                    String sDOTe = serviceName + "." + name;
                    EndpointReference[] eprsForService = listener.getEPRsForService(sDOTe, ip);
                    // we consider only the first address return by the listener
                    if (eprsForService != null && eprsForService.length > 0) {
                        return eprsForService[0].getAddress();
                    }
                } catch (SocketException e) {
                    logger.warn(e.getMessage(), e);
                } catch (AxisFault e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }

        return null;
    }

    public boolean isActive() {
        if (transportInDescName != null && parent != null) {
            AxisConfiguration axisConfiguration = getConfiguration();
            if (axisConfiguration != null) {
                AxisService service = parent;
                if (service.isEnableAllTransports()) {
                    return axisConfiguration.getTransportsIn().containsKey(transportInDescName);
                } else {
                    return service.getExposedTransports().contains(transportInDescName);
                }
            }
        }
        return false;
    }

	@Override
	public AxisConfiguration getConfiguration() {
		return parent.getConfiguration();
	}

	@Override
	public AxisService getService() {
		return parent;
	}

	@Override
	public AxisServiceGroup getServiceGroup() {
		return parent.getServiceGroup();
	}

	@Override
	public Policy getEffectivePolicy() {
		return getEffectivePolicy(getService());
	}

	@Override
	public Iterable<AxisDescription> getChildrenAsDescriptions() {
		return Collections.emptyList();
	}
}
