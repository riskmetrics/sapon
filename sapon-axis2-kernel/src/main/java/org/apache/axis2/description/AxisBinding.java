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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.hierarchy.EndpointDescendant;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.neethi.Policy;

/**
 * An AxisBinding represents a WSDL binding, and contains AxisBindingOperations.
 */
public class AxisBinding extends AxisDescriptionBase
	implements EndpointDescendant
{
	private final Map<String, Object> options;
    private final Map<String, AxisBindingMessage> faults;
    private final Map<QName, AxisBindingOperation> children;

    private QName name;
    private String type;
    private AxisEndpoint parent;

    public AxisBinding() {
        options = new HashMap<String, Object>();
        faults = new HashMap<String, AxisBindingMessage>();
        children = new HashMap<QName, AxisBindingOperation>();
    }

    public void addBindingOperation(AxisBindingOperation op) {
    	children.put(op.getName(), op);
    }

    public void setProperty(String name, Object value) {
        options.put(name, value);
    }

    public void setParent(AxisEndpoint parentEndpoint) {
    	this.parent = parentEndpoint;
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

        // don't return a SOAP version for HTTP Bindings
        if (WSDL2Constants.ATTR_WSOAP_VERSION.equals(name) &&
                WSDL2Constants.URI_WSDL2_HTTP.equals(type)) {
            return null;
        }

        obj = WSDL20DefaultValueHolder.getDefaultValue(name);

        return obj;
    }

    public QName getName() {
        return name;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
	public void engageModule(AxisModule axisModule) throws AxisFault {
        throw new UnsupportedOperationException("Sorry we do not support this");
    }

    @Override
	public boolean isEngaged(String moduleName) {
        throw new UnsupportedOperationException("axisMessage.isEngaged() is not supported");

    }

	public Iterable<AxisBindingOperation> getChildren(){
    	return children.values();
    }

	public Iterable<? extends AxisDescription> getChildrenAsDescriptions(){
    	return getChildren();
    }

	public AxisBindingOperation getBindingOperation(QName opName) {
		return children.get(opName);
	}

    public AxisBindingMessage getFault(String name) {
        return faults.get(name);
    }

    public void addFault(AxisBindingMessage fault) {
        this.faults.put(fault.getName(), fault);
    }

	@Override
	public AxisConfiguration getConfiguration() {
		return parent.getConfiguration();
	}

	@Override
	public AxisEndpoint getEndpoint() {
		return parent;
	}

	@Override
	public AxisService getService() {
		return parent.getService();
	}

	@Override
	public AxisServiceGroup getServiceGroup() {
		return parent.getServiceGroup();
	}

	@Override
	public Policy getEffectivePolicy() {
		return getEffectivePolicy(getService());
	}
}
