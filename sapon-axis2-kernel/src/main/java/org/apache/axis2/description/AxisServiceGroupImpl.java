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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.neethi.Policy;

public class AxisServiceGroupImpl extends AxisDescriptionBase
	implements AxisServiceGroup
{
    // to store module ref at deploy time parsing
    private final List<String> modulesList = new ArrayList<String>();

    private Map<String, ModuleConfiguration> moduleConfigs;

    private Map<String, AxisService> services
    	= new HashMap<String, AxisService>();

    private ClassLoader serviceGroupClassLoader;

    private String name;

    private AxisConfiguration config;

    public AxisServiceGroupImpl() {
        moduleConfigs = new HashMap<String, ModuleConfiguration>();
    }

    public AxisServiceGroupImpl(AxisConfiguration axisConfig) {
        this();
        config = axisConfig;
    }

    public void setAxisConfiguration(AxisConfiguration axisConfig) {
    	this.config = axisConfig;
    	this.parameterInclude.setParent(axisConfig);
    	this.policySubject.setParent(axisConfig);
    }

    /**
     * Adds module configuration, if there is moduleConfig tag in service.
     *
     * @param moduleConfiguration the ModuleConfiguration to add
     */
    public void addModuleConfig(ModuleConfiguration moduleConfiguration) {
        if (moduleConfigs == null) {
            moduleConfigs = new HashMap<String, ModuleConfiguration>();
        }

        moduleConfigs.put(moduleConfiguration.getModuleName(), moduleConfiguration);
    }

    public void addModuleRef(String moduleref) {
        modulesList.add(moduleref);
    }

    public void addService(AxisService service) throws AxisFault {
        if (service == null) {
            return;
        }

        if (name == null) {
            //use name of first service as default group name
            name = service.getName();
        }

        service.setParent(this);
        for(AxisModule axisModule: getEngagedModules()) {
            service.engageModule(axisModule);
        }
        service.setLastUpdate();

        AxisConfiguration axisConfig = getConfiguration();
        if (axisConfig != null) {
            axisConfig.addToAllServicesMap(service);
        }

        services.put(service.getName(), service);
    }

    /**
     * When a module gets engaged on a ServiceGroup, we have to engage it for each Service.
     *
     * @param module the newly-engaged AxisModule
     * @param engager
     * @throws AxisFault if there is a problem
     */
    @Override
	protected void onEngage(AxisModule module, AxisDescription engager) throws AxisFault {
        for (final AxisService axisService: getServices()) {
            axisService.engageModule(module, engager);
        }
    }

    @Override
	public void onDisengage(AxisModule module) throws AxisFault {
        for (final AxisService axisService: getServices()) {
            axisService.disengageModule(module);
        }
    }

    public AxisService removeService(String name) {
        AxisService service = services.remove(name);
        if (service != null) {
            getConfiguration().notifyObservers(AxisEvent.SERVICE_REMOVE, service);
        }
        return service;
    }

    public ModuleConfiguration getModuleConfig(String moduleName) {
        return moduleConfigs.get(moduleName);
    }

    public List<String> getModuleRefs() {
        return modulesList;
    }

    public AxisService getService(String name) {
        return services.get(name);
    }

    public ClassLoader getServiceGroupClassLoader() {
        return serviceGroupClassLoader;
    }

    public String getName() {
        // Note: if the serviceGroupName is not set, then this could be null.
        // If the serviceGroupName has not been set and a service is added to this group,
        // then the serviceGroupName will default to the name of the first service
        return name;
    }

    public Collection<AxisService> getServices() {
        return services.values();
    }

    public void setServiceGroupClassLoader(ClassLoader serviceGroupClassLoader) {
        this.serviceGroupClassLoader = serviceGroupClassLoader;
    }

    public void setName(String serviceGroupName) {
        this.name = serviceGroupName;
    }

	@Override
	public AxisConfiguration getConfiguration() {
		return config;
	}

	@Override
	public Policy getEffectivePolicy() {
		return getEffectivePolicy(null);
	}

	@Override
	public Iterable<? extends AxisDescription> getChildrenAsDescriptions() {
		return getServices();
	}
}
