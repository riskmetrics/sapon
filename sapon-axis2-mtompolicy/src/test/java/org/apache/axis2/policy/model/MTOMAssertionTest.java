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

package org.apache.axis2.policy.model;

import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyEngine;

public class MTOMAssertionTest extends TestCase {


    public void testSymmBinding() {
        try {
            Policy p = this.getPolicy(
            	getClass().getResourceAsStream("/policy-mtom-security.xml"));
            List<PolicyComponent> assertions = p.getAlternatives().next();

            boolean isMTOMAssertionFound = false;

            for(PolicyComponent assertion: assertions) {
                if (assertion instanceof MTOM10Assertion) {
                    isMTOMAssertionFound = true;
                    MTOM10Assertion mtomModel = (MTOM10Assertion)assertion;
                    assertEquals("MIME Serialization assertion not processed", false,
                                 mtomModel.isOptional());
                }
            }
            //The Asymm binding mean is not built in the policy processing :-(
            assertTrue("MTOM10 Assertion not found.", isMTOMAssertionFound);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testMTOM10Optional() {
        //Testing the wsp:Optional attribute in WS-MTOMPolicy 1.0 assertion
        try {
            Policy p = this.getPolicy(
            	getClass().getResourceAsStream("/policy-mtom-optional.xml"));
            List<PolicyComponent> assertions = p.getAlternatives().next();

            for(PolicyComponent assertion: assertions) {
                if (assertion instanceof MTOM10Assertion) {
                    MTOM10Assertion mtomModel = (MTOM10Assertion)assertion;
                    assertEquals("wsp:Optional attribute is not processed", true,
                                 mtomModel.isOptional());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testMTOM11Assertion () {
        // Testing the WS-MTOMPolicy 1.1 assertion
        try {
            Policy p = this.getPolicy(
            	getClass().getResourceAsStream("/policy-mtom11.xml"));
            List<PolicyComponent> assertions = p.getAlternatives().next();

            boolean isMTOMAssertionFound = false;

            for(PolicyComponent assertion: assertions) {
                if (assertion instanceof MTOM11Assertion) {
                    isMTOMAssertionFound = true;
                }
            }
            assertTrue("MTOM11 Assertion not found.", isMTOMAssertionFound);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testMTOM11AssertionOptional() {
    	//Testing the wsp:Optional attribute in WS-MTOMPolicy 1.0 assertion
        try {
            Policy p = this.getPolicy(
            	getClass().getResourceAsStream("/policy-mtom11-optional.xml"));
            List<PolicyComponent> assertions = p.getAlternatives().next();

            for(PolicyComponent assertion: assertions) {
                if (assertion instanceof MTOM10Assertion) {
                    MTOM10Assertion mtomModel = (MTOM10Assertion)assertion;
                    assertEquals("wsp:Optional attribute is not processed", true,
                                 mtomModel.isOptional());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testMTOMAssertionInheritance() {
        //Testing the MTOM10Assertion, MTOM11Assertion should be sub classes of MTOMAssertion
        boolean isSubclass;

        MTOM10Assertion mtom10 = new MTOM10Assertion();
        isSubclass = mtom10 instanceof MTOMAssertion;

        assertEquals("MTOM10Assertions is not subclass of MTOMAssertion",true, isSubclass);

        MTOM11Assertion mtom11 = new MTOM11Assertion();
        isSubclass = mtom11 instanceof MTOMAssertion;

        assertEquals("MTOM10Assertions is not subclass of MTOMAssertion",true, isSubclass);
    }

    private Policy getPolicy(InputStream stream) throws Exception {
        StAXOMBuilder builder = new StAXOMBuilder(stream);
        OMElement elem = builder.getDocumentElement();
        return PolicyEngine.getPolicy(elem);
    }
}
