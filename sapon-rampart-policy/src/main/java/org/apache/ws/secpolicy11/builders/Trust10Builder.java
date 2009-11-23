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

import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.Trust10;

public class Trust10Builder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory)
            throws IllegalArgumentException {

        element = element.getFirstChildWithName(SPConstants.POLICY);

        if (element == null) {
            throw new IllegalArgumentException(
                    "Trust10 assertion doesn't contain any Policy");
        }

        Trust10 trust10 = new Trust10(SPConstants.SP_V11);

        if (element
                .getFirstChildWithName(SP11Constants.MUST_SUPPORT_CLIENT_CHALLENGE) != null) {
            trust10.setMustSupportClientChallenge(true);
        }

        if (element
                .getFirstChildWithName(SP11Constants.MUST_SUPPORT_SERVER_CHALLENGE) != null) {
            trust10.setMustSupportServerChallenge(true);
        }

        if (element.getFirstChildWithName(SP11Constants.REQUIRE_CLIENT_ENTROPY) != null) {
            trust10.setRequireClientEntropy(true);
        }

        if (element.getFirstChildWithName(SP11Constants.REQUIRE_SERVER_ENTROPY) != null) {
            trust10.setRequireServerEntropy(true);
        }

        if (element.getFirstChildWithName(SP11Constants.MUST_SUPPORT_ISSUED_TOKENS) != null) {
            trust10.setMustSupportIssuedTokens(true);
        }

        return trust10;
    }

    public QName[] getKnownElements() {
        return new QName[] {SP11Constants.TRUST_10};
    }

}
