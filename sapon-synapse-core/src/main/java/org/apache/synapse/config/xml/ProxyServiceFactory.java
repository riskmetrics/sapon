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

package org.apache.synapse.config.xml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.core.axis2.policy.BindingPolicyInfo;
import org.apache.synapse.core.axis2.policy.MessagePolicyInfo;
import org.apache.synapse.core.axis2.policy.OperationPolicyInfo;
import org.apache.synapse.core.axis2.policy.PolicyInfo;
import org.apache.synapse.core.axis2.policy.ServicePolicyInfo;

/**
 * Factory for {@link ProxyService} instances.
 * <p/>
 * Configuration syntax:
 * <pre>
 * &lt;proxy name="string" [transports="(http |https |jms )+|all"] [pinnedServers="(serverName )+" [trace="enable|disable"]&gt;
 *    &lt;description&gt;..&lt;/description&gt;?
 *    &lt;target [inSequence="name"] [outSequence="name"] [faultSequence="name"] [endpoint="name"]&gt;
 *       &lt;endpoint&gt;...&lt;/endpoint&gt;?
 *       &lt;inSequence&gt;...&lt;/inSequence&gt;?
 *       &lt;outSequence&gt;...&lt;/outSequence&gt;?
 *       &lt;faultSequence&gt;...&lt;/faultSequence&gt;?
 *    &lt;/target&gt;?
 *    &lt;publishWSDL uri=".." key="string"&gt;
 *       ( &lt;wsdl:definition&gt;...&lt;/wsdl:definition&gt; | &lt;wsdl20:description&gt;...&lt;/wsdl20:description&gt; )?
 *       &lt;resource location="..." key="..."/&gt;*
 *    &lt;/publishWSDL&gt;?
 *    &lt;ensureSwA&gt;?
 *       &lt;namespace name="string"&gt;*
 *          $lt;element name="string"&gt;*
 *    &lt;enableAddressing/&gt;?
 *    &lt;enableSec/&gt;?
 *    &lt;enableRM/&gt;?
 *    &lt;policy where=(binding | service) key="string"/&gt;?
 *    &lt;policy where=(operation) key="string" type=(in | out)/&gt;?
 *    &lt;policy where=(message) key="string" type=(in | out)/&gt;?
 *       // optional service parameters
 *    &lt;parameter name="string"&gt;
 *       text | xml
 *    &lt;/parameter&gt;?
 * &lt;/proxy&gt;
 * </pre>
 */
public class ProxyServiceFactory {

    private static final Log log = LogFactory.getLog(ProxyServiceFactory.class);


    public static ProxyService createProxy(OMElement elem) {

        OMAttribute name = elem.getAttribute(nullName("name"));
        if (name == null) {
            handleException("The 'name' attribute is required for a Proxy service definition");
        }

        final ProxyService proxy = new ProxyService(name.getAttributeValue());

        readProxyAttrs(elem, proxy);
        readDescription(elem, proxy);
        readTarget(elem, proxy);
        readWSDL(elem, proxy);
        readPolicies(elem, proxy);
        readEnsureSwA(elem, proxy);
        readEnabled(elem, proxy);
        readParams(elem, proxy);


        String nameString = proxy.getName();
        if (nameString == null || "".equals(nameString)) {
            nameString = SynapseConstants.ANONYMOUS_PROXYSERVICE;
        }
        AspectConfiguration aspectConfiguration = new AspectConfiguration(nameString);
        proxy.configure(aspectConfiguration);

        String statistics = getAttrStr(elem, nullName(XMLConfigConstants.STATISTICS_ATTRIB_NAME));
        if (statistics != null) {
        	if (XMLConfigConstants.STATISTICS_ENABLE.equals(statistics)) {
        		aspectConfiguration.enableStatistics();
            }
        }

        return proxy;
    }

    private static void readProxyAttrs(OMElement elem, ProxyService proxy) {
        String transports = getAttrStr(elem, nullName("transports"));
        if (transports == null || ProxyService.ALL_TRANSPORTS.equals(transports)) {
        	// default to all transports using service name as destination
        } else {
        	proxy.setTransports(splitAttrs(transports));
        }

        String pinnedServers = getAttrStr(elem, nullName("pinnedServers"));
        if (pinnedServers == null) {
        	// default to all servers
        } else {
        	proxy.setPinnedServers(splitAttrs(pinnedServers));
        }

        String trace = getAttrStr(elem, nullName(XMLConfigConstants.TRACE_ATTRIB_NAME));
        if (trace != null) {
        	if (trace.equals(XMLConfigConstants.TRACE_ENABLE)) {
        		proxy.setTraceState(org.apache.synapse.SynapseConstants.TRACING_ON);
            } else if (trace.equals(XMLConfigConstants.TRACE_DISABLE)) {
            	proxy.setTraceState(org.apache.synapse.SynapseConstants.TRACING_OFF);
            }
        }

        OMAttribute startOnLoad = elem.getAttribute(nullName("startOnLoad"));
        if (startOnLoad != null) {
            proxy.setStartOnLoad(Boolean.valueOf(startOnLoad.getAttributeValue()));
        } else {
            proxy.setStartOnLoad(true);
        }
    }

