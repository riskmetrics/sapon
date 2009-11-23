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

package org.apache.rahas;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.token.Reference;

/**
 * In-memory implementation of the token storage
 */
public class SimpleTokenStore implements TokenStorage {

    protected Map<String, Token> tokens = new Hashtable<String, Token>();

    /**
     * We use a read write lock to improve concurrency while avoiding concurrent modification
     * exceptions.  We allow concurrent reads and avoid concurrent reads and modifications
     * ReentrantReadWriteLock supports a maximum of 65535 recursive write locks and 65535 read locks
     */
     protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

     protected final Lock readLock = readWriteLock.readLock();

     protected final Lock writeLock = readWriteLock.writeLock();

    public void add(Token token) throws TrustException {

        if (token != null && !"".equals(token.getId()) && token.getId() != null) {

            writeLock.lock();

            try {
                if (this.tokens.keySet().size() == 0
                    || (this.tokens.keySet().size() > 0 && !this.tokens
                        .keySet().contains(token.getId()))) {
                    tokens.put(token.getId(), token);
                } else {
                    throw new TrustException("tokenAlreadyExists",
                                            new String[]{token.getId()});
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    public void update(Token token) throws TrustException {

        if (token != null && token.getId() != null && token.getId().trim().length() != 0) {

            writeLock.lock();

            try {
                if (!this.tokens.keySet().contains(token.getId())) {
                    throw new TrustException("noTokenToUpdate", new String[]{token.getId()});
                }
                this.tokens.put(token.getId(), token);
            } finally {
                writeLock.unlock();
            }
        }

    }

    public String[] getTokenIdentifiers() throws TrustException {
        List<String> identifiers = new ArrayList<String>();

        readLock.lock();
        try {
            for (String element : tokens.keySet()) {
                identifiers.add(element);
            }
        } finally {
            readLock.unlock();
        }
        return identifiers.toArray(new String[identifiers.size()]);
    }

    public Token[] getValidTokens() throws TrustException {
        return getTokens(new int[]{Token.ISSUED, Token.RENEWED});
    }

    public Token[] getRenewedTokens() throws TrustException {
        return getTokens(Token.RENEWED);
    }


    public Token[] getCancelledTokens() throws TrustException {
        return getTokens(Token.CANCELLED);
    }

    public Token[] getExpiredTokens() throws TrustException {
        return getTokens(Token.EXPIRED);
    }

    private Token[] getTokens(int[] states) throws TrustException {
        processTokenExpiry();
        List<Token> tokens = new ArrayList<Token>();

        readLock.lock();

        try {
            for (Token token : this.tokens.values()) {
                for (int state : states) {
                    if (token.getState() == state) {
                        tokens.add(token);
                        break;
                    }
                }
            }
        } finally {
            readLock.unlock();
        }
        return tokens.toArray(new Token[tokens.size()]);
    }

    private Token[] getTokens(int state) throws TrustException {
        processTokenExpiry();
        List<Token> tokens = new ArrayList<Token>();

        readLock.lock();

        try {
            for (Object element : this.tokens.values()) {
                Token token = (Token) element;
                if (token.getState() == state) {
                    tokens.add(token);
                }
            }
        } finally {
            readLock.unlock();
        }
        return tokens.toArray(new Token[tokens.size()]);
    }

    public Token getToken(String id) throws TrustException {
        processTokenExpiry();

        readLock.lock();

        Token token;

        try {

            token = this.tokens.get(id);

            if(token == null) {
                //Try to find the token using attached refs & unattached refs
                for (Object element : this.tokens.values()) {
                    Token tempToken = (Token) element;
                    OMElement elem = tempToken.getAttachedReference();
                    if(elem != null && id.equals(getIdFromSTR(elem))) {
                        token = tempToken;
                    }
                    elem = tempToken.getUnattachedReference();
                    if(elem != null && id.equals(getIdFromSTR(elem))) {
                        token = tempToken;
                    }

                }

            }

        } finally {
            readLock.unlock();
        }

        return token;
    }

    protected void processTokenExpiry() throws TrustException {

        readLock.lock();

        try {
            for (Object element : tokens.values()) {
                Token token = (Token) element;
                if (token.getExpires() != null &&
                    token.getExpires().getTime() < System.currentTimeMillis()) {
                    token.setState(Token.EXPIRED);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public static String getIdFromSTR(OMElement str) {
        //ASSUMPTION:SecurityTokenReference/KeyIdentifier
        OMElement child = str.getFirstElement();
        if(child == null) {
            return null;
        }

        if (child.getQName().equals(new QName(WSConstants.SIG_NS, "KeyInfo"))) {
            return child.getText();
        } else if(child.getQName().equals(Reference.TOKEN)) {
            String uri = child.getAttributeValue(new QName("URI"));
            if (uri.charAt(0) == '#') {
	        uri = uri.substring(1);
            }
            return uri;
        } else {
            return null;
        }
    }



}
