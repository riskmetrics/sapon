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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.util.DetachableInputStream;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.RolePlayer;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.alt.DefaultExecutionTracker;
import org.apache.axis2.alt.ExecutionTracker;
import org.apache.axis2.alt.Flows;
import org.apache.axis2.alt.MessageContextFlags;
import org.apache.axis2.alt.MessageContextFlagsEnumSet;
import org.apache.axis2.alt.ModuleConfigAccessor;
import org.apache.axis2.client.Options;
import org.apache.axis2.description.AxisBinding;
import org.apache.axis2.description.AxisBindingMessage;
import org.apache.axis2.description.AxisBindingOperation;
import org.apache.axis2.description.AxisEndpoint;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisError;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.LoggingControl;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.wsdl.WSDLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;

/**
 *  <p>Axis2 states are held in two information models, the description
 *  hierarchy and the context hierarchy. The description hierarchy holds
 *  deployment configuration and its values do not change unless the deployment
 *  configuration changes.  The context hierarchy holds runtime
 *  information.  Both hierarchies consist of four levels: Global, Service
 *  Group, Operation, and Message. Please look at the "Information Model"
 *  section of the "Axis2 Architecture Guide" for more information.</p>
 *  <p>MessageContext holds run time information for one Message invocation. It
 *  holds reference to the OperationContext, ServiceGroupContext, and
 *  Configuration Context tied to the current message. For example, if you need
 *  access to other messages of the current invocation, you can get to them via
 *  OperationContext.  MessageContext defines class attributes and stores
 *  information as name/value pairs.  Those class attributes and pairs tweak
 *  the execution behavior of MessageContext and some of them can be find in
 *  org.apache.axis2.Constants class. (TODO we should provide list of supported
 *  options). You may set them at any level of the context hierarchy and they
 *  will affect invocations related to their child elements. </p>
 */
