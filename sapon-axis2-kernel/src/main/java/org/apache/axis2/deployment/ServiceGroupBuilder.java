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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.i18n.Messages;

public class ServiceGroupBuilder extends DescriptionBuilder {
    private OMElement serviceElement;
    private Map<String, AxisService> wsdlServices;

    public ServiceGroupBuilder(OMElement service, Map<String, AxisService> wsdlServices,
                               ConfigurationContext configCtx) {
        this.serviceElement = service;
        this.wsdlServices = wsdlServices;
        this.configCtx = configCtx;
        this.axisConfig = this.configCtx.getAxisConfiguration();
    }

    public List<AxisService> populateServiceGroup(AxisServiceGroup axisServiceGroup)
            throws DeploymentException {
        List<AxisService> serviceList = new ArrayList<AxisService>();

        try {

            // Processing service level parameters
            Iterable<OMElement> itr = serviceElement.getChildrenWithName(new QName(TAG_PARAMETER));
            processParameters(itr, axisServiceGroup, axisServiceGroup.getParent());

            Iterable<OMElement> moduleConfigs =
                    serviceElement.getChildrenWithName(new QName(TAG_MODULE_CONFIG));
            processServiceModuleConfig(moduleConfigs, axisServiceGroup.getParent(),
                                       axisServiceGroup);

            // processing service-wide modules which required to engage globally
            Iterable<OMElement> moduleRefs = serviceElement.getChildrenWithName(new QName(TAG_MODULE));
            processModuleRefs(moduleRefs, axisServiceGroup);

            Iterable<OMElement> serviceitr = serviceElement.getChildrenWithName(new QName(TAG_SERVICE));
            for(OMElement service: serviceitr) {
                OMAttribute serviceNameatt = service.getAttribute(new QName(ATTRIBUTE_NAME));
                if (serviceNameatt == null) {
                    throw new DeploymentException(
                            Messages.getMessage(DeploymentErrorMsgs.SERVICE_NAME_ERROR));
                }
                String serviceName = serviceNameatt.getAttributeValue();

                if (serviceName == null || "".equals(serviceName)) {
                    throw new DeploymentException(
                            Messages.getMessage(DeploymentErrorMsgs.SERVICE_NAME_ERROR));
                } else {
                    AxisService axisService = wsdlServices.get(serviceName);

                    if (axisService == null) {
                        axisService = new AxisService(serviceName);
                    } else {
                        axisService.setWsdlFound(true);
                        axisService.setCustomWsdl(true);
                    }

                    // the service that has to be deployed
                    axisService.setParent(axisServiceGroup);
                    axisService.setClassLoader(axisServiceGroup.getServiceGroupClassLoader());

                    ServiceBuilder serviceBuilder = new ServiceBuilder(configCtx, axisService);
                    AxisService as = serviceBuilder.populateService(service);
                    serviceList.add(as);
                }
            }
        } catch (AxisFault e) {
            throw new DeploymentException(e);
        }

        return serviceList;
    }

    /**
     * Gets the list of modules that is required to be engaged globally.
     *
     * @param moduleRefs <code>java.util.Iterator</code>
     * @throws DeploymentException <code>DeploymentException</code>
     */
    protected void processModuleRefs(Iterable<OMElement> moduleRefs, AxisServiceGroup axisServiceGroup)
            throws DeploymentException {
        try {
            for(OMElement moduleref: moduleRefs) {
                OMAttribute moduleRefAttribute = moduleref.getAttribute(new QName(TAG_REFERENCE));

                if (moduleRefAttribute != null) {
                    String refName = moduleRefAttribute.getAttributeValue();

                    if (axisConfig.getModule(refName) == null) {
                        throw new DeploymentException(
                                Messages.getMessage(DeploymentErrorMsgs.MODULE_NOT_FOUND, refName));
                    } else {
                        axisServiceGroup.addModuleref(refName);
                    }
                }
            }
        } catch (AxisFault axisFault) {
            throw new DeploymentException(axisFault);
        }
    }

    protected void processServiceModuleConfig(Iterable<OMElement> moduleConfigs, ParameterInclude parent,
                                              AxisServiceGroup axisService)
            throws DeploymentException {
        for(OMElement moduleConfig: moduleConfigs) {
            OMAttribute moduleName_att = moduleConfig.getAttribute(new QName(ATTRIBUTE_NAME));

            if (moduleName_att == null) {
                throw new DeploymentException(
                        Messages.getMessage(DeploymentErrorMsgs.INVALID_MODULE_CONFIG));
            } else {
                String module = moduleName_att.getAttributeValue();
                ModuleConfiguration moduleConfiguration =
                        new ModuleConfiguration(module, parent);
                Iterable<OMElement> parameters = moduleConfig.getChildrenWithName(new QName(TAG_PARAMETER));
                processParameters(parameters, moduleConfiguration, parent);
                axisService.addModuleConfig(moduleConfiguration);
            }
        }
    }
}
