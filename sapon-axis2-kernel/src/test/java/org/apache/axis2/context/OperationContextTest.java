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

package org.apache.axis2.context;

import javax.xml.namespace.QName;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AbstractTestCase;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;

public class OperationContextTest extends AbstractTestCase {

    private ConfigurationContext configContext = new ConfigurationContext(
            new AxisConfiguration());

    public OperationContextTest(String arg0) {
        super(arg0);
    }

    public void testMEPfindingOnRelatesTO() throws Exception {

        AxisService axisService = new AxisService("TempSC");
        configContext.getAxisConfiguration().addService(axisService);
        ServiceGroupContext sgc = configContext.createServiceGroupContext(
                axisService.getServiceGroup());
        ServiceContext sessionContext = sgc.getServiceContext(axisService);
        MessageContext messageContext1 = this.getBasicMessageContext();

        messageContext1.setMessageID(UUIDGenerator.getUUID());
        AxisOperation axisOperation = new InOutAxisOperation(new QName("test"));
        OperationContext operationContext1 = axisOperation
                .findOperationContext(messageContext1, sessionContext);
        axisOperation.registerOperationContext(messageContext1, operationContext1);

        MessageContext messageContext2 = this.getBasicMessageContext();
        messageContext2.setMessageID(UUIDGenerator.getUUID());
        messageContext2.getOptions().addRelatesTo(
                new RelatesTo(messageContext1.getMessageID()));
        messageContext2.setAxisOperation(axisOperation);
        OperationContext operationContext2 = axisOperation
                .findOperationContext(messageContext2, sessionContext);
        assertEquals(operationContext1, operationContext2);
    }

    public MessageContext getBasicMessageContext() throws AxisFault {
        MessageContext messageContext = configContext.createMessageContext();
        messageContext.setTransportIn(new TransportInDescription("axis2"));
        messageContext.setTransportOut(new TransportOutDescription("axis2"));

        return messageContext;

    }

}
