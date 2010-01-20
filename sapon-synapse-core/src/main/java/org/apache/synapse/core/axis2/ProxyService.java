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

package org.apache.synapse.core.axis2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.MTOMtoSwABuilder;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.OutOnlyAxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.WSDL11ToAxisServiceBuilder;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.description.WSDLToAxisServiceBuilder;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.policy.BindingPolicyInfo;
import org.apache.synapse.core.axis2.policy.MessagePolicyInfo;
import org.apache.synapse.core.axis2.policy.OperationPolicyInfo;
import org.apache.synapse.core.axis2.policy.PolicyInfo;
import org.apache.synapse.core.axis2.policy.ServicePolicyInfo;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.util.resolver.CustomWSDLLocator;
import org.apache.synapse.util.resolver.CustomXmlSchemaURIResolver;
import org.apache.synapse.util.resolver.ResourceMap;
import org.xml.sax.InputSource;

/**
 * <proxy-service name="string" [transports="(http |https |jms )+|all"] [trace="enable|disable"]>
 *    <description>..</description>?
 *    <target [inSequence="name"] [outSequence="name"] [faultSequence="name"] [endpoint="name"]>
 *       <endpoint>...</endpoint>
 *       <inSequence>...</inSequence>
 *       <outSequence>...</outSequence>
 *       <faultSequence>...</faultSequence>
 *    </target>?
 *    <publishWSDL uri=".." key="string">
 *       <wsdl:definition>...</wsdl:definition>?
 *       <resource location="..." key="..."/>*
 *    </publishWSDL>?
 *    <enableSec/>?
 *    <enableRM/>?
 *    <policy key="string" [type=("in" |"out")] [operationName="string"]
 *      [operationNamespace="string"]>?
 *       // optional service parameters
 *    <parameter name="string">
 *       text | xml
 *    </parameter>?
 * </proxy-service>
 */
public class ProxyService implements AspectConfigurable {

    private static final Log log = LogFactory.getLog(ProxyService.class);
    //private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);
    private final Log serviceLog;

    public static final String ABSOLUTE_SCHEMA_URL_PARAM = "showAbsoluteSchemaURL";

    /**
     * The name of the proxy service
     */
    private String name;
    /**
     * The proxy service description. This could be optional informative text about the service
     */
    private String description;
    /**
     * The transport/s over which this service should be exposed, or defaults to all available
     */
    private List<String> transports;
    /**
     * Server names for which this service should be exposed
     */
    private List<String> pinnedServers = new ArrayList<String>();
    /**
     * The target endpoint key
     */
    private String targetEndpoint = null;
    /**
     * The target inSequence key
     */
    private String targetInSequence = null;
    /**
     * The target outSequence key
     */
    private String targetOutSequence = null;
    /**
     * The target faultSequence key
     */
    private String targetFaultSequence = null;
    /**
     * The inlined definition of the target endpoint, if defined
     */
    private Endpoint targetInLineEndpoint = null;
    /**
     * The inlined definition of the target in-sequence, if defined
     */
    private SequenceMediator targetInLineInSequence = null;
    /**
     * The inlined definition of the target out-sequence, if defined
     */
    private SequenceMediator targetInLineOutSequence = null;
    /**
     * The inlined definition of the target fault-sequence, if defined
     */
    private SequenceMediator targetInLineFaultSequence = null;
    /**
     * A list of any service parameters (e.g. JMS parameters etc)
     */
    private final Map<String, Object> parameters = new HashMap<String, Object>();
    /**
     * The key for the base WSDL
     */
    private String wsdlKey;
    /**
     * The URI for the base WSDL, if defined as a URL
     */
    private URI wsdlURI;
    /**
     * The inlined representation of the service WSDL, if defined inline
     */
    private Object inLineWSDL;
    /**
     * A ResourceMap object allowing to locate artifacts (WSDL and XSD) imported
     * by the service WSDL to be located in the registry.
     */
    private ResourceMap resourceMap;
    /**
     * Policies to be set to the service, this can include service level, operation level,
     * message level or hybrid level policies as well.
     */
    private List<PolicyInfo> policies = new ArrayList<PolicyInfo>();
    /**
     * The keys for any supplied policies that would apply at the service level
     */
    private final List<String> serviceLevelPolicies = new ArrayList<String>();
    /**
     * The keys for any supplied policies that would apply at the in message level
     */
    private List<String> inMessagePolicies = new ArrayList<String>();
    /**
     * The keys for any supplied policies that would apply at the out message level
     */
    private List<String> outMessagePolicies = new ArrayList<String>();
    /**
     * Should WS Addressing be engaged on this service
     */
    private boolean wsAddrEnabled = false;
    /**
     * Should WS RM be engaged on this service
     */
    private boolean wsRMEnabled = false;
    /**
     * Should WS Sec be engaged on this service
     */
    private boolean wsSecEnabled = false;
    /**
     * Should this service be started by default on initialization?
     */
    private boolean startOnLoad = true;
    /**
     * Is this service running now?
     */
    private boolean running = false;

