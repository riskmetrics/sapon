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

package org.apache.ws.secpolicy;

import javax.xml.namespace.QName;

public class SPConstants {

    public static final String P_NS = "http://schemas.xmlsoap.org/ws/2004/09/policy";

    public static final String WSP_PREFIX = "wsp";

    public static final QName POLICY = new QName(P_NS, "Policy");

    public static final int SP_V11 = 1;

    public static final int SP_V12 = 2;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String LAYOUT = "Layout";

    /**
     * Security Header Layout : Strict
     */
    public final static String LAYOUT_STRICT = "Strict";

    /**
     * Security Header Layout : Lax
     */
    public final static String LAYOUT_LAX = "Lax";

    /**
     * Security Header Layout : LaxTimestampFirst
     */
    public final static String LAYOUT_LAX_TIMESTAMP_FIRST = "LaxTimestampFirst";

    /**
     * Security Header Layout : LaxTimestampLast
     */
    public final static String LAYOUT_LAX_TIMESTAMP_LAST = "LaxTimestampLast";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Protection Order : EncryptBeforeSigning
     */
    public final static String ENCRYPT_BEFORE_SIGNING = "EncryptBeforeSigning";

    /**
     * Protection Order : SignBeforeEncrypting
     */
    public final static String SIGN_BEFORE_ENCRYPTING = "SignBeforeEncrypting";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String ENCRYPT_SIGNATURE = "EncryptSignature";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String PROTECT_TOKENS = "ProtectTokens";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String ONLY_SIGN_ENTIRE_HEADERS_AND_BODY = "OnlySignEntireHeadersAndBody";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String INCLUDE_TIMESTAMP = "IncludeTimestamp";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String SIGNED_PARTS = "SignedParts";

    public final static String ENCRYPTED_PARTS = "EncryptedParts";

    public final static String SIGNED_ELEMENTS = "SignedElements";

    public final static String ENCRYPTED_ELEMENTS = "EncryptedElements";

    public final static String REQUIRED_ELEMENTS = "RequiredElements";

    public final static String CONTENT_ENCRYPTED_ELEMENTS = "ContentEncryptedElements";

    public final static String REQUIRED_PARTS = "RequiredParts";

    public final static String XPATH_VERSION = "XPathVersion";

    public final static String XPATH_EXPR = "XPath";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // X509 Token types

    public final static String X509_TOKEN = "X509Token";

    public final static String WSS_X509_V1_TOKEN10 = "WssX509V1Token10";

    public final static String WSS_X509_V3_TOKEN10 = "WssX509V3Token10";

    public final static String WSS_X509_PKCS7_TOKEN10 = "WssX509Pkcs7Token10";

    public final static String WSS_X509_PKI_PATH_V1_TOKEN10 = "WssX509PkiPathV1Token10";

    public final static String WSS_X509_V1_TOKEN11 = "WssX509V1Token11";

    public final static String WSS_X509_V3_TOKEN11 = "WssX509V3Token11";

    public final static String WSS_X509_PKCS7_TOKEN11 = "WssX509Pkcs7Token11";

    public final static String WSS_X509_PKI_PATH_V1_TOKEN11 = "WssX509PkiPathV1Token11";


    public final static String USERNAME_TOKEN = "UsernameToken";

    public final static String USERNAME_TOKEN10 = "WssUsernameToken10";

    public final static String USERNAME_TOKEN11 = "WssUsernameToken11";


    public final static String TRANSPORT_TOKEN = "TransportToken";

    public final static String HTTPS_TOKEN = "HttpsToken";

    public final static QName REQUIRE_CLIENT_CERTIFICATE = new QName("RequireClientCertificate");

    public final static QName HTTP_BASIC_AUTHENTICATION = new QName("HttpBasicAuthentication");

    public final static QName HTTP_DIGEST_AUTHENTICATION = new QName("HttpDigestAuthentication");

    public final static String SECURITY_CONTEXT_TOKEN = "SecurityContextToken";

    public final static String SECURE_CONVERSATION_TOKEN = "SecureConversationToken";

    public final static String ISSUED_TOKEN = "IssuedToken";


    public final static String SIGNATURE_TOKEN = "SignatureToken";

    public final static String ENCRYPTION_TOKEN = "EncryptionToken";

    public final static String PROTECTION_TOKEN = "ProtectionToken";

    public final static String INITIATOR_TOKEN = "InitiatorToken";

    public final static String RECIPIENT_TOKEN = "RecipientToken";



    public final static String SUPPORTING_TOKENS = "SupportingTokens";

    public final static String SIGNED_SUPPORTING_TOKENS = "SignedSupportingTokens";

    public final static String ENDORSING_SUPPORTING_TOKENS = "EndorsingSupportingTokens";

    public final static String SIGNED_ENDORSING_SUPPORTING_TOKENS = "SignedEndorsingSupportingTokens";

