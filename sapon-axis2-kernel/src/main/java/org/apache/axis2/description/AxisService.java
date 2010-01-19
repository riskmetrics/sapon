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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.alt.ModuleConfigAccessor;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OldMessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.dataretrieval.AxisDataLocator;
import org.apache.axis2.dataretrieval.AxisDataLocatorImpl;
import org.apache.axis2.dataretrieval.DRConstants;
import org.apache.axis2.dataretrieval.Data;
import org.apache.axis2.dataretrieval.DataRetrievalException;
import org.apache.axis2.dataretrieval.DataRetrievalRequest;
import org.apache.axis2.dataretrieval.LocatorType;
import org.apache.axis2.dataretrieval.OutputForm;
import org.apache.axis2.dataretrieval.SchemaSupplier;
import org.apache.axis2.dataretrieval.WSDLSupplier;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.deployment.util.ExcludeInfo;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.deployment.util.Utils;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.hierarchy.ServiceGroupDescendant;
import org.apache.axis2.description.java2wsdl.DefaultSchemaGenerator;
import org.apache.axis2.description.java2wsdl.DocLitBareSchemaGenerator;
import org.apache.axis2.description.java2wsdl.Java2WSDLConstants;
import org.apache.axis2.description.java2wsdl.SchemaGenerator;
import org.apache.axis2.description.java2wsdl.TypeTable;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DefaultObjectSupplier;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.engine.ObjectSupplier;
import org.apache.axis2.engine.ServiceLifeCycle;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.phaseresolver.PhaseResolver;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.util.IOUtils;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.Loader;
import org.apache.axis2.util.LoggingControl;
import org.apache.axis2.util.XMLPrettyPrinter;
import org.apache.axis2.util.XMLUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class AxisService
 */
