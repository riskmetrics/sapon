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

package org.apache.axis2.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.PolicyInclude;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;

public class WSDLBasedPolicyProcessor {
	private static final Log log
		= LogFactory.getLog(WSDLBasedPolicyProcessor.class);

	private Map<String, List<AxisModule>> ns2modules
    	= new HashMap<String, List<AxisModule>>();

    public WSDLBasedPolicyProcessor(ConfigurationContext configctx)
    {
    	AxisConfiguration axisConfiguration = configctx.getAxisConfiguration();
    	for (AxisModule axisModule : axisConfiguration.getModules().values()) {
    		String[] namespaces = axisModule.getSupportedPolicyNamespaces();
    		if (namespaces == null) {
    			continue;
    		}

    		for (String namespace : namespaces) {
    			List<AxisModule> moduleList = ns2modules.get(namespace);
    			if (moduleList == null) {
    				moduleList = new ArrayList<AxisModule>(5); //TODO: why 5?
    				ns2modules.put(namespace, moduleList);
    			}
    			moduleList.add(axisModule);
    		}
    	}
    }

    public void configureServicePolices(AxisService axisService)
    	throws AxisFault
    {
        for(final AxisOperation axisOp: axisService.getOperations()) {
            // TODO we support only operation level Policy now
            configureOperationPolices(axisOp);
        }
    }

    public void configureOperationPolices(AxisOperation op) throws AxisFault {
    	PolicyInclude policyInclude = op.getPolicyInclude();
    	Policy policy = policyInclude.getEffectivePolicy();
    	if (policy != null) {
    		policy = (Policy)policy.normalize(
    				policyInclude.getPolicyRegistry(),
    				false );

    		Set<String> namespaceSet = new HashSet<String>();
    		for (Iterator<List<PolicyComponent>> iter = policy.getAlternatives(); iter.hasNext();) {
    			List<PolicyComponent> assertionList = iter.next();
    			namespaceSet.clear();

    			//First we compute the set of distinct namespaces of assertions
    			//of this particular policy alternative.
    			for (PolicyComponent assertion: assertionList) {
    				if(assertion instanceof Assertion) {
    					QName name = ((Assertion)assertion).getName();
    					String namespaceURI = name.getNamespaceURI();
    					namespaceSet.add(namespaceURI);
    				}
    			}

    			//Compute all modules involved in process assertions that
    			//belong to any of the namespaces we found before.
    			for (String namespace: namespaceSet) {
    				List<AxisModule> modulesToEngage = ns2modules.get(namespace);
    				if (modulesToEngage == null) {
    					log.error(	"Cannot find any modules to process "
    								+ namespace + "type assertions");
    					continue;
    				} else {
    					engageModulesToAxisDescription(modulesToEngage, op);
    				}
    			}

    			//We only pick the first policy alternative. Other policy
    			//alternatives are ignored.  TODO:  maybe we should complain
    			//if there are other alternatives given?
    			break;
    		}
    	}
    }

    /**
     * Engages the list of Modules to the specified AxisDescription.
     */
    private void engageModulesToAxisDescription(List<AxisModule> modulesToEngage,
                                                AxisDescription axisDescription)
    	throws AxisFault
    {
        for (AxisModule axisModule: modulesToEngage) {
        	String moduleName = axisModule.getName();
            if (!axisDescription.isEngaged(moduleName)) {
                axisDescription.engageModule(axisModule);
                axisModule.getModule().engageNotify(axisDescription);
            }
        }
    }
}
