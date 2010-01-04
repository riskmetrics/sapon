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

package org.apache.axis2.deployment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.dataretrieval.DRConstants;
import org.apache.axis2.deployment.util.Utils;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.description.java2wsdl.Java2WSDLConstants;
import org.apache.axis2.description.java2wsdl.TypeTable;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.engine.ObjectSupplier;
import org.apache.axis2.engine.ServiceLifeCycle;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.util.Loader;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builds a service description from OM
 */
public class ServiceBuilder extends DescriptionBuilder {
	private static final Log log = LogFactory.getLog(ServiceBuilder.class);
	private AxisService service;
	private Map<String, AxisService> wsdlServiceMap
		= new HashMap<String, AxisService>();

	public ServiceBuilder(ConfigurationContext configCtx, AxisService service) {
		this.service = service;
		this.configCtx = configCtx;
		this.axisConfig = this.configCtx.getAxisConfiguration();
	}

	public ServiceBuilder(InputStream serviceInputStream,
			ConfigurationContext configCtx, AxisService service) {
		super(serviceInputStream, configCtx);
		this.service = service;
	}

	/**
	 * Populates service from corresponding OM.
	 *
	 * @param service_element
	 *            an OMElement for the &lt;service&gt; tag
	 * @return a filled-in AxisService, configured from the passed XML
	 * @throws DeploymentException
	 *             if there is a problem
	 */
	public AxisService populateService(OMElement service_element)
			throws DeploymentException {
		try {
			// Determine whether service should be activated.
			String serviceActivate = service_element
					.getAttributeValue(new QName(ATTRIBUTE_ACTIVATE));
			if (serviceActivate != null) {
				if ("true".equals(serviceActivate)) {
					service.setActive(true);
				} else if ("false".equals(serviceActivate)) {
					service.setActive(false);
				}
			}

			// Processing service level parameters
			OMAttribute serviceNameatt = service_element
					.getAttribute(new QName(ATTRIBUTE_NAME));

			// If the service name is explicitly specified in the services.xml
			// then use that as the service name
			if (serviceNameatt != null) {
				if (!"".equals(serviceNameatt.getAttributeValue().trim())) {
					AxisService wsdlService = wsdlServiceMap
							.get(serviceNameatt.getAttributeValue());
					if (wsdlService != null) {
						wsdlService.setClassLoader(service.getClassLoader());
						wsdlService.setParent(service.getServiceGroup());
						service = wsdlService;
						service.setWsdlFound(true);
						service.setCustomWsdl(true);
					}
					service.setName(serviceNameatt.getAttributeValue());
					// To be on the safe side
					if (service.getDocumentation() == null) {
						service.setDocumentation(serviceNameatt
								.getAttributeValue());
					}
				}
			}

			Iterable<OMElement> itr = service_element.getChildrenWithName(new QName(
					TAG_PARAMETER));
			processParameters(itr, service, service.getServiceGroup());

			// If multiple services in one service group have different values
			// for the PARENT_FIRST
			// parameter then the final value become the value specified by the
			// last service in the group
			// Parameter parameter =
			// service.getParameter(DeploymentClassLoader.PARENT_FIRST);
			// if (parameter !=null && "false".equals(parameter.getValue())) {
			// ClassLoader serviceClassLoader = service.getClassLoader();
			// ((DeploymentClassLoader)serviceClassLoader).setParentFirst(false);
			// }
			// process service description
			OMElement descriptionElement = service_element
					.getFirstChildWithName(new QName(TAG_DESCRIPTION));
			if (descriptionElement != null) {
				OMElement descriptionValue = descriptionElement
						.getFirstElement();
				if (descriptionValue != null) {
					service.setDocumentation(descriptionValue);
				} else {
					service.setDocumentation(descriptionElement.getText());
				}
			} else {
				serviceNameatt = service_element.getAttribute(new QName(
						ATTRIBUTE_NAME));

				if (serviceNameatt != null) {
					if (!"".equals(serviceNameatt.getAttributeValue().trim())
							&& service.getDocumentation() == null) {
						service.setDocumentation(serviceNameatt
								.getAttributeValue());
					}
				}
			}

			if (service.getParameter("ServiceClass") == null) {
				log.debug("The Service " + service.getName()
						+ " does not specify a Service Class");
			}

			// Process WS-Addressing flag attribute
			OMAttribute addressingRequiredatt = service_element
					.getAttribute(new QName(ATTRIBUTE_WSADDRESSING));
			if (addressingRequiredatt != null) {
				String addressingRequiredString = addressingRequiredatt
						.getAttributeValue();
				AddressingHelper.setAddressingRequirementParemeterValue(
						service, addressingRequiredString);
			}

			// Setting service target namespace if any
			OMAttribute targetNameSpace = service_element
					.getAttribute(new QName(TARGET_NAME_SPACE));

			if (targetNameSpace != null) {
				String nameSpeceVale = targetNameSpace.getAttributeValue();
				if (nameSpeceVale != null && !"".equals(nameSpeceVale)) {
					service.setTargetNamespace(nameSpeceVale);
				}
			} else {
				if (service.getTargetNamespace() == null
						|| "".equals(service.getTargetNamespace())) {
					service
							.setTargetNamespace(Java2WSDLConstants.DEFAULT_TARGET_NAMESPACE);
				}
			}

			// Processing service lifecycle attribute
			OMAttribute serviceLifeCycleClass = service_element
					.getAttribute(new QName(TAG_CLASS_NAME));
			if (serviceLifeCycleClass != null) {
				String className = serviceLifeCycleClass.getAttributeValue();
				loadServiceLifeCycleClass(className);
			}
			// Setting schema namespece if any
			OMElement schemaElement = service_element
					.getFirstChildWithName(new QName(SCHEMA));
			if (schemaElement != null) {
				OMAttribute schemaNameSpace = schemaElement
						.getAttribute(new QName(SCHEMA_NAME_SPACE));
				if (schemaNameSpace != null) {
					String nameSpeceVale = schemaNameSpace.getAttributeValue();
					if (nameSpeceVale != null && !"".equals(nameSpeceVale)) {
						service.setSchemaTargetNamespace(nameSpeceVale);
					}
				}
				OMAttribute elementFormDefault = schemaElement
						.getAttribute(new QName(SCHEMA_ELEMENT_QUALIFIED));
				if (elementFormDefault != null) {
					String value = elementFormDefault.getAttributeValue();
					if ("true".equals(value)) {
						service.setElementFormDefault(true);
					} else if ("false".equals(value)) {
						service.setElementFormDefault(false);
					}
				}

				// package to namespace mapping. This will be an element that
				// maps pkg names to a namespace
				// when this is doing AxisService.getSchemaTargetNamespace will
				// be overridden
				// This will be <mapping/> with @namespace and @package
				Map<String, String> pkg2nsMap = new HashMap<String, String>();
				for(OMElement mappingElement: schemaElement.getChildrenWithName(new QName(MAPPING))) {
					OMAttribute namespaceAttribute = mappingElement
								.getAttribute(new QName(ATTRIBUTE_NAMESPACE));
					OMAttribute packageAttribute = mappingElement
							.getAttribute(new QName(ATTRIBUTE_PACKAGE));
					if (namespaceAttribute != null
							&& packageAttribute != null) {
						String namespaceAttributeValue = namespaceAttribute
								.getAttributeValue();
						String packageAttributeValue = packageAttribute
								.getAttributeValue();
						if (namespaceAttributeValue != null
								&& packageAttributeValue != null) {
							pkg2nsMap.put(packageAttributeValue.trim(),
									namespaceAttributeValue.trim());
						} else {
							log
									.warn("Either value of @namespce or @packagename not available. Thus, generated will be selected.");
						}
					} else {
						log
								.warn("Either @namespce or @packagename not available. Thus, generated will be selected.");
					}
				}
				service.setP2nMap(pkg2nsMap);
			}

			// processing Default Message receivers
			OMElement messageReceiver = service_element
					.getFirstChildWithName(new QName(TAG_MESSAGE_RECEIVERS));
			if (messageReceiver != null) {
				Map<String, MessageReceiver> mrs = processMessageReceivers(service.getClassLoader(),
						messageReceiver);
				for(Map.Entry<String, MessageReceiver> e: mrs.entrySet()) {
					service.addMessageReceiver(e.getKey(), e.getValue());
				}
			}

			// Removing exclude operations
			OMElement excludeOperations = service_element
					.getFirstChildWithName(new QName(TAG_EXCLUDE_OPERATIONS));
			List<String> excludeops = null;
			if (excludeOperations != null) {
				excludeops = processExcludeOperations(excludeOperations);
			}
			if (excludeops == null) {
				excludeops = new ArrayList<String>();
			}
			Utils.addExcludeMethods(excludeops);

			// <schema targetNamespace="http://x.y.z"/>
			// setting the PolicyInclude
			// processing <wsp:Policy> .. </..> elements
			Iterable<OMElement> policyElements = service_element
					.getChildrenWithName(new QName(POLICY_NS_URI, TAG_POLICY));

			if (policyElements != null) {
				processPolicyElements(policyElements, service);
			}

			// processing <wsp:PolicyReference> .. </..> elements
			Iterable<OMElement> policyRefElements = service_element
					.getChildrenWithName(new QName(POLICY_NS_URI,
							TAG_POLICY_REF));

			if (policyRefElements != null) {
				processPolicyRefElements(policyRefElements, service);
			}

			// processing service scope
			String sessionScope = service_element.getAttributeValue(new QName(
					ATTRIBUTE_SCOPE));
			if (sessionScope != null) {
				service.setScope(sessionScope);
			}

			// processing service-wide modules which required to engage globally
			Iterable<OMElement> moduleRefs = service_element
					.getChildrenWithName(new QName(TAG_MODULE));

			processModuleRefs(moduleRefs);

			// processing transports
			OMElement transports = service_element
					.getFirstChildWithName(new QName(TAG_TRANSPORTS));
			if (transports != null) {
				List<String> trs = new ArrayList<String>();
				for(OMElement trsEle: transports.getChildrenWithName(new QName(TAG_TRANSPORT))) {
					String transportName = trsEle.getText().trim();
					trs.add(transportName);
					if (axisConfig.getTransportIn(transportName) == null) {
						throw new AxisFault("Service [ " + service.getName()
								+ "] is trying to expose in a transport : "
								+ transports
								+ " and which is not available in Axis2");
					}
				}
				service.setExposedTransports(trs);
			}
			// processing operations
			Iterable<OMElement> operationsIterator = service_element
					.getChildrenWithName(new QName(TAG_OPERATION));
			List<AxisOperation> ops = processOperations(operationsIterator);

			for(final AxisOperation operationDesc: ops) {
				List<String> wsamappings = operationDesc.getWSAMappingList();
				if (wsamappings == null) {
					continue;
				}
				if (service.getOperation(operationDesc.getName()) == null) {
					service.addOperation(operationDesc);
				}
				for(final String mapping: wsamappings) {
					if (mapping.length() > 0) {
						service.mapActionToOperation(mapping, operationDesc);
					}
				}
			}
			String objectSupplierValue = (String) service
					.getParameterValue(TAG_OBJECT_SUPPLIER);
			if (objectSupplierValue != null) {
				loadObjectSupplierClass(objectSupplierValue);
			}
			// Set the default message receiver for the operations that were
			// not listed in the services.xml
			setDefaultMessageReceivers();
			Utils.processBeanPropertyExclude(service);
			if (!service.isUseUserWSDL()) {
				// Generating schema for the service if the impl class is Java
				if (!service.isWsdlFound()) {
					// trying to generate WSDL for the service using JAM and
					// Java reflection
					try {
						if (generateWsdl(service)) {
							Utils.fillAxisService(service, axisConfig,
									excludeops, null);
						} else {
							List<String> nonRpcOperations = getNonRPCMethods(service);
							Utils.fillAxisService(service, axisConfig,
									excludeops, nonRpcOperations);
						}
					} catch (Exception e) {
						throw new DeploymentException(Messages.getMessage(
								"errorinschemagen", e.getMessage()), e);
					}
				}
			}
			if (service.isCustomWsdl()) {
				OMElement mappingElement = service_element
						.getFirstChildWithName(new QName(TAG_PACKAGE2QNAME));
				if (mappingElement != null) {
					processTypeMappings(mappingElement);
				}
			}

			for (int i = 0; i < excludeops.size(); i++) {
				String opName = excludeops.get(i);
				service.removeOperation(new QName(opName));
			}

			// Need to call the same logic towice
			setDefaultMessageReceivers();
			Iterable<OMElement> moduleConfigs = service_element
					.getChildrenWithName(new QName(TAG_MODULE_CONFIG));
			processServiceModuleConfig(moduleConfigs, service, service);

			// Loading Data Locator(s) configured
			OMElement dataLocatorElement = service_element
					.getFirstChildWithName(new QName(
							DRConstants.DATA_LOCATOR_ELEMENT));
			if (dataLocatorElement != null) {
				processDataLocatorConfig(dataLocatorElement, service);
			}

			processEndpoints(service);
			processPolicyAttachments(service_element, service);


		} catch (AxisFault axisFault) {
			throw new DeploymentException(axisFault);
		}

		return service;
	}

