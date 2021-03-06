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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.X509Token;

public class X509TokenBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory)
            throws IllegalArgumentException {
        X509Token x509Token = new X509Token(SPConstants.SP_V12);

        OMElement policyElement = element.getFirstElement();

        //Process token inclusion
        OMAttribute  includeAttr = element.getAttribute(SP12Constants.INCLUDE_TOKEN);
        if(includeAttr != null) {
            int inclusion = SP12Constants.getInclusionFromAttributeValue(includeAttr.getAttributeValue());
            x509Token.setInclusion(inclusion);
        }

        if (policyElement != null) {

            if (policyElement.getFirstChildWithName(SP12Constants.REQUIRE_DERIVED_KEYS) != null) {
                x509Token.setDerivedKeys(true);
            } else if (policyElement.getFirstChildWithName(SP12Constants.REQUIRE_IMPLIED_DERIVED_KEYS) != null) {
                x509Token.setImpliedDerivedKeys(true);
            } else if (policyElement.getFirstChildWithName(SP12Constants.REQUIRE_EXPLICIT_DERIVED_KEYS) != null) {
                x509Token.setExplicitDerivedKeys(true);
            }

            Policy policy = PolicyEngine.getPolicy(element.getFirstElement());
            policy = (Policy) policy.normalize(false);

            for (List<PolicyComponent> alts: policy.getAlternatives()) {
                processAlternative(alts, x509Token);
                break;
            }
        }
        return x509Token;
    }

    private void processAlternative(List<PolicyComponent> assertions,
    								X509Token parent)
    {
        for (Object element : assertions) {
            Assertion assertion = (Assertion) element;
            QName name = assertion.getName();

            if (SP12Constants.REQUIRE_KEY_IDENTIFIRE_REFERENCE.equals(name)) {
                parent.setRequireKeyIdentifierReference(true);

            } else if (SP12Constants.REQUIRE_ISSUER_SERIAL_REFERENCE.equals(name)) {
                parent.setRequireIssuerSerialReference(true);

            } else if (SP12Constants.REQUIRE_EMBEDDED_TOKEN_REFERENCE.equals(name)) {
                parent.setRequireEmbeddedTokenReference(true);

            } else if (SP12Constants.REQUIRE_THUMBPRINT_REFERENCE.equals(name)) {
                parent.setRequireThumbprintReference(true);

            } else if (SP12Constants.WSS_X509_V1_TOKEN_10.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V1_TOKEN10);

            } else if (SP12Constants.WSS_X509_V1_TOKEN_11.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V1_TOKEN11);

            } else if (SP12Constants.WSS_X509_V3_TOKEN_10.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V3_TOKEN10);

            } else if (SP12Constants.WSS_X509_V3_TOKEN_11.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V3_TOKEN11);

            } else if (SP12Constants.WSS_X509_PKCS7_TOKEN_10.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKCS7_TOKEN10);

            } else if (SP12Constants.WSS_X509_PKCS7_TOKEN_11.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKCS7_TOKEN11);

            } else if (SP12Constants.WSS_X509_PKI_PATH_V1_TOKEN_10.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10);

            } else if (SP12Constants.WSS_X509_PKI_PATH_V1_TOKEN_11.equals(name)) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11);

            }
        }
    }

    public QName[] getKnownElements() {
        return new QName[] {SP12Constants.X509_TOKEN};
    }
}
