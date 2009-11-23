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

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.AlgorithmSuite;
import org.apache.ws.secpolicy.model.AsymmetricBinding;
import org.apache.ws.secpolicy.model.InitiatorToken;
import org.apache.ws.secpolicy.model.Layout;
import org.apache.ws.secpolicy.model.RecipientToken;

public class AsymmetricBindingBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory) throws IllegalArgumentException {
        
        AsymmetricBinding asymmetricBinding =  new AsymmetricBinding(SPConstants.SP_V11);
        
        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
        policy = (Policy) policy.normalize(false);
        
        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List) iterator.next(), asymmetricBinding);
            
            /*
             * since there should be only one alternative
             */
            break;
        }
        
        return asymmetricBinding;
    }
    
    private void processAlternative(List assertions, AsymmetricBinding asymmetricBinding) {
               
        Assertion assertion;
        QName name;
        
        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            assertion = (Assertion) iterator.next();
            name = assertion.getName();
            
            if (SP11Constants.INITIATOR_TOKEN.equals(name)) {
                asymmetricBinding.setInitiatorToken((InitiatorToken) assertion);
                
            } else if (SP11Constants.RECIPIENT_TOKEN.equals(name)){
                asymmetricBinding.setRecipientToken((RecipientToken) assertion);
                
            } else if (SP11Constants.ALGORITHM_SUITE.equals(name)) {
                asymmetricBinding.setAlgorithmSuite((AlgorithmSuite) assertion);
            
            } else if (SP11Constants.LAYOUT.equals(name)) {
                asymmetricBinding.setLayout((Layout) assertion);
                
            } else if (SP11Constants.INCLUDE_TIMESTAMP.equals(name)) {
                asymmetricBinding.setIncludeTimestamp(true);

            } else if (SPConstants.ENCRYPT_BEFORE_SIGNING.equals(name.getLocalPart())) {
                asymmetricBinding.setProtectionOrder(SPConstants.ENCRYPT_BEFORE_SIGNING);
                
            } else if (SPConstants.SIGN_BEFORE_ENCRYPTING.equals(name.getLocalPart())) {
                asymmetricBinding.setProtectionOrder(SPConstants.SIGN_BEFORE_ENCRYPTING);
                
            } else if (SPConstants.ENCRYPT_SIGNATURE.equals(name.getLocalPart())) {
                asymmetricBinding.setSignatureProtection(true);
                
            } else if (SPConstants.PROTECT_TOKENS.equals(name.getLocalPart())) {
                asymmetricBinding.setTokenProtection(true);
                
            } else if (SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY.equals(name.getLocalPart())) {
                asymmetricBinding.setEntireHeadersAndBodySignatures(true);
            }
        }
    }
    
    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.ASYMMETRIC_BINDING};
    }
    
}
 