	private void setDefaultMessageReceivers() {
		Iterator<AxisOperation> operations = service.getPublishedOperations().iterator();
		while (operations.hasNext()) {
			AxisOperation operation = operations.next();
			if (operation.getMessageReceiver() == null) {
				MessageReceiver messageReceiver = loadDefaultMessageReceiver(
						operation.getMessageExchangePattern(), service);
				if (messageReceiver == null &&
				// we assume that if the MEP is ROBUST_IN_ONLY then the in-out
						// MR can handle that
						WSDL2Constants.MEP_URI_ROBUST_IN_ONLY.equals(operation
								.getMessageExchangePattern())) {
					messageReceiver = loadDefaultMessageReceiver(
							WSDL2Constants.MEP_URI_IN_OUT, service);

				}
				operation.setMessageReceiver(messageReceiver);
			}
		}
	}

	private void loadObjectSupplierClass(String objectSupplierValue)
			throws AxisFault {
		try {
			ClassLoader loader = service.getClassLoader();
			Class<?> objectSupplierImpl = Loader.loadClass(loader,
					objectSupplierValue.trim());
			ObjectSupplier objectSupplier = (ObjectSupplier) objectSupplierImpl
					.newInstance();
			service.setObjectSupplier(objectSupplier);
		} catch (Exception e) {
			throw AxisFault.makeFault(e);
		}
	}

