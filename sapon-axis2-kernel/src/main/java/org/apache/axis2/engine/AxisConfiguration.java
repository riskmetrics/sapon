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

package org.apache.axis2.engine;

import java.io.File;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.alt.Flows;
import org.apache.axis2.alt.ModuleConfigAccessor;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.unknowncontent.UnknownContentBuilder;
import org.apache.axis2.clustering.ClusterManager;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.dataretrieval.AxisDataLocator;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.ModuleDeployer;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.description.AxisDescriptionBase;
import org.apache.axis2.description.AxisEndpoint;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.AxisServiceGroupImpl;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.java2wsdl.Java2WSDLConstants;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.phaseresolver.PhaseResolver;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.util.TargetResolver;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;

/**
 * Class AxisConfiguration
 */
public class AxisConfiguration extends AxisDescriptionBase
	implements ModuleConfigAccessor
{
    private static final Log log = LogFactory.getLog(AxisConfiguration.class);

    private final Map<String, AxisDataLocator> dataLocators = new HashMap<String, AxisDataLocator>();
    private final Map<String, String> dataLocatorClassNames = new HashMap<String, String>();

    private final Map<String, AxisModule> allModules = new HashMap<String, AxisModule>();

    // To store mapping between default version and module name
    private final Map<String, String> nameToVersionMap = new HashMap<String, String>();

    // private final HashMap serviceGroups = new HashMap();
    private final Map<String, TransportInDescription> transportsIn = new HashMap<String, TransportInDescription>();

    private final Map<String, TransportOutDescription> transportsOut = new HashMap<String, TransportOutDescription>();

    private final Map<String, List<AxisModule>> policySupportedModules = new HashMap<String, List<AxisModule>>();

    private final List<QName> localPolicyAssertions = new ArrayList<QName>();

    private List<AxisObserver> observersList = null;

    private URL axis2Repository = null;

    private final Map<String, AxisService> allServices = new ConcurrentHashMap<String, AxisService>();
    private final Map<String, AxisService> allEndpoints = new ConcurrentHashMap<String, AxisService>();

    /**
     * Stores the module specified in the server.xml at the document parsing time.
     */
    private final List<String> globalModuleList;

    private final Map<String, String> faultyModules;

    /**
     * To store faulty services
     */
    private final Map<String, String> faultyServices;

    private List<Phase> inFaultPhases;

    private List<Phase> inPhasesUptoAndIncludingPostDispatch;

    private final Map<String, MessageReceiver> messageReceivers;

    private final Map<String, Builder> messageBuilders;

    private final Map<String, MessageFormatter> messageFormatters;

    private ClassLoader moduleClassLoader;

    private final Map<String, ModuleConfiguration> moduleConfigmap;

    private List<Phase> outFaultPhases;

    private List<Phase> outPhases;

    protected PhasesInfo phasesinfo;

    private ClassLoader serviceClassLoader;

    private ClassLoader systemClassLoader;

    //To keep track of whether the system has started or not
    private boolean start;

    private final List<TargetResolver> targetResolvers;

    private ClusterManager clusterManager;

    private AxisConfigurator configurator;

    private Map<String, AxisServiceGroup> children;

    /**
     * Constructor AxisConfiguration.
     */
    public AxisConfiguration() {
        moduleConfigmap = new HashMap<String, ModuleConfiguration>();
        globalModuleList = new ArrayList<String>();
        messageReceivers = new HashMap<String, MessageReceiver>();
        messageBuilders = new HashMap<String, Builder>();
        messageFormatters = new HashMap<String, MessageFormatter>();
        outPhases = new ArrayList<Phase>();
        inFaultPhases = new ArrayList<Phase>();
        outFaultPhases = new ArrayList<Phase>();
        faultyServices = new Hashtable<String, String>();
        faultyModules = new Hashtable<String, String>();
        observersList = new ArrayList<AxisObserver>();
        inPhasesUptoAndIncludingPostDispatch = new ArrayList<Phase>();
        systemClassLoader = AccessController.doPrivileged(
        		new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
        serviceClassLoader = systemClassLoader;
        moduleClassLoader = systemClassLoader;
        children = new HashMap<String, AxisServiceGroup>();

        this.phasesinfo = new PhasesInfo();
        targetResolvers = new ArrayList<TargetResolver>();
    }

    public void addMessageReceiver(String mepURL,
                                   MessageReceiver messageReceiver) {
        messageReceivers.put(mepURL, messageReceiver);
    }

    /**
     * Register a messageBuilder implementation against a content type.
     * This is used by Axis2 to support different message formats.
     *
     * @param contentType    the relevant content-type (i.e. "text/xml")
     * @param messageBuilder a Builder implementation
     */
    public void addMessageBuilder(String contentType,
                                  Builder messageBuilder) {
        messageBuilders.put(contentType, messageBuilder);
    }

    /**
     * Register a messageFormatter implementation against a content type.
     * This is used by Axis2 to support serialization of messages to different
     * message formats. (Eg: JSON)
     *
     * @param contentType      the relevant content-type (i.e. "text/xml")
     * @param messageFormatter a MessageFormatter implementation
     */
    public void addMessageFormatter(String contentType,
                                    MessageFormatter messageFormatter) {
        messageFormatters.put(contentType, messageFormatter);
    }

    /**
     * Add an available Module to this configuration
     *
     * @param module an AxisModule
     * @throws AxisFault in case of error
     */
    public void addModule(AxisModule module) throws AxisFault {
        module.setParent(this);

        // check whether the module version paramter is there , if so set the module version as that
        Parameter verisonParamter = module.getParameter(org.apache.axis2.Constants.MODULE_VERSION);
        if (verisonParamter !=null ) {
            String version = (String) verisonParamter.getValue();
            module.setVersion(version);
        }

        if (module.getVersion() == null) {
            if (module.getName().endsWith(AxisModule.VERSION_SNAPSHOT)) {
                allModules.put(module.getName(), module);
                String moduleName =
                        module.getName().substring(0,
                                                   module.getName().indexOf(AxisModule.VERSION_SNAPSHOT) - 1);
                module.setName(moduleName);
                module.setVersion(AxisModule.VERSION_SNAPSHOT);
            } else {
                allModules.put(module.getName(), module);
            }

        } else { // Calculate the module version from the name
            allModules.put(Utils.getModuleName(module.getName(), module.getVersion()), module);
        }
        notifyObservers(AxisEvent.MODULE_DEPLOY, module);

        // Registering the policy namespaces that the module understand
        registerModulePolicySupport(module);
        // Registering the policy assertions that are local to the system
        registerLocalPolicyAssertions(module);

    }

    public void deployModule(String moduleFileName) throws DeploymentException {
        File moduleFile = new File(moduleFileName);
        if (!moduleFile.exists()) {
            throw new DeploymentException("Module archive '" + moduleFileName + "' doesn't exist");
        }
        DeploymentFileData dfd = new DeploymentFileData(moduleFile, new ModuleDeployer(this));
        dfd.deploy();
    }

    /**
     * Remove a module with moduleName & moduleVersion
     *
     * @param moduleName
     * @param moduleVersion
     */
    public void removeModule(String moduleName, String moduleVersion) {
        allModules.remove(Utils.getModuleName(moduleName, moduleVersion));
        // TODO disengage has to be done here
    }

    /**
     * Adds module configuration, if there is a moduleConfig tag in service.
     *
     * @param moduleConfiguration a ModuleConfiguration to remember
     */
    public void addModuleConfig(ModuleConfiguration moduleConfiguration) {
        moduleConfigmap.put(moduleConfiguration.getModuleName(),
                            moduleConfiguration);
    }

    public void addObservers(AxisObserver axisObserver) {
        observersList.add(axisObserver);
    }

    /**
     * Method addService.
     *
     * @param service
     * @throws AxisFault
     */
    public synchronized void addService(AxisService service) throws AxisFault {
        AxisServiceGroup axisServiceGroup = new AxisServiceGroupImpl();
        axisServiceGroup.setName(service.getName());
        axisServiceGroup.setAxisConfiguration(this);
        axisServiceGroup.addService(service);
        addServiceGroup(axisServiceGroup);
//        processEndpoints(service, service.getAxisConfiguration());
    }

    public synchronized void addServiceGroup(AxisServiceGroup axisServiceGroup)
            throws AxisFault {
        axisServiceGroup.setAxisConfiguration(this);
        notifyObservers(AxisEvent.SERVICE_DEPLOY, axisServiceGroup);

        for(final AxisService axisService: axisServiceGroup.getServices()) {
            if (axisService.getSchemaTargetNamespace() == null) {
                axisService.setSchemaTargetNamespace(Java2WSDLConstants.AXIS2_XSD);
            }
        }
        for(final AxisService axisService: axisServiceGroup.getServices()) {
            if (axisService.isUseDefaultChains()) {
                for(final AxisOperation operation: axisService.getOperations()) {
                    operation.setPhases(phasesinfo);
                }
            }
        }
        Iterator<AxisModule> enModule = getEngagedModules().iterator();
        while (enModule.hasNext()) {
            axisServiceGroup.engageModule(enModule.next());
        }

        ArrayList<AxisService> servicesIAdded = new ArrayList<AxisService>();
        for(final AxisService axisService: axisServiceGroup.getServices()) {
            processEndpoints(axisService, axisService.getConfiguration());

            Map<String, AxisEndpoint> endpoints = axisService.getEndpoints();
            String serviceName = axisService.getName();
            try {
                addToAllServicesMap(axisService);
            } catch (AxisFault axisFault) {
                // Whoops, must have been a duplicate!  If we had a problem here, we have to
                // remove all the ones we added...
                for (AxisService axisService2 : servicesIAdded) {
                    AxisService service = axisService2;
                    allServices.remove(service.getName());
                }
                // And toss this in case anyone wants it?
                throw axisFault;
            }
            servicesIAdded.add(axisService);
            if (endpoints != null) {
                Iterator<String> endpointNameIter = endpoints.keySet().iterator();
                while (endpointNameIter.hasNext()) {
                    String endpointName = endpointNameIter.next();
                    if (log.isDebugEnabled()) {
                        log.debug("Adding service to allEndpoints map: ("
                                  + serviceName + "," + endpointName + ") ");
                    }

                    allEndpoints.put(serviceName + "." + endpointName, axisService);
                }
                if (log.isDebugEnabled()) {
                    log.debug("After adding to allEndpoints map, size is "
                              + allEndpoints.size());
                }
            }

            if (!axisService.isClientSide()) {
                notifyObservers(AxisEvent.SERVICE_DEPLOY, axisService);
            }
        }
        // serviceGroups.put(axisServiceGroup.getServiceGroupName(),
        // axisServiceGroup);
        children.put(axisServiceGroup.getName(), axisServiceGroup);
    }

    public void addToAllServicesMap(AxisService axisService) throws AxisFault {
        String serviceName = axisService.getName();
        AxisService oldService = allServices.get(serviceName);
        if (oldService == null) {
            if (log.isDebugEnabled()) {
                log.debug("Adding service to allServices map: [" + serviceName + "] ");
            }
            allServices.put(serviceName, axisService);
            if (log.isTraceEnabled()) {
                log.trace("After adding to allServices map, size is "
                          + allServices.size(),
                          new Exception("AxisConfiguration.addToAllServicesMap called from"));
            }

        } else {
            // If we were already there, that's fine.  If not, fault!
            if (oldService != axisService) {
                throw new AxisFault(Messages.getMessage("twoservicecannothavesamename",
                                                        axisService.getName()));
            }
        }
    }

    public AxisServiceGroup removeServiceGroup(String serviceGroupName) throws AxisFault {
        AxisServiceGroup axisServiceGroup = children.get(serviceGroupName);
        if (axisServiceGroup == null) {
            throw new AxisFault(Messages.getMessage("invalidservicegroupname",
                                                    serviceGroupName));
        }

        boolean isClientSide = false;
        for(final AxisService axisService: axisServiceGroup.getServices()) {
            allServices.remove(axisService.getName());
            if (!axisService.isClientSide()) {
                notifyObservers(AxisEvent.SERVICE_REMOVE, axisService);
            } else {
                isClientSide = true;
            }

            //removes the endpoints to this service
            String serviceName = axisService.getName();
            String key = null;

            for (String string : axisService.getEndpoints().keySet()) {
                key = serviceName + "." + string;
                this.allEndpoints.remove(key);
            }

        }
        children.remove(serviceGroupName);
        if (!isClientSide) {
            notifyObservers(AxisEvent.SERVICE_REMOVE, axisServiceGroup);
        }

        return axisServiceGroup;
    }

    /**
     * Method addTransportIn.
     *
     * @param transport
     * @throws AxisFault
     */
    public void addTransportIn(TransportInDescription transport) throws AxisFault {
        if (transport.getReceiver() == null) {
            throw new AxisFault(
                    "Transport Receiver can not be null for the transport "
                    + transport.getName());
        }
        transportsIn.put(transport.getName(), transport);
    }

    /**
     * Method addTransportOut.
     *
     * @param transport
     * @throws AxisFault
     */
    public void addTransportOut(TransportOutDescription transport)
            throws AxisFault {
        if (transport.getSender() == null) {
            throw new AxisFault(
                    "Transport sender can not be null for the transport "
                    + transport.getName());
        }
        transportsOut.put(transport.getName(), transport);
    }

    /**
     * Engages the default module version corresponding to given module name ,
     * or if the module name contains version number in it then it will engage
     * the correct module. Both of the below two cases are valid 1.
     * engageModule("addressing"); 2. engageModule("addressing-1.23");
     *
     * @param moduleref name of module to engage
     * @throws AxisFault
     */
    public void engageModule(String moduleref) throws AxisFault {
        AxisModule module = getModule(moduleref);
        if (module != null) {
            engageModule(module);
        } else {
            throw new AxisFault(Messages.getMessage("modulenotavailble", moduleref));
        }
    }

    /**
     * Engages a module using given name and its version ID.
     *
     * @param moduleName
     * @param versionID
     * @throws AxisFault
     */
    public void engageModule(String moduleName, String versionID)
            throws AxisFault {
        String actualName = Utils.getModuleName(moduleName, versionID);
        AxisModule module = getModule(actualName);
        if (module != null) {
            engageModule(module);
        } else {
            throw new AxisFault(Messages.getMessage("refertoinvalidmodule"));
        }
    }

    @Override
	public void onEngage(AxisModule module, AxisDescription engager) throws AxisFault {
        for(final AxisServiceGroup serviceGroup: getServiceGroups()) {
            serviceGroup.engageModule(module, engager);
        }
    }

    /**
     * To dis-engage a module from the system. This will remove all the handlers
     * belonging to this module from all the handler chains.
     *
     * @param module module to disengage
     */
    @Override
	public void onDisengage(AxisModule module) throws AxisFault {
        PhaseResolver phaseResolver = new PhaseResolver(this);
        phaseResolver.disengageModuleFromGlobalChains(module);

        for(final AxisServiceGroup axisServiceGroup: getServiceGroups()) {
            axisServiceGroup.disengageModule(module);
        }
    }

    public void notifyObservers(int event_type, AxisService service) {
        AxisEvent event = new AxisEvent(event_type);

        for (int i = 0; i < observersList.size(); i++) {
            AxisObserver axisObserver = observersList.get(i);

            try {
                if (!service.isClientSide()) {
                    axisObserver.serviceUpdate(event, service);
                }
            } catch (Throwable e) {
                // No need to stop the system due to this, so log and ignore
                log.debug(e);
            }
        }
    }

    public void notifyObservers(int event_type, AxisModule moule) {
        AxisEvent event = new AxisEvent(event_type);

        for (int i = 0; i < observersList.size(); i++) {
            AxisObserver axisObserver = observersList.get(i);

            try {
                axisObserver.moduleUpdate(event, moule);
            } catch (Throwable e) {
                // No need to stop the system due to this, so log and ignore
                log.debug(e);
            }
        }
    }

    public void notifyObservers(int event_type, AxisServiceGroup serviceGroup) {
        AxisEvent event = new AxisEvent(event_type);

        for (int i = 0; i < observersList.size(); i++) {
            AxisObserver axisObserver = observersList.get(i);

            try {
                axisObserver.serviceGroupUpdate(event, serviceGroup);
            } catch (Throwable e) {
                // No need to stop the system due to this, so log and ignore
                log.debug(e);
            }
        }
    }

    /**
     * Method removeService.
     *
     * @param name
     * @throws AxisFault
     */
    public synchronized void removeService(String name) throws AxisFault {
        AxisService service = allServices.remove(name);
        if (service != null) {
            AxisServiceGroup serviceGroup = service.getServiceGroup();
            serviceGroup.removeService(name);
            log.debug(Messages.getMessage("serviceremoved", name));
        }
    }

    /**
     * Add an AxisModule to the list of globally deployed modules.
     * <p/>
     * TODO: should this check for duplicate names?
     *
     * @param moduleName name of AxisModule to add to list.
     */
    public void addGlobalModuleRef(String moduleName) {
        globalModuleList.add(moduleName);
    }

    /**
     * Engage all the previously added global modules.
     *
     * @throws AxisFault if an individual engageModule() fails
     */
    public void engageGlobalModules() throws AxisFault {
        for (String string : globalModuleList) {
            engageModule(string);
        }
    }

    public Map<String, String> getFaultyModules() {
        return faultyModules;
    }

    public Map<String, String> getFaultyServices() {
        return faultyServices;
    }

    public void removeFaultyService(String key) {
        Iterator<String> itr = faultyServices.keySet().iterator();
        while (itr.hasNext()) {
            String fullFileName = itr.next();
            if (fullFileName.indexOf(key) > 0) {
                faultyServices.remove(fullFileName);
                return;
            }
        }
    }

    // to get the out flow correpodning to the global out flow;
    public List<Phase> getOutFlowPhases() {
        return this.outPhases;
    }

    /**
     * @return Returns ArrayList.
     */
    public List<Phase> getInFaultFlowPhases() {
        return inFaultPhases;
    }

    public List<Phase> getInFlowPhases() {
        return inPhasesUptoAndIncludingPostDispatch;
    }

    public MessageReceiver getMessageReceiver(String mepURL) {
        return messageReceivers.get(mepURL);
    }

    /**
     * @param contentType
     * @return the configured message builder implementation class name against
     *         the given content type.
     */
    public Builder getMessageBuilder(String contentType) {
        Builder builder = null;
        if (messageBuilders.isEmpty()) {
            return null;
        }
        if (contentType != null) {
            builder = messageBuilders.get(contentType);
            if (builder == null) {
                builder = messageBuilders.get(contentType.toLowerCase());
            }
            if (builder == null) {
                Iterator<Entry<String, Builder>> iterator = messageBuilders.entrySet().iterator();
                while (iterator.hasNext() && builder == null) {
                    Entry<String, Builder> entry = iterator.next();
                    String key = entry.getKey();
                    if (contentType.matches(key)) {
                        builder = entry.getValue();
                    }
                }
            }
        }
        return builder;
    }

    public Builder getMessageBuilder(String contentType, boolean defaultBuilder) {
        Builder builder = getMessageBuilder(contentType);
        if (builder == null && defaultBuilder){
            builder = new UnknownContentBuilder();
        }
        return builder;
    }

    /**
     * @param contentType
     * @return the configured message formatter implementation class name
     *         against the given content type.
     */
    public MessageFormatter getMessageFormatter(String contentType) {
        return messageFormatters.get(contentType);
    }

//    /**
//     *
//     * @deprecate Please use String version instead
//     * @param qname
//     * @return
//     */
//    public AxisModule getModule(QName qname) {
//        return getModule(qname.getLocalPart());
//    }

    /**
     * Method getModule. First it will check whether the given module is there
     * in the hashMap, if so returns that and the name, which can be either with
     * version string or without version string. <p/> If its not found and the
     * name does not contain the version string in it then checks whether the default
     * version of the module is available in the sytem for the given name, then returns
     * that.
     *
     * @param name
     * @return Returns ModuleDescription.
     */
    public AxisModule getModule(String name) {
        AxisModule module = allModules.get(name);
        if (module != null) {
            return module;
        }
        // checks whether the version string seperator is not there in the
        // module name
        String moduleName = name;
        String defaultModuleVersion = getDefaultModuleVersion(moduleName);
        if (defaultModuleVersion != null) {
            module =
                    allModules.get(Utils.getModuleName(moduleName,
                                                                    defaultModuleVersion));
            if (module != null) {
                return module;
            }
        }
        return null;
    }

    /**
     * Return the module having name=moduleName & version=moduleVersion
     *
     * @param moduleName    The module name
     * @param moduleVersion The version of the module
     * @return The AxisModule having name=moduleName & version=moduleVersion
     */
    public AxisModule getModule(String moduleName, String moduleVersion) {
        if (moduleVersion == null || moduleVersion.trim().length() == 0) {
            moduleVersion = getDefaultModuleVersion(moduleName);
        }
        return allModules.get(Utils.getModuleName(moduleName, moduleVersion));
    }

    /**
     * The class loader that becomes the parent of all the modules
     *
     * @return
     */
    public ClassLoader getModuleClassLoader() {
        return this.moduleClassLoader;
    }

    public ModuleConfiguration getModuleConfig(String moduleName) {
        return moduleConfigmap.get(moduleName);
    }

    /**
     * @return Returns HashMap.
     */
    public Map<String, AxisModule> getModules() {
        return allModules;
    }

    /**
     * @return Returns ArrayList.
     */
    public List<Phase> getOutFaultFlowPhases() {
        return outFaultPhases;
    }

    public PhasesInfo getPhasesInfo() {
        return phasesinfo;
    }

    public URL getRepository() {
        return axis2Repository;
    }

    /**
     * Method getService.
     *
     * @param name
     * @return Returns AxisService.
     */
    public AxisService getService(String name) throws AxisFault {
        AxisService axisService = allServices.get(name);
        if (axisService != null) {
            if (axisService.isActive()) {
                return axisService;
            } else {
                throw new AxisFault(Messages
                        .getMessage("serviceinactive", name));
            }
        } else {
            axisService = allEndpoints.get(name);
            if (axisService != null) {
                if (axisService.isActive()) {
                    return axisService;
                } else {
                    throw new AxisFault(Messages
                            .getMessage("serviceinactive", name));
                }
            }
        }
        return null;
    }

    /**
     * Service can start and stop, once stopped it cannot be accessed, so we
     * need a way to get the service even if service is not active.
     *
     * @return AxisService
     */
    public AxisService getServiceForActivation(String serviceName) {
        AxisService axisService = null;
        axisService = allServices.get(serviceName);
        if (axisService != null) {
            return axisService;
        } else {
            axisService = allEndpoints.get(serviceName);
            return axisService;
        }
    }

    // The class loader that becomes the parent of all the services
    public ClassLoader getServiceClassLoader() {
        return this.serviceClassLoader;
    }

    public AxisServiceGroup getServiceGroup(String serviceNameAndGroupString) {
        // return (AxisServiceGroup)
        // serviceGroups.get(serviceNameAndGroupString);
        return children.get(serviceNameAndGroupString);
    }

	public Iterable<AxisServiceGroup> getServiceGroups() {
        return children.values();
    }

    // To get all the services in the system
    public Map<String, AxisService> getServices() {
        return new HashMap<String, AxisService>(allServices);
    }

    // The class loader which become the top most parent of all the modules and
    // services
    public ClassLoader getSystemClassLoader() {
        return this.systemClassLoader;
    }

    public TransportInDescription getTransportIn(String name) {
        return transportsIn.get(name);
    }

    public TransportOutDescription getTransportOut(String name) {
        return transportsOut.get(name);
    }

    public Map<String, TransportInDescription> getTransportsIn() {
        return transportsIn;
    }

    public Map<String, TransportOutDescription> getTransportsOut() {
        return transportsOut;
    }

    @Override
	public boolean isEngaged(String moduleId) {
        AxisModule module = getModule(moduleId);
        if (module == null) {
            return false;
        }
        boolean isEngaged = super.isEngaged(module);
        if (!isEngaged) {
            AxisModule defaultModule = getDefaultModule(moduleId);
            isEngaged = engagedModules != null && engagedModules.values().contains(defaultModule);
        }
        return isEngaged;
    }

    @Override
	public boolean isEngaged(AxisModule axisModule) {
        boolean isEngaged = super.isEngaged(axisModule);
        if (!isEngaged) {
            isEngaged = engagedModules != null &&
                        engagedModules.values().contains(axisModule);
        }
        return isEngaged;
    }

    public void setGlobalOutPhase(List<Phase> outPhases) {
        this.outPhases = outPhases;
    }

    /**
     * @param list
     */
    public void setInFaultPhases(List<Phase> list) {
        inFaultPhases = list;
    }

    public void setInPhasesUptoAndIncludingPostDispatch(
    		List<Phase> inPhasesUptoAndIncludingPostDispatch) {
        this.inPhasesUptoAndIncludingPostDispatch = inPhasesUptoAndIncludingPostDispatch;
    }

    public void setModuleClassLoader(ClassLoader classLoader) {
        this.moduleClassLoader = classLoader;
    }

    /**
     * @param list
     */
    public void setOutFaultPhases(List<Phase> list) {
        outFaultPhases = list;
    }

    public void setPhasesInfo(PhasesInfo phasesInfo) {
        this.phasesinfo = phasesInfo;
    }

    public void setRepository(URL axis2Repository) {
        this.axis2Repository = axis2Repository;
    }

    public void setServiceClassLoader(ClassLoader classLoader) {
        this.serviceClassLoader = classLoader;
    }

    public void setSystemClassLoader(ClassLoader classLoader) {
        this.systemClassLoader = classLoader;
    }

    /**
     * Adds a default module version, which can be done either programmatically
     * or by using axis2.xml. The default module version is important if user
     * asks to engage a module without given version ID, in which case,
     * the default version is engaged.
     *
     * @param moduleName
     * @param moduleVersion
     */
    public void addDefaultModuleVersion(String moduleName, String moduleVersion) {
        if (nameToVersionMap.get(moduleName) == null) {
            nameToVersionMap.put(moduleName, moduleVersion);
        }
    }

    public String getDefaultModuleVersion(String moduleName) {
        return nameToVersionMap.get(moduleName);
    }

    public AxisModule getDefaultModule(String moduleName) {
        String defaultModuleVersion = getDefaultModuleVersion(moduleName);
        if (defaultModuleVersion == null) {
            return allModules.get(moduleName);
        } else {
            return allModules.get(moduleName + "-" + defaultModuleVersion);
        }
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public void stopService(String serviceName) throws AxisFault {
        AxisService service = allServices.get(serviceName);
        if (service == null) {
            throw new AxisFault(Messages.getMessage("servicenamenotvalid",
                                                    serviceName));
        }
        service.setActive(false);
        notifyObservers(AxisEvent.SERVICE_STOP, service);
    }

    public void startService(String serviceName) throws AxisFault {
        AxisService service = allServices.get(serviceName);
        if (service == null) {
            throw new AxisFault(Messages.getMessage("servicenamenotvalid",
                                                    serviceName));
        }
        service.setActive(true);
        notifyObservers(AxisEvent.SERVICE_START, service);
    }

    public List<AxisModule> getModulesForPolicyNamesapce(String namesapce) {
        return policySupportedModules.get(namesapce);
    }

    public void registerModulePolicySupport(AxisModule axisModule) {
        String[] namespaces = axisModule.getSupportedPolicyNamespaces();

        if (namespaces == null) {
            return;
        }

        List<AxisModule> modulesList;

        for (String namespace : namespaces) {
            modulesList = policySupportedModules.get(namespace);

            if (modulesList != null) {
                modulesList.add(axisModule);
            } else {
                modulesList = new ArrayList<AxisModule>();
                modulesList.add(axisModule);
                policySupportedModules.put(namespace, modulesList);
            }
        }
    }

    public void registerLocalPolicyAssertions(AxisModule axisModule) {
        QName[] localPolicyAssertions = axisModule.getLocalPolicyAssertions();

        if (localPolicyAssertions == null) {
            return;
        }

        for (QName localPolicyAssertion : localPolicyAssertions) {
            addLocalPolicyAssertion(localPolicyAssertion);
        }
    }

    public List<AxisObserver> getObserversList() {
        return observersList;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    /**
     * getTargetResolverChain returns an instance of
     * TargetResolver which iterates over the registered
     * TargetResolvers, calling each one in turn when
     * resolveTarget is called.
     */
    public TargetResolver getTargetResolverChain() {
        if (targetResolvers.isEmpty()) {
            return null;
        }
        return new TargetResolver() {
            public void resolveTarget(MessageContext messageContext) {
                Iterator<TargetResolver> iter = targetResolvers.iterator();
                while (iter.hasNext()) {
                    TargetResolver tr = iter.next();
                    tr.resolveTarget(messageContext);
                }
            }
        };
    }

    public void addTargetResolver(TargetResolver tr) {
        targetResolvers.add(tr);
    }

    public void addLocalPolicyAssertion(QName name) {
        this.localPolicyAssertions.add(name);
    }

    public List<QName> getLocalPolicyAssertions() {
        return this.localPolicyAssertions;
    }

    public void removeLocalPolicyAssertion(QName name) {
        this.localPolicyAssertions.remove(name);
    }

    public boolean isAssertionLocal(QName name) {
        return this.localPolicyAssertions.contains(name);
    }

    /**
     * Allows to define/configure Data Locator for specified dialect at Axis 2 Configuration.
     *
     * @param dialect-  an absolute URI represents the format and version of data
     * @param classname - class name of the Data Locator configured to support retrieval
     *                  for the specified dialect.
     */
    public void addDataLocatorClassNames(String dialect, String classname) {
        dataLocatorClassNames.put(dialect, classname);
    }

    /**
     * For internal used only! To store instance of DataLocator when it is first loaded. This allows to
     * reuse of the DataLocator after it is initially loaded.
     *
     * @param dialect-    an absolute URI represents the format and version of data
     * @param dataLocator - specified an DataLocator instance  to support retrieval
     *                    of the specified dialect.
     */
    public void addDataLocator(String dialect, AxisDataLocator dataLocator) {
        dataLocators.put(dialect, dataLocator);
    }

    /**
     * Return DataLocator instance for specified dialect.
     */
    public AxisDataLocator getDataLocator(String dialect) {
        return dataLocators.get(dialect);
    }


    /**
     * Return classname of DataLocator configured for specified dialect.
     */
    public String getDataLocatorClassName(String dialect) {
        return dataLocatorClassNames.get(dialect);
    }


    /**
     * Checks whether the system pre-defined phases
     * for all the flows, have been changed. If they have been changed, throws
     * a DeploymentException.
     *
     * @throws org.apache.axis2.deployment.DeploymentException
     *
     */
    public void validateSystemPredefinedPhases() throws DeploymentException {
        PhasesInfo phasesInfo = getPhasesInfo();
        setInPhasesUptoAndIncludingPostDispatch(phasesInfo.getGlobalInPhases());
        setInFaultPhases(phasesInfo.getGlobalInFaultPhases());
        setGlobalOutPhase(phasesInfo.getGlobalOutPhases());
        setOutFaultPhases(phasesInfo.getGlobalOutFaultPhases());
    }

    public AxisConfigurator getConfigurator() {
        return configurator;
    }

    public void setConfigurator(AxisConfigurator configurator) {
        this.configurator = configurator;
    }

    public void cleanup() {
        if (configurator != null) {
            configurator.cleanup();
        }
    }

    /**
     * This method can be used to insert a phase at the runtime for a given location
     * And the relative location can be specified by beforePhase and afterPhase. Parameters
     * Either or both of them can be null , if both the parameters are null then the phase
     * will be added some where in the global phase. If one of them are null then the phase
     * will be added
     *  - If the beforePhase is null then the phase will be added after the afterPhase
     *  - If the after phase is null then the phase will be added before the beforePhase
     * Type of the flow will be specified by the parameter flow.
     *   1 - Inflow
     *   2 - out flow
     *   3 - fault in flow
     *   4 - fault out flow
     *
     * @param d the Deployable representing the Phase to deploy
     * @param flow the type of the flow
     * @throws org.apache.axis2.AxisFault : If something went wrong
     */
    public void insertPhase(Deployable d, Flows flow) throws AxisFault {

        switch (flow) {
            case IN: {
                List<Phase> phaseList = phasesinfo.getAllInPhases();
                phaseList = findAndInsertPhase(d, phaseList);
                if (phaseList != null) {
                    phasesinfo.setInPhases(phaseList);
                }
                break;
            }
            case OUT: {
            	List<Phase> phaseList = phasesinfo.getAllOutPhases();
                phaseList = findAndInsertPhase(d, phaseList);
                if (phaseList != null) {
                    phasesinfo.setOutPhases(phaseList);
                }
                break;
            }
            case OUT_FAULT: {
            	List<Phase> phaseList = phasesinfo.getAllOutFaultPhases();
                phaseList = findAndInsertPhase(d, phaseList);
                if (phaseList != null) {
                    phasesinfo.setOutFaultPhases(phaseList);
                }
                break;
            }
            case IN_FAULT: {
            	List<Phase> phaseList = phasesinfo.getAllInFaultPhases();
                phaseList = findAndInsertPhase(d, phaseList);
                if (phaseList != null) {
                    phasesinfo.setInFaultPhases(phaseList);
                }
                break;
            }
        }
    }

    /**
     * Insert a Phase
     * @param d
     * @param phaseList
     * @return
     * @throws AxisFault
     */
    private List<Phase> findAndInsertPhase(Deployable d, List<Phase> phaseList) throws AxisFault {
        DeployableChain<Phase> ec = new DeployableChain<Phase>();
        String last = null;
        for (Phase phase2 : phaseList) {
            Phase phase = phase2;
            String name = phase.getName();
            Deployable existing = new Deployable(name);
            existing.setTarget(phase);
            if (last != null) {
                // Set up explicit chain relationship for preexisting phases, for now.
                ec.addRelationship(last, name);
            }
            last = name;
            try {
                ec.deploy(existing);
            } catch (Exception e) {
                // This should never happen when building a simple list like the above
                throw AxisFault.makeFault(e);
            }
        }

        try {
            ec.deploy(d);

            if (d.getTarget() == null) {
                Phase phase = new Phase();
                phase.setName(d.getName());
                d.setTarget(phase);
            }

            ec.rebuild();
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }

        phaseList = ec.getChain();

        return phaseList;
    }

    private void processEndpoints(AxisService axisService,
    		AxisConfiguration axisConfiguration) throws AxisFault {
        Map<String, AxisEndpoint> enspoints = axisService.getEndpoints();
        if (enspoints == null || enspoints.size() == 0) {
			org.apache.axis2.deployment.util.Utils.addEndpointsToService(
					axisService, axisConfiguration);
		}
	}

	@Override
	public AxisConfiguration getConfiguration() {
		return this;
	}

	@Override
	public Policy getEffectivePolicy() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}
