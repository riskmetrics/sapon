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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.SecureConversationToken;

public class SecureConversationTokenBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory)
            throws IllegalArgumentException {
        SecureConversationToken conversationToken = new SecureConversationToken(SPConstants.SP_V11);
        
        OMAttribute attribute = element.getAttribute(SP11Constants.INCLUDE_TOKEN);
        if (attribute == null) {
            throw new IllegalArgumentException(
                    "SecurityContextToken doesn't contain any sp:IncludeToken attribute");
        }
        
        String inclusionValue = attribute.getAttributeValue().trim();
        
        conversationToken.setInclusion(SP11Constants.getInclusionFromAttributeValue(inclusionValue));
        
        OMElement issuer = element.getFirstChildWithName(SP11Constants.ISSUER);
        if ( issuer != null) {
            conversationToken.setIssuerEpr(issuer.getFirstElement());
        }
        
        element = element.getFirstChildWithName(SPConstants.POLICY);
        if (element != null) {
            if (element.getFirstChildWithName(SP11Constants.REQUIRE_DERIVED_KEYS) != null) {
                conversationToken.setDerivedKeys(true);
            }

            if (element
                    .getFirstChildWithName(SP11Constants.REQUIRE_EXTERNAL_URI_REFERNCE) != null) {
                conversationToken.setRequireExternalUriRef(true);
            }

            if (element
                    .getFirstChildWithName(SP11Constants.SC10_SECURITY_CONTEXT_TOKEN) != null) {
                conversationToken.setSc10SecurityContextToken(true);
            }
            
            OMElement bootstrapPolicyElement = element.getFirstChildWithName(SP11Constants.BOOTSTRAP_POLICY);
            if (bootstrapPolicyElement != null) {
                Policy policy = PolicyEngine.getPolicy(bootstrapPolicyElement.getFirstElement());
                conversationToken.setBootstrapPolicy(policy);
            }
        }
        
        return conversationToken;
    }

    public QName[] getKnownElements() {
        return new QName[] {SP11Constants.SECURE_CONVERSATION_TOKEN};
    }

}