    private static void readDescription(OMElement elem, ProxyService proxy) {
        // setting the description of the proxy service
        OMElement descriptionElement = elem.getFirstChildWithName(synapseName("description"));
        if (descriptionElement != null) {
            proxy.setDescription(descriptionElement.getText().trim());
        }
    }

    private static void readTarget(OMElement elem, ProxyService proxy) {
    	// read definition of the target of this proxy service. The target could be an 'endpoint'
        // or a named sequence. If none of these are specified, the messages would be mediated
        // by the Synapse main mediator
        OMElement target = elem.getFirstChildWithName(synapseName("target"));
        if (target != null) {
            boolean isTargetOk = false;
            SequenceMediatorFactory mediatorFactory = new SequenceMediatorFactory();
            OMAttribute inSequence = target.getAttribute(nullName("inSequence"));
            if (inSequence != null) {
                proxy.setTargetInSequence(inSequence.getAttributeValue());
                isTargetOk = true;
            } else {
                OMElement inSequenceElement = target.getFirstChildWithName(synapseName("inSequence"));
                if (inSequenceElement != null) {
                    proxy.setTargetInLineInSequence(
                            mediatorFactory.createAnonymousSequence(inSequenceElement));
                    isTargetOk = true;
                }
            }
            OMAttribute outSequence = target.getAttribute(nullName("outSequence"));
            if (outSequence != null) {
                proxy.setTargetOutSequence(outSequence.getAttributeValue());
            } else {
                OMElement outSequenceElement = target.getFirstChildWithName(synapseName("outSequence"));
                if (outSequenceElement != null) {
                    proxy.setTargetInLineOutSequence(
                            mediatorFactory.createAnonymousSequence(outSequenceElement));
                }
            }
            OMAttribute faultSequence = target.getAttribute(nullName("faultSequence"));
            if (faultSequence != null) {
                proxy.setTargetFaultSequence(faultSequence.getAttributeValue());
            } else {
                OMElement faultSequenceElement = target.getFirstChildWithName(synapseName("faultSequence"));
                if (faultSequenceElement != null) {
                    proxy.setTargetInLineFaultSequence(
                            mediatorFactory.createAnonymousSequence(faultSequenceElement));
                }
            }
            OMAttribute tgtEndpt = target.getAttribute(nullName("endpoint"));
            if (tgtEndpt != null) {
                proxy.setTargetEndpoint(tgtEndpt.getAttributeValue());
                isTargetOk = true;
            } else {
                OMElement endpointElement = target.getFirstChildWithName(synapseName("endpoint"));
                if (endpointElement != null) {
                    proxy.setTargetInLineEndpoint(
                            EndpointFactory.getEndpointFromElement(endpointElement, true));
                    isTargetOk = true;
                }
            }
            if(!isTargetOk) {
                handleException("Target of the proxy service must declare " +
                        "either an inSequence or endpoint or both");
            }
        } else {
            handleException("Target is required for a Proxy service definition");
        }
    }

    private static void readWSDL(OMElement elem, ProxyService proxy) {
        OMElement wsdl = elem.getFirstChildWithName(synapseName("publishWSDL"));
        if (wsdl != null) {
            OMAttribute wsdlkey = wsdl.getAttribute(nullName("key"));
            if (wsdlkey != null) {
                proxy.setWSDLKey(wsdlkey.getAttributeValue());
            } else {
                OMAttribute wsdlURI = wsdl.getAttribute(nullName("uri"));
                if (wsdlURI != null) {
                    try {
                        proxy.setWsdlURI(new URI(wsdlURI.getAttributeValue()));
                    } catch (URISyntaxException e) {
                        String msg = "Error creating uri for proxy service wsdl";
                        handleException(msg, e);
                    }
                } else {
                    OMElement wsdl11 = wsdl.getFirstChildWithName(
                            new QName(WSDLConstants.WSDL1_1_NAMESPACE, "definitions"));
                    if (wsdl11 != null) {
                        proxy.setInLineWSDL(wsdl11);
                    } else {
                        OMElement wsdl20 = wsdl.getFirstChildWithName(
                                new QName(WSDL2Constants.WSDL_NAMESPACE, "description"));
                        if (wsdl20 != null) {
                            proxy.setInLineWSDL(wsdl20);
                        }
                    }
                }
            }
            proxy.setResourceMap(ResourceMapFactory.createResourceMap(wsdl));
        }

    }

