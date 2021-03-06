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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.alt.ModuleConfigAccessor;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.hierarchy.ServiceDescendant;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisError;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.phaseresolver.PhaseResolver;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;

public abstract class AxisOperation extends AxisDescriptionBase
	implements ServiceDescendant, WSDLConstants, ModuleConfigAccessor
{
	private static final Log log = LogFactory.getLog(AxisOperation.class);

    public static final String STYLE_RPC = "rpc";
    public static final String STYLE_MSG = "msg";
    public static final String STYLE_DOC = "doc";

    private int mep = WSDLConstants.MEP_CONSTANT_INVALID;

    // to hide control operation , operation which added by RM like module
    private boolean controlOperation = false;
    private String style = STYLE_DOC;

    // to store mepURL
    protected String mepURI;

    private MessageReceiver messageReceiver;

    private Map<String, ModuleConfiguration> moduleConfigmap;

    private final Map<String, AxisMessage> messages;

    // To store deploy-time module refs
    private List<String> modulerefs;

    private List<AxisMessage> faultMessages;

    private QName name;

    private AxisService parent;

    private List<String> wsamappingList;
    private String outputAction;
    private LinkedHashMap<String, String> faultActions = new LinkedHashMap<String, String>();

    private String soapAction;

    public AxisOperation() {
        mepURI = WSDL2Constants.MEP_URI_IN_OUT;
        modulerefs = new ArrayList<String>();
        moduleConfigmap = new HashMap<String, ModuleConfiguration>();
        messages = new HashMap<String, AxisMessage>();
        faultMessages = new ArrayList<AxisMessage>();
        //setup a temporary name
        QName tmpName = new QName(this.getClass().getName() + "_" + UUIDGenerator.getUUID());
        this.setName(tmpName);
    }

    public AxisOperation(QName name) {
        this();
        this.setName(name);
    }

    /**
     * Adds a message context into an operation context. Depending on MEPs,
     * this method has to be overridden. Depending on the MEP operation
     * description know how to fill the message context map in
     * operationContext. As an example, if the MEP is IN-OUT then depending on
     * messagable operation description should know how to keep them in correct
     * locations.
     *
     * @param msgContext <code>MessageContext</code>
     * @param opContext  <code>OperationContext</code>
     * @throws AxisFault <code>AxisFault</code>
     */
    public abstract void addMessageContext(MessageContext msgContext, OperationContext opContext)
            throws AxisFault;

    public abstract void addFaultMessageContext(MessageContext msgContext,
                                                OperationContext opContext)
            throws AxisFault;

    public void addModule(String moduleName) {
        modulerefs.add(moduleName);
    }

    public void setParent(AxisService parentService) {
    	this.parent = parentService;
    	this.policySubject.setParent(parentService);
    	this.parameterInclude.setParent(parentService);
    }

    /**
     * Adds module configuration, if there is moduleConfig tag in operation.
     *
     * @param moduleConfiguration a ModuleConfiguration which will be added (by name)
     */
    public void addModuleConfig(ModuleConfiguration moduleConfiguration) {
        moduleConfigmap.put(moduleConfiguration.getModuleName(), moduleConfiguration);
    }

    /**
     * This is called when a module is engaged on this operation.  Handle operation-specific tasks.
     *
     * @param axisModule AxisModule being engaged
     * @param engager    the AxisDescription where the engage occurred - could be us or a parent
     * @throws AxisFault
     */
    @Override
	public final void onEngage(AxisModule axisModule, AxisDescription engager) throws AxisFault {
        // Am I the source of this engagement?
        boolean selfEngaged = (engager == this);

        // If I'm not, the operations will already have been added by someone above, so don't
        // do it again.
        if (selfEngaged) {
            AxisService service = getService();
            if (service != null) {
                service.addModuleOperations(axisModule);
            }
        }
        AxisConfiguration axisConfig = getConfiguration();
        PhaseResolver phaseResolver = new PhaseResolver(axisConfig);
        phaseResolver.engageModuleToOperation(this, axisModule);
    }

    @Override
	protected void onDisengage(AxisModule module) {
        AxisService service = getService();
        if (service == null) {
			return;
		}

        AxisConfiguration axisConfiguration = getConfiguration();
        PhaseResolver phaseResolver = new PhaseResolver(axisConfiguration);
        if (!service.isEngaged(module.getName()) &&
            (axisConfiguration != null && !axisConfiguration.isEngaged(module.getName()))) {
            phaseResolver.disengageModuleFromGlobalChains(module);
        }
        phaseResolver.disengageModuleFromOperationChain(module, this);

        //removing operations added at the time of module engagemnt
        Map<QName, AxisOperation> moduleOperations = module.getOperations();
        if (moduleOperations != null) {
            Iterator<AxisOperation> moduleOperations_itr = moduleOperations.values().iterator();
            while (moduleOperations_itr.hasNext()) {
                AxisOperation operation = moduleOperations_itr.next();
                service.removeOperation(operation.getName());
            }
        }
    }

    /**
     * Returns as existing OperationContext related to this message if one exists.
     * <p/>
     * TODO - why both this and findOperationContext()? (GD)
     *
     * @param msgContext the MessageContext for which we'd like an OperationContext
     * @return the OperationContext, or null
     * @throws AxisFault
     */
    public OperationContext findForExistingOperationContext(MessageContext msgContext)
            throws AxisFault {
        OperationContext operationContext;

        if ((operationContext = msgContext.getOperationContext()) != null) {
            return operationContext;
        }

        // If this message is not related to another one, or it is but not one emitted
        // from the same operation, don't further look for an operation context or fault.
        if (null != msgContext.getRelatesTo()) {
            // So this message may be part of an ongoing MEP
            ConfigurationContext configContext = msgContext.getConfigurationContext();

            operationContext =
                    configContext.getOperationContext(msgContext.getRelatesTo().getValue());

            if (null == operationContext && log.isDebugEnabled()) {
                log.debug(msgContext.getLogCorrelationID() +
                          " Cannot correlate inbound message RelatesTo value [" +
                          msgContext.getRelatesTo() + "] to in-progree MEP");
            }
        }

        return operationContext;
    }

    public Iterable<AxisMessage> getMessages() {
    	return messages.values();
    }

    /**
     * Finds an OperationContext for an incoming message. An incoming message can be of two states.
     * <p/>
     * 1)This is a new incoming message of a given MEP. 2)This message is a part of an MEP which has
     * already begun.
     * <p/>
     * The method is special cased for the two MEPs
     * <p/>
     * #IN_ONLY #IN_OUT
     * <p/>
     * for two reasons. First reason is the wide usage and the second being that the need for the
     * MEPContext to be saved for further incoming messages.
     * <p/>
     * In the event that MEP of this operation is different from the two MEPs defaulted above the
     * decision of creating a new or this message relates to a MEP which already in business is
     * decided by looking at the WSA Relates TO of the incoming message.
     *
     * @param msgContext     MessageContext to search
     * @param serviceContext ServiceContext (TODO - why pass this? (GD))
     * @return the active OperationContext
     */
    public OperationContext findOperationContext(MessageContext msgContext,
                                                 ServiceContext serviceContext)
            throws AxisFault {
        OperationContext operationContext;

        if (null == msgContext.getRelatesTo()) {

            // Its a new incoming message so get the factory to create a new
            // one
            operationContext = serviceContext.createOperationContext(this);
        } else {

            // So this message is part of an ongoing MEP
            ConfigurationContext configContext = msgContext.getConfigurationContext();

            operationContext =
                    configContext.getOperationContext(msgContext.getRelatesTo().getValue());

            if (null == operationContext) {
                throw new AxisFault(Messages.getMessage("cannotCorrelateMsg",
                                                        this.name.toString(),
                                                        msgContext.getRelatesTo().getValue()));
            }
        }
        return operationContext;
    }

    public void registerOperationContext(MessageContext msgContext,
                                         OperationContext operationContext)
            throws AxisFault {
        msgContext.setAxisOperation(this);
        msgContext.getConfigurationContext().registerOperationContext(msgContext.getMessageID(),
                                                                      operationContext);
        operationContext.addMessageContext(msgContext);
        msgContext.setOperationContext(operationContext);
        if (operationContext.isComplete()) {
            operationContext.cleanup();
        }
    }

    public void registerMessageContext(MessageContext msgContext,
                                       OperationContext operationContext) throws AxisFault {
        msgContext.setAxisOperation(this);
        operationContext.addMessageContext(msgContext);
        msgContext.setOperationContext(operationContext);
        if (operationContext.isComplete()) {
            operationContext.cleanup();
        }
    }

    /**
     * Maps the String URI of the Message exchange pattern to an integer. Further, in the first
     * lookup, it will cache the looked up value so that the subsequent method calls are extremely
     * efficient.
     *
     * @return an MEP constant from WSDLConstants
     */
    public int getAxisSpecificMEPConstant() {
        if (this.mep != WSDLConstants.MEP_CONSTANT_INVALID) {
            return this.mep;
        }

        int temp = WSDLConstants.MEP_CONSTANT_INVALID;

        if (WSDL2Constants.MEP_URI_IN_OUT.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_IN_OUT;
        } else if (WSDL2Constants.MEP_URI_IN_ONLY.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_IN_ONLY;
        } else if (WSDL2Constants.MEP_URI_IN_OPTIONAL_OUT.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_IN_OPTIONAL_OUT;
        } else if (WSDL2Constants.MEP_URI_OUT_IN.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_OUT_IN;
        } else if (WSDL2Constants.MEP_URI_OUT_ONLY.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_OUT_ONLY;
        } else if (WSDL2Constants.MEP_URI_OUT_OPTIONAL_IN.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_OUT_OPTIONAL_IN;
        } else if (WSDL2Constants.MEP_URI_ROBUST_IN_ONLY.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_ROBUST_IN_ONLY;
        } else if (WSDL2Constants.MEP_URI_ROBUST_OUT_ONLY.equals(mepURI)) {
            temp = WSDLConstants.MEP_CONSTANT_ROBUST_OUT_ONLY;
        }

        if (temp == WSDLConstants.MEP_CONSTANT_INVALID) {
            throw new AxisError(Messages.getMessage("mepmappingerror"));
        }

        this.mep = temp;

        return this.mep;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.axis2.description.AxisService#getEngadgedModules()
     */

    public abstract AxisMessage getMessage(String label);
    public abstract void addMessage(AxisMessage message, String label);

    protected AxisMessage unconditionalGetMessage(String label) {
    	return messages.get(label);
    }
    protected void unconditionalAddMessage(String label, AxisMessage message) {
    	messages.put(label, message);
    }

    public String getMessageExchangePattern() {
        return mepURI;
    }

    public MessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    public ModuleConfiguration getModuleConfig(String moduleName) {
        return moduleConfigmap.get(moduleName);
    }

    public List<String> getModuleRefs() {
        return modulerefs;
    }

    public QName getName() {
        return name;
    }

    public abstract List<Phase> getPhasesInFaultFlow();

    public abstract List<Phase> getPhasesOutFaultFlow();

    public abstract List<Phase> getPhasesOutFlow();

    public abstract List<Phase> getRemainingPhasesInFlow();

    public String getStyle() {
        return style;
    }

    public List<String> getWSAMappingList() {
        return wsamappingList;
    }

    public boolean isControlOperation() {
        return controlOperation;
    }

    public void setControlOperation(boolean controlOperation) {
        this.controlOperation = controlOperation;
    }

    public void setMessageExchangePattern(String mepURI) {
        this.mepURI = mepURI;
    }

    public void setMessageReceiver(MessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public abstract void setPhasesInFaultFlow(List<Phase> list);

    public abstract void setPhasesOutFaultFlow(List<Phase> list);

    public abstract void setPhasesOutFlow(List<Phase> list);

    public abstract void setRemainingPhasesInFlow(List<Phase> list);

    public void setPhases(PhasesInfo phasesInfo) throws AxisFault {
    	try {
    		setRemainingPhasesInFlow(phasesInfo.getOperationInPhases());
    		setPhasesOutFlow(phasesInfo.getOperationOutPhases());
    		setPhasesInFaultFlow(phasesInfo.getOperationInFaultPhases());
    		setPhasesOutFaultFlow(phasesInfo.getOperationOutFaultPhases());
    	} catch (DeploymentException e) {
    		throw AxisFault.makeFault(e);
    	}
    }

    public void setStyle(String style) {
        if (!"".equals(style)) {
            this.style = style;
        }
    }

    public void setWsamappingList(List<String> wsamappingList) {
        this.wsamappingList = wsamappingList;
    }

    /**
     * Return an OperationClient suitable for this AxisOperation.
     *
     * @param sc      active ServiceContext
     * @param options active Options
     * @return an OperationClient set up appropriately for this operation
     */
    public abstract OperationClient createClient(ServiceContext sc, Options options);

    public List<AxisMessage> getFaultMessages() {
        return faultMessages;
    }

    public void setFaultMessages(AxisMessage faultMessage) {
        faultMessage.setParent(this);
        faultMessages.add(faultMessage);
        if (getFaultAction(faultMessage.getName()) == null) {
            addFaultAction(faultMessage.getName(),
                           "urn:" + name.getLocalPart() + faultMessage.getName());
        }
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    /*
    * Convenience method to access the WS-A Input Action per the
    * WS-A spec. Effectively use the soapAction if available else
    * use the first entry in the WSA Mapping list.
    *
    * Use getSoapAction when you want to get the soap action and this
    * when you want to get the wsa input action.
    */
    public String getInputAction() {
        String result = null;
        if (soapAction != null && !"".equals(soapAction)) {
            result = soapAction;
        } else {
            if (wsamappingList != null && !wsamappingList.isEmpty()) {
                result = wsamappingList.get(0);
            }
        }
        return result;
    }

    public String getOutputAction() {
        return outputAction;
    }

    public void setOutputAction(String act) {
        outputAction = act;
    }

    public void addFaultAction(String faultName, String action) {
        faultActions.put(faultName, action);
    }

    public void removeFaultAction(String faultName) {
        faultActions.remove(faultName);
    }

    public String getFaultAction(String faultName) {
        return faultActions.get(faultName);
    }

    public String[] getFaultActionNames() {
        Set<String> keys = faultActions.keySet();
        String[] faultActionNames = new String[keys.size()];
        faultActionNames = keys.toArray(faultActionNames);
        return faultActionNames;
    }

    public String getFaultAction() {
        String result = null;
        Iterator<String> iter = faultActions.values().iterator();
        if (iter.hasNext()) {
            result = iter.next();
        }
        return result;
    }

    /**
     * Typesafe access to parent service
     *
     * @return the AxisService which contains this AxisOperation
     */
    @Override
    public AxisService getService() {
        return parent;
    }

    @Override
    public AxisServiceGroup getServiceGroup() {
    	return parent.getServiceGroup();
    }

    public String getSoapAction() {
        /*
         * This AxisOperation instance may be used for the client OUT-IN or for
         * the server IN-OUT.  If the below code were changed to getInputActions, and the
         * result of getInputAction were put in the SOAP action header on a client outbound
         * message, the server would receive an INCORRECT SOAP action header.  We should leave
         * this as 'return soapAction;' OR make it client/server aware.
         */
        return soapAction;
    }

	@Override
	public AxisConfiguration getConfiguration() {
		return parent.getConfiguration();
	}

	@Override
	public Policy getEffectivePolicy() {
		return getEffectivePolicy(getService());
	}

	public Iterable<? extends AxisDescription> getChildrenAsDescriptions() {
		return getMessages();
	}
}