    public final static String ENCRYPTED_SUPPORTING_TOKENS = "EncryptedSupportingTokens";

    public final static String SIGNED_ENCRYPTED_SUPPORTING_TOKENS = "SignedEncryptedSupportingTokens";

    public final static String ENDORSING_ENCRYPTED_SUPPORTING_TOKENS = "EndorsingEncryptedSupportingTokens";

    public final static String SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS = "SignedEndorsingEncryptedSupportingTokens";

    public final static int SUPPORTING_TOKEN_SUPPORTING = 1;

    public final static int SUPPORTING_TOKEN_ENDORSING = 2;

    public final static int SUPPORTING_TOKEN_SIGNED = 3;

    public final static int SUPPORTING_TOKEN_SIGNED_ENDORSING = 4;

    public final static int SUPPORTING_TOKEN_SIGNED_ENCRYPTED = 5;

    public final static int SUPPORTING_TOKEN_ENCRYPTED = 6;

    public final static int SUPPORTING_TOKEN_ENDORSING_ENCRYPTED = 7;

    public final static int SUPPORTING_TOKEN_SIGNED_ENDORSING_ENCRYPTED = 8;

    ////////////////////////////////////////////////////////////////////////////////////////////////


    public final static String ALGO_SUITE = "AlgorithmSuite";


    // /
    // /Algorithm Suites
    // /
    public final static String ALGO_SUITE_BASIC256 = "Basic256";

    public final static String ALGO_SUITE_BASIC192 = "Basic192";

    public final static String ALGO_SUITE_BASIC128 = "Basic128";

    public final static String ALGO_SUITE_TRIPLE_DES = "TripleDes";

    public final static String ALGO_SUITE_BASIC256_RSA15 = "Basic256Rsa15";

    public final static String ALGO_SUITE_BASIC192_RSA15 = "Basic192Rsa15";

    public final static String ALGO_SUITE_BASIC128_RSA15 = "Basic128Rsa15";

    public final static String ALGO_SUITE_TRIPLE_DES_RSA15 = "TripleDesRsa15";

    public final static String ALGO_SUITE_BASIC256_SHA256 = "Basic256Sha256";

    public final static String ALGO_SUITE_BASIC192_SHA256 = "Basic192Sha256";

    public final static String ALGO_SUITE_BASIC128_SHA256 = "Basic128Sha256";

    public final static String ALGO_SUITE_TRIPLE_DES_SHA256 = "TripleDesSha256";

    public final static String ALGO_SUITE_BASIC256_SHA256_RSA15 = "Basic256Sha256Rsa15";

    public final static String ALGO_SUITE_BASIC192_SHA256_RSA15 = "Basic192Sha256Rsa15";

    public final static String ALGO_SUITE_BASIC128_SHA256_RSA15 = "Basic128Sha256Rsa15";

    public final static String ALGO_SUITE_TRIPLE_DES_SHA256_RSA15 = "TripleDesSha256Rsa15";

    // /
    // /Algorithms
    // /
    public final static String HMAC_SHA1 = "http://www.w3.org/2000/09/xmldsig#hmac-sha1";

    public final static String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    public final static String SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";

    public final static String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";

    public final static String SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";

    public final static String AES128 = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";

    public final static String AES192 = "http://www.w3.org/2001/04/xmlenc#aes192-cbc";

    public final static String AES256 = "http://www.w3.org/2001/04/xmlenc#aes256-cbc";

    public final static String TRIPLE_DES = "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";

    public final static String KW_AES128 = "http://www.w3.org/2001/04/xmlenc#kw-aes128";

    public final static String KW_AES192 = "http://www.w3.org/2001/04/xmlenc#kw-aes192";

    public final static String KW_AES256 = "http://www.w3.org/2001/04/xmlenc#kw-aes256";

    public final static String KW_TRIPLE_DES = "http://www.w3.org/2001/04/xmlenc#kw-tripledes";

    public final static String KW_RSA_OAEP = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";

    public final static String KW_RSA15 = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";

    public final static String P_SHA1 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public final static String P_SHA1_L128 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public final static String P_SHA1_L192 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public final static String P_SHA1_L256 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public final static String XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";

    public final static String XPATH20 = "http://www.w3.org/2002/06/xmldsig-filter2";

    public final static String C14N = "http://www.w3.org/2001/10/xml-c14n#";

    public final static String EX_C14N = "http://www.w3.org/2001/10/xml-exc-c14n#";

    public final static String SNT = "http://www.w3.org/TR/soap12-n11n";

    public final static String STRT10 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform";

    // /////////////////////////////////////////////////////////////////////////////////////////////

    public static final String INCLUSIVE_C14N = "InclusiveC14N";

    public static final String SOAP_NORMALIZATION_10 = "SoapNormalization10";

    public static final String STR_TRANSFORM_10 = "STRTransform10";

    public static final String XPATH10 = "XPath10";

