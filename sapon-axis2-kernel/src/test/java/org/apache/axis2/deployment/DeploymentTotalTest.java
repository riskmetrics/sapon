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

package org.apache.axis2.deployment;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.Flow;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.registry.Handler3;

public class DeploymentTotalTest extends TestCase {
    AxisConfiguration axisConfig;

    @Override
	protected void setUp() throws Exception {
        String filename = getClass().getResource("/deployment").getFile();
        axisConfig = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(filename)
                .getAxisConfiguration();
        axisConfig.deployModule(getClass().getResource("/deployment/module1").getFile());
        axisConfig.engageModule("module1");
        // OK, no exceptions.  Now make sure we read the correct file...
    }

    public void testparseService1() throws AxisFault, XMLStreamException {
        Parameter param = axisConfig.getParameter("FavoriteColor");
        assertNotNull("No FavoriteColor parameter in axis2.xml!", param);
        assertEquals("purple", param.getValue());
    }

    public void testDynamicPhase() {
        List<Phase> phases = axisConfig.getPhasesInfo().getINPhases();
        assertTrue("NewPhase wasn't found in InFlow", isPhaseInFlow(phases, "NewPhase"));
        Phase phase = phases.get(3);
        assertEquals("NewPhase not added at correct index!", "NewPhase", phase.getName());

        Flow inFlow = axisConfig.getModule("module1").getInFlow();
        int newPhaseHandlerCount = 0;
        Handler newPhaseFirst = null;
        for(int i = 0; i < inFlow.getHandlerCount(); i++) {
        	HandlerDescription hd = inFlow.getHandler(i);
        	if(hd.getRules().getPhaseName().equals("NewPhase")) {
        		newPhaseHandlerCount++;
        		if(newPhaseFirst == null) {
        			newPhaseFirst = hd.getHandler();
        		}
        	}
        }

        assertEquals("Wrong # of handlers in NewPhase", 3, newPhaseHandlerCount);
        assertTrue("Wrong type for handler", newPhaseFirst instanceof Handler3);

        phases = axisConfig.getPhasesInfo().getIN_FaultPhases();
        assertTrue("NewPhase wasn't found in InFaultFlow", isPhaseInFlow(phases, "NewPhase"));

        phases = axisConfig.getPhasesInfo().getOUTPhases();
        assertTrue("NewPhase wasn't found in OutFlow", isPhaseInFlow(phases, "NewPhase"));
    }

    private boolean isPhaseInFlow(List<Phase> inFlow, String phaseName) {
        boolean found = false;
        for (Object anInFlow : inFlow) {
            Phase phase = (Phase)anInFlow;
            if (phase.getName().equals(phaseName)) {
                found = true;
            }
        }
        return found;
    }

}
