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

package org.apache.rampart.builder;

import java.util.Vector;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.TrustException;
import org.apache.rampart.RampartConstants;
import org.apache.rampart.RampartException;
import org.apache.rampart.RampartMessageData;
import org.apache.rampart.policy.RampartPolicyData;
import org.apache.rampart.util.RampartUtil;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.model.AlgorithmSuite;
import org.apache.ws.secpolicy.model.Header;
import org.apache.ws.secpolicy.model.IssuedToken;
import org.apache.ws.secpolicy.model.SecureConversationToken;
import org.apache.ws.secpolicy.model.SignedEncryptedParts;
import org.apache.ws.secpolicy.model.SupportingToken;
import org.apache.ws.secpolicy.model.Token;
import org.apache.ws.secpolicy.model.UsernameToken;
import org.apache.ws.secpolicy.model.X509Token;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.message.WSSecDKSign;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TransportBindingBuilder extends BindingBuilder {

    private static Log log = LogFactory.getLog(TransportBindingBuilder.class);
    private static Log tlog = LogFactory.getLog(RampartConstants.TIME_LOG);
    private boolean dotDebug = false;

    public TransportBindingBuilder(){
    	dotDebug = tlog.isDebugEnabled();
    }

    public void build(RampartMessageData rmd) throws RampartException {

        log.debug("TransportBindingBuilder build invoked");

        long t0 = 0, t1 = 0;
    	if(dotDebug){
    		t1 = System.currentTimeMillis();
    	}

        RampartPolicyData rpd = rmd.getPolicyData();

        if (rpd.isIncludeTimestamp()) {
        	addTimestamp(rmd);
        }

        /*
         * Process Supporting tokens
         */
        if(rmd.isInitiator()) {
            Vector signatureValues = new Vector();

            SupportingToken sgndSuppTokens = rpd.getSignedSupportingTokens();

            if(sgndSuppTokens != null && sgndSuppTokens.getTokens() != null &&
                    sgndSuppTokens.getTokens().size() > 0) {

                log.debug("Processing signed supporting tokens");

                for(Token token: sgndSuppTokens.getTokens()) {
                    if(token instanceof UsernameToken) {
                        WSSecUsernameToken utBuilder = addUsernameToken(rmd,(UsernameToken)token);

                        utBuilder.prepare(rmd.getDocument());

                        //Add the UT
                        utBuilder.appendToHeader(rmd.getSecHeader());

                    } else {
                        throw new RampartException("unsupportedSignedSupportingToken",
                                new String[]{"{" +token.getName().getNamespaceURI()
                                + "}" + token.getName().getLocalPart()});
                    }
                }
            }

            SupportingToken sgndEndSuppTokens = rpd.getSignedEndorsingSupportingTokens();
            if(sgndEndSuppTokens != null && sgndEndSuppTokens.getTokens() != null &&
                    sgndEndSuppTokens.getTokens().size() > 0) {

                log.debug("Processing endorsing signed supporting tokens");


                SignedEncryptedParts signdParts = sgndEndSuppTokens.getSignedParts();
                for(Token token: sgndEndSuppTokens.getTokens()) {
                    if(token instanceof IssuedToken && rmd.isInitiator()) {
                        signatureValues.add(doIssuedTokenSignature(rmd, token, signdParts));
                    } else if(token instanceof X509Token) {
                        signatureValues.add(doX509TokenSignature(rmd, token, signdParts));
                    }
                }
            }

            SupportingToken endSupptokens = rpd.getEndorsingSupportingTokens();
            if(endSupptokens != null && endSupptokens.getTokens() != null &&
                    endSupptokens.getTokens().size() > 0) {
                log.debug("Processing endorsing supporting tokens");
                SignedEncryptedParts signdParts = endSupptokens.getSignedParts();
                for(Token token: endSupptokens.getTokens()) {
                    if(token instanceof IssuedToken && rmd.isInitiator()){
                        signatureValues.add(doIssuedTokenSignature(rmd, token, signdParts));
                    } else if(token instanceof X509Token) {
                        signatureValues.add(doX509TokenSignature(rmd, token, signdParts));
                    } else if (token instanceof SecureConversationToken) {
                        handleSecureConversationTokens(rmd, (SecureConversationToken)token);
                        signatureValues.add(doSecureConversationSignature(rmd, token, signdParts));
                    }
                }
            }


            SupportingToken supportingToks = rpd.getSupportingTokens();
            this.handleSupportingTokens(rmd, supportingToks);


            //Store the signature values vector
            rmd.getMsgContext().setProperty(WSHandlerConstants.SEND_SIGV, signatureValues);
        } else {
            addSignatureConfirmation(rmd, null);
        }

    	if(dotDebug){
    		t1 = System.currentTimeMillis();
    		tlog.debug("Transport binding build took "+ (t1 - t0));
    	}
    }



    /**
     * X.509 signature
     * @param rmd
     * @param token
     * @param signdParts
     */
    private byte[] doX509TokenSignature(RampartMessageData rmd, Token token, SignedEncryptedParts signdParts) throws RampartException {

        RampartPolicyData rpd = rmd.getPolicyData();
        Document doc = rmd.getDocument();

        Vector sigParts = new Vector();

        if(this.timestampElement != null){
            sigParts.add(new WSEncryptionPart(rmd.getTimestampId()));
        }

        if(signdParts != null) {
            if(signdParts.isBody()) {
                SOAPEnvelope env = rmd.getMsgContext().getEnvelope();
                sigParts.add(new WSEncryptionPart(RampartUtil.addWsuIdToElement(env.getBody())));
            }

            for(Header header: signdParts.getHeaders()) {
                WSEncryptionPart wep = new WSEncryptionPart(header.getName(),
                        header.getNamespace(),
                        "Content");
                sigParts.add(wep);
            }
        }
        if(token.isDerivedKeys()) {
            //In this case we will have to encrypt the ephmeral key with the
            //other party's key and then use it as the parent key of the
            // derived keys
            try {

                WSSecEncryptedKey encrKey = getEncryptedKeyBuilder(rmd, token);

                Element bstElem = encrKey.getBinarySecurityTokenElement();
                if(bstElem != null) {
                   RampartUtil.appendChildToSecHeader(rmd, bstElem);
                }

                encrKey.appendToHeader(rmd.getSecHeader());

                WSSecDKSign dkSig = new WSSecDKSign();

                dkSig.setWsConfig(rmd.getConfig());

                dkSig.setSigCanonicalization(rpd.getAlgorithmSuite().getInclusiveC14n());
                dkSig.setSignatureAlgorithm(rpd.getAlgorithmSuite().getSymmetricSignature());
                dkSig.setDerivedKeyLength(rpd.getAlgorithmSuite().getSignatureDerivedKeyLength()/8);

                dkSig.setExternalKey(encrKey.getEphemeralKey(), encrKey.getId());

                dkSig.prepare(doc, rmd.getSecHeader());


                if(rpd.isTokenProtection()) {
                    sigParts.add(new WSEncryptionPart(encrKey.getBSTTokenId()));
                }

                dkSig.setParts(sigParts);

                dkSig.addReferencesToSign(sigParts, rmd.getSecHeader());

                //Do signature
                dkSig.computeSignature();

                dkSig.appendDKElementToHeader(rmd.getSecHeader());

                dkSig.appendSigToHeader(rmd.getSecHeader());

                return dkSig.getSignatureValue();

            } catch (WSSecurityException e) {
                throw new RampartException("errorInDerivedKeyTokenSignature", e);
            } catch (ConversationException e) {
                throw new RampartException("errorInDerivedKeyTokenSignature", e);
            }

        } else {

            try {
                WSSecSignature sig = this.getSignatureBuider(rmd, token);


                sig.appendBSTElementToHeader(rmd.getSecHeader());

                if (rpd.isTokenProtection()
                        && !(SPConstants.INCLUDE_TOKEN_NEVER == token.getInclusion())) {
                    sigParts.add(new WSEncryptionPart(sig.getBSTTokenId()));
                }

                sig.addReferencesToSign(sigParts, rmd.getSecHeader());

                sig.appendToHeader(rmd.getSecHeader());

                sig.computeSignature();

                return sig.getSignatureValue();
            } catch (WSSecurityException e) {
                throw new RampartException("errorInSignatureWithX509Token", e);
            }


        }

    }


    /**
     * IssuedToken signature
     * @param rmd
     * @param token
     * @param signdParts
     * @throws RampartException
     */
    private byte[] doIssuedTokenSignature(RampartMessageData rmd, Token token, SignedEncryptedParts signdParts) throws RampartException {

        RampartPolicyData rpd = rmd.getPolicyData();
        Document doc= rmd.getDocument();

        //Get the issued token
        String id = RampartUtil.getIssuedToken(rmd, (IssuedToken)token);

        int inclusion = token.getInclusion();
        org.apache.rahas.Token tok = null;
        try {
          tok = rmd.getTokenStorage().getToken(id);
        } catch (TrustException e) {
          throw new RampartException("errorExtractingToken",
                  new String[]{id} ,e);
        }

        boolean tokenIncluded = false;

        if(inclusion == SPConstants.INCLUDE_TOEKN_ALWAYS ||
        ((inclusion == SPConstants.INCLUDE_TOEKN_ALWAYS_TO_RECIPIENT
                || inclusion == SPConstants.INCLUDE_TOKEN_ONCE)
                && rmd.isInitiator())) {

            //Add the token
            rmd.getSecHeader().getSecurityHeader().appendChild(
                  doc.importNode((Element) tok.getToken(), true));

            tokenIncluded = true;
        }

        Vector sigParts = new Vector();

        if(this.timestampElement != null){
            sigParts.add(new WSEncryptionPart(rmd.getTimestampId()));
        }


        if(rpd.isTokenProtection() && tokenIncluded) {
            sigParts.add(new WSEncryptionPart(id));
        }

        if(signdParts != null) {
            if(signdParts.isBody()) {
                SOAPEnvelope env = rmd.getMsgContext().getEnvelope();
                sigParts.add(new WSEncryptionPart(RampartUtil.addWsuIdToElement(env.getBody())));
            }

            for(Header header: signdParts.getHeaders()) {
                WSEncryptionPart wep = new WSEncryptionPart(header.getName(),
                        header.getNamespace(),
                        "Content");
                sigParts.add(wep);
            }
        }

        //check for derived keys
        AlgorithmSuite algorithmSuite = rpd.getAlgorithmSuite();
        if(token.isDerivedKeys()) {
          //Create a derived key and add
          try {

              //Do Signature with derived keys
              WSSecDKSign dkSign = new WSSecDKSign();

              // Setting the AttachedReference or the UnattachedReference according to the flag
              OMElement ref;
              if (tokenIncluded == true) {
                  ref = tok.getAttachedReference();
              } else {
                  ref = tok.getUnattachedReference();
              }

              if(ref != null) {
                  dkSign.setExternalKey(tok.getSecret(), (Element)
                          doc.importNode((Element) ref, true));
              } else {
                  dkSign.setExternalKey(tok.getSecret(), tok.getId());
              }

              //Set the algo info
              dkSign.setSignatureAlgorithm(algorithmSuite.getSymmetricSignature());
              dkSign.setDerivedKeyLength(algorithmSuite.getSignatureDerivedKeyLength());

              dkSign.prepare(doc);

              dkSign.appendDKElementToHeader(rmd.getSecHeader());

              dkSign.setParts(sigParts);

              dkSign.addReferencesToSign(sigParts, rmd.getSecHeader());

              //Do signature
              dkSign.computeSignature();

              dkSign.appendSigToHeader(rmd.getSecHeader());

              return dkSign.getSignatureValue();

          } catch (ConversationException e) {
              throw new RampartException(
                      "errorInDerivedKeyTokenSignature", e);
          } catch (WSSecurityException e) {
              throw new RampartException(
                      "errorInDerivedKeyTokenSignature", e);
          }

        } else {
            try {
                WSSecSignature sig = new WSSecSignature();
                sig.setWsConfig(rmd.getConfig());
                String tokId = tok.getId();
                if (tokId.charAt(0) == '#') {
		    tokId = tokId.substring(1);
                }
                sig.setCustomTokenId(tokId);
                sig.setCustomTokenValueType(WSConstants.WSS_SAML_NS +
                        WSConstants.SAML_ASSERTION_ID);
                sig.setSecretKey(tok.getSecret());
                sig.setSignatureAlgorithm(algorithmSuite.getAsymmetricSignature());
                sig.setSignatureAlgorithm(algorithmSuite.getSymmetricSignature());
                sig.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING);
                sig.prepare(rmd.getDocument(), RampartUtil.getSignatureCrypto(rpd
                        .getRampartConfig(), rmd.getCustomClassLoader()),
                        rmd.getSecHeader());

                sig.setParts(sigParts);
                sig.addReferencesToSign(sigParts, rmd.getSecHeader());

                //Do signature
                sig.computeSignature();

                //Add elements to header
                this.setInsertionLocation(RampartUtil.insertSiblingAfter(
                        rmd,
                        this.getInsertionLocation(),
                        sig.getSignatureElement()));

                return sig.getSignatureValue();

            } catch (WSSecurityException e) {
                throw new RampartException("errorInSignatureWithACustomToken", e);
            }
        }
    }

    private byte[] doSecureConversationSignature(RampartMessageData rmd, Token token, SignedEncryptedParts signdParts) throws RampartException {

        RampartPolicyData rpd = rmd.getPolicyData();
        Document doc= rmd.getDocument();

        //Get the issued token
        String id = rmd.getSecConvTokenId();

        int inclusion = token.getInclusion();
        org.apache.rahas.Token tok = null;
        try {
          tok = rmd.getTokenStorage().getToken(id);
        } catch (TrustException e) {
          throw new RampartException("errorExtractingToken",
                  new String[]{id} ,e);
        }

        boolean tokenIncluded = false;

        if(inclusion == SPConstants.INCLUDE_TOEKN_ALWAYS ||
        ((inclusion == SPConstants.INCLUDE_TOEKN_ALWAYS_TO_RECIPIENT
                || inclusion == SPConstants.INCLUDE_TOKEN_ONCE)
                && rmd.isInitiator())) {

            //Add the token
            rmd.getSecHeader().getSecurityHeader().appendChild(
                  doc.importNode((Element) tok.getToken(), true));

            tokenIncluded = true;
        }

        Vector sigParts = new Vector();

        if(this.timestampElement != null){
            sigParts.add(new WSEncryptionPart(rmd.getTimestampId()));
        }


        if(rpd.isTokenProtection() && tokenIncluded) {
            sigParts.add(new WSEncryptionPart(id));
        }

        if(signdParts != null) {
            if(signdParts.isBody()) {
                SOAPEnvelope env = rmd.getMsgContext().getEnvelope();
                sigParts.add(new WSEncryptionPart(RampartUtil.addWsuIdToElement(env.getBody())));
            }

            for(Header header: signdParts.getHeaders()) {
                WSEncryptionPart wep = new WSEncryptionPart(header.getName(),
                        header.getNamespace(),
                        "Content");
                sigParts.add(wep);
            }
        }

        //check for derived keys
        AlgorithmSuite algorithmSuite = rpd.getAlgorithmSuite();
        if(token.isDerivedKeys()) {
          //Create a derived key and add
          try {

              //Do Signature with derived keys
              WSSecDKSign dkSign = new WSSecDKSign();

              // Setting the AttachedReference or the UnattachedReference according to the flag
              OMElement ref;
              if (tokenIncluded == true) {
                  ref = tok.getAttachedReference();
              } else {
                  ref = tok.getUnattachedReference();
              }

              if(ref != null) {
                  dkSign.setExternalKey(tok.getSecret(), (Element)
                          doc.importNode((Element) ref, true));
              } else {
                  dkSign.setExternalKey(tok.getSecret(), tok.getId());
              }

              //Set the algo info
              dkSign.setSignatureAlgorithm(algorithmSuite.getSymmetricSignature());
              dkSign.setDerivedKeyLength(algorithmSuite.getSignatureDerivedKeyLength());

              dkSign.prepare(doc);

              dkSign.appendDKElementToHeader(rmd.getSecHeader());

              dkSign.setParts(sigParts);

              dkSign.addReferencesToSign(sigParts, rmd.getSecHeader());

              //Do signature
              dkSign.computeSignature();

              dkSign.appendSigToHeader(rmd.getSecHeader());

              return dkSign.getSignatureValue();

          } catch (ConversationException e) {
              throw new RampartException(
                      "errorInDerivedKeyTokenSignature", e);
          } catch (WSSecurityException e) {
              throw new RampartException(
                      "errorInDerivedKeyTokenSignature", e);
          }

        } else {
            try {
                WSSecSignature sig = new WSSecSignature();
                sig.setWsConfig(rmd.getConfig());
                sig.setCustomTokenId(tok.getId().substring(1));
                sig.setCustomTokenValueType(WSConstants.WSS_SAML_NS +
                        WSConstants.SAML_ASSERTION_ID);
                sig.setSecretKey(tok.getSecret());
                sig.setSignatureAlgorithm(algorithmSuite.getAsymmetricSignature());
                sig.setSignatureAlgorithm(algorithmSuite.getSymmetricSignature());
                sig.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING);
                sig.prepare(rmd.getDocument(), RampartUtil.getSignatureCrypto(rpd
                        .getRampartConfig(), rmd.getCustomClassLoader()),
                        rmd.getSecHeader());

                sig.setParts(sigParts);
                sig.addReferencesToSign(sigParts, rmd.getSecHeader());

                //Do signature
                sig.computeSignature();

                //Add elements to header
                this.setInsertionLocation(RampartUtil.insertSiblingAfter(
                        rmd,
                        this.getInsertionLocation(),
                        sig.getSignatureElement()));

                return sig.getSignatureValue();

            } catch (WSSecurityException e) {
                throw new RampartException("errorInSignatureWithACustomToken", e);
            }
        }
    }

    private void handleSecureConversationTokens(RampartMessageData rmd,
                                      SecureConversationToken secConvTok) throws RampartException {


            MessageContext msgContext = rmd.getMsgContext();

            String secConvTokenId = rmd.getSecConvTokenId();

            //The RSTR has to be secured with the cancelled token
            String action = msgContext.getOptions().getAction();
            boolean cancelReqResp = action.equals(RahasConstants.WST_NS_05_02 + RahasConstants.RSTR_ACTION_CANCEL_SCT) ||
                                       action.equals(RahasConstants.WST_NS_05_02 + RahasConstants.RSTR_ACTION_CANCEL_SCT) ||
                                       action.equals(RahasConstants.WST_NS_05_02 + RahasConstants.RST_ACTION_CANCEL_SCT) ||
                                       action.equals(RahasConstants.WST_NS_05_02 + RahasConstants.RST_ACTION_CANCEL_SCT);

            //In the case of the cancel req or resp we should mark the token as cancelled
            if(secConvTokenId != null && cancelReqResp) {
                try {
                    rmd.getTokenStorage().getToken(secConvTokenId).setState(org.apache.rahas.Token.CANCELLED);
                    msgContext.setProperty(RampartMessageData.SCT_ID, secConvTokenId);

                    //remove from the local map of contexts
                    String contextIdentifierKey = RampartUtil.getContextIdentifierKey(msgContext);
                    RampartUtil.getContextMap(msgContext).remove(contextIdentifierKey);
                } catch (TrustException e) {
                    throw new RampartException("errorExtractingToken",e);
                }
            }

            if (secConvTokenId == null
                    || (secConvTokenId != null &&
                            (!RampartUtil.isTokenValid(rmd, secConvTokenId) && !cancelReqResp))) {

                log.debug("No SecureConversationToken found, " +
                        "requesting a new token");

                try {

                    secConvTokenId = RampartUtil.getSecConvToken(rmd, secConvTok);
                    rmd.setSecConvTokenId(secConvTokenId);

                } catch (TrustException e) {
                    throw new RampartException("errorInObtainingSct", e);
                }
            }

/*          org.apache.rahas.Token token;
            try {
                token = rmd.getTokenStorage().getToken(secConvTokenId);
            } catch (TrustException e) {
                throw new RampartException("errorExtractingToken", e);
            }


            //Add the token to the header
           Element siblingElem = RampartUtil
                    .insertSiblingAfter(rmd, this.getInsertionLocation(),
                            (Element) token.getToken());
            this.setInsertionLocation(siblingElem);*/

        }
}
