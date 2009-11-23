/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse;


import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.commons.logging.Log;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;

/**
 * The Synapse Message Context is available to all mediators through which it
 * flows. It allows one to call to the underlying SynapseEnvironment (i.e. the
 * SOAP engine - such as Axis2) where required. It also allows one to access the
 * current SynapseConfiguration. Additionally it holds per message properties
 * (i.e. local properties valid for the lifetime of the message), and the
 * current SOAPEnvelope
 */
public interface SynapseMessageContext {

    /**
     * Get a reference to the current SynapseConfiguration
     *
     * @return the current synapse configuration
     */
	SynapseConfiguration getConfiguration();

    /**
     * Returns a reference to the host Synapse Environment
     * @return the Synapse Environment
     */
    SynapseEnvironment getEnvironment();

    /**
     * Return all the entries which are in the MessageContext. This does not represent
     * all the declared entries in the configuration, rather only the entries that the
     * context has already used. This will not lookup for the entries in the Configuration.
     * @return the set of local entries in the context
     */
    Map<String, Object> getContextEntries();

    /**
     * Sets the entries to the current context and not to the configuration. This can be
     * used to forcibly override an existing set of resources in the configuration, because
     * the resource lookup will look for the context first. But this only sets the entries
     * to the current context
     * @param entries the set of local entries to be set
     */
    void setContextEntries(Map<String, Object> entries);

    /**
     * Return the main sequence from the configuration, or the local message context
     * This method looks up for the sequence named Constants.MAIN_SEQUENCE_KEY from
     * the local message context to make this look up transactional - i.e. a request and
     * response message pair will not see a difference in the main sequence if the main
     * sequence was dynamic and changed in between at the registry
     * @return the main sequence to be used for mediation
     */
    Mediator getMainSequence();

    /**
     * Return the fault sequence from the configuration, or the local message context
     * This method looks up for the sequence named Constants.FAULT_SEQUENCE_KEY from
     * the local message context to make this look up transactional - i.e. a request and
     * response message pair will not see a difference in the fault sequence if the fault
     * sequence was dynamic and changed in between at the registry
     * @return the fault sequence to be used for mediation
     */
    Mediator getFaultSequence();

    /**
     * Return the sequence with the given key from the configuration, or the local message
     * context. This method looks up for the sequence with the given key from the local
     * message context to make this look up transactional - i.e. a request and response
     * message pair will not see a difference in the said sequence if it was dynamic and
     * changed in between at the registry
     * @param key the sequence key to be looked up
     * @return the sequence mediator mapped to the key
     */
    Mediator getSequence(String key);

    /**
     * Return the endpoint with the given key from the configuration, or the local message
     * context. This method looks up for the endpoint with the given key from the local
     * message context to make this look up transactional - i.e. a request and response
     * message pair will not see a difference in the said endpoint if it was dynamic and
     * changed in between at the registry
     * @param key the endpoint key to be looked up
     * @return the endpoint mapped to the key
     */
    Endpoint getEndpoint(String key);

    /**
     * Get the value of a custom (local) property set on the message instance
     * @param key key to look up property
     * @return value for the given key
     */
    Object getProperty(String key);

    /**
     * Get the value of a property set on the message instance, from the local registry
     * or the remote registry - by cascading through
     * @param key key to look up property
     * @return value for the given key
     */
    Object getEntry(String key);

    /**
     * Set a custom (local) property with the given name on the message instance
     * @param key key to be used
     * @param value value to be saved
     */
    void setProperty(String key, Object value);

    /**
     * Returns the Set of keys over the properties on this message context
     * @return a Set of keys over message properties
     */
    Set<String> getPropertyKeySet();

    /**
     * Get the SOAP envelope of this message
     * @return the SOAP envelope of the message
     */
    SOAPEnvelope getEnvelope();

    /**
     * Sets the given envelope as the current SOAPEnvelope for this message
     * @param envelope the envelope to be set
     * @throws org.apache.axis2.AxisFault on exception
     */
    void setEnvelope(SOAPEnvelope envelope) throws AxisFault;

    // --- SOAP Message related methods ------
    /**
     * Get the faultTo EPR if available
     * @return FaultTo epr if available
     */
    EndpointReference getFaultTo();

