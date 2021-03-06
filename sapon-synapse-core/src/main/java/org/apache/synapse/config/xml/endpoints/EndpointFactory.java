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

package org.apache.synapse.config.xml.endpoints;

import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.XMLToObjectMapper;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.IndirectEndpoint;

/**
 * All endpoint factories should extend from this abstract class. Use
 * EndpointFactory to obtain the correct endpoint for particular endpoint
 * configuration. As endpoints can be nested inside each other, EndpointFactory
 * implementations may call other EndpointFactory implementations recursively
 * to obtain the required endpoint hierarchy.
 * <p/>
 * This also serves as the {@link XMLToObjectMapper} implementation for
 * specific endpoint implementations. If the endpoint type is not known use
 * {@link XMLToEndpointMapper} as the generic {@link XMLToObjectMapper} for all
 * endpoints.
 */
public abstract class EndpointFactory implements XMLToObjectMapper {

    protected static Log log;

    protected EndpointFactory() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * Core method which is exposed for the external use, and this will find
     * the proper {@link EndpointFactory} and create the endpoint which is of
     * the format {@link Endpoint}.
     *
     * @param elem        XML from which the endpoint will be built
     * @param isAnonymous whether this is an anonymous endpoint or not
     * @return created endpoint
     */
    public static Endpoint getEndpointFromElement(OMElement elem, boolean isAnonymous) {
        return getEndpointFactory(elem).createEndpoint(elem, isAnonymous);
    }

