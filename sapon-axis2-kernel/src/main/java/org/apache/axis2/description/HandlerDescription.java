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
import org.apache.axis2.engine.Handler;

/**
 * Represents the deployment information about the handler
 */
public class HandlerDescription implements ParameterInclude {

	private final ParameterInclude parameterInclude;
	private ParameterInclude parent;

	private String name;
	private String className;
    private Handler handler;

    private PhaseRule rules;

    public HandlerDescription() {
    	this(null);
    }

    /**
     * Constructor HandlerDescription.
     *
     * @param name name of handler
     */
    public HandlerDescription(String name) {
        this.parameterInclude = new ParameterIncludeMixin();
        this.rules = new PhaseRule();
        this.name = name;
    }

    @Override
    public void addParameter(Parameter param) throws AxisFault {
    	parameterInclude.addParameter(param);
    }

    @Override
    public void removeParameter(Parameter param) throws AxisFault {
    	parameterInclude.removeParameter(param);
    }

    public void deserializeParameters(OMElement parameterElement)
    	throws AxisFault
    {
        parameterInclude.deserializeParameters(parameterElement);
    }

    /**
     * Method getClassName.
     *
     * @return Returns String.
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return Returns Handler.
     */
    public Handler getHandler() {
        return handler;
    }

    /**
     * @return Returns QName.
     */
    public String getName() {
        return name;
    }

    /**
     * Get a named Parameter
     *
     * @param name name of Parameter to search
     * @return a Parameter, which may come from us or from some parent up the tree, or null.
     */
    public Parameter getParameter(String name) {
        return parameterInclude.getParameter(name);
    }

    public List<Parameter> getParameters() {
        return parameterInclude.getParameters();
    }

    public ParameterInclude getParent() {
        return parent;
    }

    /**
     * Method getRules.
     *
     * @return Returns PhaseRule.
     */
    public PhaseRule getRules() {
        return rules;
    }

    // to check whether the parameter is locked at any level
    public boolean isParameterLocked(String parameterName) {
        return parameterInclude.isParameterLocked(parameterName);
    }

    /**
     * Method setClassName.  This should only be called if the Handler instance
     * is not yet available
     *
     * @param className the class name of the Handler class
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Explicitly set the Handler object
     *
     * @param handler a Handler instance, which will be deployed wherever this HandlerDescription is
     */
    public void setHandler(Handler handler) {
        this.handler = handler;
        this.className = handler.getClass().getName();
    }

    /**
     * Set the name
     *
     * @param name the desired name
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setParent(ParameterInclude parent) {
        this.parent = parent;
    }

    /**
     * Set the deployment rules for this HandlerDescription
     *
     * @param rules a PhaseRule object
     */
    public void setRules(PhaseRule rules) {
        this.rules = rules;
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
