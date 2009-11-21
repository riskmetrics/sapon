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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.PolicyUtil;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;

public class AxisBindingMessage extends AxisDescription {

	private String name;

	private String direction;

	private final Map<String, Object> options;

	private AxisMessage axisMessage;

	// Used to indicate whether this message is a fault or not. Needed for the
	// WSDL 2.0 serializer
	private boolean fault = false;

	private Policy effectivePolicy = null;
	private Date lastPolicyCalcuatedTime = null;

	public boolean isFault() {
		return fault;
	}

	public void setFault(boolean fault) {
		this.fault = fault;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AxisMessage getAxisMessage() {
		return axisMessage;
	}

	public void setAxisMessage(AxisMessage axisMessage) {
		this.axisMessage = axisMessage;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public AxisBindingMessage() {
		options = new HashMap<String, Object>();
	}

	public void setProperty(String name, Object value) {
		options.put(name, value);
	}

	/**
	 * @param name
	 *            name of the property to search for
	 * @return the value of the property, or null if the property is not found
	 */
	public Object getProperty(String name) {
		Object obj = options.get(name);
		if (obj != null) {
			return obj;
		}

		return null;
	}

	@Override
	public Object getKey() {
		return null; // To change body of implemented methods use File |
		// Settings | File Templates.
	}

	@Override
	public void engageModule(AxisModule axisModule) throws AxisFault {
		throw new UnsupportedOperationException("Sorry we do not support this");
	}

	@Override
	public boolean isEngaged(String moduleName) {
		throw new UnsupportedOperationException(
				"axisMessage.isEngaged() is not supported");

	}

	/**
	 * Generates the bindingMessage element (can be input, output, infault or
	 * outfault)
	 *
	 * @param tns -
	 *            The targetnamespace
	 * @param wsoap -
	 *            The SOAP namespace (WSDL 2.0)
	 * @param whttp -
	 *            The HTTP namespace (WSDL 2.0)
	 * @param nameSpaceMap -
	 *            The namespacemap of the service
	 * @return The generated bindingMessage element
	 */
//	public OMElement toWSDL20(OMNamespace wsdl, OMNamespace tns,
//			OMNamespace wsoap, OMNamespace whttp, Map<String, String> nameSpaceMap) {
//		String property;
//		OMFactory omFactory = OMAbstractFactory.getOMFactory();
//		OMElement bindingMessageElement;
//
//		// If this is a fault, create a fault element and add fault specific
//		// properties
//		if (this.isFault()) {
//			if (this.getParent() instanceof AxisBinding) {
//				bindingMessageElement = omFactory.createOMElement(
//						WSDL2Constants.FAULT_LOCAL_NAME, wsdl);
//			} else if (WSDLConstants.WSDL_MESSAGE_DIRECTION_IN.equals(this
//					.getDirection())) {
//				bindingMessageElement = omFactory.createOMElement(
//						WSDL2Constants.IN_FAULT_LOCAL_NAME, wsdl);
//			} else {
//				bindingMessageElement = omFactory.createOMElement(
//						WSDL2Constants.OUT_FAULT_LOCAL_NAME, wsdl);
//			}
//			bindingMessageElement.addAttribute(omFactory.createOMAttribute(
//					WSDL2Constants.ATTRIBUTE_REF, null, tns.getPrefix() + ":"
//							+ this.name));
//
//			WSDL20Util.extractWSDL20SoapFaultInfo(options,
//					bindingMessageElement, omFactory, wsoap);
//
//			Integer code = (Integer) this.options
//					.get(WSDL2Constants.ATTR_WHTTP_CODE);
//			if (code != null) {
//				bindingMessageElement.addAttribute(omFactory.createOMAttribute(
//						WSDL2Constants.ATTRIBUTE_CODE, whttp, code.toString()));
//			}
//
//			// Checks whether the message is an input message
//		} else if (WSDLConstants.WSDL_MESSAGE_DIRECTION_IN.equals(this
//				.getDirection())) {
//			bindingMessageElement = omFactory.createOMElement(
//					WSDL2Constants.IN_PUT_LOCAL_NAME, wsdl);
//
//			// Message should be an output message
//		} else {
//			bindingMessageElement = omFactory.createOMElement(
//					WSDL2Constants.OUT_PUT_LOCAL_NAME, wsdl);
//		}
//
//		// Populate common properties
//		property = (String) this.options
//				.get(WSDL2Constants.ATTR_WHTTP_CONTENT_ENCODING);
//		if (property != null) {
//			bindingMessageElement
//					.addAttribute(omFactory.createOMAttribute(
//							WSDL2Constants.ATTRIBUTE_CONTENT_ENCODING, whttp,
//							property));
//		}
//		List<HTTPHeaderMessage> httpHeaders = (List<HTTPHeaderMessage>) this.options.get(WSDL2Constants.ATTR_WHTTP_HEADER);
//		if (httpHeaders != null && httpHeaders.size() > 0) {
//			WSDLSerializationUtil.addHTTPHeaderElements(omFactory, httpHeaders, whttp,
//					bindingMessageElement, nameSpaceMap);
//		}
//		List<SOAPHeaderMessage> soapHeaders = (List<SOAPHeaderMessage>) this.options.get(WSDL2Constants.ATTR_WSOAP_HEADER);
//		if (soapHeaders != null && soapHeaders.size() > 0) {
//			WSDLSerializationUtil.addSOAPHeaderElements(omFactory, soapHeaders, wsoap,
//					bindingMessageElement, nameSpaceMap);
//		}
//		List<SOAPModuleMessage> soapModules = (List<SOAPModuleMessage>) this.options.get(WSDL2Constants.ATTR_WSOAP_MODULE);
//		if (soapModules != null && soapModules.size() > 0) {
//			WSDLSerializationUtil.addSOAPModuleElements(omFactory, soapModules, wsoap,
//					bindingMessageElement);
//		}
//		WSDLSerializationUtil.addWSDLDocumentationElement(this,
//				bindingMessageElement, omFactory, wsdl);
//		WSDLSerializationUtil.addPoliciesAsExtensibleElement(this,
//				bindingMessageElement);
//		return bindingMessageElement;
//	}

	public AxisBindingOperation getAxisBindingOperation() {
		return (AxisBindingOperation) parent;
	}

	public Policy getEffectivePolicy() {
	       if (lastPolicyCalcuatedTime == null || isPolicyUpdated()) {
			effectivePolicy = calculateEffectivePolicy();
		}
		return effectivePolicy;
	}

	public Policy calculateEffectivePolicy() {
		PolicySubject policySubject = null;
		List<PolicyComponent> policyList = new ArrayList<PolicyComponent>();

		// AxisBindingMessage
		policySubject = getPolicySubject();
		policyList.addAll(policySubject.getAttachedPolicyComponents());

		// AxisBindingOperation policies
		AxisBindingOperation axisBindingOperation = getAxisBindingOperation();
		if (axisBindingOperation != null) {
			policyList.addAll(axisBindingOperation.getPolicySubject()
					.getAttachedPolicyComponents());
		}

		// AxisBinding
		AxisBinding axisBinding = (axisBindingOperation == null) ? null
				: axisBindingOperation.getAxisBinding();
		if (axisBinding != null) {
			policyList.addAll(axisBinding.getPolicySubject()
					.getAttachedPolicyComponents());
		}

		// AxisEndpoint
		AxisEndpoint axisEndpoint = (axisBinding == null) ? null : axisBinding
				.getAxisEndpoint();
		if (axisEndpoint != null) {
			policyList.addAll(axisEndpoint.getPolicySubject()
					.getAttachedPolicyComponents());
		}

		// AxisMessage
		if (axisMessage != null) {
			policyList.addAll(axisMessage.getPolicySubject()
					.getAttachedPolicyComponents());
		}

		// AxisOperation
		AxisOperation axisOperation = (axisMessage == null) ? null
				: axisMessage.getAxisOperation();
		if (axisOperation != null) {
			policyList.addAll(axisOperation.getPolicySubject()
					.getAttachedPolicyComponents());
		}

		// AxisService
		AxisService axisService = (axisOperation == null) ? null
				: axisOperation.getAxisService();
		if (axisService != null) {
			policyList.addAll(axisService.getPolicySubject()
					.getAttachedPolicyComponents());
		}

		// AxisConfiguration
		AxisConfiguration axisConfiguration = (axisService == null) ? null
				: axisService.getAxisConfiguration();
		if (axisConfiguration != null) {
			policyList.addAll(axisConfiguration.getPolicySubject()
					.getAttachedPolicyComponents());
		}

		lastPolicyCalcuatedTime = new Date();
		return PolicyUtil.getMergedPolicy(policyList, axisService);
	}

	private boolean isPolicyUpdated() {
		if (getPolicySubject().getLastUpdatedTime().after(
				lastPolicyCalcuatedTime)) {
			return true;
		}
		// AxisBindingOperation
		AxisBindingOperation axisBindingOperation = getAxisBindingOperation();
		if (axisBindingOperation != null
				&& axisBindingOperation.getPolicySubject().getLastUpdatedTime()
						.after(lastPolicyCalcuatedTime)) {
			return true;
		}
		// AxisBinding
		AxisBinding axisBinding = (axisBindingOperation == null) ? null
				: axisBindingOperation.getAxisBinding();
		if (axisBinding != null
				&& axisBinding.getPolicySubject().getLastUpdatedTime().after(
						lastPolicyCalcuatedTime)) {
			return true;
		}
		// AxisEndpoint
		AxisEndpoint axisEndpoint = (axisBinding == null) ? null : axisBinding
				.getAxisEndpoint();
		if (axisEndpoint != null
				&& axisEndpoint.getPolicySubject().getLastUpdatedTime().after(
						lastPolicyCalcuatedTime)) {
			return true;
		}
		// AxisMessage
		if (axisMessage != null
				&& axisMessage.getPolicySubject().getLastUpdatedTime().after(
						lastPolicyCalcuatedTime)) {
			return true;
		}
		// AxisOperation
		AxisOperation axisOperation = (axisMessage == null) ? null
				: axisMessage.getAxisOperation();
		if (axisOperation != null
				&& axisOperation.getPolicySubject().getLastUpdatedTime().after(
						lastPolicyCalcuatedTime)) {
			return true;
		}
		// AxisService
		AxisService axisService = (axisOperation == null) ? null
				: axisOperation.getAxisService();
		if (axisService != null
				&& axisService.getPolicySubject().getLastUpdatedTime().after(
						lastPolicyCalcuatedTime)) {
			return true;
		}
		// AxisConfiguration
		AxisConfiguration axisConfiguration = (axisService == null) ? null
				: axisService.getAxisConfiguration();
		if (axisConfiguration != null
				&& axisConfiguration.getPolicySubject().getLastUpdatedTime()
						.after(lastPolicyCalcuatedTime)) {
			return true;
		}
		return false;
	}
}
