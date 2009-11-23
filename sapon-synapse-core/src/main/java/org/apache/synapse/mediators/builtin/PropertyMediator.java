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

package org.apache.synapse.mediators.builtin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

/**
 * The property mediator would save(or remove) a named property as a local property of
 * the Synapse Message Context or as a property of the Axis2 Message Context or
 * as a Transport Header.
 * Properties set this way could be extracted through the XPath extension function
 * "synapse:get-property(scope,prop-name)"
 */

public class PropertyMediator extends AbstractMediator {

    /** The Name of the property  */
    private String name = null;
    /** The Value to be set  */
    private Object value = null;
    /** The data type of the value */
    private String type = null;
    /** The XML value to be set */
    private OMElement valueElement = null;
    /** The XPath expr. to get value  */
    private SynapseXPath expression = null;
    /** The scope for which decide properties where to go*/
    private String scope = null;
    /** The Action - set or remove */
    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;
    /** Set the property (ACTION_SET) or remove it (ACTION_REMOVE). Defaults to ACTION_SET */
    private int action = ACTION_SET;

    /**
     * Sets a property into the current (local) Synapse Context or into the Axis Message Context
     * or into Transports Header and removes above properties from the corresponding locations.
     *
     * @param synCtx the message context
     * @return true always
     */
    public boolean mediate(SynapseMessageContext synCtx) {

    	log.debug("Start : Property mediator");

    	if (log.isTraceEnabled()) {
    		log.trace("Message : " + synCtx.getEnvelope());
    	}

        if (action == ACTION_SET) {

            Object resultValue = getResultValue(synCtx);

            if (log.isDebugEnabled()) {
                log.debug("Setting property : " + name + " at scope : " +
                    (scope == null ? "default" : scope) + " to : " + resultValue + " (i.e. " +
                    (value != null ? "constant : " + value :
                          "result of expression : " + expression) + ")");
            }

            if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
                //Setting property into the  Synapse Context
                synCtx.setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)
                    && synCtx instanceof Axis2SynapseMessageContext) {
                //Setting property into the  Axis2 Message Context
                Axis2SynapseMessageContext axis2smc = (Axis2SynapseMessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_CLIENT.equals(scope)
                    && synCtx instanceof Axis2SynapseMessageContext) {
                //Setting property into the  Axis2 Message Context client options
                Axis2SynapseMessageContext axis2smc = (Axis2SynapseMessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.getOptions().setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)
                    && synCtx instanceof Axis2SynapseMessageContext) {
                //Setting Transport Headers
                Axis2SynapseMessageContext axis2smc = (Axis2SynapseMessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                Object headers = axis2MessageCtx.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                if (headers != null && headers instanceof Map) {
                    Map headersMap = (HashMap) headers;
                    headersMap.put(name, resultValue);
                }
                if (headers == null) {
                    Map headersMap = new HashMap();
                    headersMap.put(name, resultValue);
                    axis2MessageCtx.setProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                            headersMap);
                }
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Removing property : " + name +
                    " (scope:" + (scope == null ? "default" : scope) + ")");
            }

            if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
                //Removing property from the  Synapse Context
                Set pros = synCtx.getPropertyKeySet();
                if (pros != null) {
                    pros.remove(name);
                }

            } else if ((XMLConfigConstants.SCOPE_AXIS2.equals(scope) ||
                XMLConfigConstants.SCOPE_CLIENT.equals(scope))
                && synCtx instanceof Axis2SynapseMessageContext) {

                //Removing property from the Axis2 Message Context
                Axis2SynapseMessageContext axis2smc = (Axis2SynapseMessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.removeProperty(name);

            } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)
                    && synCtx instanceof Axis2SynapseMessageContext) {
                // Removing transport headers
                Axis2SynapseMessageContext axis2smc = (Axis2SynapseMessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                Object headers = axis2MessageCtx.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                if (headers != null && headers instanceof Map) {
                    Map headersMap = (HashMap) headers;
                    headersMap.remove(name);
                } else {
                    log.debug("No transport headers found for the message");
                }
            }
        }
        log.debug("End : Property mediator");
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(String value) {
        setValue(value, null);
    }

    /**
     * Set the value to be set by this property mediator and the data type
     * to be used when setting the value. Accepted type names are defined in
     * XMLConfigConstants.DATA_TYPES enumeration. Passing null as the type
     * implies that 'STRING' type should be used.
     *
     * @param value the value to be set as a string
     * @param type the type name
     */
    public void setValue(String value, String type) {
        this.type = type;
        // Convert the value into specified type
        this.value = convertValue(value, type);
    }

    @Override
	public String getType() {
        return type;
    }

    public OMElement getValueElement() {
        return valueElement;
    }

    public void setValueElement(OMElement valueElement) {
        this.valueElement = valueElement;
    }

    public SynapseXPath getExpression() {
        return expression;
    }

    public void setExpression(SynapseXPath expression) {
        setExpression(expression, null);
    }

    public void setExpression(SynapseXPath expression, String type) {
        this.expression = expression;
        // Save the type information for now
        // We need to convert the result of the expression into this type during mediation
        // A null type would imply 'STRING' type
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    private Object getResultValue(SynapseMessageContext synCtx) {
        if (value != null) {
            return value;
        } else if (valueElement != null) {
            return valueElement;
        } else {
        	return convertValue(expression.stringValueOf(synCtx), type);
        }
    }

    private Object convertValue(String value, String type) {
        if (type == null) {
            // If no type is set we simply return the string value
            return value;
        }

        try {
            XMLConfigConstants.DATA_TYPES dataType = XMLConfigConstants.DATA_TYPES.valueOf(type);
            switch (dataType) {
                case BOOLEAN    : return Boolean.parseBoolean(value);
                case DOUBLE     : return Double.parseDouble(value);
                case FLOAT      : return Float.parseFloat(value);
                case INTEGER    : return Integer.parseInt(value);
                case LONG       : return Long.parseLong(value);
                case OM         : return SynapseConfigUtils.stringToOM(value);
                case SHORT      : return Short.parseShort(value);
                default         : return value;
            }
        } catch (IllegalArgumentException e) {
            String msg = "Unknown type : " + type + " for the property mediator or the " +
                    "property value cannot be converted into the specified type.";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }
}
