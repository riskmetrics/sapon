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

package org.apache.rahas.impl;

import java.text.DateFormat;
import java.util.Date;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.description.Parameter;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.RahasData;
import org.apache.rahas.Token;
import org.apache.rahas.TokenIssuer;
import org.apache.rahas.TrustException;
import org.apache.rahas.TrustUtil;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.message.token.SecurityContextToken;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SCTIssuer implements TokenIssuer {

    public final static String COMPUTED_KEY = "ComputedKey";

    private String configFile;

    private OMElement configElement;

    private String configParamName;

    /**
     * Issue a {@link SecurityContextToken} based on the wsse:Signature or
     * wsse:UsernameToken
     * <p/>
     * This will support returning the SecurityContextToken with the following
     * types of wst:RequestedProof tokens:
     * <ul>
     * <li>xenc:EncryptedKey</li>
     * <li>wst:ComputedKey</li>
     * <li>wst:BinarySecret (for secure transport)</li>
     * </ul>
     */
    public SOAPEnvelope issue(RahasData data) throws TrustException {

        SCTIssuerConfig config = null;
        if (this.configElement != null) {
            config = SCTIssuerConfig
                    .load(configElement
                            .getFirstChildWithName(SCTIssuerConfig.SCT_ISSUER_CONFIG));
        }

        // Look for the file
        if (config == null && this.configFile != null) {
            config = SCTIssuerConfig.load(this.configFile);
        }

        // Look for the param
        if (config == null && this.configParamName != null) {
            Parameter param = data.getInMessageContext().getParameter(this.configParamName);
            if (param != null && param.getValue() != null) {
                config = SCTIssuerConfig.load(((OMElement)param.getValue())
                        .getFirstChildWithName(
                        SCTIssuerConfig.SCT_ISSUER_CONFIG));
            } else {
                throw new TrustException("expectedParameterMissing",
                                         new String[]{this.configParamName});
            }
        }

        if (config == null) {
            throw new TrustException("missingConfiguration",
                                     new String[]{SCTIssuerConfig.SCT_ISSUER_CONFIG
                                             .getLocalPart()});
        }

        // Env
        return createEnvelope(data, config);
    }

    private SOAPEnvelope createEnvelope(RahasData data,
                                        SCTIssuerConfig config) throws TrustException {
        try {
            SOAPEnvelope env = TrustUtil.createSOAPEnvelope(data.getSoapNs());
            int wstVersion = data.getVersion();

            // Get the document
            Document doc = ((Element) env).getOwnerDocument();

            SecurityContextToken sct =
                    new SecurityContextToken(this.getWSCVersion(data.getTokenType()), doc);

            OMElement rstrElem =
                    TrustUtil.createRequestSecurityTokenResponseElement(wstVersion,
                                                                        env.getBody());

            OMElement rstElem =
                    TrustUtil.createRequestedSecurityTokenElement(wstVersion, rstrElem);

            rstElem.addChild((OMElement) sct.getElement());

            String tokenType = data.getTokenType();

            OMElement reqAttachedRef = null;
            OMElement reqUnattachedRef = null;
            if (config.addRequestedAttachedRef) {
                reqAttachedRef = TrustUtil.createRequestedAttachedRef(wstVersion,
                                                         rstrElem,
                                                         "#" + sct.getID(),
                                                         tokenType);
            }

            if (config.addRequestedUnattachedRef) {
                reqUnattachedRef = TrustUtil.createRequestedUnattachedRef(wstVersion,
                                                           rstrElem,
                                                           sct.getIdentifier(),
                                                           tokenType);
            }

            //Creation and expiration times
            Date creationTime = new Date();
            Date expirationTime = new Date();

            expirationTime.setTime(creationTime.getTime() + config.ttl);

            // Use GMT time in milliseconds
            DateFormat zulu = new XmlSchemaDateFormat();

            // Add the Lifetime element
            TrustUtil.createLifetimeElement(wstVersion,
                                            rstrElem,
                                            zulu.format(creationTime),
                                            zulu.format(expirationTime));

            // Store the tokens
            Token sctToken = new Token(sct.getIdentifier(),
                                       (OMElement) sct.getElement(),
                                       creationTime,
                                       expirationTime);

            if(config.addRequestedAttachedRef) {
                sctToken.setAttachedReference(reqAttachedRef.getFirstElement());
            }

            if(config.addRequestedUnattachedRef) {
                sctToken.setUnattachedReference(reqUnattachedRef.getFirstElement());
            }

            byte[] secret = TokenIssuerUtil.getSharedSecret(data, config.keyComputation, config.keySize);
            sctToken.setSecret(secret);

            //Add the RequestedProofToken
            TokenIssuerUtil.handleRequestedProofToken(data,
                                                      wstVersion,
                                                      config,
                                                      rstrElem,
                                                      sctToken,
                                                      doc);

            sctToken.setState(Token.ISSUED);
            TrustUtil.getTokenStore(data.getInMessageContext()).add(sctToken);
            return env;
        } catch (ConversationException e) {
            throw new TrustException(e.getMessage(), e);
        }
    }

    public String getResponseAction(RahasData data) throws TrustException {
        return TrustUtil.getActionValue(data.getVersion(), RahasConstants.RSTR_ACTION_SCT);
    }

    /**
     * @see org.apache.rahas.TokenIssuer#setConfigurationFile(java.lang.String)
     */
    public void setConfigurationFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * @see org.apache.rahas.TokenIssuer#setConfigurationElement(OMElement)
     */
    public void setConfigurationElement(OMElement configElement) {
        this.configElement = configElement;
    }

    public void setConfigurationParamName(String configParamName) {
        this.configParamName = configParamName;
    }

    private int getWSCVersion(String tokenTypeValue) throws ConversationException {

        if (tokenTypeValue == null) {
            return ConversationConstants.DEFAULT_VERSION;
        }

        if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_02)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_02);
        } else if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_12)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_12);
        } else {
            throw new ConversationException("unsupportedSecConvVersion");
        }
    }
}