    public static final String XPATH_FILTER20 = "XPathFilter20";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public final static String ATTR_INCLUDE_TOKEN = "IncludeToken";

    public static final String INCLUDE_TOKEN_NEVER_SUFFIX = "/IncludeToken/Never";

    public static final String INCLUDE_TOKEN_ONCE_SUFFIX = "/IncludeToken/Once";

    public static final String INCLUDE_TOEKN_ALWAYS_TO_RECIPIENT_SUFFIX = "/IncludeToken/AlwaysToRecipient";

    public static final String INCLUDE_TOEKN_ALWAYS_TO_INITIATOR_SUFFIX = "/IncludeToken/AlwaysToInitiator";

    public static final String INCLUDE_TOEKN_ALWAYS_SUFFIX = "/IncludeToken/Always";

    public static final int INCLUDE_TOKEN_NEVER = 1;

    public static final int INCLUDE_TOKEN_ONCE = 2;

    public static final int INCLUDE_TOEKN_ALWAYS_TO_RECIPIENT = 3;

    public static final int INCLUDE_TOEKN_ALWAYS_TO_INITIATOR = 4;

    public static final int INCLUDE_TOEKN_ALWAYS = 5;



    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String TRANSPORT_BINDING = "TransportBinding";

    public static final String ASYMMETRIC_BINDING = "AsymmetricBinding";

    public static final String SYMMETRIC_BINDING = "SymmetricBinding";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String REQUIRE_KEY_IDENTIFIRE_REFERENCE = "RequireKeyIdentifierReference";

    public static final String REQUIRE_ISSUER_SERIAL_REFERENCE = "RequireIssuerSerialReference";

    public static final String REQUIRE_EMBEDDED_TOKEN_REFERENCE = "RequireEmbeddedTokenReference";

    public static final String REQUIRE_THUMBPRINT_REFERENCE = "RequireThumbprintReference";

    public static final String REQUIRE_SIGNATURE_CONFIRMATION = "RequireSignatureConfirmation";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String MUST_SUPPORT_REF_KEY_IDENTIFIER = "MustSupportRefKeyIdentifier";

    public static final String MUST_SUPPORT_REF_ISSUER_SERIAL = "MustSupportRefIssuerSerial";

    public static final String MUST_SUPPORT_REF_EXTERNAL_URI = "MustSupportRefExternalURI";

    public static final String MUST_SUPPORT_REF_EMBEDDED_TOKEN = "MustSupportRefEmbeddedToken";

    public static final String MUST_SUPPORT_REF_THUMBPRINT = "MustSupportRefThumbprint";

    public static final String MUST_SUPPORT_REF_ENCRYPTED_KEY = "MustSupportRefEncryptedkey";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String WSS10 = "Wss10";

    public static final String WSS11 = "Wss11";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String TRUST_10 = "Trust10";

    public static final String TRUST_13 = "Trust13";

    public static final String MUST_SUPPORT_CLIENT_CHALLENGE = "MustSupportClientChanllenge";

    public static final String MUST_SUPPORT_SERVER_CHALLENGE = "MustSupportServerChanllenge";

    public static final String REQUIRE_CLIENT_ENTROPY = "RequireClientEntropy";

    public static final String REQUIRE_SERVER_ENTROPY = "RequireServerEntropy";

    public static final String MUST_SUPPORT_ISSUED_TOKENS = "MustSupportIssuedTokens";

    public static final String REQUIRE_REQUEST_SECURITY_TOKEN_COLLECTION = "RequireRequestSecurityTokenCollection";

    public static final String REQUIRE_APPLIES_TO = "RequireAppliesTo";

    public static final String ISSUER = "Issuer";

    public static final String REQUIRE_DERIVED_KEYS = "RequireDerivedKeys";

    public static final String REQUIRE_IMPLIED_DERIVED_KEYS = "RequireImpliedDerivedKeys";

    public static final String REQUIRE_EXPLICIT_DERIVED_KEYS = "RequireExplicitDerivedKeys";

    public static final String REQUIRE_EXTERNAL_URI_REFERNCE = "RequireExternalUriReference";

    public static final String REQUIRE_EXTERNAL_REFERNCE = "RequireExternalReference";

    public static final String REQUIRE_INTERNAL_REFERNCE = "RequireInternalReference";

    public static final String REQUEST_SECURITY_TOKEN_TEMPLATE = "RequestSecurityTokenTemplate";

    public static final String SC10_SECURITY_CONTEXT_TOKEN = "SC10SecurityContextToken";

    public static final String BOOTSTRAP_POLICY = "BootstrapPolicy";

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String HEADER = "Header";

    public static final String BODY = "Body" ;

    public static final String ATTACHMENTS = "Attachments";

    public static final QName NAME = new QName("Name");

    public static final QName NAMESPACE = new QName("Namespace");

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String NO_PASSWORD = "NoPassword";

    public static final String HASH_PASSWORD = "HashPassword";




}
