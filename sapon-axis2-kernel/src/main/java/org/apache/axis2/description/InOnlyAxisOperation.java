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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.neethi.Policy;

public class InOnlyAxisOperation extends AxisOperation {

    private AxisMessage inFaultMessage;
//    private AxisMessage inMessage;
    private AxisMessage outFaultMessage;

    // this is just to store the chain , we don't use it
    private List<Phase> outPhase;

    public InOnlyAxisOperation() {
        super();
        //setup a temporary name
        QName tmpName = new QName(this.getClass().getName() + "_" + UUIDGenerator.getUUID());
        this.setName(tmpName);
        createMessage();
        setMessageExchangePattern(WSDL2Constants.MEP_URI_IN_ONLY);
    }

    public InOnlyAxisOperation(QName name) {
        super(name);
        createMessage();
        setMessageExchangePattern(WSDL2Constants.MEP_URI_IN_ONLY);
    }

    @Override
	public OperationClient createClient(ServiceContext sc, Options options) {
        throw new UnsupportedOperationException(
                Messages.getMessage("mepnotyetimplemented", mepURI));
    }

    @Override
	public void addMessage(AxisMessage message, String label) {
        if (WSDLConstants.MESSAGE_LABEL_IN_VALUE.equals(label)) {
            unconditionalAddMessage("inMessage", message);
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    @Override
	public void addMessageContext(MessageContext msgContext, OperationContext opContext)
            throws AxisFault {
        if (!opContext.isComplete()) {
            opContext.getMessageContexts().put(MESSAGE_LABEL_IN_VALUE, msgContext);
            opContext.setComplete(true);
        } else {
            throw new AxisFault(Messages.getMessage("mepcompleted"));
        }
    }

    @Override
	public void addFaultMessageContext(MessageContext msgContext, OperationContext opContext)
            throws AxisFault {
        Map<String, MessageContext> mep = opContext.getMessageContexts();
        MessageContext faultMessageCtxt = mep.get(MESSAGE_LABEL_FAULT_VALUE);
        if (faultMessageCtxt != null) {
            throw new AxisFault(Messages.getMessage("mepcompleted"));
        } else {
            mep.put(MESSAGE_LABEL_FAULT_VALUE, msgContext);
            opContext.setComplete(true);
            opContext.cleanup();
        }
    }

    private void createMessage() {
        AxisMessage inMessage = new AxisMessage();
        inMessage.setDirection(WSDLConstants.WSDL_MESSAGE_DIRECTION_IN);
        inMessage.setParent(this);

        inFaultMessage = new AxisMessage();
        inFaultMessage.setParent(this);

        outFaultMessage = new AxisMessage();
        outFaultMessage.setParent(this);

        outPhase = new ArrayList<Phase>();

        unconditionalAddMessage("inMessage", inMessage);
    }

    @Override
    public AxisMessage getMessage(String label) {
        if (WSDLConstants.MESSAGE_LABEL_IN_VALUE.equals(label)) {
            return unconditionalGetMessage("inMessage");
        } else {
            throw new UnsupportedOperationException(Messages.getMessage("invalidacess"));
        }
    }

    @Override
	public List<Phase> getPhasesInFaultFlow() {
        return inFaultMessage.getMessageFlow();
    }

    @Override
	public List<Phase> getPhasesOutFaultFlow() {
        return outFaultMessage.getMessageFlow();
    }

    @Override
	public List<Phase> getPhasesOutFlow() {
        return outPhase;
    }

    @Override
	public List<Phase> getRemainingPhasesInFlow() {
        return (unconditionalGetMessage("inMessage")).getMessageFlow();
    }

    @Override
    public void setPhasesInFaultFlow(List<Phase> list) {
        inFaultMessage.setMessageFlow(list);
    }

    @Override
    public void setPhasesOutFaultFlow(List<Phase> list) {
        outFaultMessage.setMessageFlow(list);
    }

    @Override
    public void setPhasesOutFlow(List<Phase> list) {
        outPhase = list;
    }

    @Override
    public void setRemainingPhasesInFlow(List<Phase> list) {
        unconditionalGetMessage("inMessage").setMessageFlow(list);
    }

	@Override
	public Policy getEffectivePolicy() {
		// TODO Auto-generated method stub
		return null;
	}
}
