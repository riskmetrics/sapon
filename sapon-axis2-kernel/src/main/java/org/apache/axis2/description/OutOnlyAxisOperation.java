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
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.AddressingConstants.Final;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.wsdl.WSDLConstants;

public class OutOnlyAxisOperation extends AxisOperation {

    private AxisMessage inFaultMessage;

    // just to keep the inflow , there won't be any usage
    private List<Phase> inPhases;

    private AxisMessage outFaultMessage;

    private AxisMessage outMessage;

    public OutOnlyAxisOperation() {
        super();
        //setup a temporary name
        QName tmpName = new QName(this.getClass().getName() + "_" + UUIDGenerator.getUUID());
        this.setName(tmpName);
        createMessage();
        setMessageExchangePattern(WSDL2Constants.MEP_URI_OUT_ONLY);
    }

    public OutOnlyAxisOperation(QName name) {
        super(name);
        createMessage();
        setMessageExchangePattern(WSDL2Constants.MEP_URI_OUT_ONLY);
    }

    @Override
	public void addMessage(AxisMessage message, String label) {
        if (WSDLConstants.MESSAGE_LABEL_OUT_VALUE.equals(label)) {
            outMessage = message;
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    @Override
	public void addMessageContext(MessageContext msgContext,
                                  OperationContext opContext) throws AxisFault {
        if (!opContext.isComplete()) {
            opContext.getMessageContexts().put(MESSAGE_LABEL_OUT_VALUE,
                                               msgContext);
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
        outMessage = new AxisMessage();
        outMessage.setDirection(WSDLConstants.WSDL_MESSAGE_DIRECTION_OUT);
        outMessage.setParent(this);
        inFaultMessage = new AxisMessage();
        inFaultMessage.setParent(this);
        outFaultMessage = new AxisMessage();
        outFaultMessage.setParent(this);
        inPhases = new ArrayList<Phase>();
    }

    @Override
	public AxisMessage getMessage(String label) {
        if (WSDLConstants.MESSAGE_LABEL_OUT_VALUE.equals(label)) {
            return outMessage;
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    @Override
	public List<Phase> getPhasesInFaultFlow() {
        return (List<Phase>)inFaultMessage.getMessageFlow();
    }

    @Override
	public List<Phase> getPhasesOutFaultFlow() {
        return (List<Phase>)outFaultMessage.getMessageFlow();
    }

    @Override
	public List<Phase> getPhasesOutFlow() {
        return (List<Phase>)outMessage.getMessageFlow();
    }

    @Override
	public List<Phase> getRemainingPhasesInFlow() {
        return inPhases;
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
        outMessage.setMessageFlow(list);
    }

    @Override
	public void setRemainingPhasesInFlow(List<Phase> list) {
        inPhases = list;
    }

    /**
     * Returns a MEP client for an Out-only operation. This client can be used to
     * interact with a server which is offering an In-only operation. To use the
     * client, you must call addMessageContext() with a message context and then
     * call execute() to execute the client. Note that the execute method's
     * block parameter is ignored by this client and also the setMessageReceiver
     * method cannot be used.
     *
     * @param sc      The service context for this client to live within. Cannot be
     *                null.
     * @param options Options to use as defaults for this client. If any options are
     *                set specifically on the client then those override options
     *                here.
     */
    @Override
	public OperationClient createClient(ServiceContext sc, Options options) {
        return new OutOnlyAxisOperationClient(this, sc, options);
    }
}

/**
 * MEP client for moi.
 */
class OutOnlyAxisOperationClient extends OperationClient {

    private MessageContext mc;

    OutOnlyAxisOperationClient(OutOnlyAxisOperation axisOp, ServiceContext sc,
                               Options options) {
        super(axisOp, sc, options);
    }

    /**
     * Adds a message context to the client for processing. This method must not
     * process the message - it only records it in the MEP client. Processing
     * only occurs when execute() is called.
     *
     * @param mc the message context
     * @throws AxisFault if this is called inappropriately.
     */
    @Override
	public void addMessageContext(MessageContext mc) throws AxisFault {
        if (this.mc != null) {
            throw new AxisFault(Messages.getMessage("cannotaddmsgctx"));
        }
        this.mc = mc;
        if (mc.getMessageID() == null) {
            setMessageID(mc);
        }
        mc.setServiceContext(sc);
        axisOp.registerOperationContext(mc, oc);
        this.completed = false;
    }

    /**
     * Returns a message from the client - will return null if the requested
     * message is not available.
     *
     * @param messageLabel the message label of the desired message context
     * @return Returns the desired message context or null if its not available.
     * @throws AxisFault if the message label is invalid
     */
    @Override
	public MessageContext getMessageContext(String messageLabel)
            throws AxisFault {
        if (messageLabel.equals(WSDLConstants.MESSAGE_LABEL_OUT_VALUE)) {
            return mc;
        }
        throw new AxisFault(Messages.getMessage("unknownMsgLabel", messageLabel));
    }

    /**
     * Executes the MEP. What this does depends on the specific MEP client. The
     * basic idea is to have the MEP client execute and do something with the
     * messages that have been added to it so far. For example, if its an Out-In
     * MEP, then if the Out message has been set, then executing the client asks
     * it to send the message and get the In message, possibly using a different
     * thread.
     *
     * @param block Indicates whether execution should block or return ASAP. What
     *              block means is of course a function of the specific MEP
     *              client. IGNORED BY THIS MEP CLIENT.
     * @throws AxisFault if something goes wrong during the execution of the MEP.
     */
    @Override
	public void executeImpl(boolean block) throws AxisFault {
        if (completed) {
            throw new AxisFault(Messages.getMessage("mepiscomplted"));
        }
        ConfigurationContext cc = sc.getConfigurationContext();
        prepareMessageContext(cc, mc);

        //As this is an out-only MEP we explicitly add a more sensible default for
        //the replyTo, if required. We also need to ensure that if the replyTo EPR
        //has an anonymous address and reference parameters that it gets flowed
        //across the wire.
        EndpointReference epr = mc.getReplyTo();
        if (epr == null) {
            mc.setReplyTo(new EndpointReference(Final.WSA_NONE_URI));
        }
        else if (epr.isWSAddressingAnonymous() && epr.getAllReferenceParameters() != null) {
            mc.setProperty(AddressingConstants.INCLUDE_OPTIONAL_HEADERS, Boolean.TRUE);
        }

        // create the operation context for myself
        OperationContext oc = sc.createOperationContext(axisOp);
        oc.addMessageContext(mc);

        // here we should not set it as NON bloking. if needed then user can do
        // it. if all the senders invoke in a different threads then it causes
        // a performance problem when using a MultiThreadedHttpConnectionManager

        AxisEngine.send(mc);
        // all done
        completed = true;
    }
}
