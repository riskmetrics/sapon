/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.apache.ws.secpolicy.model;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;

public class AsymmetricBinding extends SymmetricAsymmetricBindingBase {

    private InitiatorToken initiatorToken;

    private RecipientToken recipientToken;

    public AsymmetricBinding(int version) {
        super(version);
    }

    /**
     * @return Returns the initiatorToken.
     */
    public InitiatorToken getInitiatorToken() {
        return initiatorToken;
    }

    /**
     * @param initiatorToken
     *            The initiatorToken to set.
     */
    public void setInitiatorToken(InitiatorToken initiatorToken) {
        this.initiatorToken = initiatorToken;
    }

    /**
     * @return Returns the recipientToken.
     */
    public RecipientToken getRecipientToken() {
        return recipientToken;
    }

    /**
     * @param recipientToken
     *            The recipientToken to set.
     */
    public void setRecipientToken(RecipientToken recipientToken) {
        this.recipientToken = recipientToken;
    }

    public QName getName() {
        if (version == SPConstants.SP_V12) {
            return SP12Constants.ASYMMETRIC_BINDING;
        } else {
            return SP11Constants.ASYMMETRIC_BINDING;
        }
    }

    @Override
	public PolicyComponent normalize() {

        if (isNormalized()) {
            return this;
        }

        AlgorithmSuite algorithmSuite = getAlgorithmSuite();
        List<Assertion> configs = algorithmSuite.getConfigurations();

        Policy policy = new Policy();
        ExactlyOne exactlyOne = new ExactlyOne();

        policy.addPolicyComponent(exactlyOne);

        All wrapper;
        AsymmetricBinding asymmetricBinding;

        for (Assertion assertion : configs) {
            wrapper = new All();
            asymmetricBinding = new AsymmetricBinding(this.version);

            asymmetricBinding.setAlgorithmSuite((AlgorithmSuite)assertion);
            asymmetricBinding
                    .setEntireHeadersAndBodySignatures(isEntireHeadersAndBodySignatures());
            asymmetricBinding.setIncludeTimestamp(isIncludeTimestamp());
            asymmetricBinding.setInitiatorToken(getInitiatorToken());
            asymmetricBinding.setLayout(getLayout());
            asymmetricBinding.setProtectionOrder(getProtectionOrder());
            asymmetricBinding.setRecipientToken(getRecipientToken());
            asymmetricBinding.setSignatureProtection(isSignatureProtection());
            asymmetricBinding
                    .setSignedEndorsingSupportingTokens(getSignedEndorsingSupportingTokens());
            asymmetricBinding.setTokenProtection(isTokenProtection());

            asymmetricBinding.setNormalized(true);
            wrapper.addPolicyComponent(wrapper);
        }

        return policy;

    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localname = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:AsymmetricBinding>
        writer.writeStartElement(namespaceURI, localname);
        writer.writeNamespace(prefix, namespaceURI);

        String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (pPrefix == null) {
            writer.setPrefix(SPConstants.WSP_PREFIX, SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(	SPConstants.POLICY.getNamespaceURI(),
        							SPConstants.POLICY.getLocalPart());

        if (initiatorToken == null) {
            throw new RuntimeException("InitiatorToken is not set");
        }

        // <sp:InitiatorToken>
        initiatorToken.serialize(writer);
        // </sp:InitiatorToken>

        if (recipientToken == null) {
            throw new RuntimeException("RecipientToken is not set");
        }

        // <sp:RecipientToken>
        recipientToken.serialize(writer);
        // </sp:RecipientToken>

        AlgorithmSuite algorithmSuite = getAlgorithmSuite();
        if (algorithmSuite == null) {
            throw new RuntimeException("AlgorithmSuite is not set");
        }

        // <sp:AlgorithmSuite>
        algorithmSuite.serialize(writer);
        // </sp:AlgorithmSuite>

        Layout layout = getLayout();
        if (layout != null) {
            // <sp:Layout>
            layout.serialize(writer);
            // </sp:Layout>
        }

        if (isIncludeTimestamp()) {
            // <sp:IncludeTimestamp>
            writer.writeStartElement(namespaceURI, SPConstants.INCLUDE_TIMESTAMP);
            writer.writeEndElement();
            // </sp:IncludeTimestamp>
        }

        if (SPConstants.ENCRYPT_BEFORE_SIGNING.equals(getProtectionOrder())) {
            // <sp:EncryptBeforeSign />
            writer.writeStartElement(namespaceURI, SPConstants.ENCRYPT_BEFORE_SIGNING);
            writer.writeEndElement();
        }

        if (isSignatureProtection()) {
            // <sp:EncryptSignature />
            // FIXME move the String constants to a QName
            writer.writeStartElement(namespaceURI, SPConstants.ENCRYPT_SIGNATURE);
            writer.writeEndElement();
        }

        if (isTokenProtection()) {
            // <sp:ProtectTokens />
            writer.writeStartElement(namespaceURI, SPConstants.PROTECT_TOKENS);
            writer.writeEndElement();
        }

        if (isEntireHeadersAndBodySignatures()) {
            // <sp:OnlySignEntireHeaderAndBody />
            writer.writeStartElement(namespaceURI, SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY);
            writer.writeEndElement();
        }

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:AsymmetircBinding>
        writer.writeEndElement();
    }
}
