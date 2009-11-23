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
import org.apache.ws.secpolicy.model.Layout;

public class LayoutBuilder implements AssertionBuilder {
    
    

    public Assertion build(OMElement element, AssertionBuilderFactory factory) throws IllegalArgumentException {
        Layout layout = new Layout(SPConstants.SP_V11);
        
        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
        policy = (Policy) policy.normalize(false);
        
        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext(); ) {
            processAlternative((List) iterator.next(), layout);         
            break; // there should be only one alternative
        }
                        
        return layout;
    }
    
    public QName[] getKnownElements() {
        return new QName[] {SP11Constants.LAYOUT};
    }

    public void processAlternative(List assertions, Layout parent) {
        
        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion) iterator.next();
            QName qname = assertion.getName();
            
            if (SP11Constants.STRICT.equals(qname)) {
                parent.setValue(SPConstants.LAYOUT_STRICT);
            } else if (SP11Constants.LAX.equals(qname)) {
                parent.setValue(SPConstants.LAYOUT_LAX);
            } else if (SP11Constants.LAXTSFIRST.equals(qname)) {
                parent.setValue(SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST);
            } else if (SP11Constants.LAXTSLAST.equals(qname)) {
                parent.setValue(SPConstants.LAYOUT_LAX_TIMESTAMP_LAST);
            }
            
        }
    }
}