    /**
     * Set the faultTo EPR
     * @param reference epr representing the FaultTo address
     */
    void setFaultTo(EndpointReference reference);

    /**
     * Get the from EPR if available
     * @return From epr if available
     */
    EndpointReference getFrom();

    /**
     * Set the from EPR
     * @param reference epr representing the From address
     */
    void setFrom(EndpointReference reference);

    /**
     * Get the message id if available
     * @return message id if available
     */
    String getMessageID();

    /**
     * Set the message id
     * @param string message id to be set
     */
    void setMessageID(String string);

    /**
     * Get the relatesTo of this message
     * @return RelatesTo of the message if available
     */
    RelatesTo getRelatesTo();

    /**
     * Sets the relatesTo references for this message
     * @param reference the relatesTo references array
     */
    void setRelatesTo(RelatesTo[] reference);

    /**
     * Get the replyTo EPR if available
     * @return ReplyTo epr of the message if available
     */
    EndpointReference getReplyTo();

    /**
     * Set the replyTo EPR
     * @param reference epr representing the ReplyTo address
     */
    void setReplyTo(EndpointReference reference);

    /**
     * Get the To EPR
     * @return To epr of the message if available
     */
    EndpointReference getTo();

     /**
     * Set the To EPR
     * @param reference the To EPR
     */
    void setTo(EndpointReference reference);

    /**
     * Sets the WSAAction
     * @param actionURI the WSAAction
     */
    void setWSAAction(String actionURI);

    /**
     * Returns the WSAAction
     * @return the WSAAction
     */
    String getWSAAction();

    /**
     * Returns the SOAPAction of the message
     * @return the SOAPAction
     */
    String getSoapAction();

    /**
     * Set the SOAPAction
     * @param string the SOAP Action
     */
    void setSoapAction(String string);

    /**
     * Set the message
     * @param messageID message id to be set
     */
    void setWSAMessageID(String messageID);

    /**
     * Gets the message name
     * @return the WSA MessageID
     */
    String getWSAMessageID();

    /**
     * If this message using MTOM?
     * @return true if using MTOM
     */
    boolean isDoingMTOM();

    /**
     * If this message using SWA?
     * @return true if using SWA
     */
    boolean isDoingSWA();

    /**
     * Marks as using MTOM
     * @param b true to mark as using MTOM
     */
    void setDoingMTOM(boolean b);

    /**
     * Marks as using SWA
     * @param b true to mark as using SWA
     */
    void setDoingSWA(boolean b);

    /**
     * Is this message over POX?
     * @return true if over POX
     */
    boolean isDoingPOX();

    /**
     * Marks this message as over POX
     * @param b true to mark as POX
     */
    void setDoingPOX(boolean b);

    /**
     * Is this message over GET?
     * @return true if over GET
     */
    boolean isDoingGET();

    /**
     * Marks this message as over REST/GET
     * @param b true to mark as REST/GET
     */
    void setDoingGET(boolean b);

    /**
     * Is this message a SOAP 1.1 message?
     * @return true if this is a SOAP 1.1 message
     */
    boolean isSOAP11();

    /**
     * Mark this message as a response or not.
     * @see org.apache.synapse.MessageContext#isResponse()
     * @param b true to set this as a response
     */
    void setResponse(boolean b);

    /**
     * Is this message a response to a synchronous message sent out through Synapse?
     * @return true if this message is a response message
     */
    boolean isResponse();

    /**
     * Marks this message as a fault response
     * @see org.apache.synapse.MessageContext#isFaultResponse()
     * @param b true to mark this as a fault response
     */
    void setFaultResponse(boolean b);

    /**
     * Is this message a response to a fault message?
     * @return true if this is a response to a fault message
     */
    boolean isFaultResponse();

    Stack<FaultHandler> getFaultStack();

    void pushFaultHandler(FaultHandler fault);

    /**
     * Return the service level Log for this message context or null
     * @return the service level Log for the message
     */
    Log getServiceLog();

    void setServiceLog(Log serviceLog);

    boolean send();

    //TODO: what's with the SequenceMediator argument?
    void sendAsync(SequenceMediator seq);
}
