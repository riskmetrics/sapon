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
package org.apache.ws.secpolicy12.builders;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.AlgorithmSuite;
import org.apache.ws.secpolicy.model.Layout;
import org.apache.ws.secpolicy.model.ProtectionToken;
import org.apache.ws.secpolicy.model.SymmetricBinding;

public class SymmetricBindingBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory) throws IllegalArgumentException {
        SymmetricBinding symmetricBinding = new SymmetricBinding(SPConstants.SP_V12);

        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
        policy = (Policy) policy.normalize(false);

        for (List<PolicyComponent> pc: policy.getAlternatives()) {
            processAlternatives(pc, symmetricBinding);

            break;
        }
        return symmetricBinding;
    }

    public QName[] getKnownElements() {
        return new QName[] {SP12Constants.SYMMETRIC_BINDING};
    }

    private void processAlternatives(List<PolicyComponent> assertions,
    								 SymmetricBinding symmetricBinding)
    {
        for (Object element : assertions) {
            Assertion assertion = (Assertion) element;
            QName name = assertion.getName();

            if (SP12Constants.ALGORITHM_SUITE.equals(name)) {
                symmetricBinding.setAlgorithmSuite((AlgorithmSuite) assertion);
            }
            else if (SP12Constants.LAYOUT.equals(name)) {
                symmetricBinding.setLayout((Layout) assertion);
            }
            else if (SP12Constants.INCLUDE_TIMESTAMP.equals(name)) {
                symmetricBinding.setIncludeTimestamp(true);
            }
            else if (SP12Constants.PROTECTION_TOKEN.equals(name)) {
                symmetricBinding.setProtectionToken((ProtectionToken) assertion);
            }
            else if (SP12Constants.ENCRYPT_BEFORE_SIGNING.equals(name)) {
                symmetricBinding.setProtectionOrder(SPConstants.ENCRYPT_BEFORE_SIGNING);
            }
            else if (SP12Constants.SIGN_BEFORE_ENCRYPTING.equals(name)) {
                symmetricBinding.setProtectionOrder(SPConstants.SIGN_BEFORE_ENCRYPTING);
            }
            else if (SP12Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY.equals(name)) {
                symmetricBinding.setEntireHeadersAndBodySignatures(true);
            }
            else if (SP12Constants.ENCRYPT_SIGNATURE.equals(name)) {
                symmetricBinding.setSignatureProtection(true);
            }
        }
    }
}
