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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.util.ChainIterator;
import org.apache.axis2.util.PolicyUtil;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyReference;

public class PolicySubjectMixin {

	private PolicySubject parent;
	private Collection<PolicySubject> children;

	private boolean updated = false;
	private Date lastUpdatedTime = new Date();

	private Policy effectivePolicy = null;
	private Date lastCalculatedTime = null;

	private Map<String, PolicyComponent> attachedPolicyComponents
		= new HashMap<String, PolicyComponent>();

	public void setParent(PolicySubject parent) {
		this.parent = parent;
	}

	public void attachPolicy(Policy policy) {
		String key = policy.getName();
		if (key == null) {
			key = policy.getId();
			if (key == null) {
				key = UUIDGenerator.getUUID();
				policy.setId(key);
			}
		}
		attachPolicyComponent(key, policy);
	}

	public void attachPolicyReference(PolicyReference reference) {
		attachedPolicyComponents.put(reference.getURI(), reference);
		setLastUpdatedTime(new Date());
	}

	public void attachPolicyComponents(Collection<PolicyComponent> policyComponents) {
		for (PolicyComponent component: policyComponents) {
			attachPolicyComponent(component);
		}
	}

	public void attachPolicyComponent(PolicyComponent policyComponent) {
		if (policyComponent instanceof Policy) {
			attachPolicy((Policy) policyComponent);
		} else if (policyComponent instanceof PolicyReference) {
			attachPolicyReference((PolicyReference) policyComponent);
		} else {
			throw new IllegalArgumentException(
					"Invalid top level policy component type");
		}
	}

	public void attachPolicyComponent(String key,
			PolicyComponent policyComponent) {
		attachedPolicyComponents.put(key, policyComponent);
		setLastUpdatedTime(new Date());
		setPolicyUpdated(true);
	}

	public PolicyComponent getAttachedPolicyComponent(String key) {
		return attachedPolicyComponents.get(key);
	}

	public Collection<PolicyComponent> getAttachedPolicyComponents() {
		return attachedPolicyComponents.values();
	}

	private void setPolicyUpdated(boolean updated) {
		this.updated = updated;
	}

	public void updatePolicy(Policy policy) {
		String key = (policy.getName() != null) ? policy.getName()
												: policy.getId();
		if (key == null) {
			throw new IllegalArgumentException(
					"policy doesn't have a name or an id ");
		}
		attachedPolicyComponents.put(key, policy);
		setLastUpdatedTime(new Date());
		setPolicyUpdated(true);
	}

	public PolicyComponent detachPolicyComponent(String key) {
		PolicyComponent out = attachedPolicyComponents.remove(key);
		setLastUpdatedTime(new Date());
		setPolicyUpdated(true);
		return out;
	}

	public void clearPolicyComponents() {
		attachedPolicyComponents.clear();
		setLastUpdatedTime(new Date());
		setPolicyUpdated(true);
	}

	public Date getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	private void setLastUpdatedTime(Date lastUpdatedTime) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public void applyPolicy(Policy policy) throws AxisFault {
		// TODO Auto-generated method stub

	}

	public void applyPolicy() throws AxisFault {
		// TODO Auto-generated method stub

	}

	public Iterator<PolicyComponent> getEffectivePolicyComponents() {
		if(parent == null) {
			return getAttachedPolicyComponents().iterator();
		} else {
			return new ChainIterator<PolicyComponent>(
    			getAttachedPolicyComponents().iterator(),
    			parent.getEffectivePolicyComponents());
		}
	}

	/**
	 * This method deviates from the PolicySubject interface by requiring an
	 * AxisService parameter.  This is done to satisfy
	 * PolicyUtil.getMergedPolicy(), which is used to calculate the effective
	 * policy.  Since this is a mixin class, this isn't seen as too onerous -
	 * just pass in the AxisService available to the class that's using the
	 * mixin - but it would still be nice to get rid of this requirement.
	 *
	 * @return
	 */
	public Policy getEffectivePolicy(AxisService axisService) {
		if (lastCalculatedTime == null || isPolicyUpdated()) {
			effectivePolicy = calculateEffectivePolicy(axisService);
		}
		return effectivePolicy;
	}

	private Policy calculateEffectivePolicy(AxisService axisService) {
		List<PolicyComponent> policyList = new ArrayList<PolicyComponent>();

		Iterator<PolicyComponent> policyIter = getEffectivePolicyComponents();
		while(policyIter.hasNext()) {
			PolicyComponent pc = policyIter.next();
			policyList.add(pc);
		}

		Policy result = PolicyUtil.getMergedPolicy(policyList, axisService);
		lastCalculatedTime = new Date();
		return result;
	}

	public boolean isPolicyUpdated() {
		//TODO:  this needs to be smart enough to return true whenever any
		//parent's last update should trigger a policy recalculation here,
		//while at the same time taking care to not trigger unecessary recalcs.
		return updated || (parent != null && parent.isPolicyUpdated());
	}
}