    /**
     * Creates the {@link Endpoint} object from the provided {@link OMNode}
     *
     * @param om XML node from which the endpoint will be built
     * @return created endpoint as an {@link Object}
     */
    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createEndpoint((OMElement) om, false);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    /**
     * Creates the Endpoint implementation for the given XML endpoint configuration. If the endpoint
     * configuration is an inline one, it should be an anonymous endpoint. If it is defined as an
     * immediate child element of the definitions tag it should have a name, which is used as the
     * key in local registry.
     *
     * @param epConfig          OMElement conatining the endpoint configuration.
     * @param anonymousEndpoint false if the endpoint has a name. true otherwise.
     * @return Endpoint implementation for the given configuration.
     */
    protected abstract Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint);

    private void extractTrace(EndpointDefinition def, OMElement elem) {
        OMAttribute trace
        	= elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.TRACE_ATTRIB_NAME));
        if (trace != null && trace.getAttributeValue() != null) {
        	String traceValue = trace.getAttributeValue();
        	if (XMLConfigConstants.TRACE_ENABLE.equals(traceValue)) {
        		def.setTraceState(SynapseConstants.TRACING_ON);
        	} else if (XMLConfigConstants.TRACE_DISABLE.equals(traceValue)) {
        		def.setTraceState(SynapseConstants.TRACING_OFF);
        	}
        } else {
        	def.setTraceState(SynapseConstants.TRACING_UNSET);
        }
    }

    private void extractOptimize(EndpointDefinition def, OMElement elem) {
    	OMAttribute optimize
        	= elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "optimize"));
        if (optimize != null && optimize.getAttributeValue().length() > 0) {
            String method = optimize.getAttributeValue().trim();
            if ("mtom".equalsIgnoreCase(method)) {
                def.setUseMTOM(true);
            } else if ("swa".equalsIgnoreCase(method)) {
                def.setUseSwa(true);
            }
        }
    }

    private void extractEncoding(EndpointDefinition def, OMElement elem) {
        OMAttribute encoding
        	= elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "encoding"));
        if (encoding != null && encoding.getAttributeValue() != null) {
            def.setCharSetEncoding(encoding.getAttributeValue());
        }
    }

    private void extractAddressing(EndpointDefinition def, OMElement elem) {
    	OMElement wsAddr = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableAddressing"));
        if (wsAddr != null) {

            def.setAddressingOn(true);

            OMAttribute version = wsAddr.getAttribute(new QName("version"));
            if (version != null && version.getAttributeValue() != null) {
                String versionValue = version.getAttributeValue().trim().toLowerCase();
                if (SynapseConstants.ADDRESSING_VERSION_FINAL.equals(versionValue) ||
                        SynapseConstants.ADDRESSING_VERSION_SUBMISSION.equals(versionValue)) {
                    def.setAddressingVersion(version.getAttributeValue());
                } else {
                    handleException("Unknown value for the addressing version. Possible values " +
                            "for the addressing version are 'final' and 'submission' only.");
                }
            }

            String useSepList = wsAddr.getAttributeValue(new QName("separateListener"));
            if (useSepList != null) {
                if ("true".equals(useSepList.trim().toLowerCase())) {
                    def.setUseSeparateListener(true);
                }
            }
        }
    }

    private void extractSecurity(EndpointDefinition def, OMElement elem) {
    	OMElement wsSec = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableSec"));
        if (wsSec != null) {

            def.setSecurityOn(true);

            OMAttribute policyKey      = wsSec.getAttribute(
                    new QName(XMLConfigConstants.NULL_NAMESPACE, "policy"));
            OMAttribute inboundPolicyKey  = wsSec.getAttribute(
                    new QName(XMLConfigConstants.NULL_NAMESPACE, "inboundPolicy"));
            OMAttribute outboundPolicyKey = wsSec.getAttribute(
                    new QName(XMLConfigConstants.NULL_NAMESPACE, "outboundPolicy"));

            if (policyKey != null && policyKey.getAttributeValue() != null) {
                def.setWsSecPolicyKey(policyKey.getAttributeValue());
            } else {
                if (inboundPolicyKey != null && inboundPolicyKey.getAttributeValue() != null) {
                    def.setInboundWsSecPolicyKey(inboundPolicyKey.getAttributeValue());
                }
                if (outboundPolicyKey != null && outboundPolicyKey.getAttributeValue() != null) {
                    def.setOutboundWsSecPolicyKey(outboundPolicyKey.getAttributeValue());
                }
            }
        }
    }

    private void extractRM(EndpointDefinition def, OMElement elem) {
    	OMElement wsRm = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableRM"));
        if (wsRm != null) {

            def.setReliableMessagingOn(true);

            OMAttribute policy
                    = wsRm.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                def.setWsRMPolicyKey(policy.getAttributeValue());
            }
        }
    }

    private void extractTimeout(EndpointDefinition def, OMElement elem) {
        // set the timeout configuration
        OMElement timeout = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "timeout"));
        if (timeout != null) {
            OMElement duration = timeout.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "duration"));

            if (duration != null) {
                String d = duration.getText();
                if (d != null) {
                    try {
                        long timeoutMilliSeconds = Long.parseLong(d.trim());
                        def.setTimeoutDuration(timeoutMilliSeconds);
                    } catch (NumberFormatException e) {
                        handleException("Endpoint timeout duration expected as a " +
                                "number but was not a number");
                    }
                }
            }

            OMElement action = timeout.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "action"));
            if (action != null && action.getText() != null) {
                String actionString = action.getText();
                if ("discard".equalsIgnoreCase(actionString.trim())) {

                    def.setTimeoutAction(SynapseConstants.DISCARD);

                    // set timeout duration to 30 seconds, if it is not set explicitly
                    if (def.getTimeoutDuration() == 0) {
                        def.setTimeoutDuration(30000);
                    }
                } else if ("fault".equalsIgnoreCase(actionString.trim())) {

                    def.setTimeoutAction(SynapseConstants.DISCARD_AND_FAULT);

                    // set timeout duration to 30 seconds, if it is not set explicitly
                    if (def.getTimeoutDuration() == 0) {
                        def.setTimeoutDuration(30000);
                    }
                } else {
                    handleException("Invalid timeout action, action : "
                            + actionString + " is not supported");
                }
            }
        }
    }

    private void extractMarkForSuspension(EndpointDefinition def, OMElement elem) {
    	OMElement markAsTimedout = elem.getFirstChildWithName(new QName(
    			SynapseConstants.SYNAPSE_NAMESPACE,
    			XMLConfigConstants.MARK_FOR_SUSPENSION));

    	if (markAsTimedout != null) {

    		OMElement timeoutCodes = markAsTimedout.getFirstChildWithName(new QName(
    				SynapseConstants.SYNAPSE_NAMESPACE,
    				XMLConfigConstants.ERROR_CODES));
    		if (timeoutCodes != null && timeoutCodes.getText() != null) {
    			StringTokenizer st = new StringTokenizer(timeoutCodes.getText().trim(), ", ");
    			while (st.hasMoreTokens()) {
    				String s = st.nextToken();
    				try {
    					def.addTimeoutErrorCode(Integer.parseInt(s));
    				} catch (NumberFormatException e) {
    					handleException("The timeout error codes should be specified " +
    							"as valid numbers separated by commas : " + timeoutCodes.getText(), e);
    				}
    			}
    		}

    		OMElement retriesBeforeSuspend = markAsTimedout.getFirstChildWithName(new QName(
    				SynapseConstants.SYNAPSE_NAMESPACE,
    				XMLConfigConstants.RETRIES_BEFORE_SUSPENSION));
    		if (retriesBeforeSuspend != null && retriesBeforeSuspend.getText() != null) {
    			try {
    				def.setRetriesOnTimeoutBeforeSuspend(
    						Integer.parseInt(retriesBeforeSuspend.getText().trim()));
    			} catch (NumberFormatException e) {
    				handleException("The retries before suspend [for timeouts] should be " +
    						"specified as a valid number : " + retriesBeforeSuspend.getText(), e);
    			}
    		}

    		OMElement retryDelay = markAsTimedout.getFirstChildWithName(new QName(
    				SynapseConstants.SYNAPSE_NAMESPACE,
    				XMLConfigConstants.RETRY_DELAY));
    		if (retryDelay != null && retryDelay.getText() != null) {
    			try {
    				def.setRetryDurationOnTimeout(
    						Integer.parseInt(retryDelay.getText().trim()));
    			} catch (NumberFormatException e) {
    				handleException("The retry delay for timeouts should be specified " +
    						"as a valid number : " + retryDelay.getText(), e);
    			}
    		}
    	}
    }

    private void extractSuspendDuration(EndpointDefinition def, OMElement elem) {
        // support backwards compatibility with Synapse 1.2 - for suspendDurationOnFailure
        OMElement suspendDurationOnFailure = elem.getFirstChildWithName(new QName(
            SynapseConstants.SYNAPSE_NAMESPACE, "suspendDurationOnFailure"));
        if (suspendDurationOnFailure != null && suspendDurationOnFailure.getText() != null) {

            log.warn("Configuration uses deprecated style for endpoint 'suspendDurationOnFailure'");
            try {
                def.setInitialSuspendDuration(
                        1000 * Long.parseLong(suspendDurationOnFailure.getText().trim()));
                def.setSuspendProgressionFactor((float) 1.0);
            } catch (NumberFormatException e) {
                handleException("The initial suspend duration should be specified " +
                    "as a valid number : " + suspendDurationOnFailure.getText(), e);
            }
        }
    }

    private void extractSuspendOnFail(EndpointDefinition def, OMElement elem) {
    	OMElement suspendOnFailure = elem.getFirstChildWithName(new QName(
    			SynapseConstants.SYNAPSE_NAMESPACE,
    			XMLConfigConstants.SUSPEND_ON_FAILURE));

    	if (suspendOnFailure != null) {

    		OMElement suspendCodes = suspendOnFailure.getFirstChildWithName(new QName(
    				SynapseConstants.SYNAPSE_NAMESPACE,
    				XMLConfigConstants.ERROR_CODES));
    		if (suspendCodes != null && suspendCodes.getText() != null) {

    			StringTokenizer st = new StringTokenizer(suspendCodes.getText().trim(), ", ");
    			while (st.hasMoreTokens()) {
    				String s = st.nextToken();
    				try {
    					def.addSuspendErrorCode(Integer.parseInt(s));
    				} catch (NumberFormatException e) {
    					handleException("The suspend error codes should be specified " +
    							"as valid numbers separated by commas : " + suspendCodes.getText(), e);
    				}
    			}
    		}

    		OMElement initialDuration = suspendOnFailure.getFirstChildWithName(new QName(
    				SynapseConstants.SYNAPSE_NAMESPACE,
    				XMLConfigConstants.SUSPEND_INITIAL_DURATION));
    		if (initialDuration != null && initialDuration.getText() != null) {
    			try {
    				def.setInitialSuspendDuration(
    						Integer.parseInt(initialDuration.getText().trim()));
    			} catch (NumberFormatException e) {
    				handleException("The initial suspend duration should be specified " +
    						"as a valid number : " + initialDuration.getText(), e);
    			}
    		}

    		OMElement progressionFactor = suspendOnFailure.getFirstChildWithName(new QName(
    				SynapseConstants.SYNAPSE_NAMESPACE,
    				XMLConfigConstants.SUSPEND_PROGRESSION_FACTOR));
    		if (progressionFactor != null && progressionFactor.getText() != null) {
    			try {
    				def.setSuspendProgressionFactor(
    						Float.parseFloat(progressionFactor.getText().trim()));
    			} catch (NumberFormatException e) {
    				handleException("The suspend duration progression factor should be specified " +
    						"as a valid float : " + progressionFactor.getText(), e);
    			}
    		}

    		OMElement maximumDuration = suspendOnFailure.getFirstChildWithName(new QName(
    				SynapseConstants.SYNAPSE_NAMESPACE,
    				XMLConfigConstants.SUSPEND_MAXIMUM_DURATION));
    		if (maximumDuration != null && maximumDuration.getText() != null) {
    			try {
    				def.setSuspendMaximumDuration(
    						Long.parseLong(maximumDuration.getText().trim()));
    			} catch (NumberFormatException e) {
    				handleException("The maximum suspend duration should be specified " +
    						"as a valid number : " + maximumDuration.getText(), e);
    			}
    		}
    	}
    }

    /**
     * Extracts the QoS information from the XML which represents a WSDL/Address/Default endpoints
     *
     * @param def to be filled with the extracted information
     * @param elem       XML which represents the endpoint with QoS information
     */
    protected void extractCommonEndpointProperties(EndpointDefinition def, OMElement elem) {
        extractTrace(def, elem);
        extractOptimize(def, elem);
        extractEncoding(def, elem);
        extractAddressing(def, elem);
        extractSecurity(def, elem);
        extractRM(def, elem);
        extractTimeout(def, elem);
        extractMarkForSuspension(def, elem);
        extractSuspendDuration(def, elem);
        extractSuspendOnFail(def, elem);
    }

    protected void extractSpecificEndpointProperties(EndpointDefinition definition,
        OMElement elem) {

        // overridden by the Factories which has specific building
    }

    /**
     * Returns the EndpointFactory implementation for given endpoint configuration. Throws a
     * SynapseException, if there is no EndpointFactory for given configuration.
     *
     * @param configElement Endpoint configuration.
     * @return EndpointFactory implementation.
     */
    private static EndpointFactory getEndpointFactory(OMElement configElement) {

        if (configElement.getAttribute(new QName("key")) != null) {
            return IndirectEndpointFactory.getInstance();
        }

        if (configElement.getAttribute(new QName("key-expression")) != null) {
            return ResolvingEndpointFactory.getInstance();
        }

        OMElement addressElement = configElement.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "address"));
        if (addressElement != null) {
            return AddressEndpointFactory.getInstance();
        }

        OMElement wsdlElement = configElement.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "wsdl"));
        if (wsdlElement != null) {
            return WSDLEndpointFactory.getInstance();
        }

        OMElement defaultElement = configElement.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "default"));
        if (defaultElement != null) {
            return DefaultEndpointFactory.getInstance();
        }

        OMElement lbElement = configElement.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "loadbalance"));
        if (lbElement != null) {
            OMElement sessionElement = configElement.
                    getFirstChildWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, "session"));
            if (sessionElement != null) {
                return SALoadbalanceEndpointFactory.getInstance();
            } else {
                return LoadbalanceEndpointFactory.getInstance();
            }
        }

        OMElement dlbElement = configElement.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "dynamicLoadbalance"));
        if (dlbElement != null) {
            //TODO: Handle Session affinitiy & failover
            return DynamicLoadbalanceEndpointFactory.getInstance();
        }

        OMElement foElement = configElement.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "failover"));
        if (foElement != null) {
            return FailoverEndpointFactory.getInstance();
        }

        OMElement mElement = configElement.getFirstChildWithName
        (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "mapping"));
        if (mElement != null) {
        	return MappingEndpointFactory.getInstance();
        }

        handleException("Invalid endpoint configuration.");
        // just to make the compiler happy : never executes
        return null;
    }

    /**
     * Helper method to construct children endpoints
     *
     * @param listEndpointElement OMElement representing  the children endpoints
     * @param parent              Parent endpoint
     * @return List of children endpoints
     */
    protected ArrayList<Endpoint> getEndpoints(OMElement listEndpointElement, Endpoint parent) {

        ArrayList<Endpoint> endpoints = new ArrayList<Endpoint>();
        ArrayList<String> keys = new ArrayList<String>();
        for(OMElement endptElem: listEndpointElement.getChildrenWithName(XMLConfigConstants.ENDPOINT_ELT)) {
            Endpoint endpoint = EndpointFactory.getEndpointFromElement(endptElem, true);
            if (endpoint instanceof IndirectEndpoint) {
                String key = ((IndirectEndpoint) endpoint).getKey();
                if (!keys.contains(key)) {
                    keys.add(key);
                } else {
                    handleException("Same endpoint definition cannot be used with in the siblings");
                }
            }
            endpoint.setParentEndpoint(parent);
            endpoints.add(endpoint);
        }

        return endpoints;
    }

    protected static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    protected static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
