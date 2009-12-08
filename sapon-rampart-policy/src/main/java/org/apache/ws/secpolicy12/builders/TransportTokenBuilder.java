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
import org.apache.neethi.builders.xml.XmlPrimitiveAssertion;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.HttpsToken;
import org.apache.ws.secpolicy.model.TransportToken;

public class TransportTokenBuilder implements AssertionBuilder {



    public Assertion build(OMElement element, AssertionBuilderFactory factory) throws IllegalArgumentException {
        TransportToken transportToken = new TransportToken(SPConstants.SP_V12);

        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
        policy = (Policy) policy.normalize(false);

        for (List<PolicyComponent> alts: policy.getAlternatives()) {
            processAlternative(alts, transportToken);
            break; // since there should be only one alternative
        }

        return transportToken;
    }

    public QName[] getKnownElements() {
        return new QName[] {SP12Constants.TRANSPORT_TOKEN};
    }

    private void processAlternative(List<PolicyComponent> assertions,
    								TransportToken parent)
    {
        for (Object element2 : assertions) {
            XmlPrimitiveAssertion primtive = (XmlPrimitiveAssertion) element2;
            QName qname = primtive.getName();

            if (SP12Constants.HTTPS_TOKEN.equals(qname)) {
                HttpsToken httpsToken = new HttpsToken(SPConstants.SP_V12);

                OMElement element = primtive.getValue().getFirstChildWithName(SPConstants.POLICY);

                if (element != null) {
                    OMElement child = element.getFirstElement();
                    if (child != null) {
                        if (SP12Constants.HTTP_BASIC_AUTHENTICATION.equals(child.getQName())) {
                            httpsToken.setHttpBasicAuthentication(true);
                        } else if (SP12Constants.HTTP_DIGEST_AUTHENTICATION.equals(child.getQName())) {
                            httpsToken.setHttpDigestAuthentication(true);
                        } else if (SP12Constants.REQUIRE_CLIENT_CERTIFICATE.equals(child.getQName())) {
                            httpsToken.setRequireClientCertificate(true);
                        }
                    }
                }

                parent.setToken(httpsToken);
            }
        }
    }
}
