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
import org.apache.axis2.engine.Phase;
import org.apache.axis2.phaseresolver.PhaseMetadata;
import org.apache.axis2.transport.TransportListener;

/**
 * Represents an incoming transport deployed in Axis2.
 */
public class TransportInDescription
	implements ParameterInclude
{
    private Flow faultFlow;
    private Flow inFlow;

    private Phase faultPhase;
    private Phase inPhase;

    protected String name;

    protected final ParameterInclude paramInclude;
    protected TransportListener receiver;

    public TransportInDescription(String name) {
        paramInclude = new ParameterIncludeMixin();
        this.name = name;
        inPhase = new Phase(PhaseMetadata.TRANSPORT_PHASE);
        faultPhase = new Phase(PhaseMetadata.TRANSPORT_PHASE);
    }

    /**
     * Method addParameter.
     *
     * @param param
     */
    public void addParameter(Parameter param) throws AxisFault {
        paramInclude.addParameter(param);
    }

    public void removeParameter(Parameter param) throws AxisFault {
        paramInclude.removeParameter(param);
    }

    public void deserializeParameters(OMElement parameterElement) throws AxisFault {
        this.paramInclude.deserializeParameters(parameterElement);
    }

    public Flow getFaultFlow() {
        return faultFlow;
    }

    public Phase getFaultPhase() {
        return faultPhase;
    }

    public Flow getInFlow() {
        return inFlow;
    }

    public Phase getInPhase() {
        return inPhase;
    }

    /**
     * Get Transport name as a QName
     * @return Returns QName.
     */
    public String getName() {
        return name;
    }

    /**
     * Method getParameter.
     *
     * @param name
     * @return Returns Parameter.
     */
    @Override
    public Parameter getParameter(String name) {
        return paramInclude.getParameter(name);
    }

    @Override
    public List<Parameter> getParameters() {
        return paramInclude.getParameters();
    }

    public TransportListener getReceiver() {
        return receiver;
    }

    @Override
    public boolean isParameterLocked(String parameterName) {
        return paramInclude.isParameterLocked(parameterName);
    }

    public void setFaultFlow(Flow faultFlow) {
        this.faultFlow = faultFlow;
    }

    public void setFaultPhase(Phase faultPhase) {
        this.faultPhase = faultPhase;
    }

    public void setInFlow(Flow inFlow) {
        this.inFlow = inFlow;
    }

    public void setInPhase(Phase inPhase) {
        this.inPhase = inPhase;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReceiver(TransportListener receiver) {
        this.receiver = receiver;
    }

	@Override
	public void addParameterObserver(ParameterObserver observer) {
		paramInclude.addParameterObserver(observer);
	}

	@Override
	public void removeParameterObserver(ParameterObserver observer) {
		paramInclude.removeParameterObserver(observer);
	}
}
