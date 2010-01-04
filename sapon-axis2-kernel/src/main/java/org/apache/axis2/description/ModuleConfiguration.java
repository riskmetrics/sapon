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

import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;

/**
 * This is to store deployment time data , described by
 * <moduleConfig module="modulename">
 * <parameter> ....</parameter>
 * </moduleConfig>
 * <p/>
 * Right now this just keeps stores the set of parameters
 */
public class ModuleConfiguration implements ParameterInclude {
    private String moduleName;
    private ParameterInclude parameterInclude;

    // to keep the pointer to its parent , only to access parameters
    private ParameterInclude parent;

    public ModuleConfiguration(String moduleName, ParameterInclude parent) {
        this.moduleName = moduleName;
        this.parent = parent;
        parameterInclude = new ParameterIncludeMixin();
    }

    public void addParameter(Parameter param) throws AxisFault {
    	parameterInclude.addParameter(param);
    }

    public void removeParameter(Parameter param) throws AxisFault {
    	parameterInclude.removeParameter(param);
    }

    public void deserializeParameters(OMElement parameterElement) throws AxisFault {
        this.parameterInclude.deserializeParameters(parameterElement);
    }

    public String getModuleName() {
        return moduleName;
    }

    public Parameter getParameter(String name) {
        return parameterInclude.getParameter(name);
    }

    public List<Parameter> getParameters() {
        return parameterInclude.getParameters();
    }

    public boolean isParameterLocked(String parameterName) {
    	return parameterInclude.isParameterLocked(parameterName);
    }

	@Override
	public void addParameterObserver(ParameterObserver observer) {
		parameterInclude.addParameterObserver(observer);
	}

	@Override
	public void removeParameterObserver(ParameterObserver observer) {
		parameterInclude.removeParameterObserver(observer);
	}
}
