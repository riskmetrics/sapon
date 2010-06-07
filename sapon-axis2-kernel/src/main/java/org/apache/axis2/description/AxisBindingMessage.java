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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.hierarchy.BindingOperationDescendant;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.PolicyUtil;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;

public class AxisBindingMessage extends AxisDescriptionBase
	implements BindingOperationDescendant
{
	private String name;

	private String direction;

	private final Map<String, Object> options;

	private AxisMessage axisMessage;

	private AxisBindingOperation parent;

	// Used to indicate whether this message is a fault or not. Needed for the
	// WSDL 2.0 serializer
	private boolean fault = false;

	private Policy effectivePolicy = null;
	private Date lastPolicyCalcuatedTime = null;

	public void setParent(AxisBindingOperation parent) {
		this.parent = parent;
	}

	public boolean isFault() {
		return fault;
	}

	public void setFault(boolean fault) {
		this.fault = fault;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AxisMessage getAxisMessage() {
		return axisMessage;
	}

	public void setAxisMessage(AxisMessage axisMessage) {
		this.axisMessage = axisMessage;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public AxisBindingMessage() {
		options = new HashMap<String, Object>();
	}

	public void setProperty(String name, Object value) {
		options.put(name, value);
	}

	/**
	 * @param name
	 *            name of the property to search for
	 * @return the value of the property, or null if the property is not found
	 */
	public Object getProperty(String name) {
		Object obj = options.get(name);
		if (obj != null) {
			return obj;
		}

		return null;
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

	public AxisBindingOperation getAxisBindingOperation() {
		return parent;
	}

	public Policy getEffectivePolicy() {
	       if (lastPolicyCalcuatedTime == null || isPolicyUpdated()) {
			effectivePolicy = calculateEffectivePolicy();
		}
		return effectivePolicy;
	}

	public Policy calculateEffectivePolicy() {
		List<PolicyComponent> policyList = new ArrayList<PolicyComponent>();

		// AxisBindingMessage
		policyList.addAll(getAttachedPolicyComponents());

		// AxisBindingOperation policies
		AxisBindingOperation axisBindingOperation = getAxisBindingOperation();
		if (axisBindingOperation != null) {
			policyList.addAll(axisBindingOperation.getAttachedPolicyComponents());
		}

		// AxisBinding
		AxisBinding axisBinding = (axisBindingOperation == null) ? null
				: axisBindingOperation.getBinding();
		if (axisBinding != null) {
			policyList.addAll(axisBinding.getAttachedPolicyComponents());
		}

		// AxisEndpoint
		AxisEndpoint axisEndpoint = (axisBinding == null) ? null : axisBinding
				.getEndpoint();
		if (axisEndpoint != null) {
			policyList.addAll(axisEndpoint.getAttachedPolicyComponents());
		}

		// AxisMessage
		if (axisMessage != null) {
			policyList.addAll(axisMessage.getAttachedPolicyComponents());
		}

		// AxisOperation
		AxisOperation axisOperation = (axisMessage == null) ? null
				: axisMessage.getAxisOperation();
		if (axisOperation != null) {
			policyList.addAll(axisOperation.getAttachedPolicyComponents());
		}

		// AxisService
		AxisService axisService = (axisOperation == null) ? null
				: axisOperation.getService();
		if (axisService != null) {
			policyList.addAll(axisService.getAttachedPolicyComponents());
		}

		// AxisConfiguration
		AxisConfiguration axisConfiguration = (axisService == null) ? null
				: axisService.getConfiguration();
		if (axisConfiguration != null) {
			policyList.addAll(axisConfiguration.getAttachedPolicyComponents());
		}

		lastPolicyCalcuatedTime = new Date();
		return PolicyUtil.getMergedPolicy(policyList, axisService);
	}

//	@Override
//	private boolean isPolicyUpdated() {
//		if (getLastPolicyUpdateTime().after(lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		// AxisBindingOperation
//		AxisBindingOperation axisBindingOperation = getAxisBindingOperation();
//		if (axisBindingOperation != null
//				&& axisBindingOperation.getLastPolicyUpdateTime().after(lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		// AxisBinding
//		AxisBinding axisBinding = (axisBindingOperation == null) ? null
//				: axisBindingOperation.getBinding();
//		if (axisBinding != null
//				&& axisBinding.getLastPolicyUpdateTime().after(
//						lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		// AxisEndpoint
//		AxisEndpoint axisEndpoint = (axisBinding == null) ? null : axisBinding
//				.getEndpoint();
//		if (axisEndpoint != null
//				&& axisEndpoint.getLastPolicyUpdateTime().after(lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		// AxisMessage
//		if (axisMessage != null
//				&& axisMessage.getLastPolicyUpdateTime().after(lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		// AxisOperation
//		AxisOperation axisOperation = (axisMessage == null) ? null
//				: axisMessage.getAxisOperation();
//		if (axisOperation != null
//				&& axisOperation.getLastPolicyUpdateTime().after(lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		// AxisService
//		AxisService axisService = (axisOperation == null) ? null
//				: axisOperation.getService();
//		if (axisService != null
//				&& axisService.getLastPolicyUpdateTime().after(lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		// AxisConfiguration
//		AxisConfiguration axisConfiguration = (axisService == null) ? null
//				: axisService.getConfiguration();
//		if (axisConfiguration != null
//				&& axisConfiguration.getLastPolicyUpdateTime().after(lastPolicyCalcuatedTime)) {
//			return true;
//		}
//		return false;
//	}

	@Override
	public AxisBindingOperation getBindingOperation() {
		return parent;
	}

	@Override
	public AxisBinding getBinding() {
		return parent.getBinding();
	}

	@Override
	public AxisEndpoint getEndpoint() {
		// TODO Auto-generated method stub
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
	public AxisConfiguration getConfiguration() {
		return parent.getConfiguration();
	}

	@Override
	public Iterable<? extends AxisDescription> getChildrenAsDescriptions(){
    	return Collections.emptyList();
    }
}
