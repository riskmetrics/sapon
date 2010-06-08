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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.hierarchy.OperationDescendant;
import org.apache.axis2.description.java2wsdl.Java2WSDLConstants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.phaseresolver.PhaseResolver;
import org.apache.axis2.wsdl.SOAPHeaderMessage;
import org.apache.neethi.Policy;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;


/**
 * This class represents the messages in WSDL. There can be message element in
 * services.xml which are represented by this class.
 */
public class AxisMessage extends AxisDescriptionBase
	implements OperationDescendant
{
    private List<Phase> handlerChain;
    private String name;
    private List<SOAPHeaderMessage> soapHeaders;

    //to keep data in WSDL message reference and to keep the Java2WSDL data
    // such as SchemaElementName , direction etc.
    private QName elementQname;
    private String direction;
    private String messagePartName;

    // To store deploy-time module refs
    private List<String> modulerefs;
    private String partName = Java2WSDLConstants.PARAMETERS;

    private AxisOperation parent;

    public String getMessagePartName() {
		return messagePartName;
	}

	public void setMessagePartName(String messagePartName) {
		this.messagePartName = messagePartName;
	}

	public AxisMessage() {
        soapHeaders = new ArrayList<SOAPHeaderMessage>();
        handlerChain = new ArrayList<Phase>();
        modulerefs = new ArrayList<String>();
    }

	public void setParent(AxisOperation parentOp) {
		this.parent = parentOp;
		this.parameterInclude.setParent(parentOp);
		this.policySubject.setParent(parentOp);
	}

    public List<Phase> getMessageFlow() {
        return handlerChain;
    }

    public void setMessageFlow(List<Phase> operationFlow) {
        this.handlerChain = operationFlow;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public QName getElementQName() {
        return this.elementQname;
    }

    public void setElementQName(QName element) {
        this.elementQname = element;
    }

    public XmlSchemaElement getSchemaElement() {
        XmlSchemaElement xmlSchemaElement = null;
        AxisService service = getService();
        for (final XmlSchema schema: service.getSchema()) {
        	xmlSchemaElement = getSchemaElement(schema);
            if (xmlSchemaElement != null){
                break;
            }
        }
        return xmlSchemaElement;
    }

    private XmlSchemaElement getSchemaElement(XmlSchema schema) {
    	if(schema == null) {
    		return null;
    	}

    	XmlSchemaElement xmlSchemaElement
        	= schema.getElementByName(this.elementQname);

    	if(xmlSchemaElement != null) {
    		return xmlSchemaElement;
    	}

    	// try to find in an import or an include
    	XmlSchemaObjectCollection includes = schema.getIncludes();
    	if (includes != null) {
    		Iterator<?> includesIter = includes.getIterator();
    		while (includesIter.hasNext()) {
    			Object object = includesIter.next();
    			if (object instanceof XmlSchemaExternal) {
    				XmlSchema nextSchema = ((XmlSchemaExternal) object).getSchema();
    				xmlSchemaElement = getSchemaElement(nextSchema);
        			if (xmlSchemaElement != null){
        				break;
        			}
    			} else {
    				continue;
    			}
    		}
    	}

        return xmlSchemaElement;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addSoapHeader(SOAPHeaderMessage soapHeaderMessage) {
        soapHeaders.add(soapHeaderMessage);
    }

    public List<SOAPHeaderMessage> getSoapHeaders() {
        return soapHeaders;
    }

    /**
     * We do not support adding module operations when engaging a module to an AxisMessage
     *
     * @param axisModule AxisModule to engage
     * @param engager
     * @throws AxisFault something went wrong
     */
    @Override
	public void onEngage(AxisModule axisModule, AxisDescription engager) throws AxisFault {
        PhaseResolver phaseResolver = new PhaseResolver(getConfiguration());
        phaseResolver.engageModuleToMessage(this, axisModule);
    }

    public List<String> getModulerefs() {
        return modulerefs;
    }

    public void addModuleRefs(String moduleName) {
        modulerefs.add(moduleName);
    }

    public AxisOperation getAxisOperation(){
        return parent;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

	@Override
	public AxisConfiguration getConfiguration() {
		return parent.getConfiguration();
	}

	@Override
	public AxisOperation getOperation() {
		return parent;
	}

	@Override
	public AxisService getService() {
		return parent.getService();
	}

	@Override
	public AxisServiceGroup getServiceGroup() {
		return parent.getServiceGroup();
	}

	@Override
	public Policy getEffectivePolicy() {
		return getEffectivePolicy(getService());
	}

	@Override
	public Iterable<AxisDescription> getChildrenAsDescriptions() {
		return Collections.emptyList();
	}
}
