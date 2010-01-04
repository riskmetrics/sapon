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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyReference;

public interface PolicySubject {

	//TODO: what's the difference between applyPolicy and attachPolicy?
	void applyPolicy(Policy policy) throws AxisFault;
	void applyPolicy() throws AxisFault;

	void attachPolicy(Policy policy);
	void attachPolicyReference(PolicyReference reference);

	void attachPolicyComponent(PolicyComponent policyComponent);
	void attachPolicyComponent(String key, PolicyComponent policyComponent);
	PolicyComponent getAttachedPolicyComponent(String key);
	PolicyComponent detachPolicyComponent(String key);

	void attachPolicyComponents(Collection<PolicyComponent> policyComponents);
	Collection<PolicyComponent> getAttachedPolicyComponents();
	void clearPolicyComponents();

	Iterator<PolicyComponent> getEffectivePolicyComponents();
	Policy getEffectivePolicy();

	boolean isPolicyUpdated();
	void updatePolicy(Policy policy);
	Date getLastPolicyUpdateTime();

}