public class OldMessageContext extends AbstractContext<OperationContext>
	implements MessageContext
{
    private static final Log log = LogFactory.getLog(MessageContext.class);

    static final long serialVersionUID = -7753637088257391858L;

    /**
     * @serial Tracks the revision level of a class to identify changes to the
     * class definition that are compatible to serialization/externalization.
     * If a class definition changes, then the serialization/externalization
     * of the class is affected.
     * Refer to the writeExternal() and readExternal() methods.
     */
    // supported revision levels, add a new level to manage compatible changes
    static final int REVISION_2 = 2;
    // current revision level of this object
    static final int revisionID = REVISION_2;

    /**
     * @serial An ID which can be used to correlate operations on a single
     * message in the log files, irrespective of thread switches, persistence,
     * etc.
     */
    private String logCorrelationID = null;

    /**
     * @serial Options on the message
     */
    private Options options;

    /**
     * message attachments
     * NOTE: Serialization of message attachments is handled as part of the
     * overall message serialization.  If this needs to change, then
     * investigate having the Attachment class implement the
     * java.io.Externalizable interface.
     */
    private transient Attachments attachments;

    private final MessageContextFlags flags = new MessageContextFlagsEnumSet();

    private final ExecutionTracker execTracker = new DefaultExecutionTracker();

    private boolean paused;

    /**
     * AxisMessage associated with this message context
     */
    private transient AxisMessage axisMessage;

    /**
     * AxisOperation associated with this message context
     */
    private transient AxisOperation axisOperation;

    /**
     * AxisService
     */
    private transient AxisService axisService;

    /**
     * AxisServiceGroup
     * <p/>
     * Note the service group can be set independently of the service
     * so the service might not match up with this serviceGroup
     */
    private transient AxisServiceGroup axisServiceGroup;

    /**
     * ConfigurationContext
     */
    private transient ConfigurationContext configurationContext;

    /**
     * If we're processing this MC due to flowComplete() being called in the case
     * of an Exception, this will hold the Exception which caused the problem.
     */
    private Exception failureReason;

    /**
     * @serial SOAP envelope
     */
    private SOAPEnvelope envelope;

    /**
     * @serial OperationContext
     */
    private OperationContext operationContext;

    /**
     * @serial ServiceContext
     */
    private ServiceContext serviceContext;

    /**
     * @serial service context ID
     */
    private String serviceContextID;

    /**
     * @serial service group context
     */
    private ServiceGroupContext serviceGroupContext;

    /**
     * @serial Holds a key to retrieve the correct ServiceGroupContext.
     */
    private String serviceGroupContextId;

    /**
     * @serial sessionContext
     */
    private SessionContext<?> sessionContext;

    /**
     * transport out description
     */
    private transient TransportOutDescription transportOut;

    /**
     * transport in description
     */
    private transient TransportInDescription transportIn;

    /**
     * @serial incoming transport name
     */
    //The value will be set by the transport receiver and there will be
    //validation for the transport at the dispatch phase (its post condition)
    private String incomingTransportName;

    /*
     * SelfManagedData will hold message-specific data set by handlers
     * Note that this list is not explicitly saved by the MessageContext, but
     * through the SelfManagedDataManager interface implemented by handlers.
     */
    private transient LinkedHashMap<String, Object> selfManagedDataMap = null;

    //-------------------------------------------------------------------------
    // MetaData for data to be restored in activate() after readExternal()
    //-------------------------------------------------------------------------

    /**
     * Indicates whether the message context has been reconstituted
     * and needs to have its object references reconciled
     */
    private transient boolean needsToBeActivated = false;

    private OldMessageContextExternalizer externalizer;

    /**
     * Indicates whether this message context has an
     * AxisMessage object associated with it that needs to
     * be reconciled
     */
    private transient boolean reconcileAxisMessage = false;

    //----------------------------------------------------------------
    // end MetaData section
    //----------------------------------------------------------------

    /**
     * Constructor
     */
    public OldMessageContext() {
        super(null);
        options = new Options();
    }

    /**
     * Constructor has package access
     *
     * @param configContext the associated ConfigurationContext
     */
    OldMessageContext(ConfigurationContext configContext) {
        this();
        setConfigurationContext(configContext);
    }

    @Override
	public String toString() {
        return getLogCorrelationID();
    }

    /**
     * Get a "raw" version of the logCorrelationID.  The logCorrelationID
     * is guaranteed to be unique and may be persisted along with the rest
     * of the message context.
     *
     * @return A string that can be output to a log file as an identifier
     *         for this MessageContext.  It is suitable for matching related log
     *         entries.
     */
    public String getLogCorrelationID() {
        if (logCorrelationID == null) {
            logCorrelationID = UUIDGenerator.getUUID();
        }
        return logCorrelationID;
    }

    public void setLogCorrelationID(String correlationId) {
        logCorrelationID = correlationId;
    }

    /**
     * Pause the execution of the current handler chain
     */
    public void pause() {
        paused = true;
    }

    public boolean hasAxisOperation() {
    	return axisOperation != null;
    }

    public AxisOperation getAxisOperation() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getAxisOperation");
        }
        return axisOperation;
    }

    public AxisService getAxisService() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getAxisService");
        }
        return axisService;
    }

    /*
     * <P>
     * Note the service group can be set independently of the service
     * so the service might not match up with this serviceGroup
    */
    public AxisServiceGroup getAxisServiceGroup() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getAxisServiceGroup");
        }
        return axisServiceGroup;
    }

    public ConfigurationContext getConfigurationContext() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getConfigurationContext");
        }
        return configurationContext;
    }

    public int getCurrentHandlerIndex() {
    	return execTracker.getCurrentHandlerIndex();
    }

    public int getCurrentPhaseIndex() {
        return execTracker.getCurrentPhaseIndex();
    }

    /**
     * @return Returns SOAPEnvelope.
     */
    public SOAPEnvelope getEnvelope() {
        return envelope;
    }

    public List<Handler> getExecutionChain() {
		if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getExecutionChain");
        }
        return execTracker.getExecutionChain();
    }

    /**
     * Add a Phase to the collection of executed phases for the path.
     * Phases will be inserted in a LIFO data structure.
     *
     * @param phase The phase to add to the list.
     */
    public void addExecutedPhase(Handler phase) {
    	execTracker.addExecutedPhase(phase);
    }

    /**
     * Remove the first Phase in the collection of executed phases
     */
    public Handler removeFirstExecutedPhase() {
    	return execTracker.removeFirstExecutedPhase();
    }

    /**
     * Get an iterator over the executed phase list.
     *
     * @return An Iterator over the LIFO data structure.
     */
    public LinkedList<Handler> getExecutedPhases() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getExecutedPhases");
        }
        return execTracker.getExecutedPhases();
    }

    /**
     * Reset the list of executed phases.
     * This is needed because the OutInAxisOperation currently invokes
     * receive() even when a fault occurs, and we will have already executed
     * the flowComplete on those before receiveFault() is called.
     */
    public void resetExecutedPhases() {
    	execTracker.resetExecutedPhases();
    }

    /**
     * @return Returns EndpointReference.
     */
    public EndpointReference getFaultTo() {
        return options.getFaultTo();
    }

    /**
     * @return Returns EndpointReference.
     */
    public EndpointReference getFrom() {
        return options.getFrom();
    }

    /**
     * @return Returns message id.
     */
    public String getMessageID() {
        return options.getMessageId();
    }

    /**
     * Retrieves both module specific configuration parameters as well as other
     * parameters. The order of search is as follows:
     * <ol>
     * <li> Search in module configurations inside corresponding operation
     * description if its there </li>
     * <li> Search in corresponding operation if its there </li>
     * <li> Search in module configurations inside corresponding service
     * description if its there </li>
     * <li> Next search in Corresponding Service description if its there </li>
     * <li> Next search in module configurations inside axisConfiguration </li>
     * <li> Search in AxisConfiguration for parameters </li>
     * <li> Next get the corresponding module and search for the parameters
     * </li>
     * <li> Search in HandlerDescription for the parameter </li>
     * </ol>
     * <p/> and the way of specifying module configuration is as follows
     * <moduleConfig name="addressing"> <parameter name="addressingPara"
     * >N/A</parameter> </moduleConfig>
     *
     * @param key        :
     *                   Parameter Name
     * @param moduleName :
     *                   Name of the module
     * @param handler    <code>HandlerDescription</code>
     * @return Parameter <code>Parameter</code>
     */

    public Parameter getModuleParameter(String key, String moduleName,
                                        HandlerDescription handler) {
        Parameter param = null;
        ModuleConfiguration moduleConfig;
        ModuleConfigAccessor[] moduleConfigAccessors = {
        		getAxisOperation(), getAxisService(), getAxisServiceGroup(),
        		configurationContext.getAxisConfiguration()
        };

        for(ModuleConfigAccessor mca: moduleConfigAccessors) {
        	if (mca != null) {
        		moduleConfig = mca.getModuleConfig(moduleName);
                if (moduleConfig != null) {
                    param = moduleConfig.getParameter(key);
                    if (param == null && mca instanceof ParameterInclude) {
                        param = ((ParameterInclude)mca).getParameter(key);
                    }
                }
        		if (param != null) {
        			return param;
        		}
        	}
        }

        AxisModule module
        	= configurationContext.getAxisConfiguration().getModule(moduleName);
        param = module.getParameter(key);
        if (param != null) {
        	return param;
        }

        param = handler.getParameter(key);
        return param;
    }

    public OperationContext getOperationContext() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getOperationContext");
        }
        return operationContext;
    }

    /**
     * Retrieves configuration descriptor parameters at any level. The order of
     * search is as follows:
     * <ol>
     * <li> Search in message description if it exists </li>
     * <li> If parameter is not found or if axisMessage is null, search in
     * AxisOperation </li>
     * <li> If parameter is not found or if operationContext is null, search in
     * AxisService </li>
     * <li> If parameter is not found or if axisService is null, search in
     * AxisConfiguration </li>
     * </ol>
     *
     * @param key name of desired parameter
     * @return Parameter <code>Parameter</code>
     */
    public Parameter getParameter(String key) {

        if( axisMessage != null ) {
            return axisMessage.getParameter(key);
        }

        if (axisOperation != null) {
            return axisOperation.getParameter(key);
        }

        if (axisService != null) {
            return axisService.getParameter(key);
        }

        if (axisServiceGroup != null) {
            return axisServiceGroup.getParameter(key);
        }

        if (configurationContext != null) {
            AxisConfiguration baseConfig = configurationContext
                    .getAxisConfiguration();
            return baseConfig.getParameter(key);
        }
        return null;
    }

    /**
     * Retrieves a property value. The order of search is as follows: search in
     * my own map and then look at my options. Does not search up the hierarchy.
     *
     * @param name name of the property to search for
     * @return the value of the property, or null if the property is not found
     */
    @Override
	public Object getLocalProperty(String name) {
        return getLocalProperty(name, true);
    }
    public Object getLocalProperty(String name, boolean searchOptions) {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getProperty");
        }

        // search in my own options
        Object obj = super.getLocalProperty(name);
        if (obj != null) {
            return obj;
        }

        if (searchOptions) {
            obj = options.getProperty(name);
            if (obj != null) {
                return obj;
            }
        }

        // tough
        return null;
    }

    /**
     * Retrieves a property value. The order of search is as follows: search in
     * my own map and then look in my context hierarchy, and then in options.
     * Since its possible
     * that the entire hierarchy is not present, I will start at whatever level
     * has been set.
     *
     * @param name name of the property to search for
     * @return the value of the property, or null if the property is not found
     */
    @Override
	public Object getProperty(String name) {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getProperty");
        }

        // search in my own options
        Object obj = super.getProperty(name);
        if (obj != null) {
            return obj;
        }

        obj = options.getProperty(name);
        if (obj != null) {
            return obj;
        }

        // My own context hierarchy may not all be present. So look for whatever
        // nearest level is present and ask that to find the property.
        //
        // If the context is already an ancestor, it was checked during
        // the super.getProperty call.  In such cases, the second check
        // is not performed.
        if (operationContext != null) {
            if (!isAncestor(operationContext)) {
                obj = operationContext.getProperty(name);
            }
        } else if (serviceContext != null) {
            if (!isAncestor(serviceContext)) {
                obj = serviceContext.getProperty(name);
            }
        } else if (serviceGroupContext != null) {
            if (!isAncestor(serviceGroupContext)) {
                obj =  serviceGroupContext.getProperty(name);
            }
        } else if (configurationContext != null) {
            if (!isAncestor(configurationContext)) {
                obj = configurationContext.getProperty(name);
            }
        }

        return obj;
    }

    /**
     * Check if a given property is true.  Will return false if the property
     * does not exist or is not an explicit "true" value.
     *
     * @param name name of the property to check
     * @return true if the property exists and is Boolean.TRUE, "true", 1, etc.
     */
    public boolean isPropertyTrue(String name) {
        return isPropertyTrue(name, false);
    }

    /**
     * Check if a given property is true.  Will return the passed default if the property
     * does not exist.
     *
     * @param name name of the property to check
     * @param defaultVal the default value if the property doesn't exist
     * @return true if the property exists and is Boolean.TRUE, "true", 1, etc.
     */
    public boolean isPropertyTrue(String name, boolean defaultVal) {
        return JavaUtils.isTrueExplicitly(getProperty(name), defaultVal);
    }

    /**
     * Retrieves all property values. The order of search is as follows: search in
     * my own options and then look in my context hierarchy. Since its possible
     * that the entire hierarchy is not present, it will start at whatever level
     * has been set and start there.
     * The returned map is unmodifiable, so any changes to the properties have
     * to be done by calling {@link #setProperty(String,Object)}. In addition,
     * any changes to the properties are not reflected on this map.
     *
     * @return An unmodifiable map containing the combination of all available
     *         properties or an empty map.
     */
    @Override
	public Map<String, Object> getProperties() {
        final Map<String, Object> resultMap = new HashMap<String, Object>();

        // My own context hierarchy may not all be present. So look for whatever
        // nearest level is present and add the properties
        // We have to access the contexts in reverse order, in order to allow
        // a nearer context to overwrite values from a more distant context
        if (configurationContext != null) {
            resultMap.putAll(configurationContext.getProperties());
        }
        if (serviceGroupContext != null) {
            resultMap.putAll(serviceGroupContext.getProperties());
        }
        if (serviceContext != null) {
            resultMap.putAll(serviceContext.getProperties());
        }
        if (operationContext != null) {
            resultMap.putAll(operationContext.getProperties());
        }
        // and now add options
        resultMap.putAll(options.getProperties());
        resultMap.putAll(properties);
        return Collections.unmodifiableMap(resultMap);
    }

    /**
     * @return Returns RelatesTo array.
     */
    public RelatesTo[] getRelationships() {
        return options.getRelationships();
    }

    /**
     * Get any RelatesTos of a particular type associated with this MessageContext
     *
     * @param type the relationship type
     * @return Returns RelatesTo.
     */
    public RelatesTo getRelatesTo(String type) {
        return options.getRelatesTo(type);
    }

    /**
     * @return Returns RelatesTo.
     */
    public RelatesTo getRelatesTo() {
        return options.getRelatesTo();
    }

    /**
     * @return Returns EndpointReference.
     */
    public EndpointReference getReplyTo() {
        return options.getReplyTo();
    }

    /**
     * @return Returns ServiceContext.
     */
    public ServiceContext getServiceContext() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getServiceContext");
        }
        return serviceContext;
    }

    /**
     * @return Returns the serviceContextID.
     */
    public String getServiceContextID() {
        return serviceContextID;
    }

    public ServiceGroupContext getServiceGroupContext() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getServiceGroupContext");
        }
        return serviceGroupContext;
    }

    public String getServiceGroupContextId() {
        return serviceGroupContextId;
    }

    /**
     * @return Returns SessionContext.
     */
    public SessionContext<?> getSessionContext() {
        return sessionContext;
    }

    public void setSessionContext(SessionContext<?> sessionContext) {
        this.sessionContext = sessionContext;
    }


    /**
     * @return Returns soap action.
     */
    public String getSoapAction() {
        return options.getAction();
    }

    /**
     * @return Returns EndpointReference.
     */
    public EndpointReference getTo() {
        return options.getTo();
    }

    /**
     * @return Returns TransportInDescription.
     */
    public TransportInDescription getTransportIn() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getTransportIn");
        }
        return transportIn;
    }

    /**
     * @return Returns TransportOutDescription.
     */
    public TransportOutDescription getTransportOut() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getTransportOut");
        }
        return transportOut;
    }

    public String getWSAAction() {
        return options.getAction();
    }

    /**
     * @return Returns boolean.
     */
    public boolean isDoingMTOM() {
        return flags.isDoingMTOM();
    }

    /**
     * @return Returns boolean.
     */
    public boolean isDoingREST() {
        return flags.isDoingREST();
    }

    /**
     * @return Returns boolean.
     */
    public boolean isDoingSwA() {
        return flags.isDoingSwA();
    }

    /**
     * @return Returns boolean.
     */
    public boolean isNewThreadRequired() {
        return flags.isNewThreadRequired();
    }

    /**
     * @return Returns boolean.
     */
    public boolean isOutputWritten() {
        return flags.isOutputWritten();
    }

    /**
     * @return Returns boolean.
     */
    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * @return Returns boolean.
     */
    public boolean isProcessingFault() {
        return flags.isProcessingFault();
    }

    /**
     * @return Returns boolean.
     */
    public boolean isResponseWritten() {
        return flags.isResponseWritten();
    }

    public boolean isSOAP11() {
        return flags.isSOAP11();
    }

    /**
     * @return inbound content length of 0
     */
    public long getInboundContentLength() throws IOException {
        // If there is an attachment map, the Attachments keep track
        // of the inbound content length.
        if (attachments != null) {
//            return attachments.getContentLength();
        }

        // Otherwise the length is accumulated by the DetachableInputStream.
        DetachableInputStream dis =
            (DetachableInputStream) getProperty(Constants.DETACHABLE_INPUT_STREAM);
        if (dis != null) {
            return dis.length();
        }
        return 0;
    }
    /**
     * @return Returns boolean.
     */
    public boolean isServerSide() {
        return flags.isServerSide();
    }

    public boolean getReconcileAxisMessage() {
    	return reconcileAxisMessage;
    }

    public void setReconcileAxisMessage(boolean b) {
    	reconcileAxisMessage = b;
    }

    public AxisMessage getAxisMessage() {
        if (reconcileAxisMessage) {
            if (LoggingControl.debugLoggingAllowed && log.isWarnEnabled()) {
                log.warn(this.getLogCorrelationID() +
                    ":getAxisMessage(): ****WARNING**** MessageContext.activate(configurationContext) needs to be invoked.");
            }
        }

        return axisMessage;
    }

    public void setAxisMessage(AxisMessage axisMessage) {
        this.axisMessage = axisMessage;
    }

    public void setAxisOperation(AxisOperation axisOperation) {
        this.axisOperation = axisOperation;
    }

    public void setAxisService(AxisService axisService) {
        this.axisService = axisService;
        if (this.axisService != null) {
            this.axisServiceGroup = axisService.getServiceGroup();
        } else {
            this.axisServiceGroup = null;
        }
    }

    /*
     * note setAxisServiceGroup() does not verify that the service is associated with the service group!
     */
    public void setAxisServiceGroup(AxisServiceGroup axisServiceGroup) {
        // need to set the axis service group object to null when necessary
        // for example, when extracting the message context object from
        // the object graph
        this.axisServiceGroup = axisServiceGroup;
    }

    /**
     * @param context
     */
    public void setConfigurationContext(ConfigurationContext context) {
        configurationContext = context;
    }

    public void setCurrentHandlerIndex(int currentHandlerIndex) {
        execTracker.setCurrentHandlerIndex(currentHandlerIndex);
    }

    public void setCurrentPhaseIndex(int currentPhaseIndex) {
        execTracker.setCurrentPhaseIndex(currentPhaseIndex);
    }

    public void setDoingMTOM(boolean b) {
        flags.setDoingMTOM(b);
    }

    public void setDoingREST(boolean b) {
        flags.setDoingREST(b);
    }

    public void setDoingSwA(boolean b) {
        flags.setDoingSwA(b);
    }

    /**
     * @param envelope
     */
    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        this.envelope = envelope;

        if (this.envelope != null) {
            String soapNamespaceURI = envelope.getNamespace().getNamespaceURI();

            if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI
                    .equals(soapNamespaceURI)) {
                setSOAP11(false);
            } else if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI
                    .equals(soapNamespaceURI)) {
                setSOAP11(true);
            } else {
                throw new AxisFault(
                        "Unknown SOAP Version. Current Axis handles only SOAP 1.1 and SOAP 1.2 messages");
            }
            // Inform the listeners of an attach envelope event
            if (getAxisService() != null) {
                getAxisService().attachEnvelopeEvent(this);
            }
        }
    }


    public void setExecutionChain(List<? extends Handler> executionChain) {
    	execTracker.setExecutionChain(executionChain);
    }

    /**
     * @param reference
     */
    public void setFaultTo(EndpointReference reference) {
        options.setFaultTo(reference);
    }

    /**
     * @param reference
     */
    public void setFrom(EndpointReference reference) {
        options.setFrom(reference);
    }

    /**
     * @param messageId
     */
    public void setMessageID(String messageId) {
        options.setMessageId(messageId);
    }

    /**
     * @param b
     */
    public void setNewThreadRequired(boolean b) {
        flags.setNewThreadRequired(b);
    }

    /**
     * @param context The OperationContext
     */
    public void setOperationContext(OperationContext context) {
        // allow setting the fields to null
        // useful when extracting the messge context from the object graph
        operationContext = context;

        this.setParent(operationContext);

        if (operationContext != null) {
            if (serviceContext == null) {
                setServiceContext(operationContext.getServiceContext());
            } else {
                if (operationContext.getParent() != serviceContext) {
                    throw new AxisError("ServiceContext in OperationContext does not match !");
                }
            }

            this.setAxisOperation(operationContext.getAxisOperation());
        }
    }

    /**
     * @param b
     */
    public void setOutputWritten(boolean b) {
        flags.setOutputWritten(b);
    }

    /**
     * @param b
     */
    public void setProcessingFault(boolean b) {
        flags.setProcessingFault(b);
    }

    /**
     * Add a RelatesTo
     *
     * @param reference RelatesTo describing how we relate to another message
     */
    public void addRelatesTo(RelatesTo reference) {
        options.addRelatesTo(reference);
    }

    /**
     * Set ReplyTo destination
     *
     * @param reference the ReplyTo EPR
     */
    public void setReplyTo(EndpointReference reference) {
        options.setReplyTo(reference);
    }

    /**
     * @param b
     */
    public void setResponseWritten(boolean b) {
        flags.setResponseWritten(b);
    }

    /**
     * @param b
     */
    public void setServerSide(boolean b) {
        flags.setServerSide(b);
    }

    /**
     * @param context
     */
    public void setServiceContext(ServiceContext context) {

        // allow the service context to be set to null
        // this allows the message context object to be extraced from
        // the object graph

        serviceContext = context;

        if (serviceContext != null) {
            if ((operationContext != null)
                    && (operationContext.getParent() != context)) {
                throw new AxisError("ServiceContext and OperationContext.parent do not match!");
            }
            // setting configcontext using configuration context in service context
            if (configurationContext == null) {
                // setting configcontext
                configurationContext = context.getConfigurationContext();
            }
            if (serviceGroupContext == null) {
                // setting service group context
                serviceGroupContext = context.getServiceGroupContext();
            }
            AxisService axisService = context.getAxisService();
            this.setAxisService(axisService);

            // Inform the listeners of an attach event
            if (axisService != null) {
                axisService.attachServiceContextEvent(serviceContext, this);
            }
        }
    }

    /**
     * Sets the service context id.
     *
     * @param serviceContextID
     */
    public void setServiceContextID(String serviceContextID) {
        this.serviceContextID = serviceContextID;
    }

    public void setServiceGroupContext(ServiceGroupContext serviceGroupContext) {
        // allow the service group context to be set to null
        // this allows the message context object to be extraced from
        // the object graph

        this.serviceGroupContext = serviceGroupContext;

        if (this.serviceGroupContext != null) {
            this.axisServiceGroup = serviceGroupContext.getDescription();
        }
    }

    public void setServiceGroupContextId(String serviceGroupContextId) {
        this.serviceGroupContextId = serviceGroupContextId;
    }

    /**
     * @param soapAction
     */
    public void setSoapAction(String soapAction) {
        options.setAction(soapAction);
    }

    /**
     * @param to
     */
    public void setTo(EndpointReference to) {
        options.setTo(to);
    }

    /**
     * @param in
     */
    public void setTransportIn(TransportInDescription in) {
        this.transportIn = in;
    }

    /**
     * @param out
     */
    public void setTransportOut(TransportOutDescription out) {
        transportOut = out;
    }

    /**
     * setWSAAction
     */
    public void setWSAAction(String actionURI) {
        options.setAction(actionURI);
    }

    public void setWSAMessageId(String messageID) {
        options.setMessageId(messageID);
    }

    public Options getOptions() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getOptions");
        }
        return options;
    }

    /**
     * Set the options for myself. I make the given options my own options'
     * parent so that that becomes the default. That allows the user to override
     * specific options on a given message context and not affect the overall
     * options.
     *
     * @param options the options to set
     */
    public void setOptions(Options options) {
        this.options.setParent(options);
    }

    public String getIncomingTransportName() {
        return incomingTransportName;
    }

    public void setIncomingTransportName(String incomingTransportName) {
        this.incomingTransportName = incomingTransportName;
    }

    public void setRelationships(RelatesTo[] list) {
        options.setRelationships(list);
    }


    public Policy getEffectivePolicy() {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("getEffectivePolicy");
        }

        AxisBindingMessage bindingMessage =
        	(AxisBindingMessage) getProperty(Constants.AXIS_BINDING_MESSAGE);

        // If AxisBindingMessage is not set, try to find the binding message from the AxisService
        if (bindingMessage == null) {
        	bindingMessage = findBindingMessage();
        }

        if (bindingMessage != null) {
            return bindingMessage.getEffectivePolicy();
        // If we can't find the AxisBindingMessage, then try the AxisMessage
        } else if (axisMessage != null) {
        		return axisMessage.getEffectivePolicy();
        } else {
        		return null;
        }
    }

    private AxisBindingMessage findBindingMessage() {
    	if (axisService != null && axisOperation != null ) {
			if (axisService.getEndpointName() != null) {
				AxisEndpoint axisEndpoint = axisService
						.getEndpoint(axisService.getEndpointName());
				if (axisEndpoint != null) {
					AxisBinding axisBinding = axisEndpoint.getBinding();
                    AxisBindingOperation axisBindingOperation = axisBinding
							.getBindingOperation(axisOperation.getName());

                    //If Binding Operation is not found, just return null
                    if (axisBindingOperation == null) {
                       return null;
                    }

                    String direction = axisMessage.getDirection();
					AxisBindingMessage axisBindingMessage = null;
					if (WSDLConstants.WSDL_MESSAGE_DIRECTION_IN
							.equals(direction)
							&& WSDLUtil
									.isInputPresentForMEP(axisOperation
											.getMessageExchangePattern())) {
						axisBindingMessage = axisBindingOperation
								.getChild(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
						return axisBindingMessage;

					} else if (WSDLConstants.WSDL_MESSAGE_DIRECTION_OUT
							.equals(direction)
							&& WSDLUtil
									.isOutputPresentForMEP(axisOperation
											.getMessageExchangePattern())) {
						axisBindingMessage = axisBindingOperation
								.getChild(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
						return axisBindingMessage;
					}
				}

			}
		}
    	return null;
    }


    public boolean isEngaged(String moduleName) {
        if (LoggingControl.debugLoggingAllowed) {
            checkActivateWarning("isEngaged");
        }
        boolean enegage;
        if (configurationContext != null) {
            AxisConfiguration axisConfig = configurationContext.getAxisConfiguration();
            AxisModule module = axisConfig.getModule(moduleName);
            if (module == null) {
                return false;
            }
            enegage = axisConfig.isEngaged(module);
            if (enegage) {
                return true;
            }
            if (axisServiceGroup != null) {
                enegage = axisServiceGroup.isEngaged(module);
                if (enegage) {
                    return true;
                }
            }
            if (axisService != null) {
                enegage = axisService.isEngaged(module);
                if (enegage) {
                    return true;
                }
            }
            if (axisOperation != null) {
                enegage = axisOperation.isEngaged(module);
                if (enegage) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the first child of the envelope, check if it is a soap:Body, which means there is no header.
     * We do this basically to make sure we don't parse and build the om tree of the whole envelope
     * looking for the soap header. If this method returns true, there still is no guarantee that there is
     * a soap:Header present, use getHeader() and also check for null on getHeader() to be absolutely sure.
     *
     * @return boolean
     * @deprecated The bonus you used to get from this is now built in to SOAPEnvelope.getHeader()
     */
    @Deprecated
	public boolean isHeaderPresent() {
        // If there's no envelope there can't be a header.
        if (this.envelope == null) {
            return false;
        }
        return (this.envelope.getHeader() != null);
    }

    /**
     * Setting of the attachments map should be performed at the receipt of a
     * message only. This method is only meant to be used by the Axis2
     * internals.
     *
     * @param attachments
     */
    public void setAttachments(Attachments attachments) {
        this.attachments = attachments;
    }

    /**
     * You can directly access the attachment map of the message context from
     * here. Returned attachment map can be empty.
     *
     * @return attachment
     */
    public Attachments getAttachments() {
        if (attachments == null) {
            attachments = new Attachments();
        }
        return attachments;
    }

    /**
     * Adds an attachment to the attachment Map of this message context. This
     * attachment gets serialised as a MIME attachment when sending the message
     * if SOAP with Attachments is enabled.
     *
     * @param contentID   :
     *                    will be the content ID of the MIME part (without the "cid:" prefix)
     * @param dataHandler
     */
    public void addAttachment(String contentID, DataHandler dataHandler) {
        if (attachments == null) {
            attachments = new Attachments();
        }
        attachments.addDataHandler(contentID, dataHandler);
    }

    /**
     * Adds an attachment to the attachment Map of this message context. This
     * attachment gets serialised as a MIME attachment when sending the message
     * if SOAP with Attachments is enabled. Content ID of the MIME part will be
     * auto generated by Axis2.
     *
     * @param dataHandler
     * @return the auto generated content ID of the MIME attachment
     */
    public String addAttachment(DataHandler dataHandler) {
        String contentID = UUIDGenerator.getUUID();
        addAttachment(contentID, dataHandler);
        return contentID;
    }

    /**
     * Access the DataHandler of the attachment contained in the map corresponding to the given
     * content ID. Returns "NULL" if a attachment cannot be found by the given content ID.
     *
     * @param contentID :
     *                  Content ID of the MIME attachment (without the "cid:" prefix)
     * @return Data handler of the attachment
     */
    public DataHandler getAttachment(String contentID) {
        if (attachments == null) {
            attachments = new Attachments();
        }
        return attachments.getDataHandler(contentID);
    }

    /**
     * Removes the attachment with the given content ID from the Attachments Map
     * Do nothing if a attachment cannot be found by the given content ID.
     *
     * @param contentID of the attachment (without the "cid:" prefix)
     */
    public void removeAttachment(String contentID) {
        if (attachments != null) {
            attachments.removeDataHandler(contentID);
        }
    }

    /*
     * ===============================================================
     * SelfManagedData Section
     * ===============================================================
     */

    /*
    * character to delimit strings
    */
    private final String selfManagedDataDelimiter = "*";


    /**
     * Set up a unique key in the form of
     * <OL>
     * <LI>the class name for the class that owns the key
     * <LI>delimitor
     * <LI>the key as a string
     * <LI>delimitor
     * <LI>the key's hash code as a string
     * </OL>
     *
     * @param clazz The class that owns the supplied key
     * @param key   The key
     * @return A string key
     */
    private String generateSelfManagedDataKey(Class<?> clazz, Object key) {
        return clazz.getName() + selfManagedDataDelimiter + key.toString() +
                selfManagedDataDelimiter + Integer.toString(key.hashCode());
    }

    /**
     * Add a key-value pair of self managed data to the set associated with
     * this message context.
     * <p/>
     * This is primarily intended to allow handlers to manage their own
     * message-specific data when the message context is saved/restored.
     *
     * @param clazz The class of the caller that owns the key-value pair
     * @param key   The key for this data object
     * @param value The data object
     */
    public void setSelfManagedData(Class<?> clazz, Object key, Object value) {
        if (selfManagedDataMap == null) {
            selfManagedDataMap = new LinkedHashMap<String, Object>();
        }

        // make sure we have a unique key and a delimiter so we can
        // get the classname and hashcode for serialization/deserialization
        selfManagedDataMap.put(generateSelfManagedDataKey(clazz, key), value);
    }

    /**
     * Retrieve a value of self managed data previously saved with the specified key.
     *
     * @param clazz The class of the caller that owns the key-value pair
     * @param key   The key for the data
     * @return The data object associated with the key, or NULL if not found
     */
    public Object getSelfManagedData(Class<?> clazz, Object key) {
        if (selfManagedDataMap != null) {
            return selfManagedDataMap.get(generateSelfManagedDataKey(clazz, key));
        }
        return null;
    }

    /**
     * Check to see if the key for the self managed data is available
     *
     * @param clazz The class of the caller that owns the key-value pair
     * @param key   The key to look for
     * @return TRUE if the key exists, FALSE otherwise
     */
    public boolean containsSelfManagedDataKey(Class<?> clazz, Object key) {
        if (selfManagedDataMap == null) {
            return false;
        }
        return selfManagedDataMap.containsKey(generateSelfManagedDataKey(clazz, key));
    }

    /**
     * Removes the mapping of the specified key if the specified key
     * has been set for self managed data
     *
     * @param clazz The class of the caller that owns the key-value pair
     * @param key   The key of the object to be removed
     */
    public void removeSelfManagedData(Class<?> clazz, Object key) {
        if (selfManagedDataMap != null) {
            selfManagedDataMap.remove(generateSelfManagedDataKey(clazz, key));
        }
    }

    /**
     * Return a Read-Only copy of this message context that has been extracted
     * from the object hierachy.  In other words, the message context copy does
     * not have links to the object graph.
     * <p/>
     * NOTE: The copy shares certain objects with the original.  The intent is
     * to use the copy to read values but not modify them, especially since the
     * copy is not part of the normal *Context and Axis* object graph.
     *
     * @return A copy of the message context that is not in the object graph
     */
    public MessageContext extractCopyMessageContext() {
        MessageContext copy = new OldMessageContext();
        String logCorrelationIDString = getLogCorrelationID();
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString + ":extractCopyMessageContext():  based on " +
                    logCorrelationIDString + "   into copy " + copy.getLogCorrelationID());
        }

        //---------------------------------------------------------
        // various simple fields
        //---------------------------------------------------------

        copy.setFlow(getFlow());

        copy.setProcessingFault(isProcessingFault());
        copy.setPaused(isPaused());
        copy.setOutputWritten(isOutputWritten());
        copy.setNewThreadRequired(isNewThreadRequired());
        copy.setDoingREST(isDoingREST());
        copy.setDoingMTOM(isDoingMTOM());
        copy.setDoingSwA(isDoingSwA());
        copy.setResponseWritten(isResponseWritten());
        copy.setServerSide(isServerSide());

        copy.setLastTouchedTime(getLastTouchedTime());

        //---------------------------------------------------------
        // message
        //---------------------------------------------------------
        try {
            copy.setEnvelope(envelope);
        }
        catch (Exception ex) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(logCorrelationIDString +
                    ":extractCopyMessageContext():  Exception caught when setting the copy with the envelope",
                      ex);
            }
        }

        copy.setAttachments(attachments);

        copy.setSOAP11(isSOAP11());

        //---------------------------------------------------------
        // ArrayList executionChain
        //     handler and phase related data
        //---------------------------------------------------------
        copy.setExecutionChain(getExecutionChain());

        // the setting of the execution chain is actually a reset
        // so copy the indices after putting in the execution chain
        copy.setCurrentHandlerIndex(getCurrentHandlerIndex());
        copy.setCurrentPhaseIndex(getCurrentPhaseIndex());

        //---------------------------------------------------------
        // LinkedList executedPhases
        //---------------------------------------------------------
        copy.setExecutedPhasesExplicit(getExecutedPhases());

        //---------------------------------------------------------
        // options
        //---------------------------------------------------------
        copy.setOptionsExplicit(options);

        //---------------------------------------------------------
        // axis operation
        //---------------------------------------------------------
        copy.setAxisOperation(null);

        //---------------------------------------------------------
        // operation context
        //---------------------------------------------------------
        copy.setOperationContext(null);

        //---------------------------------------------------------
        // axis service
        //---------------------------------------------------------
        copy.setAxisService(null);

        //-------------------------
        // serviceContextID string
        //-------------------------
        copy.setServiceContextID(serviceContextID);

        //-------------------------
        // serviceContext
        //-------------------------
        copy.setServiceContext(null);

        //---------------------------------------------------------
        // serviceGroup
        //---------------------------------------------------------
        copy.setServiceGroupContext(null);

        //-----------------------------
        // serviceGroupContextId string
        //-----------------------------
        copy.setServiceGroupContextId(serviceGroupContextId);

        //---------------------------------------------------------
        // axis message
        //---------------------------------------------------------
        copy.setAxisMessage(axisMessage);

        //---------------------------------------------------------
        // configuration context
        //---------------------------------------------------------
        copy.setConfigurationContext(configurationContext);

        //---------------------------------------------------------
        // session context
        //---------------------------------------------------------
        copy.setSessionContext(sessionContext);

        //---------------------------------------------------------
        // transport
        //---------------------------------------------------------

        //------------------------------
        // incomingTransportName string
        //------------------------------
        copy.setIncomingTransportName(incomingTransportName);

        copy.setTransportIn(transportIn);
        copy.setTransportOut(transportOut);

        //---------------------------------------------------------
        // properties
        //---------------------------------------------------------
        // Only set the local properties (i.e. don't use getProperties())
        copy.setProperties(properties);

        //---------------------------------------------------------
        // special data
        //---------------------------------------------------------

        copy.setSelfManagedDataMapExplicit(selfManagedDataMap);

        //---------------------------------------------------------
        // done
        //---------------------------------------------------------

        return copy;
    }

    //------------------------------------------------------------------------
    // additional setter methods needed to copy the message context object
    //------------------------------------------------------------------------

    public void setSOAP11(boolean t) {
        flags.setSOAP11(t);
    }

    public void setExecutedPhasesExplicit(LinkedList<Handler> inb) {
    	execTracker.setExecutedPhasesExplicit(inb);
    }

    public void setSelfManagedDataMapExplicit(LinkedHashMap<String, Object> map) {
        selfManagedDataMap = map;
    }

    public Map<String, Object> getSelfManagedDataMap() {
    	return selfManagedDataMap;
    }

    public void setOptionsExplicit(Options op) {
        this.options = op;
    }

    /**
     * Trace a warning message, if needed, indicating that this
     * object needs to be activated before accessing certain fields.
     *
     * @param methodname The method where the warning occurs
     */
    private void checkActivateWarning(String methodname) {
        if (needsToBeActivated()) {
            if (LoggingControl.debugLoggingAllowed && log.isWarnEnabled()) {
                log.warn(getLogCorrelationID() + ":" + methodname + "(): ****WARNING**** " + getClass().getName() +
                    ".activate(configurationContext) needs to be invoked.");
            }
        }
    }

    @Override
	public ConfigurationContext getRootContext() {
        return configurationContext;
    }

    public boolean isFault() {
        try {
            return getEnvelope().hasFault();
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Obtain the Exception which caused the processing chain to halt.
     * @return null, or an Exception.
     */
    public Exception getFailureReason() {
        return failureReason;
    }

    /**
     * Set the failure reason.  Only AxisEngine should ever do this.
     *
     * @param failureReason an Exception which caused processing to halt.
     */
    public void setFailureReason(Exception failureReason) {
        this.failureReason = failureReason;
    }

	@Override
	public boolean hasAxisMessage() {
		return axisMessage != null;
	}

	@Override
	public boolean hasAxisService() {
		return axisService != null;
	}

	@Override
	public boolean hasAxisServiceGroup() {
		return axisServiceGroup != null;
	}

	@Override
	public void setFault(boolean b) {
		flags.setFault(b);
	}

	@Override
	public MessageContext getMessageContext() {
		return this;
	}

	@Override
	public void setMessageContext(MessageContext messageContext) {
		//do nothing
	}

	@Override
	public Flows getFlow() {
		return execTracker.getFlow();
	}

	@Override
	public void setFlow(Flows flow) {
		execTracker.setFlow(flow);
	}

	@Override
	public List<Handler> flattenHandlerList(List<Handler> list,
											Map<String, Handler> map)
	{
		return execTracker.flattenHandlerList(list, map);
	}

	@Override
	public List<Handler> flattenPhaseListToHandlers(List<Handler> list,
													Map<String, Handler> map)
	{
		return execTracker.flattenPhaseListToHandlers(list, map);
	}

	@Override
	public boolean isExecutedPhasesReset() {
		return execTracker.isExecutedPhasesReset();
	}

	private void ensureExternalizer() {
		if(externalizer == null) {
			externalizer = new OldMessageContextExternalizer(this);
		}
	}
	public void readExternal(ObjectInput inObject)
		throws IOException, ClassNotFoundException
	{
		ensureExternalizer();
		externalizer.readExternal(inObject);
	}

	public void writeExternal(ObjectOutput outObject)
		throws IOException
	{
		ensureExternalizer();
		externalizer.writeExternal(outObject);
	}

	public boolean needsToBeActivated() {
		return needsToBeActivated;
	}

	public void setNeedsToBeActivated(boolean b) {
		this.needsToBeActivated = b;
	}

	@Override
	public void activate(ConfigurationContext cc) {
		ensureExternalizer();
		externalizer.activate(cc);
	}

	@Override
	public void activateWithOperationContext(OperationContext operationCtx) {
		ensureExternalizer();
		externalizer.activateWithOperationContext(operationCtx);
	}

    public void checkMustUnderstand() throws AxisFault {

        SOAPEnvelope envelope = getEnvelope();
        if (envelope.getHeader() == null) {
            return;
        }
        Set<QName> unprocessed = null;
        // Get all the headers targeted to us
        final RolePlayer rolePlayer
        	= (RolePlayer)getConfigurationContext().getAxisConfiguration().getParameterValue("rolePlayer");
        for(SOAPHeaderBlock headerBlock: envelope.getHeader().getHeadersToProcess(rolePlayer)) {
            // if this header block has been processed or mustUnderstand isn't
            // turned on then its cool
            if (headerBlock.isProcessed() || !headerBlock.getMustUnderstand()) {
                continue;
            }

            QName headerName = headerBlock.getQName();
            if(LoggingControl.debugLoggingAllowed && log.isDebugEnabled()){
                log.debug("MustUnderstand header not processed or registered as understood"+headerName);
            }
            if(isReceiverMustUnderstandProcessor()){
                if(unprocessed == null){
                    unprocessed = new TreeSet<QName>();
                }
                unprocessed.add(headerName);
                continue;
            }
            // Oops, throw an appropriate MustUnderstand fault!!
            QName faultQName = headerBlock.getVersion().getMustUnderstandFaultCode();
            throw new AxisFault(Messages.getMessage("mustunderstandfailed",
                headerBlock.getNamespace().getNamespaceURI(),
                headerBlock.getLocalName()), faultQName);
        }
        if(unprocessed != null && !unprocessed.isEmpty()){
            //Adding HeaderQNames that failed MU check as AxisService Parameter
            //They will be examined later by MessageReceivers.
            if(log.isDebugEnabled()){
                log.debug("Adding Unprocessed headers to MessageContext.");
            }
            setProperty(Constants.UNPROCESSED_HEADER_QNAMES, unprocessed);
        }
    }

    private boolean isReceiverMustUnderstandProcessor() {
        MessageReceiver receiver = null;
        if(isServerSide()) {
            receiver = getAxisOperation().getMessageReceiver();
        }
        return (receiver != null && receiver.getClass().getName().endsWith("JAXWSMessageReceiver"));
    }
}