    private static void readPolicies(OMElement elem, ProxyService proxy) {
        for(OMElement policy: elem.getChildrenWithName(synapseName("policy"))) {
        	OMAttribute where = policy.getAttribute(nullName("where"));
        	OMAttribute key = policy.getAttribute(nullName("key"));
        	OMAttribute type = policy.getAttribute(nullName("type"));
        	OMAttribute operationName = policy.getAttribute(nullName("operationName"));
        	OMAttribute operationNS = policy.getAttribute(nullName("operationNamespace"));

        	if(where == null) {
        		handleException("You must declare where you want the policy attached.");
        	}
        	if(key == null) {
        		handleException("Policy element does not specify the policy key");
        	}

        	PolicyInfo pi = null;
        	final String policyKey = key.getAttributeValue();
        	final String attachLoc = where.getAttributeValue();
        	if("binding".equals(attachLoc)) {
        		pi = new BindingPolicyInfo(policyKey);
        	} else if("service".equals(attachLoc)) {
        		pi = new ServicePolicyInfo(policyKey);
        	} else {
        		QName opName = null;
        		if (operationName != null && operationName.getAttributeValue() != null) {
        			if (operationNS != null && operationNS.getAttributeValue() != null) {
        				opName = new QName(	operationNS.getAttributeValue(),
        						operationName.getAttributeValue());
        			} else {
        				opName = new QName(operationName.getAttributeValue());
        			}
        		}
        		if("operation".equals(attachLoc)) {
        			pi = new OperationPolicyInfo(policyKey, opName);
        		} else if("message".equals(attachLoc)) {
        			int messageType = 0;
        			if ("in".equals(type.getAttributeValue())) {
        				messageType = MessagePolicyInfo.MESSAGE_TYPE_IN;
        			} else if ("out".equals(type.getAttributeValue())) {
        				messageType = MessagePolicyInfo.MESSAGE_TYPE_OUT;
        			} else {
        				handleException("Undefined policy type for the policy with key : "
        						+ key.getAttributeValue());
        			}
        			pi = new MessagePolicyInfo(policyKey, opName, messageType);
        		} else {
        			handleException("Unknown attachment location '" + attachLoc + "'");
        		}
        	}
        	proxy.addPolicyInfo(pi);
        }
    }

    private static void readParams(OMElement elem, ProxyService proxy) {
    	for(OMElement prop: elem.getChildrenWithName(synapseName("parameter"))) {
    		OMAttribute pname = prop.getAttribute(nullName("name"));
    		OMElement propertyValue = prop.getFirstElement();
    		if (pname != null) {
    			if (propertyValue != null) {
    				proxy.addParameter(pname.getAttributeValue(), propertyValue);
    			} else {
    				proxy.addParameter(pname.getAttributeValue(), prop.getText().trim());
    			}
    		} else {
    			handleException("Invalid property specified for proxy service : "
    					+ proxy.getName());
    		}
    	}
    }

    private static void readEnsureSwA(OMElement elem, ProxyService proxy) {
    	for(OMElement ensureSwA: elem.getChildrenWithName(synapseName("ensureSwA"))) {
    		OMAttribute ns = ensureSwA.getAttribute(nullName("ns"));
    		String namespace = (ns != null) ? ns.getAttributeValue() : null;
    		for(OMElement el: ensureSwA.getChildrenWithName(synapseName("element"))) {
    			OMAttribute n = el.getAttribute(nullName("name"));
    			String name = (n != null) ? n.getAttributeValue() : null;
    			if(namespace != null && name != null) {
    				proxy.ensureSwA(new QName(namespace, name));
    			}
    		}
    	}
    }

    private static void readEnabled(OMElement elem, ProxyService proxy) {
        if (elem.getFirstChildWithName(synapseName("enableAddressing")) != null) {
            proxy.setWsAddrEnabled(true);
        }

        if (elem.getFirstChildWithName(synapseName("enableRM")) != null) {
            proxy.setWsRMEnabled(true);
        }

        if (elem.getFirstChildWithName(synapseName("enableSec")) != null) {
            proxy.setWsSecEnabled(true);
        }
    }

    private static String getAttrStr(OMElement elem, QName qname) {
    	OMAttribute attr = elem.getAttribute(qname);
    	if(attr == null) {
    		return null;
    	}
    	String out = attr.getAttributeValue();
    	return out;
    }

    private static List<String> splitAttrs(String in) {
    	StringTokenizer st = new StringTokenizer(in, " ,");
        List<String> outList = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.length() != 0) {
                outList.add(token);
            }
        }
        return outList;
    }

    private static QName synapseName(String name) {
    	return new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, name);
    }

    private static QName nullName(String name) {
    	return new QName(XMLConfigConstants.NULL_NAMESPACE, name);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

}
