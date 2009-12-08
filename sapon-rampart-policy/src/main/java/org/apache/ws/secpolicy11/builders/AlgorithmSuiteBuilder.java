/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ws.secpolicy11.builders;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.WSSPolicyException;
import org.apache.ws.secpolicy.model.AlgorithmSuite;

public class AlgorithmSuiteBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory) throws IllegalArgumentException {

        AlgorithmSuite algorithmSuite = new AlgorithmSuite(SPConstants.SP_V11);

        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
        policy = (Policy) policy.normalize(false);

        List<PolicyComponent> assertions
        	= policy.getAlternatives().iterator().next();

        processAlternative(assertions, algorithmSuite);
        return algorithmSuite;
    }

    private void processAlternative(List<PolicyComponent> assertions, AlgorithmSuite algorithmSuite) {
        Assertion assertion = ((Assertion) assertions.get(0));
        String name = assertion.getName().getLocalPart();
        try {
            algorithmSuite.setAlgorithmSuite(name);
        } catch (WSSPolicyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public QName[] getKnownElements() {
        return new QName[] {SP11Constants.ALGORITHM_SUITE};
    }
}