    private Set<QName> ensuredSwAElems = new HashSet<QName>();

    public static final String ALL_TRANSPORTS = "all";
    public static final String BINDING_POLICY = ProxyService.class.getCanonicalName() + ".BINDING_POLICY";

    /**
     * The variable that indicate tracing on or off for the current mediator
     */
    private int traceState = SynapseConstants.TRACING_UNSET;

    private AspectConfiguration aspectConfiguration;

    private String fileName;

    /**
     * Constructor
     *
     * @param name the name of the Proxy service
     */
    public ProxyService(String name) {
        this.name = name;
        serviceLog = LogFactory.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX + name);
    }

    private OMElement findWSDLElement(SynapseConfiguration synCfg)
    {
        if (wsdlKey != null) {
            synCfg.getEntryDefinition(wsdlKey);
            final Object keyObject = synCfg.getEntry(wsdlKey);
            if (keyObject instanceof OMElement) {
                return (OMElement) keyObject;
            }
        }

        if (inLineWSDL != null) {
            return (OMElement) inLineWSDL;
        }

        if (wsdlURI != null) {
        	try {
        		final String publishWSDL = wsdlURI.toURL().toString();
                final OMElement element
                	= (OMElement)SynapseConfigUtils.getOMElementFromURL(publishWSDL);
               	return element;
        	} catch (MalformedURLException e) {
        		log.warn("Malformed URI for wsdl", e);
        		return null;
            } catch (IllegalArgumentException e) {
            	log.warn("URL is not absolute", e);
            	return null;
        	} catch (Exception e) {
        		log.warn("Problems loading WSDL", e);
        		return null;
        	}
        }
        log.warn("Couldn't find a WSDL by the typical means");
        return null;
    }

    private AxisService publishWSDLSafeMode() {
    	// this is if the wsdl cannot be loaded... create a dummy service and an operation for which
        // our SynapseDispatcher will properly dispatch to
        final AxisService proxyService = new AxisService();
        AxisOperation mediateOperation = new InOutAxisOperation(new QName("mediate"));
        proxyService.addOperation(mediateOperation);
        return proxyService;
    }

    private boolean enablePublishWSDLSafeMode() {
        final Map<String, Object> proxyParameters = getParameterMap();
        if (!proxyParameters.isEmpty()) {
        	final Object param = proxyParameters.get("enablePublishWSDLSafeMode");
        	if (param != null) {
                return Boolean.parseBoolean(param.toString().toLowerCase());
            }
        }
        return false;
    }

    private AxisService handleNoWSDL() {
        //handleException("Error reading from wsdl URI", e);
        if(enablePublishWSDLSafeMode()) {
            //!!!Need to add a reload function... And display that the wsdl/service is offline!!!
            if (log.isTraceEnabled()) {
                log.trace("WSDL was unable to load for: " + wsdlURI);
                log.trace("enableURISafeMode: true");
            }
            return publishWSDLSafeMode();
        } else {
        	if (log.isTraceEnabled()) {
        		log.trace("WSDL was unable to load for: " + wsdlURI);
        		log.trace("Please add <syn:parameter name=\"enableURISafeMode\">true" +
        			"</syn:parameter> to proxy service.");
            }
        	return null;
        }
    }

    private AxisService buildProxyService(OMElement wsdlElement, SynapseConfiguration synCfg) {

    	OMNamespace wsdlNamespace = wsdlElement.getNamespace();

    	// serialize and create an inputstream to read WSDL
    	InputStream wsdlInputStream = null;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	try {
    		if (log.isTraceEnabled()) {
    			log.trace("Serializing wsdlElement found to build an Axis2 service");
    		}
    		wsdlElement.serialize(baos);
    		wsdlInputStream = new ByteArrayInputStream(baos.toByteArray());
    	} catch (XMLStreamException e) {
    		handleException("Error converting to a StreamSource", e);
    	}

    	if (wsdlInputStream != null) {
    		try {
    			// detect version of the WSDL 1.1 or 2.0
    			if (log.isTraceEnabled()) {
    				log.trace("WSDL Namespace is : "
    					+ wsdlNamespace.getNamespaceURI());
    			}

    			if (wsdlNamespace != null) {
    				WSDLToAxisServiceBuilder wsdlToAxisServiceBuilder = null;

    				if (WSDL2Constants.WSDL_NAMESPACE.equals(wsdlNamespace.getNamespaceURI())) {
    					handleException("WSDL 2.0 is not currently supported");
//    					wsdlToAxisServiceBuilder =
//    						new WSDL20ToAxisServiceBuilder(wsdlInputStream, null, null);

    				} else if (org.apache.axis2.namespace.Constants.NS_URI_WSDL11.
    						equals(wsdlNamespace.getNamespaceURI())) {
    					wsdlToAxisServiceBuilder =
    						new WSDL11ToAxisServiceBuilder(wsdlInputStream);
    				} else {
    					handleException("Unknown WSDL format.. not WSDL 1.1 or WSDL 2.0");
    				}

    				if (wsdlToAxisServiceBuilder == null) {
    					throw new SynapseException(
    							"Could not get the WSDL to Axis Service Builder");
    				}

    				wsdlToAxisServiceBuilder.setBaseUri(
    						wsdlURI != null ? wsdlURI.toString() :
    							SynapseConfigUtils.getSynapseHome());

    				if (log.isTraceEnabled()) {
    					log.trace("Setting up custom resolvers");
    				}
    				// Set up the URIResolver

    				if (resourceMap != null) {
    					// if the resource map is available use it
    					wsdlToAxisServiceBuilder.setCustomResolver(
    							new CustomXmlSchemaURIResolver(resourceMap, synCfg));
    					// Axis 2 also needs a WSDLLocator for WSDL 1.1 documents
    					if (wsdlToAxisServiceBuilder instanceof WSDL11ToAxisServiceBuilder) {
    						((WSDL11ToAxisServiceBuilder)
    								wsdlToAxisServiceBuilder).setCustomWSDLResolver(
    										new CustomWSDLLocator(new InputSource(wsdlInputStream),
    												wsdlURI != null ? wsdlURI.toString() : "",
    														resourceMap, synCfg));
    					}
    				} else {
    					//if the resource map isn't available ,
    					//then each import URIs will be resolved using base URI
    					wsdlToAxisServiceBuilder.setCustomResolver(
    							new CustomXmlSchemaURIResolver());
    					// Axis 2 also needs a WSDLLocator for WSDL 1.1 documents
    					if (wsdlToAxisServiceBuilder instanceof WSDL11ToAxisServiceBuilder) {
    						((WSDL11ToAxisServiceBuilder)
    								wsdlToAxisServiceBuilder).setCustomWSDLResolver(
    										new CustomWSDLLocator(new InputSource(wsdlInputStream),
    												wsdlURI != null ? wsdlURI.toString() : ""));
    					}
    				}

    				if (log.isTraceEnabled()) {
    					log.trace("Populating Axis2 service using WSDL");
    					log.trace("WSDL : " + wsdlElement.toString());
    				}
    				final AxisService proxyService
    					= wsdlToAxisServiceBuilder.populateService();

    				// this is to clear the bindinigs and ports already in the WSDL so that the
    				// service will generate the bindings on calling the printWSDL otherwise
    				// the WSDL which will be shown is same as the original WSDL except for the
    				// service name
    				proxyService.getEndpoints().clear();
    				return proxyService;
    			} else {
    				handleException("Unknown WSDL format.. not WSDL 1.1 or WSDL 2.0");
    			}

    		} catch (AxisFault af) {
    			handleException("Error building service from WSDL", af);
    		}
    	}

    	return null;
    }

    /**
     * Build the underlying Axis2 service from the Proxy service definition
     *
     * @param synCfg  the Synapse configuration
     * @param axisCfg the Axis2 configuration
     * @return the Axis2 service for the Proxy
     */
    public AxisService buildAxisService(SynapseEnvironment synEnv, AxisConfiguration axisCfg)
    {
    	SynapseConfiguration synCfg = synEnv.getSynapseConfiguration();
        auditInfo("Building Axis service for Proxy service : " + name);
        final AxisService proxyService;

        // get the wsdlElement as an OMElement
        if (log.isTraceEnabled()) {
            log.trace("Loading the WSDL : " +
                (wsdlKey != null ? " key = " + wsdlKey :
                (wsdlURI != null ? " URI = " + wsdlURI : " <Inlined>")));
        }

        final OMElement wsdlElement = findWSDLElement(synCfg);
        if(wsdlElement == null) {
        	proxyService = handleNoWSDL();
        } else {
        	proxyService = buildProxyService(wsdlElement, synCfg);
        }

        // Set the name and description. Currently Axis2 uses the name as the
        // default Service destination
        if (proxyService == null) {
        	throw new SynapseException("Could not create a proxy service");
        }
        proxyService.setName(name);
        if (description != null) {
            proxyService.setDocumentation(description);
        }

        // process transports and expose over requested transports. If none
        // is specified, default to all transports using service name as
        // destination
        if (transports == null || transports.size() == 0) {
            // default to all transports using service name as destination
        } else {
            if (log.isTraceEnabled()) {
				log.trace("Exposing transports : " + transports);
			}
            proxyService.setExposedTransports(transports);
        }

        // process parameters
        if (log.isTraceEnabled() && parameters.size() > 0) {
            log.trace("Setting service parameters : " + parameters);
        }
        for (Object o : parameters.keySet()) {
            String name = (String) o;
            Object value = parameters.get(name);

            Parameter p = new Parameter(name, value);

            try {
                proxyService.addParameter(p);
            } catch (AxisFault af) {
                handleException("Error setting parameter : " + name + "" +
                    "to proxy service as a Parameter", af);
            }
        }

        for (PolicyInfo pi : policies) {
        	if (pi instanceof ServicePolicyInfo) {
        		proxyService.attachPolicy(
        				getPolicyFromKey(pi.getPolicyKey(), synCfg));
        	}
        	else if (pi instanceof OperationPolicyInfo) {
        		OperationPolicyInfo opi = (OperationPolicyInfo)pi;
        		AxisOperation op = proxyService.getOperation(opi.getOperation());
        		if (op != null) {
        			op.attachPolicy(
        					getPolicyFromKey(pi.getPolicyKey(), synCfg));
        		} else {
        			handleException("Couldn't find the operation specified " +
        					"by the QName : " + opi.getOperation());
        		}
        	} else if (pi instanceof MessagePolicyInfo) {
        		MessagePolicyInfo mpi = (MessagePolicyInfo)pi;
        		if (mpi.getOperation() != null) {
        			AxisOperation op = proxyService.getOperation(mpi.getOperation());
        			if (op != null) {
        				op.getMessage(mpi.getMessageLabel()).attachPolicy(
        						getPolicyFromKey(pi.getPolicyKey(), synCfg));
        			} else {
        				handleException("Couldn't find the operation " +
        						"specified by the QName : " + mpi.getOperation());
        			}
        		} else {
        			// operation is not specified and hence apply to all the applicable messages
        			for (AxisOperation op: proxyService.getOperations()) {
        				// check whether the policy is applicable
        				if (!(  (op instanceof OutOnlyAxisOperation && mpi.getType() == MessagePolicyInfo.MESSAGE_TYPE_IN)
        						||(op instanceof InOnlyAxisOperation && mpi.getType() == MessagePolicyInfo.MESSAGE_TYPE_OUT))) {

        					AxisMessage message = op.getMessage(mpi.getMessageLabel());
        					message.attachPolicy(
        							getPolicyFromKey(pi.getPolicyKey(), synCfg));
        				}
        			}
        		}
        	} else if (pi instanceof BindingPolicyInfo) {
        		Policy p = getPolicyFromKey(pi.getPolicyKey(), synCfg);
        		try {
					proxyService.attachPolicy(p);

					//We need to be able to pull out the policy later, when we
					//have access to the AxisEndpoints that are available for
					//binding.
					proxyService.addParameter(BINDING_POLICY, p);

					//Since we want the bindings to use policy references, and
					//the inclusion of policy references is dependent on being
					//able to retrieve the policy from the service by name,
					//we register the service here.
					proxyService.registerPolicy(p.getId(), p);

				} catch (AxisFault e) {
					log.error(e);
				}
        	}
        	else {
        		handleException("Undefined Policy type");
        	}
        }

        // create a custom message receiver for this proxy service
        ProxyServiceMessageReceiver msgRcvr = new ProxyServiceMessageReceiver(synEnv);
        msgRcvr.setName(name);
        msgRcvr.setProxy(this);

        for(final AxisOperation op: proxyService.getOperations()) {
            op.setMessageReceiver(msgRcvr);
        }

        try {
            proxyService.addParameter(
                    SynapseConstants.SERVICE_TYPE_PARAM_NAME, SynapseConstants.PROXY_SERVICE_TYPE);
            auditInfo("Adding service " + name + " to the Axis2 configuration");
            axisCfg.addService(proxyService);
            this.setRunning(true);
        } catch (AxisFault axisFault) {
            try {
                if (axisCfg.getService(proxyService.getName()) != null) {
                    if (log.isTraceEnabled()) {
                    	log.trace("Removing service " + name + " due to error : "
						    + axisFault.getMessage());
					}
                    axisCfg.removeService(proxyService.getName());
                }
            } catch (AxisFault ignore) {}
            handleException("Error adding Proxy service to the Axis2 engine", axisFault);
        }

		if(ensuredSwAElems.size() > 0) {
			AxisConfiguration axisConfig = proxyService.getConfiguration();
			Builder b = axisCfg.getMessageBuilder("application/xop+xml");
			if(!(b instanceof MTOMtoSwABuilder)) {
				b = new MTOMtoSwABuilder();
				axisConfig.addMessageBuilder("application/xop+xml", b);
			}
			MTOMtoSwABuilder swa = (MTOMtoSwABuilder)b;
			swa.addAll(ensuredSwAElems);
		}

        // should Addressing be engaged on this service?
        if (wsAddrEnabled) {
            auditInfo("WS-Addressing is enabled for service : " + name);
            try {
                proxyService.engageModule(axisCfg.getModule(
                    SynapseConstants.ADDRESSING_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS Addressing module on proxy service : " + name, axisFault);
            }
        }

        // should RM be engaged on this service?
        if (wsRMEnabled) {
            auditInfo("WS-Reliable messaging is enabled for service : " + name);
            try {
                proxyService.engageModule(axisCfg.getModule(
                    SynapseConstants.RM_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS RM module on proxy service : " + name, axisFault);
            }
        }

        // should Security be engaged on this service?
        if (wsSecEnabled) {
            auditInfo("WS-Security is enabled for service : " + name);
            try {
                proxyService.engageModule(axisCfg.getModule(
                    SynapseConstants.SECURITY_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS Sec module on proxy service : "
                        + name, axisFault);
            }
        }

        auditInfo("Successfully created the Axis2 service for Proxy service : " + name);
        return proxyService;
    }

    private Policy getPolicyFromKey(String key, SynapseConfiguration synCfg) {

        synCfg.getEntryDefinition(key);
        return PolicyEngine.getPolicy(
                SynapseConfigUtils.getStreamSource(synCfg.getEntry(key)).getInputStream());
    }

    /**
     * Start the proxy service
     * @param synCfg the synapse configuration
     */
    public void start(SynapseConfiguration synCfg) {
        AxisConfiguration axisConfig = synCfg.getAxisConfiguration();
        if (axisConfig != null) {

            Parameter param = axisConfig.getParameter(SynapseConstants.SYNAPSE_ENV);
            if (param != null && param.getValue() instanceof SynapseEnvironment)  {
                SynapseEnvironment env = (SynapseEnvironment) param.getValue();
                if (targetInLineInSequence != null) {
                    targetInLineInSequence.init(env);
                }
                if (targetInLineOutSequence != null) {
                    targetInLineOutSequence.init(env);
                }
                if (targetInLineFaultSequence != null) {
                    targetInLineFaultSequence.init(env);
                }
            } else {
                auditWarn("Unable to find the SynapseEnvironment. " +
                    "Components of the proxy service may not be initialized");
            }

            AxisService as = axisConfig.getServiceForActivation(this.getName());
            as.setActive(true);
            axisConfig.notifyObservers(AxisEvent.SERVICE_START, as);
            this.setRunning(true);
            auditInfo("Started the proxy service : " + name);
        } else {
            auditWarn("Unable to start proxy service : " + name +
                ". Couldn't access Axis configuration");
        }
    }

    /**
     * Stop the proxy service
     * @param synCfg the synapse configuration
     */
    public void stop(SynapseConfiguration synCfg) {
        AxisConfiguration axisConfig = synCfg.getAxisConfiguration();
        if (axisConfig != null) {

            if (targetInLineInSequence != null) {
                targetInLineInSequence.destroy();
            }
            if (targetInLineOutSequence != null) {
                targetInLineOutSequence.destroy();
            }
            if (targetInLineFaultSequence != null) {
                targetInLineFaultSequence.destroy();
            }

            try {
                AxisService as = axisConfig.getService(this.getName());
                if (as != null) {
                    as.setActive(false);
                    axisConfig.notifyObservers(AxisEvent.SERVICE_STOP, as);
                }
                this.setRunning(false);
                auditInfo("Stopped the proxy service : " + name);
            } catch (AxisFault axisFault) {
                handleException("Error stopping the proxy service : " + name, axisFault);
            }
        } else {
            auditWarn("Unable to stop proxy service : " + name +
                ". Couldn't access Axis configuration");
        }
    }

    private void handleException(String msg) {
        serviceLog.error(msg);
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void handleException(String msg, Exception e) {
        serviceLog.error(msg);
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Write to the general log, as well as any service specific logs the audit message at INFO
     * @param message the INFO level audit message
     */
    private void auditInfo(String message) {
        log.info(message);
        serviceLog.info(message);
        if (log.isTraceEnabled()) {
            log.trace(message);
        }
    }

    /**
     * Write to the general log, as well as any service specific logs the audit message at WARN
     * @param message the WARN level audit message
     */
    private void auditWarn(String message) {
        log.warn(message);
        serviceLog.warn(message);
        if (log.isTraceEnabled()) {
            log.trace(message);
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTransports() {
        return transports;
    }

    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    public Map<String, Object> getParameterMap() {
        return this.parameters;
    }

    public void setTransports(List<String> transports) {
        this.transports = transports;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public String getTargetInSequence() {
        return targetInSequence;
    }

    public void setTargetInSequence(String targetInSequence) {
        this.targetInSequence = targetInSequence;
    }

    public String getTargetOutSequence() {
        return targetOutSequence;
    }

    public void setTargetOutSequence(String targetOutSequence) {
        this.targetOutSequence = targetOutSequence;
    }

    public String getWSDLKey() {
        return wsdlKey;
    }

    public void setWSDLKey(String wsdlKey) {
        this.wsdlKey = wsdlKey;
    }

    public List<String> getServiceLevelPolicies() {
        return serviceLevelPolicies;
    }

    public void addServiceLevelPolicy(String serviceLevelPolicy) {
        this.serviceLevelPolicies.add(serviceLevelPolicy);
    }

    public boolean isWsAddrEnabled() {
        return wsAddrEnabled;
    }

    public void setWsAddrEnabled(boolean wsAddrEnabled) {
        this.wsAddrEnabled = wsAddrEnabled;
    }

    public boolean isWsRMEnabled() {
        return wsRMEnabled;
    }

    public void setWsRMEnabled(boolean wsRMEnabled) {
        this.wsRMEnabled = wsRMEnabled;
    }

    public boolean isWsSecEnabled() {
        return wsSecEnabled;
    }

    public void setWsSecEnabled(boolean wsSecEnabled) {
        this.wsSecEnabled = wsSecEnabled;
    }

    public boolean isStartOnLoad() {
        return startOnLoad;
    }

    public void setStartOnLoad(boolean startOnLoad) {
        this.startOnLoad = startOnLoad;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * Returns the int value that indicate the tracing state
     *
     * @return Returns the int value that indicate the tracing state
     */
    public int getTraceState() {
        return traceState;
    }

    /**
     * Set the tracing State variable
     *
     * @param traceState tracing state
     */
    public void setTraceState(int traceState) {
        this.traceState = traceState;
    }

    public String getTargetFaultSequence() {
        return targetFaultSequence;
    }

    public void setTargetFaultSequence(String targetFaultSequence) {
        this.targetFaultSequence = targetFaultSequence;
    }

    public Object getInLineWSDL() {
        return inLineWSDL;
    }

    public void setInLineWSDL(Object inLineWSDL) {
        this.inLineWSDL = inLineWSDL;
    }

    public URI getWsdlURI() {
        return wsdlURI;
    }

    public void setWsdlURI(URI wsdlURI) {
        this.wsdlURI = wsdlURI;
    }

    public Endpoint getTargetInLineEndpoint() {
        return targetInLineEndpoint;
    }

    public void setTargetInLineEndpoint(Endpoint targetInLineEndpoint) {
        this.targetInLineEndpoint = targetInLineEndpoint;
    }

    public SequenceMediator getTargetInLineInSequence() {
        return targetInLineInSequence;
    }

    public void setTargetInLineInSequence(SequenceMediator targetInLineInSequence) {
        this.targetInLineInSequence = targetInLineInSequence;
    }

    public SequenceMediator getTargetInLineOutSequence() {
        return targetInLineOutSequence;
    }

    public void setTargetInLineOutSequence(SequenceMediator targetInLineOutSequence) {
        this.targetInLineOutSequence = targetInLineOutSequence;
    }

    public SequenceMediator getTargetInLineFaultSequence() {
        return targetInLineFaultSequence;
    }

    public void setTargetInLineFaultSequence(SequenceMediator targetInLineFaultSequence) {
        this.targetInLineFaultSequence = targetInLineFaultSequence;
    }

    public List<String> getPinnedServers() {
        return pinnedServers;
    }

    public void setPinnedServers(List<String> pinnedServers) {
        this.pinnedServers = pinnedServers;
    }

    public ResourceMap getResourceMap() {
        return resourceMap;
    }

    public void setResourceMap(ResourceMap resourceMap) {
        this.resourceMap = resourceMap;
    }

    public List<String> getInMessagePolicies() {
        return inMessagePolicies;
    }

    public void setInMessagePolicies(List<String> inMessagePolicies) {
        this.inMessagePolicies = inMessagePolicies;
    }

    public void addInMessagePolicy(String messagePolicy) {
        this.inMessagePolicies.add(messagePolicy);
    }

    public List<String> getOutMessagePolicies() {
        return outMessagePolicies;
    }

    public void setOutMessagePolicies(List<String> outMessagePolicies) {
        this.outMessagePolicies = outMessagePolicies;
    }

    public void addOutMessagePolicy(String messagePolicy) {
        this.outMessagePolicies.add(messagePolicy);
    }

    public List<PolicyInfo> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyInfo> policies) {
        this.policies = policies;
    }

    public void addPolicyInfo(PolicyInfo pi) {
        this.policies.add(pi);
    }

    public void configure(AspectConfiguration aspectConfiguration) {
        this.aspectConfiguration = aspectConfiguration;
    }

    public AspectConfiguration getAspectConfiguration() {
        return aspectConfiguration;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void ensureSwA(QName qname) {
    	ensuredSwAElems.add(qname);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[ Proxy Service [ Name : ").append(name).append(" ] ]");
        return sb.toString();
    }
}
