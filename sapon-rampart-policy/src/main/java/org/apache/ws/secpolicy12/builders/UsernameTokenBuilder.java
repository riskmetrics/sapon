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

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.model.UsernameToken;

public class UsernameTokenBuilder implements AssertionBuilder {

    
    public Assertion build(OMElement element, AssertionBuilderFactory factory) throws IllegalArgumentException {
        UsernameToken usernameToken = new UsernameToken(SPConstants.SP_V12);
        
        OMAttribute attribute = element.getAttribute(SP12Constants.INCLUDE_TOKEN);
        
        if(attribute != null) {
            int inclusion = SP12Constants.getInclusionFromAttributeValue(attribute.getAttributeValue());
            usernameToken.setInclusion(inclusion);
        }
        
        OMElement policyElement = element.getFirstElement();
        
        if (policyElement != null && policyElement.getQName().equals(org.apache.neethi.Constants.Q_ELEM_POLICY)) {
        
            Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
            policy = (Policy) policy.normalize(false);
            
            for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
                processAlternative((List) iterator.next(), usernameToken);
                
                /*
                 * since there should be only one alternative
                 */
                break;
            }            
        }
        
        return usernameToken;
    }
        
    public QName[] getKnownElements() {
        return new QName[] {SP12Constants.USERNAME_TOKEN};
    }

    private void processAlternative(List assertions, UsernameToken parent) {
             
        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion) iterator.next();
            QName qname = assertion.getName();
            
            if (SP12Constants.WSS_USERNAME_TOKEN10.equals(qname)) {
                parent.setUseUTProfile10(true);               
            } else if (SP12Constants.WSS_USERNAME_TOKEN11.equals(qname)) {
                parent.setUseUTProfile11(true);
            } else if (SP12Constants.NO_PASSWORD.equals(qname)) {
                parent.setNoPassword(true);
            } else if (SP12Constants.HASH_PASSWORD.equals(qname)) {
                parent.setHashPassword(true);
            } else if (SP12Constants.REQUIRE_DERIVED_KEYS.equals(qname)) {
                parent.setDerivedKeys(true);
            } else if (SP12Constants.REQUIRE_EXPLICIT_DERIVED_KEYS.equals(qname)) {
                parent.setExplicitDerivedKeys(true);
            } else if (SP12Constants.REQUIRE_IMPLIED_DERIVED_KEYS.equals(qname)) {
                parent.setImpliedDerivedKeys(true);
            }
        }
    }
}
