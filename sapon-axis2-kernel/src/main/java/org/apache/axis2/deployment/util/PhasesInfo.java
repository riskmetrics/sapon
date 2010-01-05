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


package org.apache.axis2.deployment.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.axis2.deployment.DeploymentErrorMsgs;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.phaseresolver.PhaseException;
import org.apache.axis2.phaseresolver.PhaseMetadata;

public class PhasesInfo
{
	private final List<Phase> inPhases = new ArrayList<Phase>();
    private final List<Phase> inFaultPhases = new ArrayList<Phase>();
    private final List<Phase> outPhases = new ArrayList<Phase>();
    private final List<Phase> outFaultPhases= new ArrayList<Phase>();

    public List<Phase> getAllInPhases() {
        return new ArrayList<Phase>(inPhases);
    }

    public List<Phase> getAllInFaultPhases() {
        return new ArrayList<Phase>(inFaultPhases);
    }

    public List<Phase> getAllOutPhases() {
        return new ArrayList<Phase>(outPhases);
    }

    public List<Phase> getAllOutFaultPhases(){
        return new ArrayList<Phase>(outFaultPhases);
    }

    public List<Phase> getBefore(List<Phase> phases, String phaseName, boolean include)
    	throws DeploymentException
    {
    	List<Phase> beforePhases = new ArrayList<Phase>();
    	for(Phase phase: phases) {
    		try {
    			if(phaseName.equals(phase.getPhaseName())) {
    				if(include) {
    					beforePhases.add(phase.copy());
    				}
    				return beforePhases;
    			}
    			beforePhases.add(phase.copy());
    		} catch(PhaseException e) {
    			throw new DeploymentException(e);
    		}
    	}
    	return Collections.emptyList();
    }

    public List<Phase> getAfter(List<Phase> phases, String phaseName, boolean include)
    	throws DeploymentException
    {
    	List<Phase> afterPhases = new ArrayList<Phase>();
    	boolean foundPhase = false;
    	for(Phase phase: phases) {
    		try {
    			if(foundPhase) {
    				afterPhases.add(phase.copy());
    			}
    			else if(phaseName.equals(phase.getPhaseName())) {
    				if(include) {
    					afterPhases.add(phase.copy());
    				}
    				foundPhase = true;
    			}
    		} catch(PhaseException e) {
    			throw new DeploymentException(e);
    		}
    	}
    	return afterPhases;
    }


    /**
     * @return IN phases up to and including the Dispatch phase.
     * @throws DeploymentException if no dispatch phase is found.
     */
    public List<Phase> getGlobalInPhases()
    	throws DeploymentException
    {
        List<Phase> globalInPhases
        	= getBefore(inPhases, PhaseMetadata.PHASE_DISPATCH, true);

        if(globalInPhases.isEmpty()) {
        	throw new DeploymentException(
        		Messages.getMessage(DeploymentErrorMsgs.DISPATCH_PHASE_NOT_FOUND));
        }

        return globalInPhases;
    }

    /**
     * @return IN_FAULT phases up to and including the Dispatch phase.
     * @throws DeploymentException if no dispatch phase is found.
     */
    public List<Phase> getGlobalInFaultPhases()
    	throws DeploymentException
    {
        List<Phase> globalInFaultPhases
        	= getBefore(inFaultPhases, PhaseMetadata.PHASE_DISPATCH, true);

        if(globalInFaultPhases.isEmpty()) {
        	throw new DeploymentException(
        		Messages.getMessage(DeploymentErrorMsgs.DISPATCH_PHASE_NOT_FOUND));
        }

        return globalInFaultPhases;
    }

    /**
     * @return OUT phases including and after the MessageOut phase.
     * @throws DeploymentException
     */
    public List<Phase> getGlobalOutPhases()
    	throws DeploymentException
    {
    	return getAfter(outPhases, PhaseMetadata.PHASE_MESSAGE_OUT, true);
    }

    /**
     * @return OUT_FAULT phases including and after the MessageOut phase.
     * @throws DeploymentException
     */
    public List<Phase> getGlobalOutFaultPhases()
    	throws DeploymentException
    {
    	return getAfter(outFaultPhases, PhaseMetadata.PHASE_MESSAGE_OUT, true);
    }

    /**
     * @return IN phases after the Dispatch phase.
     */
    public List<Phase> getOperationInPhases()
    	throws DeploymentException
    {
    	return getAfter(inPhases, PhaseMetadata.PHASE_DISPATCH, false);
    }

    /**
     * @return IN_FAULT phases after the Dispatch phase.
     */
    public List<Phase> getOperationInFaultPhases()
    	throws DeploymentException
    {
    	return getAfter(inFaultPhases, PhaseMetadata.PHASE_DISPATCH, false);
    }

    /**
     * @return OUT phases before the MessageOut phase.
     * @throws DeploymentException
     */
    public List<Phase> getOperationOutPhases()
    	throws DeploymentException
    {
    	return getBefore(outPhases, PhaseMetadata.PHASE_MESSAGE_OUT, false);
    }

    /**
     * @return OUT_FAULT phases before the MessageOut phase.
     * @throws DeploymentException
     */
    public List<Phase> getOperationOutFaultPhases()
    	throws DeploymentException
    {
    	return getBefore(outFaultPhases, PhaseMetadata.PHASE_MESSAGE_OUT, false);
    }

    public void setInPhases(List<Phase> inPhases) {
        this.inPhases.clear();
        this.inPhases.addAll(inPhases);
    }

    public void setInFaultPhases(List<Phase> inFaultPhases) {
    	this.inFaultPhases.clear();
        this.inFaultPhases.addAll(inFaultPhases);
    }

    public void setOutPhases(List<Phase> OutPhases) {
    	this.outPhases.clear();
        this.outPhases.addAll(OutPhases);
    }

    public void setOutFaultPhases(List<Phase> outFaultPhases) {
        this.outFaultPhases.clear();
        this.outFaultPhases.addAll(outFaultPhases);
    }

//  private HandlerDescription makeHandler(OMElement handlerElement) {
//  String name = handlerElement.getAttributeValue(new QName("name"));
//  QName qname = handlerElement.resolveQName(name);
//  HandlerDescription desc = new HandlerDescription(qname.getLocalPart());
//  String className = handlerElement.getAttributeValue(new QName("class"));
//  desc.setClassName(className);
//  return desc;
//}

//public Phase makePhase(OMElement phaseElement) throws PhaseException {
//  String phaseName = phaseElement.getAttributeValue(new QName("name"));
//  Phase phase = new Phase(phaseName);
//
//  for(OMElement handlerElement: phaseElement.getChildElements()) {
//      HandlerDescription handlerDesc = makeHandler(handlerElement);
//      phase.addHandler(handlerDesc);
//  }
//
//  return phase;
//}

}
