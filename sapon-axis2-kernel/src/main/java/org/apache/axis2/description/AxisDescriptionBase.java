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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.modules.Module;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.Utils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.wsdl.WSDLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyReference;

public abstract class AxisDescriptionBase
	implements AxisDescription
{
    private static final Log log = LogFactory.getLog(AxisDescriptionBase.class);

    protected ParameterIncludeMixin parameterInclude = new ParameterIncludeMixin();
    protected PolicySubjectMixin policySubject = new PolicySubjectMixin();

    protected Map<String, AxisModule> engagedModules;

    private final OMFactory omFactory = OMAbstractFactory.getOMFactory();

    // Holds the documentation details for each element
    private OMNode documentation;

    protected void setPolicySubjectMixin(PolicySubjectMixin mixin) {
    	this.policySubject = mixin;
    }

    protected void setParameterIncludeMixin(ParameterIncludeMixin mixin) {
    	this.parameterInclude = mixin;
    }

    @Override
    public void addParameter(Parameter param) throws AxisFault {
        parameterInclude.addParameter(param);
    }

    public void addParameter(String name, Object value) throws AxisFault {
        addParameter(new Parameter(name, value));
    }

    @Override
    public void removeParameter(Parameter param) throws AxisFault {
        parameterInclude.removeParameter(param);
    }

    @Override
    public void deserializeParameters(OMElement parameterElement) throws AxisFault
    {
        parameterInclude.deserializeParameters(parameterElement);
    }

    /**
     * If the parameter is found in the current description then the Parameter
     * will be writable else it will be read only
     *
     * @param name name of Parameter to retrieve
     * @return the Parameter, if found anywhere in the stack, or null if not
     */
    @Override
    public Parameter getParameter(String name) {
        return parameterInclude.getParameter(name);
    }

    public Object getParameterValue(String name) {
        Parameter param = getParameter(name);
        if (param == null) {
            return null;
        }
        return param.getValue();
    }

    public boolean isParameterTrue(String name) {
        Parameter param = getParameter(name);
        return param != null && JavaUtils.isTrue(param.getValue());
    }

    @Override
    public List<Parameter> getParameters() {
        return parameterInclude.getParameters();
    }

    @Override
    public boolean isParameterLocked(String parameterName) {
    	return parameterInclude.isParameterLocked(parameterName);
    }

    public String getDocumentation() {
        if (documentation != null) {
            if (documentation.getType() == OMNode.TEXT_NODE) {
                return ((OMText)documentation).getText();
            } else {
                StringWriter writer = new StringWriter();
                documentation.build();
                try {
                    documentation.serialize(writer);
                } catch (XMLStreamException e) {
                    log.error(e);
                }
                writer.flush();
                return writer.toString();
            }
        }
        return null;
    }

    public OMNode getDocumentationNode() {
        return documentation;
    }

    public void setDocumentation(OMNode documentation) {
        this.documentation = documentation;
    }

    public void setDocumentation(String documentation) {
        if (!"".equals(documentation)) {
            this.documentation = omFactory.createOMText(documentation);
        }
    }

    /**
     * Set the given policy as this AxisDescription's default. Furthermore:
     * <ol>
     * 	<li>Engage modules necessary for executing the new effective policy.</li>
     *  <li>Disengage modules that are not necessary for executing the new effective policy</li>
     *  <li>Check whether each module can execute the new effective policy</li>
     *  <li>Notify each module about the new effective policy.</li>
     * </ol>
     *
     * @param policy the new policy of this AxisDescription instance. The effective policy is the
     *               merge of this argument with effective policy of parent of this
     *               AxisDescription.
     * @throws AxisFault if any module is unable to execute the effective policy of this
     *                   AxisDescription instance successfully or no module to execute some portion
     *                   (one or more PrimtiveAssertions ) of that effective policy.
     */
    public void applyPolicy(Policy policy) throws AxisFault {
        // sets AxisDescription policy
        policySubject.clearPolicyComponents();
        policySubject.attachPolicy(policy);

        /*
           * now we try to engage appropriate modules based on the merged policy
           * of axis description object and the corresponding axis binding
           * description object.
           */
        applyPolicy();
    }

    /**
     * Applies the policies on the Description Hierarchy recursively.
     *
     * @throws AxisFault an error occurred applying the policy
     */
    public void applyPolicy() throws AxisFault {
        AxisConfiguration configuration = getConfiguration();
        if (configuration == null) {
            return;
        }

        Policy applicablePolicy = getApplicablePolicy(this);
        if (applicablePolicy != null) {
            engageModulesForPolicy(applicablePolicy, configuration);
        }

        //TODO: make sure implementations apply policy recursively.
//        for (final AxisDescription child: getChildren()) {
//            child.applyPolicy();
//        }
    }



    private boolean canSupportAssertion(Assertion assertion, List<AxisModule> moduleList) {

        Module module;

        for (AxisModule axisModule : moduleList) {
            // FIXME is this step really needed ??
            // Shouldn't axisMoudle.getModule always return not-null value ??
            module = axisModule.getModule();

            if (!(module == null || module.canSupportAssertion(assertion))) {
                log.debug(axisModule.getName() + " says it can't support " + assertion.getName());
                return false;
            }
        }

        return true;
    }

    private void engageModulesForPolicy(Policy policy, AxisConfiguration axisConfiguration)
            throws AxisFault {
        /*
           * for the moment we consider policies with only one alternative. If the
           * policy contains multiple alternatives only the first alternative will
           * be considered.
           */
        Iterator<List<PolicyComponent>> iterator = policy.getAlternatives().iterator();
        if (!iterator.hasNext()) {
            throw new AxisFault("Policy doesn't contain any policy alternatives");
        }

        List<PolicyComponent> assertionList = iterator.next();

        String namespaceURI;

        List<AxisModule> moduleList;

        List<String> namespaceList = new ArrayList<String>();
        List<AxisModule> modulesToEngage = new ArrayList<AxisModule>();

        for (PolicyComponent anAssertionList : assertionList) {
            Assertion assertion = (Assertion)anAssertionList;
            namespaceURI = assertion.getName().getNamespaceURI();

            moduleList = axisConfiguration.getModulesForPolicyNamesapce(namespaceURI);

            if (moduleList == null) {
                log.debug("can't find any module to process " + assertion.getName() +
                          " type assertions");
                continue;
            }

            if (!canSupportAssertion(assertion, moduleList)) {
                throw new AxisFault("atleast one module can't support " + assertion.getName());
            }

            if (!namespaceList.contains(namespaceURI)) {
                namespaceList.add(namespaceURI);
                modulesToEngage.addAll(moduleList);
            }
        }
        engageModulesToAxisDescription(modulesToEngage, this);
    }

    private void engageModulesToAxisDescription(List<AxisModule> moduleList, AxisDescription description)
            throws AxisFault {

        AxisModule axisModule;
        Module module;

        for (Object aModuleList : moduleList) {
            axisModule = (AxisModule)aModuleList;
            // FIXME is this step really needed ??
            // Shouldn't axisMoudle.getModule always return not-null value ??
            module = axisModule.getModule();

            if (!(module == null || description.isEngaged(axisModule.getName()))) {
                // engages the module to AxisDescription
                description.engageModule(axisModule);
                // notifies the module about the engagement
                axisModule.getModule().engageNotify(description);
            }
        }
    }

    /**
     * Engage a Module at this level
     *
     * @param axisModule the Module to engage
     * @throws AxisFault if there's a problem engaging
     */
    public void engageModule(AxisModule axisModule) throws AxisFault {
        engageModule(axisModule, this);
    }

    /**
     * Engage a Module at this level, keeping track of which level the engage was originally called
     * from.  This is meant for internal use only.
     *
     * @param axisModule module to engage
     * @param source     the AxisDescription which originally called engageModule()
     * @throws AxisFault if there's a problem engaging
     */
    public void engageModule(AxisModule axisModule, AxisDescription source) throws AxisFault {
        if (engagedModules == null) {
			engagedModules = new ConcurrentHashMap<String, AxisModule>();
		}
        String moduleName = axisModule.getName();
        for (AxisModule tempAxisModule: engagedModules.values()) {
            String tempModuleName = tempAxisModule.getName();
            if (moduleName.equals(tempModuleName)) {
                String existing = tempAxisModule.getVersion();
                if (!Utils.checkVersion(axisModule.getVersion(), existing)) {
                    throw new AxisFault(Messages.getMessage("mismatchedModuleVersions",
                                                            getClass().getName(),
                                                            moduleName,
                                                            existing));
                }
            }

        }

        // Let the Module know it's being engaged.  If it's not happy about it, it can throw.
        Module module = axisModule.getModule();
        if (module != null) {
            module.engageNotify(this);
        }

        // If we have anything specific to do, let that happen
        onEngage(axisModule, source);

        engagedModules.put(Utils.getModuleName(axisModule.getName(), axisModule.getVersion()),
                           axisModule);
    }

    protected void onEngage(AxisModule module, AxisDescription engager)
            throws AxisFault {
        // Default version does nothing, feel free to override
    }

    static Collection<AxisModule> NULL_MODULES = new ArrayList<AxisModule>(0);

    public Collection<AxisModule> getEngagedModules() {
        return engagedModules == null ? NULL_MODULES : engagedModules.values();
    }

    /**
     * Check if a given module is engaged at this level.
     *
     * @param moduleName module to investigate.
     * @return true if engaged, false if not. TODO: Handle versions? isEngaged("addressing") should
     *         be true even for versioned modulename...
     */
    public boolean isEngaged(String moduleName) {
        return engagedModules != null
               && engagedModules.keySet().contains(moduleName);
    }

    public boolean isEngaged(AxisModule axisModule) {
        String id = Utils.getModuleName(axisModule.getName(), axisModule
                .getVersion());
        return engagedModules != null && engagedModules.keySet().contains(id);
    }

    public void disengageModule(AxisModule module) throws AxisFault {
        if (module == null || engagedModules == null) {
			return;
		}
        // String id = Utils.getModuleName(module.getName(),
        // module.getVersion());
        if (isEngaged(module)) {
            onDisengage(module);
            engagedModules.remove(Utils.getModuleName(module.getName(), module
                    .getVersion()));
        }
    }

    protected void onDisengage(AxisModule module) throws AxisFault {
        // Base version does nothing
    }

    private static Policy getApplicablePolicy(AxisDescription axisDescription) {
        if (axisDescription instanceof AxisMessage) {
            AxisMessage axisMessage = (AxisMessage)axisDescription;
            AxisOperation axisOperation = axisMessage.getAxisOperation();
            if (axisOperation != null) {
                AxisService axisService = axisOperation.getService();
                if (axisService != null) {
                    if (axisService.getEndpointName() != null) {
                        AxisEndpoint axisEndpoint =
                                axisService.getEndpoint(axisService.getEndpointName());
                        if (axisEndpoint != null) {
                            AxisBinding axisBinding = axisEndpoint.getBinding();
                            AxisBindingOperation axisBindingOperation =
                                    axisBinding.getBindingOperation(axisOperation.getName());
                            String direction = axisMessage.getDirection();
                            AxisBindingMessage axisBindingMessage;
                            if (WSDLConstants.WSDL_MESSAGE_DIRECTION_IN.equals(direction)
                                && WSDLUtil
                                    .isInputPresentForMEP(axisOperation
                                            .getMessageExchangePattern())) {
                                axisBindingMessage = axisBindingOperation
                                        .getChild(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                                return axisBindingMessage.getEffectivePolicy();

                            } else if (WSDLConstants.WSDL_MESSAGE_DIRECTION_OUT
                                    .equals(direction)
                                       && WSDLUtil
                                    .isOutputPresentForMEP(axisOperation
                                            .getMessageExchangePattern())) {
                                axisBindingMessage = axisBindingOperation
                                        .getChild(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
                                return axisBindingMessage.getEffectivePolicy();
                            }
                        }

                    }
                }
            }
            return ((AxisMessage)axisDescription).getEffectivePolicy();
        }
        return null;
    }

	@Override
	public void addParameterObserver(ParameterObserver observer) {
		parameterInclude.addParameterObserver(observer);
	}

	@Override
	public void removeParameterObserver(ParameterObserver observer) {
		parameterInclude.removeParameterObserver(observer);
	}

	@Override
	public void attachPolicy(Policy policy) {
		policySubject.attachPolicy(policy);
	}

	@Override
	public void attachPolicyComponent(PolicyComponent policyComponent) {
		policySubject.attachPolicyComponent(policyComponent);
	}

	@Override
	public void attachPolicyComponent(String key, PolicyComponent policyComponent) {
		policySubject.attachPolicyComponent(key, policyComponent);
	}

	@Override
	public void attachPolicyComponents(Collection<PolicyComponent> policyComponents) {
		policySubject.attachPolicyComponents(policyComponents);
	}

	@Override
	public void attachPolicyReference(PolicyReference reference) {
		policySubject.attachPolicyReference(reference);
	}

	@Override
	public PolicyComponent detachPolicyComponent(String key) {
		return policySubject.detachPolicyComponent(key);
	}

	@Override
	public PolicyComponent getAttachedPolicyComponent(String key) {
		return policySubject.getAttachedPolicyComponent(key);
	}

	@Override
	public Collection<PolicyComponent> getAttachedPolicyComponents() {
		return policySubject.getAttachedPolicyComponents();
	}

	@Override
	public void clearPolicyComponents() {
		policySubject.clearPolicyComponents();
	}

	protected Policy getEffectivePolicy(AxisService axisService) {
		return policySubject.getEffectivePolicy(axisService);
	}

	@Override
	public Iterator<PolicyComponent> getEffectivePolicyComponents() {
		return policySubject.getEffectivePolicyComponents();
	}

	@Override
	public Date getLastPolicyUpdateTime() {
		return policySubject.getLastUpdatedTime();
	}

	@Override
	public boolean isPolicyUpdated() {
		return policySubject.isPolicyUpdated();
	}

	@Override
	public void updatePolicy(Policy policy) {
		policySubject.updatePolicy(policy);
	}

}
