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

package org.apache.synapse.mediators;

import javax.xml.namespace.QName;

import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.util.xpath.SynapseXPath;

/**
 * A mediator property is a name-value or name-expression pair which could be supplied
 * for certain mediators. If expressions are supplied they are evaluated at the runtime
 * against the current message into literal String values.
 */
public class MediatorProperty {

    // TODO: these constants are related to a specific configuration language
    //       and should be moved to a class in the related package
    public static final QName PROPERTY_Q  = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");
    public static final QName ATT_NAME_Q  = new QName(XMLConfigConstants.NULL_NAMESPACE, "name");
    public static final QName ATT_VALUE_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "value");
    public static final QName ATT_EXPR_Q  = new QName(XMLConfigConstants.NULL_NAMESPACE, "expression");

    private String name;
    private String value;
    private SynapseXPath expression;

    public MediatorProperty() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SynapseXPath getExpression() {
        return expression;
    }

    public void setExpression(SynapseXPath expression) {
        this.expression = expression;
    }

    public String getEvaluatedExpression(SynapseMessageContext synCtx) {
        return expression.stringValueOf(synCtx);
    }

}