	/**
	 * Process the package name to QName mapping:
	 *
	 * &lt;packageMapping&gt; &lt;mapping packageName="foo.bar"
	 * qname="http://foo/bar/xsd"%gt; ...... ...... &lt;/packageMapping&gt;
	 *
	 * @param packageMappingElement
	 *            OMElement for the packageMappingElement
	 */
	private void processTypeMappings(OMElement packageMappingElement) {
		TypeTable typeTable = service.getTypeTable();
		if (typeTable == null) {
			typeTable = new TypeTable();
		}
		for(OMElement mappingElement: packageMappingElement.getChildrenWithName(new QName(TAG_MAPPING))) {
			String packageName = mappingElement.getAttributeValue(new QName(
					TAG_PACKAGE_NAME));
			String qName = mappingElement
					.getAttributeValue(new QName(TAG_QNAME));
			if (packageName == null || qName == null) {
				continue;
			}
			Iterator<String> keys = service.getNamespaceMap().keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				if (qName.equals(service.getNamespaceMap().get(key))) {
					typeTable.addComplexSchema(packageName, new QName(qName,
							packageName, key));
				}
			}
		}
		service.setTypeTable(typeTable);
	}

	private void loadServiceLifeCycleClass(String className)
			throws DeploymentException {
		if (className != null) {
			try {
				ClassLoader loader = service.getClassLoader();
				Class<?> serviceLifeCycleClassImpl = Loader.loadClass(loader,
						className);
				ServiceLifeCycle serviceLifeCycle = (ServiceLifeCycle) serviceLifeCycleClassImpl
						.newInstance();
				serviceLifeCycle.startUp(configCtx, service);
				service.setServiceLifeCycle(serviceLifeCycle);
			} catch (Exception e) {
				throw new DeploymentException(e.getMessage(), e);
			}
		}
	}

	private boolean generateWsdl(AxisService axisService) {
		for(final AxisOperation axisOperation: axisService.getOperations()) {
			if (axisOperation.isControlOperation()) {
				continue;
			}

			if (axisOperation.getMessageReceiver() == null) {
				continue;
			}
			String messageReceiverClass = axisOperation
				.getMessageReceiver().getClass().getName();
			if (!("org.apache.axis2.rpc.receivers.RPCMessageReceiver"
					.equals(messageReceiverClass)
					|| "org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver"
					.equals(messageReceiverClass)
					|| "org.apache.axis2.rpc.receivers.RPCInOutAsyncMessageReceiver"
					.equals(messageReceiverClass) || "org.apache.axis2.jaxws.server.JAXWSMessageReceiver"
					.equals(messageReceiverClass))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * To get the methods which do not use RPC* MessageReceivers
	 *
	 * @param axisService
	 *            the AxisService to search
	 * @return an ArrayList of the LOCAL PARTS of the QNames of any non-RPC
	 *         operations TODO: Why not just return the AxisOperations
	 *         themselves??
	 */

	private List<String> getNonRPCMethods(AxisService axisService) {
		List<String> excludeOperations = new ArrayList<String>();
		for(final AxisOperation axisOperation: axisService.getOperations()) {
			if (axisOperation.getMessageReceiver() == null) {
				continue;
			}
			final String messageReceiverClass
				= axisOperation.getMessageReceiver().getClass().getName();
			if (!(MESSAGE_RECEIVER_CLASSNAMES.contains(messageReceiverClass))) {
				excludeOperations.add(axisOperation.getName()
						.getLocalPart());
			}
		}
		return excludeOperations;
	}
	private static final List<String> MESSAGE_RECEIVER_CLASSNAMES
		= Arrays.asList(
			"org.apache.axis2.rpc.receivers.RPCMessageReceiver",
			"org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver",
			"org.apache.axis2.rpc.receivers.RPCInOutAsyncMessageReceiver",
			"org.apache.axis2.jaxws.server.JAXWSMessageReceiver"
		);

	/**
	 * Process &lt;excludeOperation&gt; element in services.xml. Each operation
	 * referenced will be removed from the AxisService.
	 *
	 * @param excludeOperations
	 *            the &lt;excludeOperations&gt; element from services.xml
	 * @return an ArrayList of the String contents of the &lt;operation&gt;
	 *         elements
	 */
	private List<String> processExcludeOperations(OMElement excludeOperations) {
		List<String> exOps = new ArrayList<String>();
		for(OMElement opName: excludeOperations.getChildrenWithName(new QName(TAG_OPERATION))) {
			exOps.add(opName.getText().trim());
		}
		return exOps;
	}

	private void processMessages(Iterable<OMElement> messages, AxisOperation operation)
			throws DeploymentException {
		for(OMElement messageElement: messages) {
			OMAttribute label = messageElement
					.getAttribute(new QName(TAG_LABEL));

			if (label == null) {
				throw new DeploymentException(Messages
						.getMessage("messagelabelcannotfound"));
			}

			AxisMessage message = operation.getMessage(label
					.getAttributeValue());

			// processing <wsp:Policy> .. </..> elements
			Iterable<OMElement> policyElements = messageElement
					.getChildrenWithName(new QName(POLICY_NS_URI, TAG_POLICY));

			if (policyElements != null) {
				processPolicyElements(policyElements, message);
			}

			// processing <wsp:PolicyReference> .. </..> elements
			Iterable<OMElement> policyRefElements = messageElement
					.getChildrenWithName(new QName(POLICY_NS_URI,
							TAG_POLICY_REF));

			if (policyRefElements != null) {
				processPolicyRefElements(policyRefElements, message);
			}

			Iterable<OMElement> parameters = messageElement.getChildrenWithName(new QName(
					TAG_PARAMETER));
			processParameters(parameters, message, operation);
		}
	}

	/**
	 * Gets the list of modules that is required to be engaged globally.
	 *
	 * @param moduleRefs
	 *            <code>java.util.Iterator</code>
	 * @throws DeploymentException
	 *             <code>DeploymentException</code>
	 */
	protected void processModuleRefs(Iterable<OMElement> moduleRefs)
			throws DeploymentException {
		try {
			for(OMElement moduleref: moduleRefs) {
				OMAttribute moduleRefAttribute = moduleref
						.getAttribute(new QName(TAG_REFERENCE));

				if (moduleRefAttribute != null) {
					String refName = moduleRefAttribute.getAttributeValue();

					if (axisConfig.getModule(refName) == null) {
						throw new DeploymentException(Messages.getMessage(
								DeploymentErrorMsgs.MODULE_NOT_FOUND, refName));
					} else {
						service.addModuleref(refName);
					}
				}
			}
		} catch (AxisFault axisFault) {
			throw new DeploymentException(axisFault);
		}
	}

	protected void processOperationModuleConfig(Iterable<OMElement> moduleConfigs,
			ParameterInclude parent, AxisOperation operation)
			throws DeploymentException {
		for(OMElement moduleConfig: moduleConfigs) {
			OMAttribute moduleName_att = moduleConfig.getAttribute(new QName(
					ATTRIBUTE_NAME));

			if (moduleName_att == null) {
				throw new DeploymentException(Messages
						.getMessage(DeploymentErrorMsgs.INVALID_MODULE_CONFIG));
			} else {
				String module = moduleName_att.getAttributeValue();
				ModuleConfiguration moduleConfiguration = new ModuleConfiguration(
						module, parent);
				Iterable<OMElement> parameters = moduleConfig
						.getChildrenWithName(new QName(TAG_PARAMETER));
				processParameters(parameters, moduleConfiguration, parent);
				operation.addModuleConfig(moduleConfiguration);
			}
		}
	}

	private List<AxisOperation> processOperations(Iterable<OMElement> operationsIter)
			throws AxisFault {
		List<AxisOperation> operations = new ArrayList<AxisOperation>();
		for(OMElement operationElem: operationsIter) {
			// getting operation name
			OMAttribute operationNameAttr
				= operationElem.getAttribute(new QName(ATTRIBUTE_NAME));
			if (operationNameAttr == null) {
				throw new DeploymentException(Messages.getMessage(Messages
						.getMessage(DeploymentErrorMsgs.INVALID_OP,
								"operation name missing")));
			}

			// setting the MEP of the operation
			OMAttribute operationMEPAttr = operationElem.getAttribute(new QName(TAG_MEP));
			String mepurl = null;

			if (operationMEPAttr != null) {
				mepurl = operationMEPAttr.getAttributeValue();
			}

			String opname = operationNameAttr.getAttributeValue();
			AxisOperation operation = null;

			// getting the namesapce from the attribute.
			OMAttribute operationNamespace = operationElem.getAttribute(new QName(
					ATTRIBUTE_NAMESPACE));
			if (operationNamespace != null) {
				String namespace = operationNamespace.getAttributeValue();
				operation = service.getOperation(new QName(namespace, opname));
			}
			if (operation == null) {
				operation = service.getOperation(new QName(opname));
			}

			if (operation == null) {
				operation = service.getOperation(new QName(service
						.getTargetNamespace(), opname));
			}
			if (operation == null) {
				if (mepurl == null) {
					// assumed MEP is in-out
					operation = new InOutAxisOperation();
					operation.setParent(service);

				} else {
					operation = AxisOperationFactory
							.getOperationDescription(mepurl);
				}
				operation.setName(new QName(opname));
				String MEP = operation.getMessageExchangePattern();
				if (WSDL2Constants.MEP_URI_IN_ONLY.equals(MEP)
						|| WSDL2Constants.MEP_URI_IN_OPTIONAL_OUT.equals(MEP)
						|| WSDL2Constants.MEP_URI_OUT_OPTIONAL_IN.equals(MEP)
						|| WSDL2Constants.MEP_URI_ROBUST_OUT_ONLY.equals(MEP)
						|| WSDL2Constants.MEP_URI_ROBUST_IN_ONLY.equals(MEP)
						|| WSDL2Constants.MEP_URI_IN_OUT.equals(MEP)) {
					AxisMessage inaxisMessage = operation
							.getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
					if (inaxisMessage != null) {
						inaxisMessage.setName(opname
								+ Java2WSDLConstants.MESSAGE_SUFFIX);
					}
				}

				if (WSDL2Constants.MEP_URI_OUT_ONLY.equals(MEP)
						|| WSDL2Constants.MEP_URI_OUT_OPTIONAL_IN.equals(MEP)
						|| WSDL2Constants.MEP_URI_IN_OPTIONAL_OUT.equals(MEP)
						|| WSDL2Constants.MEP_URI_ROBUST_OUT_ONLY.equals(MEP)
						|| WSDL2Constants.MEP_URI_IN_OUT.equals(MEP)) {
					AxisMessage outAxisMessage = operation
							.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
					if (outAxisMessage != null) {
						outAxisMessage.setName(opname
								+ Java2WSDLConstants.RESPONSE);
					}
				}
			}

			// setting the PolicyInclude

			// processing <wsp:Policy> .. </..> elements
			Iterable<OMElement> policyElements = operationElem.getChildrenWithName(new QName(
					POLICY_NS_URI, TAG_POLICY));

			if (policyElements != null) {
				processPolicyElements(policyElements, operation);
			}

			// processing <wsp:PolicyReference> .. </..> elements
			Iterable<OMElement> policyRefElements = operationElem
					.getChildrenWithName(new QName(POLICY_NS_URI,
							TAG_POLICY_REF));

			if (policyRefElements != null) {
				processPolicyRefElements(policyRefElements, operation);
			}

			// Operation Parameters
			Iterable<OMElement> parameters = operationElem.getChildrenWithName(new QName(
					TAG_PARAMETER));
			processParameters(parameters, operation, service);
			// To process wsamapping;
			processActionMappings(operationElem, operation);

			// loading the message receivers
			OMElement receiverElement = operationElem
					.getFirstChildWithName(new QName(TAG_MESSAGE_RECEIVER));

			if (receiverElement != null) {
				MessageReceiver messageReceiver = loadMessageReceiver(service
						.getClassLoader(), receiverElement);

				operation.setMessageReceiver(messageReceiver);
			} else {
				// setting default message receiver
				MessageReceiver msgReceiver = loadDefaultMessageReceiver(
						operation.getMessageExchangePattern(), service);
				operation.setMessageReceiver(msgReceiver);
			}

			// Process Module Refs
			Iterable<OMElement> modules = operationElem.getChildrenWithName(new QName(
					TAG_MODULE));

			processOperationModuleRefs(modules, operation);

			// processing Messages
			Iterable<OMElement> messages = operationElem.getChildrenWithName(new QName(
					TAG_MESSAGE));

			processMessages(messages, operation);

			// setting Operation phase
			if (axisConfig != null) {
				operation.setPhases(axisConfig.getPhasesInfo());
			}
			Iterable<OMElement> moduleConfigs = operationElem.getChildrenWithName(new QName(
					TAG_MODULE_CONFIG));
			processOperationModuleConfig(moduleConfigs, operation, operation);
			// adding the operation
			operations.add(operation);
		}
		return operations;
	}

	protected void processServiceModuleConfig(Iterable<OMElement> moduleConfigs,
			ParameterInclude parent, AxisService service)
			throws DeploymentException {
		for(OMElement moduleConfig: moduleConfigs) {
			OMAttribute moduleName_att = moduleConfig.getAttribute(new QName(
					ATTRIBUTE_NAME));

			if (moduleName_att == null) {
				throw new DeploymentException(Messages
						.getMessage(DeploymentErrorMsgs.INVALID_MODULE_CONFIG));
			} else {
				String module = moduleName_att.getAttributeValue();
				ModuleConfiguration moduleConfiguration = new ModuleConfiguration(
						module, parent);
				Iterable<OMElement> parameters = moduleConfig
						.getChildrenWithName(new QName(TAG_PARAMETER));

				processParameters(parameters, moduleConfiguration, parent);
				service.addModuleConfig(moduleConfiguration);
			}
		}
	}

	/*
	 * process data locator configuration for data retrieval.
	 */
	private void processDataLocatorConfig(OMElement dataLocatorElement,
			AxisService service) {
		OMAttribute serviceOverallDataLocatorclass = dataLocatorElement
				.getAttribute(new QName(DRConstants.CLASS_ATTRIBUTE));
		if (serviceOverallDataLocatorclass != null) {
			String className = serviceOverallDataLocatorclass
					.getAttributeValue();
			service.addDataLocatorClassNames(DRConstants.SERVICE_LEVEL,
					className);
		}

		for(OMElement locatorElement: dataLocatorElement.getChildrenWithName(new QName(DRConstants.DIALECT_LOCATOR_ELEMENT))) {
			OMAttribute dialect = locatorElement.getAttribute(new QName(
					DRConstants.DIALECT_ATTRIBUTE));
			OMAttribute dialectclass = locatorElement.getAttribute(new QName(
					DRConstants.CLASS_ATTRIBUTE));
			service.addDataLocatorClassNames(dialect.getAttributeValue(),
					dialectclass.getAttributeValue());
		}
	}

	public void setWsdlServiceMap(Map<String, AxisService> wsdlServiceMap) {
		this.wsdlServiceMap = wsdlServiceMap;
	}

	private void processEndpoints(AxisService axisService) throws AxisFault {
		String endpointName = axisService.getEndpointName();
		if (endpointName == null || endpointName.length() == 0) {
			Utils.addEndpointsToService(axisService, service.getConfiguration());
		}
	}

	private void processPolicyAttachments(OMElement serviceElement, AxisService service) throws DeploymentException {
		Iterable<OMElement> attachmentElements = serviceElement.getChildrenWithName(new QName(POLICY_NS_URI, TAG_POLICY_ATTACHMENT));
		try {
			Utils.processPolicyAttachments(attachmentElements, service);
		} catch (Exception e) {
			throw new DeploymentException(e);
		}
	}

}
