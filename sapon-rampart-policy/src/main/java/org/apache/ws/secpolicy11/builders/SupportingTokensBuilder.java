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
import org.apache.ws.secpolicy.model.AlgorithmSuite;
import org.apache.ws.secpolicy.model.SignedEncryptedElements;
import org.apache.ws.secpolicy.model.SignedEncryptedParts;
import org.apache.ws.secpolicy.model.SupportingToken;
import org.apache.ws.secpolicy.model.Token;

public class SupportingTokensBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory)
            throws IllegalArgumentException {
        QName name = element.getQName();
        SupportingToken supportingToken = null;

        if (SP11Constants.SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SPConstants.SUPPORTING_TOKEN_SUPPORTING, SPConstants.SP_V11);
        } else if (SP11Constants.SIGNED_SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SPConstants.SUPPORTING_TOKEN_SIGNED, SPConstants.SP_V11);
        } else if (SP11Constants.ENDORSING_SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SPConstants.SUPPORTING_TOKEN_ENDORSING, SPConstants.SP_V11);
        } else if (SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SPConstants.SUPPORTING_TOKEN_SIGNED_ENDORSING, SPConstants.SP_V11);
        }

        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
        policy = (Policy) policy.normalize(false);

        for (List<PolicyComponent> alts: policy.getAlternatives()) {
            processAlternative(alts, supportingToken);
            // for the moment we will say there should be only one alternative
            break;
        }

        return supportingToken;
    }

    public QName[] getKnownElements() {
        return new QName[] {  SP11Constants.SUPPORTING_TOKENS,
                SP11Constants.SIGNED_SUPPORTING_TOKENS,
                SP11Constants.ENDORSING_SUPPORTING_TOKENS,
                SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS};
    }

    private void processAlternative(List<PolicyComponent> assertions,
    								SupportingToken supportingToken)
    {
        for (PolicyComponent pc: assertions) {
            Assertion primitive = (Assertion)pc;
            QName qname = primitive.getName();

            if (SP11Constants.ALGORITHM_SUITE.equals(qname)) {
                supportingToken
                        .setAlgorithmSuite((AlgorithmSuite) primitive);
            }
            else if (SP11Constants.SIGNED_PARTS.equals(qname)) {
                supportingToken
                	    .setSignedParts((SignedEncryptedParts) primitive);
            }
            else if (SP11Constants.SIGNED_ELEMENTS.equals(qname)) {
                supportingToken
                        .setSignedElements((SignedEncryptedElements) primitive);
            }
            else if (SP11Constants.ENCRYPTED_PARTS.equals(qname)) {
                supportingToken
                        .setEncryptedParts((SignedEncryptedParts) primitive);
            }
            else if (SP11Constants.ENCRYPTED_ELEMENTS.equals(qname)) {
                supportingToken
                        .setEncryptedElements((SignedEncryptedElements) primitive);
            }
            else if (primitive instanceof Token) {
                supportingToken.addToken((Token) primitive);
            }
        }
    }
}
