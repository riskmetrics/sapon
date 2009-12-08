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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.IssuedToken;

public class IssuedTokenBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory)
            throws IllegalArgumentException {
        IssuedToken issuedToken = new IssuedToken(SPConstants.SP_V11);

        OMAttribute  includeAttr = element.getAttribute(SP11Constants.INCLUDE_TOKEN);
        if(includeAttr != null) {
            issuedToken.setInclusion(SP11Constants.getInclusionFromAttributeValue(includeAttr.getAttributeValue()));
        }
        // Extract Issuer
        OMElement issuerElem = element.getFirstChildWithName(SP11Constants.ISSUER);
        if(issuerElem != null) {
            OMElement issuerEpr = issuerElem.getFirstChildWithName(new QName(AddressingConstants.Final.WSA_NAMESPACE,"Address"));

            //try the other addressing namespace
            if (issuerEpr == null) {
                issuerEpr = issuerElem.getFirstChildWithName(new QName(AddressingConstants.Submission.WSA_NAMESPACE,"Address"));
            }

            issuedToken.setIssuerEpr(issuerEpr);
        }

        //TODO check why this returns an Address element
        //iter = issuerElem.getChildrenWithLocalName("Metadata");

        if (issuerElem != null ) {
            OMElement issuerMex = issuerElem.getFirstChildWithName(new QName(AddressingConstants.Final.WSA_NAMESPACE,"Metadata"));

          //try the other addressing namespace
            if (issuerMex == null) {
                issuerMex = issuerElem.getFirstChildWithName(new QName(AddressingConstants.Submission.WSA_NAMESPACE,"Metadata"));
            }

            issuedToken.setIssuerMex(issuerMex);
        }


        // Extract RSTTemplate
        OMElement rstTmplElem = element.getFirstChildWithName(SP11Constants.REQUEST_SECURITY_TOKEN_TEMPLATE);
        if (rstTmplElem != null) {
            issuedToken.setRstTemplate(rstTmplElem);
        }

        OMElement policyElement = element.getFirstChildWithName(org.apache.neethi.Constants.Q_ELEM_POLICY);

        if (policyElement != null) {

            Policy policy = PolicyEngine.getPolicy(policyElement);
            policy = (Policy) policy.normalize(false);

            for (List<PolicyComponent> alts: policy.getAlternatives()) {
                processAlternative(alts, issuedToken);
                break; // since there should be only one alternative ..
            }
        }

        return issuedToken;
    }

    public QName[] getKnownElements() {
        return new QName[] { SP11Constants.ISSUED_TOKEN };
    }

    private void processAlternative(List<PolicyComponent> assertions,
    								IssuedToken parent)
    {
        for (PolicyComponent assertion: assertions) {
            QName name = ((Assertion)assertion).getName();

            if (SP11Constants.REQUIRE_DERIVED_KEYS.equals(name)) {
                parent.setDerivedKeys(true);
            } else if (SP11Constants.REQUIRE_EXTERNAL_REFERNCE.equals(name)) {
                parent.setRequireExternalReference(true);
            } else if (SP11Constants.REQUIRE_INTERNAL_REFERNCE.equals(name)) {
                parent.setRequireInternalReference(true);
            }
        }

    }
}
