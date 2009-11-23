/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.commons.security.secret.handler;

import org.apache.synapse.commons.security.secret.AbstractSecretCallbackHandler;
import org.apache.synapse.commons.security.secret.SingleSecretCallback;
import org.apache.synapse.commons.security.definition.CipherInformation;
import org.apache.synapse.commons.security.enumeration.CipherOperationMode;
import org.apache.synapse.commons.security.enumeration.EncodingType;
import org.apache.synapse.commons.security.wrappers.CipherWrapper;

import java.io.ByteArrayInputStream;

/**
 * SecretCallbackHandler implementation which is compatible to the default encryption used
 * within the JBoss Application Server to decrypt database passwords.
 */
public class JBossEncryptionSecretCallbackHandler extends AbstractSecretCallbackHandler {

    private static final String PASSPHRASE = "jaas is the way";
    private static final String ALGORITHM = "Blowfish";
    
    /**
     * Decrypts the encrypted secret provided by the specified callback handler.
     * 
     * @param singleSecretCallback The singleSecretCallback which secret has to be decrypted
     */
    @Override
    protected void handleSingleSecretCallback(SingleSecretCallback singleSecretCallback) {
        singleSecretCallback.setSecret(decrypt(singleSecretCallback.getId()));
    }
    
    /**
     * Decrypts the encrypted secret using the Blowfish algorithm and the same hard-coded
     * passphrase the JBoss application server uses to decrypt database passwords.
     * 
     * @param encryptedSecret the encrypted secret
     * 
     * @return the decrypted secret.
     */
    private static String decrypt(String encryptedSecret) {
        CipherInformation cipherInformation = new CipherInformation();
        cipherInformation.setAlgorithm(ALGORITHM);
        cipherInformation.setCipherOperationMode(CipherOperationMode.DECRYPT);
        cipherInformation.setInType(EncodingType.BIGINTEGER16);
        CipherWrapper cipherWrapper = new CipherWrapper(cipherInformation, PASSPHRASE);
        return cipherWrapper.getSecret(new ByteArrayInputStream(encryptedSecret.getBytes()));
    }
}
