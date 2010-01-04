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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.hierarchy.BindingDescendant;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.neethi.Policy;

/**
 * An AxisBindingOperation represents a WSDL &lt;bindingOperation&gt;
 */
public class AxisBindingOperation extends AxisDescriptionBase
	implements BindingDescendant
{

	private AxisOperation axisOperation;

	private QName name;

	private AxisBinding parent;

	private final Map<String, AxisBindingMessage> faults;

	private final Map<String, Object> options;

	private final Map<String, AxisBindingMessage> children;

	public AxisBindingOperation() {
		options = new HashMap<String, Object>();
		faults = new HashMap<String, AxisBindingMessage>();
		children = new HashMap<String, AxisBindingMessage>();
	}

	public void setParent(AxisBinding parentBinding) {
		this.parent = parentBinding;
	}

	public List<AxisBindingMessage> getFaults() {
		return new ArrayList<AxisBindingMessage>(faults.values());
	}

	public AxisBindingMessage getFault(String name) {
		return faults.get(name);
	}

	public void addFault(AxisBindingMessage fault) {
		this.faults.put(fault.getName(), fault);
	}

	public QName getName() {
		return name;
	}

	public void setName(QName name) {
		this.name = name;
	}

	public AxisOperation getAxisOperation() {
		return axisOperation;
	}

	public void setAxisOperation(AxisOperation axisOperation) {
		this.axisOperation = axisOperation;
	}

	public void setProperty(String name, Object value) {
		options.put(name, value);
	}

	public Object getProperty(String name) {
		Object property = this.options.get(name);

		AxisBinding parent;
		if (property == null && (parent = getBinding()) != null) {
			property = parent.getProperty(name);
		}

		if (property == null) {
			property = WSDL20DefaultValueHolder.getDefaultValue(name);
		}

		return property;
	}

	@Override
	public void engageModule(AxisModule axisModule) throws AxisFault {
		throw new UnsupportedOperationException("Sorry we do not support this");
	}

	@Override
	public boolean isEngaged(String moduleName) {
		throw new UnsupportedOperationException(
				"axisMessage.isEngaged() is not supported");

	}

	public void addBindingMessage(String key, AxisBindingMessage message) {
		children.put(key, message);
	}

	public AxisBindingMessage getChild(String key) {
		return children.get(key);
	}

	public Iterable<AxisBindingMessage> getBindingMessages() {
		return children.values();
	}

	@Override
	public AxisConfiguration getConfiguration() {
		return parent.getConfiguration();
	}

	@Override
	public AxisBinding getBinding() {
		return parent;
	}

	@Override
	public AxisEndpoint getEndpoint() {
		return parent.getEndpoint();
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