public class AxisService extends AxisDescriptionBase
	implements ModuleConfigAccessor, ServiceGroupDescendant
{

	// ////////////////////////////////////////////////////////////////
	// Standard Parameter names

	/**
	 * If this param is true, and the service has exactly one AxisOperation,
	 * normal operation dispatch (via URI/soapAction/etc) will not be necessary,
	 * and we'll just default to funneling all messages to that op. This is
	 * useful for passthrough/ESB/embedded applications.
	 */
	public static final String SUPPORT_SINGLE_OP = "supportSingleOperation";
	// ////////////////////////////////////////////////////////////////

	public static final String IMPORT_TAG = "import";
	public static final String INCLUDE_TAG = "include";
	public static final String SCHEMA_LOCATION = "schemaLocation";

	private Map<String, AxisEndpoint> endpointMap = new HashMap<String, AxisEndpoint>();

	/*
	 * This is a map between the QName of the element of a message specified in
	 * the WSDL and an Operation. It enables SOAP Body-based dispatching for
	 * doc-literal bindings.
	 */
	private Map<QName, AxisOperation> messageElementQNameToOperationMap = new HashMap<QName, AxisOperation>();

	private int nsCount = 0;
	private static final Log log = LogFactory.getLog(AxisService.class);
	private URL fileName;

	private AxisServiceGroup parent;

	// Maps httpLocations to corresponding operations. Used to dispatch rest
	// messages.
	private Map<String, AxisOperation> httpLocationDispatcherMap = null;

	// A map of (String alias, AxisOperation operation). The aliases might
	// include: SOAPAction,
	// WS-Addressing action, the operation name, the AxisInputMessage name. See:
	// - invalidOperationsAliases
	// - mapActionToOperatoin()
	// - getOperationByAction()
	// REVIEW: This really should be seperate maps for the different types of
	// aliases so they don't
	// conflict with each other. For example, so that an identical operation
	// name and soap action
	// on different operatoins don't cause a collision; the following can't be
	// routed because
	// "foo" is not unique across different operations:
	// operation 1: action = foo, name = bar
	// operation 2: action = bar, name = foo
	private Map<String, AxisOperation> operationsAliasesMap = null;

	// Collection of aliases that are invalid for this service because they are
	// duplicated across
	// multiple operations under this service.
	private List<String> invalidOperationsAliases = null;
	// private HashMap operations = new HashMap();

	// to store module ref at deploy time parsing
	private List<String> moduleRefs = null;

	// to keep the time that last update time of the service
	private long lastupdate;
	private Map<String, ModuleConfiguration> moduleConfigmap;
	private String name;
	private ClassLoader serviceClassLoader;

	// to keep the XMLScheam getting either from WSDL or java2wsdl
	private ArrayList<XmlSchema> schemaList;
	// private XmlSchema schema;

	// wsdl is there for this service or not (in side META-INF)
	private boolean wsdlFound = false;

	// to store the scope of the service
	private String scope;

	// to store default message receivers
	private HashMap<String, MessageReceiver> messageReceivers;

	// to set the handler chain available in phase info
	private boolean useDefaultChains = true;

	// to keep the status of the service , since service can stop at the run
	// time
	private boolean active = true;

	private boolean elementFormDefault = true;

	// to keep the service target name space
	private String targetNamespace = Java2WSDLConstants.DEFAULT_TARGET_NAMESPACE;
	private String targetNamespacePrefix = Java2WSDLConstants.TARGETNAMESPACE_PREFIX;

	// to store the target namespace for the schema
	private String schematargetNamespace;// = Java2WSDLConstants.AXIS2_XSD;
	private String schematargetNamespacePrefix = Java2WSDLConstants.SCHEMA_NAMESPACE_PRFIX;

	private boolean enableAllTransports = true;
	private List<String> exposedTransports = new ArrayList<String>();

	// To keep reference to ServiceLifeCycle instance , if the user has
	// specified in services.xml
	private ServiceLifeCycle serviceLifeCycle;

	/**
	 * Keeps track whether the schema locations are adjusted
	 */
	private boolean schemaLocationsAdjusted = false;

	private boolean wsdlImportLocationAdjusted = false;

	/**
	 * A table that keeps a mapping of unique xsd names (Strings) against the
	 * schema objects. This is populated in the first instance the schemas are
	 * asked for and then used to serve the subsequent requests
	 */
	private Map<String, XmlSchema> schemaMappingTable = null;

	/**
	 * counter variable for naming the schemas
	 */
	private int count = 0;
	/**
	 * A custom schema Name prefix. if set this will be used to modify the
	 * schema names
	 */
	private String customSchemaNamePrefix = null;

	/**
	 * A custom schema name suffix. will be attached to the schema file name
	 * when the files are uniquely named. A good place to add a file extension
	 * if needed
	 */
	private String customSchemaNameSuffix = null;

	// ///////////////////////////////////////
	// WSDL related stuff ////////////////////
	// //////////////////////////////////////

	/** Map of prefix -> namespaceURI */
	private Map<String, String> namespaceMap;

	private String soapNsUri;
	private String endpointName;
	//private String endpointURL;

	private List<String> importedNamespaces;

	private boolean clientSide = false;

	// To keep a ref to ObjectSupplier instance
	private ObjectSupplier objectSupplier;

	// package to namespace mapping
	private Map<String, String> p2nMap;

	// to keep the exclude property details
	private ExcludeInfo excludeInfo;

	private TypeTable typeTable;

	// Data Locators for WS-Mex Support
	private Map<String, AxisDataLocator> dataLocators;
	private Map<String, String> dataLocatorClassNames;
	private AxisDataLocatorImpl defaultDataLocator;
	// Define search sequence for datalocator based on Data Locator types.
	LocatorType[] availableDataLocatorTypes = new LocatorType[] {
			LocatorType.SERVICE_DIALECT, LocatorType.SERVICE_LEVEL,
			LocatorType.GLOBAL_DIALECT, LocatorType.GLOBAL_LEVEL,
			LocatorType.DEFAULT_AXIS };

	// name of the binding used : use in codegeneration
	private String bindingName;

	// List of MessageContextListeners that listen for events on the MessageContext
	private CopyOnWriteArrayList<MessageContextListener> messageContextListeners =
		new CopyOnWriteArrayList<MessageContextListener>();

	// names list keep to preserve the parameter order
	private List<QName> operationsNameList;

	private String[] eprs;
	private boolean customWsdl = false;

	private Map<String, Policy> policyMap = new HashMap<String, Policy>();

	public AxisEndpoint getEndpoint(String key) {
		return endpointMap.get(key);
	}

	public void addEndpoint(String key, AxisEndpoint axisEndpoint) {
		this.endpointMap.put(key, axisEndpoint);
	}

	private final Map<QName, AxisOperation> operations
		= new HashMap<QName, AxisOperation>();

	public boolean isSchemaLocationsAdjusted() {
		return schemaLocationsAdjusted;
	}

	public void setSchemaLocationsAdjusted(boolean schemaLocationsAdjusted) {
		this.schemaLocationsAdjusted = schemaLocationsAdjusted;
	}

	public Map<String, XmlSchema> getSchemaMappingTable() {
		return schemaMappingTable;
	}

	public void setSchemaMappingTable(Map<String, XmlSchema> schemaMappingTable) {
		this.schemaMappingTable = schemaMappingTable;
	}

	public String getCustomSchemaNamePrefix() {
		return customSchemaNamePrefix;
	}

	public void setCustomSchemaNamePrefix(String customSchemaNamePrefix) {
		this.customSchemaNamePrefix = customSchemaNamePrefix;
	}

	public String getCustomSchemaNameSuffix() {
		return customSchemaNameSuffix;
	}

	public void setCustomSchemaNameSuffix(String customSchemaNameSuffix) {
		this.customSchemaNameSuffix = customSchemaNameSuffix;
	}

	/**
	 * Constructor AxisService.
	 */
	public AxisService() {
		super();
		this.operationsAliasesMap = new HashMap<String, AxisOperation>();
		this.invalidOperationsAliases = new ArrayList<String>();
		moduleConfigmap = new HashMap<String, ModuleConfiguration>();
		// by default service scope is for the request
		scope = Constants.SCOPE_REQUEST;
		httpLocationDispatcherMap = new HashMap<String, AxisOperation>();
		messageReceivers = new HashMap<String, MessageReceiver>();
		moduleRefs = new ArrayList<String>();
		schemaList = new ArrayList<XmlSchema>();
		serviceClassLoader = AccessController.doPrivileged(
				new PrivilegedAction<ClassLoader>() {
					public ClassLoader run() {
						return Thread.currentThread().getContextClassLoader();
					}
				});
		objectSupplier = new DefaultObjectSupplier();
		dataLocators = new HashMap<String, AxisDataLocator>();
		dataLocatorClassNames = new HashMap<String, String>();
	}

	public String getBindingName() {
		return bindingName;
	}

	public void setBindingName(String bindingName) {
		this.bindingName = bindingName;
	}

	/**
	 * get the SOAPVersion
	 */
	public String getSoapNsUri() {
		return soapNsUri;
	}

	public void setSoapNsUri(String soapNsUri) {
		this.soapNsUri = soapNsUri;
	}

	/**
	 * get the endpointName
	 */
	public String getEndpointName() {
		return endpointName;
	}

	public void setEndpointName(String endpoint) {
		this.endpointName = endpoint;
	}

	/**
	 * Constructor AxisService.
	 */
	public AxisService(String name) {
		this();
		this.name = name;
	}

	@SuppressWarnings("deprecation")
	private static final String[][] mepGroups = {
		{	WSDL2Constants.MEP_URI_IN_ONLY,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_IN_ONLY,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_IN_ONLY
		},
		{	WSDL2Constants.MEP_URI_OUT_ONLY,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_OUT_ONLY,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_OUT_ONLY
		},
		{	WSDL2Constants.MEP_URI_IN_OUT,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_IN_OUT,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_IN_OUT
		},
		{
			WSDL2Constants.MEP_URI_IN_OPTIONAL_OUT,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_IN_OPTIONAL_OUT,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_IN_OPTIONAL_OUT
		},
		{
			WSDL2Constants.MEP_URI_OUT_IN,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_OUT_IN,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_OUT_IN
		},
		{
			WSDL2Constants.MEP_URI_OUT_OPTIONAL_IN,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_OUT_OPTIONAL_IN,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_OUT_OPTIONAL_IN
		},
		{
			WSDL2Constants.MEP_URI_ROBUST_OUT_ONLY,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_ROBUST_OUT_ONLY,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_ROBUST_OUT_ONLY
		},
		{
			WSDL2Constants.MEP_URI_ROBUST_IN_ONLY,
			WSDLConstants.WSDL20_2006Constants.MEP_URI_ROBUST_IN_ONLY,
			WSDLConstants.WSDL20_2004_Constants.MEP_URI_ROBUST_IN_ONLY
		}
	};

	public void addMessageReceiver(String mepURI, MessageReceiver messageReceiver) {
		for(String[] mepGroup: mepGroups) {
			for(String mep: mepGroup) {
				if(mep.equals(mepURI)) {
					for(String m: mepGroup) {
						messageReceivers.put(m, messageReceiver);
					}
					return;
				}
			}
		}
		//if we fall through to here, we didn't hit any of the predefined MEPs.
		messageReceivers.put(mepURI, messageReceiver);
	}

	public MessageReceiver getMessageReceiver(String mepURL) {
		return messageReceivers.get(mepURL);
	}

	/**
	 * Adds module configuration , if there is moduleConfig tag in service.
	 *
	 * @param moduleConfiguration
	 */
	public void addModuleConfig(ModuleConfiguration moduleConfiguration) {
		moduleConfigmap.put(moduleConfiguration.getModuleName(),
				moduleConfiguration);
	}

	/**
	 * Add any control operations defined by a Module to this service.
	 *
	 * @param module
	 *            the AxisModule which has just been engaged
	 * @throws AxisFault
	 *             if a problem occurs
	 */
	void addModuleOperations(AxisModule module) throws AxisFault {
		Map<QName, AxisOperation> map = module.getOperations();
		Collection<AxisOperation> col = map.values();
		PhaseResolver phaseResolver = new PhaseResolver(getConfiguration());
		for (AxisOperation axisOperation2 : col) {
			AxisOperation axisOperation = copyOperation(axisOperation2);
			if (this.getOperation(axisOperation.getName()) == null) {
				List<String> wsamappings = axisOperation.getWSAMappingList();
				if (wsamappings != null) {
					for(String mapping: wsamappings) {
						mapActionToOperation(mapping, axisOperation);
					}
				}
				// If we've set the "expose" parameter for this operation, it's
				// normal (non-control) and therefore it will appear in the
				// generated WSDL. If we haven't, it's a control operation and
				// will be ignored at WSDL-gen time.
				if (axisOperation
						.isParameterTrue(DeploymentConstants.TAG_EXPOSE)) {
					axisOperation.setControlOperation(false);
				} else {
					axisOperation.setControlOperation(true);
				}

				phaseResolver.engageModuleToOperation(axisOperation, module);

				this.addOperation(axisOperation);
			}
		}
	}

	public void addModuleref(String moduleref) {
		moduleRefs.add(moduleref);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.axis2.description.AxisService#addOperation(org.apache.axis2.description.AxisOperation)
	 */

	/**
	 * Method addOperation.
	 *
	 * @param axisOperation
	 */
	public void addOperation(AxisOperation axisOperation) {
		axisOperation.setParent(this);

		if (log.isDebugEnabled()) {
			if (axisOperation.getName().equals(ServiceClient.ANON_OUT_ONLY_OP)
					|| (axisOperation.getName().equals(ServiceClient.ANON_OUT_ONLY_OP))
					|| (axisOperation.getName().equals(ServiceClient.ANON_OUT_ONLY_OP))) {
				log.debug("Client-defined operation name matches default operation name. "
						+ "this may cause interoperability issues.  Name is: " + axisOperation.getName().toString());
			}
		}

		Iterator<AxisModule> modules = getEngagedModules().iterator();

		while (modules.hasNext()) {
			AxisModule module = modules.next();
			try {
				axisOperation.engageModule(module);
			} catch (AxisFault axisFault) {
				log.info(Messages.getMessage("modulealredyengagetoservice",
						module.getName()));
			}
		}
		if (axisOperation.getMessageReceiver() == null) {
			axisOperation.setMessageReceiver(loadDefaultMessageReceiver(
					axisOperation.getMessageExchangePattern(), this));
		}
		if (axisOperation.getInputAction() == null) {
			axisOperation.setSoapAction("urn:"
					+ axisOperation.getName().getLocalPart());
		}

		if (axisOperation.getOutputAction() == null) {
			axisOperation.setOutputAction("urn:"
					+ axisOperation.getName().getLocalPart()
					+ Java2WSDLConstants.RESPONSE);
		}
		this.operations.put(axisOperation.getName(), axisOperation);

		String operationName = axisOperation.getName().getLocalPart();

		/*
		 * Some times name of the operation can be different from the name of
		 * the first child of the SOAPBody. This will put the correct mapping
		 * associating that name with the operation. This will be useful
		 * especially for the SOAPBodyBasedDispatcher
		 */

		for (final AxisMessage axisMessage: axisOperation.getMessages()) {
			String messageName = axisMessage.getName();
			if (messageName != null && !messageName.equals(operationName)) {
				mapActionToOperation(messageName, axisOperation);
			}
		}

		mapActionToOperation(operationName, axisOperation);

		String action = axisOperation.getInputAction();
		if (action.length() > 0) {
			mapActionToOperation(action, axisOperation);
		}

		List<String> wsamappings = axisOperation.getWSAMappingList();
		if (wsamappings != null) {
			for(String mapping: wsamappings) {
				mapActionToOperation(mapping, axisOperation);
			}
		}

		if (axisOperation.getMessageReceiver() == null) {
			axisOperation.setMessageReceiver(loadDefaultMessageReceiver(
					axisOperation.getMessageExchangePattern(), this));
		}
	}

	private MessageReceiver loadDefaultMessageReceiver(String mepURL,
			AxisService service) {
		MessageReceiver messageReceiver;
		if (mepURL == null) {
			mepURL = WSDL2Constants.MEP_URI_IN_OUT;
		}
		if (service != null) {
			messageReceiver = service.getMessageReceiver(mepURL);
			if (messageReceiver != null) {
				return messageReceiver;
			}
		}
		if (getConfiguration() != null) {
			return getConfiguration().getMessageReceiver(mepURL);
		}
		return null;
	}

	/**
	 * Gets a copy from module operation.
	 *
	 * @param axisOperation
	 * @return Returns AxisOperation.
	 * @throws AxisFault
	 */
	private AxisOperation copyOperation(AxisOperation axisOperation)
	throws AxisFault {
		AxisOperation operation = AxisOperationFactory
		.getOperationDescription(axisOperation
				.getMessageExchangePattern());

		operation.setMessageReceiver(axisOperation.getMessageReceiver());
		operation.setName(axisOperation.getName());

		for(Parameter parameter: axisOperation.getParameters()) {
			operation.addParameter(parameter);
		}

		operation.attachPolicyComponents(axisOperation.getAttachedPolicyComponents());
		operation.setWsamappingList(axisOperation.getWSAMappingList());
		operation.setRemainingPhasesInFlow(axisOperation
				.getRemainingPhasesInFlow());
		operation.setPhasesInFaultFlow(axisOperation.getPhasesInFaultFlow());
		operation.setPhasesOutFaultFlow(axisOperation.getPhasesOutFaultFlow());
		operation.setPhasesOutFlow(axisOperation.getPhasesOutFlow());

		operation.setOutputAction(axisOperation.getOutputAction());
		String[] faultActionNames = axisOperation.getFaultActionNames();
		for (String faultActionName : faultActionNames) {
			operation.addFaultAction(faultActionName, axisOperation
					.getFaultAction(faultActionName));
		}

		return operation;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.axis2.description.AxisService#addToengagedModules(javax.xml.namespace.QName)
	 */

	/**
	 * Engages a module. It is required to use this method.
	 *
	 * @param axisModule
	 * @param engager
	 */
	@Override
	public void onEngage(AxisModule axisModule, AxisDescription engager)
	throws AxisFault {
		// adding module operations
		addModuleOperations(axisModule);

		for(final AxisOperation axisOperation: getOperations()) {
			axisOperation.engageModule(axisModule, engager);
		}
	}

	/**
	 * Maps an alias (such as a SOAPAction, WSA action, or an operation name) to
	 * the given AxisOperation. This is used by dispatching (both SOAPAction-
	 * and WSAddressing- based dispatching) to figure out which operation a
	 * given message is for. Some notes on restrictions of "action" - A null or
	 * empty action will be ignored - An action that is a duplicate and
	 * references an idential operation is allowed - An acton that is a
	 * duplicate and references a different operation is NOT allowed. In this
	 * case, the action for the original operation is removed from the alias
	 * table, thus removing the ability to route based on this action. This is
	 * necessary to prevent mis-directing incoming message to the wrong
	 * operation based on SOAPAction.
	 *
	 * Note that an alias could be a SOAPAction, WS-Addressing Action, the
	 * operation name, or some other alias.
	 *
	 * @see #getOperationByAction(String)
	 *
	 * @param action
	 *            the alias key
	 * @param axisOperation
	 *            the operation to map to
	 */
	public void mapActionToOperation(String action, AxisOperation axisOperation) {
		if (action == null || "".equals(action)) {
			if (log.isDebugEnabled()) {
				log
				.debug("mapActionToOperation: A null or empty action cannot be used to map to an operation.");
			}
			return;
		}
		if (log.isDebugEnabled()) {
			log
			.debug("mapActionToOperation: Mapping Action to Operation: action: "
					+ action
					+ "; operation: "
					+ axisOperation
					+ "named: " + axisOperation.getName());
		}

		// First check if this action has already been flagged as invalid
		// because it is a duplicate.
		if (invalidOperationsAliases.contains(action)) {
			// This SOAPAction has already been determined to be invalid; log a
			// message
			// and do not add it to the operation alias map.
			if (log.isDebugEnabled()) {
				log
				.debug("mapActionToOperation: The action: "
						+ action
						+ " can not be used for operation: "
						+ axisOperation
						+ " with operation name: "
						+ axisOperation.getName()
						+ " because that SOAPAction is not unique for this service.");
			}
			return;
		}

		// Check if the action is currently mapping to an operation.
		AxisOperation currentlyMappedOperation = getOperationByAction(action);
		if (currentlyMappedOperation != null) {
			if (currentlyMappedOperation == axisOperation) {
				// This maps to the same operation, then it is already in the
				// alias table, so
				// just silently ignore this mapping request.
				if (log.isDebugEnabled()) {
					log
					.debug("mapActionToOperation: This operation is already mapped to this action: "
							+ action
							+ "; AxisOperation: "
							+ currentlyMappedOperation
							+ " named: "
							+ currentlyMappedOperation.getName());
				}
			} else {
				// This action is already mapped, but it is to a different
				// operation. Remove
				// the action mapping from the alias table and add it to the
				// list of invalid mappings
				operationsAliasesMap.remove(action);
				invalidOperationsAliases.add(action);
				if (log.isDebugEnabled()) {
					log
					.debug("mapActionToOperation: The action is already mapped to a different "
							+ "operation.  The mapping of the action to any operations will be "
							+ "removed.  Action: "
							+ action
							+ "; original operation: "
							+ currentlyMappedOperation
							+ " named "
							+ currentlyMappedOperation.getName()
							+ "; new operation: "
							+ axisOperation
							+ " named " + axisOperation.getName());
				}
			}
		} else {
			operationsAliasesMap.put(action, axisOperation);
			// Adding operation name to the mapping table
			// operationsAliasesMap.put(axisOperation.getName().getLocalPart(),
			// axisOperation);
		}
	}

	/**
	 * Maps an constant string in the whttp:location to the given operation.
	 * This is used by RequestURIOperationDispatcher based dispatching to figure
	 * out which operation it is that a given message is for.
	 *
	 * @param string
	 *            the constant drawn from whttp:location
	 * @param axisOperation
	 *            the operation to map to
	 */
	public void addHttpLocationDispatcherString(String string,
			AxisOperation axisOperation) {
		httpLocationDispatcherMap.put(string, axisOperation);
	}

	public void printSchema(OutputStream out) throws AxisFault {
		for (int i = 0; i < schemaList.size(); i++) {
			XmlSchema schema = addNameSpaces(i);
			schema.write(out);
		}
	}

	public XmlSchema getSchema(int index) {
		return addNameSpaces(index);
	}

	/**
	 * Release the list of schema objects. <p/> In some environments, this can
	 * provide significant relief of memory consumption in the java heap, as
	 * long as the need for the schema list has completed.
	 */
	public void releaseSchemaList() {
		if (schemaList != null) {
			// release the schema list
			schemaList.clear();
		}

		if (log.isDebugEnabled()) {
			log.debug("releaseSchemaList: schema list has been released.");
		}
	}

	private XmlSchema addNameSpaces(int i) {
		XmlSchema schema = schemaList.get(i);
		NamespaceMap map = new NamespaceMap(namespaceMap);
		NamespacePrefixList namespaceContext = schema.getNamespaceContext();
		String prefixes[] = namespaceContext.getDeclaredPrefixes();
		for (String prefix : prefixes) {
			map.add(prefix, namespaceContext.getNamespaceURI(prefix));
		}
		schema.setNamespaceContext(map);
		return schema;
	}

	public void setEPRs(String[] eprs) {
		this.eprs = eprs;
	}

	public String[] getEPRs() {
		if (eprs != null && eprs.length != 0) {
			return eprs;
		}
		eprs = calculateEPRs();
		return eprs;
	}

	private String[] calculateEPRs() {
		try {
			String requestIP = org.apache.axis2.util.Utils.getIpAddress(getConfiguration());
			return calculateEPRs(requestIP);
		} catch (SocketException e) {
			log.error("Cannot get local IP address", e);
		}
		return new String[0];
	}

	private String[] calculateEPRs(String requestIP) {
		AxisConfiguration axisConfig = getConfiguration();
		if (axisConfig == null) {
			return null;
		}
		ArrayList<String> eprList = new ArrayList<String>();
		if (enableAllTransports) {
			for (TransportInDescription transportInDescription : axisConfig.getTransportsIn().values()) {
				TransportInDescription transportIn = transportInDescription;
				addEPR(transportIn, eprList, requestIP);
			}
		} else {
			for (String trsName: this.exposedTransports) {
				TransportInDescription transportIn
					= axisConfig.getTransportIn(trsName);
				addEPR(transportIn, eprList, requestIP);
			}
		}
		eprs = eprList.toArray(new String[eprList.size()]);
		return eprs;
	}

	private void addEPR(	TransportInDescription transportIn,
							List<String> eprList,
							String requestIP )
	{
		TransportListener listener = transportIn.getReceiver();
		try {
			EndpointReference[] eprsForService
				= listener.getEPRsForService(this.name, requestIP);
			for (EndpointReference endpointReference : eprsForService) {
				eprList.add(endpointReference.getAddress());
			}
		} catch (AxisFault axisFault) {
			log.warn(axisFault.getMessage());
		}
	}

	private void printDefinitionObject(Definition definition, OutputStream out,
			String requestIP) throws AxisFault, WSDLException {
		if (isModifyUserWSDLPortAddress()) {
			setPortAddress(definition, requestIP);
		}
		if (!wsdlImportLocationAdjusted) {
			changeImportAndIncludeLocations(definition);
			wsdlImportLocationAdjusted = true;
		}
		WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();
		writer.writeWSDL(definition, out);
	}

	public void printUserWSDL(OutputStream out, String wsdlName)
	throws AxisFault {
		Definition definition = null;
		// first find the correct wsdl definition
		Parameter wsdlParameter = getParameter(WSDLConstants.WSDL_4_J_DEFINITION);
		if (wsdlParameter != null) {
			definition = (Definition) wsdlParameter.getValue();
		}

		if (definition != null) {
			try {
				printDefinitionObject(getWSDLDefinition(definition, wsdlName),
						out, null);
			} catch (WSDLException e) {
				throw AxisFault.makeFault(e);
			}
		} else {
			printWSDLError(out);
		}

	}

	/**
	 * Find the definition object for given name
	 *
	 * @param parentDefinition
	 * @param name
	 * @return wsdl definition
	 */
	private Definition getWSDLDefinition(Definition parentDefinition, String name)
	{
		if (name == null) {
			return parentDefinition;
		}

		Definition importedDefinition = null;
	    for(List<Import> values: parentDefinition.getImports().values()) {
			for (Import wsdlImport: values) {
				if (wsdlImport.getLocationURI().endsWith(name)) {
					importedDefinition = wsdlImport.getDefinition();
					break;
				} else {
					importedDefinition = getWSDLDefinition(wsdlImport
							.getDefinition(), name);
				}
				if (importedDefinition != null) {
					break;
				}
			}
			if (importedDefinition != null) {
				break;
			}
		}
		return importedDefinition;
	}

	/**
	 * this procesdue recursively adjust the wsdl imports locations and the
	 * schmea import and include locations.
	 *
	 * @param definition
	 */
	private void changeImportAndIncludeLocations(Definition definition) throws AxisFault {

		// adjust the schema locations in types section
		Types types = definition.getTypes();
		if (types != null) {
			List<?> extensibilityElements = types.getExtensibilityElements();
			for (Object extensibilityElement: extensibilityElements) {
				if (extensibilityElement instanceof Schema) {
					Schema schema = (Schema) extensibilityElement;
					changeLocations(schema.getElement());
				}
			}
		}

		for(List<Import> values: definition.getImports().values()) {
			for (Import wsdlImport: values) {
				String originalImportString = wsdlImport.getLocationURI();
				if (originalImportString.indexOf("://") == -1 && originalImportString.indexOf("?wsdl=") == -1){
					wsdlImport.setLocationURI(this.getServiceEPR() + "?wsdl=" + originalImportString);
				}
				changeImportAndIncludeLocations(wsdlImport.getDefinition());
			}
		}

	}

	/**
	 * change the schema Location in the elemment
	 *
	 * @param element
	 */

	private void changeLocations(Element element) throws AxisFault {
		NodeList nodeList = element.getChildNodes();
		String tagName;
		for (int i = 0; i < nodeList.getLength(); i++) {
			tagName = nodeList.item(i).getLocalName();
			if (IMPORT_TAG.equals(tagName) || INCLUDE_TAG.equals(tagName)) {
				processImport(nodeList.item(i));
			}
		}
	}

	private void updateSchemaLocation(XmlSchema schema) throws AxisFault {
		XmlSchemaObjectCollection includes = schema.getIncludes();
		for (int j = 0; j < includes.getCount(); j++) {
			Object item = includes.getItem(j);
			if (item instanceof XmlSchemaExternal) {
				XmlSchemaExternal xmlSchemaExternal = (XmlSchemaExternal) item;
				XmlSchema s = xmlSchemaExternal.getSchema();
				updateSchemaLocation(s, xmlSchemaExternal);
			}
		}
	}

	private void updateSchemaLocation(XmlSchema s, XmlSchemaExternal xmlSchemaExternal) throws AxisFault {
		if (s != null) {
			String schemaLocation = xmlSchemaExternal.getSchemaLocation();

			if (schemaLocation.indexOf("://") == -1 && schemaLocation.indexOf("?xsd=") == -1) {
				String newscheamlocation = this.getServiceEPR() + "?xsd=" + schemaLocation;
				xmlSchemaExternal.setSchemaLocation(newscheamlocation);
			}
		}
	}

	private void processImport(Node importNode) throws AxisFault {
		NamedNodeMap nodeMap = importNode.getAttributes();
		Node attribute;
		String attributeValue;
		for (int i = 0; i < nodeMap.getLength(); i++) {
			attribute = nodeMap.item(i);
			if (attribute.getNodeName().equals("schemaLocation")) {
				attributeValue = attribute.getNodeValue();
				if (attributeValue.indexOf("://") == -1 && attributeValue.indexOf("?xsd=") == -1) {
					attribute.setNodeValue(this.getServiceEPR() + "?xsd=" + attributeValue);
				}
			}
		}
	}

	private String getServiceEPR() {
		String serviceEPR = null;
		Parameter parameter = this.getParameter(Constants.Configuration.GENERATE_ABSOLUTE_LOCATION_URIS);
		if ((parameter != null) && JavaUtils.isTrueExplicitly(parameter.getValue())) {
			String[] eprs = this.getEPRs();
			for (String epr : eprs) {
				if ((epr != null) && (epr.startsWith("http:"))){
					serviceEPR = epr;
					break;
				}
			}
			if (serviceEPR == null){
				serviceEPR = eprs[0];
			}
		} else {
			serviceEPR = this.name;
		}
		if (serviceEPR.endsWith("/")){
			serviceEPR = serviceEPR.substring(0, serviceEPR.lastIndexOf("/"));
		}
		return serviceEPR;
	}

	/**
	 * Produces a XSD for this AxisService and prints it to the specified
	 * OutputStream.
	 *
	 * @param out
	 *            destination stream.
	 * @param xsd
	 *            schema name
	 * @return -1 implies not found, 0 implies redirect to root, 1 implies
	 *         found/printed a schema
	 * @throws IOException
	 */
	public int printXSD(OutputStream out, String xsd)
		throws IOException, AxisFault
	{
		// If we find a SchemaSupplier, use that
		SchemaSupplier supplier = (SchemaSupplier) getParameterValue("SchemaSupplier");
		if (supplier != null) {
			XmlSchema schema = supplier.getSchema(this, xsd);
			if (schema != null) {
				updateSchemaLocation(schema);
				schema.write(new OutputStreamWriter(out, "UTF8"));
				out.flush();
				out.close();
				return 1;
			}
		}

		// call the populator
		populateSchemaMappings();
		Map<String, XmlSchema> schemaMappingtable = getSchemaMappingTable();
		List<XmlSchema> schemas = getSchema();

		// a name is present - try to pump the requested schema
		if (!"".equals(xsd)) {
			XmlSchema schema = schemaMappingtable.get(xsd);
			if (schema == null) {
				int dotIndex = xsd.indexOf('.');
				if (dotIndex > 0) {
					String schemaKey = xsd.substring(0, dotIndex);
					schema = schemaMappingtable.get(schemaKey);
				}
			}
			if (schema != null) {
				// schema is there - pump it outs
				schema.write(new OutputStreamWriter(out, "UTF8"));
				out.flush();
				out.close();
			} else {
				// make sure we are only serving .xsd files and ignore requests with
				// ".." in the name.
				if (xsd.endsWith(".xsd") && xsd.indexOf("..") == -1) {
					InputStream in = getClassLoader().getResourceAsStream(
							DeploymentConstants.META_INF + "/" + xsd);
					if (in != null) {
						IOUtils.copy(in, out, true);
					} else {
						// Can't find the schema
						return -1;
					}
				} else {
					// bad schema request
					return -1;
				}
			}
		} else if (schemas.size() > 1) {
			// multiple schemas are present and the user specified
			// no name - in this case we cannot possibly pump a schema
			// so redirect to the service root
			return 0;
		} else {
			// user specified no name and there is only one schema
			// so pump that out
			List<XmlSchema> list = getSchema();
			if (list.size() > 0) {
				XmlSchema schema = getSchema(0);
				if (schema != null) {
					schema.write(new OutputStreamWriter(out, "UTF8"));
					out.flush();
					out.close();
				}
			} else {
				String xsdNotFound = "<error>"
					+ "<description>Unable to access schema for this service</description>"
					+ "</error>";
				out.write(xsdNotFound.getBytes());
				out.flush();
				out.close();
			}
		}
		return 1;
	}

	/**
	 * Produces a WSDL for this AxisService and prints it to the specified
	 * OutputStream.
	 *
	 * @param out
	 *            destination stream. The WSDL will be sent here.
	 * @param requestIP
	 *            the hostname the WSDL request was directed at. This should be
	 *            the address that appears in the generated WSDL.
	 * @throws AxisFault
	 *             if an error occurs
	 */
	public void printWSDL(OutputStream out, String requestIP) throws AxisFault {
		// If we're looking for pre-existing WSDL, use that.
		if (isUseUserWSDL()) {
			printUserWSDL(out, null);
			return;
		}

		// If we find a WSDLSupplier, use that
		WSDLSupplier supplier = (WSDLSupplier) getParameterValue("WSDLSupplier");
		if (supplier != null) {
			try {
				Definition definition = supplier.getWSDL(this);
				if (definition != null) {
					changeImportAndIncludeLocations(definition);
					printDefinitionObject(getWSDLDefinition(definition, null),
							out, requestIP);
				}
			} catch (Exception e) {
				printWSDLError(out, e);
			}
			return;
		}

		// Otherwise, generate WSDL ourselves
		String[] eprArray = requestIP == null ? new String[] { this.endpointName }
		                                      : calculateEPRs(requestIP);
		getWSDL(out, eprArray);
	}

	/**
	 * Print the WSDL with a default URL. This will be called only during
	 * codegen time.
	 *
	 * @param out
	 * @throws AxisFault
	 */
	public void printWSDL(OutputStream out) throws AxisFault {
		printWSDL(out, null);
	}

	private AxisEndpoint getAxisEndpoint(String port) {
		// if service has a single endpoint, this will cause the [serviceName] address
		// to be used in wsdl instead of the [serviceName].[endpointName]
		if (endpointMap.size() == 1 && endpointMap.containsKey(getEndpointName())) {
			return null;
		} else {
			return endpointMap.get(port);
		}
	}

	private void setPortAddress(Definition definition, String requestIP)
		throws AxisFault
	{
		for(final Service serviceElement: definition.getServices().values()) {
			for(final Port port: serviceElement.getPorts().values()) {
				AxisEndpoint endpoint = getAxisEndpoint(port.getName());
				for(final Object extensibilityEle: port.getExtensibilityElements()) {
					if (extensibilityEle instanceof SOAPAddress) {
						SOAPAddress soapAddress = (SOAPAddress) extensibilityEle;
						soapAddress.setLocationURI(
								findLocationURI(endpoint,
												soapAddress.getLocationURI(),
												requestIP));
					} else if (extensibilityEle instanceof SOAP12Address) {
						SOAP12Address soapAddress = (SOAP12Address) extensibilityEle;
						soapAddress.setLocationURI(
								findLocationURI(endpoint,
												soapAddress.getLocationURI(),
												requestIP));
					} else if (extensibilityEle instanceof HTTPAddress) {
						HTTPAddress httpAddress = (HTTPAddress) extensibilityEle;
						httpAddress.setLocationURI(
								findLocationURI(endpoint,
												httpAddress.getLocationURI(),
												requestIP));
					}
					// TODO : change the Endpoint refrence addess as well.
				}
			}
		}
	}

	private String findLocationURI(	final AxisEndpoint endpoint,
									final String existingAddress,
									final String requestIP)
		throws AxisFault
	{
		if (endpoint != null) {
			return endpoint.calculateEndpointURL();
		} else if (existingAddress == null || existingAddress.equals("REPLACE_WITH_ACTUAL_URL")) {
			return getEPRs()[0];
		} else if (requestIP == null) {
			return getLocationURI(getEPRs(), existingAddress);
		} else {
			return getLocationURI(calculateEPRs(requestIP), existingAddress);
		}
	}

	/**
	 * this method returns the new IP address corresponding to the already
	 * existing ip
	 *
	 * @param eprs
	 * @param epr
	 * @return corresponding Ip address
	 */
	private String getLocationURI(String[] eprs, String epr)
		throws AxisFault
	{
		String returnIP = null;
		if (epr == null) {
			throw new AxisFault("No epr is given in the wsdl port");
		}

		final int colonIndex = epr.indexOf(':');
		if (colonIndex < 0) {
			throw new AxisFault("invalid epr is given epr ==> " + epr);
		}

		final String existingProtocol = epr.substring(0, colonIndex).trim();
		String eprProtocol;
		for (String epr2 : eprs) {
			eprProtocol = epr2.substring(0, epr2.indexOf(":")).trim();
			if (eprProtocol.equals(existingProtocol)) {
				returnIP = epr2;
				break;
			}
		}
		if (returnIP != null) {
			return returnIP;
		} else {
			throw new AxisFault(
					"Server does not have an epr for the wsdl epr==> " + epr);
		}
	}

	private void getWSDL(OutputStream out, String[] serviceURL)
		throws AxisFault
	{
		// Retrieve WSDL using the same data retrieval path for GetMetadata
		// request.
		DataRetrievalRequest request = new DataRetrievalRequest();
		request.putDialect(DRConstants.SPEC.DIALECT_TYPE_WSDL);
		request.putOutputForm(OutputForm.INLINE_FORM);

		MessageContext context = new OldMessageContext();
		context.setAxisService(this);
		context.setTo(new EndpointReference(serviceURL[0]));

		Data[] result = getData(request, context);
		OMElement wsdlElement;
		if (result != null && result.length > 0) {
			wsdlElement = (OMElement) (result[0].getData());
			try {
				XMLPrettyPrinter.prettify(wsdlElement, out);
				out.flush();
				out.close();
			} catch (Exception e) {
				throw AxisFault.makeFault(e);
			}
		}
	}

	private void printWSDLError(OutputStream out) throws AxisFault {
		printWSDLError(out, null);
	}

	private void printWSDLError(OutputStream out, Exception e) throws AxisFault {
		try {
			String wsdlntfound = "<error>"
				+ "<description>Unable to generate WSDL 1.1 for this service</description>"
				+ "<reason>If you wish Axis2 to automatically generate the WSDL 1.1, then please "
				+ "set useOriginalwsdl as false in your services.xml</reason>";
			out.write(wsdlntfound.getBytes());
			if (e != null) {
				PrintWriter pw = new PrintWriter(out);
				e.printStackTrace(pw);
				pw.flush();
			}
			out.write("</error>".getBytes());
			out.flush();
			out.close();
		} catch (IOException ex) {
			throw AxisFault.makeFault(ex);
		}
	}

	/**
	 * Gets the description about the service which is specified in
	 * services.xml.
	 *
	 * @return Returns String.
	 * @deprecated Use getDocumentation() instead
	 */
	@Deprecated
	public String getServiceDescription() {
		return getDocumentation();
	}

	/**
	 * Method getClassLoader.
	 *
	 * @return Returns ClassLoader.
	 */
	public ClassLoader getClassLoader() {
		return this.serviceClassLoader;
	}

	/**
	 * Gets the control operation which are added by module like RM.
	 */
	public List<AxisOperation> getControlOperations() {
		final List<AxisOperation> operationList
		= new ArrayList<AxisOperation>();

		for(final AxisOperation operation: getOperations()) {
			if (operation.isControlOperation()) {
				operationList.add(operation);
			}
		}

		return operationList;
	}

	public URL getFileName() {
		return fileName;
	}

	public long getLastUpdate() {
		return lastupdate;
	}

	public ModuleConfiguration getModuleConfig(String moduleName) {
		return moduleConfigmap.get(moduleName);
	}

	public List<String> getModules() {
		return moduleRefs;
	}

	public String getName() {
		return name;
	}

	/**
	 * Method getOperation.
	 *
	 * @param operationName
	 * @return Returns AxisOperation.
	 */
	public AxisOperation getOperation(QName operationName) {
		if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
			log.debug("Get operation for " + operationName);
		}

		AxisOperation axisOperation = operations.get(operationName);

		if (axisOperation == null) {
			axisOperation = operations.get(new QName(
					getTargetNamespace(), operationName.getLocalPart()));

			if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
				log.debug("Target namespace: " + getTargetNamespace());
			}
		}

		if (axisOperation == null) {
			axisOperation
				= operationsAliasesMap.get(operationName.getLocalPart());

			if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
				log.debug("Operations aliases map: " + operationsAliasesMap);
			}
		}

		//The operation may be associated with a namespace other than the
		//target namespace, e.g. if the operation is from an imported wsdl.
		if (axisOperation == null) {
			List<String> namespaces = getImportedNamespaces();

			if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
				log.debug("Imported namespaces: " + namespaces);
			}

			if (namespaces != null) {
				for(final String namespace: namespaces) {
					axisOperation = operations.get(new QName(
							namespace, operationName.getLocalPart()));

					if (axisOperation != null) {
						break;
					}
				}
			}
		}

		if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
			log.debug("Found axis operation:  " + axisOperation);
		}

		return axisOperation;
	}

	/**
	 * Returns the AxisOperation which has been mapped to the given alias.
	 *
	 * @see #mapActionToOperation(String, AxisOperation)
	 *
	 * @param action
	 *            the alias key
	 * @return Returns the corresponding AxisOperation or null if it isn't
	 *         found.
	 */
	public AxisOperation getOperationByAction(String action) {
		return operationsAliasesMap.get(action);
	}

	/**
	 * Returns the operation given a SOAP Action. This method should be called
	 * if only one Endpoint is defined for this Service. If more than one
	 * Endpoint exists, one of them will be picked. If more than one Operation
	 * is found with the given SOAP Action; null will be returned. If no
	 * particular Operation is found with the given SOAP Action; null will be
	 * returned. If the action is in the list of invaliad aliases, which means
	 * it did not uniquely identify an operation, a null will be returned.
	 *
	 * @param soapAction
	 *            SOAP Action defined for the particular Operation
	 * @return Returns an AxisOperation if a unique Operation can be found with
	 *         the given SOAP Action otherwise will return null.
	 */
	public AxisOperation getOperationBySOAPAction(String soapAction) {

		// Check for illegal soapActions
		if ((soapAction == null) || soapAction.length() == 0) {
			if (log.isDebugEnabled()) {
				log.debug("getOperationBySOAPAction: " + soapAction
						+ " is null or ''. Returning null.");
			}
			return null;
		}

		// If the action maps to an alais that is not unique, then it can't be
		// used to map to
		// an operation.
		if (invalidOperationsAliases.contains(soapAction)) {
			if (log.isDebugEnabled()) {
				log.debug("getOperationBySOAPAction: " + soapAction
						+ " is an invalid operation alias. Returning null.");
			}
			return null;
		}

		// Get the operation from the action->operation map
		AxisOperation operation = operationsAliasesMap
		.get(soapAction);

		if (operation != null) {
			if (log.isDebugEnabled()) {
				log.debug("getOperationBySOAPAction: Operation (" + operation
						+ "," + operation.getName() + ") for soapAction: "
						+ soapAction + " found in action map.");
			}
			return operation;
		}

		// The final fallback is to check the operations for a matching name.

		// I could not find any spec statement that explicitly forbids using a
		// short name in the SOAPAction header or wsa:Action element,
		// so I believe this to be valid. There may be customers using the
		// shortname as the SOAPAction in their client code that would
		// also require this support.
		for(final AxisOperation op: getOperations()) {
			if (op.getName().getLocalPart().equals(soapAction)) {
				operation = op;
				break;
			}
		}

		if (operation != null) {
			if (log.isDebugEnabled()) {
				log.debug("getOperationBySOAPAction: Operation (" + operation
						+ "," + operation.getName() + ") for soapAction: "
						+ soapAction + " found as child.");
			}
		}

		return operation;
	}

	/**
	 * Method getOperations.
	 *
	 * @return Returns HashMap
	 */
	public Iterable<AxisOperation> getOperations() {
		return operations.values();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.axis2.description.ParameterInclude#getParameter(java.lang.String)
	 */

	/**
	 * Gets only the published operations.
	 */
	public List<AxisOperation> getPublishedOperations() {
		final List<AxisOperation> operationList
		= new ArrayList<AxisOperation>();

		for(final AxisOperation operation: getOperations()) {
			if (!operation.isControlOperation()) {
				operationList.add(operation);
			}
		}

		return operationList;
	}

	/**
	 * Method setClassLoader.
	 *
	 * @param classLoader
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.serviceClassLoader = classLoader;
	}

	public void setFileName(URL fileName) {
		this.fileName = fileName;
	}

	/**
	 * Sets the current time as last update time of the service.
	 */
	public void setLastUpdate() {
		lastupdate = new Date().getTime();
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<XmlSchema> getSchema() {
		return schemaList;
	}

	public void addSchema(XmlSchema schema) {
		if (schema != null) {
			schemaList.add(schema);
			if (schema.getTargetNamespace() != null) {
				addSchemaNameSpace(schema);
			}
		}
	}

	public void addSchema(Collection<XmlSchema> schemas) {
		Iterator<XmlSchema> iterator = schemas.iterator();
		while (iterator.hasNext()) {
			XmlSchema schema = iterator.next();
			schemaList.add(schema);
			addSchemaNameSpace(schema);
		}
	}

	public boolean isWsdlFound() {
		return wsdlFound;
	}

	public void setWsdlFound(boolean wsdlFound) {
		this.wsdlFound = wsdlFound;
	}

	public String getScope() {
		return scope;
	}

	/**
	 * @param scope -
	 *            Available scopes : Constants.SCOPE_APPLICATION
	 *            Constants.SCOPE_TRANSPORT_SESSION Constants.SCOPE_SOAP_SESSION
	 *            Constants.SCOPE_REQUEST.equals
	 */
	public void setScope(String scope) {
		if (Constants.SCOPE_APPLICATION.equals(scope)
				|| Constants.SCOPE_TRANSPORT_SESSION.equals(scope)
				|| Constants.SCOPE_SOAP_SESSION.equals(scope)
				|| Constants.SCOPE_REQUEST.equals(scope)) {
			this.scope = scope;
		}
	}

	public boolean isUseDefaultChains() {
		return useDefaultChains;
	}

	public void setUseDefaultChains(boolean useDefaultChains) {
		this.useDefaultChains = useDefaultChains;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getSchemaTargetNamespace() {
		return schematargetNamespace;
	}

	public void setSchemaTargetNamespace(String schematargetNamespace) {
		this.schematargetNamespace = schematargetNamespace;
	}

	public String getSchemaTargetNamespacePrefix() {
		return schematargetNamespacePrefix;
	}

	public void setSchemaTargetNamespacePrefix(
			String schematargetNamespacePrefix) {
		this.schematargetNamespacePrefix = schematargetNamespacePrefix;
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public String getTargetNamespacePrefix() {
		return targetNamespacePrefix;
	}

	public void setTargetNamespacePrefix(String targetNamespacePrefix) {
		this.targetNamespacePrefix = targetNamespacePrefix;
	}

	public XmlSchemaElement getSchemaElement(QName elementQName) {
		XmlSchemaElement element;
		for (int i = 0; i < schemaList.size(); i++) {
			XmlSchema schema = schemaList.get(i);
			if (schema != null) {
				element = schema.getElementByName(elementQName);
				if (element != null) {
					return element;
				}
			}
		}
		return null;
	}

	public boolean isEnableAllTransports() {
		return enableAllTransports;
	}

	/**
	 * To eneble service to be expose in all the transport
	 *
	 * @param enableAllTransports
	 */
	public void setEnableAllTransports(boolean enableAllTransports) {
		this.enableAllTransports = enableAllTransports;
		eprs = calculateEPRs();
	}

	public List<String> getExposedTransports() {
		return this.exposedTransports;
	}

	public void setExposedTransports(List<String> transports) {
		enableAllTransports = false;
		this.exposedTransports = transports;
		eprs = null; // Do not remove this. We need to force EPR
		// recalculation.
	}

	public void addExposedTransport(String transport) {
		enableAllTransports = false;
		if (!this.exposedTransports.contains(transport)) {
			this.exposedTransports.add(transport);
			try {
				eprs = calculateEPRs();
			} catch (Exception e) {
				eprs = null;
			}
		}
	}

	public void removeExposedTransport(String transport) {
		enableAllTransports = false;
		this.exposedTransports.remove(transport);
		try {
			eprs = calculateEPRs();
		} catch (Exception e) {
			eprs = null;
		}
	}

	public boolean isExposedTransport(String transport) {
		return exposedTransports.contains(transport);
	}

	@Override
	public void onDisengage(AxisModule module) throws AxisFault {
		removeModuleOperations(module);
		for(AxisOperation axisOperation: getOperations()) {
			axisOperation.disengageModule(module);
		}
		AxisConfiguration config = getConfiguration();
		if (!config.isEngaged(module.getName())) {
			PhaseResolver phaseResolver = new PhaseResolver(config);
			phaseResolver.disengageModuleFromGlobalChains(module);
		}
	}

	/**
	 * Remove any operations which were added by a given module.
	 *
	 * @param module
	 *            the module in question
	 */
	private void removeModuleOperations(AxisModule module) {
		Map<QName, AxisOperation> moduleOperations = module.getOperations();
		if (moduleOperations != null) {
			for (AxisOperation axisOperation : moduleOperations.values()) {
				AxisOperation operation = axisOperation;
				removeOperation(operation.getName());
			}
		}
	}

	// #######################################################################################
	// APIs to create AxisService

	//

	/**
	 * To create a AxisService for a given WSDL and the created client is most
	 * suitable for client side invocation not for server side invocation. Since
	 * all the soap action and wsa action is added to operations
	 *
	 * @param wsdlURL
	 *            location of the WSDL
	 * @param wsdlServiceName
	 *            name of the service to be invoke , if it is null then the
	 *            first one will be selected if there are more than one
	 * @param portName
	 *            name of the port , if there are more than one , if it is null
	 *            then the first one in the iterator will be selected
	 * @param options
	 *            Service client options, to set the target EPR
	 * @return AxisService , the created service will be return
	 */
	public static AxisService createClientSideAxisService(URL wsdlURL,
			QName wsdlServiceName, String portName, Options options)
	throws AxisFault {
		try {
			InputStream in = wsdlURL.openConnection().getInputStream();
			Document doc = XMLUtils.newDocument(in);
			WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
			reader.setFeature("javax.wsdl.importDocuments", true);
			Definition wsdlDefinition = reader.readWSDL(getBaseURI(wsdlURL
					.toString()), doc);
			if (wsdlDefinition != null) {
				wsdlDefinition.setDocumentBaseURI(getDocumentURI(wsdlURL
						.toString()));
			}
			return createClientSideAxisService(wsdlDefinition, wsdlServiceName,
					portName, options);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw AxisFault.makeFault(e);
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage(), e);
			throw AxisFault.makeFault(e);
		} catch (SAXException e) {
			log.error(e.getMessage(), e);
			throw AxisFault.makeFault(e);
		} catch (WSDLException e) {
			log.error(e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	private static String getBaseURI(String currentURI) {
		try {
			File file = new File(currentURI);
			if (file.exists()) {
				return file.getCanonicalFile().getParentFile().toURI()
				.toString();
			}
			String uriFragment = currentURI.substring(0, currentURI
					.lastIndexOf("/"));
			return uriFragment + (uriFragment.endsWith("/") ? "" : "/");
		} catch (IOException e) {
			return null;
		}
	}

	private static String getDocumentURI(String currentURI) {
		try {
			File file = new File(currentURI);
			return file.getCanonicalFile().toURI().toString();
		} catch (IOException e) {
			return null;
		}
	}

	public static AxisService createClientSideAxisService(
			Definition wsdlDefinition, QName wsdlServiceName, String portName,
			Options options) throws AxisFault {
		WSDL11ToAxisServiceBuilder serviceBuilder = new WSDL11ToAxisServiceBuilder(
				wsdlDefinition, wsdlServiceName, portName);
		serviceBuilder.setServerSide(false);
		AxisService axisService = serviceBuilder.populateService();
		AxisEndpoint axisEndpoint = axisService.getEndpoints()
		.get(axisService.getEndpointName());
		options.setTo(new EndpointReference(axisEndpoint.getEndpointURL()));
		if (axisEndpoint != null) {
			options.setSoapVersionURI((String) axisEndpoint.getBinding()
					.getProperty(WSDL2Constants.ATTR_WSOAP_VERSION));
		}
		return axisService;
	}

	/**
	 * To create an AxisService using given service impl class name first
	 * generate schema corresponding to the given java class , next for each
	 * methods AxisOperation will be created. If the method is in-out it will
	 * uses RPCMessageReceiver else RPCInOnlyMessageReceiver <p/> Note : Inorder
	 * to work this properly RPCMessageReceiver should be available in the class
	 * path otherewise operation can not continue
	 *
	 * @param implClass
	 *            Service implementation class
	 * @param axisConfig
	 *            Current AxisConfiguration
	 * @return return created AxisSrevice the creted service , it can either be
	 *         null or valid service
	 */
	public static AxisService createService(String implClass,
			AxisConfiguration axisConfig) throws AxisFault {

		try {
			HashMap<String, MessageReceiver> messageReciverMap = new HashMap<String, MessageReceiver>();
			Class<?> inOnlyMessageReceiver = Loader
			.loadClass("org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver");
			MessageReceiver messageReceiver = (MessageReceiver) inOnlyMessageReceiver
			.newInstance();
			messageReciverMap.put(WSDL2Constants.MEP_URI_IN_ONLY,
					messageReceiver);
			Class<?> inoutMessageReceiver = Loader
			.loadClass("org.apache.axis2.rpc.receivers.RPCMessageReceiver");
			MessageReceiver inOutmessageReceiver = (MessageReceiver) inoutMessageReceiver
			.newInstance();
			messageReciverMap.put(WSDL2Constants.MEP_URI_IN_OUT,
					inOutmessageReceiver);
			messageReciverMap.put(WSDL2Constants.MEP_URI_ROBUST_IN_ONLY,
					inOutmessageReceiver);

			return createService(implClass, axisConfig, messageReciverMap,
					null, null, axisConfig.getSystemClassLoader());
		} catch (Exception e) {
			throw AxisFault.makeFault(e);
		}
	}

	/**
	 * messageReceiverClassMap will hold the MessageReceivers for given meps.
	 * Key will be the mep and value will be the instance of the MessageReceiver
	 * class. Ex: Map mrMap = new HashMap();
	 * mrMap.put("http://www.w3.org/2004/08/wsdl/in-only",
	 * RPCInOnlyMessageReceiver.class.newInstance());
	 * mrMap.put("http://www.w3.org/2004/08/wsdl/in-out",
	 * RPCMessageReceiver.class.newInstance());
	 *
	 * @param implClass
	 * @param axisConfiguration
	 * @param messageReceiverClassMap
	 * @param targetNamespace
	 * @param schemaNamespace
	 * @throws AxisFault
	 */
	public static AxisService createService(String implClass,
			AxisConfiguration axisConfiguration, Map<String, MessageReceiver> messageReceiverClassMap,
			String targetNamespace, String schemaNamespace, ClassLoader loader)
	throws AxisFault {
		int index = implClass.lastIndexOf(".");
		String serviceName;
		if (index > 0) {
			serviceName = implClass.substring(index + 1, implClass.length());
		} else {
			serviceName = implClass;
		}

		SchemaGenerator schemaGenerator;
		List<String> excludeOperation = new ArrayList<String>();
		AxisService service = new AxisService();
		service.setName(serviceName);

		try {
			Parameter generateBare = service
			.getParameter(Java2WSDLConstants.DOC_LIT_BARE_PARAMETER);
			if (generateBare != null && "true".equals(generateBare.getValue())) {
				schemaGenerator = new DocLitBareSchemaGenerator(loader,
						implClass, schemaNamespace,
						Java2WSDLConstants.SCHEMA_NAMESPACE_PRFIX, service);
			} else {
				schemaGenerator = new DefaultSchemaGenerator(loader, implClass,
						schemaNamespace,
						Java2WSDLConstants.SCHEMA_NAMESPACE_PRFIX, service);
			}
			schemaGenerator
			.setElementFormDefault(Java2WSDLConstants.FORM_DEFAULT_UNQUALIFIED);
			Utils.addExcludeMethods(excludeOperation);
			schemaGenerator.setExcludeMethods(excludeOperation);
		} catch (Exception e) {
			throw AxisFault.makeFault(e);
		}

		return createService(implClass, serviceName, axisConfiguration,
				messageReceiverClassMap, targetNamespace, loader,
				schemaGenerator, service);
	}

	/**
	 * messageReceiverClassMap will hold the MessageReceivers for given meps.
	 * Key will be the mep and value will be the instance of the MessageReceiver
	 * class. Ex: Map mrMap = new HashMap();
	 * mrMap.put("http://www.w3.org/2004/08/wsdl/in-only",
	 * RPCInOnlyMessageReceiver.class.newInstance());
	 * mrMap.put("http://www.w3.org/2004/08/wsdl/in-out",
	 * RPCMessageReceiver.class.newInstance());
	 *
	 * @param implClass
	 * @param axisConfiguration
	 * @param messageReceiverClassMap
	 * @param targetNamespace
	 * @throws AxisFault
	 */
	public static AxisService createService(String implClass,
			String serviceName, AxisConfiguration axisConfiguration,
			Map<String, MessageReceiver> messageReceiverClassMap, String targetNamespace,
			ClassLoader loader, SchemaGenerator schemaGenerator,
			AxisService axisService) throws AxisFault {
		Parameter parameter = new Parameter(Constants.SERVICE_CLASS, implClass);
		OMElement paraElement = Utils.getParameter(Constants.SERVICE_CLASS,
				implClass, false);
		parameter.setParameterElement(paraElement);
		axisService.setUseDefaultChains(false);
		axisService.addParameter(parameter);
		axisService.setName(serviceName);
		axisService.setClassLoader(loader);

		Map<String, String> map = new HashMap<String, String>();
		map.put(Java2WSDLConstants.AXIS2_NAMESPACE_PREFIX,
				Java2WSDLConstants.AXIS2_XSD);
		map.put(Java2WSDLConstants.DEFAULT_SCHEMA_NAMESPACE_PREFIX,
				Java2WSDLConstants.URI_2001_SCHEMA_XSD);
		axisService.setNamespaceMap(map);
		Utils.processBeanPropertyExclude(axisService);
		axisService.setElementFormDefault(false);
		try {
			axisService.addSchema(schemaGenerator.generateSchema());
		} catch (Exception e) {
			throw AxisFault.makeFault(e);
		}
		axisService.setSchemaTargetNamespace(schemaGenerator
				.getSchemaTargetNameSpace());
		axisService.setTypeTable(schemaGenerator.getTypeTable());
		if (targetNamespace == null) {
			targetNamespace = schemaGenerator.getSchemaTargetNameSpace();
		}
		if (targetNamespace != null && !"".equals(targetNamespace)) {
			axisService.setTargetNamespace(targetNamespace);
		}
		Method[] method = schemaGenerator.getMethods();
		PhasesInfo pinfo = axisConfiguration.getPhasesInfo();
		for (Method jmethod : method) {
			AxisOperation operation = axisService.getOperation(new QName(
					jmethod.getName()));
			String mep = operation.getMessageExchangePattern();
			MessageReceiver mr;
			if (messageReceiverClassMap != null) {

				if (messageReceiverClassMap.get(mep) != null) {
					Object obj = messageReceiverClassMap.get(mep);
					if (obj instanceof MessageReceiver) {
						mr = (MessageReceiver) obj;
						operation.setMessageReceiver(mr);
					} else {
						log
						.error("Object is not an instance of MessageReceiver, thus, default MessageReceiver has been set");
						mr = axisConfiguration.getMessageReceiver(operation
								.getMessageExchangePattern());
						operation.setMessageReceiver(mr);
					}
				} else {
					log
					.error("Required MessageReceiver couldn't be found, thus, default MessageReceiver has been used");
					mr = axisConfiguration.getMessageReceiver(operation
							.getMessageExchangePattern());
					operation.setMessageReceiver(mr);
				}
			} else {
				log
				.error("MessageRecevierClassMap couldn't be found, thus, default MessageReceiver has been used");
				mr = axisConfiguration.getMessageReceiver(operation
						.getMessageExchangePattern());
				operation.setMessageReceiver(mr);
			}
			operation.setPhases(pinfo);
			axisService.addOperation(operation);
		}

		String endpointName = axisService.getEndpointName();
		if ((endpointName == null || endpointName.length() == 0)
				&& axisService.getConfiguration() != null) {
			Utils.expandServiceEndpoints(axisService);
		}

		return axisService;

	}

	public void removeOperation(QName opName) {
		AxisOperation operation = operations.remove(opName);
		if (operation != null) {
			List<String> mappingList = operation.getWSAMappingList();
			if (mappingList != null) {
				for(String actionMapping: mappingList) {
					operationsAliasesMap.remove(actionMapping);
				}
			}
			operationsAliasesMap.remove(operation.getName().getLocalPart());
		}
	}

	/**
	 * Get the namespace map for this service.
	 *
	 * @return a Map of prefix (String) to namespace URI (String)
	 */
	public Map<String, String> getNamespaceMap() {
		return namespaceMap;
	}

	/**
	 * Get the namespaces associated with imported WSDLs
	 *
	 * @return a <code>List</code> of namespace URIs (String)
	 */
	public List<String> getImportedNamespaces() {
		return importedNamespaces;
	}

	/**
	 * Set the namespaces associated with imported WSDLs
	 *
	 * @param importedNamespaces
	 */
	public void setImportedNamespaces(List<String> importedNamespaces) {
		this.importedNamespaces = importedNamespaces;
	}

	public void setNamespaceMap(Map<String, String> namespaceMap) {
		this.namespaceMap = namespaceMap;
	}

	private void addSchemaNameSpace(XmlSchema schema) {
		String targetNameSpace = schema.getTargetNamespace();
		String prefix = schema.getNamespaceContext().getPrefix(targetNameSpace);

		if (namespaceMap == null) {
			namespaceMap = new HashMap<String, String>();
		}

		if (!namespaceMap.values().contains(targetNameSpace)) {
			// i.e this target namespace not exists in the namesapce map
			// find a non exists prefix to add this target namesapce
			while ((prefix == null) || namespaceMap.keySet().contains(prefix)) {
				prefix = "ns" + nsCount++;
			}
			namespaceMap.put(prefix, targetNameSpace);
		}

	}

	public Map<String, String> populateSchemaMappings() {
		// when calling from other than codegen. i.e from deployment
		// engine we don't have to override the absolute http locations.
		return populateSchemaMappings(false);
	}

	/**
	 * runs the schema mappings if it has not been run previously it is best
	 * that this logic be in the axis service since one can call the axis
	 * service to populate the schema mappings
	 */
	public Map<String, String> populateSchemaMappings(boolean overrideAbsoluteAddress) {

		// populate the axis service with the necessary schema references
		List<XmlSchema> schema = this.schemaList;
		Map<String, String> changedSchemaLocations = null;
		if (!this.schemaLocationsAdjusted) {
			Map<XmlSchema, String> nameTable = new HashMap<XmlSchema, String>();
			Map<String, String> sourceURIToNewLocationMap
			= new HashMap<String, String>();
			// calculate unique names for the schemas
			calculateSchemaNames(schema, nameTable, sourceURIToNewLocationMap,
					overrideAbsoluteAddress);
			// adjust the schema locations as per the calculated names
			changedSchemaLocations
			= adjustSchemaNames(schema, nameTable, sourceURIToNewLocationMap);
			// reverse the nametable so that there is a mapping from the
			// name to the schemaObject
			setSchemaMappingTable(swapMappingTable(nameTable));
			setSchemaLocationsAdjusted(true);
		}
		return changedSchemaLocations;
	}

	/**
	 * run 1 -calcualte unique names
	 *
	 * @param schemas
	 */
	private void calculateSchemaNames(	List<XmlSchema> schemas,
			Map<XmlSchema, String> nameTable,
			Map<String, String> sourceURIToNewLocationMap,
			boolean overrideAbsoluteAddress)
	{
		// first traversal - fill the hashtable
		for (int i = 0; i < schemas.size(); i++) {
			XmlSchema schema = schemas.get(i);
			XmlSchemaObjectCollection includes = schema.getIncludes();

			for (int j = 0; j < includes.getCount(); j++) {
				Object item = includes.getItem(j);
				XmlSchema s;
				if (item instanceof XmlSchemaExternal) {
					XmlSchemaExternal externalSchema = (XmlSchemaExternal) item;
					s = externalSchema.getSchema();

					if (s != null
							&& getSchemaLocationWithDot(
									sourceURIToNewLocationMap, s) == null) {
						// insert the name into the table
						insertIntoNameTable(nameTable, s,
								sourceURIToNewLocationMap,
								overrideAbsoluteAddress);
						// recursively call the same procedure
						calculateSchemaNames(Arrays
								.asList(new XmlSchema[] { s }), nameTable,
								sourceURIToNewLocationMap,
								overrideAbsoluteAddress);
					}
				}
			}
		}
	}

	/**
	 * A quick private sub routine to insert the names
	 *
	 * @param nameTable
	 * @param s
	 */
	private void insertIntoNameTable(	Map<XmlSchema, String> nameTable,
			XmlSchema s,
			Map<String, String> sourceURIToNewLocationMap,
			boolean overrideAbsoluteAddress)
	{
		String sourceURI = s.getSourceURI();
		// check whether the source URI is an absolute one and are
		// we allowed to override it.
		// if the absolute URI overriding is not allowed the use the
		// original sourceURI as new one
		if (sourceURI.startsWith("http") && !overrideAbsoluteAddress) {
			nameTable.put(s, sourceURI);
			sourceURIToNewLocationMap.put(sourceURI, sourceURI);
		} else {
			String newURI = sourceURI.substring(sourceURI.lastIndexOf('/') + 1);
			if (newURI.endsWith(".xsd")) {
				// remove the .xsd extension
				newURI = newURI.substring(0, newURI.lastIndexOf("."));
			} else {
				newURI = "xsd" + count++;
			}

			newURI = customSchemaNameSuffix != null ? newURI
					+ customSchemaNameSuffix : newURI;
			// make it unique
			while (nameTable.containsValue(newURI)) {
				newURI = newURI + count++;
			}

			nameTable.put(s, newURI);
			sourceURIToNewLocationMap.put(sourceURI, newURI);
		}
	}

	/**
	 * Run 2 - adjust the names
	 */
	private Map<String, String> adjustSchemaNames(
			final List<XmlSchema> schemas,
			final Map<XmlSchema, String> nameTable,
			final Map<String, String> sourceURIToNewLocationMap)
			{
		Map<String, String> importedSchemas
		= new HashMap<String, String>();
		for(XmlSchema schema: schemas) {
			adjustSchemaName(schema, importedSchemas, sourceURIToNewLocationMap);
		}
		for(XmlSchema schema: nameTable.keySet()) {
			adjustSchemaName(schema, importedSchemas, sourceURIToNewLocationMap);
		}
		return importedSchemas;
			}

	/**
	 * Adjust a single schema
	 *
	 * @param parentSchema
	 * @param nameTable
	 */
	private void adjustSchemaName(	XmlSchema parentSchema,
			Map<String, String> importedSchemas,
			Map<String, String> sourceURIToNewLocationMap)
	{
		//TODO:  what is this XmlSchemaObjectCollection?
		XmlSchemaObjectCollection includes = parentSchema.getIncludes();
		for (int j = 0; j < includes.getCount(); j++) {
			Object item = includes.getItem(j);
			if (item instanceof XmlSchemaExternal) {
				XmlSchemaExternal xmlSchemaExternal = (XmlSchemaExternal) item;
				XmlSchema s = xmlSchemaExternal.getSchema();
				adjustSchemaLocation(s, xmlSchemaExternal, importedSchemas, sourceURIToNewLocationMap);
			}
		}
	}

	/**
	 * Adjusts a given schema location
	 *
	 * @param s
	 * @param xmlSchemaExternal
	 * @param nameTable
	 */
	private void adjustSchemaLocation(XmlSchema s,
			XmlSchemaExternal xmlSchemaExternal,
			Map<String, String> importedSchemas,
			Map<String, String> sourceURIToNewLocationMap) {
		if (s != null) {
			String schemaLocation = xmlSchemaExternal.getSchemaLocation();

			String newSchemaLocation = customSchemaNamePrefix == null ?
					// use the default mode
					(this.getServiceEPR() + "?xsd=" + getSchemaLocationWithDot(
							sourceURIToNewLocationMap, s))
							:
								// custom prefix is present - add the custom prefix
								(customSchemaNamePrefix + getSchemaLocationWithDot(
										sourceURIToNewLocationMap, s));
					xmlSchemaExternal.setSchemaLocation(newSchemaLocation);
					importedSchemas.put(schemaLocation, newSchemaLocation);
		}
	}

	private Object getSchemaLocationWithDot(
			final Map<String, String> sourceURIToNewLocationMap,
			final XmlSchema schema)
	{
		String o = sourceURIToNewLocationMap.get(schema.getSourceURI());
		if (o != null && o.indexOf(".") < 0) {
			return o + ".xsd";
		}
		return o;
	}

	/**
	 * Swap the key,value pairs
	 *
	 * @param originalTable
	 */
	private <K,V> Map<V, K> swapMappingTable(final Map<K, V> originalTable)
	{
		final Map<V, K> swappedTable = new HashMap<V, K>(originalTable.size());
		for(final Map.Entry<K, V> e: originalTable.entrySet()) {
			swappedTable.put(e.getValue(), e.getKey());
		}
		return swappedTable;
	}

	public boolean isClientSide() {
		return clientSide;
	}

	public void setClientSide(boolean clientSide) {
		this.clientSide = clientSide;
	}

	public boolean isElementFormDefault() {
		return elementFormDefault;
	}

	public void setElementFormDefault(boolean elementFormDefault) {
		this.elementFormDefault = elementFormDefault;
	}

	/**
	 * User can set a parameter in services.xml saying he want to show the
	 * original WSDL that he put into META-INF once someone ask for ?wsdl so if
	 * you want to use your own WSDL then add following parameter into
	 * services.xml <parameter name="useOriginalwsdl">true</parameter>
	 */
	public boolean isUseUserWSDL() {
		Parameter parameter = getParameter("useOriginalwsdl");
		if (parameter != null) {
			String value = (String) parameter.getValue();
			if ("true".equals(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * By default the port address in user WSDLs is modified, set the following
	 * parameter to override this behaviour <parameter
	 * name="modifyUserWSDLPortAddress">false</parameter>
	 */
	public boolean isModifyUserWSDLPortAddress() {
		Parameter parameter = getParameter("modifyUserWSDLPortAddress");
		if (parameter != null) {
			String value = (String) parameter.getValue();
			if ("false".equals(value)) {
				return false;
			}
		}
		return true;
	}

	public ServiceLifeCycle getServiceLifeCycle() {
		return serviceLifeCycle;
	}

	public void setServiceLifeCycle(ServiceLifeCycle serviceLifeCycle) {
		this.serviceLifeCycle = serviceLifeCycle;
	}

	public Map<String, String> getP2nMap() {
		return p2nMap;
	}

	public void setP2nMap(Map<String, String> p2nMap) {
		this.p2nMap = p2nMap;
	}

	public ObjectSupplier getObjectSupplier() {
		return objectSupplier;
	}

	public void setObjectSupplier(ObjectSupplier objectSupplier) {
		this.objectSupplier = objectSupplier;
	}

	public TypeTable getTypeTable() {
		return typeTable;
	}

	public void setTypeTable(TypeTable typeTable) {
		this.typeTable = typeTable;
	}

	/**
	 * Find a data locator from the available data locators (both configured and
	 * default ones) to retrieve Metadata or data specified in the request.
	 *
	 * @param request
	 *            an {@link DataRetrievalRequest} object
	 * @param msgContext
	 *            message context
	 * @return array of {@link Data} object for the request.
	 * @throws AxisFault
	 */
	public Data[] getData(	DataRetrievalRequest request,
							MessageContext msgContext )
		throws AxisFault
	{
		String dialect = request.getDialect();
		for(LocatorType locatorType: availableDataLocatorTypes) {
			AxisDataLocator dataLocator = getDataLocator(locatorType, dialect);
			if(dataLocator == null) {
				continue;
			}
			Data[] data = dataLocator.getData(request, msgContext);
			if(data == null) {
				continue;
			}
			return data;
		}

		return null;
	}

	/**
	 * Save data Locator configured at service level for this Axis Service
	 *
	 * @param dialect-
	 *            an absolute URI represents the Dialect i.e. WSDL, Policy,
	 *            Schema or "ServiceLevel" for non-dialect service level data
	 *            locator.
	 * @param dataLocatorClassName -
	 *            class name of the Data Locator configured to support data
	 *            retrieval for the specified dialect.
	 */
	public void addDataLocatorClassNames(String dialect,
			String dataLocatorClassName) {
		dataLocatorClassNames.put(dialect, dataLocatorClassName);
	}

	/*
	 * Get data locator instance based on the LocatorType and dialect.
	 */
	public AxisDataLocator getDataLocator(LocatorType locatorType,
			String dialect) throws AxisFault {
		AxisDataLocator locator;
		if (locatorType == LocatorType.SERVICE_DIALECT) {
			locator = getServiceDataLocator(dialect);
		} else if (locatorType == LocatorType.SERVICE_LEVEL) {
			locator = getServiceDataLocator(DRConstants.SERVICE_LEVEL);
		} else if (locatorType == LocatorType.GLOBAL_DIALECT) {
			locator = getGlobalDataLocator(dialect);
		} else if (locatorType == LocatorType.GLOBAL_LEVEL) {
			locator = getGlobalDataLocator(DRConstants.GLOBAL_LEVEL);
		} else if (locatorType == LocatorType.DEFAULT_AXIS) {
			locator = getDefaultDataLocator();
		} else {
			locator = getDefaultDataLocator();
		}

		return locator;
	}

	// Return default Axis2 Data Locator
	private AxisDataLocator getDefaultDataLocator()
	throws DataRetrievalException {

		if (defaultDataLocator == null) {
			defaultDataLocator = new AxisDataLocatorImpl(this);
		}

		defaultDataLocator.loadServiceData();

		return defaultDataLocator;
	}

	/*
	 * Checks if service level data locator configured for specified dialect.
	 * Returns an instance of the data locator if exists, and null otherwise.
	 */
	private AxisDataLocator getServiceDataLocator(String dialect)
	throws AxisFault {
		AxisDataLocator locator;
		locator = dataLocators.get(dialect);
		if (locator == null) {
			String className = dataLocatorClassNames.get(dialect);
			if (className != null) {
				locator = loadDataLocator(className);
				dataLocators.put(dialect, locator);
			}

		}

		return locator;

	}

	/*
	 * Checks if global level data locator configured for specified dialect.
	 * @param dialect- an absolute URI represents the Dialect i.e. WSDL, Policy,
	 * Schema or "GlobalLevel" for non-dialect Global level data locator.
	 * Returns an instance of the data locator if exists, and null otherwise.
	 */

	public AxisDataLocator getGlobalDataLocator(String dialect)
		throws AxisFault
	{
		AxisConfiguration axisConfig = getConfiguration();
		AxisDataLocator locator = null;
		if (axisConfig != null) {
			locator = axisConfig.getDataLocator(dialect);
			if (locator == null) {
				String className = axisConfig.getDataLocatorClassName(dialect);
				if (className != null) {
					locator = loadDataLocator(className);
					axisConfig.addDataLocator(dialect, locator);
				}
			}
		}

		return locator;
	}

	protected AxisDataLocator loadDataLocator(String className)
	throws AxisFault {

		AxisDataLocator locator;

		try {
			Class<?> dataLocator;
			dataLocator = Class.forName(className, true, serviceClassLoader);
			locator = (AxisDataLocator) dataLocator.newInstance();
		} catch (ClassNotFoundException e) {
			throw AxisFault.makeFault(e);
		} catch (IllegalAccessException e) {
			throw AxisFault.makeFault(e);
		} catch (InstantiationException e) {
			throw AxisFault.makeFault(e);

		}

		return locator;
	}

	/**
	 * Set the map of WSDL message element QNames to AxisOperations for this
	 * service. This map is used during SOAP Body-based routing for
	 * document/literal bare services to match the first child element of the
	 * SOAP Body element to an operation. (Routing for RPC and document/literal
	 * wrapped services occurs via the operationsAliasesMap.) <p/> From section
	 * 4.7.6 of the WS-I BP 1.1: the "operation signature" is "the fully
	 * qualified name of the child element of SOAP body of the SOAP input
	 * message described by an operation in a WSDL binding," and thus this map
	 * must be from a QName to an operation.
	 *
	 * @param messageElementQNameToOperationMap
	 *            The map from WSDL message element QNames to AxisOperations.
	 */
	public void setMessageElementQNameToOperationMap(
			final Map<QName, AxisOperation> messageElementQNameToOperationMap)
	{
		this.messageElementQNameToOperationMap = messageElementQNameToOperationMap;
	}

	/**
	 * Look up an AxisOperation for this service based off of an element QName
	 * from a WSDL message element.
	 *
	 * @param messageElementQName
	 *            The QName to search for.
	 * @return The AxisOperation registered to the QName or null if no match was
	 *         found.
	 * @see #setMessageElementQNameToOperationMap(Map)
	 */
	public AxisOperation getOperationByMessageElementQName(QName messageElementQName) {
		return messageElementQNameToOperationMap.get(messageElementQName);
	}

	/**
	 * Add an entry to the map between element QNames in WSDL messages and
	 * AxisOperations for this service.
	 *
	 * @param messageElementQName
	 *            The QName of the element on the input message that maps to the
	 *            given operation.
	 * @param operation
	 *            The AxisOperation to be mapped to.
	 * @see #setMessageElementQNameToOperationMap(Map)
	 */
	public void addMessageElementQNameToOperationMapping(
			QName messageElementQName, AxisOperation operation) {
		// when setting an operation we have to set it only if the
		// messegeElementQName does not
		// exists in the map.
		// does exists means there are two or more operations which has the same
		// input element (in doc/literal
		// this is possible. In this case better to set it as null without
		// giving
		// a random operation.
		if (messageElementQNameToOperationMap.containsKey(messageElementQName)
				&& messageElementQNameToOperationMap.get(messageElementQName) != operation) {
			messageElementQNameToOperationMap.put(messageElementQName, null);
		} else {
			messageElementQNameToOperationMap.put(messageElementQName,
					operation);
		}

	}

	// TODO : Explain what goes in this map!
	public Map<String, AxisEndpoint> getEndpoints() {
		return endpointMap;
	}

	public boolean isCustomWsdl() {
		return customWsdl;
	}

	public void setCustomWsdl(boolean customWsdl) {
		this.customWsdl = customWsdl;
	}

	public List<QName> getOperationsNameList() {
		return operationsNameList;
	}

	public void setOperationsNameList(List<QName> operationsNameList) {
		this.operationsNameList = operationsNameList;
	}

	@Override
	public AxisServiceGroup getServiceGroup() {
		return parent;
	}

	public void setParent(AxisServiceGroup parent) {
		this.parent = parent;
		this.parameterInclude.setParent(parent);
		this.policySubject.setParent(parent);
	}

	@Override
	public String toString() {
		return getName();
	}

	public ExcludeInfo getExcludeInfo() {
		return excludeInfo;
	}

	public void setExcludeInfo(ExcludeInfo excludeInfo) {
		this.excludeInfo = excludeInfo;
	}

	public void registerPolicy(String key, Policy policy) {
		policyMap.put(key, policy);
	}

	public Policy lookupPolicy(String key) {
		return policyMap.get(key);
	}

	/**
	 * Add a ServiceContextListener
	 * @param scl
	 */
	public void addMessageContextListener(MessageContextListener scl) {
		messageContextListeners.add(scl);
	}

	/**
	 * Remove a ServiceContextListener
	 * @param scl
	 */
	public void removeMessageContextListener(MessageContextListener scl) {
		messageContextListeners.remove(scl);
	}

	/**
	 * @param cls Class of ServiceContextListener
	 * @return true if ServiceContextLister is in the list
	 */
	public boolean hasMessageContextListener(Class<?> cls) {
		for (MessageContextListener mcl: messageContextListeners) {
			if (mcl.getClass().equals(cls)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Signal an Attach ServiceContext Event
	 * @param sc ServiceContext
	 * @param mc MessageContext
	 */
	public void attachServiceContextEvent(ServiceContext sc, MessageContext mc) {
		for (MessageContextListener mcl: messageContextListeners) {
			mcl.attachServiceContextEvent(sc, mc);
		}
	}

	/**
	 * Signal an Attach Envelope Event
	 * @param mc MessageContext
	 */
	public void attachEnvelopeEvent(MessageContext mc) {
		for (MessageContextListener mcl: messageContextListeners) {
			mcl.attachEnvelopeEvent(mc);
		}
	}

	@Override
	public AxisConfiguration getConfiguration() {
		return (parent != null) ? parent.getConfiguration() : null;
	}

	@Override
	public Policy getEffectivePolicy() {
		return getEffectivePolicy(this);
	}
}
