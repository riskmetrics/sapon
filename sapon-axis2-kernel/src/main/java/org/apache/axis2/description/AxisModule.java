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

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyReference;

/**
 * <p>This holds the information about a Module. </p>
 * <ol>
 * <li>parameters<li>
 * <li>handlers<li>
 * <ol>
 * <p>Handler are registered once they are available. They are available to all services if axis2.xml
 * has a module ref="." or available to a single service if services.xml have module ref=".."</p>
 */
public class AxisModule
	implements ParameterInclude, PolicySubject
{
    private final FlowInclude flowInclude = new FlowInclude();

    private ParameterIncludeMixin parameters = new ParameterIncludeMixin();
    private Module module;
    private ClassLoader moduleClassLoader;
    // To keep the File that module came from
    private URL fileName;

    private String name;

    //This is to keep the version number of the module, if the module name is a-b-c-1.3.mar ,
    // then the module version would be 1.3
    private String version;

    // to store module operations , which are suppose to be added to a service if it is engaged to a service
    private Map<QName, AxisOperation> operations = new HashMap<QName, AxisOperation>();
    private AxisConfiguration parent;

    // Small description about the module
    private String moduleDescription;

    private String[] supportedPolicyNames;

    private QName[] localPolicyAssertions;
    public static final String VERSION_SNAPSHOT = "SNAPSHOT";
    public static final String MODULE_SERVICE = "moduleService";

    private PolicySubjectMixin policySubject = new PolicySubjectMixin();

    public AxisModule() {
    	this(null);
    }

    /**
     * Constructor ModuleDescription.
     *
     * @param name : Name of the module
     */
    public AxisModule(String name) {
        this.name = name;
    }

    public void addOperation(AxisOperation axisOperation) {
        operations.put(axisOperation.getName(), axisOperation);
    }

    @Override
    public void addParameter(Parameter param) throws AxisFault {
    	parameters.addParameter(param);
    }

    @Override
    public void removeParameter(Parameter param) throws AxisFault {
    	parameters.removeParameter(param);
    }

    public void deserializeParameters(OMElement parameterElement) throws AxisFault {
        this.parameters.deserializeParameters(parameterElement);
    }

    public Flow getFaultInFlow() {
        return flowInclude.getFaultInFlow();
    }

    public Flow getFaultOutFlow() {
        return flowInclude.getFaultOutFlow();
    }

    public Flow getInFlow() {
        return flowInclude.getInFlow();
    }

    public Module getModule() {
        return module;
    }

    public ClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }

    /**
     * Get the name of this Module
     * @return a String name.
     */
    public String getName() {
        return name;
    }

    public Map<QName, AxisOperation> getOperations() {
        return operations;
    }

    public Flow getOutFlow() {
        return flowInclude.getOutFlow();
    }

    public Parameter getParameter(String name) {
        return parameters.getParameter(name);
    }

    public List<Parameter> getParameters() {
        return parameters.getParameters();
    }

    public AxisConfiguration getParent() {
        return parent;
    }

    // to check whether a given parameter is locked
    public boolean isParameterLocked(String parameterName) {
    	return parameters.isParameterLocked(parameterName);
    }

    public void setFaultInFlow(Flow faultFlow) {
        flowInclude.setFaultInFlow(faultFlow);
    }

    public void setFaultOutFlow(Flow faultFlow) {
        flowInclude.setFaultOutFlow(faultFlow);
    }

    public void setInFlow(Flow inFlow) {
        flowInclude.setInFlow(inFlow);
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public void setModuleClassLoader(ClassLoader moduleClassLoader) {
        this.moduleClassLoader = moduleClassLoader;
    }

    /**
     * @param name  : Setting name of the module
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setOutFlow(Flow outFlow) {
        flowInclude.setOutFlow(outFlow);
    }

    public void setParent(AxisConfiguration parent) {
        this.parent = parent;
        this.policySubject.setParent(parent);
        this.parameters.setParent(parent);
    }

    public String getModuleDescription() {
        return moduleDescription;
    }

    public void setModuleDescription(String moduleDescription) {
        this.moduleDescription = moduleDescription;
    }

    public String[] getSupportedPolicyNamespaces() {
        return supportedPolicyNames;
    }

    public void setSupportedPolicyNamespaces(String[] supportedPolicyNamespaces) {
        this.supportedPolicyNames = supportedPolicyNamespaces;
    }

    public QName[] getLocalPolicyAssertions() {
        return localPolicyAssertions;
    }

    public void setLocalPolicyAssertions(QName[] localPolicyAssertions) {
        this.localPolicyAssertions = localPolicyAssertions;
    }

    public URL getFileName() {
        return fileName;
    }

    public void setFileName(URL fileName) {
        this.fileName = fileName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

	@Override
	public void addParameterObserver(ParameterObserver observer) {
		parameters.addParameterObserver(observer);
	}

	@Override
	public void removeParameterObserver(ParameterObserver observer) {
		parameters.removeParameterObserver(observer);
	}

	@Override
	public void applyPolicy(Policy policy) throws AxisFault {
		policySubject.applyPolicy(policy);
	}

	@Override
	public void applyPolicy() throws AxisFault {
		policySubject.applyPolicy();
	}

	@Override
	public void attachPolicy(Policy policy) {
		policySubject.attachPolicy(policy);
	}

	@Override
	public void attachPolicyComponent(PolicyComponent policyComponent) {
		policySubject.attachPolicyComponent(policyComponent);
	}

	@Override
	public void attachPolicyComponent(String key, PolicyComponent policyComponent) {
		policySubject.attachPolicyComponent(key, policyComponent);
	}

	@Override
	public void attachPolicyComponents(Collection<PolicyComponent> policyComponents) {
		policySubject.attachPolicyComponents(policyComponents);
	}

	@Override
	public void attachPolicyReference(PolicyReference reference) {
		policySubject.attachPolicyReference(reference);
	}

	@Override
	public void clearPolicyComponents() {
		policySubject.clearPolicyComponents();
	}

	@Override
	public PolicyComponent detachPolicyComponent(String key) {
		return policySubject.detachPolicyComponent(key);
	}

	@Override
	public PolicyComponent getAttachedPolicyComponent(String key) {
		return policySubject.getAttachedPolicyComponent(key);
	}

	@Override
	public Collection<PolicyComponent> getAttachedPolicyComponents() {
		return policySubject.getAttachedPolicyComponents();
	}

	@Override
	public Policy getEffectivePolicy() {
		return policySubject.getEffectivePolicy(null);
	}

	@Override
	public Iterator<PolicyComponent> getEffectivePolicyComponents() {
		return policySubject.getEffectivePolicyComponents();
	}

	@Override
	public Date getLastPolicyUpdateTime() {
		return policySubject.getLastUpdatedTime();
	}

	@Override
	public boolean isPolicyUpdated() {
		return policySubject.isPolicyUpdated();
	}

	@Override
	public void updatePolicy(Policy policy) {
		policySubject.updatePolicy(policy);
	}
}
