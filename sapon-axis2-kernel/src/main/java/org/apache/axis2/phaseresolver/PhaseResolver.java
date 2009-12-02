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


package org.apache.axis2.phaseresolver;

import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.alt.Flows;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.Flow;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.wsdl.WSDLConstants;

public class PhaseResolver
{
	private AxisConfiguration axisConfig;
    private PhaseHolder phaseHolder;

    public PhaseResolver(AxisConfiguration axisconfig) {
        this.axisConfig = axisconfig;
    }

    private void engageModuleToFlow(Flow flow, List<Phase> handlerChain) throws PhaseException {
        phaseHolder = new PhaseHolder(handlerChain);
        if (flow != null) {
            for(HandlerDescription metadata: flow.handlers()) {
                phaseHolder.addHandler(metadata);
            }
        }
    }

    private void engageModuleToOperation(AxisOperation axisOperation,
                                         AxisModule axisModule,
                                         Flows flowType) throws PhaseException {
        List<Phase> phases = new ArrayList<Phase>();
        Flow flow = null;
        switch (flowType) {
            case IN : {
                phases.addAll(axisConfig.getInFlowPhases());
                phases.addAll(axisOperation.getRemainingPhasesInFlow());
                flow = axisModule.getInFlow();
                break;
            }
            case OUT: {
                phases.addAll(axisOperation.getPhasesOutFlow());
                phases.addAll(axisConfig.getOutFlowPhases());
                flow = axisModule.getOutFlow();
                break;
            }
            case OUT_FAULT: {
                phases.addAll(axisOperation.getPhasesOutFaultFlow());
                phases.addAll(axisConfig.getOutFaultFlowPhases());
                flow = axisModule.getFaultOutFlow();
                break;
            }
            case IN_FAULT: {
                phases.addAll(axisOperation.getPhasesInFaultFlow());
                phases.addAll(axisConfig.getInFaultFlowPhases());
                flow = axisModule.getFaultInFlow();
                break;
            }
        }
        engageModuleToFlow(flow, phases);
    }

    public void engageModuleToOperation(AxisOperation axisOperation, AxisModule module)
            throws PhaseException
    {
        for (Flows type: Flows.values()) {
            engageModuleToOperation(axisOperation, module, type);
        }
    }

    /**
     * Remove the handlers associated with the given module from the
     * AxisConfiguration's global handler chains.
     *
     * @param module
     */
    public void disengageModuleFromGlobalChains(AxisModule module)
    {
        Flow flow = module.getInFlow();
        if (flow != null) {
            for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, axisConfig.getInFlowPhases());
            }
        }

        flow = module.getOutFlow();
        if (flow != null) {
            for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, axisConfig.getOutFlowPhases());
            }
        }

        flow = module.getFaultInFlow();
        if (flow != null) {
            for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, axisConfig.getInFaultFlowPhases());
            }
        }

        flow = module.getFaultOutFlow();
        if (flow != null) {
        	for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, axisConfig.getOutFaultFlowPhases());
            }
        }
    }

    /**
     * To remove handlers from operations chians this method can be used , first it take inflow
     * of the module and then take handler one by one and then remove those handlers from
     * global inchain ,
     * the same procedure will be carry out for all the other flows as well.
     *
     * @param module
     */
    public void disengageModuleFromOperationChain(AxisModule module, AxisOperation operation) {

        Flow flow = module.getInFlow();
        if (flow != null) {
            for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, operation.getRemainingPhasesInFlow());
            }
        }

        flow = module.getOutFlow();
        if (flow != null) {
            for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, operation.getPhasesOutFlow());
            }
        }

        flow = module.getFaultInFlow();
        if (flow != null) {
            for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, operation.getPhasesInFaultFlow());
            }
        }

        flow = module.getFaultOutFlow();
        if (flow != null) {
            for(HandlerDescription handler: flow.handlers()) {
                removeHandlerFromPhases(handler, operation.getPhasesOutFaultFlow());
            }
        }
    }

    /**
     * To remove a single handler from a given list of phases
     *
     * @param handler
     * @param phaseList
     */
    private void removeHandlerFromPhases(HandlerDescription handler, List<Phase> phaseList) {
        String phaseName = handler.getRules().getPhaseName();
        for(Phase phase: phaseList) {
            if (phase.getPhaseName().equals(phaseName)) {
                phase.removeHandler(handler);
                break;
            }
        }
    }

    public void engageModuleToMessage(AxisMessage axisMessage, AxisModule axisModule)
            throws PhaseException
    {
        String direction = axisMessage.getDirection();
        AxisOperation axisOperation = axisMessage.getAxisOperation();
        if (WSDLConstants.MESSAGE_LABEL_OUT_VALUE.equalsIgnoreCase(direction)) {
            engageModuleToOperation(axisOperation, axisModule, Flows.OUT);
        } else if (WSDLConstants.MESSAGE_LABEL_IN_VALUE.equalsIgnoreCase(direction)) {
            engageModuleToOperation(axisOperation, axisModule, Flows.IN);
        } else if (WSDLConstants.MESSAGE_LABEL_FAULT_VALUE.equals(direction)) {
            //TODO : Need to handle fault correctly
        }
    }
}
